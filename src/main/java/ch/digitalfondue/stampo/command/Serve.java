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

import ch.digitalfondue.stampo.ServeAndWatch;
import ch.digitalfondue.stampo.Stampo;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

@Parameters(separators = "=")
public class Serve extends Command {
  @Parameter(description = "port", names = "--port")
  private int port = 8080;
  
  public void setPort(int port) {
    this.port = port;
  }

  @Parameter(description = "hostname", names = "--hostname")
  private String hostname = "localhost";
  
  public void setHostname(String hostname) {
    this.hostname = hostname;
  }

  @Parameter(description = "disable rebuild on change", names = "--disable-rebuild-on-change")
  private boolean disableRebuildOnChange = false;
  
  public void setDisableRebuildOnChange(boolean flag) {
    disableRebuildOnChange = flag;
  }
  
  @Parameter(description = "disable auto reload", names = "--disable-auto-reload")
  private boolean disableAutoReload = false;
  
  public void setDisableAutoReload(boolean flag) {
    disableAutoReload = flag;
  }
  
  @Parameter(description = "block thread on start", names = "--blocking-on-start")
  private boolean blockingOnStart = false;
  
  public void setBlockingOnStart(boolean flag) {
	  blockingOnStart = flag;
  }

  @Override
  void runWithPaths(String inputPath, String outputPath) {
    Runnable triggerBuild = getBuildRunnable(inputPath, outputPath);
    triggerBuild.run();
    System.out.println("stampo serving at " + hostname + ":" + port);
    if (disableAutoReload) {
      System.out.println("auto-reload is disabled");
    }
    if (disableRebuildOnChange) {
      System.out.println("rebuild on change is disabled");
    }
    new ServeAndWatch(hostname, port, !disableRebuildOnChange, !disableAutoReload, 
        new Stampo(Paths.get(inputPath), Paths.get(outputPath)).getConfiguration(), triggerBuild, blockingOnStart)
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