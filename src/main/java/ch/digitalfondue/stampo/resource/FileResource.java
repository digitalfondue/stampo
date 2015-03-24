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
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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



  private static class Content {
    private final Map<String, Object> metadata;
    private final String textContent;

    Content(Map<String, Object> metadata, String textContent) {
      this.metadata = metadata;
      this.textContent = textContent;
    }
  }



  @SuppressWarnings("unchecked")
  private static Content readContent(String content) {

    Pattern p = Pattern.compile("^---$", Pattern.MULTILINE);

    Matcher m = p.matcher(content);
    if (m.find()) {

      int findStart1 = m.start();
      int findEnd1 = m.end();

      if (content.substring(0, findStart1).trim().isEmpty() && m.find()) {
        int findStart2 = m.start();
        int findEnd2 = m.end();

        // we remove the new line after the last "---"
        String leftTrimmedContent = content.substring(findEnd2 + System.lineSeparator().length());

        return new Content(Optional.ofNullable(
            new Yaml().loadAs(content.substring(findEnd1, findStart2), Map.class)).orElse(
            Collections.emptyMap()), leftTrimmedContent);
      }
    }


    return new Content(Collections.emptyMap(), content);
  }

  //
  private Content readContent() {
    try {
      return readContent(new String(Files.readAllBytes(path), StandardCharsets.UTF_8));
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
