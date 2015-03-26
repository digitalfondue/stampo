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
import java.util.Comparator;

import ch.digitalfondue.stampo.StampoGlobalConfiguration;

public class ResourceFactory {

  private final DirectorySupplier directorySupplier;
  private final FileResourceSupplier fileResourceSupplier;
  private final Comparator<FileResource> fileResourceComparator;
  private final StampoGlobalConfiguration configuration;

  public ResourceFactory(DirectorySupplier directorySupplier,
      FileResourceSupplier fileResourceSupplier, Comparator<FileResource> fileResourceComparator,
      StampoGlobalConfiguration configuration) {
    this.directorySupplier = directorySupplier;
    this.fileResourceSupplier = fileResourceSupplier;
    this.fileResourceComparator = fileResourceComparator;
    this.configuration = configuration;
  }


  public Directory directory(Path path, Resource parent) {
    return directorySupplier.get(this, path, parent);
  }

  public FileResource fileResource(Path path, Resource parent) {
    return fileResourceSupplier.get(configuration, path, parent);
  }

  @FunctionalInterface
  public interface DirectorySupplier {
    Directory get(ResourceFactory factory, Path path, Resource parent);
  }

  @FunctionalInterface
  public interface FileResourceSupplier {
    FileResource get(StampoGlobalConfiguration configuration, Path path, Resource parent);
  }

  public Comparator<FileResource> getFileResourceComparator() {
    return fileResourceComparator;
  }

  public StampoGlobalConfiguration getConfiguration() {
    return configuration;
  }
}
