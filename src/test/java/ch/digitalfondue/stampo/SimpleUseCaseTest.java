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
import java.nio.file.Files;
import java.util.Collections;

import org.junit.Assert;
import org.junit.Test;

import ch.digitalfondue.stampo.TestUtils.InputOutputDirs;

public class SimpleUseCaseTest {

  @Test
  public void simpleContent() throws IOException {
    try (InputOutputDirs iod = TestUtils.get()) {
      write(iod.inputDir.resolve("content/index.html"),
          "<h1>Hello World</h1>".getBytes(StandardCharsets.UTF_8));
      Stampo stampo = new Stampo(iod.inputDir, iod.outputDir, Collections.emptyMap());
      stampo.build();

      Assert.assertEquals("<h1>Hello World</h1>", TestUtils.fileOutputAsString(iod, "index.html"));
    }
  }
  
  @Test
  public void simpleContentWithoutExtension() throws IOException {
    try (InputOutputDirs iod = TestUtils.get()) {
      write(iod.inputDir.resolve("content/index"),
          "<h1>Hello World</h1>".getBytes(StandardCharsets.UTF_8));
      Stampo stampo = new Stampo(iod.inputDir, iod.outputDir, Collections.emptyMap());
      stampo.build();

      Assert.assertEquals("<h1>Hello World</h1>", TestUtils.fileOutputAsString(iod, "index"));
    }
  }


  @Test
  public void simpleContentMultipleLines() throws IOException {
    try (InputOutputDirs iod = TestUtils.get()) {
      String content = "<h1>Hello World</h1>\n this is a test\n\nthis\nis\n\n";
      write(iod.inputDir.resolve("content/index.html"), content.getBytes(StandardCharsets.UTF_8));
      Stampo stampo = new Stampo(iod.inputDir, iod.outputDir, Collections.emptyMap());
      stampo.build();

      Assert.assertEquals(content, TestUtils.fileOutputAsString(iod, "index.html"));
    }
  }

  @Test
  public void simpleContentWithMetadataAndPebbleTemplate() throws IOException {
    try (InputOutputDirs iod = TestUtils.get()) {
      String content = "---\n" + //
          "answer: 42\n" + //
          "---\n" + //
          "<h1>The answer is {{metadata.rawMap.answer}}</h1>";
      write(iod.inputDir.resolve("content/index.html.peb"), content.getBytes(StandardCharsets.UTF_8));
      Stampo stampo = new Stampo(iod.inputDir, iod.outputDir, Collections.emptyMap());
      stampo.build();

      Assert.assertEquals("<h1>The answer is 42</h1>", TestUtils.fileOutputAsString(iod, "index.html"));
    }
  }
  
  @Test
  public void simpleContentWithMetadataAndPebbleTemplateWindowFile() throws IOException {
    try (InputOutputDirs iod = TestUtils.get()) {
      String content = "---\r\n" + //
          "answer: 42\r\n" + //
          "---\r\n" + //
          "<h1>The answer is {{metadata.rawMap.answer}}</h1>\r\n";
      write(iod.inputDir.resolve("content/index.html.peb"), content.getBytes(StandardCharsets.UTF_8));
      Stampo stampo = new Stampo(iod.inputDir, iod.outputDir, Collections.emptyMap());
      stampo.build();

      Assert.assertEquals("<h1>The answer is 42</h1>\r\n", TestUtils.fileOutputAsString(iod, "index.html"));
    }
  }
  
  @Test
  public void simpleContentWithMetadataAndPebbleTemplateWithOverridePath() throws IOException {
    try (InputOutputDirs iod = TestUtils.get()) {
      String content = "---\n" + //
          "answer: 42\n" + //
          "override-output-to-path: index2.html\n" + //
          "---\n" + //
          "<h1>The answer is {{metadata.rawMap.answer}}</h1>";
      write(iod.inputDir.resolve("content/index.html.peb"), content.getBytes(StandardCharsets.UTF_8));
      Stampo stampo = new Stampo(iod.inputDir, iod.outputDir, Collections.emptyMap());
      stampo.build();

      Assert.assertFalse(Files.exists(iod.outputDir.resolve("index.html")));
      Assert.assertEquals("<h1>The answer is 42</h1>", TestUtils.fileOutputAsString(iod, "index2.html"));
    }
  }
  
  
  @Test
  public void simpleContentWithMetadataAndFreemarkerTemplate() throws IOException {
    try (InputOutputDirs iod = TestUtils.get()) {
      String content = "---\n" + //
          "answer: 42\n" + //
          "---\n" + //
          "<h1>The answer is ${metadata.rawMap['answer']}</h1>";
      write(iod.inputDir.resolve("content/index.html.ftl"), content.getBytes(StandardCharsets.UTF_8));
      Stampo stampo = new Stampo(iod.inputDir, iod.outputDir, Collections.emptyMap());
      stampo.build();

      Assert.assertEquals("<h1>The answer is 42</h1>", TestUtils.fileOutputAsString(iod, "index.html"));
    }
  }
}
