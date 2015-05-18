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
package ch.digitalfondue.stampo;

// Based on the WatchDir example from Oracle.

/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted
 * provided that the following conditions are met:
 * 
 * - Redistributions of source code must retain the above copyright notice, this list of conditions
 * and the following disclaimer.
 * 
 * - Redistributions in binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other materials provided with
 * the distribution.
 * 
 * - Neither the name of Oracle nor the names of its contributors may be used to endorse or promote
 * products derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

public class WatchDir {

  private final WatchService watcher;
  private final Map<WatchKey, Path> keys;
  private final Set<Path> ignore;
  private final Set<String> ignorePattern;

  /**
   * Creates a WatchService and registers the given directory
   */
  WatchDir(Path dir, Set<Path> ignore, Set<String> ignorePattern) throws IOException {
    this.watcher = dir.getFileSystem().newWatchService();
    this.keys = new HashMap<>();
    this.ignore = ignore;
    this.ignorePattern = ignorePattern;

    registerAll(dir);
  }

  /**
   * Register the given directory with the WatchService
   */
  private void register(Path dir) throws IOException {

    WatchKey key = dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
    keys.put(key, dir);
  }

  /**
   * Register the given directory, and all its sub-directories, with the WatchService.
   */
  private void registerAll(final Path start) throws IOException {
    // register directory and sub-directories
    Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
      @Override
      public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
          throws IOException {

        if (ignore.contains(dir)) {
          return FileVisitResult.SKIP_SUBTREE;
        }

        register(dir);
        return FileVisitResult.CONTINUE;
      }
    });
  }



  /**
   * Process a single key queued to the watcher
   * 
   * @param delayQueue
   */
  void processEvent(DelayQueue<Delayed> delayQueue) {


    // wait for key to be signaled
    WatchKey key;
    try {
      key = watcher.poll(250, TimeUnit.MILLISECONDS);
    } catch (InterruptedException | ClosedWatchServiceException x) {
      return;
    }
    
    if(key == null) {
      return;
    }

    Path dir = keys.get(key);
    if (dir == null) {
      return;
    }

    for (WatchEvent<?> event : key.pollEvents()) {
      WatchEvent.Kind<?> kind = event.kind();

      if (kind == OVERFLOW) {
        continue;
      }

      // Context for directory entry event is the file name of entry
      @SuppressWarnings("unchecked")
      WatchEvent<Path> ev = (WatchEvent<Path>) event;
      Path name = ev.context();
      Path child = dir.resolve(name);

      //
      if (ignore.contains(child)) {
        return;
      }


      if (!ignorePattern.stream().anyMatch(
          m -> child.getFileSystem().getPathMatcher(m).matches(child.getFileName()))) {
        delayQueue.add(new WatchDirDelay());
      }


      // if directory is created, and watching recursively, then
      // register it and its sub-directories
      if (kind == ENTRY_CREATE) {
        try {
          if (Files.isDirectory(child, NOFOLLOW_LINKS)) {
            registerAll(child);
          }
        } catch (IOException x) {
        }
      }
    }


    // reset key and remove from set if directory no longer accessible
    boolean valid = key.reset();
    if (!valid) {
      keys.remove(key);

      // all directories are inaccessible
      if (keys.isEmpty()) {
        return;
      }
    }
  }

  private static class WatchDirDelay implements Delayed {
    private final long time = System.currentTimeMillis() + 500;

    @Override
    public int compareTo(Delayed o) {
      if (o == this) {
        return 0;
      } else if (o instanceof WatchDirDelay) {
        WatchDirDelay other = (WatchDirDelay) o;
        return Long.compare(this.time, other.time);
      } else {
        throw new IllegalStateException();
      }
    }

    @Override
    public long getDelay(TimeUnit unit) {
      return unit.convert(time - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
    }
  }
}
