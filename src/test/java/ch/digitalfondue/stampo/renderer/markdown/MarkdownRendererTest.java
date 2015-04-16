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

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.junit.Test;

import ch.digitalfondue.stampo.StampoGlobalConfiguration;
import ch.digitalfondue.stampo.processor.FileResourceParameters;
import ch.digitalfondue.stampo.processor.FileResourceProcessorOutput;
import ch.digitalfondue.stampo.processor.LayoutParameters;
import ch.digitalfondue.stampo.processor.LayoutProcessorOutput;
import ch.digitalfondue.stampo.renderer.Renderer;
import ch.digitalfondue.stampo.resource.Directory;

public class MarkdownRendererTest {

	@Test
	public void registerRenderer() {
		Renderer renderer = new MarkdownRenderer();

		Directory root = mock(Directory.class);
		StampoGlobalConfiguration configuration = mock(StampoGlobalConfiguration.class);

		Map<String, Function<LayoutParameters, LayoutProcessorOutput>> extensionProcessorLayout = new HashMap<>();
		renderer.registerLayoutRenderer(root, configuration,
				extensionProcessorLayout);
		//no layout added
		assertTrue(extensionProcessorLayout.isEmpty());

		
		Map<String, Function<FileResourceParameters, FileResourceProcessorOutput>> extensionProcessorResource = new HashMap<>();
		renderer.registerResourceRenderer(root, configuration, extensionProcessorResource);
		
		assertEquals(5, extensionProcessorResource.size()); //5 extensions .md, .mkdown, ....
	}
}
