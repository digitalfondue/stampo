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
package ch.digitalfondue.stampo.processor;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

import ch.digitalfondue.stampo.PathUtils;
import ch.digitalfondue.stampo.StampoGlobalConfiguration;
import ch.digitalfondue.stampo.processor.includeall.FlattenedStructuredDocument;
import ch.digitalfondue.stampo.processor.includeall.OutputPathsEnv;
import ch.digitalfondue.stampo.processor.includeall.Pagination;
import ch.digitalfondue.stampo.processor.includeall.StructuredDocument;
import ch.digitalfondue.stampo.resource.Directory;
import ch.digitalfondue.stampo.resource.DirectoryResource;
import ch.digitalfondue.stampo.resource.FileResource;
import ch.digitalfondue.stampo.resource.FileResourceWithMetadataSection;
import ch.digitalfondue.stampo.resource.LocaleAwareDirectory;
import ch.digitalfondue.stampo.resource.ResourceFactory;
import ch.digitalfondue.stampo.resource.RootResource;
import ch.digitalfondue.stampo.taxonomy.Taxonomy;

public class IncludeAllPaginator implements Directive {

  private final Directory root;
  private final StampoGlobalConfiguration configuration;
  private final Function<FileResource, Path> outputPathExtractor;
  private final Function<Locale, BiFunction<FileResource, Map<String, Object>, FileResourceProcessorOutput>> resourceProcessor;
  private final ResourceFactory resourceFactory;
  private final Taxonomy taxonomy;

  public IncludeAllPaginator(
      Directory root,
      StampoGlobalConfiguration configuration,
      Function<FileResource, Path> outputPathExtractor,
      Function<Locale, BiFunction<FileResource, Map<String, Object>, FileResourceProcessorOutput>> resourceProcessor,
      Taxonomy taxonomy) {
    this.root = root;
    this.configuration = configuration;
    this.outputPathExtractor = outputPathExtractor;
    this.resourceProcessor = resourceProcessor;
    this.resourceFactory =
        new ResourceFactory(DirectoryResource::new, FileResourceWithMetadataSection::new,
            Comparator.comparing(FileResource::getPath), configuration);
    this.taxonomy = taxonomy;
  }

  @Override
  public String name() {
    return "include-all";
  }

  @Override
  public List<PathAndModelSupplier> generateOutputPaths(FileResource resource, Locale locale,
      Path defaultOutputPath) {

    String path =
        ofNullable(resource.getMetadata().getRawMap().get("include-all")).map(String.class::cast)
            .orElseThrow(IllegalArgumentException::new);

    Path baseDirectory = configuration.getBaseDirectory();
    Path p = baseDirectory.resolve(path);
    if (!p.startsWith(baseDirectory)) {
      throw new IllegalArgumentException(p + " must be inside of the basedirectory: "
          + baseDirectory);// cannot be outside
    }

    Directory toIncludeAllDir =
        new LocaleAwareDirectory(locale, new RootResource(resourceFactory, p),
            FileResourceWithMetadataSection::new);

    StructuredDocument doc = new StructuredDocument(0, toIncludeAllDir, p);

    int maxDepth =
        (Integer) resource.getMetadata().getRawMap().getOrDefault("paginate-at-depth", 1);

    List<FlattenedStructuredDocument> res = new ArrayList<>();
    doc.toOutputPaths(new OutputPathsEnv(maxDepth, locale, resource, configuration, root, outputPathExtractor, resourceProcessor, taxonomy), res);

    addPaginationInfoToModel(res);

    return res.stream().map(f -> new PathAndModelSupplier(f.path, () -> f.model)).collect(toList());
  }

  private void generateToc(List<FlattenedStructuredDocument> res, int from) {
    FlattenedStructuredDocument base = res.get(from);
    int minDepth = base.depth;
    for (int i = from + 1; i < res.size(); i++) {
      FlattenedStructuredDocument current = res.get(i);
      if (minDepth >= current.depth) {
        break;
      }
      base.tocRoot.add(current.tocRoot);
    }
  }

  private void addPaginationInfoToModel(List<FlattenedStructuredDocument> res) {
    final int resCount = res.size();
    for (int i = 0; i < resCount; i++) {
      FlattenedStructuredDocument current = res.get(i);
      
      generateToc(res, i);

      String previousPageUrl = null;
      String previousPageTitle = null;

      String nextPageUrl = null;
      String nextPageTitle = null;

      if (i > 0) {
        previousPageUrl = PathUtils.relativePathTo(res.get(i - 1).path, current.path);
        previousPageTitle = res.get(i - 1).title.orElse(null);
      }
      if (i < resCount - 1) {
        nextPageUrl = PathUtils.relativePathTo(res.get(i + 1).path, current.path);
        nextPageTitle = res.get(i + 1).title.orElse(null);
      }
      current.model.put("pagination", new Pagination(i + 1, resCount, previousPageUrl,
          previousPageTitle, nextPageUrl, nextPageTitle));
      current.model.put("toc", current.tocRoot.toHtml(current.path));
    }
  }

}
