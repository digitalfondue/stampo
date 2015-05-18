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

import static java.util.Optional.empty;
import static java.util.Optional.of;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import ch.digitalfondue.stampo.PathUtils;

public class Toc {
  final List<Toc> toc = new ArrayList<>();
  final Optional<Integer> baseDepth;
  final Optional<Integer> headerLevel;
  final Optional<String> name;
  final Optional<String> id;
  final Path outputPath;

  public Toc(Optional<Integer> baseDepth, Optional<Integer> headerLevel, Optional<String> name,
      Optional<String> id, Path outputPath) {
    this.baseDepth = baseDepth;
    this.headerLevel = headerLevel;
    this.name = name;
    this.id = id;
    this.outputPath = outputPath;
  }

  //for adding another toc root
  public void add(Toc toc) {
    if (!toc.baseDepth.isPresent()) {
      throw new IllegalStateException("Cannot add non root Toc");
    }

    if (toc.baseDepth.get().intValue() - 1 == baseDepth.get().intValue()) {
      // it's a direct children
      this.toc.add(toc);
    } else if (toc.baseDepth.get().intValue() > baseDepth.get().intValue() && !this.toc.isEmpty()
        && this.toc.get(this.toc.size() - 1).baseDepth.isPresent()) {
      // it's a children of the latest children
      this.toc.get(this.toc.size() - 1).add(toc);
    } else {
      throw new IllegalStateException("Cannot add toc");
    }
  }

  public void add(int headerLevel, String name, String id) {
    if (!toc.isEmpty() && toc.get(toc.size() - 1).headerLevel.isPresent()
        && toc.get(toc.size() - 1).headerLevel.get() < headerLevel) {
      toc.get(toc.size() - 1).add(headerLevel, name, id);
    } else {
      this.toc.add(new Toc(empty(), of(headerLevel), of(name), of(id), outputPath));
    }
  }

  public String toHtml(Path path) {
    StringBuilder sb = new StringBuilder();
    name.ifPresent((n) -> {
      sb.append("<a href=\"").append(PathUtils.relativePathTo(outputPath, path));
      id.ifPresent(i -> {
        sb.append("#").append(i);
      });
      sb.append("\">").append(n).append("</a>");
    });
    if (!toc.isEmpty()) {
      sb.append("\n<ol>");
      toc.stream().forEach(t -> sb.append("<li>").append(t.toHtml(path)).append("</li>\n"));
      sb.append("</ol>\n");
    }
    return sb.toString();
  }

  public Toc copy() {
    Toc toc = new Toc(baseDepth, headerLevel, name, id, outputPath);
    toc.toc.addAll(this.toc.stream().map(Toc::copy).collect(Collectors.toList()));
    return toc;
  }
}