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

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class FileMetadata {

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


  public Optional<List<Locale>> getOnlyForLocales() {
    return Optional.ofNullable(metadata.get("only-for-locales")).map(TO_STRING_LIST)
        .map((l) -> l.stream().map(Locale::forLanguageTag).collect(Collectors.toList()));
  }

  public Optional<String> getOverrideOutputToPath() {
    return Optional.ofNullable(metadata.get("override-output-to-path")).map(Object::toString);
  }

  public Optional<Locale> getOverrideLocale() {
    return Optional.ofNullable(metadata.get("override-locale")).map(Object::toString)
        .map(Locale::forLanguageTag);
  }

  public Set<String> getTags() {
    return toSet("tags");
  }

  public Set<String> getCategories() {
    return toSet("categories");
  }

  public Optional<String> getOverrideLayout() {
    return Optional.ofNullable(metadata.get("override-layout")).map(Object::toString);
  }

  private Set<String> toSet(String propertyName) {
    return new LinkedHashSet<>(Optional.ofNullable(metadata.get(propertyName)).map(TO_STRING_LIST)
        .orElse(Collections.emptyList()));
  }
}
