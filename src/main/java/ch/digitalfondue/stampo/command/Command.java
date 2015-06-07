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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import ch.digitalfondue.stampo.Stampo;
import ch.digitalfondue.stampo.exception.ConfigurationException;
import ch.digitalfondue.stampo.exception.LayoutException;
import ch.digitalfondue.stampo.exception.MissingDirectoryException;
import ch.digitalfondue.stampo.exception.TemplateException;
import ch.digitalfondue.stampo.exception.YamlParserException;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

@Parameters(separators = "=")
public abstract class Command implements Runnable {
  
  @Parameter(description = "src path", arity = 1)
  protected List<String> srcPath = new ArrayList<>();
  
  public void setSrcPath(String path) {
    if(srcPath.size() == 0) {
      srcPath.add(path);
    }
  }
  
  @Parameter(description = "dist path", names = "--dist")
  protected List<String> distPath = new ArrayList<>();
  
  public void setDistPath(String path) {
    if(distPath.size() == 0) {
      distPath.add(path);
    }
  }
  
  @Parameter(description = "hide draft", names = "--hide-draft")
  protected boolean hideDraft = false;
  
  public void setHideDraft(boolean hideDraft) {
    this.hideDraft = hideDraft;
  }

  @Parameter(description = "print stack trace", names = "--debug")
  protected boolean printStackTrace = false;

  private String inputPath() {
    return Paths.get(this.srcPath.stream().findFirst().orElse("."))
        .toAbsolutePath().normalize().toString();
  }
  
  private String outputPath(String inputPath) {
    return Paths.get(this.distPath.stream().findFirst().orElse(inputPath.concat("/output")))
        .toAbsolutePath().normalize().toString();
  }

  @Override
  public void run() {
    String inputPath = inputPath();
    System.out.println("stampo working path is " + inputPath);
    String outputPath = outputPath(inputPath);
    System.out.println("stampo destination path is " + outputPath);
    try {
      runWithPaths(inputPath, outputPath);
    } catch (MissingDirectoryException | YamlParserException | TemplateException
        | LayoutException| ConfigurationException e) {
      System.err.println(e.getMessage());
      if (printStackTrace) {
        Optional.ofNullable(e.getCause()).ifPresent(Throwable::printStackTrace);
      }
      System.exit(1);
    }
  }
  
  public Map<String, Object> getConfigurationOverride() {
    Map<String, Object> conf = new HashMap<>();
    conf.put("hide-draft", hideDraft);
    return conf;
  }

  abstract void runWithPaths(String inputPath, String outputhPath);
  
  
  static Runnable getBuildRunnable(String inputPath, String outputPath, Map<String, Object> configurationOverride) {
    return () -> {
      long start = System.currentTimeMillis();
      Stampo s = new Stampo(Paths.get(inputPath), Paths.get(outputPath), configurationOverride);
      s.build();
      long end = System.currentTimeMillis();
      System.out.println("built in " + (end - start) + "ms, output in "
          + s.getConfiguration().getBaseOutputDir());
    };
  }

  public List<String> getSrcPath() {
    return srcPath;
  }
  
  public List<String> getDistPath() {
    return distPath;
  }

  public boolean isPrintStackTrace() {
    return printStackTrace;
  }
}