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
package ch.digitalfondue.stampo.resource;

import static java.util.Optional.ofNullable;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class FileMetadata {

  public static final String METADATA_ONLY_FOR_LOCALES = "only-for-locales";
  public static final String METADATA_OVERRIDE_OUTPUT_TO_PATH = "override-output-to-path";
  public static final String METADATA_OVERRIDE_LOCALE = "override-locale";
  public static final String METADATA_OVERRIDE_LAYOUT = "override-layout";
  public static final String METADATA_OVERRIDE_USE_UGLY_URL = "override-use-ugly-url";
  
  public static final String METADATA_DIRECTIVE = "directive";

  public static final String METADATA_DATE = "date";
  

  private final Map<String, Object> metadata;
  //
  private final String directive;
  private final Optional<Date> date;
  private final Optional<List<Locale>> onlyForLocales;
  private final Optional<String> overrideOutputToPath;
  private final Optional<Locale> overrideLocale;
  private final Optional<String> overrideLayout;
  private final Optional<Boolean> overrideUseUglyUrl;
  //

  public FileMetadata(Map<String, Object> metadata) {
    this.metadata = metadata;
    //
    this.date = ofNullable(metadata.get(METADATA_DATE)).map(Date.class::cast);
    this.onlyForLocales = ofNullable(metadata.get(METADATA_ONLY_FOR_LOCALES)).map(TO_STRING_LIST).map(
        l -> l.stream().map(Locale::forLanguageTag).collect(Collectors.toList()));
    this.overrideOutputToPath = ofNullable(metadata.get(METADATA_OVERRIDE_OUTPUT_TO_PATH)).map(Object::toString);
    this.overrideLocale = ofNullable(metadata.get(METADATA_OVERRIDE_LOCALE)).map(Object::toString).map(Locale::forLanguageTag);
    this.overrideLayout = ofNullable(metadata.get(METADATA_OVERRIDE_LAYOUT)).map(Object::toString);
    this.overrideUseUglyUrl = ofNullable(metadata.get(METADATA_OVERRIDE_USE_UGLY_URL)).map(Boolean.class::cast);
    this.directive = ofNullable(metadata.get(METADATA_DIRECTIVE)).map(Object::toString).orElse("default");
    //
  }

  public Map<String, Object> getRawMap() {
    return metadata;
  }

  @SuppressWarnings("unchecked")
  private static final Function<Object, List<String>> TO_STRING_LIST = s -> {
    if (s instanceof String) {
      return Collections.singletonList(s.toString());
    } else if (s instanceof List) {
      return (List<String>) s;
    } else {
      throw new IllegalArgumentException("wrong type for locales: " + s);
    }
  };

  public Optional<Date> getDate() {
    return date;
  }

  public Optional<List<Locale>> getOnlyForLocales() {
    return onlyForLocales;
  }

  public Optional<String> getOverrideOutputToPath() {
    return overrideOutputToPath;
  }

  public Optional<Locale> getOverrideLocale() {
    return overrideLocale;
  }

  public Optional<String> getOverrideLayout() {
    return overrideLayout;
  }

  public Optional<Boolean> getOverrideUseUglyUrl() {
    return overrideUseUglyUrl;
  }
  
  public String getDirective() {
    return directive;
  }
}
