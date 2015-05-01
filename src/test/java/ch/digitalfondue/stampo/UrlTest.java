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

import static java.nio.file.Files.write;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.Assert;
import org.junit.Test;

import ch.digitalfondue.stampo.TestUtils.InputOutputDirs;

public class UrlTest {

  /**
   * Files named index.html are not transformed in clean url The others, have the clean url
   * generation applied:
   * 
   * <pre>
   *    /index.html -> /index.html
   *    /not-index.html -> /not-index/index.html
   * </pre>
   */
  @Test
  public void testDefaultCleanUrl() throws IOException {
    try (InputOutputDirs iod = TestUtils.get()) {
      String content = "<h1>Hello World</h1>";
      byte[] b = content.getBytes(StandardCharsets.UTF_8);

      write(iod.inputDir.resolve("content/index.html"), b);
      write(iod.inputDir.resolve("content/not-ugly-url.html"), b);
      Stampo stampo = new Stampo(iod.inputDir, iod.outputDir);
      stampo.build();

      Assert.assertEquals(content, TestUtils.fileOutputAsString(iod, "index.html"));
      Assert.assertEquals(content, TestUtils.fileOutputAsString(iod, "not-ugly-url/index.html"));
    }
  }

  /**
   * Default ugly url: true
   * 
   * @throws IOException
   */
  @Test
  public void testGlobalUglyUrl() throws IOException {

    try (InputOutputDirs iod = TestUtils.get()) {
      String content = "<h1>Hello World</h1>";
      byte[] b = content.getBytes(StandardCharsets.UTF_8);

      write(iod.inputDir.resolve("content/index.html"), b);
      write(iod.inputDir.resolve("content/ugly-url.html"), b);

      write(iod.inputDir.resolve("configuration.yaml"),
          "use-ugly-url: true".getBytes(StandardCharsets.UTF_8));

      Stampo stampo = new Stampo(iod.inputDir, iod.outputDir);
      stampo.build();

      Assert.assertEquals(content, TestUtils.fileOutputAsString(iod, "index.html"));
      Assert.assertEquals(content, TestUtils.fileOutputAsString(iod, "ugly-url.html"));
    }
  }

  
  /**
   * Default ugly url: false
   * Override ugly url as true in a single file
   * 
   * @throws IOException
   */
  @Test
  public void testOverrideUglyUrl() throws IOException {

    try (InputOutputDirs iod = TestUtils.get()) {
      String content = "<h1>Hello World</h1>";
      byte[] b = content.getBytes(StandardCharsets.UTF_8);

      String overrideContent = "---\n" + //
          "override-use-ugly-url : true\n" + //
          "---\n" + //
          "<h1>Hello World</h1>";

      write(iod.inputDir.resolve("content/index.html"), b);
      write(iod.inputDir.resolve("content/not-ugly-url.html"), b);
      write(iod.inputDir.resolve("content/ugly-url.html"), overrideContent.getBytes(StandardCharsets.UTF_8));

      Stampo stampo = new Stampo(iod.inputDir, iod.outputDir);
      stampo.build();

      Assert.assertEquals(content, TestUtils.fileOutputAsString(iod, "index.html"));
      Assert.assertEquals(content, TestUtils.fileOutputAsString(iod, "not-ugly-url/index.html"));
      Assert.assertEquals(content, TestUtils.fileOutputAsString(iod, "ugly-url.html"));
    }
  }
  
  /**
   * Default ugly url: true
   * Override ugly url as false in a single file
   * 
   * @throws IOException
   */
  @Test
  public void testOverrideNotUglyUrl() throws IOException {

    try (InputOutputDirs iod = TestUtils.get()) {
      
      write(iod.inputDir.resolve("configuration.yaml"),
          "use-ugly-url: true".getBytes(StandardCharsets.UTF_8));
      
      String content = "<h1>Hello World</h1>";
      byte[] b = content.getBytes(StandardCharsets.UTF_8);

      String overrideContent = "---\n" + //
          "override-use-ugly-url : false\n" + //
          "---\n" + //
          "<h1>Hello World</h1>";

      write(iod.inputDir.resolve("content/index.html"), b);
      write(iod.inputDir.resolve("content/ugly-url.html"), b);
      write(iod.inputDir.resolve("content/not-ugly-url.html"), overrideContent.getBytes(StandardCharsets.UTF_8));

      Stampo stampo = new Stampo(iod.inputDir, iod.outputDir);
      stampo.build();

      Assert.assertEquals(content, TestUtils.fileOutputAsString(iod, "index.html"));
      Assert.assertEquals(content, TestUtils.fileOutputAsString(iod, "ugly-url.html"));
      Assert.assertEquals(content, TestUtils.fileOutputAsString(iod, "not-ugly-url/index.html"));
      
    }
  }
}
