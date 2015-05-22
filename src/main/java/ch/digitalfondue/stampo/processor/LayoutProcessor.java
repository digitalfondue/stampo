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

import static java.nio.file.Files.exists;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import ch.digitalfondue.stampo.StampoGlobalConfiguration;
import ch.digitalfondue.stampo.resource.Directory;
import ch.digitalfondue.stampo.resource.FileResource;

import com.google.common.io.Files;


class LayoutProcessor {

  private final Map<String, Function<LayoutParameters, LayoutProcessorOutput>> layoutEngines;
  private final FileResourceProcessor fileResourceProcessor;
  private final Path contentDir;
  private final Path layoutDir;


  LayoutProcessor(StampoGlobalConfiguration configuration, Directory root,
      FileResourceProcessor fileResourceProcessor) {
    this.contentDir = configuration.getContentDir();
    this.layoutDir = configuration.getLayoutDir();
    this.fileResourceProcessor = fileResourceProcessor;

    Map<String, Function<LayoutParameters, LayoutProcessorOutput>> l = new LinkedHashMap<>();
    configuration.getRenderers().forEach(renderer -> renderer.registerLayoutRenderer(root, configuration, l));
    layoutEngines = Collections.unmodifiableMap(l);
  }

  // get the correct layout engine given the file extension of the path, if no layout engine
  // has been found, the file will be copied as it is
  LayoutProcessorOutput applyLayout(FileResource resource, Locale locale, Map<String, Object> model) {

    Optional<Path> layout = findLayout(resource);

    return layout.map(Path::toString)//
        .map(Files::getFileExtension)//
        .map(layoutEngines::get)
        //
        .orElse(lParam -> new LayoutProcessorOutput(lParam.model.get("content").toString(), "none", lParam.layoutTemplate, lParam.locale))
          .apply(new LayoutParameters(layout, resource.getPath(), locale, model));
  }

  private String processedExtension(FileResource fileResource) {
    return Files.getFileExtension(fileResourceProcessor.finalOutputName(fileResource));
  }


  /**
   * Given a file "/content/this/is/a/file.ext" it will try to resolve the correct template located
   * at
   * 
   * <pre>
   * - /layout/this/is/a/file.p(file.ext).peb
   * - /layout/this/is/a/index.p(file.ext).peb
   * - /layout/this/is/index.p(file.ext).peb 
   * - /layout/this/index.p(file.ext).peb
   * - /layout/index.p(file.ext).peb
   * </pre>
   * 
   * where p(file.ext) is {@link #processedExtension(FileResource)}
   * 
   * TODO: simplify code, it's ugly ;(
   */
  private Optional<Path> findLayout(FileResource resource) {

    // handle override
    Optional<Path> maybeOverride =
        resource.getMetadata().getOverrideLayout().map(layoutDir::resolve);
    if (maybeOverride.isPresent()) {
      return maybeOverride;
    }


    Path content = resource.getPath();

    Collection<String> extensions = layoutEngines.keySet();


    Path relativeContent = layoutDir.resolve(contentDir.relativize(content));

    String templatePreExtension = "." + processedExtension(resource);//

    LayoutBasePath tentativeLayout = new LayoutBasePath(relativeContent.getParent(), extensions);


    Optional<Path> pathLayout =
        tentativeLayout.exist(Files.getNameWithoutExtension(relativeContent.getFileName()
            .toString()) + templatePreExtension);

    if (pathLayout.isPresent()) {
      return pathLayout;
    } else {
      tentativeLayout = new LayoutBasePath(tentativeLayout.basePath, extensions);
      do {
        pathLayout = tentativeLayout.exist("index" + templatePreExtension);
        if (pathLayout.isPresent()) {
          return pathLayout;
        }
        Optional<Path> maybeParent = Optional.ofNullable(tentativeLayout.basePath.getParent());
        if (!maybeParent.isPresent()) {
          break;
        }
        tentativeLayout = new LayoutBasePath(maybeParent.get(), extensions);
      } while (tentativeLayout.basePath.startsWith(layoutDir));
    }
    return Optional.empty();
  }

  private static class LayoutBasePath {
    private final Collection<String> extensions;
    private final Path basePath;

    LayoutBasePath(Path basePath, Collection<String> extensions) {
      this.basePath = basePath;
      this.extensions = extensions;
    }

    Optional<Path> exist(String name) {
      for (String extension : extensions) {
        Path maybeLayout = basePath.resolve(name + "." + extension);
        if (exists(maybeLayout)) {
          return Optional.of(maybeLayout);
        }
      }
      return Optional.empty();
    }
  }
  // --------------------

}
