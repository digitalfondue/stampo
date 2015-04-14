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

import java.nio.file.Path;
import java.util.Map;
import java.util.function.Supplier;

class PathAndModelSupplier {

  private final Path outputPath;
  private final Supplier<Map<String, Object>> modelSupplier;

  PathAndModelSupplier(Path outputPath, Supplier<Map<String, Object>> modelSupplier) {
    this.outputPath = outputPath;
    this.modelSupplier = modelSupplier;
  }

  public Path getOutputPath() {
    return outputPath;
  }

  public Supplier<Map<String, Object>> getModelSupplier() {
    return modelSupplier;
  }
}
