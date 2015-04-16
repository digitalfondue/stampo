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

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class StructuredFileExtension {
  
  private final List<String> processorRelatedExts;
  private final Optional<String> processorFileExtensionOverride;
  private final Optional<String> maybeFileExtension;
  private final Set<String> locales;
  private final String rest;
  
  
  public StructuredFileExtension(List<String> processorRelatedExts,
      Optional<String> processorFileExtensionOverride,
      Optional<String> maybeFileExtension, Set<String> locales, List<String> rest) {
    this.processorRelatedExts = processorRelatedExts;
    this.processorFileExtensionOverride = processorFileExtensionOverride;
    this.maybeFileExtension = maybeFileExtension;
    this.locales = locales;
    this.rest = rest.stream().collect(Collectors.joining("."));
  }
  
  public String getFinalFileExtension() {
    String finalExt = processorFileExtensionOverride.orElseGet(() -> maybeFileExtension.orElse(""));
    return (rest.length() > 0 ? "." : "") + rest + (finalExt.length() > 0 ? "." : "") + finalExt;
  }

  public List<String> getProcessorRelatedExts() {
    return processorRelatedExts;
  }

  public Optional<String> getProcessorFileExtensionOverride() {
    return processorFileExtensionOverride;
  }

  public Optional<String> getMaybeFileExtension() {
    return maybeFileExtension;
  }

  public Set<String> getLocales() {
    return locales;
  }

  public String getRest() {
    return rest;
  }
}