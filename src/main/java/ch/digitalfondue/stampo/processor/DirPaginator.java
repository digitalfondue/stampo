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
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import ch.digitalfondue.stampo.PathUtils;
import ch.digitalfondue.stampo.StampoGlobalConfiguration;
import ch.digitalfondue.stampo.exception.ConfigurationException;
import ch.digitalfondue.stampo.resource.DirPaginationConfiguration;
import ch.digitalfondue.stampo.resource.Directory;
import ch.digitalfondue.stampo.resource.FileMetadata;
import ch.digitalfondue.stampo.resource.FileResource;
import ch.digitalfondue.stampo.taxonomy.Taxonomy;

// TODO: refactor!
public class DirPaginator extends Paginator {

  private static final Comparator<FileResource> NEW_FILE_FIRST = Comparator.comparingLong(
      FileResource::getCreationTime).reversed();


  public DirPaginator(
      Directory root,
      StampoGlobalConfiguration configuration,
      Function<FileResource, Path> outputPathExtractor,
      Function<Locale, BiFunction<FileResource, Map<String, Object>, FileResourceProcessorOutput>> resourceProcessor,
      Taxonomy taxonomy) {
    super(root, configuration, outputPathExtractor, resourceProcessor, taxonomy);
  }



  public List<PathAndModelSupplier> handlePagination(FileResource resource, Locale locale,
      Path defaultOutputPath) {

    List<PathAndModelSupplier> outpuPaths = new ArrayList<PathAndModelSupplier>();

    DirPaginationConfiguration dirPaginationConf = resource.getMetadata()//
        .getDirectoryPaginationConfiguration().get();

    Path targetDirPath = configuration.getBaseDirectory()//
        .resolve(leftTrimSlash(dirPaginationConf.getBaseDirectory()));

    checkTargetDirectory(resource, dirPaginationConf, targetDirPath);

    if (targetDirPath.startsWith(configuration.getContentDir())) {

      outpuPaths.addAll(handleContentDir(resource, locale, defaultOutputPath, dirPaginationConf,
          targetDirPath));

    } else if (targetDirPath.startsWith(configuration.getStaticDir())) {
      try {
        outpuPaths.addAll(handleStaticDir(resource, defaultOutputPath, dirPaginationConf,
            targetDirPath));
      } catch (IOException ioe) {
        throw new IllegalStateException(ioe);
      }
    } else {
      throw new ConfigurationException(resource.getPath(),
          "cannot paginate outside static or content directory, directory used is " + targetDirPath);
    }
    return outpuPaths;
  }
  
  private static String leftTrimSlash(String s) {
    return s.startsWith("/") ? s.substring(1) : s;
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

  private Predicate<Path> matchPattern(DirPaginationConfiguration dirPaginationConf) {
    FileSystem inputFs = configuration.getBaseDirectory().getFileSystem();

    List<PathMatcher> matchers =
        dirPaginationConf.getMatchPattern().stream().map(inputFs::getPathMatcher)
            .collect(Collectors.toList());

    Path baseDir = configuration.getBaseDirectory();

    return matchers.isEmpty() ? (Path p) -> true : (Path p) -> matchers.stream().anyMatch(
        (matcher) -> matcher.matches(baseDir.relativize(p)));
  }

  private static void recurAddFileResources(Directory dir, List<FileResource> fr) {
    fr.addAll(dir.getFiles().values());
    for (Directory childDir : dir.getDirectories().values()) {
      recurAddFileResources(childDir, fr);
    }
  }

  private static Collection<FileResource> extractFilesFrom(Directory dir,
      DirPaginationConfiguration dirPaginationConf) {
    if (dirPaginationConf.isRecursive()) {
      List<FileResource> fr = new ArrayList<>();
      recurAddFileResources(dir, fr);
      fr.sort(NEW_FILE_FIRST);
      return fr;
    } else {
      return dir.getFiles().values();
    }
  }


  private List<PathAndModelSupplier> handleContentDir(FileResource resource, Locale locale,
      Path defaultOutputPath, DirPaginationConfiguration dirPaginationConf, Path targetDirPath) {

    Directory dir =
        root.getDirectory(configuration.getContentDir().relativize(targetDirPath)).orElseThrow(
            () -> new ConfigurationException(resource.getPath(), "cannot find the directory '"
                + targetDirPath + "' to paginate over"));

    Predicate<Path> patternFilter = matchPattern(dirPaginationConf);

    List<FileResource> files =
        extractFilesFrom(dir, dirPaginationConf)
            .stream()
            .filter(f -> {
              FileMetadata m = f.getMetadata();
              // filter out the files with pagination
                return !m.getTaxonomyPaginationConfiguration().isPresent()
                    && !m.getDirectoryPaginationConfiguration().isPresent();
              }).filter((f) -> patternFilter.test(f.getPath())).collect(toList());

    return registerPaths(files, defaultOutputPath, dirPaginationConf.getPageSize(), resource,
            path -> (f -> toPageContent(f, locale, path)));
  }

  private List<PathAndModelSupplier> handleStaticDir(FileResource resource, Path defaultOutputPath,
      DirPaginationConfiguration dirPaginationConf, Path targetDirPath) throws IOException {
    Comparator<Path> comparator = Comparator.comparing((Path p) -> p.getFileName().toString(),//
        new AlphaNumericStringComparator(Locale.ENGLISH)).reversed();

    int depth = dirPaginationConf.isRecursive() ? Integer.MAX_VALUE : 1;

    Path baseOutputDir = configuration.getBaseOutputDir();
    Path staticDir = configuration.getStaticDir();

    Predicate<Path> patternFilter = matchPattern(dirPaginationConf);

    List<Path> files =
        Files.walk(targetDirPath, depth).filter(Files::isRegularFile).filter(patternFilter)
            .sorted(comparator)
            .map(file -> baseOutputDir.resolve(staticDir.relativize(file).toString()))
            .collect(toList());

    List<PathAndModelSupplier> toAdd =
        registerPaths(files, defaultOutputPath, dirPaginationConf.getPageSize(), resource,
            path -> (f -> PathUtils.relativePathTo(f, path)));
    return toAdd;
  }



}
