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
import com.fasterxml.jackson.databind.ObjectMapper;
import org.lockss.account.AccountManager;
import org.lockss.account.UserAccount;
import org.lockss.app.LockssDaemon;
import org.lockss.laaws.config.api.UsersApiDelegate;
import org.lockss.laaws.config.model.SerializedUserAccount;
import org.lockss.log.L4JLogger;
import org.lockss.spring.base.BaseSpringApiServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UsersApiServiceImpl extends BaseSpringApiServiceImpl
      implements UsersApiDelegate {
  private static L4JLogger log = L4JLogger.getLogger();

  @Autowired
  private ObjectMapper objMapper;

  protected AccountManager getAccountManager() {
    return LockssDaemon.getLockssDaemon().getAccountManager();
  }

  @Override
  public ResponseEntity<Void> addUserAccount(SerializedUserAccount userAccount) {
    try {
      Class<?> cls = Class.forName(userAccount.getUserAccountType());

      if (!cls.isInstance(UserAccount.class)) {
        log.error("User account type must be a subclass of UserAccount");
        return ResponseEntity.badRequest().build();
      }

      UserAccount acct = (UserAccount) objMapper.readValue(userAccount.getSerializedData(),
          // FIXME: This seems dangerous; refactor to use a whitelist of classes
          Class.forName(userAccount.getUserAccountType()));

      getAccountManager().addUser(acct);
      return ResponseEntity.ok().build();
    } catch (ClassNotFoundException e) {
      log.error("Unknown user account type", e);
      return ResponseEntity.badRequest().build();
    } catch (JsonProcessingException e) {
      log.error("Error deserializing user account", e);
      return ResponseEntity.badRequest().build();
    } catch (AccountManager.NotStoredException | AccountManager.NotAddedException e) {
      log.error("Could not store user account", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  @Override
  public ResponseEntity<SerializedUserAccount> getUserAccount(String username) {
    UserAccount acct = getAccountManager().getUserOrNull(username);

    if (acct == null) {
      log.warn("User not found");
      return ResponseEntity.notFound().build();
    }

    try {
      SerializedUserAccount sua =  new SerializedUserAccount();
      sua.setUserAccountType(acct.getType());
      sua.setSerializedData(objMapper.writeValueAsString(acct));
      return ResponseEntity.ok(sua);
    } catch (JsonProcessingException e) {
      log.error("Could not serialize user account", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  @Override
  public ResponseEntity<List<String>> getUserAccountNames() {
    List<String> userNames = getAccountManager().getUsers()
        .stream()
        .map(UserAccount::getName)
        .collect(Collectors.toList());

    return ResponseEntity.ok(userNames);
  }

  @Override
  public ResponseEntity<Void> removeUserAccount(String username) {
    if (!getAccountManager().deleteUser(username)) {
      log.warn("User not removed");
      // Q: Something else?
    }

    return ResponseEntity.ok().build();
  }

  @Override
  public ResponseEntity<Void> updateUserAccount(String username, SerializedUserAccount userAccount) {
    return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
  }
}
