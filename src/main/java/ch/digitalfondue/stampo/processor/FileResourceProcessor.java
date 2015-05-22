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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import ch.digitalfondue.stampo.StampoGlobalConfiguration;
import ch.digitalfondue.stampo.resource.Directory;
import ch.digitalfondue.stampo.resource.FileMetadata;
import ch.digitalfondue.stampo.resource.FileResource;
import ch.digitalfondue.stampo.resource.Resource;
import ch.digitalfondue.stampo.resource.StructuredFileExtension;

class FileResourceProcessor {

  private final Map<String, Function<FileResourceParameters, FileResourceProcessorOutput>> processors;

  private final Path contentDir, outputDir;
  private final StampoGlobalConfiguration configuration;

  FileResourceProcessor(StampoGlobalConfiguration configuration, Path outputDir, Directory root) {

    this.contentDir = configuration.getContentDir();
    this.outputDir = outputDir;
    this.configuration = configuration;

    Map<String, Function<FileResourceParameters, FileResourceProcessorOutput>> p = new HashMap<>();

    configuration.getRenderers().forEach(renderer -> renderer.registerResourceRenderer(root, configuration, p));

    processors = Collections.unmodifiableMap(p);
  }
  
  private static class ProcessedFileResource implements FileResource {
    

    private final FileResource fileResource;
    private final Optional<String> content;
    
    ProcessedFileResource(FileResource fileResource, Optional<String> content) {
      this.fileResource = fileResource;
      this.content = content;
    }

    @Override
    public Resource getParent() {
      return fileResource.getParent();
    }

    @Override
    public Path getPath() {
      return fileResource.getPath();
    }

    @Override
    public StampoGlobalConfiguration getConfiguration() {
      return fileResource.getConfiguration();
    }

    @Override
    public FileMetadata getMetadata() {
      return fileResource.getMetadata();
    }

    @Override
    public Optional<String> getContent() {
      return content;
    }

    @Override
    public StructuredFileExtension getStructuredFileExtension() {
      return fileResource.getStructuredFileExtension();
    }
    
  }


  FileResourceProcessorOutput applyProcessors(FileResource fileResource, Locale locale, Map<String, Object> model) {

    StructuredFileExtension ext = fileResource.getStructuredFileExtension();
    
    List<String> processorsExt = new ArrayList<>(ext.getProcessorRelatedExts());
    Collections.reverse(processorsExt);
    
    List<Function<FileResourceParameters, FileResourceProcessorOutput>> processorsToApply = processorsExt.stream().map(processors::get).collect(Collectors.toList());
    if(processorsToApply.isEmpty()) {
      processorsToApply = Collections.singletonList(x -> {
        return new FileResourceProcessorOutput(fileResource.getContent().orElseThrow(IllegalArgumentException::new), fileResource.getPath(), "none", locale);
      });
    }
    
    //TODO can be converted in a reduce operation
    FileResourceParameters param = new FileResourceParameters(fileResource, locale, model);
    FileResourceProcessorOutput output = null;
    for(Function<FileResourceParameters, FileResourceProcessorOutput> toApply : processorsToApply) {
      output = toApply.apply(param);
      param = new FileResourceParameters(new ProcessedFileResource(fileResource, Optional.ofNullable(output.getContent())), locale, model);
    }
    return output;
  }

  /**
   * 
   * <pre>
   * 
   * Generate the final file name output.
   * 
   * The file extension is decomposed in the following sections:
   * 
   * [rest][locales][maybeFileExtension][processorRelatedExts]
   * 
   * the file extensions are decomposed and read from the back to the front. E.g:
   * 
   * test.bla.en.fr.html.peb
   * 
   * - [peb]   as a processorRelatedExt
   * - [html]  as a maybeFileExtension
   * - [fr,en] as a locale if they are registered in the global configuration
   * - [bla]   as a rest 
   * 
   * All the components are optionals.
   * 
   * For composing the final extension, rest is concatenated with :
   * 
   *  - if the first processor has a override name it will be used (for example, markdown will always generate html)
   *  - or else if maybeFileExtension is present it will be used.
   * </pre>
   * 
   * @param fileResource
   * @return
   */
  String finalOutputName(FileResource fileResource) {
    
    StructuredFileExtension fileExt = fileResource.getStructuredFileExtension();
    
    String fileNameWithoutExt = fileResource.getFileNameWithoutExtensions();
    String extension = fileExt.getFinalFileExtension();
    
    boolean useUglyUrl = fileResource.getMetadata().getOverrideUseUglyUrl().orElseGet(configuration::useUglyUrl);

    if (!useUglyUrl && ".html".equals(extension) && !"index".equals(fileNameWithoutExt)
        && !fileResource.getMetadata().getOverrideOutputToPath().isPresent()) {
      return fileNameWithoutExt + "/index.html";// nice url
    } else {
      return fileNameWithoutExt + extension;// "ugly url"
    }
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
        .orElseGet(() -> resourcePath.getFileSystem().getPath(""));

    String finalOutput = rel.resolve(finalOutputName(resource)).toString();

    // outputDir and contentDir can have different underlying FileSystems
    return outputDir.resolve(finalOutput);
  }
}
