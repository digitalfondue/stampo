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

import static java.nio.file.Files.exists;
import static java.nio.file.Files.isDirectory;
import static java.nio.file.Files.newDirectoryStream;
import static java.nio.file.Files.probeContentType;
import io.undertow.Undertow;
import io.undertow.io.DefaultIoCallback;
import io.undertow.io.Sender;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;

import org.xnio.IoUtils;


public class ServeAndWatch {

  private final StampoGlobalConfiguration configuration;
  private final String hostname;
  private final int port;
  private final boolean rebuildOnChange;


  public ServeAndWatch(String hostname, int port, boolean rebuildOnChange,
      StampoGlobalConfiguration configuration) {
    this.configuration = configuration;
    this.hostname = hostname;
    this.rebuildOnChange = rebuildOnChange;
    this.port = port;
  }

  public void serve(Runnable triggerBuild) {

    DelayQueue<Delayed> delayQueue = new DelayQueue<Delayed>();

    if (rebuildOnChange) {
      new Thread(() -> {
        try {
          new WatchDir(configuration.getBaseDirectory(), Collections.singleton(configuration
              .getBaseOutputDir()), configuration.getIgnorePatterns()).processEvents(delayQueue);
        } catch (IOException ioe) {
        }
      }).start();


      new Thread(() -> {
        try {
          for (;;) {
            delayQueue.take();
            if (delayQueue.isEmpty()) {
              try {
                triggerBuild.run();
              } catch (Throwable e) {
                e.printStackTrace();
              }
            }
          }
        } catch (InterruptedException ie) {
        }
      }).start();

    }
    
    Undertow server = Undertow.builder()
        .addHttpListener(port, hostname)
        .setHandler((ex) -> {
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
              
              StringBuilder sb = new StringBuilder("<li><a href=\"..\">go up</a>");
              for (Path path : ds) {

                String fileName = p.relativize(path).toString();
                sb.append(String.format("<li><a href=\"%s\">%s</a>", fileName, fileName));
              }
              
              sender.send(sb.toString(), StandardCharsets.UTF_8);
            }
          } else if (exists(p)) {

            String contentType = probeContentType(p);
            setContentTypeAndNoCache(ex, contentType.equals("text/html") ? "text/html;charset=utf-8"
                : contentType);
            ex.setResponseCode(200);

            Sender sender = ex.getResponseSender();
            FileChannel fc = FileChannel.open(p, StandardOpenOption.READ);
            sender.transferFrom(fc, new CloseFileChannelIoCallback(fc));
            
          } else {
            setContentTypeAndNoCache(ex, "text/html;charset=utf-8");
            ex.setResponseCode(404);
            ex.getResponseSender().send("404 not found " + ex.getRequestURI().toString(), StandardCharsets.UTF_8);
          }
          
        }).build();
    server.start();
  }
  
  public static class CloseFileChannelIoCallback extends DefaultIoCallback {
    
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
    .put(Headers.PRAGMA, "no-cache")
    .put(Headers.EXPIRES, "0");
  }
}
