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

public class DraftTest {

  
  @Test
  public void draftEnabled() throws IOException {
    try (InputOutputDirs iod = TestUtils.get()) {
      write(iod.inputDir.resolve("content/index.html"),
          "---\ndraft: true\n---\n<h1>Hello World</h1>".getBytes(StandardCharsets.UTF_8));
      Stampo stampo = new Stampo(iod.inputDir, iod.outputDir, Collections.emptyMap());
      stampo.build();
      Assert.assertTrue(Files.exists(iod.outputDir.resolve("index.html")));
      Assert.assertEquals("<h1>Hello World</h1>", TestUtils.fileOutputAsString(iod, "index.html"));
      
      
      Stampo stampo2 = new Stampo(iod.inputDir, iod.outputDir, Collections.singletonMap("hide-draft", true));
      stampo2.build();
      
      Assert.assertFalse(Files.exists(iod.outputDir.resolve("index.html")));
      
    }
  }
}
