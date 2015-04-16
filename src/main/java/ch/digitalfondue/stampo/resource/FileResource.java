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


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import ch.digitalfondue.stampo.StampoGlobalConfiguration;

public interface FileResource extends Resource {
  
  StampoGlobalConfiguration getConfiguration();
  FileMetadata getMetadata();
  Optional<String> getContent();
  StructuredFileExtension getStructuredFileExtension();

  /**
   * File extensions
   * 
   * E.g.
   * 
   * <pre>
   * - test -> []
   * - test.txt -> [txt]
   * - test.txt.peb -> [txt, peb]
   * - test.en.txt.peb -> [en, txt, peb]
   * </pre>
   * 
   * */
  default List<String> getFileExtensions() {
    String fileName = getPath().getFileName().toString();
    String extensions = fileName.substring(getFileNameWithoutExtensions().length());
    return Arrays.stream(extensions.split("\\.")).filter(s->s.length() > 0).collect(Collectors.toCollection(ArrayList::new));
  }
  
  default String getFileNameWithoutExtensions() {
    String fileName = getPath().getFileName().toString();
    int idx = fileName.indexOf('.');
    return idx == -1 ? fileName : fileName.substring(0, idx);
  }
  
  default long getCreationTime() {
    return getMetadata().getDate().map(Date::getTime).orElseGet(() -> Resource.super.getCreationTime());
  }
}
