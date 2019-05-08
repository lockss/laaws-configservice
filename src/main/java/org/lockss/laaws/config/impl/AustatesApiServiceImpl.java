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

import java.io.IOException;
import java.security.AccessControlException;
import org.lockss.app.LockssDaemon;
import org.lockss.laaws.config.api.AustatesApiDelegate;
import org.lockss.log.L4JLogger;
import org.lockss.plugin.AuUtil;
import org.lockss.spring.auth.Roles;
import org.lockss.spring.auth.SpringAuthenticationFilter;
import org.lockss.state.AuStateBean;
import org.lockss.state.StateManager;
import org.lockss.util.JsonUtil;
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
   * @return a {@code ResponseEntity<String>} with the Archival Unit state if
   *         successful, or a {@code ResponseEntity<String>} with the error
   *         information otherwise..
   */
  @Override
  public ResponseEntity<String> getAuState(String auid) {
    log.debug2("auid = {}", auid);

    try {
      // Validate the AUId.
      ResponseEntity<String> errorResponseEntity = validateAuid(auid);

      if (errorResponseEntity != null) {
        return errorResponseEntity;
      }

      // Get the Archival Unit state.
      String result = getStateManager().getAuStateBean(auid).toJson();
      log.debug2("result = {}", result);
      return new ResponseEntity<String>(result, HttpStatus.OK);
    } catch (IllegalArgumentException iae) {
      String message = "Cannot get the state for auid = '" + auid + "'";
      log.error(message, iae);
      return getErrorResponseEntity(HttpStatus.BAD_REQUEST, message, iae);
    } catch (Exception e) {
      String message = "Cannot get the state for auid = '" + auid + "'";
      log.error(message, e);
      return getErrorResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR, message,
	  e);
    }
  }

  /**
   * Updates the state of an Archival Unit with the provided values.
   * 
   * @param auid
   *          A String with the AU identifier.
   * @param auState
   *          A String with the Archival Unit state changes.
   * @param xLockssRequestCookie
   *          A String with "X-Lockss-Request-Cookie" request header.
   * @return a {@code ResponseEntity<Void>} if successful, or a
   *         {@code ResponseEntity<String>} with the error information
   *         otherwise.
   */
  @Override
  public ResponseEntity patchAuState(String auid, String auState,
      String xLockssRequestCookie) {
    log.debug2("auid = {}", auid);
    log.debug2("auState = {}", auState);
    log.debug2("xLockssRequestCookie = {}", xLockssRequestCookie);

    // Check authorization.
    try {
      SpringAuthenticationFilter.checkAuthorization(Roles.ROLE_AU_ADMIN);
    } catch (AccessControlException ace) {
      log.warn(ace.getMessage());
      return getErrorResponseEntity(HttpStatus.FORBIDDEN, null, ace);
    }

    try {
      // Validate the input parameters.
      ResponseEntity<String> errorResponseEntity =
	  validateAuidAndAustate(auid, auState);

      if (errorResponseEntity != null) {
        return errorResponseEntity;
      }

      // Update the Archival Unit state.
      getStateManager().updateAuStateFromJson(auid, auState,
	  xLockssRequestCookie);

      return new ResponseEntity<Void>(HttpStatus.OK);
    } catch (IllegalArgumentException iae) {
      String message = "Cannot update the state for auid = '" + auid + "'";
      log.error(message, iae);
      log.error("auState = {}", auState);
      return getErrorResponseEntity(HttpStatus.BAD_REQUEST, message, iae);
    } catch (Exception e) {
      String message = "Cannot update the state for auid = '" + auid + "'";
      log.error(message, e);
      log.error("auState = {}", auState);
      return getErrorResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR, message,
	  e);
    }
  }

  /**
   * Validates the AU identifier.
   * 
   * @param auid
   *          A String with the AU identifier.
   * @return a {@code ResponseEntity<String>} with the error response entity, or
   *         <code>null</code> if the validation succeeds.
   */
  private ResponseEntity<String> validateAuid(String auid) {
    // Check whether there is no AUId.
    if (auid == null || auid.isEmpty()) {
      // Yes: Report the problem.
      String message = "Invalid auId = '" + auid + "'";
      log.error(message);
      return getErrorResponseEntity(HttpStatus.BAD_REQUEST, message, null);
    }

    // No: Success.
    return null;
  }

  /**
   * Validates the AU identifier and state.
   * 
   * @param auid
   *          A String with the AU identifier.
   * @param auState
   *          A String with the Archival Unit state.
   * @return a {@code ResponseEntity<String>} with the error response entity, or
   *         <code>null</code> if the validation succeeds.
   */
  private ResponseEntity<String> validateAuidAndAustate(String auid,
      String auState) throws IOException {
    // Validate the AUId.
    ResponseEntity<String> errorResponseEntity = validateAuid(auid);

    if (errorResponseEntity != null) {
      return errorResponseEntity;
    }

    // Check whether there is no AU state.
    if (auState == null || auState.isEmpty()) {
      // Yes: Report the problem.
      String message = "Invalid auState = '" + auState + "'";
      log.error(message);
      return getErrorResponseEntity(HttpStatus.BAD_REQUEST, message, null);
    }

    // No: Validate the AUId consistency between arguments.
    String auIdInAuState =
	AuUtil.updateFromJson(new AuStateBean(), auState).getAuId();

    if (auIdInAuState != null && !auIdInAuState.isEmpty()
	&& !auIdInAuState.equals(auid)) {
      String message = "Incompatible auId in auState = '" + auState + "'";
      log.error(message);
      log.error("auid = {}",auid);
      return getErrorResponseEntity(HttpStatus.BAD_REQUEST, message, null);
    }

    // Success.
    return null;
  }

  /**
   * Provides the state manager.
   *
   * @return a StateManager with the state manager.
   */
  private StateManager getStateManager() {
    return LockssDaemon.getLockssDaemon().getManagerByType(StateManager.class);
  }

  /**
   * Provides the response entity when there is an error.
   * 
   * @param status
   *          An HttpStatus with the error HTTP status.
   * @param message
   *          A String with the error message.
   * @param e
   *          An Exception with theerror exception.
   * @return a {@code ResponseEntity<String>} with the error response entity.
   */
  private ResponseEntity<String> getErrorResponseEntity(HttpStatus status,
      String message, Exception e) {
    String errorMessage = message;

    if (e != null) {
      if (errorMessage == null) {
	errorMessage = e.getMessage();
      } else {
	errorMessage = errorMessage + " - " + e.getMessage();
      }
    }

    return new ResponseEntity<String>(JsonUtil.toJsonError(status.value(),
	errorMessage), status);
  }
}
