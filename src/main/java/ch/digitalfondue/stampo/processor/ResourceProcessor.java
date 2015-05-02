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
package ch.digitalfondue.stampo.processor;

import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.newBufferedWriter;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import ch.digitalfondue.stampo.ProcessedInputHandler;
import ch.digitalfondue.stampo.StampoGlobalConfiguration;
import ch.digitalfondue.stampo.resource.Directory;
import ch.digitalfondue.stampo.resource.FileMetadata;
import ch.digitalfondue.stampo.resource.FileResource;
import ch.digitalfondue.stampo.taxonomy.Taxonomy;

// TODO: cleanup and break up the class
public class ResourceProcessor {

  private final LayoutProcessor layoutProcessor;
  private final FileResourceProcessor fileResourceProcessor;
  private final Directory root;
  private final StampoGlobalConfiguration configuration;
  private final Path outputDir;
  private final Taxonomy taxonomy;
  private final Map<String, Directive> directives;

  public ResourceProcessor(Path outputDir, Directory root, StampoGlobalConfiguration configuration,
      Taxonomy taxonomy) {

    this.root = root;
    this.configuration = configuration;
    this.outputDir = outputDir;
    this.taxonomy = taxonomy;

    this.fileResourceProcessor = new FileResourceProcessor(configuration, outputDir, root);
    this.layoutProcessor = new LayoutProcessor(configuration, root, fileResourceProcessor);

    this.directives =
        Arrays
            .asList(
                new DirPaginator(root, configuration, this::extractOutputPath,
                    (locale) -> (f, m) -> fileResourceProcessor.applyProcessors(f, locale, m),
                    taxonomy),//
                new TaxonomyPaginator(root, configuration, this::extractOutputPath, (locale) -> (f,
                    m) -> fileResourceProcessor.applyProcessors(f, locale, m), taxonomy),//
                new DefaultDirective())//
            .stream()//
            .collect(Collectors.toMap(Directive::name, Function.identity()));
  }



  public void process(FileResource resource, Locale locale, ProcessedInputHandler outputHandler) {

    FileMetadata metadata = resource.getMetadata();

    Locale finalLocale = metadata.getOverrideLocale().orElse(locale);


    Path defaultOutputPath = extractOutputPath(resource);

    List<PathAndModelSupplier> outputPaths =
        directives.get(metadata.getDirective()).generateOutputPaths(resource, finalLocale,
            defaultOutputPath);

    //
    outputPaths.forEach(outputPathAndModel -> processToPath(resource, outputHandler, finalLocale,
        outputPathAndModel.getOutputPath(), outputPathAndModel.getModelSupplier()));

  }


  private Path extractOutputPath(FileResource resource) {
    FileMetadata metadata = resource.getMetadata();
    Path defaultOutputPath =
        metadata.getOverrideOutputToPath().map(outputDir::resolve)
            .orElseGet(() -> fileResourceProcessor.normalizeOutputPath(resource));
    return defaultOutputPath;
  }



  private void processToPath(FileResource resource, ProcessedInputHandler outputHandler,
      Locale finalLocale, Path outputPath, Supplier<Map<String, Object>> additionalData) {


    if (!outputPath.startsWith(outputDir)) {
      throw new IllegalStateException("output path " + outputPath
          + " must be a child of outputDir: " + outputDir
          + " (override-output-to-path must be a relative path: it must not begin with \"/\")");
    }

    try {
      // ensure presence of base directory
      createDirectories(outputPath.getParent());
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }

    Map<String, Object> model =
        ModelPreparer.prepare(root, configuration, finalLocale, resource, outputPath, taxonomy,
            additionalData.get());

    FileResourceProcessorOutput processed =
        fileResourceProcessor.applyProcessors(resource, finalLocale, model);

    Map<String, Object> layoutModel = new HashMap<String, Object>(model);

    layoutModel.put("content", processed.getContent());

    LayoutProcessorOutput processedLayout =
        layoutProcessor.applyLayout(resource, finalLocale, layoutModel);

    try (Writer writer =
        newBufferedWriter(outputPath, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW,
            StandardOpenOption.WRITE)) {
      writer.write(outputHandler.apply(processed, processedLayout));
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }
}
