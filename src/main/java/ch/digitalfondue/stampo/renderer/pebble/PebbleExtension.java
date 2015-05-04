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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.ResourceBundle.Control;

import ch.digitalfondue.stampo.StampoGlobalConfiguration;

import com.mitchellbosecke.pebble.extension.AbstractExtension;
import com.mitchellbosecke.pebble.extension.Function;
import com.mitchellbosecke.pebble.template.EvaluationContext;

class PebbleExtension extends AbstractExtension {

  private final StampoGlobalConfiguration configuration;

  PebbleExtension(StampoGlobalConfiguration configuration) {
    this.configuration = configuration;
  }

  @Override
  public Map<String, Function> getFunctions() {
    Map<String, Function> f = new HashMap<>();
    f.put("messageWithBundle", new MessageFunction(configuration, Optional.empty()));
    f.put("message", new MessageFunction(configuration, Optional.of("messages")));
    f.put("fromMap", new FromMapFunction());
    return f;
  }
  
  private static final class FromMapFunction implements Function {

    @Override
    public List<String> getArgumentNames() {
      return Arrays.asList("map", "key");
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object execute(Map<String, Object> args) {
      return ((Map<Object, Object>) args.get("map")).get(args.get("key"));
    }
    
  }

  private static final class MessageFunction implements Function {

    private final Control control;
    private final Optional<String> bundle;

    public MessageFunction(StampoGlobalConfiguration configuration, Optional<String> bundle) {
      this.control = configuration.getResourceBundleControl();
      this.bundle = bundle;
    }

    @Override
    public List<String> getArgumentNames() {
      return null;
    }

    @Override
    public Object execute(Map<String, Object> args) {
      
      String bundleName = bundle.orElse((String) args.get("0"));
      
      int initialIdx = bundle.isPresent() ? 0 : 1;
      
      String code = (String) args.get(Integer.toString(initialIdx));
      List<Object> parameters = new ArrayList<>();
      for (int i = initialIdx + 1; i < args.size() && args.containsKey(Integer.toString(i)); i++) {
        parameters.add(args.get(Integer.toString(i)));
      }

      EvaluationContext context = (EvaluationContext) args.get("_context");
      Locale locale = context.getLocale();

      return MessageFormat.format(
          ResourceBundle.getBundle(bundleName, locale, control).getString(code),
          parameters.toArray());
    }

  }
}
