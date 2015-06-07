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
package ch.digitalfondue.stampo.processor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import ch.digitalfondue.stampo.Stampo;
import ch.digitalfondue.stampo.TestUtils;
import ch.digitalfondue.stampo.TestUtils.InputOutputDirs;
import ch.digitalfondue.stampo.resource.RootResource;

public class ModelPreparerTest {


  /**
   * The static dir order the files using a "natural ordering" instead of the lexicographical order
   */
  @Test
  public void checkStaticFileOrder() throws IOException {
    try (InputOutputDirs iod = TestUtils.get()) {
      Path staticDir = iod.inputDir.resolve("static");
      Files.createDirectories(staticDir);

      List<String> fileNames = new ArrayList<>();
      for (int i = 0; i < 20; i++) {
        String name = "test" + i + ".txt";
        Files.createFile(staticDir.resolve(name));
        fileNames.add(name);
      }

      Stampo stampo = new Stampo(iod.inputDir, iod.outputDir, Collections.emptyMap());
      RootResource staticDirResource =
          ModelPreparer.staticResources(stampo.getConfiguration()).get();

      // check order
      List<String> filesFromStaticDir = new ArrayList<>(staticDirResource.getFiles().keySet());
      Assert.assertEquals(fileNames, filesFromStaticDir);
    }
  }
}
