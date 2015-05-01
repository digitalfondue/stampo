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

import org.junit.Assert;
import org.junit.Test;

import ch.digitalfondue.stampo.TestUtils.InputOutputDirs;
import ch.digitalfondue.stampo.exception.ConfigurationException;

public class StampoContentPaginationTest {

  @Test
  public void paginationTest() throws IOException {
    try (InputOutputDirs iod = get()) {
      write(iod.inputDir.resolve("content/index.html.peb"),
          fromTestResource("pagination/index.html.peb"));

      writePosts(iod);


      Stampo stampo = new Stampo(iod.inputDir, iod.outputDir);
      stampo.build();

      Assert.assertEquals(fromTestResourceAsString("pagination/result/pagination/index.html"),
          fileOutputAsString(iod, "index.html"));
      Assert.assertEquals(fromTestResourceAsString("pagination/result/pagination/page2.html"),
          fileOutputAsString(iod, "page/2/index.html"));
    }
  }

  private void writePosts(InputOutputDirs iod) throws IOException {
    Files.createDirectories(iod.inputDir.resolve("content/post"));
    Files.createDirectories(iod.inputDir.resolve("content/post/durpdurp"));

    for (int i = 1; i <= 20; i++) {
      write(iod.inputDir.resolve("content/post/post" + i + ".md"),
          fromTestResource("pagination/post/post" + i + ".md"));
    }

    for (int i = 21; i <= 31; i++) {
      write(iod.inputDir.resolve("content/post/durpdurp/post" + i + ".md"),
          fromTestResource("pagination/post/durpdurp/post" + i + ".md"));
    }
  }

  @Test
  public void recursiveTest() throws IOException {
    try (InputOutputDirs iod = get()) {
      write(iod.inputDir.resolve("content/index.html.peb"),
          fromTestResource("pagination/index-recursive.html.peb"));

      writePosts(iod);


      Stampo stampo = new Stampo(iod.inputDir, iod.outputDir);
      stampo.build();

      Assert.assertEquals(fromTestResourceAsString("pagination/result/recursive/index.html"),
          fileOutputAsString(iod, "index.html"));
      Assert.assertEquals(fromTestResourceAsString("pagination/result/recursive/page2.html"),
          fileOutputAsString(iod, "page/2/index.html"));
      Assert.assertEquals(fromTestResourceAsString("pagination/result/recursive/page7.html"),
          fileOutputAsString(iod, "page/7/index.html"));
    }
  }


  @Test
  public void emptyDirTest() throws IOException {
    try (InputOutputDirs iod = get()) {
      write(iod.inputDir.resolve("content/index.html.peb"),
          fromTestResource("pagination/index-recursive.html.peb"));
      Files.createDirectories(iod.inputDir.resolve("content/post"));
      Stampo stampo = new Stampo(iod.inputDir, iod.outputDir);
      stampo.build();

      Assert.assertEquals(fromTestResourceAsString("pagination/result/emptydir/index.html"),
          fileOutputAsString(iod, "index.html"));
    }
  }

  @Test(expected = ConfigurationException.class)
  public void paginationMissingDirToPaginateTest() throws IOException {
    try (InputOutputDirs iod = get()) {
      write(iod.inputDir.resolve("content/index.html"),
          fromTestResource("pagination/index-recursive.html.peb"));
      Stampo stampo = new Stampo(iod.inputDir, iod.outputDir);
      stampo.build();
    }
  }

}
