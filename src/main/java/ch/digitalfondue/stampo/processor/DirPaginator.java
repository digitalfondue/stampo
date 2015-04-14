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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import ch.digitalfondue.stampo.StampoGlobalConfiguration;
import ch.digitalfondue.stampo.exception.ConfigurationException;
import ch.digitalfondue.stampo.resource.Directory;
import ch.digitalfondue.stampo.resource.DirPaginationConfiguration;
import ch.digitalfondue.stampo.resource.FileMetadata;
import ch.digitalfondue.stampo.resource.FileResource;

// TODO: refactor!
public class DirPaginator {

  private static final String PAGE_DIRECTORY_NAME = "page";

  private final Directory root;
  private final StampoGlobalConfiguration configuration;
  private final Function<FileResource, Path> outputPathExtractor;
  private final Function<Locale, BiFunction<FileResource, Map<String, Object>, FileResourceProcessorOutput>> resourceProcessor;

  public DirPaginator(
      Directory root,
      StampoGlobalConfiguration configuration,
      Function<FileResource, Path> outputPathExtractor,
      Function<Locale, BiFunction<FileResource, Map<String, Object>, FileResourceProcessorOutput>> resourceProcessor) {
    this.root = root;
    this.configuration = configuration;
    this.outputPathExtractor = outputPathExtractor;
    this.resourceProcessor = resourceProcessor;
  }



  public List<PathAndModelSupplier> handleDirPagination(FileResource resource, Locale locale,
      Path defaultOutputPath) {

    List<PathAndModelSupplier> outpuPaths = new ArrayList<PathAndModelSupplier>();

    DirPaginationConfiguration dirPaginationConf = resource.getMetadata()//
        .getDirectoryPaginationConfiguration().get();

    Path targetDirPath = configuration.getBaseDirectory()//
        .resolve(leftTrimSlash(dirPaginationConf.getBaseDirectory()));

    checkTargetDirectory(resource, dirPaginationConf, targetDirPath);

    if (targetDirPath.startsWith(configuration.getContentDir())) {
      
      outpuPaths.addAll(handleContentDir(resource, locale, defaultOutputPath, dirPaginationConf, targetDirPath));

    } else if (targetDirPath.startsWith(configuration.getStaticDir())) {
      try {
        
        outpuPaths.addAll(handleStaticDir(resource, defaultOutputPath, dirPaginationConf, targetDirPath));
      } catch (IOException ioe) {
        throw new IllegalStateException(ioe);
      }
    } else {
      throw new ConfigurationException(resource.getPath(),
          "cannot paginate outside static or content directory, directory used is " + targetDirPath);
    }
    return outpuPaths;
  }


  // TODO: apply filter, apply recursive
  private List<PathAndModelSupplier> handleContentDir(FileResource resource, Locale locale,
      Path defaultOutputPath, DirPaginationConfiguration dirPaginationConf, Path targetDirPath) {
    
    Directory dir =
        root.getDirectory(configuration.getContentDir().relativize(targetDirPath)).orElseThrow(
            () -> new ConfigurationException(resource.getPath(), "cannot find the directory '"
                + targetDirPath + "' to paginate over"));
    
    List<FileResource> files =
        dir.getFiles()
            .values()
            .stream()
            .filter(f -> {
              FileMetadata m = f.getMetadata();
              // filter out the files with pagination
                return !m.getTaxonomyPaginationConfiguration().isPresent()
                    && !m.getDirectoryPaginationConfiguration().isPresent();
              }).collect(toList());

    List<PathAndModelSupplier> toAdd =
        registerPaths(files, defaultOutputPath, dirPaginationConf.getPageSize(), resource,
            path -> (f -> toPageContent(f, locale, path)));
    return toAdd;
  }
  
  //TODO: apply filter
  private List<PathAndModelSupplier> handleStaticDir(FileResource resource, Path defaultOutputPath,
      DirPaginationConfiguration dirPaginationConf, Path targetDirPath) throws IOException {
    Comparator<Path> comparator = Comparator.comparing((Path p) -> p.getFileName().toString(),//
        new AlphaNumericStringComparator(Locale.ENGLISH)).reversed();

    int depth = dirPaginationConf.isRecursive() ? Integer.MAX_VALUE : 1;

    Path baseOutputDir = configuration.getBaseOutputDir();
    Path staticDir = configuration.getStaticDir();

    List<Path> files =
        Files.walk(targetDirPath, depth).filter(Files::isRegularFile).sorted(comparator)
            .map(file -> baseOutputDir.resolve(staticDir.relativize(file).toString()))
            .collect(toList());

    List<PathAndModelSupplier> toAdd =
        registerPaths(files, defaultOutputPath, dirPaginationConf.getPageSize(), resource,
            path -> (f -> toRelativeUrlToContent(f, path)));
    return toAdd;
  }


  private <T, R> List<PathAndModelSupplier> registerPaths(List<T> files, Path defaultOutputPath,
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


  private void checkTargetDirectory(FileResource resource,
      DirPaginationConfiguration dirPaginationConfiguration, Path targetDir) {
    if (!targetDir.startsWith(configuration.getBaseDirectory())) {
      throw new ConfigurationException(resource.getPath(),
          "selected directory for pagination must be inside target dir");
    }

    if (!Files.exists(targetDir)) {
      throw new ConfigurationException(resource.getPath(), "target directory "
          + dirPaginationConfiguration.getBaseDirectory() + " must exists");
    }
  }

  private static String leftTrimSlash(String s) {
    return s.startsWith("/") ? s.substring(1) : s;
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

  private String toRelativeUrlToContent(Path contentPath, Path pagePath) {
    if ("index.html".equals(contentPath.getFileName().toString())) {
      contentPath = contentPath.getParent();
    }

    if ("index.html".equals(pagePath.getFileName().toString())) {
      pagePath = pagePath.getParent();
    }

    return pagePath.relativize(contentPath).toString();
  }

  private PageContent toPageContent(FileResource fileResource, Locale locale, Path pagePath) {

    Map<String, Object> model =
        ModelPreparer.prepare(root, configuration, locale, fileResource, Collections.emptyMap());

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

    return new PageContent(fileResource, processed.getContent(), toRelativeUrlToContent(outputPath,
        pagePath));
  }


}
