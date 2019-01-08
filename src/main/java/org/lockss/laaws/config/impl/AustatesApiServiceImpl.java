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
import org.lockss.app.LockssDaemon;
import org.lockss.laaws.config.api.AustatesApiDelegate;
import org.lockss.log.L4JLogger;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.AuUtil;
import org.lockss.plugin.PluginManager;
import org.lockss.spring.auth.Roles;
import org.lockss.spring.auth.SpringAuthenticationFilter;
import org.lockss.state.AuState;
import org.lockss.state.StateManager;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

/**
 * Service for accessing Archival Unit states.
 */
@Service
public class AustatesApiServiceImpl implements AustatesApiDelegate {
  private static L4JLogger log = L4JLogger.getLogger();

  /**
   * Provides the state of an AU given the AU identifier.
   * 
   * @param auid
   *          A String with the AU identifier.
   * @return a {@code ResponseEntity<String>} with the Archival Unit state.
   */
  @Override
  public ResponseEntity<String> getAuState(String auid) {
    log.debug2("auid = {}", auid);

    try {
      // Validation.
      if (auid == null || auid.isEmpty()) {
	String message = "Invalid auid = '" + auid + "'";
	log.error(message);
	return new ResponseEntity<String>(toJsonError(message),
	    HttpStatus.BAD_REQUEST);
      }

      // Get the Archival Unit.
      ArchivalUnit au = getPluginManager().getAuFromIdIfExists(auid);

      if (au == null) {
	String message = "No Archival Unit found for auid = '" + auid + "'";
	log.error(message);
	return new ResponseEntity<String>(toJsonError(message),
	    HttpStatus.NOT_FOUND);
      }

      // Get the Archival Unit state.
      String result = getStateManager().getAuState(au).getBean().toJson();
      log.debug2("result = {}", result);
      return new ResponseEntity<String>(result, HttpStatus.OK);
    } catch (Exception e) {
      String message = "Cannot getAuState() for auid = '" + auid + "'";
      log.error(message, e);
      return new ResponseEntity<String>(toJsonError(message),
	  HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Stores the provided Archival Unit state.
   * 
   * @param auid
   *          A String with the AU identifier.
   * @param auState
   *          A String with the Archival Unit state.
   * @return a {@code ResponseEntity<String>} with the Archival Unit state.
   */
  @Override
  public ResponseEntity<String> postAuState(String auid, String auState) {
    log.debug2("auid = {}", auid);
    log.debug2("auState = {}", auState);

    // Check authorization.
    try {
      SpringAuthenticationFilter.checkAuthorization(Roles.ROLE_AU_ADMIN);
    } catch (AccessControlException ace) {
      log.warn(ace.getMessage());
      return new ResponseEntity<String>(HttpStatus.FORBIDDEN);
    }

    try {
      // Validation.
      if (auid == null || auid.isEmpty()) {
	String message = "Invalid auId = '" + auid + "'";
	log.error(message);
	return new ResponseEntity<String>(toJsonError(message),
	    HttpStatus.BAD_REQUEST);
      }

      if (auState == null || auState.isEmpty()) {
	String message = "Invalid auState = '" + auState + "'";
	log.error(message);
	return new ResponseEntity<String>(toJsonError(message),
	    HttpStatus.BAD_REQUEST);
      }

      // Get the Archival Unit.
      ArchivalUnit au = getPluginManager().getAuFromIdIfExists(auid);

      if (au == null) {
	String message = "No Archival Unit found for auid = '" + auid + "'";
	log.error(message);
	return new ResponseEntity<String>(toJsonError(message),
	    HttpStatus.NOT_FOUND);
      }

      // Add the Archival Unit state.
      StateManager stateManager = getStateManager();
      AuState aus = stateManager.getAuState(au);
      aus.getBean().updateFromJson(auState, getLockssDaemon());
      stateManager.storeAuState(aus);

      // Get the added Archival Unit state.
      String result = stateManager.getAuState(getPluginManager()
	  .getAuFromIdIfExists(auid)).getBean().toJson();
      log.debug2("result = {}", result);
      return new ResponseEntity<String>(result, HttpStatus.OK);
    } catch (IllegalStateException ise) {
      String message = "Cannot postAuState(): ";
      log.error(message, ise);
      return new ResponseEntity<String>(toJsonError(message + ise.getMessage()),
	  HttpStatus.BAD_REQUEST);
    } catch (Exception e) {
      String message = "Cannot postAuState() for auid = '" + auid + "'";
      log.error(message, e);
      log.error("auState = {}", auState);
      return new ResponseEntity<String>(toJsonError(message + e.getMessage()),
	  HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Updates the state of an Archival Unit with the provided values.
   * 
   * @param auid
   *          A String with the AU identifier.
   * @param auState
   *          A String with the Archival Unit state changes.
   * @return a {@code ResponseEntity<String>} with the Archival Unit state.
   */
  @Override
  public ResponseEntity<String> patchAuState(String auid, String auState) {
    log.debug2("auid = {}", auid);
    log.debug2("auState = {}", auState);

    // Check authorization.
    try {
      SpringAuthenticationFilter.checkAuthorization(Roles.ROLE_AU_ADMIN);
    } catch (AccessControlException ace) {
      log.warn(ace.getMessage());
      return new ResponseEntity<String>(HttpStatus.FORBIDDEN);
    }

    try {
      // Validation.
      if (auid == null || auid.isEmpty()) {
	String message = "Invalid auId = '" + auid + "'";
	log.error(message);
	return new ResponseEntity<String>(toJsonError(message),
	    HttpStatus.BAD_REQUEST);
      }

      if (auState == null || auState.isEmpty()) {
	String message = "Invalid auState = '" + auState + "'";
	log.error(message);
	return new ResponseEntity<String>(toJsonError(message),
	    HttpStatus.BAD_REQUEST);
      }

      // Get the Archival Unit.
      ArchivalUnit au = getPluginManager().getAuFromIdIfExists(auid);

      if (au == null) {
	String message = "No Archival Unit found for auid = '" + auid + "'";
	log.error(message);
	return new ResponseEntity<String>(toJsonError(message),
	    HttpStatus.NOT_FOUND);
      }

      // Update the Archival Unit state.
      StateManager stateManager = getStateManager();
      AuState aus = stateManager.getAuState(au);
      aus.getBean().updateFromJson(auState, getLockssDaemon());
      stateManager.updateAuState(aus, AuUtil.jsonToMap(auState).keySet());

      // Get the updated Archival Unit state.
      String result = stateManager.getAuState(getPluginManager()
	  .getAuFromIdIfExists(auid)).getBean().toJson();
      log.debug2("result = {}", result);
      return new ResponseEntity<String>(result, HttpStatus.OK);
    } catch (Exception e) {
      String message = "Cannot postAuState() for auid = '" + auid + "'";
      log.error(message, e);
      log.error("auState = {}", auState);
      return new ResponseEntity<String>(toJsonError(message + e.getMessage()),
	  HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Provides the daemon.
   *
   * @return a LockssDaemon with the daemon.
   */
  private LockssDaemon getLockssDaemon() {
    return LockssDaemon.getLockssDaemon();
  }

  /**
   * Provides the plugin manager.
   *
   * @return a PluginManager with the plugin manager.
   */
  private PluginManager getPluginManager() {
    return getLockssDaemon().getPluginManager();
  }

  /**
   * Provides the state manager.
   *
   * @return a StateManager with the state manager.
   */
  private StateManager getStateManager() {
    return getLockssDaemon().getManagerByType(StateManager.class);
  }

  /**
   * Provides an error message formatted in JSON.
   * 
   * @param message
   *          A String with the text of the message to be formatted.
   * @return a String with the error message formatted in JSON.
   */
  private String toJsonError(String message) {
    return "{\"error\": {\"message\":\"" + message + "\"}}";
  }
}
