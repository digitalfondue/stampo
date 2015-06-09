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

import java.util.Optional;

import org.junit.Assert;
import org.junit.Test;

import ch.digitalfondue.stampo.command.Build;
import ch.digitalfondue.stampo.command.Check;
import ch.digitalfondue.stampo.command.Help;
import ch.digitalfondue.stampo.command.Serve;

public class MainCommandsTest {

  private static String[] of(String... strings) {
    return strings;
  }

  @Test
  public void callDefault() {
    Runnable r = StampoMain.fromParameters(of());
    Assert.assertTrue(r instanceof Build);
    Build b = (Build) r;
    Assert.assertFalse(b.getSrcPath().isPresent());
  }

  @Test
  public void callDefaultWithPath() {
    Runnable r = StampoMain.fromParameters(of("--src=/my/path"));
    Build b = (Build) r;
    Assert.assertTrue(b.getSrcPath().isPresent());
    Assert.assertEquals("/my/path", b.getSrcPath().get());
  }
  
  
  @Test
  public void callBuild() {
    Runnable r = StampoMain.fromParameters(of("build"));
    Assert.assertTrue(r instanceof Build);
    Assert.assertFalse(((Build) r).getSrcPath().isPresent());
    
    Runnable r2 = StampoMain.fromParameters(of("build", "--src=/my/path"));
    Assert.assertTrue(r2 instanceof Build);
    Assert.assertEquals("/my/path", ((Build) r2).getSrcPath().get());
    Assert.assertEquals(false, ((Build) r2).isHideDraft());
    
    Runnable r3 = StampoMain.fromParameters(of("build", "--src=/my/path", "--hide-draft=true"));
    Assert.assertTrue(r3 instanceof Build);
    Assert.assertEquals("/my/path", ((Build) r3).getSrcPath().get());
    Assert.assertEquals(true, ((Build) r3).isHideDraft());
  }
  
  
  @Test
  public void callHelp() {
    Runnable r = StampoMain.fromParameters(of("help"));
    Assert.assertTrue(r instanceof Help);
  }
  
  @Test
  public void callCheck() {
    Runnable r = StampoMain.fromParameters(of("check"));
    Assert.assertTrue(r instanceof Check);
    Assert.assertFalse(((Check) r).getSrcPath().isPresent());
    
    Runnable r2 = StampoMain.fromParameters(of("check", "--src=/my/path"));
    Assert.assertTrue(r2 instanceof Check);
    Assert.assertEquals("/my/path", ((Check) r2).getSrcPath().get());
  }
  
  
  @Test
  public void callServe() {
    Runnable r = StampoMain.fromParameters(of("serve"));
    Assert.assertTrue(r instanceof Serve);
    checkServeParams(((Serve) r), Optional.empty(), 8080, "localhost", false, false);
    
    checkServeParams(((Serve) StampoMain.fromParameters(of("serve", "--src=/my/path"))), Optional.of("/my/path"), 8080, "localhost", false, false);
    
    checkServeParams(((Serve) StampoMain.fromParameters(of("serve", "--port=4242", "--src=/my/path"))), Optional.of("/my/path"), 4242, "localhost", false, false);
    
    checkServeParams(((Serve) StampoMain.fromParameters(of("serve", "--port=4242", "--hostname=derpderp", "--src=/my/path"))), Optional.of("/my/path"), 4242, "derpderp", false, false);
    
    
    checkServeParams(((Serve) StampoMain.fromParameters(of("serve", "--disable-rebuild-on-change=true"))), Optional.empty(), 8080, "localhost", true, false);
    
    checkServeParams(((Serve) StampoMain.fromParameters(of("serve", "--disable-rebuild-on-change=true", "--disable-auto-reload=true"))), Optional.empty(), 8080, "localhost", true, true);
    
  }
  
  private static void checkServeParams(Serve s, Optional<String> path, int port, String hostname, boolean disableRebuildOnChange, boolean disableAutoReload) {
    Assert.assertEquals(path, s.getSrcPath());
    Assert.assertEquals(port, s.getPort());
    Assert.assertEquals(hostname, s.getHostname());
    Assert.assertEquals(disableRebuildOnChange, s.isDisableRebuildOnChange());
    Assert.assertEquals(disableAutoReload, s.isDisableAutoReload());
  }
}
