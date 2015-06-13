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

import static java.nio.file.Files.delete;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.isDirectory;
import static java.nio.file.Files.walkFileTree;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import org.junit.Assert;
import org.junit.Test;

import ch.digitalfondue.stampo.command.New;

public class NewCommandTest {

  private static String[] of(String... strings) {
    return strings;
  }

  private void cleanUp(Path path) {
    if (exists(path) && isDirectory(path)) {
      try {
        walkFileTree(path, new SimpleFileVisitor<Path>() {
          @Override
          public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            delete(file);
            return FileVisitResult.CONTINUE;
          }

          @Override
          public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            delete(dir);
            return FileVisitResult.CONTINUE;
          }
        });
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }
  }

  @Test
  public void callNewMissingArguments() {
    Runnable r = StampoMain.fromParameters(of("new"));
    Assert.assertTrue(r instanceof New);
    New n = (New) r;
    Assert.assertFalse(n.getPath().isPresent());
    Assert.assertFalse(n.getName().isPresent());
  }

  @Test
  public void callNewWithProjectName() {
    Runnable r = StampoMain.fromParameters(of("new", "--name=test"));
    Assert.assertTrue(r instanceof New);
    New n = (New) r;
    Assert.assertFalse(n.getPath().isPresent());
    Assert.assertTrue(n.getName().isPresent());

    try {
      n.run();

      Assert.assertTrue(Paths.get("./test/test.txt").toFile().exists());
      Assert.assertTrue(Paths.get("./test/child").toFile().exists());
      Assert.assertTrue(Paths.get("./test/child").toFile().isDirectory());
      Assert.assertTrue(Paths.get("./test/child/test-child.txt").toFile().exists());
    } finally {
      cleanUp(Paths.get("./test"));
    }
  }

  @Test
  public void callNewWithPath() {
    Runnable r = StampoMain.fromParameters(of("new", "--dest=./stampo-path"));
    Assert.assertTrue(r instanceof New);
    New n = (New) r;
    Assert.assertTrue(n.getPath().isPresent());
    Assert.assertFalse(n.getName().isPresent());

    try {
      n.run();

      Assert.assertTrue(Paths.get("./stampo-path/test.txt").toFile().exists());
      Assert.assertTrue(Paths.get("./stampo-path/child").toFile().exists());
      Assert.assertTrue(Paths.get("./stampo-path/child").toFile().isDirectory());
      Assert.assertTrue(Paths.get("./stampo-path/child/test-child.txt").toFile().exists());
    } finally {
      cleanUp(Paths.get("./stampo-path"));
    }
  }

  @Test
  public void callNewWithPathAndArchetype() {
    Runnable r = StampoMain.fromParameters(of("new", "--dest=./stampo-path", "--archetype=site"));
    Assert.assertTrue(r instanceof New);
    New n = (New) r;
    Assert.assertTrue(n.getPath().isPresent());
    Assert.assertFalse(n.getName().isPresent());

    try {
      n.run();

      Assert.assertTrue(Paths.get("./stampo-path/test.txt").toFile().exists());
      Assert.assertTrue(Paths.get("./stampo-path/child").toFile().exists());
      Assert.assertTrue(Paths.get("./stampo-path/child").toFile().isDirectory());
      Assert.assertTrue(Paths.get("./stampo-path/child/test-child.txt").toFile().exists());
      Assert.assertTrue(Paths.get("./stampo-path/site").toFile().exists());
      Assert.assertTrue(Paths.get("./stampo-path/site").toFile().isDirectory());
      Assert.assertTrue(Paths.get("./stampo-path/site/site-test.txt").toFile().exists());
    } finally {
      cleanUp(Paths.get("./stampo-path"));
    }
  }

  @Test
  public void callNewWithPathAndWrongArchetype() {
    Runnable r =
        StampoMain.fromParameters(of("new", "--dest=./stampo-path", "--archetype=wrong-value"));
    Assert.assertTrue(r instanceof New);
    New n = (New) r;
    Assert.assertTrue(n.getPath().isPresent());
    Assert.assertFalse(n.getName().isPresent());

    ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    System.setErr(new PrintStream(outContent));

    n.run();
    Assert.assertEquals("invalid archetype, specify one of: [basic, site, blog, doc]", outContent
        .toString().replaceAll("[\r\n]", ""));
  }

}
