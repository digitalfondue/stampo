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
import static ch.digitalfondue.stampo.TestUtils.get;
import static java.nio.file.Files.write;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;

import org.junit.Assert;
import org.junit.Test;

import ch.digitalfondue.stampo.TestUtils.InputOutputDirs;

public class DataDirectoryTest {


  @Test
  public void testDataDir() throws IOException {
    try (InputOutputDirs iod = get()) {


      write(iod.inputDir.resolve("content/index.html.peb"), "{{data['test'].key}} {{data['test.test'].key}} {% for kv in data['test.test-multiple'] %}{{kv.key}} {% endfor %}".getBytes(StandardCharsets.UTF_8));
      
      
      Files.createDirectories(iod.inputDir.resolve("data"));
      
      
      write(iod.inputDir.resolve("data/test.yaml"), "key: value1".getBytes(StandardCharsets.UTF_8));
      
      Files.createDirectories(iod.inputDir.resolve("data/test/"));
      
      write(iod.inputDir.resolve("data/test/test.yaml"), "key: value2".getBytes(StandardCharsets.UTF_8));
      
      //multiple yaml object separated with "---"
      write(iod.inputDir.resolve("data/test/test-multiple.yaml"), "---\nkey: value3\n---\nkey: value4\n---".getBytes(StandardCharsets.UTF_8));
      
      Stampo stampo = new Stampo(iod.inputDir, iod.outputDir, Collections.emptyMap());
      stampo.build();
      
      Assert.assertEquals("value1 value2 value3 value4 ",
          fileOutputAsString(iod, "index.html"));
    }
  }
}
