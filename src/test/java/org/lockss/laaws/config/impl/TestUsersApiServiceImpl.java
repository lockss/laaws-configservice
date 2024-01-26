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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.lockss.account.BasicUserAccount;
import org.lockss.account.UserAccount;
import org.lockss.app.LockssDaemon;
import org.lockss.config.RestConfigClient;
import org.lockss.log.L4JLogger;
import org.lockss.spring.test.SpringLockssTestCase4;
import org.lockss.state.StateManager;
import org.lockss.util.ListUtil;
import org.lockss.util.rest.RestUtil;
import org.lockss.util.rest.exception.LockssRestException;
import org.lockss.util.rest.exception.LockssRestHttpException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.http.*;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Tests for {@link UsersApiServiceImpl}.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class TestUsersApiServiceImpl extends SpringLockssTestCase4 {
  private static L4JLogger log = L4JLogger.getLogger();

  private static final String EMPTY_STRING = "";
  private static final String UNKNOWN_USER = "UnknownUser";
  private static final String X_LOCKSS_REQUEST_COOKIE_NAME =
      "X-Lockss-Request-Cookie";

  // Credentials.
  private final Credentials USER_ADMIN =
      new Credentials("lockss-u", "lockss-p");
  private final Credentials ANYBODY =
      new Credentials("someUser", "somePassword");

  // The port that Tomcat is using during this test.
  @LocalServerPort
  private int port;

  // The application Context used to specify the command line arguments to be
  // used for the tests.
  @Autowired
  ApplicationContext appCtx;

  /**
   * Set up code to be run before each test.
   *
   * @throws IOException if there are problems.
   */
  @Before
  public void setUpBeforeEachTest() throws IOException {
    log.debug2("port = {}", port);

    // Set up the temporary directory where the test data will reside.
    setUpTempDirectory(TestAustatesApiServiceImpl.class.getCanonicalName());

    // Set up the UI port.
    setUpUiPort(UI_PORT_CONFIGURATION_TEMPLATE, UI_PORT_CONFIGURATION_FILE);

    log.debug2("Done");
  }

  /**
   * Provides the standard command line arguments to start the server.
   *
   * @return a {@code List<String>} with the command line arguments.
   */
  private List<String> getCommandLineArguments() {
    log.debug2("Invoked");

    List<String> cmdLineArgs = new ArrayList<String>();
    cmdLineArgs.add("-p");
    cmdLineArgs.add(getPlatformDiskSpaceConfigPath());
    cmdLineArgs.add("-p");
    cmdLineArgs.add(getUiPortConfigFile().getAbsolutePath());
    cmdLineArgs.add("-p");
    cmdLineArgs.add("test/config/lockss.txt");
    cmdLineArgs.add("-p");
    cmdLineArgs.add("test/config/lockss.opt");

    log.debug2("cmdLineArgs = {}", cmdLineArgs);
    return cmdLineArgs;
  }

  @Test
  public void runAuthenticatedTests() throws Exception {
    // Specify the command line parameters to be used for the tests.
    List<String> cmdLineArgs = getCommandLineArguments();
    cmdLineArgs.add("-p");
    cmdLineArgs.add("test/config/testAuthOn.txt");

    CommandLineRunner runner = appCtx.getBean(CommandLineRunner.class);
    runner.run(cmdLineArgs.toArray(new String[cmdLineArgs.size()]));

    postUserAccountsAuthenticatedTest();
    getUserAccountAuthenticatedTest();
    patchUserAccountAuthenticatedTest();
    deleteUserAccountAuthenticatedTest();
  }

  @Test
  public void runUnauthenticatedTests() throws Exception {
    // TODO - test all REST calls fail without authentication?
  }

  /** Tests for POST /users REST endpoint. */
  public void postUserAccountsAuthenticatedTest() throws Exception {
    StateManager stateManager =
        LockssDaemon.getLockssDaemon().getManagerByType(StateManager.class);

    UserAccount acct1 = makeUser();
    UserAccount acct2 = makeUser();
    UserAccount acct3 = makeUser();

    assertNull(stateManager.getUserAccount(acct1.getName()));
    assertNull(stateManager.getUserAccount(acct2.getName()));
    assertNull(stateManager.getUserAccount(acct3.getName()));

    runTestPostUserAccounts(null, null, HttpStatus.UNAUTHORIZED);
    runTestPostUserAccounts(null, ANYBODY, HttpStatus.UNAUTHORIZED);
    runTestPostUserAccounts(null, USER_ADMIN, HttpStatus.BAD_REQUEST);

    runTestPostUserAccountsClient(null, null, HttpStatus.UNAUTHORIZED);
    runTestPostUserAccountsClient(null, ANYBODY, HttpStatus.UNAUTHORIZED);
    runTestPostUserAccountsClient(null, USER_ADMIN, HttpStatus.BAD_REQUEST);

    runTestPostUserAccounts(Collections.emptyList(), USER_ADMIN, HttpStatus.OK);
    runTestPostUserAccountsClient(Collections.emptyList(), USER_ADMIN, HttpStatus.OK);
    runTestPostUserAccounts(ListUtil.list(acct1, acct2), USER_ADMIN, HttpStatus.OK);
    runTestPostUserAccountsClient(ListUtil.list(acct3), USER_ADMIN, HttpStatus.OK);

    assertNotNull(stateManager.getUserAccount(acct1.getName()));
    assertNotNull(stateManager.getUserAccount(acct2.getName()));
    assertNotNull(stateManager.getUserAccount(acct3.getName()));

    // TODO: Write more comprehensive tests and asserts
  }

  /** Tests for GET /users/{username} REST endpoint. */
  public void getUserAccountAuthenticatedTest() throws Exception {
    UserAccount acct1 = makeUser();

    // Run tests using a RestTemplate directly
    runTestGetUserAccount(null, null, HttpStatus.UNAUTHORIZED);
    runTestGetUserAccount(null, ANYBODY, HttpStatus.UNAUTHORIZED);
    runTestGetUserAccount(null, USER_ADMIN, HttpStatus.NOT_FOUND);
    runTestGetUserAccount(EMPTY_STRING, USER_ADMIN, HttpStatus.NOT_FOUND);
    runTestGetUserAccount(acct1.getName(), USER_ADMIN, HttpStatus.NOT_FOUND);

    // Run tests using our RestConfigClient
    runTestGetUserAccountClient(null, null, HttpStatus.UNAUTHORIZED);
    runTestGetUserAccountClient(null, ANYBODY, HttpStatus.UNAUTHORIZED);
    runTestGetUserAccountClient(null, USER_ADMIN, HttpStatus.NOT_FOUND);
    runTestGetUserAccountClient(EMPTY_STRING, USER_ADMIN, HttpStatus.NOT_FOUND);
    runTestGetUserAccountClient(acct1.getName(), USER_ADMIN, HttpStatus.NOT_FOUND);

    StateManager stateManager =
        LockssDaemon.getLockssDaemon().getManagerByType(StateManager.class);

    // Add user to the StateManager
    stateManager.storeUserAccount(acct1);

    runTestGetUserAccount(acct1.getName(), USER_ADMIN, HttpStatus.OK);
    runTestGetUserAccountClient(acct1.getName(), USER_ADMIN, HttpStatus.OK);

    // TODO: Write more comprehensive tests and asserts
  }

  /** Tests for PATCH /users/{username} REST endpoint. */
  private void patchUserAccountAuthenticatedTest() throws Exception {
    StateManager stateManager =
        LockssDaemon.getLockssDaemon().getManagerByType(StateManager.class);

    // Add a user to the StateManager that we'll patch
    UserAccount acct1 = makeUser();
    stateManager.storeUserAccount(acct1);
    assertEquals(0, acct1.getLastLogin());

    runTestPatchUserAccount(null, null, null, null, HttpStatus.UNAUTHORIZED);
    runTestPatchUserAccount(null, null, null, ANYBODY, HttpStatus.UNAUTHORIZED);
    runTestPatchUserAccount(null, null, null, USER_ADMIN, HttpStatus.NOT_FOUND);
    runTestPatchUserAccount(EMPTY_STRING, null, null, USER_ADMIN, HttpStatus.NOT_FOUND);
    runTestPatchUserAccount(acct1.getName(), null, null, USER_ADMIN, HttpStatus.BAD_REQUEST);

    runTestPatchUserAccountClient(null, null, null, null, HttpStatus.UNAUTHORIZED);
    runTestPatchUserAccountClient(null, null, null, ANYBODY, HttpStatus.UNAUTHORIZED);
    runTestPatchUserAccountClient(null, null, null, USER_ADMIN, HttpStatus.NOT_FOUND);
    runTestPatchUserAccountClient(EMPTY_STRING, null, null, USER_ADMIN, HttpStatus.NOT_FOUND);
    runTestPatchUserAccountClient(acct1.getName(), null, null, USER_ADMIN, HttpStatus.BAD_REQUEST);

    // Attempt to update a user that doesn't exist
    String json0 = "{\"lastLogin\":\"12345\"}";
    runTestPatchUserAccount(UNKNOWN_USER, json0, null, USER_ADMIN, HttpStatus.NOT_FOUND);

    // Update the user's lastLogin: It would be nice to exercise UserAccount#jsonFromUserAccount() here
    // but we would be updating the same UserAccount object in the same StateManager without additional
    // testing infrastructure.
    String json1 = "{\"lastLogin\":\"12345\"}";
    runTestPatchUserAccount(acct1.getName(), json1, null, USER_ADMIN, HttpStatus.OK);
    assertEquals(12345, acct1.getLastLogin());

    String json2 = "{\"lastLogin\":\"67890\"}";
    runTestPatchUserAccountClient(acct1.getName(), json2, null, USER_ADMIN, HttpStatus.OK);
    assertEquals(67890, acct1.getLastLogin());

    // TODO: Write more comprehensive tests and asserts
  }

  /** Tests for DELETE /users/{username} REST endpoint. */
  public void deleteUserAccountAuthenticatedTest() throws Exception {
    StateManager stateManager =
        LockssDaemon.getLockssDaemon().getManagerByType(StateManager.class);

    // Add users to the StateManager that we'll delete
    UserAccount acct1 = makeUser();
    UserAccount acct2 = makeUser();
    stateManager.storeUserAccount(acct1);
    stateManager.storeUserAccount(acct2);

    // Sanity check
    assertTrue(stateManager.hasUserAccount(acct1.getName()));
    assertTrue(stateManager.hasUserAccount(acct2.getName()));

    runTestDeleteUserAccount(null, null, HttpStatus.UNAUTHORIZED);
    runTestDeleteUserAccount(null, ANYBODY, HttpStatus.UNAUTHORIZED);
    runTestDeleteUserAccount(null, USER_ADMIN, HttpStatus.NOT_FOUND);
    runTestDeleteUserAccount(EMPTY_STRING, USER_ADMIN, HttpStatus.NOT_FOUND);

    runTestDeleteUserAccountClient(null, null, HttpStatus.UNAUTHORIZED);
    runTestDeleteUserAccountClient(null, ANYBODY, HttpStatus.UNAUTHORIZED);
    runTestDeleteUserAccountClient(null, USER_ADMIN, HttpStatus.NOT_FOUND);
    runTestDeleteUserAccountClient(EMPTY_STRING, USER_ADMIN, HttpStatus.NOT_FOUND);

    runTestDeleteUserAccount(acct1.getName(), USER_ADMIN, HttpStatus.OK);
    runTestDeleteUserAccount(acct1.getName(), USER_ADMIN, HttpStatus.OK);
    runTestDeleteUserAccountClient(acct2.getName(), USER_ADMIN, HttpStatus.OK);
    runTestDeleteUserAccountClient(acct2.getName(), USER_ADMIN, HttpStatus.OK);

    assertFalse(stateManager.hasUserAccount(acct1.getName()));
    assertFalse(stateManager.hasUserAccount(acct2.getName()));
  }

  /**
   * Performs a POST {@code /users} REST call directly using a {@link TestRestTemplate}.
   *
   * @param userAccounts A List containing one or more UserAccounts.
   * @param credentials
   *          A Credentials with the request credentials.
   * @param expectedStatus
   *          An HttpStatus with the HTTP status of the result.
   * @return A String containing the REST response.
   */
  private String runTestPostUserAccounts(List<UserAccount> userAccounts,
                                         Credentials credentials,
                                         HttpStatus expectedStatus) throws Exception {

//    log.debug2("userAccounts.size() = {}", userAccounts.size());
    log.debug2("credentials = {}", credentials);
    log.debug2("expectedStatus = {}", expectedStatus);

    // Get the test URL template.
    String template = getTestUrlTemplate("/users");

    // Create the URI of the request to the REST service.
    URI uri = UriComponentsBuilder.fromUriString(template)
        .build().encode().toUri();
    log.trace("uri = {}", uri);

    // Initialize the request to the REST service.
    RestTemplateBuilder templateBuilder = RestUtil.getRestTemplateBuilder(0, 0);

    HttpEntity<String> requestEntity = null;
    HttpHeaders headers = new HttpHeaders();

    headers.setContentType(MediaType.APPLICATION_JSON);

    // Get the individual credentials elements.
    String user = null;
    String password = null;

    if (credentials != null) {
      user = credentials.getUser();
      password = credentials.getPassword();
    }

    // Check whether there are any custom headers to be specified in the request.
    if (user != null || password != null) {
      // Set up the authentication credentials, if necessary.
      if (credentials != null) {
        credentials.setUpBasicAuthentication(headers);
      }

      log.trace("requestHeaders = {}", () -> headers.toSingleValueMap());
    }

    // Create the request entity.
    String requestBody = null;

    if (userAccounts != null) {
      requestBody = UserAccount.getUserAccountObjectWriter()
          .writeValueAsString(userAccounts);
    }

    requestEntity = new HttpEntity<String>(requestBody, headers);

    // Make the request and get the response.
    ResponseEntity<String> response = new TestRestTemplate(templateBuilder)
        .exchange(uri, HttpMethod.POST, requestEntity, String.class);

    // Get the response status.
    HttpStatusCode statusCode = response.getStatusCode();
    HttpStatus status = HttpStatus.valueOf(statusCode.value());
    assertEquals(expectedStatus, status);

    String result = null;

    if (RestUtil.isSuccess(status)) {
      result = response.getBody();
    }

    log.debug2("result = {}", result);
    return result;
  }

  /**
   * Performs a GET operation directly using a {@link TestRestTemplate}.
   *
   * @param username
   *          A String with the name of the user account.
   * @param credentials
   *          A Credentials with the request credentials.
   * @param expectedStatus
   *          An HttpStatus with the HTTP status of the result.
   * @return a {@link Iterable<String>} with the user account names configured
   */
  private String runTestGetUserAccount(String username,
                                       Credentials credentials,
                                       HttpStatus expectedStatus) {
    log.debug2("username = {}", username);
    log.debug2("credentials = {}", credentials);
    log.debug2("expectedStatus = {}", expectedStatus);

    // Get the test URL template.
    String template = getTestUrlTemplate("/users/{username}");

    // Create the URI of the request to the REST service.
    UriComponents uriComponents = UriComponentsBuilder.fromUriString(template)
        .build().expand(Collections.singletonMap("username", username));

    URI uri = UriComponentsBuilder.newInstance().uriComponents(uriComponents)
        .build().encode().toUri();
    log.trace("uri = {}", uri);

    // Initialize the request to the REST service.
    RestTemplateBuilder templateBuilder = RestUtil.getRestTemplateBuilder(0, 0);

    HttpEntity<String> requestEntity = null;

    // Get the individual credentials elements.
    String user = null;
    String password = null;

    if (credentials != null) {
      user = credentials.getUser();
      password = credentials.getPassword();
    }

    // Check whether there are any custom headers to be specified in the
    // request.
    if (user != null || password != null) {

      // Initialize the request headers.
      HttpHeaders headers = new HttpHeaders();

      // Set up the authentication credentials, if necessary.
      if (credentials != null) {
        credentials.setUpBasicAuthentication(headers);
      }

      log.trace("requestHeaders = {}", () -> headers.toSingleValueMap());

      // Create the request entity.
      requestEntity = new HttpEntity<String>(null, headers);
    }

    // Make the request and get the response.
    ResponseEntity<String> response = new TestRestTemplate(templateBuilder).
        exchange(uri, HttpMethod.GET, requestEntity, String.class);

    // Get the response status.
    HttpStatusCode statusCode = response.getStatusCode();
    HttpStatus status = HttpStatus.valueOf(statusCode.value());
    assertEquals(expectedStatus, status);

    String result = null;

    if (RestUtil.isSuccess(status)) {
      result = response.getBody();
    }

    log.debug2("result = {}", result);
    return result;
  }

  /**
   * Performs a PATCH /users/{username} operation using {@link RestConfigClient}.
   *
   * @param username       A String with the name of the user account.
   * @param json           A String with the JSON patch of the user account.
   * @param cookie         A String with the cookie to include in JMS messages.
   * @param credentials    A Credentials with the request credentials.
   * @param expectedStatus An HttpStatus with the HTTP status of the result.
   * @return a String with the stored Archival Unit state.
   */
  private String runTestPatchUserAccount(String username,
                                         String json,
                                         String cookie,
                                         Credentials credentials,
                                         HttpStatus expectedStatus) {

    log.debug2("username = {}", username);
    log.debug2("json = {}", json);
    log.debug2("cookie = {}", cookie);

    log.debug2("credentials = {}", credentials);
    log.debug2("expectedStatus = {}", expectedStatus);

    // Get the test URL template.
    String template = getTestUrlTemplate("/users/{username}");

    // Create the URI of the request to the REST service.
    UriComponents uriComponents = UriComponentsBuilder.fromUriString(template)
        .build().expand(Collections.singletonMap("username", username));

    URI uri = UriComponentsBuilder.newInstance().uriComponents(uriComponents)
        .build().encode().toUri();
    log.trace("uri = {}", uri);

    // Initialize the request to the REST service.
    RestTemplateBuilder templateBuilder = RestUtil.getRestTemplateBuilder(0, 0);

    HttpEntity<String> requestEntity = null;
    HttpHeaders headers = new HttpHeaders();

    headers.setContentType(MediaType.APPLICATION_JSON);

    // Get the individual credentials elements.
    String user = null;
    String password = null;

    if (credentials != null) {
      user = credentials.getUser();
      password = credentials.getPassword();
    }

    // Check whether there are any custom headers to be specified in the request.
    if (user != null || password != null) {
      // Set up the authentication credentials, if necessary.
      if (credentials != null) {
        credentials.setUpBasicAuthentication(headers);
      }

      log.trace("requestHeaders = {}", () -> headers.toSingleValueMap());
    }

    // Set the request cookie, if passed.
    if (cookie != null) {
      headers.set(X_LOCKSS_REQUEST_COOKIE_NAME, cookie);
    }

    // Create the request entity.
    requestEntity = new HttpEntity<String>(json, headers);

    // Make the request and get the response.
    ResponseEntity<String> response = new TestRestTemplate(templateBuilder)
        .exchange(uri, HttpMethod.PATCH, requestEntity, String.class);

    // Get the response status.
    HttpStatusCode statusCode = response.getStatusCode();
    HttpStatus status = HttpStatus.valueOf(statusCode.value());
    assertEquals(expectedStatus, status);

    String result = null;

    if (RestUtil.isSuccess(status)) {
      result = response.getBody();
    }

    log.debug2("result = {}", result);
    return result;
  }

  /**
   * Performs a DELETE /users/{username} operation directly using a {@link TestRestTemplate}.
   *
   * @param username
   *          A String with the name of the user account.
   * @param credentials
   *          A Credentials with the request credentials.
   * @param expectedStatus
   *          An HttpStatus with the HTTP status of the result.
   * @return a {@link Iterable<String>} with the user account names configured
   */
  private String runTestDeleteUserAccount(String username,
                                          Credentials credentials,
                                          HttpStatus expectedStatus) {
    log.debug2("username = {}", username);
    log.debug2("credentials = {}", credentials);
    log.debug2("expectedStatus = {}", expectedStatus);

    // Get the test URL template.
    String template = getTestUrlTemplate("/users/{username}");

    // Create the URI of the request to the REST service.
    UriComponents uriComponents = UriComponentsBuilder.fromUriString(template)
        .build().expand(Collections.singletonMap("username", username));

    URI uri = UriComponentsBuilder.newInstance().uriComponents(uriComponents)
        .build().encode().toUri();
    log.trace("uri = {}", uri);

    // Initialize the request to the REST service.
    RestTemplateBuilder templateBuilder = RestUtil.getRestTemplateBuilder(0, 0);

    HttpEntity<String> requestEntity = null;

    // Get the individual credentials elements.
    String user = null;
    String password = null;

    if (credentials != null) {
      user = credentials.getUser();
      password = credentials.getPassword();
    }

    // Check whether there are any custom headers to be specified in the
    // request.
    if (user != null || password != null) {

      // Initialize the request headers.
      HttpHeaders headers = new HttpHeaders();

      // Set up the authentication credentials, if necessary.
      if (credentials != null) {
        credentials.setUpBasicAuthentication(headers);
      }

      log.trace("requestHeaders = {}", () -> headers.toSingleValueMap());

      // Create the request entity.
      requestEntity = new HttpEntity<String>(null, headers);
    }

    // Make the request and get the response.
    ResponseEntity<String> response = new TestRestTemplate(templateBuilder).
        exchange(uri, HttpMethod.DELETE, requestEntity, String.class);

    // Get the response status.
    HttpStatusCode statusCode = response.getStatusCode();
    HttpStatus status = HttpStatus.valueOf(statusCode.value());
    assertEquals(expectedStatus, status);

    String result = null;

    if (RestUtil.isSuccess(status)) {
      result = response.getBody();
    }

    log.debug2("result = {}", result);
    return result;
  }

  /**
   * Performs a POST /users operation using {@link RestConfigClient}.
   *
   * @param userAccounts   User accounts to add through the REST client.
   * @param credentials    A Credentials with the request credentials.
   * @param expectedStatus An HttpStatus with the HTTP status of the result.
   * @return a String with the stored Archival Unit state.
   */
  private UserAccount runTestPostUserAccountsClient(List<UserAccount> userAccounts,
                                                    Credentials credentials,
                                                    HttpStatus expectedStatus) {
    log.debug2("credentials = {}", credentials);
    log.debug2("expectedStatus = {}", expectedStatus);

    UserAccount result = null;

    try {
      // Make the request and get the result.
      getRestConfigClient(credentials).postUserAccounts(userAccounts);
      log.debug2("result = {}", result);

      if (!RestUtil.isSuccess(expectedStatus)) {
        fail("Should have thrown LockssRestHttpException");
      }
    } catch (LockssRestHttpException lrhe) {
      assertEquals(expectedStatus.value(), lrhe.getHttpStatusCode());
      assertEquals(expectedStatus.getReasonPhrase(),
          lrhe.getHttpStatusMessage());
    } catch (LockssRestException lre) {
      fail("Should have thrown LockssRestHttpException");
    }

    return result;
  }

  /**
   * Performs a GET /users/{username} operation using {@link RestConfigClient}.
   *
   * @param username       A String with the name of the user account.
   * @param credentials    A Credentials with the request credentials.
   * @param expectedStatus An HttpStatus with the HTTP status of the result.
   * @return a String with the stored Archival Unit state.
   */
  private UserAccount runTestGetUserAccountClient(String username,
                                                  Credentials credentials,
                                                  HttpStatus expectedStatus) {
    log.debug2("username = {}", username);
    log.debug2("credentials = {}", credentials);
    log.debug2("expectedStatus = {}", expectedStatus);

    UserAccount result = null;

    try {
      // Make the request and get the result.
      result = getRestConfigClient(credentials).getUserAccount(username);
      log.debug2("result = {}", result);

      if (!RestUtil.isSuccess(expectedStatus)) {
        fail("Should have thrown LockssRestHttpException");
      }
    } catch (LockssRestHttpException lrhe) {
      assertEquals(expectedStatus.value(), lrhe.getHttpStatusCode());
      assertEquals(expectedStatus.getReasonPhrase(),
          lrhe.getHttpStatusMessage());
    } catch (IOException e) {
      fail("Unexpected IOException", e);
    }

    return result;
  }

  /**
   * Performs a PATCH /users/{username} operation using {@link RestConfigClient}.
   *
   * @param username       A String with the name of the user account.
   * @param json           A String with the JSON patch of the user account.
   * @param cookie         A String with the cookie to include in JMS messages.
   * @param credentials    A Credentials with the request credentials.
   * @param expectedStatus An HttpStatus with the HTTP status of the result.
   * @return a String with the stored Archival Unit state.
   */
  private UserAccount runTestPatchUserAccountClient(String username,
                                                    String json,
                                                    String cookie,
                                                    Credentials credentials,
                                                    HttpStatus expectedStatus) {
    log.debug2("username = {}", username);
    log.debug2("credentials = {}", credentials);
    log.debug2("expectedStatus = {}", expectedStatus);

    UserAccount result = null;

    try {
      // Make the request and get the result.
      result = getRestConfigClient(credentials).patchUserAccount(username, json, cookie);
      log.debug2("result = {}", result);

      if (!RestUtil.isSuccess(expectedStatus)) {
        fail("Should have thrown LockssRestHttpException");
      }
    } catch (LockssRestHttpException lrhe) {
      assertEquals(expectedStatus.value(), lrhe.getHttpStatusCode());
      assertEquals(expectedStatus.getReasonPhrase(),
          lrhe.getHttpStatusMessage());
    } catch (IOException e) {
      fail("Unexpected IOException", e);
    }

    return result;
  }

  /**
   * Performs a DELETE /users/{username} operation using {@link RestConfigClient}.
   *
   * @param username       A String with the name of the user account.
   * @param credentials    A Credentials with the request credentials.
   * @param expectedStatus An HttpStatus with the HTTP status of the result.
   * @return a String with the stored Archival Unit state.
   */
  private void runTestDeleteUserAccountClient(String username,
                                                     Credentials credentials,
                                                     HttpStatus expectedStatus) {
    log.debug2("username = {}", username);
    log.debug2("credentials = {}", credentials);
    log.debug2("expectedStatus = {}", expectedStatus);

    try {
      // Make the request and get the result.
      getRestConfigClient(credentials).deleteUserAccount(username);

      if (!RestUtil.isSuccess(expectedStatus)) {
        fail("Should have thrown LockssRestHttpException");
      }
    } catch (LockssRestHttpException lrhe) {
      assertEquals(expectedStatus.value(), lrhe.getHttpStatusCode());
      assertEquals(expectedStatus.getReasonPhrase(),
          lrhe.getHttpStatusMessage());
    } catch (IOException e) {
      fail("Unexpected IOException", e);
    }
  }

  private UserAccount makeUser() throws Exception {
    UserAccount acct = makeUser(UUID.randomUUID().toString());
    acct.setPassword(UUID.randomUUID().toString());
    return acct;
  }

  private UserAccount makeUser(String name) {
    UserAccount acct = new BasicUserAccount.Factory().newUser(name, null);
    return acct;
  }

  /**
   * Provides the URL template to be tested.
   *
   * @param pathAndQueryParams
   *          A String with the path and query parameters of the URL template to
   *          be tested.
   * @return a String with the URL template to be tested.
   */
  private String getTestUrlTemplate(String pathAndQueryParams) {
    return "http://localhost:" + port + pathAndQueryParams;
  }

  /**
   * Provides the REST Configuration service client to be tested.
   *
   * @param credentials
   *          A Credentials with the request credentials.
   * @return a RestConfigClient with the REST Configuration service client.
   */
  private RestConfigClient getRestConfigClient(Credentials credentials) {
    // Check whether there are any credentials to be specified in the request.
    if (credentials != null) {
      // Yes.
      return new RestConfigClient("http://" + credentials.getUser() + ":"
          + credentials.getPassword() + "@localhost:" + port);
    }

    // No.
    return new RestConfigClient("http://localhost:" + port);
  }
}
