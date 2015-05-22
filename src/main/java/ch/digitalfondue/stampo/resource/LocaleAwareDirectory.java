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

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.Optional;


public class LocaleAwareDirectory implements Directory {

  private final Locale locale;
  private final Directory directory;
  private final BiFunction<FileResource, Directory, FileResource> fileResourceWrapper;

  public LocaleAwareDirectory(Locale locale, Directory directory, BiFunction<FileResource, Directory, FileResource> fileResourceWrapper) {
    this.locale = locale;
    this.directory = directory;
    this.fileResourceWrapper = fileResourceWrapper;
  }

  @Override
  public Map<String, FileResource> getFiles() {
    Map<String, FileResource> filtered = new LinkedHashMap<>();
    for (Entry<String, FileResource> kv : directory.getFiles().entrySet()) {

      FileResource file = kv.getValue();

      FileMetadata metadata = file.getMetadata();

      Optional<Boolean> includeByMetadata = metadata.getOnlyForLocales()//
          .map(l -> l.contains(locale));
      
      String formattedLocale = locale.toString();
      
      Set<String> localesInFileExt = file.getStructuredFileExtension().getLocales();

      boolean includeByFileExtension = !localesInFileExt.isEmpty() ? file.getStructuredFileExtension().getLocales().contains(formattedLocale) : true;

      if (includeByMetadata.orElse(includeByFileExtension)) {
        filtered.put(kv.getKey(), fileResourceWrapper.apply(kv.getValue(), this));
      }
    }
    return filtered;
  }

  @Override
  public Map<String, Directory> getDirectories() {
    Map<String, Directory> wrapped = new LinkedHashMap<>();
    for (Entry<String, Directory> k : directory.getDirectories().entrySet()) {
      wrapped.put(k.getKey(), new LocaleAwareDirectory(locale, k.getValue(), fileResourceWrapper));
    }
    return wrapped;
  }

  @Override
  public Resource getParent() {
    return new LocaleAwareDirectory(locale, (Directory) directory.getParent(), fileResourceWrapper);
  }

  @Override
  public Path getPath() {
    return directory.getPath();
  }
}
