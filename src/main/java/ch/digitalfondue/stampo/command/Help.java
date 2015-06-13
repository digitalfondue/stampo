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

import java.util.List;

import joptsimple.OptionParser;
import joptsimple.OptionSet;

public class Help implements Opts {

  
  private enum CommandName {
    BUILD {
      @Override
      void printHelp() {
        System.out.println("Build the site");
        System.out.println("Usage:");
        System.out.println("  stampo build [options]");
        System.out.println();
        System.out.println("Options");
        printCommonOptions();
      }
    }, 
    SERVE {
      @Override
      void printHelp() {
        System.out.println("Launch a webserver and trigger a build");
        System.out.println("Usage:");
        System.out.println("  stampo serve [options]");
        System.out.println();
        System.out.println("Options");
        printCommonOptions();
        System.out.println("  --port=[portnumber]      Port number, default 8080");
        System.out.println("  --hostname=[hostname]    Hostname, default localhost");
        System.out.println();
        System.out.println("  --disable-rebuild-on-change=true/false    Rebuild on change, default false");
        System.out.println("  --disable-auto-reload=true/false          Disable autoreload, default false");
      }
    }, 
    CHECK {
      @Override
      void printHelp() {
        System.out.println("Check if the website build correctly");
        System.out.println("Usage:");
        System.out.println("  stampo check [options]");
        System.out.println();
        System.out.println("Options");
        printCommonOptions();
      }
    },
    NEW {
        @Override
        void printHelp() {
          System.out.println("Create a basic website, based on existing templates");
          System.out.println("Usage:");
          System.out.println("  stampo new [options]");
          System.out.println();
          System.out.println("Options");
          System.out.println("  --dest=[dest-path]      Destination of the template, mandatory if --name is not set");
          System.out.println("  --name=[project-name]]  Extract the template in the current directory,\n"
                           + "                          under the folder [project-name].\n"
                           + "                          Mandatory, ignored if --dest is set.");
          System.out.println();
          System.out.println("  --archetype=[basic|site|blog|doc]      Define the template to extract. default basic");
        }
      },
    HELP {
      @Override
      void printHelp() {
        System.out.println();
        System.out.println("Usage:");
        System.out.println("  stampo [command] [options]");
        System.out.println();
        System.out.println("Available Commands:");
        System.out.println("  build                    Build the site, use the current working\n"
                         + "                           directory if not specified");
        System.out.println("  serve                    Build and serve the site, as a default, it will\n"
                         + "                           listen to localhost:8080");
        System.out.println("  check                    Check if the site build correctly");
        System.out.println("  new                      Create a new stampo site using a predefined archetype");
        System.out.println("  help                     This help");
        System.out.println();
        System.out.println();
        System.out.println("Global Options");
        printCommonOptions();
        System.out.println();
        System.out.println("Use \"stampo help [command]\" for a detailed information about the command");
      }
    };

    void printHelp() {}
  }
  private final OptionParser optionParser = new OptionParser();
  
  private CommandName selected = CommandName.HELP;
  
  
  public Help() {
  }

  @Override
  public void run() {
    selected.printHelp();
  }

  @Override
  public OptionParser getOptionParser() {
    return optionParser;
  }

  @Override
  public void assign(OptionSet optionSet) {
    List<?> nonOpts= optionSet.nonOptionArguments();
    if (nonOpts.contains("build")) {
      selected = CommandName.BUILD;
    } else if (nonOpts.contains("serve")) {
      selected = CommandName.SERVE;
    } else if (nonOpts.contains("check")) {
      selected = CommandName.CHECK;
    } else if (nonOpts.contains("new")) {
      selected = CommandName.NEW;
    } else {
      selected = CommandName.HELP;
    }
  }
  
  private static void printCommonOptions() {
    System.out.println();
    System.out.println("  --src=[path]             Path of the source directory");
    System.out.println("  --dist=[path]            Path of the output directory \n"
        + "                           /!\\ BEWARE: it will cleanup the content /!\\");
    System.out.println("  --hide-draft=true/false  Hide draft files (default value is false)");
    System.out.println("  --debug=true/false       Will show the stacktraces on error");
    System.out.println();
  }
  
}