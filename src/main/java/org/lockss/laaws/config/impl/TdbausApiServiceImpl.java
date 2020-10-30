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

import java.util.List;
import org.josql.Query;
import org.josql.QueryExecutionException;
import org.josql.QueryResults;
import org.lockss.laaws.config.api.TdbausApiDelegate;
import org.lockss.log.L4JLogger;
import org.lockss.spring.base.BaseSpringApiServiceImpl;
import org.lockss.util.josql.JosqlUtil;
import org.lockss.ws.entities.TdbAuWsResult;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

/**
 * Service for accessing the TDB AUs.
 */
@Service
public class TdbausApiServiceImpl extends BaseSpringApiServiceImpl
implements TdbausApiDelegate {
  private static L4JLogger log = L4JLogger.getLogger();

  /**
   * Provides the selected properties of selected title database archival units.
   * 
   * @param tdbAuQuery A String with the
   *                   <a href="package-summary.html#SQL-Like_Query">SQL-like
   *                   query</a> used to specify what properties to retrieve
   *                   from which title database archival units.
   * @return a {@code ResponseEntity<List<TdbAuWsResult>>} with the results.
   */
  @Override
  public ResponseEntity getTdbAus(String tdbAuQuery) {
    log.debug2("tdbAuQuery = {}", tdbAuQuery);

    // Check whether the service has not been fully initialized.
    if (!waitReady()) {
      // Yes: Notify the client.
      return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
    }

    TdbAuHelper tdbAuHelper = new TdbAuHelper();
    List<TdbAuWsResult> results = null;

    try {
      // Create the full query.
      String fullQuery = JosqlUtil.createFullQuery(tdbAuQuery,
	  TdbAuHelper.SOURCE_FQCN, TdbAuHelper.PROPERTY_NAMES,
	  TdbAuHelper.RESULT_FQCN);
      log.trace("fullQuery = {}", fullQuery);

      // Create a new JoSQL query.
      Query q = new Query();

      try {
	// Parse the SQL-like query.
	q.parse(fullQuery);

	// Execute the query.
	QueryResults qr = q.execute(tdbAuHelper.createUniverse());

	// Get the query results.
	results = (List<TdbAuWsResult>)qr.getResults();
	log.trace("results.size() = {}", results.size());
	log.trace("results = {}", tdbAuHelper.nonDefaultToString(results));
	return new ResponseEntity<List<TdbAuWsResult>>(results, HttpStatus.OK);
      } catch (QueryExecutionException qee) {
	String message =
	    "Cannot getTdbAus() for tdbAuQuery = '" + tdbAuQuery + "'";
	log.error(message, qee);
	return new ResponseEntity<String>(message,
	    HttpStatus.INTERNAL_SERVER_ERROR);
      }
    } catch (Exception e) {
      String message =
	  "Cannot getTdbAus() for tdbAuQuery = '" + tdbAuQuery + "'";
      log.error(message, e);
      return new ResponseEntity<String>(message,
	  HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }
}
