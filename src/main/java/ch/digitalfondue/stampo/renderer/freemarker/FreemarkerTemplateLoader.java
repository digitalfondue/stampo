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
package ch.digitalfondue.stampo.renderer.freemarker;

import static java.nio.file.Files.newBufferedReader;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import ch.digitalfondue.stampo.StampoGlobalConfiguration;
import ch.digitalfondue.stampo.resource.Directory;
import ch.digitalfondue.stampo.resource.FileResource;
import freemarker.cache.TemplateLoader;

class FreemarkerTemplateLoader implements TemplateLoader {


  private final StampoGlobalConfiguration configuration;
  private final Directory root;
  private final Path contentDir;

  public FreemarkerTemplateLoader(StampoGlobalConfiguration configuration, Directory root) {
    this.configuration = configuration;
    this.contentDir = configuration.getContentDir();
    this.root = root;
  }

  @Override
  public Reader getReader(Object templateSource, String encoding) throws IOException {
    Path template = (Path) templateSource;
    if (template.startsWith(contentDir)) {// content
      return new StringReader(FileResource.getContentFileResource(template, contentDir, root)
          .getContent());
    } else {// layout or others (import/include)
      return newBufferedReader(template, StandardCharsets.UTF_8);
    }
  }

  @Override
  public long getLastModified(Object templateSource) {
    return -1;
  }

  @Override
  public Object findTemplateSource(String name) throws IOException {
    //handle relative or absolute path names
    Path orig = configuration.getContentDir().getFileSystem().getPath(name).normalize();
    Path p = configuration.getContentDir().getFileSystem().getPath("/" + name).normalize();
    if (p.startsWith(configuration.getBaseDirectory())) {
      return p;
    } else if (orig.startsWith(configuration.getBaseDirectory())) {
      return orig;
    } else {
      return configuration.getBaseDirectory().resolve(name);
    }
  }

  @Override
  public void closeTemplateSource(Object templateSource) throws IOException {}
}
