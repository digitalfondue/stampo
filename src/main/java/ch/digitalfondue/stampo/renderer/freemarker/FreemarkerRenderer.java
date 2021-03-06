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
package ch.digitalfondue.stampo.renderer.freemarker;

import java.io.StringWriter;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.Function;

import ch.digitalfondue.stampo.PathUtils;
import ch.digitalfondue.stampo.StampoGlobalConfiguration;
import ch.digitalfondue.stampo.exception.LayoutException;
import ch.digitalfondue.stampo.exception.TemplateException;
import ch.digitalfondue.stampo.processor.FileResourceParameters;
import ch.digitalfondue.stampo.processor.FileResourceProcessorOutput;
import ch.digitalfondue.stampo.processor.LayoutParameters;
import ch.digitalfondue.stampo.processor.LayoutProcessorOutput;
import ch.digitalfondue.stampo.renderer.Renderer;
import ch.digitalfondue.stampo.resource.Directory;
import freemarker.ext.beans.BeansWrapperBuilder;
import freemarker.ext.beans.ResourceBundleModel;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateMethodModelEx;

public class FreemarkerRenderer implements Renderer {


  @Override
  public void registerLayoutRenderer(Directory root, StampoGlobalConfiguration configuration,
      Map<String, Function<LayoutParameters, LayoutProcessorOutput>> extensionProcessor) {

    Configuration c = getConfiguration(root, configuration);
    extensionProcessor.put("ftl", params -> {

      try {
        
        registerResourceBundleResolver(params.model, params.locale, configuration);
        Template template = c.getTemplate(params.layoutTemplate.get().toString(), params.locale);
        StringWriter sw = new StringWriter();
        template.process(params.model, sw);
        
        
        return new LayoutProcessorOutput(sw.toString(), "freemarker", params.layoutTemplate,
            params.locale);
      } catch (Exception e) {
        throw new LayoutException(params.layoutTemplate.get(), params.targetResource, e);
      }
    });

  }

  @Override
  public void registerResourceRenderer(
      Directory root,
      StampoGlobalConfiguration configuration,
      Map<String, Function<FileResourceParameters, FileResourceProcessorOutput>> extensionProcessor) {

    Configuration c = getConfiguration(root, configuration);

    extensionProcessor.put("ftl", params -> {
      try {
        
        registerResourceBundleResolver(params.model, params.locale, configuration);
        Template template = c.getTemplate(params.fileResource.getPath().toString(), params.locale);
        StringWriter sw = new StringWriter();
        template.process(params.model, sw);
        
        return new FileResourceProcessorOutput(sw.toString(), params.fileResource.getPath(),
            "freemarker", params.locale);
      } catch (Exception e) {
        throw new TemplateException(params.fileResource.getPath(), e);
      }
    });
  }
  
  private static void registerResourceBundleResolver(Map<String, Object> model, Locale locale, StampoGlobalConfiguration configuration) {
    model.put("message", new ResourceBundleModel(ResourceBundle.getBundle("messages", locale, configuration.getResourceBundleControl()), new BeansWrapperBuilder(Configuration.VERSION_2_3_22).build()));
    
    TemplateMethodModelEx messageWithBundle = (arguments) -> {
      if (arguments.size() < 2) {
        throw new IllegalArgumentException(
            "messageWithBundle must have at least 2 parameters, passed only " + arguments.size());
      }
      
      String bundleName = arguments.get(0).toString();
      String code = arguments.get(1).toString();
      
      List<Object> parameters = new ArrayList<>();
      for (int i = 2; i < arguments.size(); i++) {
        parameters.add(arguments.get(i));
      }
      
      return MessageFormat.format(
          ResourceBundle.getBundle(bundleName, locale, configuration.getResourceBundleControl()).getString(code),
          parameters.toArray());
    };
    
    model.put("messageWithBundle", messageWithBundle);
    
    TemplateMethodModelEx defaultOrLocale = (arguments) -> {
      String loc = arguments.get(0).toString();
      return configuration.getDefaultLocale().map(l -> l.toLanguageTag().equals(loc) ? "" : loc)
          .orElse(loc);
    };
    model.put("defaultOrLocale", defaultOrLocale);
    
    TemplateMethodModelEx switchToLocale = (arguments) -> {
      String localeToSwitch = arguments.get(0).toString();
      Path fileResourceOutputPath = (Path) model.get("fileResourceOutputPath");
      return PathUtils.switchToLocale(Locale.forLanguageTag(localeToSwitch), locale, fileResourceOutputPath, configuration);
    };
    
    model.put("switchToLocale", switchToLocale);
  }

  private static Configuration getConfiguration(Directory root,
      StampoGlobalConfiguration configuration) {
    Configuration c = new Configuration(Configuration.VERSION_2_3_22);
    c.setDefaultEncoding("UTF-8");
    c.setLocalizedLookup(false);
    c.setTemplateLoader(new FreemarkerTemplateLoader(configuration.getContentDir(), configuration.getBaseDirectory(), root));
    return c;
  }

  @Override
  public List<String> resourceExtensions() {
    return Collections.singletonList("ftl");
  }

  @Override
  public Map<String, String> extensionTransformMapping() {
    return Collections.emptyMap();
  }

}
