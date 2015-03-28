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

import org.junit.Assert;
import org.junit.Test;

import ch.digitalfondue.stampo.TestUtils.InputOutputDirs;

public class StampoMultiLocalesTest {


  @Test
  public void testMultipleLocales() throws IOException {
    try (InputOutputDirs iod = TestUtils.get()) {
      
      write(iod.inputDir.resolve("content/index.html.peb"),
          "<h1>Hello World {{locale}}</h1>".getBytes(StandardCharsets.UTF_8));
      
      Files.createDirectories(iod.inputDir.resolve("content/post"));
      
      write(iod.inputDir.resolve("content/post/first.html"),
          "<h1>First {{locale}}</h1>".getBytes(StandardCharsets.UTF_8));
      
      write(iod.inputDir.resolve("content/post/second.html"),
          "<h1>Second {{locale}}</h1>".getBytes(StandardCharsets.UTF_8));
      
      write(iod.inputDir.resolve("configuration.yaml"), "locales: ['en', 'de','fr']".getBytes(StandardCharsets.UTF_8));
      
      Stampo stampo = new Stampo(iod.inputDir, iod.outputDir);
      
      stampo.build();
      
      Assert.assertTrue(Files.exists(iod.outputDir.resolve("en/index.html")));
      Assert.assertTrue(Files.exists(iod.outputDir.resolve("de/index.html")));
      Assert.assertTrue(Files.exists(iod.outputDir.resolve("fr/index.html")));
      
      Assert.assertTrue(Files.exists(iod.outputDir.resolve("en/post/first/index.html")));
      Assert.assertTrue(Files.exists(iod.outputDir.resolve("de/post/first/index.html")));
      Assert.assertTrue(Files.exists(iod.outputDir.resolve("fr/post/first/index.html")));
      
      
      Assert.assertTrue(Files.exists(iod.outputDir.resolve("en/post/second/index.html")));
      Assert.assertTrue(Files.exists(iod.outputDir.resolve("de/post/second/index.html")));
      Assert.assertTrue(Files.exists(iod.outputDir.resolve("fr/post/second/index.html")));
    }
  }
}
