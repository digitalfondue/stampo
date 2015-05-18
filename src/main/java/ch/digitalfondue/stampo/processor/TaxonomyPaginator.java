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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

import ch.digitalfondue.stampo.StampoGlobalConfiguration;
import ch.digitalfondue.stampo.processor.paginator.PaginationConfiguration;
import ch.digitalfondue.stampo.processor.paginator.Paginator;
import ch.digitalfondue.stampo.resource.Directory;
import ch.digitalfondue.stampo.resource.FileResource;
import ch.digitalfondue.stampo.taxonomy.Taxonomy;

public class TaxonomyPaginator extends Paginator implements Directive {

  private static final String METADATA_PAGINATE_OVER_TAXONOMY = "paginate-over-taxonomy";

  public TaxonomyPaginator(
      Directory root,
      StampoGlobalConfiguration configuration,
      Function<FileResource, Path> outputPathExtractor,
      Function<Locale, BiFunction<FileResource, Map<String, Object>, FileResourceProcessorOutput>> resourceProcessor,
      Taxonomy taxonomy) {
    super(root, configuration, outputPathExtractor, resourceProcessor, taxonomy);
  }

  private Optional<TaxonomyPaginationConfiguration> getTaxonomyPaginationConfiguration(
      Map<String, Object> metadata) {
    return ofNullable(metadata.get(METADATA_PAGINATE_OVER_TAXONOMY)).map(Object::toString).map(
        taxonomy -> {
          int pageSize =
              ofNullable(metadata.get(METADATA_PAGINATE_PAGE_SIZE)).map(Integer.class::cast)
                  .orElse(DEFAULT_PAGE_SIZE);
          return new TaxonomyPaginationConfiguration(taxonomy, pageSize);
        });
  }


  @Override
  public List<PathAndModelSupplier> generateOutputPaths(FileResource resource, Locale locale,
      Path defaultOutputPath) {
    Path parentDir = defaultOutputPath.getParent();

    List<PathAndModelSupplier> outpuPaths = new ArrayList<>();

    TaxonomyPaginationConfiguration conf =
        getTaxonomyPaginationConfiguration(resource.getMetadata().getRawMap()).orElseThrow(
            IllegalArgumentException::new);

    Map<String, Map<String, List<FileResource>>> groups = taxonomy.getGroups();

    if (groups.containsKey(conf.getTaxonomy())) {
      outpuPaths.addAll(handleTaxonomyGroup(resource, locale, parentDir,
          groups.get(conf.getTaxonomy()), conf));
    }

    return outpuPaths;
  }


  private List<PathAndModelSupplier> handleTaxonomyGroup(FileResource resource, Locale locale,
      Path baseDir, Map<String, List<FileResource>> map, TaxonomyPaginationConfiguration conf) {

    List<PathAndModelSupplier> toAdd = new ArrayList<>();

    for (Entry<String, List<FileResource>> tagFiles : map.entrySet()) {
      String finalDirName = tagFiles.getKey() + "/index.html";
      toAdd.addAll(registerPaths(tagFiles.getValue(), baseDir.resolve(finalDirName),
          conf.getPageSize(), resource, path -> (f -> toPageContent(f, locale, path))));
    }

    return toAdd;
  }


  @Override
  public String name() {
    return "taxonomy-pagination";
  }
  
  private static class TaxonomyPaginationConfiguration extends PaginationConfiguration {

    private final String taxonomy;

    public TaxonomyPaginationConfiguration(String taxonomy, int pageSize) {
      super(pageSize);
      this.taxonomy = taxonomy;
    }

    public String getTaxonomy() {
      return taxonomy;
    }
  }
}
