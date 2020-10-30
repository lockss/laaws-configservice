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

import org.lockss.laaws.config.api.AustatusesApiDelegate;
import org.lockss.log.L4JLogger;
import org.lockss.spring.base.BaseSpringApiServiceImpl;
import org.lockss.util.StringUtil;
import org.lockss.ws.entities.AuStatus;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

/**
 * Service for accessing the status of archival units.
 */
@Service
public class AustatusesApiServiceImpl extends BaseSpringApiServiceImpl
implements AustatusesApiDelegate {
  private static L4JLogger log = L4JLogger.getLogger();

  /**
   * Provides the status information of an archival unit in the system.
   * 
   * @param auId A String with the identifier of the archival unit.
   * @return a {@code ResponseEntity<AuStatus>} AuStatus with the status
   *         information of the archival unit.
   */
  @Override
  public ResponseEntity getAuStatus(String auId) {
    log.debug2("auId = {}", auId);

    // Check whether the service has not been fully initialized.
    if (!waitReady()) {
      // Yes: Notify the client.
      return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
    }

    // Input validation.
    if (StringUtil.isNullString(auId)) {
      String message = "Cannot getAuStatus() for auId = '" + auId
	  + "': Invalid Archival Unit identifier.";
      log.error(message);
      return new ResponseEntity<String>(message, HttpStatus.BAD_REQUEST);
    }

    try {
      // Get the status.
      AuStatus result = new AuHelper().getAuStatus(auId);
      log.debug2("result = " + result);
      return new ResponseEntity<AuStatus>(result, HttpStatus.OK);
    } catch (Exception e) {
      String message = "Cannot getAuStatus() for auId = '" + auId + "'";
      log.error(message, e);
      return new ResponseEntity<String>(message,
	  HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }
}
