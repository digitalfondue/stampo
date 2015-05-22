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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import ch.digitalfondue.stampo.PathUtils;

public class Toc {
  private final List<Header> headers = new ArrayList<>();
  

  // for adding another toc root
  public void add(Toc toc) {
    headers.addAll(toc.headers);
  }

  public void add(int headerLevel, String name, String id, Path outputPath) {
    headers.add(new Header(headerLevel, name, outputPath));
  }

  public String toHtml(Path path) {
    StringBuilder sb = new StringBuilder();
    int lvl = 0;
    int opened = 0;
    for (Header h : headers) {
      if (h.level > lvl) {
        opened++;
        sb.append("<ol>");
        lvl = h.level;
      } else if (h.level < lvl) {
        sb.append("</ol>");
        opened--;
        lvl = h.level;
      }
      sb.append("<li>").append("<a href=\"").append(PathUtils.relativePathTo(h.outputPath, path))
          .append("\">").append(h.name).append("</a>").append("</li>");
    }

    for (int i = 0; i < opened; i++) {
      sb.append("</ol>");
    }

    return sb.toString();
  }

  static class Header {
    final int level;
    final String name;
    final Path outputPath;

    Header(int level, String name, Path outputPath) {
      this.level = level;
      this.name = name;
      this.outputPath = outputPath;
    }
  }
}
