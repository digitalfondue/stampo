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
  
  public static final String METADATA_PAGINATE_OVER_DIRECTORY = "paginate-over-directory";
  public static final String METADATA_PAGINATE_OVER_TAXONOMY = "paginate-over-taxonomy";
  public static final String METADATA_PAGINATE_MATCH = "paginate-match";
  public static final String METADATA_PAGINATE_RECURSIVE = "paginate-recursive";
  public static final String METADATA_PAGINATE_PAGE_SIZE = "paginate-page-size";

  public static final String METADATA_DATE = "date";
  
  public static final int DEFAULT_PAGE_SIZE = 10;

  private final Map<String, Object> metadata;

  public FileMetadata(Map<String, Object> metadata) {
    this.metadata = metadata;
  }

  public Map<String, Object> getRawMap() {
    return metadata;
  }

  @SuppressWarnings("unchecked")
  private static final Function<Object, List<String>> TO_STRING_LIST = (s) -> {
    if (s instanceof String) {
      return Collections.singletonList(s.toString());
    } else if (s instanceof List) {
      return (List<String>) s;
    } else {
      throw new IllegalArgumentException("wrong type for locales: " + s);
    }
  };

  public Optional<Date> getDate() {
    return ofNullable(metadata.get(METADATA_DATE)).map(Date.class::cast);
  }

  public Optional<List<Locale>> getOnlyForLocales() {
    return ofNullable(metadata.get(METADATA_ONLY_FOR_LOCALES)).map(TO_STRING_LIST).map(
        (l) -> l.stream().map(Locale::forLanguageTag).collect(Collectors.toList()));
  }

  public Optional<String> getOverrideOutputToPath() {
    return ofNullable(metadata.get(METADATA_OVERRIDE_OUTPUT_TO_PATH)).map(Object::toString);
  }

  public Optional<Locale> getOverrideLocale() {
    return ofNullable(metadata.get(METADATA_OVERRIDE_LOCALE)).map(Object::toString).map(
        Locale::forLanguageTag);
  }

  public Optional<String> getOverrideLayout() {
    return ofNullable(metadata.get(METADATA_OVERRIDE_LAYOUT)).map(Object::toString);
  }

  public Optional<Boolean> getOverrideUseUglyUrl() {
    return ofNullable(metadata.get(METADATA_OVERRIDE_USE_UGLY_URL)).map(Boolean.class::cast);
  }
  
  public Optional<DirectoryPaginationConfiguration> getDirectoryPaginationConfiguration() {
    return ofNullable(metadata.get(METADATA_PAGINATE_OVER_DIRECTORY)).map(Object::toString).map((dir) -> {
      List<String> ignorePattern = ofNullable(metadata.get(METADATA_PAGINATE_MATCH)).map(TO_STRING_LIST).orElse(Collections.emptyList());
      int pageSize = ofNullable(metadata.get(METADATA_PAGINATE_PAGE_SIZE)).map(Integer.class::cast).orElse(DEFAULT_PAGE_SIZE);
      boolean recursive = ofNullable(metadata.get(METADATA_PAGINATE_RECURSIVE)).map(Boolean.class::cast).orElse(false);
      return new DirectoryPaginationConfiguration(dir, ignorePattern, pageSize, recursive);
    });
  }
  
  public Optional<TaxonomyPaginationConfiguration> getTaxonomyPaginationConfiguration() {
    return ofNullable(metadata.get(METADATA_PAGINATE_OVER_TAXONOMY)).map(TO_STRING_LIST).map(taxonomies -> {
      int pageSize = ofNullable(metadata.get(METADATA_PAGINATE_PAGE_SIZE)).map(Integer.class::cast).orElse(DEFAULT_PAGE_SIZE);
      return new TaxonomyPaginationConfiguration(taxonomies, pageSize);
    });
    
  }
}
