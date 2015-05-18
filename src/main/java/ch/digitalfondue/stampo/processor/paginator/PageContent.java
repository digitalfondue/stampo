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

import ch.digitalfondue.stampo.resource.FileResource;

public class PageContent {
  private final FileResource resource;
  private final String renderedResource;
  private final String relativeUrlToContent;

  public PageContent(FileResource resource, String renderedResource, String relativeUrlToContent) {
    this.resource = resource;
    this.renderedResource = renderedResource;
    this.relativeUrlToContent = relativeUrlToContent;
  }

  public FileResource getResource() {
    return resource;
  }

  public String getRenderedResource() {
    return renderedResource;
  }

  public String getRelativeUrlToContent() {
    return relativeUrlToContent;
  }

}