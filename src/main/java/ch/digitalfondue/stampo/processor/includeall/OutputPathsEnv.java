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
import java.util.Locale;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

import ch.digitalfondue.stampo.StampoGlobalConfiguration;
import ch.digitalfondue.stampo.processor.FileResourceProcessorOutput;
import ch.digitalfondue.stampo.resource.Directory;
import ch.digitalfondue.stampo.resource.FileResource;
import ch.digitalfondue.stampo.taxonomy.Taxonomy;

public class OutputPathsEnv {
  public final int maxDepth;
  public final Locale locale;
  public final FileResource resource;
  public final StampoGlobalConfiguration configuration;
  public final Directory root;
  public final Function<FileResource, Path> outputPathExtractor;
  public final Function<Locale, BiFunction<FileResource, Map<String, Object>, FileResourceProcessorOutput>> resourceProcessor;
  public final Taxonomy taxonomy;

  public OutputPathsEnv(int maxDepth, Locale locale, FileResource resource, StampoGlobalConfiguration configuration, Directory root, Function<FileResource, Path> outputPathExtractor, Function<Locale, BiFunction<FileResource, Map<String, Object>, FileResourceProcessorOutput>> resourceProcessor, Taxonomy taxonomy) {
    this.maxDepth = maxDepth;
    this.locale = locale;
    this.resource = resource;
    this.configuration = configuration;
    this.root = root;
    this.outputPathExtractor = outputPathExtractor;
    this.resourceProcessor = resourceProcessor;
    this.taxonomy = taxonomy;
  }
}