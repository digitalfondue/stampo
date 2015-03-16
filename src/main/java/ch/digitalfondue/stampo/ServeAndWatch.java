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

import static java.nio.file.Files.copy;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.isDirectory;
import static java.nio.file.Files.newDirectoryStream;
import static java.nio.file.Files.probeContentType;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.util.Collections;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

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

  private static HttpServer createServer(String hostname, int port) {
    try {
      return HttpServer.create(new InetSocketAddress(hostname, port), 0);
    } catch (IOException ioe) {
      throw new IllegalStateException(ioe);
    }
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

    HttpServer server = createServer(hostname, port);

    server.createContext(
        "/",
        (ex) -> {
          System.out.println("requested url: " + ex.getRequestURI());

          String req = ex.getRequestURI().toString().substring(1);

          Path p = configuration.getBaseOutputDir().resolve(req);

          boolean isPathDirectory = isDirectory(p);

          if (isPathDirectory && req.length() > 0 && !req.endsWith("/")) {
            // redirect to req+"/"
            setLocation(ex, "/" + req + "/");
            ex.sendResponseHeaders(302, -1);
            return;
          }

          if (isPathDirectory && exists(p.resolve("index.html"))) {
            p = p.resolve("index.html");
          }

          if (isDirectory(p)) {
            setContentTypeAndNoCache(ex, "text/html;charset=utf-8");
            ex.sendResponseHeaders(200, 0);
            try (OutputStream os = ex.getResponseBody();
                OutputStreamWriter osw = new OutputStreamWriter(os, StandardCharsets.UTF_8);
                DirectoryStream<Path> ds = newDirectoryStream(p)) {

              osw.write("<li><a href=\"..\">go up</a>");
              for (Path path : ds) {

                String fileName = p.relativize(path).toString();
                osw.write(String.format("<li><a href=\"%s\">%s</a>", fileName, fileName));
              }
            }
          } else if (exists(p)) {

            String contentType = probeContentType(p);
            setContentTypeAndNoCache(ex, contentType.equals("text/html") ? "text/html;charset=utf-8"
                : contentType);
            ex.sendResponseHeaders(200, 0);


            try (OutputStream os = ex.getResponseBody()) {
              copy(p, os);
            }
          } else {
            setContentTypeAndNoCache(ex, "text/html;charset=utf-8");
            ex.sendResponseHeaders(404, 0);
            try (OutputStream os = ex.getResponseBody()) {
              os.write(("404 not found " + ex.getRequestURI().toString())
                  .getBytes(StandardCharsets.UTF_8));
            }
          }
        });
    server.setExecutor(null);
    server.start();
  }

  private static void setLocation(HttpExchange ex, String location) {
    ex.getResponseHeaders().set("Location", location);
  }

  private static void setContentTypeAndNoCache(HttpExchange ex, String contentType) {
    ex.getResponseHeaders().set("Content-Type", contentType);
    ex.getResponseHeaders().set("Cache-Control", "no-cache, no-store, must-revalidate");
    ex.getResponseHeaders().set("Pragma", "no-cache"); 
    ex.getResponseHeaders().set("Expires", "0");
  }
}
