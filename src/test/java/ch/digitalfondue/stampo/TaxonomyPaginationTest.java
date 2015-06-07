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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;

import org.junit.Assert;
import org.junit.Test;

import ch.digitalfondue.stampo.TestUtils.InputOutputDirs;

public class TaxonomyPaginationTest {


  @Test
  public void testTaxonomyTags() throws IOException {

    try (InputOutputDirs iod = get()) {
      write(iod.inputDir.resolve("content/tags.html.peb"),
          fromTestResource("pagination/tags.html.peb"));

      write(iod.inputDir.resolve("configuration.yaml"), "taxonomies: [tags, authors]".getBytes(StandardCharsets.UTF_8));

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


      Stampo stampo = new Stampo(iod.inputDir, iod.outputDir, Collections.emptyMap());
      stampo.build();
      
      Assert.assertEquals(fromTestResourceAsString("pagination/result/taxonomy/tags-index.html"),
          fileOutputAsString(iod, "tags/test/index.html"));
      Assert.assertEquals(fromTestResourceAsString("pagination/result/taxonomy/tags-page2.html"),
          fileOutputAsString(iod, "tags/test/page/2/index.html"));
      Assert.assertEquals(fromTestResourceAsString("pagination/result/taxonomy/hack-index.html"),
          fileOutputAsString(iod, "tags/hack/index.html"));
    }
  }
}
