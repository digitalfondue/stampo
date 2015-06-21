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

public class IncludeAllDirectiveMultiLangTest {

  private static List<String> files = Arrays.asList(//
      "configuration.yaml",//
      "doc/1-about.en.md",//
      "doc/1-about.fr.md",//
      "doc/2-installation.en.md",//
      "doc/3-configuration-and-administration.en.md",//

      "doc/4-user-manual.en.md",//

      "doc/4-user-manual/1-get-started.en.md",//
      "doc/4-user-manual/2-project.en.md",//

      "doc/4-user-manual/2-project/1-create-a-project.en.md",//
      "doc/4-user-manual/2-project/2-create-a-board.en.md",//
      "doc/4-user-manual/2-project/3-statistics.en.md",//

      "doc/4-user-manual/3-derpo/1-create-a-derpo.en.md",//
      "doc/4-user-manual/3-derpo/1-create-a-derpo.fr.md",//
      "doc/4-user-manual/3-derpo/2-create-a-derpo-board.en.md",//
      "doc/4-user-manual/3-derpo/3-derpo-statistics.en.md");
  


  private void addAllFiles(InputOutputDirs iod) throws IOException {

    Files.createDirectories(iod.inputDir.resolve("doc/4-user-manual/2-project/"));
    Files.createDirectories(iod.inputDir.resolve("doc/4-user-manual/3-derpo/"));

    for (String file : files) {
      write(iod.inputDir.resolve(file), fromTestResource("includeallmultilang/" + file));
    }
  }
  
  private static void check(String base, String name, InputOutputDirs iod) throws IOException {
    Assert.assertEquals(fromTestResourceAsString(base + name), fileOutputAsString(iod, name));
  }

  @Test
  public void depthMultiLangDoc() throws IOException {
    try (InputOutputDirs iod = get()) {
      Files.createDirectories(iod.inputDir.resolve("content"));
      write(iod.inputDir.resolve("content/index.fr.en.html.peb"),
          fromTestResource("includeallmultilang/content/index.fr.en.html.peb"));
      addAllFiles(iod);
      
      Stampo stampo = new Stampo(iod.inputDir, iod.outputDir, Collections.emptyMap());
      stampo.build();
      
      String base = "includeallmultilang/result/";
      
      //fr check
      check(base, "fr/index.html", iod);
      check(base, "fr/1-about/index.html", iod);
      check(base, "fr/4-user-manual/3-derpo/1-create-a-derpo/index.html", iod);
      
      //en check
      check(base, "en/index.html", iod);
      check(base, "en/1-about/index.html", iod);
      check(base, "en/2-installation/index.html", iod);
      check(base, "en/3-configuration-and-administration/index.html", iod);
      check(base, "en/4-user-manual/index.html", iod);
      check(base, "en/4-user-manual/1-get-started/index.html", iod);
      check(base, "en/4-user-manual/2-project/index.html", iod);
      check(base, "en/4-user-manual/3-derpo/1-create-a-derpo/index.html", iod);

    }
  }
}
