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

import static java.util.stream.Stream.concat;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ch.digitalfondue.stampo.StampoGlobalConfiguration;
import ch.digitalfondue.stampo.resource.Directory;
import ch.digitalfondue.stampo.resource.FileResource;

import com.google.common.collect.Lists;

class FileResourceProcessor {

  private final Map<String, Function<FileResourceParameters, FileResourceProcessorOutput>> processors;
  private final Map<String, String> resultingProcessedFileExtensions;

  private final Path contentDir, outputDir;
  private final StampoGlobalConfiguration configuration;
  private final Directory root;


  FileResourceProcessor(StampoGlobalConfiguration configuration, Path outputDir, Directory root) {

    this.contentDir = configuration.getContentDir();
    this.outputDir = outputDir;
    this.configuration = configuration;
    this.root = root;

    Map<String, Function<FileResourceParameters, FileResourceProcessorOutput>> p = new HashMap<>();
    Map<String, String> r = new HashMap<>();

    configuration.getRenderers().forEach(
        (renderer) -> renderer.registerResourceRenderer(root, configuration, p, r));

    processors = Collections.unmodifiableMap(p);
    resultingProcessedFileExtensions = Collections.unmodifiableMap(r);
  }


  FileResourceProcessorOutput applyProcessors(FileResource fileResource, Locale locale) {

    Map<String, Object> model = new HashMap<>();
    model.put("root", root);
    model.put("configuration", configuration);
    model.put("metadata", fileResource.getMetadata());
    model.put("resource", fileResource);
    model.put("locale", locale);


    Optional<String> extension = fileResource.getFileExtensions().stream().findFirst();

    return extension
        .map(processors::get)
        .orElse(
            (x) -> {
              return new FileResourceProcessorOutput(fileResource.getContent(), fileResource
                  .getPath(), "none", locale);
            }).apply(new FileResourceParameters(fileResource, locale, model));
  }

  /**
   * 
   * <pre>
   * 
   * Generate the final file name output.
   * 
   * As a rule:
   * 
   * - if the file has a processor attached and the processor has a extension output:
   *    - e.g. markdown: content/test-markdown.md will generate a file in /output/test-markdown.html
   * 
   * - if the file has a processor attached but the processor has no extension output _but_ the user has added a second extension:
   *    - e.g. pebble: /content/test-pebble-html.html.peb will generate a file /output/test-pebble-html.html
   *    
   * - fallback scenario 
   *   - e.g. /content/test.txt will generate a file in /output/test.txt
   *   - e.g. /content/test-pebble.peb will generate a file /output/test-pebble.peb
   * 
   * </pre>
   * 
   * @param fileResource
   * @return
   */
  String finalOutputName(FileResource fileResource) {
    String fileNameWithoutExt = fileResource.getFileNameWithoutExtensions();
    List<String> exts = fileResource.getFileExtensions();

    Optional<String> ext1 = exts.stream().findFirst();
    Optional<String> ext2 = exts.stream().skip(1).findFirst();

    boolean hasProcessor = ext1.map(processors::get).isPresent();
    boolean hasProcessedFileExtension = ext1.map(resultingProcessedFileExtensions::get).isPresent();

    Stream<String> s;

    if (hasProcessor && hasProcessedFileExtension) {
      s =
          concat(exts.stream().skip(1).collect(reverse()).stream(),
              streamOpt(ext1.map(resultingProcessedFileExtensions::get)));
    } else if (hasProcessor && !hasProcessedFileExtension && ext2.isPresent()) {
      s = concat(exts.stream().skip(2).collect(reverse()).stream(), streamOpt(ext2));
    } else {
      s = exts.stream().collect(reverse()).stream();
    }

    // remove locales in the extensions
    s =
        s.filter((ext) -> !configuration.getLocales().stream().map(Object::toString)
            .collect(Collectors.toList()).contains(ext));


    // handle the case filename.LOCALE.peb -> expected result must be filename.peb
    String extension = s.collect(Collectors.joining(".", ".", ""));
    if (extension.equals(".")) {
      extension = extension + ext1.orElse("");
    }
    
    boolean useUglyUrl = fileResource.getMetadata().getOverrideUseUglyUrl().orElseGet(configuration::useUglyUrl);

    if (!useUglyUrl && ".html".equals(extension) && !"index".equals(fileNameWithoutExt)
        && !fileResource.getMetadata().getOverrideOutputToPath().isPresent()) {
      return fileNameWithoutExt + "/index.html";// nice url
    } else {
      return fileNameWithoutExt + extension;// "ugly url"
    }
  }

  private static <T> Collector<T, List<T>, List<T>> reverse() {
    return Collector.of(ArrayList::new, (l, t) -> l.add(t), (l, r) -> {
      l.addAll(r);
      return l;
    }, Lists::<T>reverse);
  }

  private static <T> Stream<T> streamOpt(Optional<T> opt) {
    return opt.isPresent() ? Stream.of(opt.get()) : Stream.empty();
  }

  /**
   * Generate the output path given the resource and the output dir.
   * 
   * E.g.: /content/test/test.html -> /output/test/test.html
   * 
   * @param resource
   * @return
   */
  public Path normalizeOutputPath(FileResource resource) {
    Path resourcePath = resource.getPath();

    Path rel = Optional.ofNullable((contentDir.relativize(resourcePath)).getParent())//
        .orElse(resourcePath.getFileSystem().getPath(""));

    String finalOutput = rel.resolve(finalOutputName(resource)).toString();

    // outputDir and contentDir can have different underlying FileSystems
    return outputDir.resolve(finalOutput);
  }
}
