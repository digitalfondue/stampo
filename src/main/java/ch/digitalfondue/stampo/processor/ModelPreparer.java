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

import java.nio.file.Files;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import ch.digitalfondue.stampo.StampoGlobalConfiguration;
import ch.digitalfondue.stampo.resource.Directory;
import ch.digitalfondue.stampo.resource.DirectoryResource;
import ch.digitalfondue.stampo.resource.FileResource;
import ch.digitalfondue.stampo.resource.ResourceFactory;
import ch.digitalfondue.stampo.resource.RootResource;
import ch.digitalfondue.stampo.resource.StaticFileResource;

public class ModelPreparer {

  
  public static Map<String, Object> prepare(Directory root, StampoGlobalConfiguration configuration, Locale locale, FileResource resource, Map<String, Object> additionalData) {
    Map<String, Object> model = new HashMap<>();
    model.put("root", root);
    model.put("configuration", configuration);
    model.put("locale", locale);
    model.put("resource", resource);
    model.put("metadata", resource.getMetadata());
    
    model.putAll(additionalData);
    
    staticResources(configuration).ifPresent((staticRootResource) -> model.put("static", staticRootResource));
    
    return model;
  }
  
  
  static final Optional<RootResource> staticResources(StampoGlobalConfiguration configuration) {
    if(Files.exists(configuration.getStaticDir())) {
      Comparator<FileResource> comparator = Comparator.comparing(FileResource::getName, new AlphaNumericStringComparator(Locale.ENGLISH));
      ResourceFactory resourceFactory = new ResourceFactory(DirectoryResource::new, StaticFileResource::new, comparator, configuration);
      return Optional.of(new RootResource(resourceFactory, configuration.getStaticDir()));
    } else {
      return Optional.empty();
    }
  }
  
}
