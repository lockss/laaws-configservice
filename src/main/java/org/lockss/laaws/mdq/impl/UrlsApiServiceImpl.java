/*

Copyright (c) 2000-2018 Board of Trustees of Leland Stanford Jr. University,
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
package org.lockss.laaws.mdq.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.lockss.app.LockssDaemon;
import org.lockss.daemon.OpenUrlResolver;
import org.lockss.daemon.OpenUrlResolver.OpenUrlInfo;
import org.lockss.laaws.mdq.api.UrlsApiDelegate;
import org.lockss.laaws.mdq.model.UrlInfo;
import org.lockss.log.L4JLogger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

/**
 * Service for access to URLs.
 */
@Service
public class UrlsApiServiceImpl implements UrlsApiDelegate {
  private static final L4JLogger log = L4JLogger.getLogger();

  /**
   * Provides the URL for a DOI given the DOI.
   * 
   * @param doi
   *          A String with the DOI for which the URL is requested.
   * @return a {@code ResponseEntity<UrlInfo>} with the URL information.
   */
  @Override
  public ResponseEntity<UrlInfo> getUrlsDoi(String doi) {
    log.debug2("doi = {}", () -> doi);

    try {
      // Build an OpenURL query.
      Map<String, String> params = new HashMap<String,String>();
      params.put("rft_id", "info:doi/" + doi);

      return new ResponseEntity<UrlInfo>(resolveOpenUrl(params), HttpStatus.OK);
    } catch (Exception e) {
      String message = "Cannot getUrlsDoi() for doi = '" + doi + "'";
      log.error(message, e);
      return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Provides the URL that results from performing an OpenURL query
   * 
   * @param params
   *          A {@code List<String>} with the OpenURL query parameters.
   * @return a {@code ResponseEntity<UrlInfo>} with the URL information.
   */
  @Override
  public ResponseEntity<UrlInfo> getUrlsOpenUrl(List<String> params) {
    log.debug2("params = {}", () -> params);

    try {
      // Build the OpenURL query.
      Map<String, String> openUrlParams = new HashMap<String,String>();

      for (String param : params) {
	int sepLoc = param.trim().indexOf("=");

	if (sepLoc > 0 && sepLoc < param.length() - 1) {
	  openUrlParams.put(param.substring(0, sepLoc),
	      param.substring(sepLoc + 1));
	}
      }

      log.trace("openUrlParams = {}", () -> openUrlParams);

      return new ResponseEntity<UrlInfo>(resolveOpenUrl(openUrlParams),
	  HttpStatus.OK);
    } catch (Exception e) {
      String message = "Cannot getUrlsOpenUrl() for params = '" + params + "'";
      log.error(message, e);
      return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Provides the results of an OpenUrl query.
   * 
   * @param params
   *          A {@code Map<String, String>} with the OpenURL query parameters.
   * @return a UrlInfo with the results.
   */
  private UrlInfo resolveOpenUrl(Map<String, String> params) {
    log.debug2("params = {}", () -> params);

    // The unique URLs that result from performing the query.
    Set<String> urls = new HashSet<String>();

    // Make the query.
    OpenUrlInfo openUrlInfo =
	new OpenUrlResolver(LockssDaemon.getLockssDaemon())
	.resolveOpenUrl(params);
    log.trace("openUrlInfo = {}", () -> openUrlInfo);

    // Loop through all the results.
    Iterator<OpenUrlInfo> iterator = openUrlInfo.iterator();

    while (iterator.hasNext()) {
      OpenUrlInfo next = iterator.next();
      log.trace("next = {}", () -> next);

      String url = next.getResolvedUrl();
      log.trace("url = {}", () -> url);

      if (url != null && !"null".equalsIgnoreCase(url)) {
	// Accumulate the resulting unique URLs.
	urls.add(url);
      }
    }

    log.trace("urls = {}", () -> urls);

    UrlInfo result = new UrlInfo();
    result.setParams(params);
    result.setUrls(new ArrayList<String>(urls));
    log.debug2("result = {}", () -> result);
    return result;
  }
}
