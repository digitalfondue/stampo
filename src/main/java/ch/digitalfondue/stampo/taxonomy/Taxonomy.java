package ch.digitalfondue.stampo.taxonomy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ch.digitalfondue.stampo.resource.Directory;
import ch.digitalfondue.stampo.resource.FileResource;

public class Taxonomy {

  private final Map<String, List<FileResource>> groups = new HashMap<>();
  private final Set<String> groupingProperties;

  public Taxonomy(Set<String> groupingProperties, Directory directory,
      Comparator<FileResource> fileSorter) {
    this.groupingProperties = groupingProperties;
    generateGroups(directory);
    groups.forEach((k, l) -> {
      Collections.sort(l, fileSorter);
    });
  }

  private void generateGroups(Directory dir) {
    dir.getFiles().values().forEach(this::addToGroups);
    dir.getDirectories().values().forEach(this::generateGroups);
  }

  @SuppressWarnings("unchecked")
  private void addToGroups(FileResource file) {

    Map<String, Object> m = file.getMetadata().getRawMap();
    for (String prop : groupingProperties) {
      if (m.containsKey(prop) && m.get(prop) != null) {
        Object val = m.get(prop);
        Collection<Object> vals;
        if (val instanceof Collection) {
          vals = (Collection<Object>) val;
        } else {
          vals = Collections.singleton(val);
        }

        for (Object v : vals) {
          String key = v.toString();
          if (!groups.containsKey(key)) {
            groups.put(key, new ArrayList<FileResource>());
          }
          groups.get(key).add(file);
        }
      }
    }
  }
}
