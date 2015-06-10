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
package ch.digitalfondue.stampo;

import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.delete;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.isDirectory;
import static java.nio.file.Files.newInputStream;
import static java.nio.file.Files.walkFileTree;
import static java.util.Collections.emptyMap;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import ch.digitalfondue.stampo.exception.MissingDirectoryException;
import ch.digitalfondue.stampo.exception.YamlParserException;
import ch.digitalfondue.stampo.processor.ResourceProcessor;
import ch.digitalfondue.stampo.renderer.Renderer;
import ch.digitalfondue.stampo.renderer.freemarker.FreemarkerRenderer;
import ch.digitalfondue.stampo.renderer.markdown.MarkdownRenderer;
import ch.digitalfondue.stampo.renderer.pebble.PebbleRenderer;
import ch.digitalfondue.stampo.resource.Directory;
import ch.digitalfondue.stampo.resource.DirectoryResource;
import ch.digitalfondue.stampo.resource.FileResource;
import ch.digitalfondue.stampo.resource.FileResourceWithMetadataSection;
import ch.digitalfondue.stampo.resource.LocaleAwareDirectory;
import ch.digitalfondue.stampo.resource.PathOverrideAwareDirectory;
import ch.digitalfondue.stampo.resource.PathOverrideAwareDirectory.Mode;
import ch.digitalfondue.stampo.resource.ResourceFactory;
import ch.digitalfondue.stampo.resource.RootResource;
import ch.digitalfondue.stampo.taxonomy.Taxonomy;

/**
 *
 */
public class Stampo {

  private final StampoGlobalConfiguration configuration;

  
  @SuppressWarnings("unchecked")
  public Stampo(Path baseInputDir, Path outputDir, List<Renderer> renderers, Map<String, Object> configurationOverride) {
    Path configFile = baseInputDir.resolve("configuration.yaml");
    
    Map<String, Object> finalConf = new HashMap<>();
    
    if (exists(configFile)) {
      Yaml yaml = new Yaml();
      try (InputStream is = newInputStream(configFile)) {
        Map<String, Object> c = ofNullable((Map<String, Object>) yaml.loadAs(is, Map.class)).orElse(emptyMap());
        
        finalConf.putAll(c);
        finalConf.putAll(configurationOverride);
        
        this.configuration = new StampoGlobalConfiguration(finalConf, baseInputDir, outputDir, renderers);
      } catch (IOException ioe) {
        throw new IllegalArgumentException(ioe);
      } catch (YAMLException pe) {
        throw new YamlParserException(configFile, pe);
      }
    } else {
      finalConf.putAll(configurationOverride);
      this.configuration = new StampoGlobalConfiguration(finalConf, baseInputDir, outputDir, renderers);
    }
  }

  
  public Stampo(Path baseInputDir, Path outputDir, Map<String, Object> configurationOverride) {
    this(baseInputDir, outputDir, Arrays.asList(new PebbleRenderer(), new MarkdownRenderer(), new FreemarkerRenderer()), configurationOverride);
  }

  public StampoGlobalConfiguration getConfiguration() {
    return configuration;
  }

  public void build() {
    build((processedFile, processedLayout) -> processedLayout.getContent(), (in, out) -> {
      try {
        Files.copy(in, out);
      } catch (IOException ioe) {
        throw new IllegalStateException(ioe);
      }
    });
  }

  public void build(ProcessedInputHandler outputHandler,
      BiConsumer<Path, Path> staticDirectoryAction) {

    if (!exists(configuration.getContentDir())) {
      throw new MissingDirectoryException(configuration.getContentDir());
    }

    List<Locale> locales = configuration.getLocales();
    
    cleanupBuildDirectory();

    Comparator<FileResource> newFileFirst = Comparator.comparingLong(FileResource::getCreationTime).reversed();
    
    ResourceFactory resourceFactory = new ResourceFactory(DirectoryResource::new, FileResourceWithMetadataSection::new, newFileFirst, configuration);
    
    Directory root = new RootResource(resourceFactory, configuration.getContentDir(), configuration);
    Directory rootWithOverrideHidden = new PathOverrideAwareDirectory(Mode.HIDE, root, FileResourceWithMetadataSection::new);
    Directory rootWithOnlyOverride =
        new PathOverrideAwareDirectory(Mode.SHOW_ONLY_PATH_OVERRIDE, root, FileResourceWithMetadataSection::new);
    
    if (locales.size() > 1) {

      Optional<Locale> defaultLocale = configuration.getDefaultLocale();

      for (Locale locale : locales) {
        
        Directory localeAwareRoot = new LocaleAwareDirectory(locale, rootWithOverrideHidden, FileResourceWithMetadataSection::new);
        
        Taxonomy taxonomy = new Taxonomy(configuration.getTaxonomyGroups(), newFileFirst);    
        taxonomy.add(localeAwareRoot);
        

        Path finalOutputDir = defaultLocale.flatMap(l -> l.equals(locale) ? of(configuration.getBaseOutputDir()) : empty())//
                .orElse(configuration.getBaseOutputDir().resolve(locale.toLanguageTag()));


        render(localeAwareRoot, new ResourceProcessor(finalOutputDir, localeAwareRoot,
            configuration, taxonomy), locale, outputHandler);
      }
      
      Taxonomy taxonomy = new Taxonomy(configuration.getTaxonomyGroups(), newFileFirst);    
      taxonomy.add(rootWithOnlyOverride);

      render(rootWithOnlyOverride, new ResourceProcessor(configuration.getBaseOutputDir(),
          rootWithOnlyOverride, configuration, taxonomy), defaultLocale.orElse(Locale.ENGLISH), outputHandler);
    } else {
      
      Taxonomy taxonomy = new Taxonomy(configuration.getTaxonomyGroups(), newFileFirst);
      taxonomy.add(rootWithOnlyOverride);    
      taxonomy.add(rootWithOverrideHidden);
      
      render(rootWithOverrideHidden, new ResourceProcessor(configuration.getBaseOutputDir(),
          rootWithOverrideHidden, configuration, taxonomy), locales.get(0), outputHandler);

      render(rootWithOnlyOverride, new ResourceProcessor(configuration.getBaseOutputDir(),
          rootWithOnlyOverride, configuration, taxonomy), locales.get(0), outputHandler);
    }

    copyStaticDirectory(staticDirectoryAction);
  }

  private void render(Directory root, ResourceProcessor renderer, Locale locale,
      ProcessedInputHandler outputHandler) {
    root.getFiles().values().forEach(f -> renderer.process(f, locale, outputHandler));
    root.getDirectories().values().forEach(d -> {
      render(d, renderer, locale, outputHandler);
    });
  }

  private void copyStaticDirectory(BiConsumer<Path, Path> staticDirectoryAction) {
    
    Path baseOutputDir = configuration.getBaseOutputDir();
    Path staticDir = configuration.getStaticDir();
    
    if (exists(configuration.getStaticDir()) && isDirectory(configuration.getStaticDir())) {
      try {
        walkFileTree(configuration.getStaticDir(), new SimpleFileVisitor<Path>() {
          @Override
          public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            Path outputPath = baseOutputDir.resolve(staticDir.relativize(file).toString());
            createDirectories(outputPath.getParent());
            staticDirectoryAction.accept(file, outputPath);
            return FileVisitResult.CONTINUE;
          }
        });
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }
  }

  public void cleanupBuildDirectory() {
    if (exists(configuration.getBaseOutputDir()) && isDirectory(configuration.getBaseOutputDir())) {
      try {
        walkFileTree(configuration.getBaseOutputDir(), new SimpleFileVisitor<Path>() {
          @Override
          public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            delete(file);
            return FileVisitResult.CONTINUE;
          }

          @Override
          public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            delete(dir);
            return FileVisitResult.CONTINUE;
          }
        });
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }
  }
}
