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
import org.lockss.laaws.config.api.AususpecturlsApiDelegate;
import org.lockss.log.L4JLogger;
import org.lockss.plugin.AuUtil;
import org.lockss.spring.auth.Roles;
import org.lockss.spring.auth.AuthUtil;
import org.lockss.spring.base.BaseSpringApiServiceImpl;
import org.lockss.state.StateManager;
import org.lockss.util.JsonUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

/**
 * Service for accessing Archival Unit suspect URL versions.
 */
@Service
public class AususpecturlsApiServiceImpl extends BaseSpringApiServiceImpl
    implements AususpecturlsApiDelegate {
  private static L4JLogger log = L4JLogger.getLogger();

  /**
   * Provides the suspect URL versions of an AU given the AU identifier.
   * 
   * @param auid
   *          A String with the AU identifier.
   * @return a {@code ResponseEntity<String>} with the Archival Unit suspect URL
   *         versions if successful, or a {@code ResponseEntity<String>} with
   *         the error information otherwise.
   */
  @Override
  public ResponseEntity<String> getAuSuspectUrlVersions(String auid) {
    log.debug2("auid = {}", auid);

    // Check whether the service has not been fully initialized.
    if (!waitReady()) {
      // Yes: Notify the client.
      return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
    }

    try {
      // Validate the AUId.
      ResponseEntity<String> errorResponseEntity = validateAuid(auid);

      if (errorResponseEntity != null) {
        return errorResponseEntity;
      }

      // Get the Archival Unit suspect URL versions.
      String result = getStateManager().getAuSuspectUrlVersions(auid).toJson();
      log.debug2("result = {}", result);
      return new ResponseEntity<String>(result, HttpStatus.OK);
    } catch (IllegalArgumentException iae) {
      String message =
	  "Cannot get the suspect URL versions for auid = '" + auid + "'";
      log.error(message, iae);
      return getErrorResponseEntity(HttpStatus.BAD_REQUEST, message, iae);
    } catch (Exception e) {
      String message =
	  "Cannot get the suspect URL versions for auid = '" + auid + "'";
      log.error(message, e);
      return getErrorResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR, message,
	  e);
    }
  }

  /**
   * Updates the suspect URL versions of an Archival Unit with the provided
   * values.
   * 
   * @param auid
   *          A String with the AU identifier.
   * @param auSuspectUrlVersions
   *          A String with the Archival Unit suspect URL versions changes.
   * @param xLockssRequestCookie
   *          A String with "X-Lockss-Request-Cookie" request header.
   * @return a {@code ResponseEntity<Void>} if successful, or a
   *         {@code ResponseEntity<String>} with the error information
   *         otherwise.
   */
  @Override
  public ResponseEntity putAuSuspectUrlVersions(String auid,
      String auSuspectUrlVersions, String xLockssRequestCookie) {
    log.debug2("auid = {}", auid);
    log.debug2("auSuspectUrlVersions = {}", auSuspectUrlVersions);
    log.debug2("xLockssRequestCookie = {}", xLockssRequestCookie);

    // Check whether the service has not been fully initialized.
    if (!waitReady()) {
      // Yes: Notify the client.
      return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
    }

    // Check for required role.
    try {
      AuthUtil.checkHasRole(Roles.ROLE_AU_ADMIN);
    } catch (AccessControlException ace) {
      log.warn(ace.getMessage());
      return getErrorResponseEntity(HttpStatus.FORBIDDEN, null, ace);
    }

    try {
      // Validate the input parameters.
      ResponseEntity<String> errorResponseEntity =
	  validateAuidAndAuSuspectUrlVersions(auid, auSuspectUrlVersions);

      if (errorResponseEntity != null) {
        return errorResponseEntity;
      }

      // Update the Archival Unit suspect URL versions.
      getStateManager().updateAuSuspectUrlVersionsFromJson(auid,
	  auSuspectUrlVersions, xLockssRequestCookie);

      return new ResponseEntity<Void>(HttpStatus.OK);
    } catch (IllegalArgumentException iae) {
      String message =
	  "Cannot update the suspect URL versions for auid = '" + auid + "'";
      log.error(message, iae);
      log.error("auSuspectUrlVersions = {}", auSuspectUrlVersions);
      return getErrorResponseEntity(HttpStatus.BAD_REQUEST, message, iae);
    } catch (Exception e) {
      String message =
	  "Cannot update the suspect URL versions for auid = '" + auid + "'";
      log.error(message, e);
      log.error("auSuspectUrlVersions = {}", auSuspectUrlVersions);
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
   * Validates the AU identifier and suspect URL versions.
   * 
   * @param auid
   *          A String with the AU identifier.
   * @param auSuspectUrlVersions
   *          A String with the Archival Unit suspect URL versions.
   * @return a {@code ResponseEntity<String>} with the error response entity, or
   *         <code>null</code> if the validation succeeds.
   */
  private ResponseEntity<String> validateAuidAndAuSuspectUrlVersions(String auid,
      String auSuspectUrlVersions) throws IOException {
    // Validate the AUId.
    ResponseEntity<String> errorResponseEntity = validateAuid(auid);

    if (errorResponseEntity != null) {
      return errorResponseEntity;
    }

    // Check whether there are no AU suspect URL versions.
    if (auSuspectUrlVersions == null || auSuspectUrlVersions.isEmpty()) {
      // Yes: Report the problem.
      String message =
	  "Invalid auSuspectUrlVersions = '" + auSuspectUrlVersions + "'";
      log.error(message);
      return getErrorResponseEntity(HttpStatus.BAD_REQUEST, message, null);
    }

    // No: Validate the AUId consistency between arguments.
    String auIdInAuSuspectUrlVersions =
	AuUtil.auSuspectUrlVersionsFromJson(auSuspectUrlVersions).getAuid();

    if (auIdInAuSuspectUrlVersions != null
	&& !auIdInAuSuspectUrlVersions.isEmpty()
	&& !auIdInAuSuspectUrlVersions.equals(auid)) {
      String message = "Incompatible auId in auSuspectUrlVersions = '"
	+ auSuspectUrlVersions + "'";
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
   *          An Exception with the error exception.
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
