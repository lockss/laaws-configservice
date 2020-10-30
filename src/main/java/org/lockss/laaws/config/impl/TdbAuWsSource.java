/*

Copyright (c) 2014-2020 Board of Trustees of Leland Stanford Jr. University,
all rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this
list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation and/or
other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

 */
package org.lockss.laaws.config.impl;

import java.util.Map;
import org.lockss.app.LockssDaemon;
import org.lockss.config.TdbAu;
import org.lockss.plugin.Plugin;
import org.lockss.plugin.PluginManager;
import org.lockss.ws.entities.TdbAuWsResult;
import org.lockss.ws.entities.TdbPublisherWsResult;
import org.lockss.ws.entities.TdbTitleWsResult;

/**
 * Container for the information that is used as the source for a query related
 * to title database archival units.
 */
public class TdbAuWsSource extends TdbAuWsResult {
  private TdbAu.Id tdbAuId;
  private TdbAu tdbAu = null;

  private boolean auIdPopulated = false;
  private boolean namePopulated = false;
  private boolean pluginNamePopulated = false;
  private boolean tdbTitlePopulated = false;
  private boolean tdbPublisherPopulated = false;
  private boolean downPopulated = false;
  private boolean activePopulated = false;
  private boolean paramsPopulated = false;
  private boolean attrsPopulated = false;
  private boolean propsPopulated = false;

  private LockssDaemon theDaemon = null;
  private PluginManager pluginMgr = null;
  private Plugin plugin = null;

  /**
   * Constructor.
   * 
   * @param tdbAuId A TdbAu.Id with the identifier of the TDB AU.
   */
  public TdbAuWsSource(TdbAu.Id tdbAuId) {
    this.tdbAuId = tdbAuId;
  }

  @Override
  public String getAuId() {
    if (!auIdPopulated) {
      try {
	setAuId(getTdbAu().getAuId(getPluginManager()));
      } catch (IllegalStateException ise) {
	// Do nothing.
      }

      auIdPopulated = true;
    }

    return super.getAuId();
  }

  @Override
  public String getName() {
    if (!namePopulated) {
      setName(getTdbAu().getName());
      namePopulated = true;
    }

    return super.getName();
  }

  @Override
  public String getPluginName() {
    if (!pluginNamePopulated) {
      if (getPlugin() != null) {
	setPluginName(plugin.getPluginId());
      }

      pluginNamePopulated = true;
    }

    return super.getPluginName();
  }

  @Override
  public TdbTitleWsResult getTdbTitle() {
    if (!tdbTitlePopulated) {
      setTdbTitle(new TdbTitleWsSource(getTdbAu().getTdbTitle()));
      tdbTitlePopulated = true;
    }

    return super.getTdbTitle();
  }

  @Override
  public TdbPublisherWsResult getTdbPublisher() {
    if (!tdbPublisherPopulated) {
      setTdbPublisher(new TdbPublisherWsSource(getTdbAu().getTdbPublisher()));
      tdbPublisherPopulated = true;
    }

    return super.getTdbPublisher();
  }

  @Override
  public Boolean getDown() {
    if (!downPopulated) {
      setDown(getTdbAu().isDown());
      downPopulated = true;
    }

    return super.getDown();
  }

  @Override
  public Boolean getActive() {
    if (!activePopulated) {
      String auId = getAuId();

      if (auId == null) {
	setActive(false);
      } else {
	setActive(null != getPluginManager().getAuFromId(auId));
      }

      activePopulated = true;
    }

    return super.getActive();
  }

  @Override
  public Map<String, String> getParams() {
    if (!paramsPopulated) {
      setParams(getTdbAu().getParams());
      paramsPopulated = true;
    }

    return super.getParams();
  }

  @Override
  public Map<String, String> getAttrs() {
    if (!attrsPopulated) {
      setAttrs(getTdbAu().getAttrs());
      attrsPopulated = true;
    }

    return super.getAttrs();
  }

  @Override
  public Map<String, String> getProps() {
    if (!propsPopulated) {
      setProps(getTdbAu().getProperties());
      propsPopulated = true;
    }

    return super.getProps();
  }

  /**
   * Provides the title database archival unit, initializing it if necessary.
   * 
   * @return a TdbAu with the title database archival unit.
   */
  private TdbAu getTdbAu() {
    if (tdbAu == null) {
      tdbAu = tdbAuId.getTdbAu();
    }

    return tdbAu;
  }

  /**
   * Provides the daemon, initializing it if necessary.
   * 
   * @return a LockssDaemon with the daemon.
   */
  private LockssDaemon getTheDaemon() {
    if (theDaemon == null) {
      theDaemon = LockssDaemon.getLockssDaemon();
    }

    return theDaemon;
  }

  /**
   * Provides the plugin manager, initializing it if necessary.
   * 
   * @return a PluginManager with the node manager.
   */
  private PluginManager getPluginManager() {
    if (pluginMgr == null) {
      pluginMgr = getTheDaemon().getPluginManager();
    }

    return pluginMgr;
  }

  /**
   * Provides the plugin, initializing it if necessary.
   * 
   * @return a Plugin with the plugin.
   */
  private Plugin getPlugin() {
    if (plugin == null) {
      plugin = getTdbAu().getPlugin(getPluginManager());
    }

    return plugin;
  }
}
