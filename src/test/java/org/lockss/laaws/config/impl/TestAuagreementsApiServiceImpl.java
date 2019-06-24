/*

Copyright (c) 2000-2019 Board of Trustees of Leland Stanford Jr. University,
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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.lockss.app.LockssDaemon;
import org.lockss.config.RestConfigClient;
import org.lockss.log.L4JLogger;
import org.lockss.protocol.AgreementType;
import org.lockss.protocol.AuAgreements;
import org.lockss.protocol.IdentityManager;
import org.lockss.rs.RestUtil;
import org.lockss.rs.exception.LockssRestException;
import org.lockss.rs.exception.LockssRestHttpException;
import org.lockss.state.StateManager;
import org.lockss.test.MockLockssDaemon;
import org.lockss.test.SpringLockssTestCase;
import org.lockss.util.ListUtil;
import org.lockss.util.StringUtil;
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
 * Test class for org.lockss.laaws.config.api.AuagreementsApiServiceImpl.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class TestAuagreementsApiServiceImpl extends SpringLockssTestCase {
  private static L4JLogger log = L4JLogger.getLogger();

  private static final String EMPTY_STRING = "";

  // The identifiers of AUs.
  private static final String BAD_AUID = "badAuId";
  private static final String AUID_1 = "plugin1&auid1";
  private static final String AUID_2 = "plugin1&auid2";
  private static final String AUID_3 = "plugin1&auid3";
  private static final String AUID_4 = "plugin1&auid4";
  private static final String AUID_5 = "plugin1&auid5";
  private static final String AUID_6 = "plugin1&auid6";
  private static final String AUID_7 = "plugin2&auid7";

  // Credentials.
  private final Credentials USER_ADMIN =
      new Credentials("lockss-u", "lockss-p");
  private final Credentials AU_ADMIN =
      new Credentials("au-admin", "I'mAuAdmin");
  private final Credentials CONTENT_ADMIN =
      new Credentials("content-admin", "I'mContentAdmin");
  private final Credentials ANYBODY =
      new Credentials("someUser", "somePassword");

  // Good first peer identity ID string list.
  private List<String> goodPeerIdentityIdStringList1 =
      ListUtil.list("TCP:[1.2.3.4]:111", "TCP:[2.3.4.5]:222",
	  "TCP:[3.4.5.6]:333");

  // Good first peer identity ID string list subset.
  private List<String> goodPeerIdentityIdStringList1s =
      ListUtil.list("TCP:[2.3.4.5]:222", "TCP:[3.4.5.6]:333");

  // Good second peer identity ID string list.
  private List<String> goodPeerIdentityIdStringList2 =
      ListUtil.list("TCP:[9.8.7.6]:999", "TCP:[8.7.6.5]:888");

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

  MockLockssDaemon daemon = null;
  IdentityManager idMgr;

  /**
   * Set up code to be run before each test.
   * 
   * @throws IOException if there are problems.
   */
  @Before
  public void setUpBeforeEachTest() throws IOException {
    log.debug2("port = {}", port);

    // Set up the temporary directory where the test data will reside.
    setUpTempDirectory(TestAuagreementsApiServiceImpl.class.getCanonicalName());

    // Set up the UI port.
    setUpUiPort(UI_PORT_CONFIGURATION_TEMPLATE, UI_PORT_CONFIGURATION_FILE);

    daemon = getMockLockssDaemon();
    idMgr = daemon.getIdentityManager();
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
    getAuAgreementsUnAuthenticatedTest();
    patchAuAgreementsUnAuthenticatedTest();

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
    getAuAgreementsAuthenticatedTest();
    patchAuAgreementsAuthenticatedTest();

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
    String template = getTestUrlTemplate("/auagreements/{auid}");

    // Create the URI of the request to the REST service.
    UriComponents uriComponents = UriComponentsBuilder.fromUriString(template)
	.build().expand(Collections.singletonMap("auid", auId));

    URI uri = UriComponentsBuilder.newInstance().uriComponents(uriComponents)
	.build().encode().toUri();
    log.trace("uri = {}", uri);

    // Initialize the request to the REST service.
    RestTemplate restTemplate = new RestTemplate();

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
   * Runs the getAuAgreements()-related un-authenticated-specific tests.
   */
  private void getAuAgreementsUnAuthenticatedTest() throws Exception {
    log.debug2("Invoked");

    StateManager stateManager =
	LockssDaemon.getLockssDaemon().getManagerByType(StateManager.class);

    // No AUId: Spring reports it cannot find a match to an endpoint.
    runTestGetAuAgreements(null, null, HttpStatus.NOT_FOUND);

    // No AUId using the REST service client.
    assertNull(runTestGetAuAgreementsClient(null, null, HttpStatus.NOT_FOUND));

    // Empty AUId: Spring reports it cannot find a match to an endpoint.
    runTestGetAuAgreements(EMPTY_STRING, ANYBODY, HttpStatus.NOT_FOUND);

    // Empty AUId using the REST service client.
    assertNull(runTestGetAuAgreementsClient(EMPTY_STRING, ANYBODY,
	HttpStatus.NOT_FOUND));

    // Bad AUId.
    runTestGetAuAgreements(BAD_AUID, null, HttpStatus.BAD_REQUEST);

    // No credentials.
    String result = runTestGetAuAgreements(AUID_1, null, HttpStatus.OK);

    // Verify.
    assertEquals(stateManager.getAuAgreements(AUID_1).toJson(), result);

    // No credentials using the REST service client.
    assertEquals(result,
	runTestGetAuAgreementsClient(AUID_1, null, HttpStatus.OK));

    // Bad credentials.
    result = runTestGetAuAgreements(AUID_2, ANYBODY, HttpStatus.OK);

    // Verify.
    assertEquals(stateManager.getAuAgreements(AUID_2).toJson(), result);

    // Bad credentials using the REST service client.
    assertEquals(result,
	runTestGetAuAgreementsClient(AUID_2, ANYBODY, HttpStatus.OK));

    getAuAgreementsCommonTest();

    log.debug2("Done");
  }

  /**
   * Runs the getAuAgreements()-related authenticated-specific tests.
   */
  private void getAuAgreementsAuthenticatedTest() throws Exception {
    log.debug2("Invoked");

    // No AUId.
    runTestGetAuAgreements(null, ANYBODY, HttpStatus.UNAUTHORIZED);

    // No AUId using the REST service client.
    assertNull(runTestGetAuAgreementsClient(null, null,
	HttpStatus.UNAUTHORIZED));

    // Empty AUId.
    runTestGetAuAgreements(EMPTY_STRING, null, HttpStatus.UNAUTHORIZED);

    // Empty AUId using the REST service client.
    assertNull(runTestGetAuAgreementsClient(EMPTY_STRING, null,
	HttpStatus.UNAUTHORIZED));

    // Bad AUId.
    runTestGetAuAgreements(BAD_AUID, null, HttpStatus.UNAUTHORIZED);

    // Bad AUId using the REST service client.
    assertNull(runTestGetAuAgreementsClient(BAD_AUID, null,
	HttpStatus.UNAUTHORIZED));

    // No credentials.
    runTestGetAuAgreements(AUID_1, null, HttpStatus.UNAUTHORIZED);

    // No credentials using the REST service client.
    assertNull(runTestGetAuAgreementsClient(AUID_1, null,
	HttpStatus.UNAUTHORIZED));

    // Bad credentials.
    runTestGetAuAgreements(AUID_2, ANYBODY, HttpStatus.UNAUTHORIZED);

    // Bad credentials using the REST service client.
    assertNull(runTestGetAuAgreementsClient(AUID_2, ANYBODY,
	HttpStatus.UNAUTHORIZED));

    getAuAgreementsCommonTest();

    log.debug2("Done");
  }

  /**
   * Runs the getAuAgreements()-related authentication-independent tests.
   */
  private void getAuAgreementsCommonTest() throws Exception {
    log.debug2("Invoked");

    StateManager stateManager =
	LockssDaemon.getLockssDaemon().getManagerByType(StateManager.class);

    // No AUId: Spring reports it cannot find a match to an endpoint.
    runTestGetAuAgreements(null, USER_ADMIN, HttpStatus.NOT_FOUND);

    // No AUId using the REST service client.
    assertNull(runTestGetAuAgreementsClient(null, USER_ADMIN,
	HttpStatus.NOT_FOUND));

    // Empty AUId: Spring reports it cannot find a match to an endpoint.
    runTestGetAuAgreements(EMPTY_STRING, AU_ADMIN, HttpStatus.NOT_FOUND);

    // Empty AUId using the REST service client.
    assertNull(runTestGetAuAgreementsClient(EMPTY_STRING, AU_ADMIN,
	HttpStatus.NOT_FOUND));

    // Bad AUId.
    runTestGetAuAgreements(BAD_AUID, USER_ADMIN, HttpStatus.BAD_REQUEST);

    // Bad AUId using the REST service client.
    assertNull(runTestGetAuAgreementsClient(BAD_AUID, USER_ADMIN,
	HttpStatus.BAD_REQUEST));

    // Good AUId.
    String result = runTestGetAuAgreements(AUID_1, AU_ADMIN, HttpStatus.OK);

    // Verify
    assertEquals(stateManager.getAuAgreements(AUID_1).toJson(), result);

    // Good AUId using the REST service client.
    assertEquals(result,
	runTestGetAuAgreementsClient(AUID_1, AU_ADMIN, HttpStatus.OK));

    // Good AUId.
    result = runTestGetAuAgreements(AUID_2, USER_ADMIN, HttpStatus.OK);

    // Verify
    assertEquals(stateManager.getAuAgreements(AUID_2).toJson(), result);

    // Good AUId using the REST service client.
    assertEquals(result,
	runTestGetAuAgreementsClient(AUID_2, USER_ADMIN, HttpStatus.OK));

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
   * @return a String with the Archival Unit poll agreements.
   */
  private String runTestGetAuAgreements(String auId, Credentials credentials,
      HttpStatus expectedStatus) {
    log.debug2("auId = {}", auId);
    log.debug2("credentials = {}", credentials);
    log.debug2("expectedStatus = {}", expectedStatus);

    // Get the test URL template.
    String template = getTestUrlTemplate("/auagreements/{auid}");

    // Create the URI of the request to the REST service.
    UriComponents uriComponents = UriComponentsBuilder.fromUriString(template)
	.build().expand(Collections.singletonMap("auid", auId));

    URI uri = UriComponentsBuilder.newInstance().uriComponents(uriComponents)
	.build().encode().toUri();
    log.trace("uri = {}", uri);

    // Initialize the request to the REST service.
    RestTemplate restTemplate = new RestTemplate();

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
   * @return a String with the Archival Unit poll agreements.
   */
  private String runTestGetAuAgreementsClient(String auId,
      Credentials credentials, HttpStatus expectedStatus) {
    log.debug2("auId = {}", auId);
    log.debug2("credentials = {}", credentials);
    log.debug2("expectedStatus = {}", expectedStatus);

    String result = null;

    try {
      // Make the request and get the result.
      result = getRestConfigClient(credentials).getArchivalUnitAgreements(auId);
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
   * Runs the patchAuAgreements()-related un-authenticated-specific tests.
   */
  private void patchAuAgreementsUnAuthenticatedTest() throws Exception {
    log.debug2("Invoked");

    StateManager stateManager =
	LockssDaemon.getLockssDaemon().getManagerByType(StateManager.class);

    // No AUId: Spring reports it cannot find a match to an endpoint.
    runTestPatchAuAgreements(null, null, null, null, HttpStatus.NOT_FOUND);

    runTestPatchAuAgreements(null, null, MediaType.APPLICATION_JSON, ANYBODY,
	HttpStatus.NOT_FOUND);

    // No AUId using the REST service client.
    runTestPatchAuAgreementsClient(null, null, null, null,
	HttpStatus.NOT_FOUND);

    // Empty AUId: Spring reports it cannot find a match to an endpoint.
    runTestPatchAuAgreements(EMPTY_STRING, null, null, CONTENT_ADMIN,
	HttpStatus.NOT_FOUND);

    runTestPatchAuAgreements(EMPTY_STRING, null, MediaType.APPLICATION_JSON,
	null, HttpStatus.NOT_FOUND);

    // Empty AUId using the REST service client.
    runTestPatchAuAgreementsClient(EMPTY_STRING, null, ANYBODY, EMPTY_STRING,
	HttpStatus.NOT_FOUND);

    // Bad AUId.
    runTestPatchAuAgreements(BAD_AUID, null, null, ANYBODY,
	HttpStatus.UNSUPPORTED_MEDIA_TYPE);

    runTestPatchAuAgreements(BAD_AUID, null, MediaType.APPLICATION_JSON,
	CONTENT_ADMIN, HttpStatus.BAD_REQUEST);

    // Bad AUId using the REST service client.
    runTestPatchAuAgreementsClient(BAD_AUID, null, CONTENT_ADMIN, COOKIE_1,
	HttpStatus.BAD_REQUEST);

    // No AU poll agreements.
    runTestPatchAuAgreements(AUID_1, null, null, null,
	HttpStatus.UNSUPPORTED_MEDIA_TYPE);

    runTestPatchAuAgreements(AUID_1, null, MediaType.APPLICATION_JSON, ANYBODY,
	HttpStatus.BAD_REQUEST);

    runTestPatchAuAgreements(AUID_1, EMPTY_STRING, null, CONTENT_ADMIN,
	HttpStatus.UNSUPPORTED_MEDIA_TYPE);

    runTestPatchAuAgreements(AUID_1, EMPTY_STRING, MediaType.APPLICATION_JSON,
	null, HttpStatus.BAD_REQUEST);

    // No AU poll agreements using the REST service client.
    runTestPatchAuAgreementsClient(AUID_1, null, null, COOKIE_2,
	HttpStatus.BAD_REQUEST);

    runTestPatchAuAgreementsClient(AUID_1, EMPTY_STRING, ANYBODY, null,
	HttpStatus.BAD_REQUEST);

    AuAgreements auAgreements = createAuAgreements(AUID_1,
	goodPeerIdentityIdStringList1, AgreementType.POR, 0.0625f);

    // No Content-Type header.
    runTestPatchAuAgreements(AUID_1, toJsonWithBadPids(auAgreements), null,
	ANYBODY, HttpStatus.UNSUPPORTED_MEDIA_TYPE);

    // Bad peer agreement ID.
    runTestPatchAuAgreements(AUID_1, toJsonWithBadPids(auAgreements),
	MediaType.APPLICATION_JSON, CONTENT_ADMIN, HttpStatus.BAD_REQUEST);

    // Bad peer agreement ID using the REST service client.
    runTestPatchAuAgreementsClient(AUID_1, toJsonWithBadPids(auAgreements),
	CONTENT_ADMIN, EMPTY_STRING, HttpStatus.BAD_REQUEST);

    // Get the current poll agreements of the second AU.
    AuAgreements auAgreements2 = stateManager.getAuAgreements(AUID_2);

    assertEquals(auAgreements2,	AuAgreements.fromJson(AUID_2,
	runTestGetAuAgreements(AUID_2, ANYBODY, HttpStatus.OK), daemon));

    // Patch first AU.
    auAgreements = createAuAgreements(AUID_1, goodPeerIdentityIdStringList1,
	AgreementType.POP, 0.125f);

    runTestPatchAuAgreements(AUID_1, auAgreements.toJson(),
	MediaType.APPLICATION_JSON, null, HttpStatus.OK);

    // Verify.
    assertAuAgreementsMatch(auAgreements, stateManager.getAuAgreements(AUID_1));
    AuAgreements auAgreements1 = auAgreements;

    // Verify that the current poll agreements of the second AU have not been
    // affected.
    assertAuAgreementsMatch(auAgreements2,
	stateManager.getAuAgreements(AUID_2));

    // Patch second AU using the REST service client.
    auAgreements = createAuAgreements(AUID_2, goodPeerIdentityIdStringList2,
	AgreementType.SYMMETRIC_POR, 0.250f);

    runTestPatchAuAgreementsClient(AUID_2, auAgreements.toJson(), ANYBODY,
	COOKIE_1, HttpStatus.OK);

    // Verify.
    assertAuAgreementsMatch(auAgreements, stateManager.getAuAgreements(AUID_2));
    auAgreements2 = auAgreements;

    // Verify that the current poll agreements of the first AU have not been
    // affected.
    assertAuAgreementsMatch(auAgreements1,
	stateManager.getAuAgreements(AUID_1));

    // Patch third AU.
    auAgreements = createAuAgreements(AUID_3, goodPeerIdentityIdStringList1,
	AgreementType.SYMMETRIC_POP, 0.375f);

    runTestPatchAuAgreements(AUID_3, auAgreements.toJson(),
	MediaType.APPLICATION_JSON, CONTENT_ADMIN, HttpStatus.OK);

    // Verify.
    assertAuAgreementsMatch(auAgreements,
	stateManager.getAuAgreements(AUID_3));

    // Patch third AU again with different types of agreements for the same
    // peers.
    auAgreements = createAuAgreements(AUID_3, goodPeerIdentityIdStringList1,
	AgreementType.SYMMETRIC_POR_HINT, 0.75f);

    runTestPatchAuAgreements(AUID_3, auAgreements.toJson(),
	MediaType.APPLICATION_JSON, null, HttpStatus.OK);

    // Verify.
    assertAuAgreementsMatch(auAgreements, stateManager.getAuAgreements(AUID_3));

    // Patch fourth AU using the REST service client.
    auAgreements = createAuAgreements(AUID_4, goodPeerIdentityIdStringList1,
	AgreementType.POR_HINT, 0.4375f);

    runTestPatchAuAgreementsClient(AUID_4, auAgreements.toJson(), ANYBODY,
	COOKIE_2, HttpStatus.OK);

    // Verify.
    assertAuAgreementsMatch(auAgreements, stateManager.getAuAgreements(AUID_4));
    AuAgreements auAgreements4 = auAgreements;

    // Patch fourth AU again with a subset of agreements for the same peers
    // using the REST service client.
    auAgreements = createAuAgreements(AUID_4, goodPeerIdentityIdStringList1s,
	AgreementType.POR_HINT, 0.875f);

    runTestPatchAuAgreementsClient(AUID_4, auAgreements.toJson(),
	CONTENT_ADMIN, null, HttpStatus.OK);

    // Verify.
    assertAuAgreementsMatch(expectedMergedAuAgreements(auAgreements4,
	auAgreements), stateManager.getAuAgreements(AUID_4));

    // Patch fifth AU.
    auAgreements = createAuAgreements(AUID_5, goodPeerIdentityIdStringList1,
	AgreementType.POP_HINT, 0.5f);

    runTestPatchAuAgreements(AUID_5, auAgreements.toJson(),
	MediaType.APPLICATION_JSON, null, HttpStatus.OK);

    // Verify.
    assertAuAgreementsMatch(auAgreements, stateManager.getAuAgreements(AUID_5));
    AuAgreements auAgreements5 = auAgreements;

    // Patch fifth AU again with agreements for other peers.
    auAgreements = createAuAgreements(AUID_5, goodPeerIdentityIdStringList2,
	AgreementType.SYMMETRIC_POR_HINT, 0.75f);

    runTestPatchAuAgreements(AUID_5, auAgreements.toJson(),
	MediaType.APPLICATION_JSON, CONTENT_ADMIN, HttpStatus.OK);

    // Verify.
    assertAuAgreementsMatch(expectedMergedAuAgreements(auAgreements5,
	auAgreements), stateManager.getAuAgreements(AUID_5));

    // Verify that the current poll agreements of the first AU have not been
    // affected.
    assertAuAgreementsMatch(auAgreements1,
	stateManager.getAuAgreements(AUID_1));

    // Verify that the current poll agreements of the second AU have not been
    // affected.
    assertAuAgreementsMatch(auAgreements2,
	stateManager.getAuAgreements(AUID_2));

    patchAuAgreementsCommonTest();

    log.debug2("Done");
  }

  /**
   * Runs the patchAuAgreements()-related authenticated-specific tests.
   */
  private void patchAuAgreementsAuthenticatedTest() throws Exception {
    log.debug2("Invoked");

    // No AUId.
    runTestPatchAuAgreements(null, null, null, null, HttpStatus.UNAUTHORIZED);

    runTestPatchAuAgreements(null, null, MediaType.APPLICATION_JSON, ANYBODY,
	HttpStatus.UNAUTHORIZED);

    // Spring reports it cannot find a match to an endpoint.
    runTestPatchAuAgreements(null, null, MediaType.APPLICATION_JSON,
	CONTENT_ADMIN, HttpStatus.NOT_FOUND);

    // No AUId using the REST service client.
    runTestPatchAuAgreementsClient(null, null, null, null,
	HttpStatus.UNAUTHORIZED);

    // Empty AUId.
    runTestPatchAuAgreements(EMPTY_STRING, null, null, ANYBODY,
	HttpStatus.UNAUTHORIZED);

    runTestPatchAuAgreements(EMPTY_STRING, null, MediaType.APPLICATION_JSON,
	ANYBODY, HttpStatus.UNAUTHORIZED);

    // Spring reports it cannot find a match to an endpoint.
    runTestPatchAuAgreements(EMPTY_STRING, null, MediaType.APPLICATION_JSON,
	CONTENT_ADMIN, HttpStatus.NOT_FOUND);

    // Empty AUId using the REST service client.
    runTestPatchAuAgreementsClient(EMPTY_STRING, null, ANYBODY, EMPTY_STRING,
	HttpStatus.UNAUTHORIZED);

    // Bad AUId.
    runTestPatchAuAgreements(BAD_AUID, null, MediaType.APPLICATION_JSON,
	null, HttpStatus.UNAUTHORIZED);

    runTestPatchAuAgreements(BAD_AUID, null, MediaType.APPLICATION_JSON,
	CONTENT_ADMIN, HttpStatus.BAD_REQUEST);

    // Bad AUId using the REST service client.
    runTestPatchAuAgreementsClient(BAD_AUID, null, ANYBODY, COOKIE_1,
	HttpStatus.UNAUTHORIZED);

    runTestPatchAuAgreementsClient(BAD_AUID, null, CONTENT_ADMIN, COOKIE_2,
	HttpStatus.BAD_REQUEST);

    // No AU poll agreements.
    runTestPatchAuAgreements(AUID_1, null, null, null, HttpStatus.UNAUTHORIZED);

    runTestPatchAuAgreements(AUID_1, null, MediaType.APPLICATION_JSON,
	ANYBODY, HttpStatus.UNAUTHORIZED);

    runTestPatchAuAgreements(AUID_1, null, MediaType.APPLICATION_JSON,
	CONTENT_ADMIN, HttpStatus.BAD_REQUEST);

    runTestPatchAuAgreements(AUID_1, EMPTY_STRING, null, null,
	HttpStatus.UNAUTHORIZED);

    runTestPatchAuAgreements(AUID_1, EMPTY_STRING,
	MediaType.APPLICATION_JSON, ANYBODY, HttpStatus.UNAUTHORIZED);

    runTestPatchAuAgreements(AUID_1, EMPTY_STRING,
	MediaType.APPLICATION_JSON, CONTENT_ADMIN, HttpStatus.BAD_REQUEST);

    // No AU poll agreements using the REST service client.
    runTestPatchAuAgreementsClient(AUID_1, null, null, null,
	HttpStatus.UNAUTHORIZED);

    runTestPatchAuAgreementsClient(AUID_1, null, CONTENT_ADMIN, EMPTY_STRING,
	HttpStatus.BAD_REQUEST);

    runTestPatchAuAgreementsClient(AUID_1, EMPTY_STRING, ANYBODY, COOKIE_1,
	HttpStatus.UNAUTHORIZED);

    runTestPatchAuAgreementsClient(AUID_1, EMPTY_STRING, CONTENT_ADMIN,
	COOKIE_2, HttpStatus.BAD_REQUEST);

    AuAgreements auAgreements = createAuAgreements(AUID_1,
	goodPeerIdentityIdStringList1, AgreementType.SYMMETRIC_POR_HINT,
	0.125f);

    // No Content-Type header.
    runTestPatchAuAgreements(AUID_1, toJsonWithBadPids(auAgreements), null,
	null, HttpStatus.UNAUTHORIZED);

    // Bad peer agreement ID.
    runTestPatchAuAgreements(AUID_1, toJsonWithBadPids(auAgreements),
	MediaType.APPLICATION_JSON, ANYBODY, HttpStatus.UNAUTHORIZED);

    runTestPatchAuAgreements(AUID_1, toJsonWithBadPids(auAgreements),
	MediaType.APPLICATION_JSON, CONTENT_ADMIN, HttpStatus.FORBIDDEN);

    // Bad peer agreement ID using the REST service client.
    runTestPatchAuAgreementsClient(AUID_1, toJsonWithBadPids(auAgreements),
	null, null, HttpStatus.UNAUTHORIZED);

    runTestPatchAuAgreementsClient(AUID_1, toJsonWithBadPids(auAgreements),
	CONTENT_ADMIN, EMPTY_STRING, HttpStatus.FORBIDDEN);

    // Patch first AU.
    runTestPatchAuAgreements(AUID_1, auAgreements.toJson(),
	MediaType.APPLICATION_JSON, null, HttpStatus.UNAUTHORIZED);

    // Patch first AU using the REST service client.
    runTestPatchAuAgreementsClient(AUID_1, auAgreements.toJson(), ANYBODY,
	COOKIE_1, HttpStatus.UNAUTHORIZED);

    // Patch second AU.
    runTestPatchAuAgreements(AUID_2, auAgreements.toJson(),
	MediaType.APPLICATION_JSON, CONTENT_ADMIN, HttpStatus.FORBIDDEN);

    // Patch second AU using the REST service client.
    runTestPatchAuAgreementsClient(AUID_2, auAgreements.toJson(),
	CONTENT_ADMIN, COOKIE_2, HttpStatus.FORBIDDEN);

    patchAuAgreementsCommonTest();

    log.debug2("Done");
  }

  /**
   * Runs the patchAuAgreements()-related authentication-independent tests.
   */
  private void patchAuAgreementsCommonTest() throws Exception {
    log.debug2("Invoked");

    StateManager stateManager =
	LockssDaemon.getLockssDaemon().getManagerByType(StateManager.class);

    // No AUId: Spring reports it cannot find a match to an endpoint.
    runTestPatchAuAgreements(null, null, null, AU_ADMIN, HttpStatus.NOT_FOUND);

    runTestPatchAuAgreements(null, null, MediaType.APPLICATION_JSON, USER_ADMIN,
	HttpStatus.NOT_FOUND);

    // No AUId using the REST service client.
    runTestPatchAuAgreementsClient(null, null, AU_ADMIN, null,
	HttpStatus.NOT_FOUND);

    // Empty AUId: Spring reports it cannot find a match to an endpoint.
    runTestPatchAuAgreements(EMPTY_STRING, null, null, USER_ADMIN,
	HttpStatus.NOT_FOUND);

    runTestPatchAuAgreements(EMPTY_STRING, null, MediaType.APPLICATION_JSON,
	AU_ADMIN, HttpStatus.NOT_FOUND);

    // Empty AUId using the REST service client.
    runTestPatchAuAgreementsClient(EMPTY_STRING, null, USER_ADMIN, EMPTY_STRING,
	HttpStatus.NOT_FOUND);

    // Bad AUId.
    runTestPatchAuAgreements(BAD_AUID, null, null, AU_ADMIN,
	HttpStatus.UNSUPPORTED_MEDIA_TYPE);

    runTestPatchAuAgreements(BAD_AUID, null, MediaType.APPLICATION_JSON,
	USER_ADMIN, HttpStatus.BAD_REQUEST);

    // Bad AUId using the REST service client.
    runTestPatchAuAgreementsClient(BAD_AUID, null, AU_ADMIN, COOKIE_1,
	HttpStatus.BAD_REQUEST);

    // No AU poll agreements.
    runTestPatchAuAgreements(AUID_1, null, null, USER_ADMIN,
	HttpStatus.UNSUPPORTED_MEDIA_TYPE);

    runTestPatchAuAgreements(AUID_1, null, MediaType.APPLICATION_JSON,
	AU_ADMIN, HttpStatus.BAD_REQUEST);

    runTestPatchAuAgreements(AUID_1, EMPTY_STRING, null, AU_ADMIN,
	HttpStatus.UNSUPPORTED_MEDIA_TYPE);

    runTestPatchAuAgreements(AUID_1, EMPTY_STRING,
	MediaType.APPLICATION_JSON, USER_ADMIN, HttpStatus.BAD_REQUEST);

    // No AU poll agreements using the REST service client.
    runTestPatchAuAgreementsClient(AUID_1, null, USER_ADMIN, COOKIE_2,
	HttpStatus.BAD_REQUEST);

    runTestPatchAuAgreementsClient(AUID_1, EMPTY_STRING, AU_ADMIN, null,
	HttpStatus.BAD_REQUEST);

    AuAgreements auAgreements = createAuAgreements(AUID_1,
	goodPeerIdentityIdStringList1, AgreementType.SYMMETRIC_POP_HINT,
	0.9375f);

    // No Content-Type header.
    runTestPatchAuAgreements(AUID_1, toJsonWithBadPids(auAgreements), null,
	USER_ADMIN, HttpStatus.UNSUPPORTED_MEDIA_TYPE);

    // Bad peer agreement ID.
    runTestPatchAuAgreements(AUID_1, toJsonWithBadPids(auAgreements),
	MediaType.APPLICATION_JSON, AU_ADMIN, HttpStatus.BAD_REQUEST);

    // Bad peer agreement ID using the REST service client.
    runTestPatchAuAgreementsClient(AUID_1, toJsonWithBadPids(auAgreements),
	USER_ADMIN, EMPTY_STRING, HttpStatus.BAD_REQUEST);

    // Get the current poll agreements of the second AU.
    AuAgreements auAgreements2 = AuAgreements.fromJson(AUID_2,
	runTestGetAuAgreements(AUID_2, USER_ADMIN, HttpStatus.OK), daemon);

    // Verify.
    assertAuAgreementsMatch(stateManager.getAuAgreements(AUID_2),
	auAgreements2);

    // Patch first AU using the REST service client.
    auAgreements = createAuAgreements(AUID_1, goodPeerIdentityIdStringList1,
	AgreementType.SYMMETRIC_POR_HINT, 0.875f);

    runTestPatchAuAgreementsClient(AUID_1, auAgreements.toJson(), AU_ADMIN,
	COOKIE_1, HttpStatus.OK);

    // Verify.
    assertAuAgreementsMatch(auAgreements, stateManager.getAuAgreements(AUID_1));
    AuAgreements auAgreements1 = auAgreements;

    // Verify that the current poll agreements of the second AU have not been
    // affected.
    assertAuAgreementsMatch(auAgreements2,
	stateManager.getAuAgreements(AUID_2));

    // Patch second AU.
    auAgreements = createAuAgreements(AUID_2, goodPeerIdentityIdStringList2,
	AgreementType.POP_HINT, 0.75f);

    runTestPatchAuAgreements(AUID_2, auAgreements.toJson(),
	MediaType.APPLICATION_JSON, USER_ADMIN, HttpStatus.OK);

    // Verify.
    assertAuAgreementsMatch(auAgreements, stateManager.getAuAgreements(AUID_2));
    auAgreements2 = auAgreements;

    // Verify that the current poll agreements of the first AU have not been
    // affected.
    assertAuAgreementsMatch(auAgreements1,
	stateManager.getAuAgreements(AUID_1));

    // Patch sixth AU using the REST service client.
    auAgreements = createAuAgreements(AUID_6, goodPeerIdentityIdStringList1,
	AgreementType.POR_HINT, 0.625f);

    runTestPatchAuAgreementsClient(AUID_6, auAgreements.toJson(), USER_ADMIN,
	COOKIE_2, HttpStatus.OK);

    // Verify.
    assertAuAgreementsMatch(auAgreements, stateManager.getAuAgreements(AUID_6));
    AuAgreements auAgreements6 = auAgreements;

    // Patch sixth AU again with a subset of agreements for the same peers using
    // the REST service client.
    auAgreements = createAuAgreements(AUID_6, goodPeerIdentityIdStringList1s,
	AgreementType.POP, 0.5f);

    runTestPatchAuAgreementsClient(AUID_6, auAgreements.toJson(), AU_ADMIN,
	null, HttpStatus.OK);

    // Verify.
    assertAuAgreementsMatch(expectedMergedAuAgreements(auAgreements6,
	auAgreements), stateManager.getAuAgreements(AUID_6));

    // Patch seventh AU.
    auAgreements = createAuAgreements(AUID_7, goodPeerIdentityIdStringList1,
	AgreementType.SYMMETRIC_POP, 0.875f);

    runTestPatchAuAgreements(AUID_7, auAgreements.toJson(),
	MediaType.APPLICATION_JSON, USER_ADMIN, HttpStatus.OK);

    // Verify.
    assertAuAgreementsMatch(auAgreements, stateManager.getAuAgreements(AUID_7));
    AuAgreements auAgreements7 = auAgreements;

    // Patch seventh AU again with agreements for other peers.
    auAgreements = createAuAgreements(AUID_7,
	goodPeerIdentityIdStringList2, AgreementType.SYMMETRIC_POR_HINT, 0.75f);

    runTestPatchAuAgreements(AUID_7, auAgreements.toJson(),
	MediaType.APPLICATION_JSON, USER_ADMIN, HttpStatus.OK);

    // Verify.
    assertAuAgreementsMatch(expectedMergedAuAgreements(auAgreements7,
	auAgreements), stateManager.getAuAgreements(AUID_7));

    // Verify that the current poll agreements of the first AU have not been
    // affected.
    assertAuAgreementsMatch(auAgreements1,
	stateManager.getAuAgreements(AUID_1));

    // Verify that the current poll agreements of the second AU have not been
    // affected.
    assertAuAgreementsMatch(auAgreements2,
	stateManager.getAuAgreements(AUID_2));

    log.debug2("Done");
  }

  /**
   * Performs a PATCH operation.
   * 
   * @param auId
   *          A String with the Archival Unit identifier.
   * @param auAgreements
   *          A String with the parts of the Archival Unit poll agreements to be
   *          replaced.
   * @param contentType
   *          A MediaType with the content type of the request.
   * @param credentials
   *          A Credentials with the request credentials.
   * @param expectedStatus
   *          An HttpStatus with the HTTP status of the result.
   */
  private void runTestPatchAuAgreements(String auId, String auAgreements,
      MediaType contentType, Credentials credentials, HttpStatus expectedStatus)
  {
    log.debug2("auId = {}", auId);
    log.debug2("auAgreements = {}", auAgreements);
    log.debug2("contentType = {}", contentType);
    log.debug2("credentials = {}", credentials);
    log.debug2("expectedStatus = {}", expectedStatus);

    // Get the test URL template.
    String template = getTestUrlTemplate("/auagreements/{auid}");

    // Create the URI of the request to the REST service.
    UriComponents uriComponents = UriComponentsBuilder.fromUriString(template)
	.build().expand(Collections.singletonMap("auid", auId));

    URI uri = UriComponentsBuilder.newInstance().uriComponents(uriComponents)
	.build().encode().toUri();
    log.trace("uri = {}", uri);

    // Initialize the request to the REST service.
    RestTemplate restTemplate = new RestTemplate();

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
      requestEntity = new HttpEntity<String>(auAgreements, headers);
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
   * @param auAgreements
   *          A String with the Archival Unit poll agreements.
   * @param credentials
   *          A Credentials with the request credentials.
   * @param xLockssRequestCookie
   *          A String with the request cookie.
   * @param expectedStatus
   *          An HttpStatus with the HTTP status of the result.
   */
  private void runTestPatchAuAgreementsClient(String auId, String auAgreements,
      Credentials credentials, String xLockssRequestCookie,
      HttpStatus expectedStatus) {
    log.debug2("auId = {}", auId);
    log.debug2("auAgreements = {}", auAgreements);
    log.debug2("credentials = {}", credentials);
    log.debug2("xLockssRequestCookie = {}", xLockssRequestCookie);
    log.debug2("expectedStatus = {}", expectedStatus);

    try {
      // Make the request.
      getRestConfigClient(credentials).patchArchivalUnitAgreements(auId,
	  auAgreements, xLockssRequestCookie);

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
   * Creates poll agreements for an Archival Unit.
   * 
   * @param auId
   *          A String with the Archival Unit identifier.
   * @param peerIdentityIdStringList
   *          {@code A Collection<String>} with peer identities identifiers.
   * @param agreementType
   *          An AgreementType with the type agreement.
   * @param agreementPercent
   *          A float with the agreement percent.
   * @return An AuAgreements with the Archival Unit poll agreements.
   */
  private AuAgreements createAuAgreements(String auId,
      Collection<String> peerIdentityIdStringList, AgreementType agreementType,
      float agreementPercent) throws Exception {
    log.debug2("auId = {}", auId);
    log.debug2("peerIdentityIdStringList = {}", peerIdentityIdStringList);
    log.debug2("agreementType = {}", agreementType);
    log.debug2("agreementPercent = {}", agreementPercent);

    // Create the agreements.
    AuAgreements auAgreements = AuAgreements.make(auId, idMgr);

    // Populate the agreements details.
    for (String peerIdentityIdString : peerIdentityIdStringList) {
      auAgreements.signalPartialAgreement(
	  idMgr.findPeerIdentity(peerIdentityIdString),
	  agreementType, agreementPercent, TimeBase.nowMs());
    }

    log.debug2("auAgreements = {}", auAgreements);
    return auAgreements;
  }

  /**
   * Verifies that two AuAgreements objects match in their PeerAgreements.
   * 
   * @param aua1
   *          An AuAgreements with the first object to be matched.
   * @param aua2
   *          An AuAgreements with the second object to be matched.
   */
  private void assertAuAgreementsMatch(AuAgreements aua1, AuAgreements aua2) {
    assertEquals(aua1.getAllPeerAgreements(), aua2.getAllPeerAgreements());
  }

  /**
   * Provides the result of merging partial poll agreements into other poll
   * agreements.
   * 
   * @param original
   *          An AuAgreements where to merge the partial other.
   * @param partial
   *          An AuAgreements with the poll agreements to be merged.
   * @return an AuAgreements with the resulting merged poll agreements.
   */
  private AuAgreements expectedMergedAuAgreements(AuAgreements original,
      AuAgreements partial) throws Exception {
    log.debug2("original = {}", original);
    log.debug2("partial = {}", partial);

    // Get the Archival Unit ID.
    String auId = original.getAuid();
    log.trace("auId = {}", auId);

    // Create an empty AuAgreements
    AuAgreements auAgreements = AuAgreements.make(auId, idMgr);
    // Merge original in first, then partial
    auAgreements.updateFromJson(original.toJson(), daemon);
    auAgreements.updateFromJson(partial.toJson(), daemon);
    return auAgreements;
  }

  /**
   * Returns a json string representing the AuAgreements, but with
   * malformed PeerIdentity s.
   * @param aua An AuAgreements to serialize
   * @return json string with all PeerIdentity strings replaced with a
   * malformed one
   */
  String toJsonWithBadPids(AuAgreements aua) throws Exception {
    String json = aua.toJson();
    json = StringUtil.replaceString(json, "TCP:[", "NOPROTO:[");
    return json;
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
