/*

Copyright (c) 2000-2022 Board of Trustees of Leland Stanford Jr. University,
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

import java.security.AccessControlException;
import java.io.*;
import java.util.*;

import org.lockss.account.UserAccount;
import org.lockss.app.LockssDaemon;
import org.lockss.config.ConfigManager;
import org.lockss.laaws.config.api.AuidsApiDelegate;
import org.lockss.log.L4JLogger;
import org.lockss.plugin.*;
import org.lockss.plugin.definable.NamedArchivalUnit;
import org.lockss.spring.auth.Roles;
import org.lockss.spring.auth.AuthUtil;
import org.lockss.spring.base.BaseSpringApiServiceImpl;
import org.lockss.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Service for manipulating AUIDs
 */
@Service
public class AuidsApiServiceImpl extends BaseSpringApiServiceImpl
    implements AuidsApiDelegate {
  private static L4JLogger log = L4JLogger.getLogger();

  @Override
  public ResponseEntity calculateAuid(String pluginId,
                                      String handle,
                                      Object auConfig) {

    log.debug2("pluginId: {}", pluginId);
    log.debug2("handle: {}", handle);
    log.debug2("auConfig: {}", auConfig);

    // Check whether the service has not been fully initialized.
    if (!waitReady()) {
      // Yes: Notify the client.
      return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
    }

    // Check for required role
    try {
      AuthUtil.checkHasRole(Roles.ROLE_ANY);
    } catch (AccessControlException ace) {
      log.warn(ace.getMessage());
      return new ResponseEntity<Void>(HttpStatus.FORBIDDEN);
    }

    if (handle != null) {
      if (auConfig != null) {
        return stringResponse("Must not supply both auConfig and handle",
                             HttpStatus.BAD_REQUEST);
      }
      if (pluginId == null) {
        pluginId = NamedArchivalUnit.NAMED_PLUGIN_NAME;
      }
      try {
        String auid = PluginManager.generateAuId(pluginId,
                                                 PropUtil.fromArgs("handle",
                                                                   handle));
        return calcAuidResult(auid);
      } catch (IllegalArgumentException e) {
        return stringResponse("Illegal AU config: " + e.getMessage(),
                             HttpStatus.BAD_REQUEST);
      }
    }
    if (auConfig == null || pluginId == null) {
      return stringResponse("Must supply handle, or auConfig and pluginId",
                           HttpStatus.BAD_REQUEST);
    }
    Map auMap;
    try {
      // I think this is safe - Spring never passes anything but a string
      auMap = AuUtil.jsonToMap((String)auConfig);
    } catch (IOException e) {
      log.debug2("Couldn't parse auConfig: {}", auConfig, e);
      return stringResponse("Illegal map formet: " + auConfig +
                            ": " + e.getMessage(),
                            HttpStatus.BAD_REQUEST);
    }
    PluginManager pluginMgr = getPluginManager();
    String plugKey = PluginManager.pluginKeyFromName(pluginId);
    if (!pluginMgr.ensurePluginLoaded(plugKey)) {
      return stringResponse("Plugin not found: " + pluginId,
                            HttpStatus.NOT_FOUND);
    }
    Properties p =new Properties();
    p.putAll(auMap);
    try {
      String auid = PluginManager.generateAuId(pluginMgr.getPlugin(plugKey),
                                               ConfigManager.fromProperties(p));
      return calcAuidResult(auid);
    } catch (IllegalArgumentException e) {
      return stringResponse("Illegal AU config: " + e.getMessage(),
                           HttpStatus.BAD_REQUEST);
    }
  }

  ResponseEntity calcAuidResult(String auid) {
    return jsonResponse(MapUtil.map("auid", auid), HttpStatus.OK);
  }

}
