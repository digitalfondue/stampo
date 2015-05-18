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
package ch.digitalfondue.stampo.processor.paginator;

import java.util.List;
import java.util.function.BiFunction;

public class Page<T> {

  private final long currentPage;
  private final long pageSize;
  private final long pageCount;
  private final long totalItemCount;
  private final BiFunction<Long, Long, String> pageNameGenerator;
  private final List<T> pageContent;

  public Page(long currentPage, long pageSize, long pageCount, long totalItemCount,
      BiFunction<Long, Long, String> pageNameGenerator, List<T> pageContent) {
    this.currentPage = currentPage;
    this.pageSize = pageSize;
    this.pageCount = pageCount;
    this.totalItemCount = totalItemCount;
    this.pageNameGenerator = pageNameGenerator;
    this.pageContent = pageContent;
  }

  public long getCurrentPage() {
    return currentPage;
  }

  public long getPageSize() {
    return pageSize;
  }

  public long getPageCount() {
    return pageCount;
  }

  public long getTotalItemCount() {
    return totalItemCount;
  }

  public boolean isFirstPage() {
    return currentPage == 1;
  }

  public boolean isLastPage() {
    return currentPage == pageCount;
  }

  public String getPreviousPageRelativeLink() {
    return pageNameGenerator.apply(currentPage, currentPage - 1);
  }

  public String getNextPageRelativeLink() {
    return pageNameGenerator.apply(currentPage, currentPage + 1);
  }

  public List<T> getPageContent() {
    return pageContent;
  }
}