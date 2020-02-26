/*

Copyright (c) 2000-2020 Board of Trustees of Leland Stanford Jr. University,
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

import org.lockss.app.LockssDaemon;
import org.lockss.laaws.config.api.AusubstancesApiDelegate;
import org.lockss.log.L4JLogger;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.AuUtil;
import org.lockss.spring.base.BaseSpringApiServiceImpl;
import org.lockss.state.AuState;
import org.lockss.state.SubstanceChecker;
import org.lockss.util.StringUtil;
import org.lockss.ws.entities.CheckSubstanceResult;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

/**
 * Service for accessing the status of archival units.
 */
@Service
public class AusubstancesApiServiceImpl extends BaseSpringApiServiceImpl
implements AusubstancesApiDelegate {
  static final String MISSING_AU_ID_ERROR_MESSAGE = "Missing auId";
  static final String NO_SUBSTANCE_ERROR_MESSAGE =
      "No substance patterns defined for plugin";
  static final String NO_SUCH_AU_ERROR_MESSAGE = "No such Archival Unit";
  static final String UNEXPECTED_SUBSTANCE_CHECKER_ERROR_MESSAGE =
      "Error in SubstanceChecker; see log";

  private static L4JLogger log = L4JLogger.getLogger();

  /**
   * Updates the substance check of an archival unit in the system.
   * 
   * @param auId A String with the identifier of the archival unit.
   * @return a {@code ResponseEntity<CheckSubstanceResult>} with the 
   *         substance check information of the archival unit.
   */
  @Override
  public ResponseEntity<CheckSubstanceResult> putAuSubstanceCheck(String auId) {
    log.debug2("auId = {}", auId);

    // Check whether the service has not been fully initialized.
    if (!waitReady()) {
      // Yes: Notify the client.
      return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
    }

    // Input validation.
    if (StringUtil.isNullString(auId)) {
      return new ResponseEntity<>(new CheckSubstanceResult(auId, null, null,
	  MISSING_AU_ID_ERROR_MESSAGE), HttpStatus.OK);
    }

    try {
      // Get the Archival Unit to be checked.
      ArchivalUnit au =
  	LockssDaemon.getLockssDaemon().getPluginManager().getAuFromId(auId);
      log.trace("au = {}", au);

      // Handle a missing Archival Unit.
      if (au == null) {
	return new ResponseEntity<>(new CheckSubstanceResult(auId, null, null,
	    NO_SUCH_AU_ERROR_MESSAGE), HttpStatus.OK);
      }

      // Create the substance checker.
      SubstanceChecker subChecker = new SubstanceChecker(au);

      if (!subChecker.isEnabled()) {
	return new ResponseEntity<>(new CheckSubstanceResult(auId, null, null,
	    NO_SUBSTANCE_ERROR_MESSAGE), HttpStatus.OK);
      }

      // Get the cached substance check state.
      AuState auState = AuUtil.getAuState(au);
      log.trace("auState = {}", auState);

      SubstanceChecker.State oldState = null;

      if (auState != null) {
	oldState = auState.getSubstanceState();
	log.trace("oldState = {}", oldState);
      }

      // Get the actual substance check state.
      SubstanceChecker.State newState = subChecker.findSubstance();
      log.trace("newState = {}", newState);

      String errorMessage = null;

      // Record the result and populate the response.
      if (newState != null) {
	switch (newState) {
	  case Unknown:
	    log.error("Shouldn't happen: SubstanceChecker returned Unknown");
	    errorMessage = UNEXPECTED_SUBSTANCE_CHECKER_ERROR_MESSAGE;
	    break;
	  case Yes:
	    if (auState != null) {
	      auState.setSubstanceState(SubstanceChecker.State.Yes);
	    }

	    break;
	  case No:
	    if (auState != null) {
	      auState.setSubstanceState(SubstanceChecker.State.No);
	    }

	    break;
	}
      } else {
  	log.error("Shouldn't happen: SubstanceChecker returned null");
  	errorMessage = UNEXPECTED_SUBSTANCE_CHECKER_ERROR_MESSAGE;
      }

      CheckSubstanceResult result = new CheckSubstanceResult(auId,
  	convertToWsState(oldState), convertToWsState(newState), errorMessage);
      log.debug2("result = {}", result);

      return new ResponseEntity<CheckSubstanceResult>(result, HttpStatus.OK);
    } catch (Exception e) {
      String message = "Cannot getAuSubstanceCheck() for auId = '" + auId + "'";
      log.error(message, e);
      return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Provides a conversion from a SubstanceChecker.State to the corresponding
   * CheckSubstanceResult.State.
   * 
   * @param state
   *          A SubstanceChecker.State with the state to be converted.
   * @return a CheckSubstanceResult.State with the converted state.
   */
  private CheckSubstanceResult.State convertToWsState(
      SubstanceChecker.State state) {
    if (state != null) {
      switch (state) {
      case Unknown:
	return CheckSubstanceResult.State.Unknown;
      case Yes:
	return CheckSubstanceResult.State.Yes;
      case No:
	return CheckSubstanceResult.State.No;
      }
    }

    return null;
  }
}
