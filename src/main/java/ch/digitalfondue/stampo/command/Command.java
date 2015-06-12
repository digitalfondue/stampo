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
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import ch.digitalfondue.stampo.Stampo;
import ch.digitalfondue.stampo.exception.ConfigurationException;
import ch.digitalfondue.stampo.exception.LayoutException;
import ch.digitalfondue.stampo.exception.TemplateException;
import ch.digitalfondue.stampo.exception.YamlParserException;

public abstract class Command implements Opts {
  
  protected final OptionParser optionParser = new OptionParser();
  
  protected Optional<String> srcPath = Optional.empty();
  protected Optional<String> distPath = Optional.empty();
  
  protected boolean hideDraft = false;
  protected boolean printStackTrace = false;
  
  //
  private final OptionSpec<String> srcParam;
  private final OptionSpec<String> distParam;
  private final OptionSpec<Boolean> hideDraftParam;
  private final OptionSpec<Boolean> debugParam;
  
  Command() {
    srcParam = optionParser.accepts("src").withRequiredArg().ofType(String.class);
    distParam = optionParser.accepts("dist").withRequiredArg().ofType(String.class);
    hideDraftParam = optionParser.accepts("hide-draft").withRequiredArg().ofType(Boolean.class).defaultsTo(false);
    debugParam = optionParser.accepts("debug").withRequiredArg().ofType(Boolean.class).defaultsTo(false);
  }
  
  @Override
  public void assign(OptionSet optionSet) {
    
    if(optionSet.hasArgument(srcParam)) {
      setSrcPath(optionSet.valueOf(srcParam));
    }
    
    if(optionSet.hasArgument(distParam)) {
      setDistPath(optionSet.valueOf(distParam));
    }
    
    hideDraft = optionSet.valueOf(hideDraftParam);
    printStackTrace = optionSet.valueOf(debugParam);
  }
  
  public void setSrcPath(String path) {
    srcPath = Optional.of(path);
  }
  
  
  
  public void setDistPath(String path) {
    distPath = Optional.of(path);
  }
  
  
  
  public void setHideDraft(boolean hideDraft) {
    this.hideDraft = hideDraft;
  }

  

  private String inputPath() {
    return Paths.get(this.srcPath.orElse("."))
        .toAbsolutePath().normalize().toString();
  }
  
  private String outputPath(String inputPath) {
    return Paths.get(this.distPath.orElse(inputPath.concat("/output")))
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
    } catch (YamlParserException | TemplateException | LayoutException| ConfigurationException e) {
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

  public Optional<String> getSrcPath() {
    return srcPath;
  }
  
  public Optional<String> getDistPath() {
    return distPath;
  }

  public boolean isPrintStackTrace() {
    return printStackTrace;
  }
  
  @Override
  public OptionParser getOptionParser() {
    return optionParser;
  }

  public boolean isHideDraft() {
    return hideDraft;
  }
}