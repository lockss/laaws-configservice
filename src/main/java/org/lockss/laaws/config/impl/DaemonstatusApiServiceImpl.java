/*

Copyright (c) 2000-2025 Board of Trustees of Leland Stanford Jr. University,
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

import org.lockss.app.LockssApp;
import org.lockss.daemon.status.StatusService;
import org.lockss.daemon.status.StatusTable;
import org.lockss.laaws.config.api.DaemonstatusApiDelegate;
import org.lockss.log.L4JLogger;
import org.lockss.spring.base.BaseSpringApiServiceImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.BitSet;

@Service
public class DaemonstatusApiServiceImpl extends BaseSpringApiServiceImpl
    implements DaemonstatusApiDelegate {

  private static L4JLogger log = L4JLogger.getLogger();

  public StatusTable getTable(String tableName, String key, BitSet options)
      throws StatusService.NoSuchTableException {
    if (tableName == null) {
      throw new
          StatusService.NoSuchTableException("Called with null tableName");
    }

    StatusTable table = new StatusTable(tableName, key);
    if (options != null) {
      table.setOptions(options);
    }
    StatusService statusServ = LockssApp.getLockssApp().getStatusService();
    statusServ.fillInTable(table);
    return table;
  }

  @Override
  public ResponseEntity<Object> getStatusTable(String tableId, String key) {
    try {
      StatusTable st = getTable(tableId, key, null);
      return new ResponseEntity<>(st, HttpStatus.OK);
    } catch (StatusService.NoSuchTableException e) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
  }
}
