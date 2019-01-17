/*

Copyright (c) 2000-2019 Board of Trustees of Leland Stanford Jr. University,
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
import java.util.Collection;
import java.util.Map;
import org.lockss.app.LockssDaemon;
import org.lockss.config.AuConfiguration;
import org.lockss.config.ConfigManager;
import org.lockss.laaws.config.api.AusApiDelegate;
import org.lockss.log.L4JLogger;
import org.lockss.plugin.PluginManager;
import org.lockss.spring.auth.Roles;
import org.lockss.spring.auth.SpringAuthenticationFilter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

/**
 * Service for accessing Archival Unit configurations.
 */
@Service
public class AusApiServiceImpl implements AusApiDelegate {
  private static L4JLogger log = L4JLogger.getLogger();

  /**
   * Deletes the configuration for an AU given the AU identifier.
   * 
   * @param auid
   *          A String with the AU identifier.
   * @return {@code ResponseEntity<AuConfiguration>} with the deleted
   *         configuration.
   */
  @Override
  public ResponseEntity deleteAuConfig(String auid) {
    if (log.isDebugEnabled()) log.debug("auid = " + auid);

    // Check authorization.
    try {
      SpringAuthenticationFilter.checkAuthorization(Roles.ROLE_AU_ADMIN);
    } catch (AccessControlException ace) {
      log.warn(ace.getMessage());
      return new ResponseEntity<Void>(HttpStatus.FORBIDDEN);
    }

    try {
      if (auid == null || auid.isEmpty()) {
	String message = "Invalid auid = '" + auid + "'";
	log.error(message);
	return new ResponseEntity<String>(message, HttpStatus.BAD_REQUEST);
      }

      PluginManager pluginManager = getPluginManager();

      AuConfiguration result = pluginManager.getStoredAuConfiguration(auid);
      if (log.isDebugEnabled()) log.debug("result = " + result);

      pluginManager.deleteAuConfiguration(auid);
      return new ResponseEntity<AuConfiguration>(result, HttpStatus.OK);
    } catch (IllegalArgumentException iae) {
      String message = "No Archival Unit found for auid = '" + auid + "'";
      log.error(message);
      return new ResponseEntity<String>(message, HttpStatus.NOT_FOUND);
    } catch (Exception e) {
      String message = "Cannot deleteAuConfig() for auid = '" + auid + "'";
      log.error(message, e);
      return new ResponseEntity<String>(message,
	  HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Provides the configuration for all AUs.
   * 
   * @return a {@code ResponseEntity<Collection<AuConfiguration>>} with the
   *         configuration for all AUs.
   */
  @Override
  public ResponseEntity getAllAuConfig() {
    if (log.isDebugEnabled()) log.debug("Invoked");

    try {
      Collection<AuConfiguration> result =
	  getConfigManager().retrieveAllArchivalUnitConfiguration();
      if (log.isDebugEnabled()) log.debug("result = " + result);
      return new ResponseEntity<Collection<AuConfiguration>>(result,
	  HttpStatus.OK);
    } catch (Exception e) {
      String message = "Cannot getAllAuConfig()";
      log.error(message, e);
      return new ResponseEntity<String>(message,
	  HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Provides the configuration for an AU given the AU identifier.
   * 
   * @param auid
   *          A String with the AU identifier.
   * @return a {@code ResponseEntity<AuConfiguration>} with the AU
   *         configuration.
   */
  @Override
  public ResponseEntity getAuConfig(String auid) {
    if (log.isDebugEnabled()) log.debug("auid = " + auid);

    try {
      if (auid == null || auid.isEmpty()) {
	String message = "Invalid auid = '" + auid + "'";
	log.error(message);
	return new ResponseEntity<String>(message, HttpStatus.BAD_REQUEST);
      }

      AuConfiguration result =
	  getPluginManager().getStoredAuConfiguration(auid);
      if (log.isDebugEnabled()) log.debug("result = " + result);
      return new ResponseEntity<AuConfiguration>(result, HttpStatus.OK);
    } catch (IllegalArgumentException iae) {
      String message = "No Archival Unit found for auid = '" + auid + "'";
      log.error(message);
      return new ResponseEntity<String>(message, HttpStatus.NOT_FOUND);
    } catch (Exception e) {
      String message = "Cannot getAuConfig() for auid = '" + auid + "'";
      log.error(message, e);
      return new ResponseEntity<String>(message,
	  HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Stores the provided Archival Unit configuration.
   * 
   * @param AuConfiguration
   *          An AuConfiguration with the Archival Unit configuration.
   * @return a {@code ResponseEntity<Void>} with the Archival Unit
   *         configuration.
   */
  @Override
  public ResponseEntity putAuConfig(AuConfiguration auConfiguration) {
    if (log.isDebugEnabled()) log.debug("auConfiguration = " + auConfiguration);

    // Check authorization.
    try {
      SpringAuthenticationFilter.checkAuthorization(Roles.ROLE_AU_ADMIN);
    } catch (AccessControlException ace) {
      log.warn(ace.getMessage());
      return new ResponseEntity<Void>(HttpStatus.FORBIDDEN);
    }

    String auId = null;

    try {
      auId = auConfiguration.getAuId();

      if (auId == null || auId.isEmpty()) {
	String message = "Invalid auId = '" + auId + "'";
	log.error(message);
	return new ResponseEntity<String>(message, HttpStatus.BAD_REQUEST);
      }

      Map<String, String> auConfig = auConfiguration.getAuConfig();
      if (auConfig == null || auConfig.isEmpty()) {
	String message =
	    "Configuration to be stored is not allowed to be null or empty";
	log.error(message);
	return new ResponseEntity<String>(message, HttpStatus.BAD_REQUEST);
      }

      // Update the Archival Unit configuration.
      getPluginManager().updateAuInDatabase(auConfiguration);

      return new ResponseEntity<Void>(HttpStatus.OK);
    } catch (IllegalArgumentException iae) {
      String message = "No Archival Unit found for auid = '" + auId + "'";
      log.error(message);
      return new ResponseEntity<String>(message, HttpStatus.NOT_FOUND);
    } catch (Exception e) {
      String message = "Cannot putAuConfig()";
      log.error(message, e);
      return new ResponseEntity<String>(message,
	  HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Provides the configuration manager.
   *
   * @return a ConfigManager with the configuration manager.
   */
  private ConfigManager getConfigManager() {
    return ConfigManager.getConfigManager();
  }

  /**
   * Provides the plugin manager.
   *
   * @return a PluginManager with the plugin manager.
   */
  private PluginManager getPluginManager() {
    return LockssDaemon.getLockssDaemon().getPluginManager();
  }
}
