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
import java.util.Locale;
import java.util.Map;

import ch.digitalfondue.stampo.ProcessedInputHandler;
import ch.digitalfondue.stampo.StampoGlobalConfiguration;
import ch.digitalfondue.stampo.resource.Directory;
import ch.digitalfondue.stampo.resource.FileMetadata;
import ch.digitalfondue.stampo.resource.FileResource;

public class ResourceProcessor {

  private final LayoutProcessor layoutProcessor;
  private final FileResourceProcessor fileResourceProcessor;
  private final Directory root;
  private final StampoGlobalConfiguration configuration;
  private final Path outputDir;

  public ResourceProcessor(Path outputDir, Directory root, StampoGlobalConfiguration configuration) {

    this.root = root;
    this.configuration = configuration;
    this.outputDir = outputDir;

    this.fileResourceProcessor = new FileResourceProcessor(configuration, outputDir, root);
    this.layoutProcessor = new LayoutProcessor(configuration, root, fileResourceProcessor);
  }


  public void process(FileResource resource, Locale locale, ProcessedInputHandler outputHandler) {

    FileMetadata metadata = resource.getMetadata();
    
    //TODO here for the handling of a paginated resource

    Locale finalLocale = metadata.getOverrideLocale().orElse(locale);
    Path outputPath =
        metadata.getOverrideOutputToPath().map(outputDir::resolve)
            .orElse(fileResourceProcessor.normalizeOutputPath(resource));

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

    FileResourceProcessorOutput processed =
        fileResourceProcessor.applyProcessors(resource, finalLocale);

    Map<String, Object> model = ModelPreparer.prepare(root, configuration, finalLocale, resource);
    
    model.put("content", processed.getContent());

    LayoutProcessorOutput processedLayout =
        layoutProcessor.applyLayout(resource, finalLocale, model);

    try (Writer writer =
        newBufferedWriter(outputPath, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW,
            StandardOpenOption.WRITE)) {
      writer.write(outputHandler.apply(processed, processedLayout));
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }
}
