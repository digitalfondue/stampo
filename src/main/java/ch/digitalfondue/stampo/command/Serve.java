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

import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import ch.digitalfondue.stampo.ServeAndWatch;
import ch.digitalfondue.stampo.Stampo;

public class Serve extends Command {
  
  public Serve() {
    super();
    portParam = optionParser.accepts("port").withRequiredArg().ofType(Integer.class).defaultsTo(8080);
    hostnameParam = optionParser.accepts("hostname").withRequiredArg().ofType(String.class).defaultsTo("localhost");
    disableRebuildOnChangeParam = optionParser.accepts("disable-rebuild-on-change").withRequiredArg().ofType(Boolean.class).defaultsTo(false);
    disableAutoReloadParam = optionParser.accepts("disable-auto-reload").withRequiredArg().ofType(Boolean.class).defaultsTo(false);
    blockingOnStartParam = optionParser.accepts("blocking-on-start").withRequiredArg().ofType(Boolean.class).defaultsTo(false);
  }
  
  private int port = 8080;
  private String hostname = "localhost";
  private boolean disableRebuildOnChange = false;
  private boolean disableAutoReload = false;
  private boolean blockingOnStart = false;
  
  //
  private final OptionSpec<Integer> portParam;
  private final OptionSpec<String> hostnameParam;
  private final OptionSpec<Boolean> disableRebuildOnChangeParam;
  private final OptionSpec<Boolean> disableAutoReloadParam;
  private final OptionSpec<Boolean> blockingOnStartParam;
  //
  
  @Override
  public void assign(OptionSet optionSet) {
    super.assign(optionSet);
    port = optionSet.valueOf(portParam);
    hostname = optionSet.valueOf(hostnameParam);
    disableRebuildOnChange = optionSet.valueOf(disableRebuildOnChangeParam);
    disableAutoReload = optionSet.valueOf(disableAutoReloadParam);
    blockingOnStart = optionSet.valueOf(blockingOnStartParam);
  }
  
  public void setPort(int port) {
    this.port = port;
  }
  
  public void setHostname(String hostname) {
    this.hostname = hostname;
  }
  
  public void setDisableRebuildOnChange(boolean flag) {
    disableRebuildOnChange = flag;
  }
  
  public void setDisableAutoReload(boolean flag) {
    disableAutoReload = flag;
  }
  
  
  public void setBlockingOnStart(boolean flag) {
	  blockingOnStart = flag;
  }

  @Override
  void runWithPaths(String inputPath, String outputPath) {
    Runnable triggerBuild = getBuildRunnable(inputPath, outputPath, getConfigurationOverride());
    triggerBuild.run();
    System.out.println("stampo serving at " + hostname + ":" + port);
    if (disableAutoReload) {
      System.out.println("auto-reload is disabled");
    }
    if (disableRebuildOnChange) {
      System.out.println("rebuild on change is disabled");
    }
    new ServeAndWatch(hostname, port, !disableRebuildOnChange, !disableAutoReload, 
        new Stampo(Paths.get(inputPath), Paths.get(outputPath), getConfigurationOverride()).getConfiguration(), triggerBuild, blockingOnStart)
        .start();
  }

  public int getPort() {
    return port;
  }

  public String getHostname() {
    return hostname;
  }

  public boolean isDisableRebuildOnChange() {
    return disableRebuildOnChange;
  }

  public boolean isDisableAutoReload() {
    return disableAutoReload;
  }
}