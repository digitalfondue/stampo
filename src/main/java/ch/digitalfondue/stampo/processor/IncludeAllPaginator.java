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

import static ch.digitalfondue.stampo.processor.ModelPreparer.prepare;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static java.util.Optional.ofNullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

import ch.digitalfondue.stampo.StampoGlobalConfiguration;
import ch.digitalfondue.stampo.resource.Directory;
import ch.digitalfondue.stampo.resource.DirectoryResource;
import ch.digitalfondue.stampo.resource.FileMetadata;
import ch.digitalfondue.stampo.resource.FileResource;
import ch.digitalfondue.stampo.resource.FileResourceWithMetadataSection;
import ch.digitalfondue.stampo.resource.LocaleAwareDirectory;
import ch.digitalfondue.stampo.resource.Resource;
import ch.digitalfondue.stampo.resource.ResourceFactory;
import ch.digitalfondue.stampo.resource.RootResource;
import ch.digitalfondue.stampo.resource.StructuredFileExtension;
import ch.digitalfondue.stampo.taxonomy.Taxonomy;

public class IncludeAllPaginator implements Directive {

  private final Directory root;
  private final StampoGlobalConfiguration configuration;
  private final Function<FileResource, Path> outputPathExtractor;
  private final Function<Locale, BiFunction<FileResource, Map<String, Object>, FileResourceProcessorOutput>> resourceProcessor;
  private final ResourceFactory resourceFactory;
  private final Taxonomy taxonomy;
  private final Comparator<FileResource> comparator = Comparator.comparing((FileResource f) -> f.getPath().toString(), new AlphaNumericStringComparator(Locale.ENGLISH));

  public IncludeAllPaginator(Directory root, StampoGlobalConfiguration configuration,
      Function<FileResource, Path> outputPathExtractor,
      Function<Locale, BiFunction<FileResource, Map<String, Object>, FileResourceProcessorOutput>> resourceProcessor, Taxonomy taxonomy) {
    this.root = root;
    this.configuration = configuration;
    this.outputPathExtractor = outputPathExtractor;
    this.resourceProcessor = resourceProcessor;
    this.resourceFactory = new ResourceFactory(DirectoryResource::new, FileResourceWithMetadataSection::new, Comparator.comparing(FileResource::getPath), configuration);
    this.taxonomy = taxonomy;
  }

  @Override
  public List<PathAndModelSupplier> generateOutputPaths(FileResource resource, Locale locale,
      Path defaultOutputPath) {

    String path = ofNullable(resource.getMetadata().getRawMap().get("include-all"))
            .map(String.class::cast).orElseThrow(IllegalArgumentException::new);

    Path p = configuration.getBaseDirectory().resolve(path);
    if (!p.startsWith(configuration.getBaseDirectory())) {
      throw new IllegalArgumentException();// cannot be outside
    }
    
    Directory toIncludeAllDir = new LocaleAwareDirectory(locale, new RootResource(resourceFactory, p), FileResourceWithMetadataSection::new);
    
    return traverseDirs(toIncludeAllDir, locale, resource, defaultOutputPath);
  }
  
  private static class SectionAccumulator {
    private final Locale locale;
    private final Directory baseDir;
    private final FileResource resource;
    private final int depth;
    private final int depthLimit;
    
    private final List<PathAndModelSupplier> pathAndModelSupplier;

    SectionAccumulator(Directory baseDir, Locale locale, FileResource resource, List<PathAndModelSupplier> pathAndModelSupplier, int depth, int depthLimit) {
      this.baseDir = baseDir;
      this.locale = locale;
      this.resource = resource;
      this.pathAndModelSupplier = pathAndModelSupplier;
      this.depth = depth;
      this.depthLimit = depthLimit;
    }

    SectionAccumulator(SectionAccumulator sectionModel, int depth) {
      this.baseDir = sectionModel.baseDir;
      this.locale = sectionModel.locale;
      this.resource = sectionModel.resource;
      this.depthLimit = sectionModel.depthLimit;
      this.pathAndModelSupplier = sectionModel.pathAndModelSupplier;
      this.depth = depth;
    }
  }
  
  
  private List<PathAndModelSupplier> traverseDirs(Directory dir, Locale locale, FileResource resource, Path defaultOutputPath) {
    List<PathAndModelSupplier> pathsAndModel = new ArrayList<>();
    
    int maxDepth = (Integer) resource.getMetadata().getRawMap().getOrDefault("paginate-at-depth", 0);
    SectionAccumulator sp = new SectionAccumulator(dir, locale, resource, pathsAndModel, 1, maxDepth);
    traverseSections(dir, defaultOutputPath, sp);
    return pathsAndModel;
  }
  
  
  private void traverseSections(Directory currentDir, Path defaultOutputPath, SectionAccumulator acc) {
    currentDir.getFiles().values().stream().sorted(comparator).forEach(v -> {
      
      Path virtualPath = acc.resource.getPath().getParent().resolve(acc.baseDir.getPath().relativize(v.getPath()));
      FileResource virtualResource = new VirtualPathFileResource(virtualPath, v);
      Path finalOutputPathForResource = outputPathExtractor.apply(virtualResource);
      FileResourceProcessorOutput out = resourceProcessor.apply(acc.locale).apply(v, prepare(root, configuration, acc.locale, v, finalOutputPathForResource, taxonomy, emptyMap()));
      
      if(acc.depth < acc.depthLimit) {
        
        ofNullable(currentDir.getDirectories().get(v.getFileNameWithoutExtensions())).ifPresent(section -> {
          traverseSections(section, defaultOutputPath, new SectionAccumulator(acc, acc.depth + 1));
        });
        
        acc.pathAndModelSupplier.add(new PathAndModelSupplier(finalOutputPathForResource, () ->
          prepare(root, configuration, acc.locale, virtualResource, defaultOutputPath, taxonomy, singletonMap("includeAllResult", out.getContent()))
        ));
        
      } else if(acc.depth == acc.depthLimit) {//cutoff point in the pagination
        
        StringBuilder sb = new StringBuilder();
        
        ofNullable(currentDir.getDirectories().get(v.getFileNameWithoutExtensions())).ifPresent(section -> {
          fetchContentRec(currentDir, finalOutputPathForResource, new SectionAccumulator(acc, acc.depth + 1), sb);
        });
        
        
        acc.pathAndModelSupplier.add(new PathAndModelSupplier(finalOutputPathForResource, () ->
          prepare(root, configuration, acc.locale, virtualResource, defaultOutputPath, taxonomy, singletonMap("includeAllResult", out.getContent() + sb.toString()))
        ));
      }
      
    });
  }
  
  private void fetchContentRec(Directory currentDir, Path defaultOutputPath, SectionAccumulator acc, StringBuilder sb) {
    currentDir.getFiles().values().stream().sorted(comparator).forEach(v -> {
      FileResourceProcessorOutput out = resourceProcessor.apply(acc.locale).apply(v, prepare(root, configuration, acc.locale, v, defaultOutputPath, taxonomy, Collections.emptyMap()));
      sb.append(out.getContent());
      ofNullable(currentDir.getDirectories().get(v.getFileNameWithoutExtensions())).ifPresent(section -> {
        fetchContentRec(section, defaultOutputPath, new SectionAccumulator(acc, acc.depth + 1), sb);
      });
    });
  }

  @Override
  public String name() {
    return "include-all";
  }
  
  
  //override the path of a file resource
  private static class VirtualPathFileResource implements FileResource {
    
    private final Path path;
    private final FileResource fileResource;
    
    VirtualPathFileResource(Path path, FileResource fileResource) {
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
      //TODO: should use parent override for use-ugly url!
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

}
