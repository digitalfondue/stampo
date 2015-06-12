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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.LogManager;
import java.util.stream.Collectors;

import joptsimple.OptionSet;
import ch.digitalfondue.stampo.command.Build;
import ch.digitalfondue.stampo.command.Check;
import ch.digitalfondue.stampo.command.Help;
import ch.digitalfondue.stampo.command.New;
import ch.digitalfondue.stampo.command.Opts;
import ch.digitalfondue.stampo.command.Serve;

public class StampoMain {
  
  static Runnable fromParameters(String[] args) {
    Map<String, Opts> commands = new HashMap<>();

    commands.put("serve", new Serve());
    commands.put("check", new Check());
    commands.put("build", new Build());
    commands.put("help", new Help());
    commands.put("new", new New());
    
    
    List<String> params = new ArrayList<>(args.length == 0 ? Arrays.asList("build") : Arrays.asList(args));
    params.stream().findFirst().ifPresent(command -> {
      if(!commands.containsKey(command)) {
        params.add(0, "build");
      }
    });
    
    Opts command = commands.get(params.get(0));
    
    OptionSet parsed = command.getOptionParser().parse(params.stream().skip(1).collect(Collectors.toList()).toArray(new String[] {}));
    command.assign(parsed);
    return command;
  }


  public static void main(String[] args) {
    // disable logging
    LogManager.getLogManager().reset();
    //
    fromParameters(args).run();
  }
}
