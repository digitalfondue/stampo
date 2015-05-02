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

import static ch.digitalfondue.stampo.TestUtils.fromTestResource;
import static ch.digitalfondue.stampo.TestUtils.fromTestResourceAsString;
import static ch.digitalfondue.stampo.TestUtils.get;
import static java.nio.file.Files.write;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.CountDownLatch;

import org.junit.Assert;
import org.junit.Test;

import ch.digitalfondue.stampo.TestUtils.InputOutputDirs;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;

public class ServeAndWatchTest {

  @Test
  public void checkOutput() throws IOException, InterruptedException {
    try (InputOutputDirs iod = get(); WebClient webClient = new WebClient()) {

      //
      createContentPagination(iod);
      //


      Stampo stampo = new Stampo(iod.inputDir, iod.outputDir);
      
      CountDownLatch cdl = new CountDownLatch(1);

      ServeAndWatch sw =
          new ServeAndWatch("localhost", 8080, true, true, stampo.getConfiguration(), () -> {
            new Stampo(iod.inputDir, iod.outputDir).build();
            cdl.countDown();
          });
      sw.start();

      // check 404
      try {
        webClient.getPage("http://localhost:8080");
      } catch (FailingHttpStatusCodeException e) {
        Assert.assertEquals(404, e.getResponse().getStatusCode());
      }

      stampo.build();

      // check index
      Assert.assertEquals(fromTestResourceAsString("pagination/result/recursive/index.html"),
          webClient.getPage("http://localhost:8080").getWebResponse().getContentAsString());


      // check directory listing
      Assert.assertTrue(webClient.getPage("http://localhost:8080/post/").getWebResponse()
          .getContentAsString()
          .contains(fromTestResourceAsString("serveandwatch/directory-listing.html")));
      
      
      // static content
      Page page = webClient.getPage("http://localhost:8080/texts/1.txt");
      Assert.assertEquals("text/plain", page.getWebResponse().getContentType());
      Assert.assertEquals("hello world", page.getWebResponse().getContentAsString("UTF-8"));
      
      
      try {
        webClient.getPage("http://localhost:8080/texts/2.txt");
      } catch (FailingHttpStatusCodeException e) {
        Assert.assertEquals(404, e.getResponse().getStatusCode());
      }
      
      // add new file
      write(iod.inputDir.resolve("static/texts/2.txt"), "hello world 2".getBytes(StandardCharsets.UTF_8));
      cdl.await();
      // newly created static content
      Page page2 = webClient.getPage("http://localhost:8080/texts/2.txt");
      Assert.assertEquals("text/plain", page2.getWebResponse().getContentType());
      Assert.assertEquals("hello world 2", page2.getWebResponse().getContentAsString("UTF-8"));
      //
      
      
      sw.stop();
    }
  }

  private void createContentPagination(InputOutputDirs iod) throws IOException {
    write(iod.inputDir.resolve("content/index.html.peb"),
        fromTestResource("pagination/index-recursive.html.peb"));
    Files.createDirectories(iod.inputDir.resolve("content/post"));
    Files.createDirectories(iod.inputDir.resolve("content/post/durpdurp"));
    
    Files.createDirectories(iod.inputDir.resolve("static/texts"));

    write(iod.inputDir.resolve("static/texts/1.txt"), "hello world".getBytes(StandardCharsets.UTF_8));
    

    for (int i = 1; i <= 20; i++) {
      write(iod.inputDir.resolve("content/post/post" + i + ".md"),
          fromTestResource("pagination/post/post" + i + ".md"));
    }

    for (int i = 21; i <= 31; i++) {
      write(iod.inputDir.resolve("content/post/durpdurp/post" + i + ".md"),
          fromTestResource("pagination/post/durpdurp/post" + i + ".md"));
    }
  }
}
