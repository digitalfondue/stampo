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

public class StaticPaginationTest {

  @Test
  public void paginationTest() throws IOException {
    try (InputOutputDirs iod = get()) {
      write(iod.inputDir.resolve("content/gallery.html.peb"),
          fromTestResource("pagination/gallery.html.peb"));

      Files.createDirectories(iod.inputDir.resolve("static/gallery"));

      for (int i = 0; i < 31; i++) {
        write(iod.inputDir.resolve("static/gallery/" + i + ".jpg"), new byte[] {});
      }
      
      Stampo stampo = new Stampo(iod.inputDir, iod.outputDir);
      stampo.build();
      
      Assert.assertEquals(fromTestResourceAsString("pagination/result/staticpagination/index.html"),
          fileOutputAsString(iod, "gallery/index.html"));
      Assert.assertEquals(fromTestResourceAsString("pagination/result/staticpagination/page2.html"),
          fileOutputAsString(iod, "gallery/page/2/index.html"));
      Assert.assertEquals(fromTestResourceAsString("pagination/result/staticpagination/page3.html"),
          fileOutputAsString(iod, "gallery/page/3/index.html"));
      Assert.assertEquals(fromTestResourceAsString("pagination/result/staticpagination/page4.html"),
          fileOutputAsString(iod, "gallery/page/4/index.html"));
      
      System.err.println(fileOutputAsString(iod, "gallery/page/4/index.html"));
    }
  }
}
