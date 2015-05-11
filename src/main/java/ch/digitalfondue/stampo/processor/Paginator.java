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

import static java.util.stream.Collectors.toList;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import ch.digitalfondue.stampo.PathUtils;
import ch.digitalfondue.stampo.StampoGlobalConfiguration;
import ch.digitalfondue.stampo.resource.Directory;
import ch.digitalfondue.stampo.resource.FileResource;
import ch.digitalfondue.stampo.taxonomy.Taxonomy;

public class Paginator {
  
  private static final String PAGE_DIRECTORY_NAME = "page";
  protected static final String METADATA_PAGINATE_PAGE_SIZE = "paginate-page-size";
  protected static final int DEFAULT_PAGE_SIZE = 10;

  protected final Directory root;
  protected final StampoGlobalConfiguration configuration;
  protected final Function<FileResource, Path> outputPathExtractor;
  protected final Function<Locale, BiFunction<FileResource, Map<String, Object>, FileResourceProcessorOutput>> resourceProcessor;
  protected final Taxonomy taxonomy;

  public Paginator(
      Directory root,
      StampoGlobalConfiguration configuration,
      Function<FileResource, Path> outputPathExtractor,
      Function<Locale, BiFunction<FileResource, Map<String, Object>, FileResourceProcessorOutput>> resourceProcessor,
      Taxonomy taxonomy) {
    this.root = root;
    this.configuration = configuration;
    this.outputPathExtractor = outputPathExtractor;
    this.resourceProcessor = resourceProcessor;
    this.taxonomy = taxonomy;
  }

  protected <T, R> List<PathAndModelSupplier> registerPaths(List<T> files, Path defaultOutputPath,
      long pageSize, FileResource resource, Function<Path, Function<T, R>> contentMapper) {

    Path basePageDir = defaultOutputPath.getParent().resolve(PAGE_DIRECTORY_NAME);

    List<PathAndModelSupplier> outpuPaths = new ArrayList<>();
    long count = files.size();

    // -1, as we don't count the base page
    long additionalPages = Math.max((count / pageSize + (count % pageSize > 0 ? 1 : 0)) - 1, 0);

    Supplier<Map<String, Object>> indexPageModelSupplier =
        prepareModelSupplier(1, pageSize, additionalPages, files, defaultOutputPath, resource,
            contentMapper.apply(defaultOutputPath));
    outpuPaths.add(new PathAndModelSupplier(defaultOutputPath, indexPageModelSupplier));

    for (int i = 0; i < additionalPages; i++) {
      Path pageOutputPath = basePageDir.resolve(pageName(i + 2, resource));
      Supplier<Map<String, Object>> pageModelSupplier =
          prepareModelSupplier(i + 2, pageSize, additionalPages, files, defaultOutputPath,
              resource, contentMapper.apply(pageOutputPath));
      outpuPaths.add(new PathAndModelSupplier(pageOutputPath, pageModelSupplier));
    }

    return outpuPaths;
  }

  private <T, T1> Supplier<Map<String, Object>> prepareModelSupplier(long currentPage,
      long pageSize, long additionalPagesCount, List<T> content, Path defaultOutputPath,
      FileResource fileResource, Function<T, T1> contentMapper) {
    return () -> {
      Map<String, Object> model = new HashMap<>();
      List<T1> pageContent = content.stream().skip((currentPage - 1) * pageSize)//
          .limit(pageSize).map(contentMapper).collect(toList());
      BiFunction<Long, Long, String> paginationFunction =
          urlPaginationGenerator(defaultOutputPath, fileResource);
      model.put("pagination",
          new Page<>(currentPage, pageSize, additionalPagesCount + 1, content.size(),
              paginationFunction, pageContent));
      return model;
    };
  }

  private static String removeIndexHtml(String s) {
    return s.replaceFirst("/index\\.html$", "/");
  }

  private boolean useUglyUrl(FileResource fileResource) {
    return fileResource.getMetadata().getOverrideUseUglyUrl().orElse(configuration.useUglyUrl());
  }

  private String pageName(long pageNumber, FileResource fileResource) {
    if (useUglyUrl(fileResource)) {
      return pageNumber + ".html";
    } else {
      return pageNumber + "/index.html";
    }
  }


  private BiFunction<Long, Long, String> urlPaginationGenerator(Path defaultOutputPath,
      FileResource fileResource) {
    return (currentPage, nextPageToLink) -> {
      // handle override + ugly url
      String url;
      if (currentPage == 1) {
        url = PAGE_DIRECTORY_NAME + "/" + pageName(nextPageToLink, fileResource);
      } else if (currentPage == 2 && nextPageToLink == 1) {
        url = (useUglyUrl(fileResource) ? "" : "../") + "../" + (defaultOutputPath.getFileName());
      } else {
        url = (useUglyUrl(fileResource) ? "" : "../") + pageName(nextPageToLink, fileResource);
      }
      return removeIndexHtml(url);
    };
  }


  protected PageContent toPageContent(FileResource fileResource, Locale locale, Path pagePath) {

    Map<String, Object> model =
        ModelPreparer.prepare(root, configuration, locale, fileResource, pagePath, taxonomy);

    FileResourceProcessorOutput processed =
        resourceProcessor.apply(locale).apply(fileResource, model);

    Path outputPath = outputPathExtractor.apply(fileResource);

    //
    if ("index.html".equals(outputPath.getFileName().toString())) {
      outputPath = outputPath.getParent();
    }

    if ("index.html".equals(pagePath.getFileName().toString())) {
      pagePath = pagePath.getParent();
    }
    //

    return new PageContent(fileResource, processed.getContent(), PathUtils.relativePathTo(
        outputPath, pagePath));
  }
}
