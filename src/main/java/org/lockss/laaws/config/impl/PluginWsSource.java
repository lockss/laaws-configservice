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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.lockss.app.LockssDaemon;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.Plugin;
import org.lockss.plugin.PluginManager;
import org.lockss.plugin.definable.DefinablePlugin;
import org.lockss.util.ExternalizableMap;
import org.lockss.ws.entities.PluginWsResult;

/**
 * Container for the information that is used as the source for a query related
 * to plugins.
 */
public class PluginWsSource extends PluginWsResult {
  private Plugin plugin;

  private boolean pluginIdPopulated = false;
  private boolean namePopulated = false;
  private boolean versionPopulated = false;
  private boolean typePopulated = false;
  private boolean definitionPopulated = false;
  private boolean registryPopulated = false;
  private boolean urlPopulated = false;
  private boolean auCountPopulated = false;
  private boolean publishingPlatformPopulated = false;

  private LockssDaemon theDaemon = null;
  private PluginManager pluginMgr = null;
  private PluginManager.PluginInfo pluginInfo = null;

  /**
   * Constructor.
   * 
   * @param plugin A Plugin with the plugin information.
   */
  public PluginWsSource(Plugin plugin) {
    this.plugin = plugin;
  }

  @Override
  public String getPluginId() {
    if (!pluginIdPopulated) {
      setPluginId(plugin.getPluginId());
      pluginIdPopulated = true;
    }

    return super.getPluginId();
  }

  @Override
  public String getName() {
    if (!namePopulated) {
      setName(plugin.getPluginName());
      namePopulated = true;
    }

    return super.getName();
  }

  @Override
  public String getVersion() {
    if (!versionPopulated) {
      setVersion(plugin.getVersion());
      versionPopulated = true;
    }

    return super.getVersion();
  }

  @Override
  public String getType() {
    if (!typePopulated) {
      setType(getPluginManager().getPluginType(plugin));
      typePopulated = true;
    }

    return super.getType();
  }

  @Override
  public Map<String, String> getDefinition() {
    if (!definitionPopulated) {
      if (plugin instanceof DefinablePlugin) {
        ExternalizableMap eMap = ((DefinablePlugin)plugin).getDefinitionMap();

        if (eMap.size() > 0) {
          Map<String, String> result = new HashMap<String, String>();

          for (Map.Entry<String, Object> entry : eMap.entrySet()) {
            result.put((String)entry.getKey(), entry.getValue().toString());
          }

          setDefinition(result);
        }
      }

      definitionPopulated = true;
    }

    return super.getDefinition();
  }

  @Override
  public String getRegistry() {
    if (!registryPopulated) {
      if (getPluginInfo() != null) {
	ArchivalUnit au = pluginInfo.getRegistryAu();

	if (au != null) {
	  setRegistry(au.getName());
	}
      }

      registryPopulated = true;
    }

    return super.getRegistry();
  }

  @Override
  public String getUrl() {
    if (!urlPopulated) {
      if (getPluginInfo() != null) {
	String infoCuUrl = pluginInfo.getCuUrl();

	if (infoCuUrl != null) {
	  setUrl(infoCuUrl);
	}
      }

      urlPopulated = true;
    }

    return super.getUrl();
  }

  @Override
  public Integer getAuCount() {
    if (!auCountPopulated) {
      Collection<ArchivalUnit> allAus = plugin.getAllAus();

      if (allAus != null) {
	setAuCount(allAus.size());
      }

      auCountPopulated = true;
    }

    return super.getAuCount();
  }

  @Override
  public String getPublishingPlatform() {
    if (!publishingPlatformPopulated) {
      setPublishingPlatform(plugin.getPublishingPlatform());
      publishingPlatformPopulated = true;
    }

    return super.getPublishingPlatform();
  }

  /**
   * Provides the plugin info, initializing it if necessary.
   * 
   * @return a PluginManager.PluginInfo with the plugin info.
   */
  private PluginManager.PluginInfo getPluginInfo() {
    if (pluginInfo == null) {
      if (getPluginManager().isLoadablePlugin(plugin)) {
	pluginInfo = pluginMgr.getLoadablePluginInfo(plugin);
      }
    }

    return pluginInfo;
  }

  /**
   * Provides the plugin manager, initializing it if necessary.
   * 
   * @return a PluginManager with the plugin manager.
   */
  private PluginManager getPluginManager() {
    if (pluginMgr == null) {
      pluginMgr = getTheDaemon().getPluginManager();
    }

    return pluginMgr;
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
}
