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
import java.util.Collections;
import java.util.Optional;

import ch.digitalfondue.stampo.StampoGlobalConfiguration;

public class StaticFileResource implements FileResource {
  
  private final StampoGlobalConfiguration configuration;
  private final Path path;
  private final Resource parent;
  private final StructuredFileExtension structuredFileExtension;

  public StaticFileResource(StampoGlobalConfiguration configuration, Path path, Resource parent) {
    this.configuration = configuration;
    this.path = path;
    this.parent = parent;
    this.structuredFileExtension = new StructuredFileExtension(Collections.emptyList(), Optional.empty(), Optional.empty(), Collections.emptySet(), getFileExtensions());
  }

  @Override
  public Resource getParent() {
    return parent;
  }

  @Override
  public Path getPath() {
    return path;
  }

  @Override
  public StampoGlobalConfiguration getConfiguration() {
    return configuration;
  }

  @Override
  public FileMetadata getMetadata() {
    return new FileMetadata(Collections.emptyMap());
  }

  @Override
  public Optional<String> getContent() {
    return Optional.empty();
  }

  @Override
  public StructuredFileExtension getStructuredFileExtension() {
    return structuredFileExtension;
  }

}
