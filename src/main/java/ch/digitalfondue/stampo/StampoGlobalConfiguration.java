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

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableSet;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle.Control;
import java.util.Set;

import ch.digitalfondue.stampo.i18n.ResourceBundleControl;
import ch.digitalfondue.stampo.renderer.Renderer;

public class StampoGlobalConfiguration {
  
  public static final String CONF_LOCALES = "locales";
  public static final String CONF_DEFAULT_LOCALES = "default-locale";
  public static final String CONF_USE_UGLY_URL = "use-ugly-url";

  private final Map<String, Object> configuration;
  private final List<Locale> locales;
  private final Optional<Locale> defaultLocale;
  private final Set<String> ignorePatterns;
  private final List<Renderer> renderers;


  private final Path baseDirectory;
  private final Path baseOutputDir;
  private final Path contentDir;
  private final Path layoutDir;
  private final Path staticDir;
  private final Path localesDir;



  public StampoGlobalConfiguration(Map<String, Object> configuration, Path baseDirectory,
      Path baseOutputDir, List<Renderer> renderers) {
    this.configuration = configuration;
    this.renderers = renderers;
    this.locales = extractLocales(configuration);
    this.defaultLocale = defaultLocale(configuration);
    this.ignorePatterns = extractIgnorePatterns(configuration);

    this.baseDirectory = baseDirectory;
    this.baseOutputDir = baseOutputDir;

    this.contentDir = baseDirectory.resolve("content").normalize();
    this.layoutDir = baseDirectory.resolve("layout").normalize();
    this.staticDir = baseDirectory.resolve("static").normalize();
    this.localesDir = baseDirectory.resolve("locales").normalize();
  }

  // patterns follow
  // http://docs.oracle.com/javase/7/docs/api/java/nio/file/FileSystem.html#getPathMatcher%28java.lang.String%29
  @SuppressWarnings("unchecked")
  private Set<String> extractIgnorePatterns(Map<String, Object> configuration) {
    return unmodifiableSet(new HashSet<>((List<String>) configuration.getOrDefault(
        "ignore-patterns",//
        asList("glob:.*",// hidden files
            "glob:*~", "glob:#*#", "glob:.#*", // emacs
            "glob:*~", "glob:[._]*.s[a-w][a-z]", "glob:[._]s[a-w][a-z]", "glob:*.un~" // vim
        ))));
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
}
