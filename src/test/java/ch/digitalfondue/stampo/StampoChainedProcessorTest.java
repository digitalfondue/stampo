package ch.digitalfondue.stampo;

import static java.nio.file.Files.write;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.Assert;
import org.junit.Test;

import ch.digitalfondue.stampo.TestUtils.InputOutputDirs;

public class StampoChainedProcessorTest {

  
  @Test
  public void pebbleAndMarkdown() throws IOException {
    try (InputOutputDirs iod = TestUtils.get()) {
      Files.createDirectories(iod.inputDir.resolve("content/test"));
      write(iod.inputDir.resolve("content/test/index.peb.md"),
          "#{{outputPath}}".getBytes(StandardCharsets.UTF_8));
      Stampo stampo = new Stampo(iod.inputDir, iod.outputDir);
      stampo.build();

      Assert.assertEquals("<h1>test</h1>", TestUtils.fileOutputAsString(iod, "test/index.html"));
    }
  }
  
}
