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
		Map<String, String> extensionTransformMapping = new HashMap<>();
		renderer.registerResourceRenderer(root, configuration,
				extensionProcessorResource, extensionTransformMapping);
		
		assertEquals(5, extensionProcessorResource.size()); //5 extensions .md, .mkdown, ....
		assertEquals(5, extensionTransformMapping.size()); // .md -> .html * 5
	}
}
