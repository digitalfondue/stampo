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

import static com.google.common.io.Files.getFileExtension;
import static com.google.common.io.Files.getNameWithoutExtension;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.yaml.snakeyaml.Yaml;

import ch.digitalfondue.stampo.StampoGlobalConfiguration;


public class FileResource implements Resource {

  private final Path path;
  private final Resource parent;
  private final StampoGlobalConfiguration configuration;

  // caching the metadata, as this section is small
  private final FileMetadata metadata;

  public FileResource(StampoGlobalConfiguration configuration, Path path, Resource parent) {
    this.configuration = configuration;
    this.path = path;
    this.parent = parent;
    this.metadata = new FileMetadata(readContent().metadata);
  }
  
  public FileResource(FileResource fileResource, Resource parent) {
    this.configuration = fileResource.configuration;
    this.path = fileResource.path;
    this.parent = parent;
    this.metadata = fileResource.metadata;
  }

  public boolean containLocaleInFileExtensions() {
    return getFileExtensions()
        .stream()
        .anyMatch(
            configuration.getLocales().stream().map(Object::toString).collect(Collectors.toList())::contains);
  }

  /**
   * File extensions, ordered last to first.
   * 
   * E.g.
   * 
   * <pre>
   * - test -> []
   * - test.txt -> [txt]
   * - test.txt.peb -> [peb, txt]
   * - test.en.txt.peb -> [peb, txt, en]
   * </pre>
   * 
   * */
  public List<String> getFileExtensions() {
    List<String> e = new ArrayList<String>(3);
    String p = path.toString();
    while (!"".equals(getFileExtension(p))) {
      e.add(getFileExtension(p));
      p = getNameWithoutExtension(p);
    }
    return Collections.unmodifiableList(e);
  }

  public String getFileNameWithoutExtensions() {
    String extension = getFileExtensions().stream().collect(Collectors.joining(".", ".", ""));
    return getNameWithoutExtension(path.toString().substring(0,
        path.toString().length() - extension.length()));
  }

  @Override
  public Resource getParent() {
    return parent;
  }

  @Override
  public Path getPath() {
    return path;
  }

  @Override
  public long getCreationTime() {
    return metadata.getDate().map(Date::getTime).orElseGet(() -> Resource.super.getCreationTime());
  }

  public String getContent() {
    return readContent().textContent;
  }

  public FileMetadata getMetadata() {
    return metadata;
  }

  /**
   * State machine for parsing a resource that can potentially have an embedded metadata section.
   */
  private static class Context {
    private final Iterator<String> it;
    private final StringBuilder beforePossibleMetadaSection = new StringBuilder();
    private final StringBuilder possibleMetadataSection = new StringBuilder();
    private final StringBuilder content = new StringBuilder();
    private State state = State.SKIP_EMPTY_LINES;

    public Context(Iterator<String> it) {
      this.it = it;
    }

    public String getTextContent() {
      if (state == State.NO_SEPARATOR) {
        return new StringBuilder(beforePossibleMetadaSection.toString())
            .append(possibleMetadataSection.toString()).append(content.toString()).toString();
      } else if (state == State.END_SEPARATOR) {
        return content.toString();
      } else {
        throw new IllegalStateException("cannot get text content in the current state");
      }
    }
  }

  private static class Content {
    private final Map<String, Object> metadata;
    private final String textContent;

    Content(Map<String, Object> metadata, String textContent) {
      this.metadata = metadata;
      this.textContent = textContent;
    }
  }

  private enum State {
    SKIP_EMPTY_LINES {
      boolean process(Context ctx) {
        while (ctx.it.hasNext()) {
          String line = ctx.it.next();
          ctx.beforePossibleMetadaSection.append(line).append("\n");
          if ("".equals(line.trim())) {
            continue;
          } else if ("---".equals(line)) {
            ctx.state = BEGIN_SEPARATOR;
            return true;
          } else {
            ctx.state = NO_SEPARATOR;
            return true;
          }
        }
        ctx.state = NO_SEPARATOR;
        return true;
      }
    },
    BEGIN_SEPARATOR {
      boolean process(Context ctx) {
        while (ctx.it.hasNext()) {
          String line = ctx.it.next();
          if ("---".equals(line)) {
            ctx.state = END_SEPARATOR;
            return true;
          } else {
            ctx.possibleMetadataSection.append(line).append("\n");
          }
        }
        ctx.state = NO_SEPARATOR;
        return true;
      }
    },
    END_SEPARATOR {
      boolean process(Context ctx) {
        readAllLines(ctx);
        return false;
      }
    },
    NO_SEPARATOR {
      boolean process(Context ctx) {
        readAllLines(ctx);
        return false;
      }
    };

    abstract boolean process(Context ctx);

    void readAllLines(Context ctx) {
      while (ctx.it.hasNext()) {
        String line = ctx.it.next();
        ctx.content.append(line).append("\n");
      }
    }
  }


  // TODO: add mode READ_ONLY_METADATA and READ_ONLY_CONTENT for possible optimization case
  private Content readContent() {
    try (Stream<String> str = Files.lines(path, StandardCharsets.UTF_8)) {
      Context context = new Context(str.iterator());
      while (context.state.process(context));
      @SuppressWarnings("unchecked")
      Map<String, Object> metadata =
          context.state == State.END_SEPARATOR ? (Map<String, Object>) new Yaml().loadAs(
              context.possibleMetadataSection.toString(), Map.class) : Collections.emptyMap();
      return new Content(metadata, context.getTextContent());
    } catch (IOException ioe) {
      throw new IllegalStateException(ioe);
    }
  }


  public static FileResource getContentFileResource(Path template, Path contentDir, Directory root) {
    if (!template.startsWith(contentDir)) {
      throw new IllegalArgumentException();
    }
    Path relativeContent = contentDir.relativize(template);
    Directory base = root;
    for (Iterator<Path> d = relativeContent.iterator(); d.hasNext();) {
      Path p = d.next();
      if (d.hasNext()) {
        base = base.getDirectories().get(p.toString());
      } else {
        return base.getFiles().get(p.toString());
      }
    }
    throw new IllegalArgumentException("not found");
  }

  public StampoGlobalConfiguration getConfiguration() {
    return configuration;
  }
}
