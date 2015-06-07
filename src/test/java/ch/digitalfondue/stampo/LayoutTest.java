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

import static ch.digitalfondue.stampo.TestUtils.fileOutputAsString;
import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.write;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;

import org.junit.Assert;
import org.junit.Test;

import ch.digitalfondue.stampo.TestUtils.InputOutputDirs;

public class LayoutTest {


  @Test
  public void testSimplePebbleLayout() throws IOException {
    try (InputOutputDirs iod = TestUtils.get()) {
      Files.createDirectories(iod.inputDir.resolve("layout/"));
      Files.createDirectories(iod.inputDir.resolve("content/"));
      
      write(iod.inputDir.resolve("layout/index.html.peb"),
          "layout here {% block content %}{{content|raw}}{%endblock %} layout here"
              .getBytes(StandardCharsets.UTF_8));
      write(iod.inputDir.resolve("content/index.md"),
          "*content as markdown*".getBytes(StandardCharsets.UTF_8));

      Stampo stampo = new Stampo(iod.inputDir, iod.outputDir, Collections.emptyMap());
      stampo.build();
      
      Assert.assertEquals("layout here <p><em>content as markdown</em></p> layout here",
          fileOutputAsString(iod, "index.html"));
    }
  }

  @Test
  public void testSimpleFreemarkerLayout() throws IOException {
    try (InputOutputDirs iod = TestUtils.get()) {
      createDirectories(iod.inputDir.resolve("layout"));
      write(iod.inputDir.resolve("layout/index.html.ftl"),
          "layout here ${content} layout here".getBytes(StandardCharsets.UTF_8));
      write(iod.inputDir.resolve("content/index.md"),
          "*content as markdown*".getBytes(StandardCharsets.UTF_8));
      Stampo stampo = new Stampo(iod.inputDir, iod.outputDir, Collections.emptyMap());
      stampo.build();
      
      Assert.assertEquals("layout here <p><em>content as markdown</em></p> layout here",
          fileOutputAsString(iod, "index.html"));
    }
  }
}
