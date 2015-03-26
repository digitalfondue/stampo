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
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ch.digitalfondue.stampo.StampoGlobalConfiguration;

public class RootResource implements Resource, Directory {

  private final Path path;
  private final Map<String, Directory> directories;
  private final Map<String, FileResource> files;

  public RootResource(StampoGlobalConfiguration configuration, Path path, Comparator<FileResource> fileResourceComparator) {
    List<Directory> ds = new ArrayList<>();
    List<FileResource> fs = new ArrayList<>();

    this.path = path;
    try {
      Files.newDirectoryStream(path).forEach((p) -> {
        if (Files.isDirectory(p)) {
          ds.add(new DirectoryResource(configuration, p, this, fileResourceComparator));
        } else if (!mustBeIgnored(p, configuration.getIgnorePatterns())) {
          fs.add(new FileResourceWithMetadataSection(configuration, p, this));
        }
      });
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }

    fs.sort(fileResourceComparator);

    this.files = toMap(fs);
    //
    ds.sort(Comparator.comparing(Directory::getName));

    this.directories = toMap(ds);
  }

  private static boolean mustBeIgnored(Path p, Set<String> pathMatchers) {
    FileSystem fs = p.getFileSystem();
    return pathMatchers.stream().anyMatch(m -> fs.getPathMatcher(m).matches(p.getFileName()));
  }

  private static <T extends Resource> Map<String, T> toMap(List<T> l) {
    Map<String, T> m = new LinkedHashMap<>();
    for (T t : l) {
      m.put(t.getName(), t);
    }
    return Collections.unmodifiableMap(m);
  }

  public Map<String, FileResource> getFiles() {
    return files;
  }

  public Map<String, Directory> getDirectories() {
    return directories;
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
