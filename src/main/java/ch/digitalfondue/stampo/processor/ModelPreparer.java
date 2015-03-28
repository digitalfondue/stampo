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
package ch.digitalfondue.stampo.processor;

import java.nio.file.Files;
import java.text.DecimalFormatSymbols;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ch.digitalfondue.stampo.StampoGlobalConfiguration;
import ch.digitalfondue.stampo.resource.Directory;
import ch.digitalfondue.stampo.resource.DirectoryResource;
import ch.digitalfondue.stampo.resource.FileResource;
import ch.digitalfondue.stampo.resource.ResourceFactory;
import ch.digitalfondue.stampo.resource.RootResource;
import ch.digitalfondue.stampo.resource.StaticFileResource;

public class ModelPreparer {

  
  public static Map<String, Object> prepare(Directory root, StampoGlobalConfiguration configuration, Locale locale, FileResource resource) {
    Map<String, Object> model = new HashMap<>();
    model.put("root", root);
    model.put("configuration", configuration);
    model.put("locale", locale);
    model.put("resource", resource);
    model.put("metadata", resource.getMetadata());
    
    staticResources(configuration).ifPresent((staticRootResource) -> model.put("static", staticRootResource));
    
    return model;
  }
  
  
  private static final Optional<RootResource> staticResources(StampoGlobalConfiguration configuration) {
    if(Files.exists(configuration.getStaticDir())) {
      Comparator<FileResource> comparator = Comparator.comparing(FileResource::getName, new AlphaNumericStringComparator(Locale.ENGLISH));
      ResourceFactory resourceFactory = new ResourceFactory(DirectoryResource::new, StaticFileResource::new, comparator, configuration);
      return Optional.of(new RootResource(resourceFactory, configuration.getStaticDir()));
    } else {
      return Optional.empty();
    }
  }
  
  
  
  // imported from http://simplesql.tigris.org/servlets/ProjectDocumentList?folderID=0
  /*
   * Copyright (c) 2007 Eric Berry <elberry@gmail.com>
   *
   * Permission is hereby granted, free of charge, to any person obtaining a copy
   * of this software and associated documentation files (the "Software"), to deal
   * in the Software without restriction, including without limitation the rights
   * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
   * copies of the Software, and to permit persons to whom the Software is
   * furnished to do so, subject to the following conditions:
   * 
   * The above copyright notice and this permission notice shall be included in
   * all copies or substantial portions of the Software.
   * 
   * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
   * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
   * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
   * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
   * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
   * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
   * THE SOFTWARE.
   */

  

  /**
   * Compares Strings by human values instead of traditional machine values.
   * @author elberry
   */
  private static class AlphaNumericStringComparator implements Comparator<String> {

     private Pattern alphaNumChunkPattern;


     public AlphaNumericStringComparator(Locale locale) {
        DecimalFormatSymbols dfs = new DecimalFormatSymbols(locale);
        char localeDecimalSeparator = dfs.getDecimalSeparator();
        // alphaNumChunkPatter initialized here to get correct decimal separator for locale.
        alphaNumChunkPattern = Pattern.compile("(\\d+\\" + localeDecimalSeparator + "\\d+)|(\\d+)|(\\D+)");
     }

     public int compare(String s1, String s2) {
        int compareValue = 0;
        Matcher s1ChunkMatcher = alphaNumChunkPattern.matcher(s1);
        Matcher s2ChunkMatcher = alphaNumChunkPattern.matcher(s2);
        String s1ChunkValue = null;
        String s2ChunkValue = null;
        while (s1ChunkMatcher.find() && s2ChunkMatcher.find() && compareValue == 0) {
           s1ChunkValue = s1ChunkMatcher.group();
           s2ChunkValue = s2ChunkMatcher.group();
           try {
              // compare double values - ints get converted to doubles. Eg. 100 = 100.0
              Double s1Double = Double.valueOf(s1ChunkValue);
              Double s2Double = Double.valueOf(s2ChunkValue);
              compareValue = s1Double.compareTo(s2Double);
           } catch (NumberFormatException e) {
              // not a number, use string comparison.
              compareValue = s1ChunkValue.compareTo(s2ChunkValue);
           }
           // if they are equal thus far, but one has more left, it should come after the one that doesn't.
           if (compareValue == 0) {
              if (s1ChunkMatcher.hitEnd() && !s2ChunkMatcher.hitEnd()) {
                 compareValue = -1;
              } else if (!s1ChunkMatcher.hitEnd() && s2ChunkMatcher.hitEnd()) {
                 compareValue = 1;
              }
           }
        }
        return compareValue;
     }
  }
}
