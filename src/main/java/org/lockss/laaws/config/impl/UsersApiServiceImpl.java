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
import org.lockss.laaws.config.TypedUserAccount;
import org.lockss.log.L4JLogger;
import org.lockss.spring.base.BaseSpringApiServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
  public ResponseEntity<List<TypedUserAccount>> addUserAccounts(List<TypedUserAccount> userAccounts) {
    try {
      for (TypedUserAccount tua : userAccounts) {
        Class<?> cls = Class.forName(tua.getUserAccountType());

        if (!cls.isInstance(UserAccount.class)) {
          log.error("User account type must be a subclass of UserAccount");
          return ResponseEntity.badRequest().build();
        }

        UserAccount acct = (UserAccount) objMapper.readValue(tua.getSerializedData(),
            // FIXME: This seems dangerous; refactor to use a whitelist of classes
            Class.forName(tua.getUserAccountType()));

        getAccountManager().addUser(acct);
      }
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
  public ResponseEntity<List<TypedUserAccount>> getUserAccounts() {
    Collection<UserAccount> userAccounts = getAccountManager().getUsers();
    List<TypedUserAccount> tuas = new ArrayList<>(userAccounts.size());

    try {
      for (UserAccount acct : userAccounts) {
        TypedUserAccount tua = new TypedUserAccount();
        tua.setUserAccountType(acct.getType());
        tua.setSerializedData(objMapper.writeValueAsString(tua));
        tuas.add(tua);
      }
    } catch (JsonProcessingException e) {
      log.error("Could not serialize user account", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }

    return ResponseEntity.ok(tuas);
  }

  @Override
  public ResponseEntity<TypedUserAccount> getUserAccount(String username) {
    UserAccount acct = getAccountManager().getUserOrNull(username);

    if (acct == null) {
      log.warn("User not found");
      return ResponseEntity.notFound().build();
    }

    try {
      TypedUserAccount tua = new TypedUserAccount();
      tua.setUserAccountType(acct.getType());
      tua.setSerializedData(objMapper.writeValueAsString(tua));
      return ResponseEntity.ok(tua);
    } catch (JsonProcessingException e) {
      log.error("Could not serialize user account", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
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
  public ResponseEntity<Void> updateUserAccount(String username, TypedUserAccount tua) {
    // TODO
    return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
  }
}