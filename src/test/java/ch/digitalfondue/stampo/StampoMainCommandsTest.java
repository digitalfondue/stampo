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

import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import ch.digitalfondue.stampo.command.Build;
import ch.digitalfondue.stampo.command.Check;
import ch.digitalfondue.stampo.command.Help;
import ch.digitalfondue.stampo.command.Serve;

public class StampoMainCommandsTest {

  private static String[] of(String... strings) {
    return strings;
  }

  @Test
  public void callDefault() {
    Runnable r = StampoMain.fromParameters(of());
    Assert.assertTrue(r instanceof Build);
    Build b = (Build) r;
    Assert.assertTrue(b.getPath().isEmpty());
  }

  @Test
  public void callDefaultWithPath() {
    Runnable r = StampoMain.fromParameters(of("/my/path"));
    Build b = (Build) r;
    Assert.assertTrue(b.getPath().size() == 1);
    Assert.assertEquals("/my/path", b.getPath().get(0));
  }
  
  
  @Test
  public void callBuild() {
    Runnable r = StampoMain.fromParameters(of("build"));
    Assert.assertTrue(r instanceof Build);
    Assert.assertTrue(((Build) r).getPath().isEmpty());
    
    Runnable r2 = StampoMain.fromParameters(of("build", "/my/path"));
    Assert.assertTrue(r2 instanceof Build);
    Assert.assertEquals("/my/path", ((Build) r2).getPath().get(0));
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
    Assert.assertTrue(((Check) r).getPath().isEmpty());
    
    Runnable r2 = StampoMain.fromParameters(of("check", "/my/path"));
    Assert.assertTrue(r2 instanceof Check);
    Assert.assertEquals("/my/path", ((Check) r2).getPath().get(0));
  }
  
  
  @Test
  public void callServe() {
    Runnable r = StampoMain.fromParameters(of("serve"));
    Assert.assertTrue(r instanceof Serve);
    checkServeParams(((Serve) r), Collections.emptyList(), 8080, "localhost", false, false);
    
    checkServeParams(((Serve) StampoMain.fromParameters(of("serve", "/my/path"))), Collections.singletonList("/my/path"), 8080, "localhost", false, false);
    
    checkServeParams(((Serve) StampoMain.fromParameters(of("serve", "--port=4242", "/my/path"))), Collections.singletonList("/my/path"), 4242, "localhost", false, false);
    
    checkServeParams(((Serve) StampoMain.fromParameters(of("serve", "--port=4242", "--hostname=derpderp", "/my/path"))), Collections.singletonList("/my/path"), 4242, "derpderp", false, false);
    
    
    checkServeParams(((Serve) StampoMain.fromParameters(of("serve", "--disable-rebuild-on-change"))), Collections.emptyList(), 8080, "localhost", true, false);
    
    checkServeParams(((Serve) StampoMain.fromParameters(of("serve", "--disable-rebuild-on-change", "--disable-auto-reload"))), Collections.emptyList(), 8080, "localhost", true, true);
    
  }
  
  private static void checkServeParams(Serve s, List<String> path, int port, String hostname, boolean disableRebuildOnChange, boolean disableAutoReload) {
    Assert.assertEquals(path, s.getPath());
    Assert.assertEquals(port, s.getPort());
    Assert.assertEquals(hostname, s.getHostname());
    Assert.assertEquals(disableRebuildOnChange, s.isDisableRebuildOnChange());
    Assert.assertEquals(disableAutoReload, s.isDisableAutoReload());
  }
}
