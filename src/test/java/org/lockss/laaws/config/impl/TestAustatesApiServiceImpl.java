/*

Copyright (c) 2000-2020 Board of Trustees of Leland Stanford Jr. University,
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

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.lockss.app.LockssDaemon;
import org.lockss.config.RestConfigClient;
import org.lockss.log.L4JLogger;
import org.lockss.util.rest.RestUtil;
import org.lockss.util.rest.exception.LockssRestException;
import org.lockss.util.rest.exception.LockssRestHttpException;
import org.lockss.state.AuStateBean;
import org.lockss.state.StateManager;
import org.lockss.spring.test.SpringLockssTestCase4;
import org.lockss.util.time.TimeBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Test class for org.lockss.laaws.config.api.AustatesApiServiceImpl.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class TestAustatesApiServiceImpl extends SpringLockssTestCase4 {
  private static L4JLogger log = L4JLogger.getLogger();

  private static final String EMPTY_STRING = "";

  // The identifiers of AUs.
  private static final String BAD_AUID = "badAuId";
  private static final String AUID_1 = "plugin1&auid1";
  private static final String AUID_2 = "plugin1&auid2";
  private static final String AUID_3 = "plugin1&auid3";
  private static final String AUID_4 = "plugin1&auid4";

  // Credentials.
  private final Credentials USER_ADMIN =
      new Credentials("lockss-u", "lockss-p");
  private final Credentials AU_ADMIN =
      new Credentials("au-admin", "I'mAuAdmin");
  private final Credentials CONTENT_ADMIN =
      new Credentials("content-admin", "I'mContentAdmin");
  private final Credentials ANYBODY =
      new Credentials("someUser", "somePassword");

  // Request cookies.
  private static final String COOKIE_1 = "xLockssRequestCookie_1";
  private static final String COOKIE_2 = "xLockssRequestCookie_2";

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
   * Runs the tests with authentication turned off.
   * 
   * @throws Exception
   *           if there are problems.
   */
  @Test
  public void runUnAuthenticatedTests() throws Exception {
    log.debug2("Invoked");

    // Specify the command line parameters to be used for the tests.
    List<String> cmdLineArgs = getCommandLineArguments();
    cmdLineArgs.add("-p");
    cmdLineArgs.add("test/config/testAuthOff.txt");

    CommandLineRunner runner = appCtx.getBean(CommandLineRunner.class);
    runner.run(cmdLineArgs.toArray(new String[cmdLineArgs.size()]));

    runGetSwaggerDocsTest(getTestUrlTemplate("/v2/api-docs"));
    runMethodsNotAllowedUnAuthenticatedTest();
    getAuStateUnAuthenticatedTest();
    patchAuStateUnAuthenticatedTest();

    log.debug2("Done");
  }

  /**
   * Runs the tests with authentication turned on.
   * 
   * @throws Exception
   *           if there are problems.
   */
  @Test
  public void runAuthenticatedTests() throws Exception {
    log.debug2("Invoked");

    // Specify the command line parameters to be used for the tests.
    List<String> cmdLineArgs = getCommandLineArguments();
    cmdLineArgs.add("-p");
    cmdLineArgs.add("test/config/testAuthOn.txt");

    CommandLineRunner runner = appCtx.getBean(CommandLineRunner.class);
    runner.run(cmdLineArgs.toArray(new String[cmdLineArgs.size()]));

    runGetSwaggerDocsTest(getTestUrlTemplate("/v2/api-docs"));
    runMethodsNotAllowedAuthenticatedTest();
    getAuStateAuthenticatedTest();
    patchAuStateAuthenticatedTest();

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
   * Runs the invalid method-related un-authenticated-specific tests.
   */
  private void runMethodsNotAllowedUnAuthenticatedTest() {
    log.debug2("Invoked");

    // No AUId: Spring reports it cannot find a match to an endpoint.
    runTestMethodNotAllowed(null, null, HttpMethod.POST, HttpStatus.NOT_FOUND);

    // Empty AUId: Spring reports it cannot find a match to an endpoint.
    runTestMethodNotAllowed(EMPTY_STRING, ANYBODY, HttpMethod.PUT,
	HttpStatus.NOT_FOUND);

    // Bad AUId.
    runTestMethodNotAllowed(BAD_AUID, ANYBODY, HttpMethod.POST,
	HttpStatus.METHOD_NOT_ALLOWED);

    runTestMethodNotAllowed(BAD_AUID, null, HttpMethod.PUT,
	HttpStatus.METHOD_NOT_ALLOWED);

    // Good AUId.
    runTestMethodNotAllowed(AUID_1, null, HttpMethod.PUT,
	HttpStatus.METHOD_NOT_ALLOWED);

    runTestMethodNotAllowed(AUID_1, ANYBODY, HttpMethod.POST,
	HttpStatus.METHOD_NOT_ALLOWED);

    runMethodsNotAllowedCommonTest();

    log.debug2("Done");
  }

  /**
   * Runs the invalid method-related authenticated-specific tests.
   */
  private void runMethodsNotAllowedAuthenticatedTest() {
    log.debug2("Invoked");

    // No AUId.
    runTestMethodNotAllowed(null, ANYBODY, HttpMethod.POST,
	HttpStatus.UNAUTHORIZED);

    // Empty AUId.
    runTestMethodNotAllowed(EMPTY_STRING, null, HttpMethod.PUT,
	HttpStatus.UNAUTHORIZED);

    // Bad AUId.
    runTestMethodNotAllowed(BAD_AUID, ANYBODY, HttpMethod.POST,
	HttpStatus.UNAUTHORIZED);

    // No credentials.
    runTestMethodNotAllowed(AUID_1, null, HttpMethod.PUT,
	HttpStatus.UNAUTHORIZED);

    // Bad credentials.
    runTestMethodNotAllowed(AUID_2, ANYBODY, HttpMethod.POST,
	HttpStatus.UNAUTHORIZED);

    runMethodsNotAllowedCommonTest();

    log.debug2("Done");
  }

  /**
   * Runs the invalid method-related authentication-independent tests.
   */
  private void runMethodsNotAllowedCommonTest() {
    log.debug2("Invoked");

    // No AUId: Spring reports it cannot find a match to an endpoint.
    runTestMethodNotAllowed(null, USER_ADMIN, HttpMethod.POST,
	HttpStatus.NOT_FOUND);

    // Empty AUId: Spring reports it cannot find a match to an endpoint.
    runTestMethodNotAllowed(EMPTY_STRING, AU_ADMIN, HttpMethod.PUT,
	HttpStatus.NOT_FOUND);

    // Bad AUId.
    runTestMethodNotAllowed(BAD_AUID, USER_ADMIN, HttpMethod.PUT,
	HttpStatus.METHOD_NOT_ALLOWED);

    runTestMethodNotAllowed(BAD_AUID, AU_ADMIN, HttpMethod.POST,
	HttpStatus.METHOD_NOT_ALLOWED);

    // Good AUId.
    runTestMethodNotAllowed(AUID_1, AU_ADMIN, HttpMethod.PUT,
	HttpStatus.METHOD_NOT_ALLOWED);

    runTestMethodNotAllowed(AUID_1, USER_ADMIN, HttpMethod.POST,
	HttpStatus.METHOD_NOT_ALLOWED);

    log.debug2("Done");
  }

  /**
   * Performs an operation using a method that is not allowed.
   * 
   * @param auId
   *          A String with the Archival Unit identifier.
   * @param credentials
   *          A Credentials with the request credentials.
   * @param method
   *          An HttpMethod with the request method.
   * @param expectedStatus
   *          An HttpStatus with the HTTP status of the result.
   */
  private void runTestMethodNotAllowed(String auId, Credentials credentials,
      HttpMethod method, HttpStatus expectedStatus) {
    log.debug2("auId = {}", auId);
    log.debug2("credentials = {}", credentials);
    log.debug2("method = {}", method);
    log.debug2("expectedStatus = {}", expectedStatus);

    // Get the test URL template.
    String template = getTestUrlTemplate("/austates/{auid}");

    // Create the URI of the request to the REST service.
    UriComponents uriComponents = UriComponentsBuilder.fromUriString(template)
	.build().expand(Collections.singletonMap("auid", auId));

    URI uri = UriComponentsBuilder.newInstance().uriComponents(uriComponents)
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
    ResponseEntity<String> response = new TestRestTemplate(restTemplate)
	.exchange(uri, method, requestEntity, String.class);

    // Get the response status.
    HttpStatus statusCode = response.getStatusCode();
    assertFalse(RestUtil.isSuccess(statusCode));
    assertEquals(expectedStatus, statusCode);
  }

  /**
   * Runs the getAuState()-related un-authenticated-specific tests.
   */
  private void getAuStateUnAuthenticatedTest() throws Exception {
    log.debug2("Invoked");

    StateManager stateManager =
	LockssDaemon.getLockssDaemon().getManagerByType(StateManager.class);

    // No AUId: Spring reports it cannot find a match to an endpoint.
    runTestGetAuState(null, null, HttpStatus.NOT_FOUND);

    // No AUId using the REST service client.
    assertNull(runTestGetAuStateClient(null, null, HttpStatus.NOT_FOUND));

    // Empty AUId: Spring reports it cannot find a match to an endpoint.
    runTestGetAuState(EMPTY_STRING, ANYBODY, HttpStatus.NOT_FOUND);

    // Empty AUId using the REST service client.
    assertNull(runTestGetAuStateClient(EMPTY_STRING, ANYBODY,
	HttpStatus.NOT_FOUND));

    // Bad AUId.
    runTestGetAuState(BAD_AUID, null, HttpStatus.BAD_REQUEST);

    // No credentials.
    String result = runTestGetAuState(AUID_1, null, HttpStatus.OK);

    // Verify.
    assertEquals(stateManager.getAuStateBean(AUID_1).toJson(), result);

    // No credentials using the REST service client.
    assertEquals(result, runTestGetAuStateClient(AUID_1, null, HttpStatus.OK));

    // Bad credentials.
    result = runTestGetAuState(AUID_2, ANYBODY, HttpStatus.OK);

    // Verify.
    assertEquals(stateManager.getAuStateBean(AUID_2).toJson(), result);

    // Bad credentials using the REST service client.
    assertEquals(result,
	runTestGetAuStateClient(AUID_2, ANYBODY, HttpStatus.OK));

    getAuStateCommonTest();

    log.debug2("Done");
  }

  /**
   * Runs the getAuState()-related authenticated-specific tests.
   * 
   */
  private void getAuStateAuthenticatedTest() throws Exception {
    log.debug2("Invoked");

    // No AUId.
    runTestGetAuState(null, null, HttpStatus.UNAUTHORIZED);

    // No AUId using the REST service client.
    assertNull(runTestGetAuStateClient(null, null, HttpStatus.UNAUTHORIZED));

    // Empty AUId.
    runTestGetAuState(EMPTY_STRING, null, HttpStatus.UNAUTHORIZED);

    // Empty AUId using the REST service client.
    assertNull(runTestGetAuStateClient(EMPTY_STRING, null,
	HttpStatus.UNAUTHORIZED));

    // Bad AUId.
    runTestGetAuState(BAD_AUID, ANYBODY, HttpStatus.UNAUTHORIZED);

    // Bad AUId using the REST service client.
    assertNull(runTestGetAuStateClient(BAD_AUID, null,
	HttpStatus.UNAUTHORIZED));

    // No credentials.
    runTestGetAuState(AUID_1, null, HttpStatus.UNAUTHORIZED);

    // No credentials using the REST service client.
    assertNull(runTestGetAuStateClient(AUID_1, null,
	HttpStatus.UNAUTHORIZED));

    // Bad credentials.
    runTestGetAuState(AUID_2, ANYBODY, HttpStatus.UNAUTHORIZED);

    // Bad credentials using the REST service client.
    assertNull(runTestGetAuStateClient(AUID_2, ANYBODY,
	HttpStatus.UNAUTHORIZED));

    getAuStateCommonTest();

    log.debug2("Done");
  }

  /**
   * Runs the getAuState()-related authentication-independent tests.
   */
  private void getAuStateCommonTest() throws Exception {
    log.debug2("Invoked");

    StateManager stateManager =
	LockssDaemon.getLockssDaemon().getManagerByType(StateManager.class);

    // No AUId: Spring reports it cannot find a match to an endpoint.
    runTestGetAuState(null, USER_ADMIN, HttpStatus.NOT_FOUND);

    // No AUId using the REST service client.
    assertNull(runTestGetAuStateClient(null, USER_ADMIN, HttpStatus.NOT_FOUND));

    // Empty AUId: Spring reports it cannot find a match to an endpoint.
    runTestGetAuState(EMPTY_STRING, AU_ADMIN, HttpStatus.NOT_FOUND);

    // Empty AUId using the REST service client.
    assertNull(runTestGetAuStateClient(EMPTY_STRING, AU_ADMIN,
	HttpStatus.NOT_FOUND));

    // Bad AUId.
    runTestGetAuState(BAD_AUID, USER_ADMIN, HttpStatus.BAD_REQUEST);

    // Bad AUId using the REST service client.
    assertNull(runTestGetAuStateClient(BAD_AUID, USER_ADMIN,
	HttpStatus.BAD_REQUEST));

    // Good AUId.
    log.error("Calling runTestGetAuState for " + AUID_1);
    String result = runTestGetAuState(AUID_1, AU_ADMIN, HttpStatus.OK);
    log.error("Back from calling runTestGetAuState for " + AUID_1);

    // Verify
    assertEquals(stateManager.getAuStateBean(AUID_1).toJson(), result);

    // Good AUId using the REST service client.
    log.error("Calling runTestGetAuStateClient for " + AUID_1);
    assertEquals(result,
	runTestGetAuStateClient(AUID_1, AU_ADMIN, HttpStatus.OK));
    log.error("Back from calling runTestGetAuStateClient for " + AUID_1);

    // Good AUId.
    result = runTestGetAuState(AUID_2, USER_ADMIN, HttpStatus.OK);

    // Verify
    assertEquals(stateManager.getAuStateBean(AUID_2).toJson(), result);

    // Good AUId using the REST service client.
    assertEquals(result,
	runTestGetAuStateClient(AUID_2, USER_ADMIN, HttpStatus.OK));

    log.debug2("Done");
  }

  /**
   * Performs a GET operation.
   * 
   * @param auId
   *          A String with the identifier of the Archival Unit.
   * @param credentials
   *          A Credentials with the request credentials.
   * @param expectedStatus
   *          An HttpStatus with the HTTP status of the result.
   * @return a String with the stored Archival Unit state.
   */
  private String runTestGetAuState(String auId, Credentials credentials,
      HttpStatus expectedStatus) {
    log.debug2("auId = {}", auId);
    log.debug2("credentials = {}", credentials);
    log.debug2("expectedStatus = {}", expectedStatus);

    // Get the test URL template.
    String template = getTestUrlTemplate("/austates/{auid}");

    // Create the URI of the request to the REST service.
    UriComponents uriComponents = UriComponentsBuilder.fromUriString(template)
	.build().expand(Collections.singletonMap("auid", auId));

    URI uri = UriComponentsBuilder.newInstance().uriComponents(uriComponents)
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
   * Performs a GET operation using the REST service client.
   * 
   * @param auId
   *          A String with the identifier of the Archival Unit.
   * @param credentials
   *          A Credentials with the request credentials.
   * @param expectedStatus
   *          An HttpStatus with the HTTP status of the result.
   * @return a String with the stored Archival Unit state.
   */
  private String runTestGetAuStateClient(String auId, Credentials credentials,
      HttpStatus expectedStatus) {
    log.debug2("auId = {}", auId);
    log.debug2("credentials = {}", credentials);
    log.debug2("expectedStatus = {}", expectedStatus);

    String result = null;

    try {
      // Make the request and get the result.
      result = getRestConfigClient(credentials).getArchivalUnitState(auId);
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
   * Runs the patchAuState()-related un-authenticated-specific tests.
   */
  private void patchAuStateUnAuthenticatedTest() throws Exception {
    log.debug2("Invoked");

    StateManager stateManager =
	LockssDaemon.getLockssDaemon().getManagerByType(StateManager.class);

    // No AUId: Spring reports it cannot find a match to an endpoint.
    runTestPatchAuState(null, null, null, null, HttpStatus.NOT_FOUND);

    runTestPatchAuState(null, null, MediaType.APPLICATION_JSON, ANYBODY,
	HttpStatus.NOT_FOUND);

    // No AUId using the REST service client.
    runTestPatchAuStateClient(null, null, null, null, HttpStatus.NOT_FOUND);

    // Empty AUId: Spring reports it cannot find a match to an endpoint.
    runTestPatchAuState(EMPTY_STRING, null, null, CONTENT_ADMIN,
	HttpStatus.NOT_FOUND);

    runTestPatchAuState(EMPTY_STRING, null, MediaType.APPLICATION_JSON, null,
	HttpStatus.NOT_FOUND);

    // Empty AUId using the REST service client.
    runTestPatchAuStateClient(EMPTY_STRING, null, ANYBODY, EMPTY_STRING,
	HttpStatus.NOT_FOUND);

    // Bad AUId.
    runTestPatchAuState(BAD_AUID, null, null, ANYBODY,
	HttpStatus.UNSUPPORTED_MEDIA_TYPE);

    runTestPatchAuState(BAD_AUID, null, MediaType.APPLICATION_JSON,
	CONTENT_ADMIN, HttpStatus.BAD_REQUEST);

    // Bad AUId using the REST service client.
    runTestPatchAuStateClient(BAD_AUID, null, CONTENT_ADMIN, COOKIE_1,
	HttpStatus.BAD_REQUEST);

    // No AU state.
    runTestPatchAuState(AUID_1, null, null, null,
	HttpStatus.UNSUPPORTED_MEDIA_TYPE);

    runTestPatchAuState(AUID_1, null, MediaType.APPLICATION_JSON, ANYBODY,
	HttpStatus.BAD_REQUEST);

    runTestPatchAuState(AUID_1, EMPTY_STRING, null, CONTENT_ADMIN,
	HttpStatus.UNSUPPORTED_MEDIA_TYPE);

    runTestPatchAuState(AUID_1, EMPTY_STRING, MediaType.APPLICATION_JSON, null,
	HttpStatus.BAD_REQUEST);

    // No AU state using the REST service client.
    runTestPatchAuStateClient(AUID_1, null, null, COOKIE_2,
	HttpStatus.BAD_REQUEST);

    runTestPatchAuStateClient(AUID_1, EMPTY_STRING, ANYBODY, null,
	HttpStatus.BAD_REQUEST);

    AuStateBean bean = new AuStateBean();
    long creationTime = TimeBase.nowMs();
    bean.setAuCreationTime(creationTime);

    // No Content-Type header.
    runTestPatchAuState(AUID_1, bean.toJson(), null, null,
	HttpStatus.UNSUPPORTED_MEDIA_TYPE);

    // Get the current state of the first AU.
    String backupState1 = runTestGetAuState(AUID_1, null, HttpStatus.OK);

    // Verify.
    AuStateBean bean1 = stateManager.getAuStateBean(AUID_1);
    assertEquals(bean1.toJson(), backupState1);

    // Get the current state of the second AU.
    String backupState2 = runTestGetAuState(AUID_2, ANYBODY, HttpStatus.OK);

    // Verify.
    AuStateBean bean2 = stateManager.getAuStateBean(AUID_2);
    assertEquals(bean2.toJson(), backupState2);

    // Patch first AU.
    runTestPatchAuState(AUID_1, bean.toJson(), MediaType.APPLICATION_JSON, null,
	HttpStatus.OK);

    // Verify.
    assertEquals(creationTime,
	stateManager.getAuStateBean(AUID_1).getAuCreationTime());

    // Verify that the current state of the second AU has not been affected.
    assertEquals(backupState2,
	runTestGetAuState(AUID_2, CONTENT_ADMIN, HttpStatus.OK));

    // Restore the original state of the first AU.
    runTestPatchAuState(AUID_1, backupState1, MediaType.APPLICATION_JSON,
	ANYBODY, HttpStatus.OK);

    // Verify.
    assertEquals(backupState1, runTestGetAuState(AUID_1, null, HttpStatus.OK));

    // Verify that the current state of the second AU has not been affected.
    assertEquals(backupState2,
	runTestGetAuState(AUID_2, ANYBODY, HttpStatus.OK));

    // Patch second AU using the REST service client.
    runTestPatchAuStateClient(AUID_2, bean.toJson(), CONTENT_ADMIN,
	EMPTY_STRING, HttpStatus.OK);

    // Verify.
    assertEquals(creationTime,
	stateManager.getAuStateBean(AUID_2).getAuCreationTime());

    // Verify that the current state of the first AU has not been affected.
    assertEquals(backupState1,
	runTestGetAuState(AUID_1, CONTENT_ADMIN, HttpStatus.OK));

    // Restore the original state of the second AU.
    runTestPatchAuState(AUID_2, backupState2, MediaType.APPLICATION_JSON, null,
	HttpStatus.OK);

    // Verify.
    assertEquals(backupState2, runTestGetAuState(AUID_2, null, HttpStatus.OK));

    // Verify that the current state of the first AU has not been affected.
    assertEquals(backupState1,
	runTestGetAuState(AUID_1, ANYBODY, HttpStatus.OK));

    // Patch third AU.
    runTestPatchAuState(AUID_3, bean.toJson(), MediaType.APPLICATION_JSON,
	CONTENT_ADMIN, HttpStatus.OK);

    // Verify.
    assertEquals(creationTime,
	stateManager.getAuStateBean(AUID_3).getAuCreationTime());

    // Verify that the current state of the first AU has not been affected.
    assertEquals(backupState1, runTestGetAuState(AUID_1, null, HttpStatus.OK));

    // Verify that the current state of the second AU has not been affected.
    assertEquals(backupState2,
	runTestGetAuState(AUID_2, ANYBODY, HttpStatus.OK));

    patchAuStateCommonTest();

    log.debug2("Done");
  }

  /**
   * Runs the patchAuState()-related authenticated-specific tests.
   */
  private void patchAuStateAuthenticatedTest() throws Exception {
    log.debug2("Invoked");

    // No AUId.
    runTestPatchAuState(null, null, null, null, HttpStatus.UNAUTHORIZED);

    runTestPatchAuState(null, null, MediaType.APPLICATION_JSON, ANYBODY,
	HttpStatus.UNAUTHORIZED);

    // Spring reports it cannot find a match to an endpoint.
    runTestPatchAuState(null, null, MediaType.APPLICATION_JSON, CONTENT_ADMIN,
	HttpStatus.NOT_FOUND);

    // No AUId using the REST service client.
    runTestPatchAuStateClient(null, null, null, null, HttpStatus.UNAUTHORIZED);

    // Empty AUId.
    runTestPatchAuState(EMPTY_STRING, null, null, ANYBODY,
	HttpStatus.UNAUTHORIZED);

    runTestPatchAuState(EMPTY_STRING, null, MediaType.APPLICATION_JSON, ANYBODY,
	HttpStatus.UNAUTHORIZED);

    // Spring reports it cannot find a match to an endpoint.
    runTestPatchAuState(EMPTY_STRING, null, MediaType.APPLICATION_JSON,
	CONTENT_ADMIN, HttpStatus.NOT_FOUND);

    // Empty AUId using the REST service client.
    runTestPatchAuStateClient(EMPTY_STRING, null, ANYBODY, EMPTY_STRING,
	HttpStatus.UNAUTHORIZED);

    // Bad AUId.
    runTestPatchAuState(BAD_AUID, null, MediaType.APPLICATION_JSON, null,
	HttpStatus.UNAUTHORIZED);

    runTestPatchAuState(BAD_AUID, null, MediaType.APPLICATION_JSON,
	CONTENT_ADMIN, HttpStatus.BAD_REQUEST);

    // Bad AUId using the REST service client.
    runTestPatchAuStateClient(BAD_AUID, null, ANYBODY, COOKIE_1,
	HttpStatus.UNAUTHORIZED);

    runTestPatchAuStateClient(BAD_AUID, null, CONTENT_ADMIN, COOKIE_2,
	HttpStatus.BAD_REQUEST);

    // No AU state.
    runTestPatchAuState(AUID_1, null, null, null, HttpStatus.UNAUTHORIZED);

    runTestPatchAuState(AUID_1, null, MediaType.APPLICATION_JSON, ANYBODY,
	HttpStatus.UNAUTHORIZED);

    runTestPatchAuState(AUID_1, null, MediaType.APPLICATION_JSON, CONTENT_ADMIN,
	HttpStatus.BAD_REQUEST);

    runTestPatchAuState(AUID_1, EMPTY_STRING, null, null,
	HttpStatus.UNAUTHORIZED);

    runTestPatchAuState(AUID_1, EMPTY_STRING, MediaType.APPLICATION_JSON,
	ANYBODY, HttpStatus.UNAUTHORIZED);

    runTestPatchAuState(AUID_1, EMPTY_STRING, MediaType.APPLICATION_JSON,
	CONTENT_ADMIN, HttpStatus.BAD_REQUEST);

    // No AU state using the REST service client.
    runTestPatchAuStateClient(AUID_1, null, null, null,
	HttpStatus.UNAUTHORIZED);

    runTestPatchAuStateClient(AUID_1, null, CONTENT_ADMIN, EMPTY_STRING,
	HttpStatus.BAD_REQUEST);

    runTestPatchAuStateClient(AUID_1, EMPTY_STRING, ANYBODY, COOKIE_1,
	HttpStatus.UNAUTHORIZED);

    runTestPatchAuStateClient(AUID_1, EMPTY_STRING, CONTENT_ADMIN, COOKIE_2,
	HttpStatus.BAD_REQUEST);

    AuStateBean bean = new AuStateBean();
    long creationTime = TimeBase.nowMs();
    bean.setAuCreationTime(creationTime);

    // No Content-Type header.
    runTestPatchAuState(AUID_1, bean.toJson(), null, null,
	HttpStatus.UNAUTHORIZED);

    // Patch first AU.
    runTestPatchAuState(AUID_1, bean.toJson(), MediaType.APPLICATION_JSON, null,
	HttpStatus.UNAUTHORIZED);

    // Patch first AU using the REST service client.
    runTestPatchAuStateClient(AUID_1, bean.toJson(), ANYBODY, null,
	HttpStatus.UNAUTHORIZED);

    // Patch second AU.
    runTestPatchAuState(AUID_2, bean.toJson(), MediaType.APPLICATION_JSON,
	CONTENT_ADMIN, HttpStatus.FORBIDDEN);

    // Patch second AU using the REST service client.
    runTestPatchAuStateClient(AUID_2, bean.toJson(), CONTENT_ADMIN,
	EMPTY_STRING, HttpStatus.FORBIDDEN);

    patchAuStateCommonTest();

    log.debug2("Done");
  }

  /**
   * Runs the patchAuState()-related authentication-independent tests.
   */
  private void patchAuStateCommonTest() throws Exception {
    log.debug2("Invoked");

    StateManager stateManager =
	LockssDaemon.getLockssDaemon().getManagerByType(StateManager.class);

    // No AUId: Spring reports it cannot find a match to an endpoint.
    runTestPatchAuState(null, null, null, AU_ADMIN, HttpStatus.NOT_FOUND);

    runTestPatchAuState(null, null, MediaType.APPLICATION_JSON, USER_ADMIN,
	HttpStatus.NOT_FOUND);

    // No AUId using the REST service client.
    runTestPatchAuStateClient(null, null, AU_ADMIN, null, HttpStatus.NOT_FOUND);

    // Empty AUId: Spring reports it cannot find a match to an endpoint.
    runTestPatchAuState(EMPTY_STRING, null, null, AU_ADMIN,
	HttpStatus.NOT_FOUND);

    runTestPatchAuState(EMPTY_STRING, null, MediaType.APPLICATION_JSON,
	USER_ADMIN, HttpStatus.NOT_FOUND);

    // Empty AUId using the REST service client.
    runTestPatchAuStateClient(EMPTY_STRING, null, USER_ADMIN, EMPTY_STRING,
	HttpStatus.NOT_FOUND);

    // Bad AUId.
    runTestPatchAuState(BAD_AUID, null, null, AU_ADMIN,
	HttpStatus.UNSUPPORTED_MEDIA_TYPE);

    runTestPatchAuState(BAD_AUID, null, MediaType.APPLICATION_JSON,
	USER_ADMIN, HttpStatus.BAD_REQUEST);

    // Bad AUId using the REST service client.
    runTestPatchAuStateClient(BAD_AUID, null, AU_ADMIN, COOKIE_1,
	HttpStatus.BAD_REQUEST);

    // No AU state.
    runTestPatchAuState(AUID_1, null, null, AU_ADMIN,
	HttpStatus.UNSUPPORTED_MEDIA_TYPE);

    runTestPatchAuState(AUID_1, null, MediaType.APPLICATION_JSON, USER_ADMIN,
	HttpStatus.BAD_REQUEST);

    runTestPatchAuState(AUID_1, EMPTY_STRING, null, AU_ADMIN,
	HttpStatus.UNSUPPORTED_MEDIA_TYPE);

    runTestPatchAuState(AUID_1, EMPTY_STRING, MediaType.APPLICATION_JSON,
	USER_ADMIN, HttpStatus.BAD_REQUEST);

    // No AU state using the REST service client.
    runTestPatchAuStateClient(AUID_1, null, USER_ADMIN, COOKIE_2,
	HttpStatus.BAD_REQUEST);

    runTestPatchAuStateClient(AUID_1, EMPTY_STRING, AU_ADMIN, null,
	HttpStatus.BAD_REQUEST);

    AuStateBean bean = new AuStateBean();
    long creationTime = TimeBase.nowMs();
    bean.setAuCreationTime(creationTime);

    // No Content-Type header.
    runTestPatchAuState(AUID_1, bean.toJson(), null, AU_ADMIN,
	HttpStatus.UNSUPPORTED_MEDIA_TYPE);

    runTestPatchAuState(AUID_1, bean.toJson(), null, USER_ADMIN,
	HttpStatus.UNSUPPORTED_MEDIA_TYPE);

    // Get the current state of the first AU.
    String backupState1 =
	runTestGetAuState(AUID_1, AU_ADMIN, HttpStatus.OK);

    // Verify.
    AuStateBean bean1 = stateManager.getAuStateBean(AUID_1);
    assertEquals(bean1.toJson(), backupState1);

    // Get the current state of the second AU.
    String backupState2 = runTestGetAuState(AUID_2, USER_ADMIN, HttpStatus.OK);

    // Verify.
    AuStateBean bean2 = stateManager.getAuStateBean(AUID_2);
    assertEquals(bean2.toJson(), backupState2);

    // Patch first AU using the REST service client.
    runTestPatchAuStateClient(AUID_1, bean.toJson(), AU_ADMIN, EMPTY_STRING,
	HttpStatus.OK);

    // Verify.
    assertEquals(creationTime,
	stateManager.getAuStateBean(AUID_1).getAuCreationTime());

    // Verify that the current state of the second AU has not been affected.
    assertEquals(backupState2,
	runTestGetAuState(AUID_2, USER_ADMIN, HttpStatus.OK));

    // Restore the original state of the first AU.
    runTestPatchAuState(AUID_1, backupState1, MediaType.APPLICATION_JSON,
	AU_ADMIN, HttpStatus.OK);

    // Verify.
    assertEquals(backupState1,
	runTestGetAuState(AUID_1, USER_ADMIN, HttpStatus.OK));

    // Verify that the current state of the second AU has not been affected.
    assertEquals(backupState2,
	runTestGetAuState(AUID_2, AU_ADMIN, HttpStatus.OK));

    // Patch second AU.
    runTestPatchAuState(AUID_2, bean.toJson(), MediaType.APPLICATION_JSON,
	USER_ADMIN, HttpStatus.OK);

    // Verify.
    assertEquals(creationTime,
	stateManager.getAuStateBean(AUID_2).getAuCreationTime());

    // Verify that the current state of the first AU has not been affected.
    assertEquals(backupState1,
	runTestGetAuState(AUID_1, AU_ADMIN, HttpStatus.OK));

    // Restore the original state of the second AU using the REST service
    // client.
    runTestPatchAuStateClient(AUID_2, backupState2, USER_ADMIN, COOKIE_1,
	HttpStatus.OK);

    // Verify.
    assertEquals(backupState2,
	runTestGetAuState(AUID_2, AU_ADMIN, HttpStatus.OK));

    // Verify that the current state of the first AU has not been affected.
    assertEquals(backupState1,
	runTestGetAuState(AUID_1, USER_ADMIN, HttpStatus.OK));

    // Patch fourth AU.
    runTestPatchAuState(AUID_4, bean.toJson(), MediaType.APPLICATION_JSON,
	AU_ADMIN, HttpStatus.OK);

    // Verify.
    assertEquals(creationTime,
	stateManager.getAuStateBean(AUID_4).getAuCreationTime());

    // Verify that the current state of the first AU has not been affected.
    assertEquals(backupState1,
	runTestGetAuState(AUID_1, USER_ADMIN, HttpStatus.OK));

    // Verify that the current state of the second AU has not been affected.
    assertEquals(backupState2,
	runTestGetAuState(AUID_2, AU_ADMIN, HttpStatus.OK));

    log.debug2("Done");
  }

  /**
   * Performs a PATCH operation.
   * 
   * @param auState
   *          A String with the parts of the Archival Unit state to be replaced.
   * @param contentType
   *          A MediaType with the content type of the request.
   * @param credentials
   *          A Credentials with the request credentials.
   * @param expectedStatus
   *          An HttpStatus with the HTTP status of the result.
   */
  private void runTestPatchAuState(String auId, String auState,
      MediaType contentType, Credentials credentials, HttpStatus expectedStatus)
  {
    log.debug2("auId = {}", auId);
    log.debug2("auState = {}", auState);
    log.debug2("contentType = {}", contentType);
    log.debug2("credentials = {}", credentials);
    log.debug2("expectedStatus = {}", expectedStatus);

    // Get the test URL template.
    String template = getTestUrlTemplate("/austates/{auid}");

    // Create the URI of the request to the REST service.
    UriComponents uriComponents = UriComponentsBuilder.fromUriString(template)
	.build().expand(Collections.singletonMap("auid", auId));

    URI uri = UriComponentsBuilder.newInstance().uriComponents(uriComponents)
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
    if (contentType != null || user != null || password != null) {

      // Initialize the request headers.
      HttpHeaders headers = new HttpHeaders();

      // Check whether there is a custom "Content-Type" header.
      if (contentType != null) {
	// Yes: Set it.
	headers.setContentType(contentType);
      }

      // Set up the authentication credentials, if necessary.
      if (credentials != null) {
	credentials.setUpBasicAuthentication(headers);
      }

      log.trace("requestHeaders = {}", () -> headers.toSingleValueMap());

      // Create the request entity.
      requestEntity = new HttpEntity<String>(auState, headers);
    }

    // Make the request and get the response. 
    ResponseEntity<String> response = new TestRestTemplate(restTemplate)
	.exchange(uri, HttpMethod.PATCH, requestEntity, String.class);

    // Get the response status.
    HttpStatus statusCode = response.getStatusCode();
    assertEquals(expectedStatus, statusCode);
  }

  /**
   * Performs a PATCH operation for an Archival Unit using the REST service
   * client.
   * 
   * @param auId
   *          A String with the Archival Unit identifier.
   * @param auState
   *          A String with the parts of the Archival Unit state to be replaced.
   * @param credentials
   *          A Credentials with the request credentials.
   * @param xLockssRequestCookie
   *          A String with the request cookie.
   * @param expectedStatus
   *          An HttpStatus with the HTTP status of the result.
   */
  private void runTestPatchAuStateClient(String auId, String auState,
      Credentials credentials, String xLockssRequestCookie,
      HttpStatus expectedStatus) {
    log.debug2("auId = {}", auId);
    log.debug2("auState = {}", auState);
    log.debug2("credentials = {}", credentials);
    log.debug2("xLockssRequestCookie = {}", xLockssRequestCookie);
    log.debug2("expectedStatus = {}", expectedStatus);

    try {
      // Make the request.
      getRestConfigClient(credentials).patchArchivalUnitState(auId, auState,
	  xLockssRequestCookie);

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
