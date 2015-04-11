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

import com.beust.jcommander.Parameters;

@Parameters(separators = "=")
public class Help implements Runnable {

  @Override
  public void run() {
    System.out.println();
    System.out.println("Usage:");
    System.out.println("  stampo [command] <source>");
    System.out.println();
    System.out.println("Available Commands:");
    System.out.println("  build                 Build the site, use the current working\n"
                     + "                        directory if not specified");
    System.out.println("  serve                 Build and serve the site, as a default, it will\n"
                     + "                        listen to localhost:8080");
    System.out.println("  check                 Check if the site build correctly");
    System.out.println("  help                  This help");
  }
  
}