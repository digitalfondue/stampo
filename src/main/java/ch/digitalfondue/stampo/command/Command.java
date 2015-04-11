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

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import ch.digitalfondue.stampo.Stampo;
import ch.digitalfondue.stampo.exception.LayoutException;
import ch.digitalfondue.stampo.exception.MissingDirectoryException;
import ch.digitalfondue.stampo.exception.TemplateException;
import ch.digitalfondue.stampo.exception.YamlParserException;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

@Parameters(separators = "=")
public abstract class Command implements Runnable {
  
  @Parameter(description = "path", arity = 1)
  protected List<String> path = new ArrayList<>();

  @Parameter(description = "print stack trace", names = "--debug")
  protected boolean printStackTrace = false;

  private String workingPath() {
    return Paths.get(this.path.stream().findFirst().orElse("."))
        .toAbsolutePath().normalize().toString();
  }

  @Override
  public void run() {
    String workingPath = workingPath();
    System.out.println("stampo working path is " + workingPath);
    try {
      runWithWorkingPath(workingPath);
    } catch (MissingDirectoryException | YamlParserException | TemplateException
        | LayoutException e) {
      System.err.println(e.getMessage());
      if (printStackTrace) {
        Optional.ofNullable(e.getCause()).ifPresent(Throwable::printStackTrace);
      }
      System.exit(1);
    }
  }

  abstract void runWithWorkingPath(String workingPath);
  
  
  static Runnable getBuildRunnable(String workingPath) {
    return () -> {
      long start = System.currentTimeMillis();
      Stampo s = new Stampo(workingPath);
      s.build();
      long end = System.currentTimeMillis();
      System.out.println("built in " + (end - start) + "ms, output in "
          + s.getConfiguration().getBaseOutputDir());
    };
  }

  public List<String> getPath() {
    return path;
  }

  public boolean isPrintStackTrace() {
    return printStackTrace;
  }
}