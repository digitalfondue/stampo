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
package ch.digitalfondue.stampo.command;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

public class New implements Opts {
	
	protected final OptionParser optionParser = new OptionParser();
	
	protected Optional<String> name = Optional.empty();
	protected Optional<String> path = Optional.empty();
	protected String archetype = null;
	
	//
	private final OptionSpec<String> archetypeParam;
	private final OptionSpec<String> nameParam;
	private final OptionSpec<String> srcParam;
	
	//
	public final static String ARCHETYPE_BASIC = "basic";
	public final static String ARCHETYPE_SITE = "site";
	public final static String ARCHETYPE_BLOG = "blog";
	public final static String ARCHETYPE_DOC = "doc";
	
	private final List<String> VALID_ARCHETYPES = Arrays.asList(ARCHETYPE_BASIC, ARCHETYPE_SITE, ARCHETYPE_BLOG, ARCHETYPE_DOC);
	
	public New() {
	    archetypeParam = optionParser.accepts("archetype").withRequiredArg().ofType(String.class).defaultsTo(ARCHETYPE_BASIC);
	    nameParam = optionParser.accepts("name").withRequiredArg().ofType(String.class);
	    srcParam = optionParser.accepts("dest").withRequiredArg().ofType(String.class);
	}
	
	@Override
	public void assign(OptionSet optionSet) {
		archetype = optionSet.valueOf(archetypeParam);
		if(optionSet.hasArgument(nameParam)) {
			name =  Optional.of(optionSet.valueOf(nameParam));
		}
		if(optionSet.hasArgument(srcParam)) {
	      	setSrcPath(optionSet.valueOf(srcParam));
	    }
	}
	
	public void setSrcPath(String path) {
		this.path = Optional.of(path);
	}
	
	public Optional<String> getPath() {
	    return path;
	}
	  
	public Optional<String> getName() {
		return name;
	}
	
	private Path inputPath() {
		String n = name.isPresent() ? name.get() : "";
		return Paths.get(path.orElse("./"+n))
			.toAbsolutePath().normalize();
	}
	
	public void setArchetype(String archetype) {
		this.archetype = archetype;
	}

	@Override
	public void run() {
		if(!path.isPresent() && !name.isPresent()) {
			System.err.println("--dest=<destination_path> or --name=<project_name> should be set");
			return;
		}
		
		if(!VALID_ARCHETYPES.contains(archetype)) {
			System.err.println("invalid archetype, specify one of: " + VALID_ARCHETYPES);
			return;
		}
		
		//build output path depending on the input parameters
		Path outputPath = inputPath();
		
		//extract resources in the path
		Predicate<ZipEntry> isDir = ZipEntry::isDirectory;
		URL zip = Thread.currentThread().getContextClassLoader().getResource("template/"+archetype+".zip");
		try(ZipFile file = new ZipFile(zip.getFile());) {
			file.stream().filter(isDir.negate()).forEach(ze -> extractFile(outputPath, ze, file));
		} catch (Exception e) {
			System.err.println("error extracting template, " + e.getMessage());
			return;
		}
			
		
	}
	
	private void extractFile(Path outputPath, ZipEntry ze, ZipFile zip) {
		Path destinationPath = outputPath.resolve(ze.getName());
		try(InputStream stream = zip.getInputStream(ze)) {
			Files.createDirectories(destinationPath.getParent());
			Files.copy(stream, destinationPath);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
	}

	@Override
	public OptionParser getOptionParser() {
		return optionParser;
	}

}
