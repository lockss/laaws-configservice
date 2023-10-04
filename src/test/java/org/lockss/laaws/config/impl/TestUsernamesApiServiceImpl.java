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
import org.lockss.util.rest.RestUtil;
import org.lockss.util.rest.exception.LockssRestException;
import org.lockss.util.rest.exception.LockssRestHttpException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.ApplicationContext;
import org.springframework.http.*;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class TestUsernamesApiServiceImpl extends SpringLockssTestCase4 {
  private static L4JLogger log = L4JLogger.getLogger();

  // Credentials.
  private final Credentials USER_ADMIN =
      new Credentials("lockss-u", "lockss-p");
  private final Credentials AU_ADMIN =
      new Credentials("au-admin", "I'mAuAdmin");
  private final Credentials CONTENT_ADMIN =
      new Credentials("content-admin", "I'mContentAdmin");
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

  /**
   * Runs the tests with authentication turned off.
   *
   * @throws Exception
   *           if there are problems.
   */
  @Test
  public void runUnauthenticatedTests() throws Exception {
    // Specify the command line parameters to be used for the tests.
    List<String> cmdLineArgs = getCommandLineArguments();
    cmdLineArgs.add("-p");
    cmdLineArgs.add("test/config/testAuthOff.txt");

    CommandLineRunner runner = appCtx.getBean(CommandLineRunner.class);
    runner.run(cmdLineArgs.toArray(new String[cmdLineArgs.size()]));

    getUserAccountNamesUnauthenticatedTest();
  }

  public void getUserAccountNamesUnauthenticatedTest() throws Exception {
    runTestGetUserAccountNames(null, HttpStatus.UNAUTHORIZED);
    runTestGetUserAccountNames(ANYBODY, HttpStatus.UNAUTHORIZED);
    runTestGetUserAccountNamesClient(null, HttpStatus.UNAUTHORIZED);
    runTestGetUserAccountNamesClient(ANYBODY, HttpStatus.UNAUTHORIZED);

    getUserAccountNamesCommonTest();
  }

  @Test
  public void runAuthenticatedTests() throws Exception {
    // Specify the command line parameters to be used for the tests.
    List<String> cmdLineArgs = getCommandLineArguments();
    cmdLineArgs.add("-p");
    cmdLineArgs.add("test/config/testAuthOn.txt");

    CommandLineRunner runner = appCtx.getBean(CommandLineRunner.class);
    runner.run(cmdLineArgs.toArray(new String[cmdLineArgs.size()]));

    getUserAccountNamesAuthenticatedTest();
  }

  public void getUserAccountNamesAuthenticatedTest() throws Exception {
    runTestGetUserAccountNames(null, HttpStatus.UNAUTHORIZED);
    runTestGetUserAccountNames(ANYBODY, HttpStatus.UNAUTHORIZED);
    runTestGetUserAccountNamesClient(null, HttpStatus.UNAUTHORIZED);
    runTestGetUserAccountNamesClient(ANYBODY, HttpStatus.UNAUTHORIZED);

    getUserAccountNamesCommonTest();
  }

  /**
   * Runs the getUserAccountNames()-related authentication-independent tests.
   */
  private void getUserAccountNamesCommonTest() throws Exception {
    ObjectMapper objMapper = new ObjectMapper();

    StateManager stateManager =
        LockssDaemon.getLockssDaemon().getManagerByType(StateManager.class);

    // Assert results with an empty StateManager
    assertEquals(
        objMapper.writeValueAsString(stateManager.getUserAccountNames()),
        runTestGetUserAccountNames(USER_ADMIN, HttpStatus.OK));

    assertIterableEquals(
        stateManager.getUserAccountNames(),
        runTestGetUserAccountNamesClient(USER_ADMIN, HttpStatus.OK));

    // Add a user to the StateManager
    UserAccount acct1 = makeUser("User1");
    acct1.setPassword("ok12345");
    stateManager.storeUserAccount(acct1);

    // Assert results with a populated StateManager
    assertEquals(
        objMapper.writeValueAsString(stateManager.getUserAccountNames()),
        runTestGetUserAccountNames(USER_ADMIN, HttpStatus.OK));

    assertIterableEquals(
        stateManager.getUserAccountNames(),
        runTestGetUserAccountNamesClient(USER_ADMIN, HttpStatus.OK));
  }

  UserAccount makeUser(String name) {
    UserAccount acct = new BasicUserAccount.Factory().newUser(name, null);
    return acct;
  }

  /**
   * Performs a GET operation directly using a {@link TestRestTemplate}.
   *
   * @param credentials
   *          A Credentials with the request credentials.
   * @param expectedStatus
   *          An HttpStatus with the HTTP status of the result.
   * @return a {@link Iterable<String>} with the user account names configured
   */
  private String runTestGetUserAccountNames(Credentials credentials,
                                            HttpStatus expectedStatus) {
    log.debug2("credentials = {}", credentials);
    log.debug2("expectedStatus = {}", expectedStatus);

    // Get the test URL template.
    String template = getTestUrlTemplate("/usernames");

    // Create the URI of the request to the REST service.
    URI uri = UriComponentsBuilder.fromUriString(template)
        .build().encode().toUri();
    log.trace("uri = {}", uri);

    // Initialize the request to the REST service.
    RestTemplate restTemplate = RestUtil.getRestTemplate();

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
    ResponseEntity<String> response = new TestRestTemplate(restTemplate).
        exchange(uri, HttpMethod.GET, requestEntity, String.class);

    // Get the response status.
    HttpStatus statusCode = response.getStatusCode();
    assertEquals(expectedStatus, statusCode);

    String result = null;

    if (RestUtil.isSuccess(statusCode)) {
      result = response.getBody();
    }

    log.debug2("result = {}", result);
    return result;
  }

  /**
   * Performs a GET operation using {@link RestConfigClient}.
   *
   * @param credentials    A Credentials with the request credentials.
   * @param expectedStatus An HttpStatus with the HTTP status of the result.
   * @return a String with the stored Archival Unit state.
   */
  private Iterable<String> runTestGetUserAccountNamesClient(Credentials credentials,
                                                            HttpStatus expectedStatus) {
    log.debug2("credentials = {}", credentials);
    log.debug2("expectedStatus = {}", expectedStatus);

    Iterable<String> result = null;

    try {
      // Make the request and get the result.
      result = getRestConfigClient(credentials).getUserAccountNames();
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
