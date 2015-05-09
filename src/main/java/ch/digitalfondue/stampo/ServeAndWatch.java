/**
 * Copyright (C) 2015 digitalfondue (info@digitalfondue.ch)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ch.digitalfondue.stampo;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.isDirectory;
import static java.nio.file.Files.newDirectoryStream;
import static java.nio.file.Files.probeContentType;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.io.DefaultIoCallback;
import io.undertow.io.Sender;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.websockets.core.AbstractReceiveListener;
import io.undertow.websockets.core.StreamSourceFrameChannel;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.WebSockets;
import io.undertow.websockets.spi.WebSocketHttpExchange;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.channels.FileChannel;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.xnio.IoUtils;

import ch.digitalfondue.stampo.processor.AlphaNumericStringComparator;

import com.google.common.io.CharStreams;


public class ServeAndWatch {

  private final StampoGlobalConfiguration configuration;
  private final String hostname;
  private final int port;
  private final boolean rebuildOnChange;
  private final boolean autoReload;
  private final String reloadScript = getReloadScript();
  private final Runnable triggerBuild;
  
  private final AtomicBoolean run = new AtomicBoolean(false);
  private final CountDownLatch blockOnStart;
  private Undertow server;
  private Optional<Thread> dirWatcherThread;
  private Optional<Thread> changeNotifierThread;
  
  private final Comparator<Path> pathComparator = Comparator.comparing(Path::toString, new AlphaNumericStringComparator(Locale.ENGLISH));


  public ServeAndWatch(String hostname, int port, boolean rebuildOnChange, boolean autoReload,
      StampoGlobalConfiguration configuration, Runnable triggerBuild, boolean blockingOnStart) {
    this.configuration = configuration;
    this.hostname = hostname;
    this.rebuildOnChange = rebuildOnChange;
    this.autoReload = autoReload;
    this.port = port;
    this.triggerBuild = triggerBuild;
    this.blockOnStart = new CountDownLatch(blockingOnStart ? 1 : 0);
  }

  public void start() {

    Set<WebSocketChannel> activeChannels = Collections.newSetFromMap(new ConcurrentHashMap<>());

    DelayQueue<Delayed> delayQueue = new DelayQueue<Delayed>();

    run.set(true);
    
    if (rebuildOnChange) {
      
      dirWatcherThread = 
          Optional.of(new Thread(() -> {
            try {
              WatchDir wd =
                  new WatchDir(configuration.getBaseDirectory(), Collections
                      .singleton(configuration.getBaseOutputDir()), configuration
                      .getIgnorePatterns());
              
              while (run.get()) {
                wd.processEvent(delayQueue);
              }
            } catch (IOException ioe) {
            }
          }));


      changeNotifierThread =
          Optional.of(new Thread(() -> {
            try {
              while (run.get()) {
                Delayed d = delayQueue.poll(500, TimeUnit.MILLISECONDS);
                if (d != null && delayQueue.isEmpty()) {
                  try {
                    triggerBuild.run();
                    activeChannels.stream().filter(WebSocketChannel::isOpen)
                        .forEach((wsc) -> WebSockets.sendText("change", wsc, null));
                  } catch (Throwable e) {
                    e.printStackTrace();
                  }
                }
              }
            } catch (InterruptedException ie) {
              //
            }
          }));
    } else {
      dirWatcherThread = Optional.empty();
      changeNotifierThread = Optional.empty();
    }
    
    dirWatcherThread.ifPresent(Thread::start);
    changeNotifierThread.ifPresent(Thread::start);

    server = prepareServer(activeChannels);
    server.start();
    
    try {
      blockOnStart.await();
    } catch (InterruptedException ie) {
    }
  }

  public void stop() {
    
    blockOnStart.countDown();
    
    run.set(false);

    Consumer<Thread> t = (thread) -> {
      try {
        thread.join();
      } catch(InterruptedException ie) {
        //
      }
    };
    
    dirWatcherThread.ifPresent(t);
    changeNotifierThread.ifPresent(t);
    server.stop();
  }

  private Undertow prepareServer(Set<WebSocketChannel> activeChannels) {

    HttpHandler handler =
        autoReload ? Handlers.path()
            .addExactPath("/stampo-reload", websocketHandler(activeChannels))
            .addPrefixPath("/", staticResourcesHandler()) : staticResourcesHandler();

    Undertow server =
        Undertow.builder().addHttpListener(port, hostname).setHandler(handler).build();
    return server;
  }


  private static class WebsocketReceiveListener extends AbstractReceiveListener {

    private final Set<WebSocketChannel> activeChannels;

    public WebsocketReceiveListener(Set<WebSocketChannel> activeChannels) {
      this.activeChannels = activeChannels;
    }

    @Override
    protected void onClose(WebSocketChannel webSocketChannel, StreamSourceFrameChannel channel)
        throws IOException {
      activeChannels.remove(webSocketChannel);
      super.onClose(webSocketChannel, channel);
    }

    @Override
    protected void onError(WebSocketChannel channel, Throwable error) {
      activeChannels.remove(channel);
      super.onError(channel, error);
    }
  }

  private static HttpHandler websocketHandler(Set<WebSocketChannel> activeChannels) {
    return Handlers.websocket((WebSocketHttpExchange exchange, WebSocketChannel channel) -> {
      activeChannels.add(channel);
      channel.getReceiveSetter().set(new WebsocketReceiveListener(activeChannels));
      channel.resumeReceives();
    });
  }

  private static String getReloadScript() {
    try (InputStream is =
        ServeAndWatch.class.getClassLoader().getResourceAsStream("stampo-reload-script.js")) {
      return CharStreams.toString(new InputStreamReader(is, UTF_8));
    } catch (IOException e) {
      throw new IllegalStateException("cannot read stampo-reload-script.js", e);
    }
  }

  private String injectWebsocketScript(String s) {
    return autoReload ? s.replaceFirst("<head>",
        "<head><script>/* websocket script for auto reload */ " + reloadScript + "</script>") : s;
  }

  private HttpHandler staticResourcesHandler() {
    return (ex) -> {
      System.out.println("requested url: " + ex.getRequestURI());
      String req = ex.getRequestURI().toString().substring(1);
      Path p = configuration.getBaseOutputDir().resolve(req);
      boolean isPathDirectory = isDirectory(p);
      if (isPathDirectory && req.length() > 0 && !req.endsWith("/")) {
        // redirect to req+"/"
        ex.getResponseHeaders().put(Headers.LOCATION, "/" + req + "/");
        ex.setResponseCode(302);
        return;
      }

      if (isPathDirectory && exists(p.resolve("index.html"))) {
        p = p.resolve("index.html");
      }

      if (isDirectory(p)) {
        setContentTypeAndNoCache(ex, "text/html;charset=utf-8");
        ex.setResponseCode(200);

        try (DirectoryStream<Path> ds = newDirectoryStream(p)) {
          Sender sender = ex.getResponseSender();

          StringBuilder sb =
              new StringBuilder(
                  "<!DOCTYPE html><html><head></head><body><li><a href=\"..\">go up</a>");
          
          List<Path> paths = new ArrayList<>();
          ds.iterator().forEachRemaining(paths::add);
          paths.sort(pathComparator);
          
          for (Path path : paths) {

            String fileName = p.relativize(path).toString();
            sb.append(String.format("<li><a href=\"%s\">%s</a>", fileName, fileName));
          }

          sb.append("</body></html>");

          sender.send(injectWebsocketScript(sb.toString()), UTF_8);
        }
      } else if (exists(p)) {

        String contentType = probeContentType(p);

        boolean isHtmlFile = contentType.equals("text/html");
        setContentTypeAndNoCache(ex, isHtmlFile ? "text/html;charset=utf-8" : contentType);
        ex.setResponseCode(200);

        Sender sender = ex.getResponseSender();

        if (!isHtmlFile) {
          FileChannel fc = FileChannel.open(p, StandardOpenOption.READ);
          sender.transferFrom(fc, new CloseFileChannelIoCallback(fc));
        } else {
          sender.send(injectWebsocketScript(new String(Files.readAllBytes(p), UTF_8)), UTF_8);
        }

      } else {
        setContentTypeAndNoCache(ex, "text/html;charset=utf-8");
        ex.setResponseCode(404);
        ex.getResponseSender().send(
            injectWebsocketScript("<!DOCTYPE html><html><head></head><body>404 not found "
                + ex.getRequestURI().toString() + "</body></html>"), UTF_8);
      }
    };
  }

  static class CloseFileChannelIoCallback extends DefaultIoCallback {

    private final FileChannel fc;

    public CloseFileChannelIoCallback(FileChannel fc) {
      this.fc = fc;
    }

    @Override
    public void onComplete(HttpServerExchange exchange, Sender sender) {
      IoUtils.safeClose(fc);
      super.onComplete(exchange, sender);
    }

    @Override
    public void onException(HttpServerExchange exchange, Sender sender, IOException exception) {
      IoUtils.safeClose(fc);
      super.onException(exchange, sender, exception);
    }
  }



  private static void setContentTypeAndNoCache(HttpServerExchange ex, String contentType) {
    ex.getResponseHeaders().put(Headers.CONTENT_TYPE, contentType)
        .put(Headers.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
        .put(Headers.PRAGMA, "no-cache").put(Headers.EXPIRES, "0");
  }
}
