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
package ch.digitalfondue.stampo.processor.includeall;

public class Pagination {
  private final int page;
  private final int total;
  private final int depth;
  private final String previousPageUrl;
  private final String previousPageTitle;
  private final String nextPageUrl;
  private final String nextPageTitle;

  public Pagination(int page, int total, int depth, String previousPageUrl, String previousPageTitle,
      String nextPageUrl, String nextPageTitle) {
    this.page = page;
    this.total = total;
    this.depth = depth;
    this.previousPageUrl = previousPageUrl;
    this.previousPageTitle = previousPageTitle;
    this.nextPageUrl = nextPageUrl;
    this.nextPageTitle = nextPageTitle;
  }

  public int getPage() {
    return page;
  }

  public int getTotal() {
    return total;
  }
  
  public int getDepth() {
    return depth;
  }

  public String getPreviousPageUrl() {
    return previousPageUrl;
  }

  public String getNextPageUrl() {
    return nextPageUrl;
  }

  public String getPreviousPageTitle() {
    return previousPageTitle;
  }

  public String getNextPageTitle() {
    return nextPageTitle;
  }
}