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
package ch.digitalfondue.stampo.resource;

import java.nio.file.Path;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

public interface Directory extends Resource {

  Map<String, FileResource> getFiles();

  Map<String, Directory> getDirectories();
  
  default boolean containAnyFiles() {
    return !getFiles().isEmpty() || getDirectories().values().stream().map(Directory::containAnyFiles).reduce(false, (l,r) -> l || r); 
  }
  
  default Optional<Directory> getDirectory(Path path) {
    
    Directory currentDir = this;
    Iterator<Path> it = path.iterator();
    while(it.hasNext() && currentDir != null) {
      currentDir = currentDir.getDirectories().get(it.next().toString());
    }
    
    return Optional.ofNullable(currentDir);
  }
}
