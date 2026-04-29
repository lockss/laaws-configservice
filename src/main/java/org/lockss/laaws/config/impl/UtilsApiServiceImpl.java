/*

Copyright (c) 2000-2025, Board of Trustees of Leland Stanford Jr. University

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

 */

package org.lockss.laaws.config.impl;

import org.lockss.app.LockssDaemon;
import org.lockss.laaws.config.api.UtilsApiDelegate;
import org.lockss.log.L4JLogger;
import org.lockss.plugin.PluginManager;
import org.lockss.spring.base.BaseSpringApiServiceImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class UtilsApiServiceImpl extends BaseSpringApiServiceImpl
    implements UtilsApiDelegate {

  private static L4JLogger log = L4JLogger.getLogger();

  @Override
  public ResponseEntity<List<String>> normalizeUrl(String url) {
    LockssDaemon theDaemon = LockssDaemon.getLockssDaemon();
    PluginManager pluginMgr = theDaemon.getPluginManager();

    if (!pluginMgr.areAusStarted()) {
      String msg = "AUs still starting";
      return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
    }

    try {
      Set<String> normalizedUrls = pluginMgr.normalizeUrl(url);
      return new ResponseEntity<>(new ArrayList<>(normalizedUrls), HttpStatus.OK);
    } catch (MalformedURLException e) {
      String msg = "Malformed URL: " + url;
      return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }
  }
}
