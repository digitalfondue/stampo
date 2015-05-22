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
package ch.digitalfondue.stampo.renderer.markdown;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.pegdown.PegDownProcessor;

import ch.digitalfondue.stampo.StampoGlobalConfiguration;
import ch.digitalfondue.stampo.processor.FileResourceParameters;
import ch.digitalfondue.stampo.processor.FileResourceProcessorOutput;
import ch.digitalfondue.stampo.processor.LayoutParameters;
import ch.digitalfondue.stampo.processor.LayoutProcessorOutput;
import ch.digitalfondue.stampo.renderer.Renderer;
import ch.digitalfondue.stampo.resource.Directory;

public class MarkdownRenderer implements Renderer {
  
  private static final List<String> EXTENSIONS = Arrays.asList("markdown", "mdown", "mkdn", "mkd", "md");

  private static Function<FileResourceParameters, FileResourceProcessorOutput> fileResourceProcessor() {
    PegDownProcessor pegDownProcessor = new PegDownProcessor();
    return params -> new FileResourceProcessorOutput(pegDownProcessor.markdownToHtml(params.fileResource.getContent().orElseThrow(IllegalArgumentException::new)), params.fileResource.getPath(), "markdown", params.locale);
  }

  @Override
  public void registerLayoutRenderer(Directory root, StampoGlobalConfiguration configuration,
      Map<String, Function<LayoutParameters, LayoutProcessorOutput>> extensionProcessor) {}

  @Override
  public void registerResourceRenderer(
      Directory root,
      StampoGlobalConfiguration configuration,
      Map<String, Function<FileResourceParameters, FileResourceProcessorOutput>> extensionProcessor) {

    Function<FileResourceParameters, FileResourceProcessorOutput> processor =
        fileResourceProcessor();

    for (String extension : EXTENSIONS) {
      extensionProcessor.put(extension, processor);
    }
  }

  @Override
  public List<String> resourceExtensions() {
    return EXTENSIONS;
  }

  @Override
  public Map<String, String> extensionTransformMapping() {
    return EXTENSIONS.stream().collect(Collectors.toMap(Function.identity(), ignore -> "html"));
  }

}
