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

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

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
    
    StructuredDocument doc = new StructuredDocument(0, toIncludeAllDir, p);
    
    int maxDepth = (Integer) resource.getMetadata().getRawMap().getOrDefault("paginate-at-depth", 1);
    
    List<PathAndModelSupplier> res = new ArrayList<>();
    doc.toOutputPaths(0, new OutputPathsEnv(maxDepth, locale, toIncludeAllDir, resource, defaultOutputPath), res);
    return res;
  }
  
  private static class OutputPathsEnv {
    final int maxDepth;
    final Locale locale;
    final Directory baseDir;
    final FileResource resource;
    final Path defaultOutputPath;
    
    public OutputPathsEnv(int maxDepth, Locale locale, Directory baseDir, FileResource resource, Path defaultOutputPath) {
      this.maxDepth = maxDepth;
      this.locale = locale;
      this.baseDir = baseDir;
      this.resource = resource;
      this.defaultOutputPath = defaultOutputPath;
    }
  }
  
  
  private class StructuredDocument {
    final int depth;
    final Path relativeToBasePath;
    final Optional<FileResource> fileResource;
    final Optional<Directory> directory;
    final List<StructuredDocument> childDocuments;
    
    
    StructuredDocument(int depth, Directory directory, Path basePath) {
      this.depth = depth;
      this.fileResource = empty();
      this.directory = of(directory);
      this.childDocuments = from(depth, of(directory), basePath);
      this.relativeToBasePath = basePath.relativize(directory.getPath());
    }
    
    StructuredDocument(int depth, FileResource resource, Optional<Directory> directory, Path basePath) {
      this.depth = depth;
      this.fileResource = of(resource);
      this.directory = directory;
      this.childDocuments = from(depth, directory, basePath);
      this.relativeToBasePath = basePath.relativize(resource.getPath());
    }
    
    void toOutputPaths(int depth, OutputPathsEnv env, List<PathAndModelSupplier> res) {
      
      FileResource v = fileResource.orElseGet(() -> new FileResourcePlaceHolder(relativeToBasePath, configuration));
      
      Path virtualPath = env.resource.getPath().getParent().resolve(this.relativeToBasePath.toString());
      
      FileResource virtualResource = new VirtualPathFileResource(virtualPath, v);
      Path finalOutputPathForResource = outputPathExtractor.apply(virtualResource);
      
      StringBuilder sb = new StringBuilder();
      
      if(depth < env.maxDepth) {
        sb.append(renderFile(env.locale));
        childDocuments.forEach(sd -> sd.toOutputPaths(depth + 1, env, res));
      } else if (depth == env.maxDepth ) {//cutoff point
        sb.append(renderFile(env.locale));
        renderChildDocuments(env.locale).forEach(sb::append);
      }
      
      res.add(new PathAndModelSupplier(finalOutputPathForResource, () -> Collections.singletonMap("includeAllResult", sb.toString())));
      
    }
    
    
    String renderFile(Locale locale) {
      return fileResource.map(f -> resourceProcessor.apply(locale).apply(f, Collections.emptyMap()).getContent()).orElse("");
    }
    
    
    List<String> renderChildDocuments(Locale locale) {
      return childDocuments.stream().map(c -> c.renderFile(locale) + c.renderChildDocuments(locale).stream().collect(Collectors.joining())).collect(Collectors.toList());
    }
    
    
    List<StructuredDocument> from(int depth, Optional<Directory> directory, Path basePath) {
      List<StructuredDocument> res = new ArrayList<>();
      
      return directory.map((d) -> {
        
        Set<Path> alreadyVisisted = new HashSet<>();
        
        d.getFiles().values().forEach(f -> {
          Optional<Directory> associatedChildDir = ofNullable(d.getDirectories().get(f.getFileNameWithoutExtensions()));
          associatedChildDir.map(Directory::getPath).ifPresent(alreadyVisisted::add);
          res.add(new StructuredDocument(depth + 1, f, associatedChildDir, basePath));
        });
        
        
        d.getDirectories().values().stream().filter(dir -> !alreadyVisisted.contains(dir.getPath())).forEach(dir -> {
          res.add(new StructuredDocument(depth + 1, dir, basePath));
        });
        
        Collections.sort(res, Comparator.comparing((StructuredDocument sd) -> sd.fileResource.map(FileResource::getFileNameWithoutExtensions).orElseGet(() -> sd.directory.map(Directory::getName).orElse(""))));
        
        
        return res;
      }).orElseGet(Collections::emptyList);
    }
  }
  
  private static class FileResourcePlaceHolder implements FileResource {
    
    final StampoGlobalConfiguration conf;
    final Path path;
    
    FileResourcePlaceHolder(Path path, StampoGlobalConfiguration conf) {
      this.path = path;
      this.conf = conf;
    }

    @Override
    public Resource getParent() {
      return null;
    }

    @Override
    public Path getPath() {
      return path;
    }

    @Override
    public StampoGlobalConfiguration getConfiguration() {
      return conf;
    }

    @Override
    public FileMetadata getMetadata() {
      return new FileMetadata(Collections.emptyMap());
    }

    @Override
    public Optional<String> getContent() {
      return of("");
    }

    @Override
    public StructuredFileExtension getStructuredFileExtension() {
      return new StructuredFileExtension(Collections.emptyList(), empty(), of("html"), Collections.emptySet(), Collections.emptyList());
    }
    
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
