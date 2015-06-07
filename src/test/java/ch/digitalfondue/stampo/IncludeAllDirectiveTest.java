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
import static ch.digitalfondue.stampo.TestUtils.fromTestResource;
import static ch.digitalfondue.stampo.TestUtils.fromTestResourceAsString;
import static ch.digitalfondue.stampo.TestUtils.get;
import static java.nio.file.Files.write;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import ch.digitalfondue.stampo.TestUtils.InputOutputDirs;

public class IncludeAllDirectiveTest {

  private static List<String> files = Arrays.asList(//
      "doc/01-first-page.md",//
      "doc/02-second-page.md",//
      "doc/03-third-page.md",//
      "doc/01-first-page/01-01.md",//
      "doc/01-first-page/01-02.md",//
      "doc/01-first-page/01-02/01-02-01.md",//
      "doc/02-second-page/02-01.md");

  private void addAllFiles(InputOutputDirs iod) throws IOException {

    Files.createDirectories(iod.inputDir.resolve("doc/01-first-page/01-02/"));
    Files.createDirectories(iod.inputDir.resolve("doc/02-second-page/"));

    for (String file : files) {
      write(iod.inputDir.resolve(file), fromTestResource("includeall/" + file));
    }
  }

  private static void check(String base, String name, InputOutputDirs iod) throws IOException {
    Assert.assertEquals(fromTestResourceAsString(base + name), fileOutputAsString(iod, name));
  }

  @Test
  public void depth2Test() throws IOException {
    try (InputOutputDirs iod = get()) {

      Files.createDirectories(iod.inputDir.resolve("content"));
      write(iod.inputDir.resolve("content/index.html.peb"),
          fromTestResource("includeall/content/default-index-depth-2/index.html.peb"));
      addAllFiles(iod);

      Stampo stampo = new Stampo(iod.inputDir, iod.outputDir, Collections.emptyMap());
      stampo.build();

      String base = "includeall/result/default-index-depth-2/";
      check(base, "index.html", iod);

      check(base, "01-first-page/index.html", iod);
      check(base, "01-first-page/01-01/index.html", iod);
      check(base, "01-first-page/01-02/index.html", iod);
      check(base, "02-second-page/index.html", iod);
      check(base, "02-second-page/02-01/index.html", iod);
      check(base, "03-third-page/index.html", iod);
    }
  }

  @Test
  public void singleTest() throws IOException {
    try (InputOutputDirs iod = get()) {

      Files.createDirectories(iod.inputDir.resolve("content"));
      write(iod.inputDir.resolve("content/index.html.peb"),
          fromTestResource("includeall/content/single/single.html.peb"));
      addAllFiles(iod);
      
      Stampo stampo = new Stampo(iod.inputDir, iod.outputDir, Collections.emptyMap());
      stampo.build();
      
      String base = "includeall/result/single/";
      check(base, "index.html", iod);
    }
  }
}
