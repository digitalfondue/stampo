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

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class RootResource implements Resource, Directory {

  private final Path path;
  private final ResourceFactory resourceFactory;
  

  public RootResource(ResourceFactory resourceFactory, Path path) {
    this.path = path;
    this.resourceFactory = resourceFactory;
  }

  private static boolean mustBeIgnored(Path p, Set<String> pathMatchers) {
    FileSystem fs = p.getFileSystem();
    return pathMatchers.stream().anyMatch(m -> fs.getPathMatcher(m).matches(p.getFileName()));
  }

  private <T extends Resource> Map<String, T> fromDirectoryStream(Filter<Path> filter, Function<Path, T> mapper, Comparator<T> comparator) {
    
    //linkedhashmap as we want to preserve the insertion order
    try (DirectoryStream<Path> dirs = Files.newDirectoryStream(path, filter)) {
      return StreamSupport.stream(dirs.spliterator(), false)
            .map(mapper)
            .sorted(comparator)
            .collect(Collectors.toMap(Resource::getName, Function.identity(), (k, v) -> {throw new IllegalStateException("duplicate key " + k);}, LinkedHashMap::new));
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  public Map<String, FileResource> getFiles() {
    
    return fromDirectoryStream(
        (p) -> Files.isRegularFile(p) && !mustBeIgnored(p, resourceFactory.getConfiguration().getIgnorePatterns()), 
        p -> resourceFactory.fileResource(p, this), 
        resourceFactory.getFileResourceComparator());
  }

  public Map<String, Directory> getDirectories() {
    
    return fromDirectoryStream(Files::isDirectory, p -> resourceFactory.directory(p, this), Comparator.comparing(Directory::getName));
    
  }

  @Override
  public Path getPath() {
    return path;
  }

  @Override
  public Resource getParent() {
    return this;
  }
}
