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

import static java.util.Optional.ofNullable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import ch.digitalfondue.stampo.command.Build;
import ch.digitalfondue.stampo.command.Check;
import ch.digitalfondue.stampo.command.Help;
import ch.digitalfondue.stampo.command.Serve;

import com.beust.jcommander.JCommander;

public class StampoMain {


  public static void main(String[] args) {
    // disable logging
    LogManager.getLogManager().reset();
    //

    Logger.getLogger(Stampo.class.getName()).info("hello");

    Map<String, Runnable> commands = new HashMap<>();

    commands.put("serve", new Serve());
    commands.put("check", new Check());
    commands.put("build", new Build());
    commands.put("help", new Help());

    JCommander jc = new JCommander();
    commands.forEach((k, v) -> jc.addCommand(k, v));

    jc.parseWithoutValidation(args);

    ofNullable(jc.getParsedCommand()).map(commands::get).orElse(new Build(Arrays.asList(args)))
        .run();
  }
}
