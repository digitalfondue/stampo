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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.yaml.snakeyaml.Yaml;

import ch.digitalfondue.stampo.StampoGlobalConfiguration;


public class FileResourceWithMetadataSection implements FileResource {

  private final Path path;
  private final Resource parent;
  private final StampoGlobalConfiguration configuration;
  private final StructuredFileExtension structuredFileExtension;

  // caching the metadata, as this section is small
  private final FileMetadata metadata;

  public FileResourceWithMetadataSection(StampoGlobalConfiguration configuration, Path path, Resource parent) {
    this.configuration = configuration;
    this.path = path;
    this.parent = parent;
    this.metadata = new FileMetadata(readContent(ReadMode.ONLY_METADATA).metadata);
    this.structuredFileExtension = classifyFileExtension();
  }

  public FileResourceWithMetadataSection(FileResource fileResource, Resource parent) {
    this.configuration = fileResource.getConfiguration();
    this.path = fileResource.getPath();
    this.parent = parent;
    this.metadata = fileResource.getMetadata();
    this.structuredFileExtension = fileResource.getStructuredFileExtension();
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
  public Optional<String> getContent() {
    return Optional.ofNullable(readContent(ReadMode.ONLY_TEXT).textContent);
  }

  @Override
  public FileMetadata getMetadata() {
    return metadata;
  }
  
  @Override
  public StampoGlobalConfiguration getConfiguration() {
    return configuration;
  }

  private static class Content {
    private final Map<String, Object> metadata;
    private final String textContent;

    Content(Map<String, Object> metadata, String textContent) {
      this.metadata = metadata;
      this.textContent = textContent;
    }
  }
  
  private static Optional<String> charAt(String s, int idx) {
    try {
      return Optional.of(Character.toString(s.charAt(idx)));
    } catch (IndexOutOfBoundsException e) {
      return Optional.empty();
    }
  }

  @SuppressWarnings("unchecked")
  private static Content readContent(String content, ReadMode mode) {

    Pattern p = Pattern.compile("^---$", Pattern.MULTILINE);
    
    Map<String, Object> metadata = Collections.emptyMap();
    String contentInFile = content;

    Matcher m = p.matcher(content);
    if (m.find()) {

      int findStart1 = m.start();
      int findEnd1 = m.end();

      if (content.substring(0, findStart1).trim().isEmpty() && m.find()) {
        int findStart2 = m.start();
        int findEnd2 = m.end();
        
        String next2Chars = charAt(content, findEnd2).orElse("") + charAt(content, findEnd2 + 1).orElse("");
        
        int offset = next2Chars.equals("\r\n") ? 2 : (next2Chars.startsWith("\n") ||  next2Chars.startsWith("\r") ? 1 : 0);
        
        // we remove the new line after the last "---"
        contentInFile = content.substring(findEnd2 + offset);
        
        if (mode == ReadMode.ONLY_METADATA) {
          metadata =
              Optional.ofNullable(
                  new Yaml().loadAs(content.substring(findEnd1, findStart2), Map.class)).orElse(
                  Collections.emptyMap());
        }
      } 
    } 


    return new Content(metadata, contentInFile);
  }
  
  private enum ReadMode {
    ONLY_METADATA, ONLY_TEXT
  }

  private Content readContent(ReadMode mode) {
    try {
      return readContent(new String(Files.readAllBytes(path), StandardCharsets.UTF_8), mode);
    } catch (IOException ioe) {
      throw new IllegalStateException(ioe);
    }
  }
 
  /* given a list of extensions, return a structured view */
  private StructuredFileExtension classifyFileExtension() {
    List<String> exts = getFileExtensions();
    Collections.reverse(exts);
    //
    
    List<String> processorRelated = takeWhile(exts, configuration.getProcessorResourceExtensions()::contains);
    
    //e.g. : md -> html mapping is present 
    Optional<String> processorFileExtensionOverride = processorRelated.stream().findFirst().map(configuration.getProcessorExtensionTransformMapping()::get);
    
    List<String> afterProcessorRelated = exts.subList(processorRelated.size(), exts.size());
    
    // if the next token does not match a locale, it's possibly a file extension
    Optional<String> maybeFileExtension = afterProcessorRelated.stream().findFirst().filter(ext -> !configuration.getLocalesAsString().contains(ext));
    
    List<String> afterMaybeFileExtension = afterProcessorRelated.subList(maybeFileExtension.isPresent() ? 1 : 0, afterProcessorRelated.size());
    List<String> locales = takeWhile(afterMaybeFileExtension, (ext -> configuration.getLocalesAsString().contains(ext)));
    List<String> afterLocales = new ArrayList<>(afterMaybeFileExtension.subList(locales.size(), afterMaybeFileExtension.size()));
    Collections.reverse(afterLocales);
    
    return new StructuredFileExtension(processorRelated, processorFileExtensionOverride, maybeFileExtension, new HashSet<>(locales), afterLocales);
  }

  private static <T> List<T> takeWhile(List<T> l, Predicate<T> pred) {
    List<T> r = new ArrayList<>(l.size());
    for (T i : l) {
      if(pred.test(i)) {
        r.add(i);
      } else {
        return r;
      }
    }
    return r;
  }

  @Override
  public StructuredFileExtension getStructuredFileExtension() {
    return structuredFileExtension;
  }
}
