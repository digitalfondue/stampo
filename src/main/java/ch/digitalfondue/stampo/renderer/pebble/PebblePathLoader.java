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
package ch.digitalfondue.stampo.renderer.pebble;

import static java.nio.file.Files.newBufferedReader;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import ch.digitalfondue.stampo.renderer.Renderer;
import ch.digitalfondue.stampo.resource.Directory;

import com.mitchellbosecke.pebble.error.LoaderException;
import com.mitchellbosecke.pebble.loader.Loader;

class PebblePathLoader implements Loader {

  private final Path contentDir;
  private final Directory root;


  PebblePathLoader(Path contentDir, Directory root) {
    this.contentDir = contentDir;
    this.root = root;
  }

  @Override
  public Reader getReader(String templateName) throws LoaderException {

    Path template = contentDir.getFileSystem().getPath(templateName).normalize();

    try {
      if (template.startsWith(contentDir)) {
        return new StringReader(Renderer.getContentFileResource(template, contentDir, root).getContent().orElseThrow(IllegalArgumentException::new));
      } else {
        // it's outside the content dir: must be resolved over the baseDir
        return newBufferedReader(template, StandardCharsets.UTF_8);
      }
    } catch (IOException ioe) {
      throw new LoaderException(ioe, "was not able to load referenced template");
    }
  }

  @Override
  public void setCharset(String charset) {}

  @Override
  public void setPrefix(String prefix) {}

  @Override
  public void setSuffix(String suffix) {}
}
