/*

 Copyright (c) 2017 Board of Trustees of Leland Stanford Jr. University,
 all rights reserved.

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in
 all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL
 STANFORD UNIVERSITY BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

 Except as contained in this notice, the name of Stanford University shall not
 be used in advertising or otherwise to promote the sale, use or other dealings
 in this Software without prior written authorization from Stanford University.

 */
package org.lockss.laaws.config.api.impl;

import java.util.Properties;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import org.apache.log4j.Logger;
import org.lockss.config.TdbAu;
import org.lockss.config.TdbUtil;
import org.lockss.laaws.config.api.ApiException;
import org.lockss.laaws.config.api.AusApiService;

/**
 * Implementation of the base provider of access to Archival Unit
 * configurations.
 */
public class AusApiServiceImpl extends AusApiService {
  private static Logger log = Logger.getLogger(AusApiServiceImpl.class);

  /**
   * Provides the title database of an AU given the AU identifier.
   * 
   * @param auid
   *          A String with the AU identifier.
   * @param securityContext
   *          A SecurityContext providing access to security related
   *          information.
   * @return a Response with any data that needs to be returned to the runtime.
   * @throws ApiException
   *           if there are problems.
   */
  @Override
  public Response getTdbAu(String auid, SecurityContext securityContext)
      throws ApiException {
    if (log.isDebugEnabled()) log.debug("auid = " + auid);

    try {
      TdbAu tdbAu = TdbUtil.getTdbAu(auid);

      if (tdbAu != null) {
	tdbAu.prettyLog(2);
	Properties properties = tdbAu.toProperties();
	if (log.isDebugEnabled()) log.debug("properties = " + properties);
	return Response.ok().entity(properties).build();
      } else {
	String message = "No Archival Unit found for auid = '" + auid + "'";
	log.error(message);
	return Response.status(404).entity(message).type("text/plain").build();
      }
    } catch (Exception e) {
      String message = "Cannot getTdbAu()";
      log.error(message, e);
      throw new ApiException(1, message + ": " + e.getMessage());
    }
  }
}
