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

import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.common.io.Resources;
import com.google.common.jimfs.Jimfs;

public class TestUtils {


  public static class InputOutputDirs implements Closeable {
    public final FileSystem fileSystem;
    public final Path inputDir;
    public final Path outputDir;

    public InputOutputDirs(FileSystem fileSystem, Path inputDir, Path outputDir) {
      this.fileSystem = fileSystem;
      this.inputDir = inputDir;
      this.outputDir = outputDir;
    }

    @Override
    public void close() throws IOException {
      fileSystem.close();
    }
  }


  public static InputOutputDirs get() throws IOException {

    FileSystem fs = Jimfs.newFileSystem("base");
    Path baseInputDir = fs.getPath("input");
    Path outputDir = fs.getPath("output");
    Files.createDirectories(baseInputDir.resolve("content"));

    return new InputOutputDirs(fs, baseInputDir, outputDir);
  }

  public static String fileOutputAsString(InputOutputDirs iod, String path) throws IOException {
    return new String(Files.readAllBytes(iod.outputDir.resolve(path)), StandardCharsets.UTF_8);
  }

  public static byte[] fromTestResource(String name) {
    try {
      return Resources.toByteArray(Resources.getResource(name));
    } catch (IOException e) {
      throw new IllegalArgumentException(e);
    }
  }

  public static String fromTestResourceAsString(String name) {
    return new String(fromTestResource(name), StandardCharsets.UTF_8);
  }

}
