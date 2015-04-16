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

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import ch.digitalfondue.stampo.StampoGlobalConfiguration;
import ch.digitalfondue.stampo.exception.LayoutException;
import ch.digitalfondue.stampo.exception.TemplateException;
import ch.digitalfondue.stampo.processor.FileResourceParameters;
import ch.digitalfondue.stampo.processor.FileResourceProcessorOutput;
import ch.digitalfondue.stampo.processor.LayoutParameters;
import ch.digitalfondue.stampo.processor.LayoutProcessorOutput;
import ch.digitalfondue.stampo.renderer.Renderer;
import ch.digitalfondue.stampo.resource.Directory;

import com.mitchellbosecke.pebble.PebbleEngine;
import com.mitchellbosecke.pebble.error.PebbleException;

public class PebbleRenderer implements Renderer {

  private static PebbleEngine build(Directory root, StampoGlobalConfiguration configuration) {
    PebbleEngine e = new PebbleEngine(new PebblePathLoader(configuration.getContentDir(), configuration.getBaseDirectory(), root));
    e.addExtension(new PebbleExtension(configuration));
    return e;
  }



  @Override
  public void registerLayoutRenderer(Directory root, StampoGlobalConfiguration configuration,
      Map<String, Function<LayoutParameters, LayoutProcessorOutput>> extensionProcessor) {

    PebbleEngine engine = build(root, configuration);

    extensionProcessor.put(
        "peb",
        (lParam) -> {
          try {
            StringWriter sw = new StringWriter();
            engine.getTemplate(lParam.layoutTemplate.get().toString()).evaluate(sw, lParam.model,
                lParam.locale);
            return new LayoutProcessorOutput(sw.toString(), "pebble", lParam.layoutTemplate,
                lParam.locale);
          } catch (PebbleException | IOException e) {
            throw new LayoutException(lParam.layoutTemplate.get(), lParam.targetResource, e);
          }
        });
  }

  @Override
  public void registerResourceRenderer(
      Directory root,
      StampoGlobalConfiguration configuration,
      Map<String, Function<FileResourceParameters, FileResourceProcessorOutput>> extensionProcessor) {
    PebbleEngine pebble = build(root, configuration);
    extensionProcessor.put("peb", (params) -> {
      try {
        Writer writer = new StringWriter();
        pebble.getTemplate(params.fileResource.getPath().toString())//
            .evaluate(writer, params.model, params.locale);
        return new FileResourceProcessorOutput(writer.toString(), params.fileResource.getPath(),
            "pebble", params.locale);
      } catch (PebbleException | IOException e) {
        throw new TemplateException(params.fileResource.getPath(), e);
      }
    });
  }



  @Override
  public List<String> resourceExtensions() {
    return Collections.singletonList("peb");
  }



  @Override
  public Map<String, String> extensionTransformMapping() {
    return Collections.emptyMap();
  }
}
