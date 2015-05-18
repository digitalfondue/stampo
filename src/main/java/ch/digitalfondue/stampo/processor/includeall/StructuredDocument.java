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
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import ch.digitalfondue.stampo.processor.ModelPreparer;
import ch.digitalfondue.stampo.resource.Directory;
import ch.digitalfondue.stampo.resource.FileResource;

public class StructuredDocument {
  
  private final int depth;
  private final Path relativeToBasePath;
  private final Optional<FileResource> fileResource;
  private final Optional<Directory> directory;
  private final List<StructuredDocument> childDocuments;


  public StructuredDocument(int depth, Directory directory, Path basePath) {
    this.depth = depth;
    this.fileResource = empty();
    this.directory = of(directory);
    this.childDocuments = from(depth, of(directory), basePath);
    this.relativeToBasePath = basePath.relativize(directory.getPath());
  }

  private StructuredDocument(int depth, FileResource resource, Optional<Directory> directory,
      Path basePath) {
    this.depth = depth;
    this.fileResource = of(resource);
    this.directory = directory;
    this.childDocuments = from(depth, directory, basePath);
    this.relativeToBasePath = basePath.relativize(resource.getPath());
  }

  public void toOutputPaths(OutputPathsEnv env, List<FlattenedStructuredDocument> res) {

    FileResource v =
        fileResource.orElseGet(() -> new FileResourcePlaceHolder(relativeToBasePath,
            env.configuration));

    Path virtualPath =
        env.resource.getPath().getParent().resolve(this.relativeToBasePath.toString());

    FileResource virtualResource = new VirtualPathFileResource(virtualPath, v);
    Path finalOutputPathForResource = env.outputPathExtractor.apply(virtualResource);

    StringBuilder sb = new StringBuilder();
    Map<String, Object> model =
        ModelPreparer.prepare(env.root, env.configuration, env.locale, virtualResource,
            finalOutputPathForResource, env.taxonomy);
    sb.append(renderFile(env, model));


    if (depth < env.maxDepth) {

      String includeAllResult = sb.toString();

      Map<String, Object> modelForSupplier =
          ModelPreparer.prepare(env.root, env.configuration, env.locale, virtualResource,
              finalOutputPathForResource, env.taxonomy,
              Collections.singletonMap("includeAllResult", includeAllResult));

      TocAndMainTitle tocRoot =
          extractTocFrom(depth, includeAllResult, finalOutputPathForResource);
      
      res.add(new FlattenedStructuredDocument(depth, finalOutputPathForResource,
          modelForSupplier, tocRoot.toc, tocRoot.title));

      childDocuments.forEach(sd -> sd.toOutputPaths(env, res));
    } else if (depth == env.maxDepth) {// cutoff point
      renderChildDocuments(env, model).forEach(sb::append);

      String includeAllResult = sb.toString();
      
      Map<String, Object> modelForSupplier =
          ModelPreparer.prepare(env.root, env.configuration, env.locale, virtualResource,
              finalOutputPathForResource, env.taxonomy,
              Collections.singletonMap("includeAllResult", includeAllResult));

      TocAndMainTitle tocRoot =
          extractTocFrom(depth, includeAllResult, finalOutputPathForResource);
      
      
      res.add(new FlattenedStructuredDocument(depth, finalOutputPathForResource,
          modelForSupplier, tocRoot.toc, tocRoot.title));
    }
  }

  private String renderFile(OutputPathsEnv env, Map<String, Object> model) {
    return fileResource.map(f -> env.resourceProcessor.apply(env.locale).apply(f, model).getContent())
        .orElse("");
  }


  private List<String> renderChildDocuments(OutputPathsEnv env, Map<String, Object> model) {
    return childDocuments
        .stream()
        .map(
            c -> c.renderFile(env, model)
                + c.renderChildDocuments(env, model).stream().collect(Collectors.joining()))
        .collect(toList());
  }


  private List<StructuredDocument> from(int depth, Optional<Directory> directory, Path basePath) {
    List<StructuredDocument> res = new ArrayList<>();

    return directory.map(
        (d) -> {

          Set<Path> alreadyVisisted = new HashSet<>();

          d.getFiles()
              .values()
              .forEach(
                  f -> {
                    Optional<Directory> associatedChildDir =
                        ofNullable(d.getDirectories().get(f.getFileNameWithoutExtensions()));
                    associatedChildDir.map(Directory::getPath).ifPresent(alreadyVisisted::add);
                    res.add(new StructuredDocument(depth + 1, f, associatedChildDir, basePath));
                  });


          d.getDirectories().values().stream()
              .filter(dir -> !alreadyVisisted.contains(dir.getPath())).forEach(dir -> {
                res.add(new StructuredDocument(depth + 1, dir, basePath));
              });

          Collections.sort(
              res,
              Comparator.comparing((StructuredDocument sd) -> sd.fileResource.map(
                  FileResource::getFileNameWithoutExtensions).orElseGet(
                  () -> sd.directory.map(Directory::getName).orElse(""))));


          return res;
        }).orElseGet(Collections::emptyList);
  }
  
  private static int headerLevel(String name) {
    return Integer.parseInt(name.substring(1));
  }
  
  private static TocAndMainTitle extractTocFrom(int depth, String s, Path finalOutputPathForResource) {
    Elements titles = Jsoup.parseBodyFragment(s).select("h1,h2,h3,h4,h5,h6");

    Toc root = new Toc(of(depth), empty(), empty(), empty(), finalOutputPathForResource);
    for (Element e : titles) {
   // FIXME add id, use path + e.text() as a id
      root.add(headerLevel(e.tagName()), e.text(), ""); 
    }
    return new TocAndMainTitle(root, titles.stream().findFirst().map(Element::text));
  }
}