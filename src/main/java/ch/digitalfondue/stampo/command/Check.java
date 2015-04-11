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
package ch.digitalfondue.stampo.command;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import ch.digitalfondue.stampo.Stampo;

import com.beust.jcommander.Parameters;
import com.google.common.jimfs.Jimfs;

@Parameters(separators = "=")
public class Check extends Command {

  @Override
  void runWithWorkingPath(String workingPath) {
    try (FileSystem fs = Jimfs.newFileSystem()) {
      buildAndPrintResult(workingPath, fs);
    } catch (IOException ioe) {
      throw new IllegalStateException(ioe);
    }
  }

  private void buildAndPrintResult(String workingPath, FileSystem fs) {
    Path output = fs.getPath("output");
    Stampo stampo = new Stampo(Paths.get(workingPath), output);
    
    stampo.build((file, layout) -> {
      return String.format(
          "  from file: %s\n" + //
              "  selected rendering engine: %s\n" + //
              "  locale for resource: %s\n" + //
              "  selected layout path: %s\n" + //
              "  selected layout engine: %s\n" + //
              "  locale for layout: %s\n\n",//
          file.getPath(),
          file.getProcessorEngine(),//
          file.getLocale(), layout.getPath().map(Object::toString).orElse("no layout"),
          layout.getLayoutEngine(), layout.getLocale());
    }, (in, out) -> {
      try {
        Files.write(out, Arrays.asList("  from static input " + in), StandardCharsets.UTF_8);
      } catch (IOException ioe) {
        throw new IllegalStateException(ioe);
      }
    });

    System.out.println();
    System.out.println("The build will generate the following files:");
    System.out.println();
    try {
      Files.walk(output).forEach((p) -> {
        if (!Files.isDirectory(p)) {
          System.out.println("- " + output.relativize(p));
          try {
            Files.readAllLines(p, StandardCharsets.UTF_8).forEach(System.out::println);
          } catch (Exception e) {
            throw new IllegalStateException(e);
          }
        }
      });
      
      System.out.println();
      System.out.println("Everything seems ok!");
    } catch (IOException ioe) {
      throw new IllegalStateException(ioe);
    }
  }
}