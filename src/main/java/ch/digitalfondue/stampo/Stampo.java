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
package ch.digitalfondue.stampo;

import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.delete;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.isDirectory;
import static java.nio.file.Files.newInputStream;
import static java.nio.file.Files.walkFileTree;
import static java.nio.file.Paths.get;
import static java.util.Collections.emptyMap;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.ConstructorException;
import org.yaml.snakeyaml.parser.ParserException;

import ch.digitalfondue.stampo.exception.LayoutException;
import ch.digitalfondue.stampo.exception.MissingDirectoryException;
import ch.digitalfondue.stampo.exception.TemplateException;
import ch.digitalfondue.stampo.exception.YamlParserException;
import ch.digitalfondue.stampo.processor.ResourceProcessor;
import ch.digitalfondue.stampo.renderer.Renderer;
import ch.digitalfondue.stampo.renderer.freemarker.FreemarkerRenderer;
import ch.digitalfondue.stampo.renderer.markdown.MarkdownRenderer;
import ch.digitalfondue.stampo.renderer.pebble.PebbleRenderer;
import ch.digitalfondue.stampo.resource.Directory;
import ch.digitalfondue.stampo.resource.LocaleAwareDirectory;
import ch.digitalfondue.stampo.resource.PathOverrideAwareDirectory;
import ch.digitalfondue.stampo.resource.PathOverrideAwareDirectory.Mode;
import ch.digitalfondue.stampo.resource.RootResource;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.common.jimfs.Jimfs;

/**
 *
 */
public class Stampo {

  private final StampoGlobalConfiguration configuration;


  @SuppressWarnings("unchecked")
  public Stampo(Path baseInputDir, Path outputDir) {

    Path configFile = baseInputDir.resolve("configuration.yaml");

    List<Renderer> renderers =
        Arrays.asList(new PebbleRenderer(), new MarkdownRenderer(), new FreemarkerRenderer());

    if (exists(configFile)) {
      Yaml yaml = new Yaml();
      try (InputStream is = newInputStream(configFile)) {
        Map<String, Object> c = ofNullable((Map<String, Object>) yaml.loadAs(is, Map.class)).orElse(Collections.emptyMap());
        this.configuration = new StampoGlobalConfiguration(c, baseInputDir, outputDir, renderers);
      } catch (IOException ioe) {
        throw new IllegalArgumentException(ioe);
      } catch (ConstructorException | ParserException pe) {
        throw new YamlParserException(configFile, pe);
      }
    } else {
      this.configuration =
          new StampoGlobalConfiguration(emptyMap(), baseInputDir, outputDir, renderers);
    }
  }


  public Stampo(String baseDirectory) {
    this(get(baseDirectory).normalize(), get(baseDirectory).normalize().resolve("output")
        .normalize());
  }

  public StampoGlobalConfiguration getConfiguration() {
    return configuration;
  }

  public void build() {
    build((processedFile, processedLayout) -> processedLayout.getContent(), (in, out) -> {
      try {
        Files.copy(in, out);
      } catch (IOException ioe) {
        throw new IllegalStateException(ioe);
      }
    });
  }

  public void build(ProcessedInputHandler outputHandler,
      BiConsumer<Path, Path> staticDirectoryAction) {

    if (!Files.exists(configuration.getContentDir())) {
      throw new MissingDirectoryException(configuration.getContentDir());
    }

    List<Locale> locales = configuration.getLocales();


    cleanupBuildDirectory();

    Directory root = new RootResource(configuration, configuration.getContentDir());
    Directory rootWithOverrideHidden = new PathOverrideAwareDirectory(Mode.HIDE, root);
    Directory rootWithOnlyOverride =
        new PathOverrideAwareDirectory(Mode.SHOW_ONLY_PATH_OVERRIDE, root);

    if (locales.size() > 1) {

      Optional<Locale> defaultLocale = configuration.getDefaultLocale();

      for (Locale locale : locales) {

        Directory localeAwareRoot = new LocaleAwareDirectory(locale, rootWithOverrideHidden);

        Path finalOutputDir =
            defaultLocale.flatMap(
                (l) -> l.equals(locale) ? of(configuration.getBaseOutputDir()) : empty())//
                .orElse(configuration.getBaseOutputDir().resolve(locale.toString()));

        //uncheckedCreateDirectories(finalOutputDir);

        render(localeAwareRoot, new ResourceProcessor(finalOutputDir, localeAwareRoot,
            configuration), locale, outputHandler);
      }

      render(rootWithOnlyOverride, new ResourceProcessor(configuration.getBaseOutputDir(),
          rootWithOnlyOverride, configuration), defaultLocale.orElse(Locale.ENGLISH), outputHandler);
    } else {
      //uncheckedCreateDirectories(configuration.getBaseOutputDir());
      render(rootWithOverrideHidden, new ResourceProcessor(configuration.getBaseOutputDir(),
          rootWithOverrideHidden, configuration), locales.get(0), outputHandler);

      render(rootWithOnlyOverride, new ResourceProcessor(configuration.getBaseOutputDir(),
          rootWithOnlyOverride, configuration), locales.get(0), outputHandler);
    }

    copyStaticDirectory(staticDirectoryAction);
  }

//  private static void uncheckedCreateDirectories(Path dir) {
//    try {
//      createDirectories(dir);
//    } catch (IOException e) {
//      throw new IllegalArgumentException(e);
//    }
//  }

  private void render(Directory root, ResourceProcessor renderer, Locale locale,
      ProcessedInputHandler outputHandler) {
    root.getFiles().values().forEach((f) -> renderer.process(f, locale, outputHandler));
    root.getDirectories().values().forEach((d) -> {
      render(d, renderer, locale, outputHandler);
    });
  }

  private void copyStaticDirectory(BiConsumer<Path, Path> staticDirectoryAction) {
    if (exists(configuration.getStaticDir()) && isDirectory(configuration.getStaticDir())) {
      try {
        walkFileTree(configuration.getStaticDir(), new SimpleFileVisitor<Path>() {
          @Override
          public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            Path outputPath =
                configuration.getBaseOutputDir().resolve(
                    configuration.getStaticDir().relativize(file).toString());
            createDirectories(outputPath.getParent());
            staticDirectoryAction.accept(file, outputPath);
            return FileVisitResult.CONTINUE;
          }
        });
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }
  }

  private void cleanupBuildDirectory() {
    if (exists(configuration.getBaseOutputDir()) && isDirectory(configuration.getBaseOutputDir())) {
      try {
        walkFileTree(configuration.getBaseOutputDir(), new SimpleFileVisitor<Path>() {
          @Override
          public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            delete(file);
            return FileVisitResult.CONTINUE;
          }

          @Override
          public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            delete(dir);
            return FileVisitResult.CONTINUE;
          }
        });
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }
  }

  @Parameters
  private static abstract class Command implements Runnable {
    @Parameter(description = "path", arity = 1)
    List<String> path;

    @Parameter(description = "print stack trace", names = "debug")
    boolean printStackTrace = false;

    private String workingPath() {
      return Paths.get(ofNullable(this.path).flatMap((l) -> l.stream().findFirst()).orElse("."))
          .toAbsolutePath().normalize().toString();
    }

    @Override
    public void run() {
      String workingPath = workingPath();
      System.out.println("working path is " + workingPath);
      try {
        runWithWorkingPath(workingPath);
      } catch (MissingDirectoryException | YamlParserException | TemplateException
          | LayoutException e) {
        System.err.println(e.getMessage());
        if (printStackTrace) {
          Optional.ofNullable(e.getCause()).ifPresent(Throwable::printStackTrace);
        }
        System.exit(1);
      }
    }

    public abstract void runWithWorkingPath(String workingPath);
  }

  @Parameters
  private static class Clean extends Command {

    @Override
    public void runWithWorkingPath(String workingPath) {
      new Stampo(workingPath).cleanupBuildDirectory();
      System.out.println("cleanup done");
    }

  }

  @Parameters
  private static class Serve extends Command {
    @Parameter(description = "port", names = "--port")
    int port = 8080;

    @Parameter(description = "hostname", names = "--hostname")
    String hostname = "localhost";

    @Parameter(description = "rebuild on change", names = "--rebuild-on-change")
    boolean rebuildOnChange = true;

    @Override
    public void runWithWorkingPath(String workingPath) {
      Runnable triggerBuild = getBuildRunnable(workingPath);
      triggerBuild.run();
      new ServeAndWatch(hostname, port, rebuildOnChange, new Stampo(workingPath).getConfiguration())
          .serve(triggerBuild);
    }
  }

  @Parameters
  private static class Check extends Command {

    @Override
    public void runWithWorkingPath(String workingPath) {
      Path output = Jimfs.newFileSystem().getPath("output");

      new Stampo(Paths.get(workingPath), output).build((file, layout) -> {
        return String.format(
            "  from file: %s\n" + //
                "  selected rendering engine: %s\n" + //
                "  locale for resource: %s\n" + //
                "  selected layout path: %s\n" + //
                "  selected layout engine: %s\n" + //
                "  locale for layout: %s\n\n",//
            file.getPath(),
            file.getProcessorEngine(),//
            file.getLocale(), layout.getPath().map(Object::toString).orElse("no layout"),
            layout.getLayoutEngine(), layout.getLocale());
      }, (in, out) -> {
        try {
          Files.write(out, Arrays.asList("  from static input " + in), StandardCharsets.UTF_8);
        } catch (IOException ioe) {
          throw new IllegalStateException(ioe);
        }
      });

      System.out.println();
      System.out.println("The build will generate the following files:");
      System.out.println();
      try {
        Files.walk(output).forEach((p) -> {
          if (!Files.isDirectory(p)) {
            System.out.println("- " + output.relativize(p));
            try {
              Files.readAllLines(p, StandardCharsets.UTF_8).forEach(System.out::println);
            } catch (Exception e) {
              throw new IllegalStateException(e);
            }
          }
        });
      } catch (IOException ioe) {
        throw new IllegalStateException(ioe);
      }
      System.out.println();
      System.out.println("Everything seems ok!");
    }
  }

  @Parameters
  private static class Build extends Command {

    @Override
    public void runWithWorkingPath(String workingPath) {
      getBuildRunnable(workingPath).run();
    }
  }

  private static Runnable getBuildRunnable(String workingPath) {
    return () -> {
      long start = System.currentTimeMillis();
      Stampo s = new Stampo(workingPath);
      s.build();
      long end = System.currentTimeMillis();
      System.out.println("built in " + (end - start) + "ms, output in "
          + s.getConfiguration().getBaseOutputDir());
    };
  }

  public static void main(String[] args) throws IOException {

    Map<String, Command> commands = new HashMap<>();

    commands.put("serve", new Serve());
    commands.put("check", new Check());
    commands.put("build", new Build());
    commands.put("clean", new Clean());

    JCommander jc = new JCommander();
    commands.forEach((k, v) -> jc.addCommand(k, v));

    jc.parseWithoutValidation(args);
    String command = ofNullable(jc.getParsedCommand()).orElse("build");

    commands.get(command).run();
  }
}
