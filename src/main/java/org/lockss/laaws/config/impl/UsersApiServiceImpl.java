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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.lockss.account.UserAccount;
import org.lockss.laaws.config.api.UsersApiDelegate;
import org.lockss.log.L4JLogger;
import org.lockss.plugin.AuUtil;
import org.lockss.spring.base.BaseSpringApiServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class UsersApiServiceImpl extends BaseSpringApiServiceImpl
    implements UsersApiDelegate {
  private static L4JLogger log = L4JLogger.getLogger();

  @Autowired
  private ObjectMapper objMapper;

  @Override
  public ResponseEntity<String> addUserAccounts(String userAccountsJson) {
    try {
      UserAccount[] userAccounts = objMapper
          .readerFor(UserAccount[].class)
          .readValue(userAccountsJson);

      if (userAccounts == null) {
        log.error("User accounts missing from request");
        return ResponseEntity.badRequest().build();
      }

      List<UserAccount> successfullyAdded = new ArrayList<>();

      for (UserAccount acct : userAccounts) {
        try {
          getStateManager().storeUserAccount(acct);
          successfullyAdded.add(acct);
        } catch (IOException e) {
          log.error("Could not add user account: {}", acct);
        }
      }

      return ResponseEntity.ok(objMapper.writeValueAsString(successfullyAdded));
    } catch (JsonProcessingException e) {
      log.error("Error deserializing user account", e);
      return ResponseEntity.badRequest().build();
    }
  }

  @Override
  public ResponseEntity<String> getUserAccount(String username) {
    UserAccount acct = getStateManager().getUserAccount(username);

    if (acct == null) {
      log.warn("User not found");
      return ResponseEntity.notFound().build();
    }

    try {
      return ResponseEntity.ok(objMapper.writeValueAsString(acct));
    } catch (JsonProcessingException e) {
      log.error("Could not serialize user account", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  @Override
  public ResponseEntity<Void> removeUserAccount(String username) {
    UserAccount acct = getStateManager().getUserAccount(username);
    getStateManager().removeUserAccount(acct);
    return ResponseEntity.ok().build();
  }

  @Override
  public ResponseEntity<String> updateUserAccount(String username, String userAccountUpdates, String cookie) {
    try {
      UserAccount result = getStateManager().updateUserAccountFromJson(username, userAccountUpdates, cookie);
      return ResponseEntity.ok(objMapper.writeValueAsString(result));
    } catch (JsonProcessingException e) {
      log.error("Could not serialize user account", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    } catch (IOException e) {
      log.error("Could not update user account", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }
}
