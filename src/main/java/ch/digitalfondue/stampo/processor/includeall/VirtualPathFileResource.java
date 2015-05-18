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
package ch.digitalfondue.stampo.processor.includeall;

import java.nio.file.Path;
import java.util.Optional;

import ch.digitalfondue.stampo.StampoGlobalConfiguration;
import ch.digitalfondue.stampo.resource.FileMetadata;
import ch.digitalfondue.stampo.resource.FileResource;
import ch.digitalfondue.stampo.resource.Resource;
import ch.digitalfondue.stampo.resource.StructuredFileExtension;

// override the path of a file resource
public class VirtualPathFileResource implements FileResource {

  private final Path path;
  private final FileResource fileResource;

  public VirtualPathFileResource(Path path, FileResource fileResource) {
    this.path = path;
    this.fileResource = fileResource;
  }

  @Override
  public Resource getParent() {
    throw new IllegalStateException();
  }

  @Override
  public Path getPath() {
    return path;
  }

  @Override
  public StampoGlobalConfiguration getConfiguration() {
    return fileResource.getConfiguration();
  }

  @Override
  public FileMetadata getMetadata() {
    // TODO: should use parent override for use-ugly url!
    return fileResource.getMetadata();
  }

  @Override
  public Optional<String> getContent() {
    return fileResource.getContent();
  }

  @Override
  public StructuredFileExtension getStructuredFileExtension() {
    return fileResource.getStructuredFileExtension();
  }

}