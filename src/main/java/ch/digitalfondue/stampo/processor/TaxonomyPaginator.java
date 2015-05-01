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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiFunction;
import java.util.function.Function;

import ch.digitalfondue.stampo.StampoGlobalConfiguration;
import ch.digitalfondue.stampo.resource.Directory;
import ch.digitalfondue.stampo.resource.FileResource;
import ch.digitalfondue.stampo.resource.TaxonomyPaginationConfiguration;
import ch.digitalfondue.stampo.taxonomy.Taxonomy;

public class TaxonomyPaginator extends Paginator {

  public TaxonomyPaginator(
      Directory root,
      StampoGlobalConfiguration configuration,
      Function<FileResource, Path> outputPathExtractor,
      Function<Locale, BiFunction<FileResource, Map<String, Object>, FileResourceProcessorOutput>> resourceProcessor,
      Taxonomy taxonomy) {
    super(root, configuration, outputPathExtractor, resourceProcessor, taxonomy);
  }


  public List<PathAndModelSupplier> handlePagination(FileResource resource, Locale finalLocale, Path defaultOutputPath) {
    Path parentDir = defaultOutputPath.getParent();
    
    List<PathAndModelSupplier> outpuPaths = new ArrayList<>();

    TaxonomyPaginationConfiguration conf =
        resource.getMetadata().getTaxonomyPaginationConfiguration().get();

    Map<String, Map<String, List<FileResource>>> groups = taxonomy.getGroups();
    
    if (groups.containsKey(conf.getTaxonomy())) {
      outpuPaths.addAll(handleTaxonomyGroup(resource, finalLocale, parentDir, groups.get(conf.getTaxonomy()), conf));
    }
  
    return outpuPaths;
  }


  private List<PathAndModelSupplier> handleTaxonomyGroup(FileResource resource, Locale finalLocale,
      Path baseDir, Map<String, List<FileResource>> map, TaxonomyPaginationConfiguration conf) {
    
    List<PathAndModelSupplier> toAdd = new ArrayList<>();
    
    for (Entry<String, List<FileResource>> tagFiles : map.entrySet()) {
      String finalDirName = tagFiles.getKey() + "/index.html";
      toAdd.addAll(registerPaths(tagFiles.getValue(), baseDir.resolve(finalDirName), conf.getPageSize(),
          resource, path -> (f -> toPageContent(f, finalLocale, path))));
    }
    
    return toAdd;
  }

}
