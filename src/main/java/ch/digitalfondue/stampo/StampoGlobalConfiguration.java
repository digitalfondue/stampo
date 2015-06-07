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

import static java.nio.file.Files.newInputStream;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableMap;
import static java.util.Collections.unmodifiableSet;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle.Control;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.yaml.snakeyaml.Yaml;

import ch.digitalfondue.stampo.i18n.ResourceBundleControl;
import ch.digitalfondue.stampo.renderer.Renderer;

public class StampoGlobalConfiguration {
  
  public static final String CONF_LOCALES = "locales";
  public static final String CONF_DEFAULT_LOCALES = "default-locale";
  public static final String CONF_USE_UGLY_URL = "use-ugly-url";
  public static final String CONF_TAXONOMIES = "taxonomies";
  public static final String CONF_HIDE_DRAFT = "hide-draft";

  private final Map<String, Object> configuration;
  private final List<Locale> locales;
  private final Set<String> localesAsString;
  private final Optional<Locale> defaultLocale;
  private final Set<String> ignorePatterns;
  private final List<Renderer> renderers;


  private final Path baseDirectory;
  private final Path baseOutputDir;
  private final Path contentDir;
  private final Path layoutDir;
  private final Path staticDir;
  private final Path localesDir;
  private final Path dataDir;

  private final Map<String, Object> data;
  
  //
  private final Set<String> processorResourceExtensions;
  private final Map<String, String> processorExtensionTransformMapping;
  //
  private final Set<String> taxonomies;

  public StampoGlobalConfiguration(Map<String, Object> configuration, Path baseDirectory,
      Path baseOutputDir, List<Renderer> renderers) {
    this.configuration = configuration;
    this.renderers = renderers;
    this.locales = extractLocales(configuration);
    this.localesAsString = unmodifiableSet(locales.stream().map(Object::toString).collect(toSet()));
    this.defaultLocale = defaultLocale(configuration);
    this.ignorePatterns = extractIgnorePatterns(configuration);
    this.taxonomies = extractTaxonomies(configuration);

    this.baseDirectory = baseDirectory;
    this.baseOutputDir = baseOutputDir;

    this.contentDir = baseDirectory.resolve("content").normalize();
    this.layoutDir = baseDirectory.resolve("layout").normalize();
    this.staticDir = baseDirectory.resolve("static").normalize();
    this.localesDir = baseDirectory.resolve("locales").normalize();
    this.dataDir = baseDirectory.resolve("data").normalize();
    
    this.data = extractData();
    
    
    Set<String> resProcExt = new HashSet<>();
    Map<String, String> resProcMapping = new HashMap<>();
    for (Renderer renderer : renderers) {
      resProcExt.addAll(renderer.resourceExtensions());
      resProcMapping.putAll(renderer.extensionTransformMapping());
    }
    
    processorResourceExtensions = unmodifiableSet(resProcExt);
    processorExtensionTransformMapping = unmodifiableMap(resProcMapping);
  }

  // patterns follow
  // http://docs.oracle.com/javase/7/docs/api/java/nio/file/FileSystem.html#getPathMatcher%28java.lang.String%29
  @SuppressWarnings("unchecked")
  private Set<String> extractIgnorePatterns(Map<String, Object> configuration) {
    List<String> patterns = asList("glob:.*",// hidden files
        "glob:*~", "glob:#*#", "glob:.#*", // emacs
        "glob:*~", "glob:[._]*.s[a-w][a-z]", "glob:[._]s[a-w][a-z]", "glob:*.un~" // vim
    );
    Set<String> s = new HashSet<>((List<String>) configuration.getOrDefault("ignore-patterns",patterns));
    s.addAll(patterns);
    return unmodifiableSet(s);
  }

  @SuppressWarnings("unchecked")
  private static List<Locale> extractLocales(Map<String, Object> configuration) {

    Object locales = configuration.getOrDefault(CONF_LOCALES, Locale.ENGLISH.toString());
    List<String> l;
    if (locales instanceof String) {
      l = singletonList(locales.toString());
    } else if (locales instanceof List) {
      l = (List<String>) locales;
    } else {
      throw new IllegalArgumentException("wrong type for locales: " + locales);
    }

    return l.stream().map(Locale::forLanguageTag).collect(toList());
  }
  
  @SuppressWarnings("unchecked")
  private static Set<String> extractTaxonomies(Map<String, Object> configuration) {
    Optional<Object> maybeTaxonomies = ofNullable(configuration.get(CONF_TAXONOMIES));
    return maybeTaxonomies.map(taxonomies -> {
      if(taxonomies instanceof String) {
        return singleton(taxonomies.toString());
      } else if (taxonomies instanceof Collection) {
        return ((Collection<Object>) taxonomies).stream().map(Object::toString).collect(toCollection(LinkedHashSet::new));
      } else {
        throw new IllegalArgumentException("wrong type for taxonomies: " + taxonomies);
      }
    }).orElse(emptySet());
  }
  
  
  private static class KeyValue {
    private final String key;
    private final Object value;
    
    KeyValue(String key, Object value) {
      this.key = key;
      this.value = value;
    }

    public String getKey() {
      return key;
    }

    public Object getValue() {
      return value;
    }
  }
  
  private Map<String, Object> extractData() {
    Path dataDir = getDataDir();
    if(Files.exists(dataDir)) {
      try {
        return Files.walk(dataDir).filter(p -> {
          String fileName = p.getFileName().toString();
          return Files.isRegularFile(p) && (fileName.endsWith(".yaml") || fileName.endsWith(".yml"));
        }).map(p -> {
          String keyName = PathUtils.relativePathTo(p, dataDir).replace('/', '.').replaceFirst("\\.ya{0,1}ml$", "");
          try (InputStream is = newInputStream(p)) {
            List<Object> o = StreamSupport.stream(new Yaml().loadAll(is).spliterator(), false).filter(Objects::nonNull).collect(toList());
            return new KeyValue(keyName, o.size() == 0 ? null : o.size() == 1 ? o.get(0) : o);
          } catch (IOException e) {
            throw new IllegalStateException(e);
          }
        }).collect(Collectors.toMap(KeyValue::getKey, KeyValue::getValue));
      } catch (IOException ioe) {
        throw new IllegalStateException(ioe);
      }
    }
    return emptyMap();
  }
  
  public Set<String> getTaxonomyGroups() {
    return taxonomies;
  }

  private static Optional<Locale> defaultLocale(Map<String, Object> configuration) {
    return ofNullable(configuration.get(CONF_DEFAULT_LOCALES)).map(Object::toString).map(
        Locale::forLanguageTag);
  }

  public Control getResourceBundleControl() {
    return new ResourceBundleControl(localesDir);
  }

  public Map<String, Object> getConfiguration() {
    return configuration;
  }

  public Optional<Locale> getDefaultLocale() {
    return defaultLocale;
  }

  public List<Locale> getLocales() {
    return locales;
  }
  
  public boolean useUglyUrl() {
    return ofNullable(configuration.get(CONF_USE_UGLY_URL)).map(Boolean.class::cast).orElse(false);
  }
  
  public boolean hideDraft() {
    return ofNullable(configuration.get(CONF_HIDE_DRAFT)).map(Boolean.class::cast).orElse(false);
  }

  public Path getBaseDirectory() {
    return baseDirectory;
  }

  public Path getBaseOutputDir() {
    return baseOutputDir;
  }

  public Path getContentDir() {
    return contentDir;
  }

  public Path getLayoutDir() {
    return layoutDir;
  }


  public Path getStaticDir() {
    return staticDir;
  }

  public Set<String> getIgnorePatterns() {
    return ignorePatterns;
  }

  public Path getLocalesDir() {
    return localesDir;
  }

  public List<Renderer> getRenderers() {
    return renderers;
  }

  public Path getDataDir() {
    return dataDir;
  }

  public Map<String, Object> getData() {
    return data;
  }

  public Set<String> getLocalesAsString() {
    return localesAsString;
  }

  public Set<String> getProcessorResourceExtensions() {
    return processorResourceExtensions;
  }

  public Map<String, String> getProcessorExtensionTransformMapping() {
    return processorExtensionTransformMapping;
  }
}
