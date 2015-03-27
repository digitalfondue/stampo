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
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

  private static <T extends Resource> Map<String, T> toMap(List<T> l) {
    Map<String, T> m = new LinkedHashMap<>();//we care about insertion order
    for (T t : l) {
      m.put(t.getName(), t);
    }
    return Collections.unmodifiableMap(m);
  }

  public Map<String, FileResource> getFiles() {
    try (DirectoryStream<Path> dirs = Files.newDirectoryStream(path, (p) -> Files.isRegularFile(p) && !mustBeIgnored(p, resourceFactory.getConfiguration().getIgnorePatterns()))) {
      
      List<FileResource> fs = StreamSupport.stream(dirs.spliterator(), false)
           .map(p -> resourceFactory.fileResource(p, this))
           .sorted(resourceFactory.getFileResourceComparator())
           .collect(Collectors.toList());
      
      return toMap(fs);
      
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  public Map<String, Directory> getDirectories() {
    try (DirectoryStream<Path> dirs = Files.newDirectoryStream(path, Files::isDirectory)) {
      
      List<Directory> ds = StreamSupport.stream(dirs.spliterator(), false)
          .map(p -> resourceFactory.directory(p, this))
          .sorted(Comparator.comparing(Directory::getName))
          .collect(Collectors.toList());
      
      return toMap(ds);
      
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
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
