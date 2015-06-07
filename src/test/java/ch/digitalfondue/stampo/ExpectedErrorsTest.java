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

import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.write;
import static java.util.Arrays.asList;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.Collections;

import org.junit.Test;

import ch.digitalfondue.stampo.TestUtils.InputOutputDirs;
import ch.digitalfondue.stampo.exception.LayoutException;
import ch.digitalfondue.stampo.exception.MissingDirectoryException;
import ch.digitalfondue.stampo.exception.TemplateException;
import ch.digitalfondue.stampo.exception.YamlParserException;

import com.google.common.jimfs.Jimfs;

public class ExpectedErrorsTest {

  @Test(expected = MissingDirectoryException.class)
  public void testMissingContentDirectoryException() throws IOException {
    try (FileSystem fs = Jimfs.newFileSystem("base")) {
      Path baseInputDir = fs.getPath("input");
      Path outputDir = fs.getPath("output");

      createDirectories(baseInputDir);
      createDirectories(outputDir);

      Stampo stampo = new Stampo(baseInputDir, outputDir, Collections.emptyMap());
      stampo.build();
    }
  }


  @Test(expected = YamlParserException.class)
  public void testYamlParserException() throws IOException {
    try (InputOutputDirs iod = TestUtils.get()) {
      write(iod.inputDir.resolve("configuration.yaml"), asList("wrong=yamlformat"),
          StandardCharsets.UTF_8);

      Stampo stampo = new Stampo(iod.inputDir, iod.outputDir, Collections.emptyMap());
      stampo.build();
    }
  }

  @Test(expected = LayoutException.class)
  public void testPebbleLayoutException() throws IOException {
    try (InputOutputDirs iod = TestUtils.get()) {
      createDirectories(iod.inputDir.resolve("layout"));

      // error in template layout: unclosed output expression
      write(iod.inputDir.resolve("layout/index.html.peb"), asList("{{content | raw"),
          StandardCharsets.UTF_8);
      write(iod.inputDir.resolve("content/index.md"), asList("*content as markdown*"),
          StandardCharsets.UTF_8);
      Stampo stampo = new Stampo(iod.inputDir, iod.outputDir, Collections.emptyMap());

      stampo.build();
    }
  }

  @Test(expected = LayoutException.class)
  public void testFreemarkerLayoutException() throws IOException {
    try (InputOutputDirs iod = TestUtils.get()) {
      createDirectories(iod.inputDir.resolve("layout"));
      // error in template layout: unclosed output expression
      write(iod.inputDir.resolve("layout/index.html.ftl"), asList("${content"),
          StandardCharsets.UTF_8);
      write(iod.inputDir.resolve("content/index.md"), asList("*content as markdown*"),
          StandardCharsets.UTF_8);
      Stampo stampo = new Stampo(iod.inputDir, iod.outputDir, Collections.emptyMap());

      stampo.build();
    }
  }


  @Test(expected = TemplateException.class)
  public void testPebbleTemplateException() throws IOException {
    try (InputOutputDirs iod = TestUtils.get()) {
      // error in template: unclosed output expression
      write(iod.inputDir.resolve("content/index.html.peb"), asList("{{metadata"),
          StandardCharsets.UTF_8);
      Stampo stampo = new Stampo(iod.inputDir, iod.outputDir, Collections.emptyMap());

      stampo.build();
    }
  }

  @Test(expected = TemplateException.class)
  public void testFreemarkerTemplateException() throws IOException {
    try (InputOutputDirs iod = TestUtils.get()) {
      // error in template: unclosed output expression
      write(iod.inputDir.resolve("content/index.html.ftl"), asList("${metadata"),
          StandardCharsets.UTF_8);
      Stampo stampo = new Stampo(iod.inputDir, iod.outputDir, Collections.emptyMap());

      stampo.build();
    }
  }
}
