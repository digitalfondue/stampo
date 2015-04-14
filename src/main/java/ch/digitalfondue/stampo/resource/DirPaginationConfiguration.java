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

public class DirPaginationConfiguration extends PaginationConfiguration {

  private final List<String> matchPattern;
  private final String baseDirectory;
  private final boolean recursive;

  public DirPaginationConfiguration(String baseDirectory, List<String> matchPattern,
      int pageSize, boolean recursive) {
    super(pageSize);
    this.baseDirectory = baseDirectory;
    this.matchPattern = matchPattern;
    this.recursive = recursive;
  }

  public boolean isRecursive() {
    return recursive;
  }

  public String getBaseDirectory() {
    return baseDirectory;
  }

  public List<String> getMatchPattern() {
    return matchPattern;
  }
}
