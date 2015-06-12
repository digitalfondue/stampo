package ch.digitalfondue.stampo;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.file.Paths;

import org.junit.Assert;
import org.junit.Test;

import ch.digitalfondue.stampo.command.New;

public class NewCommandTest {
	
	private static String[] of(String... strings) {
		return strings;
	}
	
	private void deleteProjectDirectory(File path) {
		if(!path.exists()) return;
		if(path.isDirectory()) {
			for (File f : path.listFiles()){
				deleteProjectDirectory(f);
            }
		}
		path.delete();
	}
	
	@Test
	public void callNewMissingArguments() {
		Runnable r = StampoMain.fromParameters(of("new"));
	    Assert.assertTrue(r instanceof New);
	    New n = (New) r;
	    Assert.assertFalse(n.getPath().isPresent());
	    Assert.assertFalse(n.getName().isPresent());
	}
	
	@Test
	public void callNewWithProjectName() {
		Runnable r = StampoMain.fromParameters(of("new", "--name=test"));
	    Assert.assertTrue(r instanceof New);
	    New n = (New) r;
	    Assert.assertFalse(n.getPath().isPresent());
	    Assert.assertTrue(n.getName().isPresent());
	    
	    n.run();
	    
	    //assert
	    Assert.assertTrue(Paths.get("./test/test.txt").toFile().exists());
	    Assert.assertTrue(Paths.get("./test/child").toFile().exists());
	    Assert.assertTrue(Paths.get("./test/child").toFile().isDirectory());
	    Assert.assertTrue(Paths.get("./test/child/test-child.txt").toFile().exists());
	    
	    //cleanup
	    deleteProjectDirectory(Paths.get("./test").toFile());
	}
	
	@Test
	public void callNewWithPath() {
		Runnable r = StampoMain.fromParameters(of("new", "--dest=./stampo-path"));
	    Assert.assertTrue(r instanceof New);
	    New n = (New) r;
	    Assert.assertTrue(n.getPath().isPresent());
	    Assert.assertFalse(n.getName().isPresent());
	    
	    n.run();
	    
	    //assert
	    Assert.assertTrue(Paths.get("./stampo-path/test.txt").toFile().exists());
	    Assert.assertTrue(Paths.get("./stampo-path/child").toFile().exists());
	    Assert.assertTrue(Paths.get("./stampo-path/child").toFile().isDirectory());
	    Assert.assertTrue(Paths.get("./stampo-path/child/test-child.txt").toFile().exists());
	    
	    //cleanup
	    deleteProjectDirectory(Paths.get("./stampo-path").toFile());
	}
	
	@Test
	public void callNewWithPathAndArchetype() {
		Runnable r = StampoMain.fromParameters(of("new", "--dest=./stampo-path", "--archetype=site"));
	    Assert.assertTrue(r instanceof New);
	    New n = (New) r;
	    Assert.assertTrue(n.getPath().isPresent());
	    Assert.assertFalse(n.getName().isPresent());
	    
	    n.run();
	    
	    //assert
	    Assert.assertTrue(Paths.get("./stampo-path/test.txt").toFile().exists());
	    Assert.assertTrue(Paths.get("./stampo-path/child").toFile().exists());
	    Assert.assertTrue(Paths.get("./stampo-path/child").toFile().isDirectory());
	    Assert.assertTrue(Paths.get("./stampo-path/child/test-child.txt").toFile().exists());
	    Assert.assertTrue(Paths.get("./stampo-path/site").toFile().exists());
	    Assert.assertTrue(Paths.get("./stampo-path/site").toFile().isDirectory());
	    Assert.assertTrue(Paths.get("./stampo-path/site/site-test.txt").toFile().exists());
	    
	    //cleanup
	    deleteProjectDirectory(Paths.get("./stampo-path").toFile());
	}
	
	@Test
	public void callNewWithPathAndWrongArchetype() {
		Runnable r = StampoMain.fromParameters(of("new", "--dest=./stampo-path", "--archetype=wrong-value"));
	    Assert.assertTrue(r instanceof New);
	    New n = (New) r;
	    Assert.assertTrue(n.getPath().isPresent());
	    Assert.assertFalse(n.getName().isPresent());
	    
	    ByteArrayOutputStream outContent = new ByteArrayOutputStream();
	    System.setErr(new PrintStream(outContent));
	    
	    
	    n.run();
	    Assert.assertEquals("invalid archetype, specify one of: [basic, site, blog, doc]", outContent.toString().replaceAll("[\r\n]", ""));
	}

}
