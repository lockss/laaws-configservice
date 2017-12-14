/*

 Copyright (c) 2017 Board of Trustees of Leland Stanford Jr. University,
 all rights reserved.

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in
 all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL
 STANFORD UNIVERSITY BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

 Except as contained in this notice, the name of Stanford University shall not
 be used in advertising or otherwise to promote the sale, use or other dealings
 in this Software without prior written authorization from Stanford University.

 */
package org.lockss.laaws.config.server;

// import org.lockss.app.LockssApp;
import org.lockss.app.LockssDaemon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Startup code.
 */
public class LaawsConfigApp extends LockssDaemon {
  private static final Logger log =
      LoggerFactory.getLogger(LaawsConfigApp.class);

  // Manager descriptors.  The order of this table determines the order in
  // which managers are initialized and started.
  private static final ManagerDesc[] myManagerDescs = {
    new ManagerDesc(ACCOUNT_MANAGER, "org.lockss.account.AccountManager"),
    new ManagerDesc(PLUGIN_MANAGER, "org.lockss.plugin.PluginManager"),
    new ManagerDesc(COUNTER_REPORTS_MANAGER,
		    "org.lockss.exporter.counter.CounterReportsManager"),
    new ManagerDesc(SERVLET_MANAGER, "org.lockss.servlet.AdminServletManager"),
  };

  public static void main(String[] args) {
    AppSpec spec = new AppSpec()
      .setName("Config Service")
      .setArgs(args)
//       .addAppConfig(PluginManager.PARAM_START_ALL_AUS, "true")
      .setAppManagers(myManagerDescs);
    startStatic(LaawsConfigApp.class, spec);
  }

  public LaawsConfigApp() throws Exception {
    super();
  }
}
