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
import org.lockss.config.AuConfiguration;
import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.daemon.TitleConfig;
import org.lockss.laaws.config.api.AusApiDelegate;
import org.lockss.log.L4JLogger;
import org.lockss.plugin.*;
import org.lockss.plugin.definable.NamedArchivalUnit;
import org.lockss.remote.RemoteApi;
import org.lockss.remote.RemoteApi.BatchAuStatus;
import org.lockss.servlet.DebugPanel;
import org.lockss.spring.auth.Roles;
import org.lockss.spring.auth.AuthUtil;
import org.lockss.spring.base.BaseSpringApiServiceImpl;
import org.lockss.util.*;
import org.lockss.ws.entities.ContentConfigurationResult;
import org.lockss.ws.entities.RequestAuControlResult;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Service for accessing Archival Unit configurations.
 */
@Service
public class AusApiServiceImpl extends BaseSpringApiServiceImpl
    implements AusApiDelegate {
  private static L4JLogger log = L4JLogger.getLogger();

  static final String MISSING_AU_ID_ERROR_MESSAGE = "Missing auId";
  static final String NO_SUCH_AU_ERROR_MESSAGE = "No such Archival Unit";
  static final String DISABLED_METADATA_PROCESSING_ERROR_MESSAGE =
      "Metadata processing is not enabled";
  static final String ACTION_DISABLE_METADATA_INDEXING = "Disable Indexing";
  static final String DISABLE_METADATA_INDEXING_ERROR_MESSAGE =
      "Cannot disable AU metadata indexing";
  static final String ACTION_ENABLE_METADATA_INDEXING = "Enable Indexing";
  static final String ENABLE_METADATA_INDEXING_ERROR_MESSAGE =
      "Cannot enable AU metadata indexing";

  // TODO: Avoid repeating here the values of the constants defined in
  // the not accessible MetadataExtractorManager.
  static final String PARAM_INDEXING_ENABLED =
      "org.lockss.metadataManager.indexing_enabled";
  static final boolean DEFAULT_INDEXING_ENABLED = false;

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

    // Check whether the service has not been fully initialized.
    if (!waitReady()) {
      // Yes: Notify the client.
      return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
    }

    // Check for required role
    try {
      AuthUtil.checkHasRole(Roles.ROLE_AU_ADMIN);
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

    // Check whether the service has not been fully initialized.
    if (!waitReady()) {
      // Yes: Notify the client.
      return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
    }

    try {
      Collection<AuConfiguration> result =
	  getConfigManager().retrieveAllArchivalUnitConfiguration();
      log.debug2("result = {}", result);
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

    // Check whether the service has not been fully initialized.
    if (!waitReady()) {
      // Yes: Notify the client.
      return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
    }

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
   * @param auConfiguration
   *          An AuConfiguration with the Archival Unit configuration.
   * @param auidArg
   *          The auid passed in the request, currently ignored
   * @return a {@code ResponseEntity<Void>} with the Archival Unit
   *         configuration.
   */
  @Override
  public ResponseEntity putAuConfig(String auidArg,
                                    AuConfiguration auConfiguration) {
    if (log.isDebugEnabled()) log.debug("auConfiguration = " + auConfiguration);

    // Check whether the service has not been fully initialized.
    if (!waitReady()) {
      // Yes: Notify the client.
      return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
    }

    // Check for required role
    try {
      AuthUtil.checkHasRole(Roles.ROLE_AU_ADMIN);
    } catch (AccessControlException ace) {
      log.warn(ace.getMessage());
      return new ResponseEntity<Void>(HttpStatus.FORBIDDEN);
    }

    String auId = null;

    try {
      auId = auConfiguration.getAuId();

      if (auId == null || auId.isEmpty() || !auId.equals(auidArg)) {
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
      getPluginManager().updateAuConfigFromExternalSource(auConfiguration);

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
   * Enables the metadata indexing of an archival unit.
   * 
   * @param auId A String with the identifier (auid) of the archival unit.
   * @return a {@code ResponseEntity<RequestAuControlResult>} with the result of
   *         the operation.
   */
  @Override
  public ResponseEntity putAusMdEnable(String auId) {
    log.debug2("auId = {}", auId);

    // Check whether the service has not been fully initialized.
    if (!waitReady()) {
      // Yes: Notify the client.
      return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
    }

    // Check for required role
    try {
      AuthUtil.checkHasRole(Roles.ROLE_AU_ADMIN);
    } catch (AccessControlException ace) {
      log.warn(ace.getMessage());
      return new ResponseEntity<Void>(HttpStatus.FORBIDDEN);
    }

    // Add to the audit log a reference to this operation, if necessary.
    try {
      audit(ACTION_ENABLE_METADATA_INDEXING, auId);
    } catch (AccessControlException ace) {
      log.warn(ace.getMessage());
      return new ResponseEntity<Void>(HttpStatus.FORBIDDEN);
    }

    RequestAuControlResult result = null;

    try {
      if (!ConfigManager.getCurrentConfig().getBoolean(
	  PARAM_INDEXING_ENABLED, DEFAULT_INDEXING_ENABLED)) {
	result = new RequestAuControlResult(auId, false,
	    DISABLED_METADATA_PROCESSING_ERROR_MESSAGE);
	log.debug2("result = {}", result);
	return new ResponseEntity<RequestAuControlResult>(result,
	    HttpStatus.CONFLICT);
      }

      // Handle a missing auId.
      if (StringUtil.isNullString(auId)) {
	result = new RequestAuControlResult(auId, false,
	    MISSING_AU_ID_ERROR_MESSAGE);
	log.debug2("result = {}", result);
	return new ResponseEntity<RequestAuControlResult>(result,
	    HttpStatus.BAD_REQUEST);
      }

      // Get the Archival Unit to have its metadata indexing enabled.
      ArchivalUnit au =
	  LockssDaemon.getLockssDaemon().getPluginManager().getAuFromId(auId);
      log.trace("au = {}", au);

      // Handle a missing Archival Unit.
      if (au == null) {
	result =
	    new RequestAuControlResult(auId, false, NO_SUCH_AU_ERROR_MESSAGE);
	log.debug2("result = {}", result);
	return new ResponseEntity<RequestAuControlResult>(result,
	    HttpStatus.BAD_REQUEST);
      }

      try {
	// TODO: Implement via AU state.
	//metadataMgr.enableAuIndexing(au);
	result = new RequestAuControlResult(auId, true, null);
      } catch (Exception e) {
	result = new RequestAuControlResult(auId, false,
	    ENABLE_METADATA_INDEXING_ERROR_MESSAGE + ": " + e.getMessage());
	return new ResponseEntity<RequestAuControlResult>(result,
	    HttpStatus.INTERNAL_SERVER_ERROR);
      }

      log.debug2("result = {}", result);
      return new ResponseEntity<RequestAuControlResult>(result, HttpStatus.OK);
    } catch (Exception e) {
      String message = "Cannot putAusReactivate()";
      log.error(message, e);
      return new ResponseEntity<String>(message,
  	  HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Disables the metadata indexing of an archival unit.
   * 
   * @param auId
   *          A String with the identifier (auid) of the archival unit.
   * @return a {@code ResponseEntity<RequestAuControlResult>} with the result of
   *         the operation.
   */
  @Override
  public ResponseEntity putAusMdDisable(String auId) {
    log.debug2("auId = {}", auId);

    // Check whether the service has not been fully initialized.
    if (!waitReady()) {
      // Yes: Notify the client.
      return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
    }

    // Check for required role
    try {
      AuthUtil.checkHasRole(Roles.ROLE_AU_ADMIN);
    } catch (AccessControlException ace) {
      log.warn(ace.getMessage());
      return new ResponseEntity<Void>(HttpStatus.FORBIDDEN);
    }

    // Add to the audit log a reference to this operation, if necessary.
    try {
      audit(ACTION_DISABLE_METADATA_INDEXING, auId);
    } catch (AccessControlException ace) {
      log.warn(ace.getMessage());
      return new ResponseEntity<Void>(HttpStatus.FORBIDDEN);
    }

    RequestAuControlResult result = null;

    try {
      if (!ConfigManager.getCurrentConfig().getBoolean(
	  PARAM_INDEXING_ENABLED, DEFAULT_INDEXING_ENABLED)) {
	result = new RequestAuControlResult(auId, false,
	    DISABLED_METADATA_PROCESSING_ERROR_MESSAGE);
	log.debug2("result = {}", result);
	return new ResponseEntity<RequestAuControlResult>(result,
	    HttpStatus.CONFLICT);
      }

      // Handle a missing auId.
      if (StringUtil.isNullString(auId)) {
	result = new RequestAuControlResult(auId, false,
	    MISSING_AU_ID_ERROR_MESSAGE);
	log.debug2("result = {}", result);
	return new ResponseEntity<RequestAuControlResult>(result,
	    HttpStatus.BAD_REQUEST);
      }

      // Get the Archival Unit to have its metadata indexing disabled.
      ArchivalUnit au =
	  LockssDaemon.getLockssDaemon().getPluginManager().getAuFromId(auId);
      log.trace("au = {}", au);

      // Handle a missing Archival Unit.
      if (au == null) {
	result =
	    new RequestAuControlResult(auId, false, NO_SUCH_AU_ERROR_MESSAGE);
	log.debug2("result = {}", result);
	return new ResponseEntity<RequestAuControlResult>(result,
	    HttpStatus.BAD_REQUEST);
      }

      try {
	// TODO: Implement via AU state.
	//metadataMgr.disableAuIndexing(au);
	result = new RequestAuControlResult(auId, true, null);
      } catch (Exception e) {
	result = new RequestAuControlResult(auId, false,
	    DISABLE_METADATA_INDEXING_ERROR_MESSAGE + ": " + e.getMessage());
	return new ResponseEntity<RequestAuControlResult>(result,
	    HttpStatus.INTERNAL_SERVER_ERROR);
      }

      log.debug2("result = {}", result);
      return new ResponseEntity<RequestAuControlResult>(result, HttpStatus.OK);
    } catch (Exception e) {
      String message = "Cannot putAusReactivate()";
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
   * Adds to the audit log a reference to this operation, if necessary.
   * 
   * @param action
   *          A String with the name of the operation.
   * @param auId
   *          A String with the identifier (auid) of the archival unit.
   * @throws AccessControlException if the user cannot be validated.
   */
  private void audit(String action, String auId) throws AccessControlException {
    log.debug2("action = {}", action);
    log.debug2("auId = {}", auId);

    String userName =
	SecurityContextHolder.getContext().getAuthentication().getName();
    log.trace("userName = {}", userName);

    // Get the user account.
    UserAccount userAccount = null;

    try {
      userAccount =
          LockssDaemon.getLockssDaemon().getAccountManager().getUser(userName);
      log.trace("userAccount = {}", userAccount);
    } catch (Exception e) {
      log.error("userName = {}", userName);
      log.error("LockssDaemon.getLockssDaemon().getAccountManager()."
          + "getUser(" + userName + ")", e);
      throw new AccessControlException("Unable to get user '" + userName + "'");
    }

    if (userAccount != null && !DebugPanel.noAuditActions.contains(action)) {
      userAccount.auditableEvent("Called AusApi web service operation '"
	  + action + "' AU ID: " + auId);
    }
  }
}
