/*

Copyright (c) 2000-2023, Board of Trustees of Leland Stanford Jr. University

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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections4.IterableUtils;
import org.lockss.app.LockssDaemon;
import org.lockss.laaws.config.api.UsernamesApiDelegate;
import org.lockss.log.L4JLogger;
import org.lockss.spring.base.BaseSpringApiServiceImpl;
import org.lockss.state.StateManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
public class UsernamesApiServiceImpl extends BaseSpringApiServiceImpl
    implements UsernamesApiDelegate {
  private static L4JLogger log = L4JLogger.getLogger();

  @Autowired
  private ObjectMapper objMapper;

  protected StateManager getStateManager() {
    return LockssDaemon.getLockssDaemon()
        .getManagerByType(StateManager.class);
  }

  @Override
  public ResponseEntity<List<String>> getUserAccountNames() {
    try {
      List<String> usernames =
          IterableUtils.toList(getStateManager().getUserAccountNames());
      return ResponseEntity.ok(usernames);
    } catch (IOException e) {
      log.error("Could not get user account names", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }
}
