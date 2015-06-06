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

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Stack;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import ch.digitalfondue.stampo.PathUtils;
import ch.digitalfondue.stampo.StampoGlobalConfiguration;
import ch.digitalfondue.stampo.resource.Directory;
import ch.digitalfondue.stampo.resource.DirectoryResource;
import ch.digitalfondue.stampo.resource.FileResource;
import ch.digitalfondue.stampo.resource.FileResourceWithMetadataSection;
import ch.digitalfondue.stampo.resource.LocaleAwareDirectory;
import ch.digitalfondue.stampo.resource.ResourceFactory;
import ch.digitalfondue.stampo.resource.RootResource;
import ch.digitalfondue.stampo.taxonomy.Taxonomy;

public class IncludeAllPaginator implements Directive {
  
  private static Comparator<FileOrDir> FILE_OR_DIR_COMPARATOR = Comparator.comparing(FileOrDir::getName, new AlphaNumericStringComparator(Locale.ENGLISH));

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
    this.resourceFactory = new ResourceFactory(DirectoryResource::new, FileResourceWithMetadataSection::new, Comparator.comparing(FileResource::getPath), configuration);
    this.taxonomy = taxonomy;
  }

  @Override
  public String name() {
    return "include-all";
  }

  @Override
  public List<PathAndModelSupplier> generateOutputPaths(FileResource resource, Locale locale,
      Path defaultOutputPath) {

    String path = ofNullable(resource.getMetadata().getRawMap().get("include-all")).map(String.class::cast)
            .orElseThrow(IllegalArgumentException::new);

    Path baseDirectory = configuration.getBaseDirectory();
    Path includeAllBasePath = baseDirectory.resolve(path);
    if (!includeAllBasePath.startsWith(baseDirectory)) {
      throw new IllegalArgumentException(includeAllBasePath
          + " must be inside of the basedirectory: " + baseDirectory);// cannot be outside
    }

    final Directory toIncludeAllDir;

    if (configuration.getLocales().size() > 1) {
      toIncludeAllDir = new LocaleAwareDirectory(locale, new RootResource(resourceFactory, includeAllBasePath), FileResourceWithMetadataSection::new);
    } else {
      toIncludeAllDir = new RootResource(resourceFactory, includeAllBasePath);
    }

    int maxDepth = (Integer) resource.getMetadata().getRawMap().getOrDefault("paginate-at-depth", 1);
    
    boolean includeChildSummary = (Boolean) resource.getMetadata().getRawMap().getOrDefault("include-child-summary", false); 


    List<IncludeAllPage> pages = flattenAndGroupRecursively(toIncludeAllDir, maxDepth, 1);
    
    List<IncludeAllPage> pagesWithDepth0Handled = handleDepth0Page(pages, resource, maxDepth, defaultOutputPath);
    
    List<IncludeAllPageWithOutput> pagesWithOutput = pagesWithDepth0Handled.stream()
            .map(iap -> addOutputInformation(iap, defaultOutputPath, includeAllBasePath, locale))
            .collect(Collectors.toList());
    
    List<IncludeAllPageWithPagination> pagesWithPagination = addPaginationInformation(pagesWithOutput, includeChildSummary);
    return pagesWithPagination.stream().map(fl -> toPathAndModuleSupplier(fl, locale)).collect(Collectors.toList());
  }

  /*
   * From a given directory, it will flatten the file hierarchy in a list of "pages" (That can contain more than one FileResource)
   */
  private List<IncludeAllPage> flattenAndGroupRecursively(Directory dir, int maxDepth, int depth) {

    List<FileOrDir> fileOrDirs = new ArrayList<>();

    Set<String> pairedDirectories = new HashSet<>();

    Map<String, Directory> childDirs = dir.getDirectories();
    Map<String, FileResource> childFiles = dir.getFiles();

    for (FileResource fr : childFiles.values()) {
      String fileNameWithoutExt = fr.getFileNameWithoutExtensions();
      Optional<Directory> pairedDir = childDirs.containsKey(fileNameWithoutExt) ? of(childDirs.get(fileNameWithoutExt)) : empty();
      fileOrDirs.add(new FileOrDir(of(fr), pairedDir));
      
      pairedDir.ifPresent(d -> {
        pairedDirectories.add(d.getName());
      });
    }

    fileOrDirs.addAll(childDirs.values().stream()
        .filter(d -> !pairedDirectories.contains(d.getName()))
        .map(d -> new FileOrDir(empty(), of(d))).collect(Collectors.toList()));

    fileOrDirs.sort(FILE_OR_DIR_COMPARATOR);


    List<IncludeAllPage> frs = new ArrayList<>();
    for (FileOrDir fd : fileOrDirs) {
      if (depth > maxDepth) {

        if (frs.isEmpty()) {
          frs.add(new IncludeAllPage(depth, new ArrayList<>()));
        }
        IncludeAllPage singleFile = frs.get(0);
        fd.file.ifPresent(singleFile.files::add);
        fd.dir.ifPresent(d -> flattenAndGroupRecursively(d, maxDepth, depth + 1).forEach(iap -> singleFile.files.addAll(iap.files)));
        
      } else {
        IncludeAllPage fileRes = new IncludeAllPage(depth, new ArrayList<>());
        frs.add(fileRes);
        fd.file.ifPresent(fileRes.files::add);

        List<IncludeAllPage> pairedFiles =
            fd.dir.map(d -> flattenAndGroupRecursively(d, maxDepth, depth + 1)).orElse(Collections.emptyList());
        if (depth >= maxDepth) {
          pairedFiles.forEach(iap -> fileRes.files.addAll(iap.files));
        } else {
          pairedFiles.forEach(frs::add);
        }
      }
    }
    return frs;
  }

  /*
   * It's a special case: depth 0 represent the page with the include-all directive. We let the user
   * decide if he want to render it (default: yes). Additionally we handle the case where maxDepth
   * is 0, we should transfer the IncludeAllPage object under the page that have the include-all
   * directive.
   */
  private List<IncludeAllPage> handleDepth0Page(List<IncludeAllPage> pages, FileResource resource, int maxDepth, Path defaultOutputPath) {
    
    boolean skipDepth0 = (boolean) resource.getMetadata().getRawMap().getOrDefault("ignore-depth-0-page", false);

    if (maxDepth == 0) {
      List<FileResource> files = new ArrayList<FileResource>();
      files.add(new FileResourcePlaceHolder(defaultOutputPath, configuration));
      files.addAll(pages.get(0).files);// we know that it has size 1
      return Collections.singletonList(new IncludeAllPage(0, files));
      
    } else if (!skipDepth0) {
      pages.add(0, new IncludeAllPage(0, Collections.singletonList(new FileResourcePlaceHolder(defaultOutputPath, configuration))));
    }

    return pages;
  }
  
  
  // generate the final output path
  private IncludeAllPageWithOutput addOutputInformation(IncludeAllPage pages, Path defaultBaseOutputPath, Path includeAllBasePath, Locale locale) {

    // depth = 0 is the file that has the include-all directive
    if (pages.depth == 0) {
      return new IncludeAllPageWithOutput(pages, pages.files.get(0), pages.files.get(0).getPath(), locale);
    }

    Path parentDefaultOutputPath = defaultBaseOutputPath.getParent();
    FileResource virtualResource = pages.files.stream().findFirst()
        .map(fr -> new VirtualPathFileResource(parentDefaultOutputPath.resolve(includeAllBasePath.relativize(fr.getPath()).toString()), fr))
        .orElseThrow(IllegalStateException::new);

    Path finalOutputPath = outputPathExtractor.apply(virtualResource).normalize();

    return new IncludeAllPageWithOutput(pages, virtualResource, finalOutputPath, locale);
  }

  /*
   * Augment the objects with pagination information. It's done in a separate step as we need the previous and next page reference. 
   */
  private List<IncludeAllPageWithPagination> addPaginationInformation(List<IncludeAllPageWithOutput> pages, boolean includeChildSummary) {

    List<Header> globalToc = new ArrayList<>();

    List<IncludeAllPageWithPagination> processedResources = new ArrayList<>();
    for (int i = 0; i < pages.size(); i++) {
      if (!pages.get(i).files.isEmpty()) {

        IncludeAllPageWithOutput current = pages.get(i);
        
        int summaryPositionBegin = globalToc.size();
        
        globalToc.addAll(current.summary);
        
        int includedChildCount = 0;
        if(includeChildSummary) {
          for (int j = i+1; j < pages.size(); j++) {
            if(pages.get(j).depth > current.depth) {
              includedChildCount += pages.get(j).summary.size();
            } else {
              break;
            }
          }
        }
        
        int summaryPositionEnd = globalToc.size() + includedChildCount;

        String previousPageUrl = null;
        String previousPageTitle = null;
        String nextPageUrl = null;
        String nextPageTitle = null;
        
        
        List<Link> breadcrumbs = extractBreadcrumbs(pages, i, current.outputPath);
        
        if (i > 0) {
          previousPageUrl = PathUtils.relativePathTo(pages.get(i - 1).outputPath, current.outputPath);
          previousPageTitle = pages.get(i - 1).title.orElse(null);
        }
        if (i < pages.size() - 1) {
          nextPageUrl = PathUtils.relativePathTo(pages.get(i + 1).outputPath, current.outputPath);
          nextPageTitle = pages.get(i + 1).title.orElse(null);
        }

        Pagination pagination = new Pagination(i + 1, pages.size(), current.depth, new Link(previousPageUrl, 
            previousPageTitle), new Link(nextPageUrl, nextPageTitle), current.title.orElse(null), breadcrumbs);
        processedResources.add(new IncludeAllPageWithPagination(current, pagination, globalToc, summaryPositionBegin, summaryPositionEnd));
      }
    }
    return processedResources;
  }
  
  private List<Link> extractBreadcrumbs(List<IncludeAllPageWithOutput> pages, int currentPosition, Path outputPath) {
    LinkedList<Link> breadcrumbs = new LinkedList<>();
    
    int currentDepth = pages.get(currentPosition).depth;
    
    for(int i = currentPosition; i >= 0; i--) {
      
      IncludeAllPageWithOutput curr = pages.get(i);
      if (curr.depth < currentDepth && curr.title.isPresent()) {
        breadcrumbs.addFirst(new Link(PathUtils.relativePathTo(curr.outputPath, pages.get(currentPosition).outputPath), curr.title.get()));
        currentDepth = curr.depth;
      }
    }
    
    return breadcrumbs;
  }
  
  //
  private static String htmlSummary(List<Header> globalToc, int positionStart, int positionEnd, Path path) {
    Stack<Integer> stack = new Stack<>();
    StringBuilder sbStack = new StringBuilder();
    
    //get the relative position of the first header element
    int startCnt = 1;
    final int firstLevel = globalToc.get(positionStart).level;
    for (int i = positionStart - 1; i >= 0; i--) {
      Header current = globalToc.get(i);
      if(current.level == firstLevel) {
        startCnt++;
      } else if(current.level < firstLevel) {
        break;
      }
    }
   
    List<Header> summary = globalToc.subList(positionStart, positionEnd);
    
    for (Header h : summary) {
      if (stack.isEmpty() || stack.peek().intValue() < h.level) {
        stack.push(h.level);
        sbStack.append("<ol>");
      } else if (!stack.isEmpty()) {
        while (!stack.isEmpty() && stack.peek().intValue() > h.level) {
          stack.pop();
          sbStack.append("</ol>\n");
        }
      }
      sbStack.append("<li>").append("<a href=\"")
          .append(PathUtils.relativePathTo(h.outputPath, path)).append("#").append(h.id)
          .append("\">").append(h.name).append("</a>\n");
    }
    //
    for (int i = 0; i < stack.size(); i++) {
      sbStack.append("</ol>\n");
    }
    
    //add attribute start to first ol
    sbStack.replace(3, 3, " start=\"" + startCnt + "\"");
    
    return sbStack.toString();
  }


  private PathAndModelSupplier toPathAndModuleSupplier(IncludeAllPageWithPagination page, Locale locale) {

    Supplier<Map<String, Object>> supplier = () -> {
          Map<String, Object> additionalModel = new HashMap<>();
          additionalModel.put("includeAllResult", page.page.content());
          additionalModel.put("pagination", page.pagination);
          additionalModel.put("summary", htmlSummary(page.globalToc, page.summaryPositionStart, page.summaryPositionEnd, page.page.outputPath));
          additionalModel.put("globalToc", htmlSummary(page.globalToc, 0, page.globalToc.size(), page.page.outputPath));
          return ModelPreparer.prepare(root, configuration, locale, page.page.virtualResource,
              page.page.outputPath, taxonomy, additionalModel);
        };
    return new PathAndModelSupplier(page.page.outputPath, supplier);
  }

  private static class FileOrDir {
    final Optional<FileResource> file;
    final Optional<Directory> dir;
    final String name;

    FileOrDir(Optional<FileResource> file, Optional<Directory> dir) {
      this.file = file;
      this.dir = dir;
      // we are sure that at least one of the two is present
      this.name = file.map(FileResource::getFileNameWithoutExtensions).orElseGet(() -> dir.get().getName());
    }

    String getName() {
      return name;
    }
  }

  private static class IncludeAllPage {
    final int depth;
    final List<FileResource> files;

    IncludeAllPage(int depth, List<FileResource> files) {
      this.depth = depth;
      this.files = files;
    }
  }

  private class IncludeAllPageWithOutput extends IncludeAllPage {

    final FileResource virtualResource;
    final Path outputPath;
    final Locale locale;
    final Optional<String> title;
    final List<Header> summary;


    IncludeAllPageWithOutput(IncludeAllPage includeAllPage, FileResource virtualResource,
        Path outputPath, Locale locale) {
      super(includeAllPage.depth, includeAllPage.files);
      this.virtualResource = virtualResource;
      this.outputPath = outputPath;
      this.locale = locale;

      Elements titles = Jsoup.parseBodyFragment(content()).select("h1,h2,h3,h4,h5,h6");
      this.title = titles.stream().findFirst().map(Element::text);
      this.summary = titles.stream().map(e -> new Header(headerLevel(e.tagName()), e.text(), e.getElementsByTag("a").attr("name"), outputPath)).collect(Collectors.toList());
    }

    int headerLevel(String name) {
      return Integer.parseInt(name.substring(1));
    }

    String content() {
      Map<String, Object> modelForIncludeAllPage = ModelPreparer.prepare(root, configuration, locale, virtualResource, outputPath, taxonomy);
      return files.stream()
          .map(f -> resourceProcessor.apply(locale).apply(f, modelForIncludeAllPage))
          .map(FileResourceProcessorOutput::getContent).collect(Collectors.joining());
    }
  }
  
  public static class Link {
    private final String url;
    private final String title;
    
    private Link(String url, String title) {
      this.url = url;
      this.title = title;
    }

    public String getUrl() {
      return url;
    }

    public String getTitle() {
      return title;
    }
  }

  private static class IncludeAllPageWithPagination {
    final IncludeAllPageWithOutput page;
    final Pagination pagination;
    final List<Header> globalToc;
    final int summaryPositionStart;
    final int summaryPositionEnd;

    IncludeAllPageWithPagination(IncludeAllPageWithOutput page, Pagination pagination,
        List<Header> globalToc, int summaryPositionStart, int summaryPositionEnd) {
      this.page = page;
      this.pagination = pagination;
      this.globalToc = globalToc;
      this.summaryPositionStart = summaryPositionStart;
      this.summaryPositionEnd = summaryPositionEnd;
    }
  }

  static class Header {
    final int level;
    final String name;
    final String id;
    final Path outputPath;

    Header(int level, String name, String id, Path outputPath) {
      this.level = level;
      this.name = name;
      this.id = id;
      this.outputPath = outputPath;
    }
  }

  public static class Pagination {
    private final int page;
    private final int total;
    private final int depth;
    private final Link previousPage;
    private final Link nextPage;
    private final String pageTitle;
    private final List<Link> breadcrumbs;

    public Pagination(int page, int total, int depth, Link previousPage, Link nextPage, String pageTitle, List<Link> breadcrumbs) {
      this.page = page;
      this.total = total;
      this.depth = depth;
      this.previousPage = previousPage;
      this.nextPage = nextPage;
      this.pageTitle = pageTitle;
      this.breadcrumbs = breadcrumbs;
    }

    public int getPage() {
      return page;
    }

    public int getTotal() {
      return total;
    }

    public int getDepth() {
      return depth;
    }

    public String getPreviousPageUrl() {
      return previousPage.url;
    }

    public String getNextPageUrl() {
      return nextPage.url;
    }

    public String getPreviousPageTitle() {
      return previousPage.title;
    }

    public String getNextPageTitle() {
      return nextPage.title;
    }

    public String getPageTitle() {
      return pageTitle;
    }

    public List<Link> getBreadcrumbs() {
      return breadcrumbs;
    }
  }
}
