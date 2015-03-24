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
package ch.digitalfondue.stampo.i18n;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.ResourceBundle.Control;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.parser.ParserException;

import ch.digitalfondue.stampo.exception.YamlParserException;

public class ResourceBundleControl extends Control {

  private final Path localesDir;

  public ResourceBundleControl(Path localesDir) {
    this.localesDir = localesDir;
  }


  @Override
  public ResourceBundle newBundle(String baseName, Locale locale, String format,
      ClassLoader loader, boolean reload) throws IllegalAccessException, InstantiationException,
      IOException {

    String bundleName = toBundleName(baseName, locale);
    Path propFile = localesDir.resolve(bundleName + ".yaml");

    return new StampoResourceBundle(propFile);
  }



  private static class StampoResourceBundle extends ResourceBundle {

    private final Path propFile;

    public StampoResourceBundle(Path propFile) {
      this.propFile = propFile;
    }

    @Override
    protected Object handleGetObject(String key) {
      // TODO: add some kind of caching (store latest modification time)
      return extractProperties(propFile).get(key);
    }

    @Override
    public Enumeration<String> getKeys() {
      return null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractProperties(Path propFile) {
      Map<String, Object> properties = new HashMap<>();
      if (Files.exists(propFile)) {
        try (InputStream is = Files.newInputStream(propFile)) {
          properties =
              Collections.unmodifiableMap(Optional.ofNullable(
                  (Map<String, Object>) new Yaml().loadAs(is, Map.class)).orElse(
                  Collections.emptyMap()));
        } catch (ParserException pe) {
          YamlParserException ype = new YamlParserException(propFile, pe);
          System.err.println(ype.getMessage());
          throw ype;
        } catch (IOException ioe) {
          throw new IllegalStateException(ioe);
        }
      }
      return properties;
    }
  }
}
