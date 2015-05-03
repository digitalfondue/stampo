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
import static java.nio.file.Files.write;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.Assert;
import org.junit.Test;

import ch.digitalfondue.stampo.TestUtils.InputOutputDirs;

public class I18nTest {

  @Test
  public void testSingleLocale() throws IOException {
    try (InputOutputDirs iod = TestUtils.get()) {
      Files.createDirectories(iod.inputDir.resolve("locales"));

      write(iod.inputDir.resolve("locales/messages.yaml"),
          "hello: Hello world!".getBytes(StandardCharsets.UTF_8));
      write(iod.inputDir.resolve("locales/custom_messages.yaml"),
          "hello: Custom Hello world!".getBytes(StandardCharsets.UTF_8));

      write(iod.inputDir.resolve("content/index.html.peb"),
          "{{message('hello')}} : {{messageWithBundle('custom_messages', 'hello')}}"
              .getBytes(StandardCharsets.UTF_8));


      write(iod.inputDir.resolve("content/index2.html.ftl"),
          "${message('hello')}".getBytes(StandardCharsets.UTF_8));

      Stampo stampo = new Stampo(iod.inputDir, iod.outputDir);
      stampo.build();

      Assert.assertEquals("Hello world! : Custom Hello world!",
          fileOutputAsString(iod, "index.html"));

      Assert.assertEquals("Hello world!", fileOutputAsString(iod, "index2/index.html"));
    }
  }
}
