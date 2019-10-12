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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.lockss.app.LockssDaemon;
import org.lockss.config.RestConfigClient;
import org.lockss.log.L4JLogger;
import org.lockss.protocol.DatedPeerIdSet;
import org.lockss.protocol.DatedPeerIdSetImpl;
import org.lockss.protocol.IdentityManager;
import org.lockss.protocol.MockPeerIdentity;
import org.lockss.protocol.PeerIdentity;
import org.lockss.util.rest.RestUtil;
import org.lockss.util.rest.exception.LockssRestException;
import org.lockss.util.rest.exception.LockssRestHttpException;
import org.lockss.state.StateManager;
import org.lockss.test.MockLockssDaemon;
import org.lockss.test.SpringLockssTestCase;
import org.lockss.util.ListUtil;
import org.lockss.util.StringUtil;
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
 * Test class for org.lockss.laaws.config.api.NoaupeersApiServiceImpl.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class TestNoaupeersApiServiceImpl extends SpringLockssTestCase {
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
    setUpTempDirectory(TestNoaupeersApiServiceImpl.class.getCanonicalName());

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
    getNoAuPeersUnAuthenticatedTest();
    putNoAuPeersUnAuthenticatedTest();

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
    getNoAuPeersAuthenticatedTest();
    putNoAuPeersAuthenticatedTest();

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
    runTestMethodNotAllowed(EMPTY_STRING, ANYBODY, HttpMethod.PATCH,
	HttpStatus.NOT_FOUND);

    // Bad AUId.
    runTestMethodNotAllowed(BAD_AUID, ANYBODY, HttpMethod.POST,
	HttpStatus.METHOD_NOT_ALLOWED);

    runTestMethodNotAllowed(BAD_AUID, null, HttpMethod.PATCH,
	HttpStatus.METHOD_NOT_ALLOWED);

    // Good AUId.
    runTestMethodNotAllowed(AUID_1, null, HttpMethod.PATCH,
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
    runTestMethodNotAllowed(EMPTY_STRING, null, HttpMethod.PATCH,
	HttpStatus.UNAUTHORIZED);

    // Bad AUId.
    runTestMethodNotAllowed(BAD_AUID, ANYBODY, HttpMethod.POST,
	HttpStatus.UNAUTHORIZED);

    // No credentials.
    runTestMethodNotAllowed(AUID_1, null, HttpMethod.PATCH,
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
    runTestMethodNotAllowed(EMPTY_STRING, AU_ADMIN, HttpMethod.PATCH,
	HttpStatus.NOT_FOUND);

    // Bad AUId.
    runTestMethodNotAllowed(BAD_AUID, USER_ADMIN, HttpMethod.PATCH,
	HttpStatus.METHOD_NOT_ALLOWED);

    runTestMethodNotAllowed(BAD_AUID, AU_ADMIN, HttpMethod.POST,
	HttpStatus.METHOD_NOT_ALLOWED);

    // Good AUId.
    runTestMethodNotAllowed(AUID_1, AU_ADMIN, HttpMethod.PATCH,
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
    String template = getTestUrlTemplate("/noaupeers/{auid}");

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
   * Runs the getNoAuPeers()-related un-authenticated-specific tests.
   */
  private void getNoAuPeersUnAuthenticatedTest() throws Exception {
    log.debug2("Invoked");

    StateManager stateManager =
	LockssDaemon.getLockssDaemon().getManagerByType(StateManager.class);

    // No AUId: Spring reports it cannot find a match to an endpoint.
    runTestGetNoAuPeers(null, null, HttpStatus.NOT_FOUND);

    // No AUId using the REST service client.
    assertNull(runTestGetNoAuPeersClient(null, null, HttpStatus.NOT_FOUND));

    // Empty AUId: Spring reports it cannot find a match to an endpoint.
    runTestGetNoAuPeers(EMPTY_STRING, ANYBODY, HttpStatus.NOT_FOUND);

    // Empty AUId using the REST service client.
    assertNull(runTestGetNoAuPeersClient(EMPTY_STRING, ANYBODY,
	HttpStatus.NOT_FOUND));

    // Bad AUId.
    runTestGetNoAuPeers(BAD_AUID, null, HttpStatus.BAD_REQUEST);

    // No credentials.
    String result = runTestGetNoAuPeers(AUID_1, null, HttpStatus.OK);

    // Verify.
    assertEquals(stateManager.getNoAuPeerSet(AUID_1).toJson(), result);

    // No credentials using the REST service client.
    assertEquals(result,
	runTestGetNoAuPeersClient(AUID_1, null, HttpStatus.OK));

    // Bad credentials.
    result = runTestGetNoAuPeers(AUID_2, ANYBODY, HttpStatus.OK);

    // Verify.
    assertEquals(stateManager.getNoAuPeerSet(AUID_2).toJson(), result);

    // Bad credentials using the REST service client.
    assertEquals(result,
	runTestGetNoAuPeersClient(AUID_2, ANYBODY, HttpStatus.OK));

    getNoAuPeerSetCommonTest();

    log.debug2("Done");
  }

  /**
   * Runs the getNoAuPeerSet()-related authenticated-specific tests.
   */
  private void getNoAuPeersAuthenticatedTest() throws Exception {
    log.debug2("Invoked");

    // No AUId.
    runTestGetNoAuPeers(null, ANYBODY, HttpStatus.UNAUTHORIZED);

    // No AUId using the REST service client.
    assertNull(runTestGetNoAuPeersClient(null, null, HttpStatus.UNAUTHORIZED));

    // Empty AUId.
    runTestGetNoAuPeers(EMPTY_STRING, null, HttpStatus.UNAUTHORIZED);

    // Empty AUId using the REST service client.
    assertNull(runTestGetNoAuPeersClient(EMPTY_STRING, null,
	HttpStatus.UNAUTHORIZED));

    // Bad AUId.
    runTestGetNoAuPeers(BAD_AUID, null, HttpStatus.UNAUTHORIZED);

    // Bad AUId using the REST service client.
    assertNull(runTestGetNoAuPeersClient(BAD_AUID, null,
	HttpStatus.UNAUTHORIZED));

    // No credentials.
    runTestGetNoAuPeers(AUID_1, null, HttpStatus.UNAUTHORIZED);

    // No credentials using the REST service client.
    assertNull(runTestGetNoAuPeersClient(AUID_1, null,
	HttpStatus.UNAUTHORIZED));

    // Bad credentials.
    runTestGetNoAuPeers(AUID_2, ANYBODY, HttpStatus.UNAUTHORIZED);

    // Bad credentials using the REST service client.
    assertNull(runTestGetNoAuPeersClient(AUID_2, ANYBODY,
	HttpStatus.UNAUTHORIZED));

    getNoAuPeerSetCommonTest();

    log.debug2("Done");
  }

  /**
   * Runs the getNoAuPeerSet()-related authentication-independent tests.
   */
  private void getNoAuPeerSetCommonTest() throws Exception {
    log.debug2("Invoked");

    StateManager stateManager =
	LockssDaemon.getLockssDaemon().getManagerByType(StateManager.class);

    // No AUId: Spring reports it cannot find a match to an endpoint.
    runTestGetNoAuPeers(null, USER_ADMIN, HttpStatus.NOT_FOUND);

    // No AUId using the REST service client.
    assertNull(runTestGetNoAuPeersClient(null, USER_ADMIN,
	HttpStatus.NOT_FOUND));

    // Empty AUId: Spring reports it cannot find a match to an endpoint.
    runTestGetNoAuPeers(EMPTY_STRING, AU_ADMIN, HttpStatus.NOT_FOUND);

    // Empty AUId using the REST service client.
    assertNull(runTestGetNoAuPeersClient(EMPTY_STRING, AU_ADMIN,
	HttpStatus.NOT_FOUND));

    // Bad AUId.
    runTestGetNoAuPeers(BAD_AUID, USER_ADMIN, HttpStatus.BAD_REQUEST);

    // Bad AUId using the REST service client.
    assertNull(runTestGetNoAuPeersClient(BAD_AUID, USER_ADMIN,
	HttpStatus.BAD_REQUEST));

    // Good AUId.
    String result = runTestGetNoAuPeers(AUID_1, AU_ADMIN, HttpStatus.OK);

    // Verify
    assertEquals(stateManager.getNoAuPeerSet(AUID_1).toJson(), result);

    // Good AUId using the REST service client.
    assertEquals(result,
	runTestGetNoAuPeersClient(AUID_1, AU_ADMIN, HttpStatus.OK));

    // Good AUId.
    result = runTestGetNoAuPeers(AUID_2, USER_ADMIN, HttpStatus.OK);

    // Verify
    assertEquals(stateManager.getNoAuPeerSet(AUID_2).toJson(), result);

    // Good AUId using the REST service client.
    assertEquals(result,
	runTestGetNoAuPeersClient(AUID_2, USER_ADMIN, HttpStatus.OK));

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
   * @return a String with the Archival Unit NoAuPeerSet object.
   */
  private String runTestGetNoAuPeers(String auId, Credentials credentials,
      HttpStatus expectedStatus) {
    log.debug2("auId = {}", auId);
    log.debug2("credentials = {}", credentials);
    log.debug2("expectedStatus = {}", expectedStatus);

    // Get the test URL template.
    String template = getTestUrlTemplate("/noaupeers/{auid}");

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
   * @return a String with the Archival Unit NoAuPeerSet object.
   */
  private String runTestGetNoAuPeersClient(String auId,
      Credentials credentials, HttpStatus expectedStatus) {
    log.debug2("auId = {}", auId);
    log.debug2("credentials = {}", credentials);
    log.debug2("expectedStatus = {}", expectedStatus);

    String result = null;

    try {
      // Make the request and get the result.
      result = getRestConfigClient(credentials).getNoAuPeers(auId);
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
   * Runs the putNoAuPeers()-related un-authenticated-specific tests.
   */
  private void putNoAuPeersUnAuthenticatedTest() throws Exception {
    log.debug2("Invoked");

    StateManager stateManager =
	LockssDaemon.getLockssDaemon().getManagerByType(StateManager.class);

    // No AUId: Spring reports it cannot find a match to an endpoint.
    runTestPutNoAuPeers(null, null, null, null, HttpStatus.NOT_FOUND);

    runTestPutNoAuPeers(null, null, MediaType.APPLICATION_JSON, ANYBODY,
	HttpStatus.NOT_FOUND);

    // No AUId using the REST service client.
    runTestPutNoAuPeersClient(null, null, null, null, HttpStatus.NOT_FOUND);

    // Empty AUId: Spring reports it cannot find a match to an endpoint.
    runTestPutNoAuPeers(EMPTY_STRING, null, null, CONTENT_ADMIN,
	HttpStatus.NOT_FOUND);

    runTestPutNoAuPeers(EMPTY_STRING, null, MediaType.APPLICATION_JSON,
	null, HttpStatus.NOT_FOUND);

    // Empty AUId using the REST service client.
    runTestPutNoAuPeersClient(EMPTY_STRING, null, ANYBODY, EMPTY_STRING,
	HttpStatus.NOT_FOUND);

    // Bad AUId.
    runTestPutNoAuPeers(BAD_AUID, null, null, ANYBODY,
	HttpStatus.UNSUPPORTED_MEDIA_TYPE);

    runTestPutNoAuPeers(BAD_AUID, null, MediaType.APPLICATION_JSON,
	CONTENT_ADMIN, HttpStatus.BAD_REQUEST);

    // Bad AUId using the REST service client.
    runTestPutNoAuPeersClient(BAD_AUID, null, CONTENT_ADMIN, COOKIE_1,
	HttpStatus.BAD_REQUEST);

    // No AU NoAuPeerSet object.
    runTestPutNoAuPeers(AUID_1, null, null, null,
	HttpStatus.UNSUPPORTED_MEDIA_TYPE);

    runTestPutNoAuPeers(AUID_1, null, MediaType.APPLICATION_JSON, ANYBODY,
	HttpStatus.BAD_REQUEST);

    runTestPutNoAuPeers(AUID_1, EMPTY_STRING, null, CONTENT_ADMIN,
	HttpStatus.UNSUPPORTED_MEDIA_TYPE);

    runTestPutNoAuPeers(AUID_1, EMPTY_STRING, MediaType.APPLICATION_JSON,
	null, HttpStatus.BAD_REQUEST);

    // No AU NoAuPeerSet object using the REST service client.
    runTestPutNoAuPeersClient(AUID_1, null, null, COOKIE_2,
	HttpStatus.BAD_REQUEST);

    runTestPutNoAuPeersClient(AUID_1, EMPTY_STRING, ANYBODY, null,
	HttpStatus.BAD_REQUEST);

    DatedPeerIdSetImpl noAuPeers =
	createNoAuPeers(AUID_1, goodPeerIdentityIdStringList1);

    // No Content-Type header.
    runTestPutNoAuPeers(AUID_1, toJsonWithBadPids(noAuPeers), null, ANYBODY,
	HttpStatus.UNSUPPORTED_MEDIA_TYPE);

    // Bad peer agreement ID.
    runTestPutNoAuPeers(AUID_1, toJsonWithBadPids(noAuPeers),
	MediaType.APPLICATION_JSON, CONTENT_ADMIN, HttpStatus.BAD_REQUEST);

    // Bad peer agreement ID using the REST service client.
    runTestPutNoAuPeersClient(AUID_1, toJsonWithBadPids(noAuPeers),
	CONTENT_ADMIN, EMPTY_STRING, HttpStatus.BAD_REQUEST);

    // Get the current NoAuPeerSet object of the second AU.
    DatedPeerIdSet noAuPeers2 = stateManager.getNoAuPeerSet(AUID_2);

    assertNoAuPeersMatch((DatedPeerIdSetImpl)noAuPeers2,
	DatedPeerIdSetImpl.fromJson(AUID_2, runTestGetNoAuPeers(AUID_2, ANYBODY,
	    HttpStatus.OK), daemon));

    // Update first AU.
    noAuPeers = createNoAuPeers(AUID_1, goodPeerIdentityIdStringList1);

    runTestPutNoAuPeers(AUID_1, noAuPeers.toJson(), MediaType.APPLICATION_JSON,
	null, HttpStatus.OK);

    // Verify.
    assertNoAuPeersMatch(noAuPeers,
	(DatedPeerIdSetImpl)(stateManager.getNoAuPeerSet(AUID_1)));
    DatedPeerIdSetImpl noAuPeers1 = noAuPeers;

    // Verify that the current NoAuPeerSet object of the second AU have not been
    // affected.
    assertNoAuPeersMatch((DatedPeerIdSetImpl)noAuPeers2,
	(DatedPeerIdSetImpl)(stateManager.getNoAuPeerSet(AUID_2)));

    // Update second AU using the REST service client.
    noAuPeers = createNoAuPeers(AUID_2, goodPeerIdentityIdStringList2);

    runTestPutNoAuPeersClient(AUID_2, noAuPeers.toJson(), ANYBODY, COOKIE_1,
	HttpStatus.OK);

    // Verify.
    assertNoAuPeersMatch(noAuPeers,
	(DatedPeerIdSetImpl)(stateManager.getNoAuPeerSet(AUID_2)));
    noAuPeers2 = noAuPeers;

    // Verify that the current NoAuPeerSet object of the first AU have not been
    // affected.
    assertNoAuPeersMatch(noAuPeers1,
	(DatedPeerIdSetImpl)(stateManager.getNoAuPeerSet(AUID_1)));

    // Update third AU.
    noAuPeers = createNoAuPeers(AUID_3, goodPeerIdentityIdStringList1);

    runTestPutNoAuPeers(AUID_3, noAuPeers.toJson(), MediaType.APPLICATION_JSON,
	CONTENT_ADMIN, HttpStatus.OK);

    // Verify.
    assertNoAuPeersMatch(noAuPeers,
	(DatedPeerIdSetImpl)(stateManager.getNoAuPeerSet(AUID_3)));

    // Update fourth AU using the REST service client.
    noAuPeers = createNoAuPeers(AUID_4, goodPeerIdentityIdStringList1);

    runTestPutNoAuPeersClient(AUID_4, noAuPeers.toJson(), ANYBODY, COOKIE_2,
	HttpStatus.OK);

    // Verify.
    assertNoAuPeersMatch(noAuPeers,
	(DatedPeerIdSetImpl)(stateManager.getNoAuPeerSet(AUID_4)));

    // Update fourth AU again with a subset of the same peers using the REST
    // service client.
    noAuPeers = createNoAuPeers(AUID_4, goodPeerIdentityIdStringList1s);

    runTestPutNoAuPeersClient(AUID_4, noAuPeers.toJson(), CONTENT_ADMIN, null,
	HttpStatus.OK);

    // Verify.
    assertNoAuPeersMatch(noAuPeers,
	(DatedPeerIdSetImpl)(stateManager.getNoAuPeerSet(AUID_4)));

    // Update fifth AU.
    noAuPeers = createNoAuPeers(AUID_5, goodPeerIdentityIdStringList1);

    runTestPutNoAuPeers(AUID_5, noAuPeers.toJson(), MediaType.APPLICATION_JSON,
	null, HttpStatus.OK);

    // Verify.
    assertNoAuPeersMatch(noAuPeers,
	(DatedPeerIdSetImpl)(stateManager.getNoAuPeerSet(AUID_5)));

    // Update fifth AU again with other peers.
    noAuPeers = createNoAuPeers(AUID_5, goodPeerIdentityIdStringList2);

    runTestPutNoAuPeers(AUID_5, noAuPeers.toJson(), MediaType.APPLICATION_JSON,
	CONTENT_ADMIN, HttpStatus.OK);

    // Verify.
    assertNoAuPeersMatch(noAuPeers,
	(DatedPeerIdSetImpl)(stateManager.getNoAuPeerSet(AUID_5)));

    // Verify that the current NoAuPeerSet object of the first AU have not been
    // affected.
    assertNoAuPeersMatch(noAuPeers1,
	(DatedPeerIdSetImpl)(stateManager.getNoAuPeerSet(AUID_1)));

    // Verify that the current NoAuPeerSet object of the second AU have not been
    // affected.
    assertNoAuPeersMatch((DatedPeerIdSetImpl)noAuPeers2,
	(DatedPeerIdSetImpl)(stateManager.getNoAuPeerSet(AUID_2)));

    putNoAuPeersCommonTest();

    log.debug2("Done");
  }

  /**
   * Runs the putNoAuPeers()-related authenticated-specific tests.
   */
  private void putNoAuPeersAuthenticatedTest() throws Exception {
    log.debug2("Invoked");

    // No AUId.
    runTestPutNoAuPeers(null, null, null, null, HttpStatus.UNAUTHORIZED);

    runTestPutNoAuPeers(null, null, MediaType.APPLICATION_JSON, ANYBODY,
	HttpStatus.UNAUTHORIZED);

    // Spring reports it cannot find a match to an endpoint.
    runTestPutNoAuPeers(null, null, MediaType.APPLICATION_JSON, CONTENT_ADMIN,
	HttpStatus.NOT_FOUND);

    // No AUId using the REST service client.
    runTestPutNoAuPeersClient(null, null, null, null, HttpStatus.UNAUTHORIZED);

    // Empty AUId.
    runTestPutNoAuPeers(EMPTY_STRING, null, null, ANYBODY,
	HttpStatus.UNAUTHORIZED);

    runTestPutNoAuPeers(EMPTY_STRING, null, MediaType.APPLICATION_JSON, ANYBODY,
	HttpStatus.UNAUTHORIZED);

    // Spring reports it cannot find a match to an endpoint.
    runTestPutNoAuPeers(EMPTY_STRING, null, MediaType.APPLICATION_JSON,
	CONTENT_ADMIN, HttpStatus.NOT_FOUND);

    // Empty AUId using the REST service client.
    runTestPutNoAuPeersClient(EMPTY_STRING, null, ANYBODY, EMPTY_STRING,
	HttpStatus.UNAUTHORIZED);

    // Bad AUId.
    runTestPutNoAuPeers(BAD_AUID, null, MediaType.APPLICATION_JSON, null,
	HttpStatus.UNAUTHORIZED);

    runTestPutNoAuPeers(BAD_AUID, null, MediaType.APPLICATION_JSON,
	CONTENT_ADMIN, HttpStatus.BAD_REQUEST);

    // Bad AUId using the REST service client.
    runTestPutNoAuPeersClient(BAD_AUID, null, ANYBODY, COOKIE_1,
	HttpStatus.UNAUTHORIZED);

    runTestPutNoAuPeersClient(BAD_AUID, null, CONTENT_ADMIN, COOKIE_2,
	HttpStatus.BAD_REQUEST);

    // No AU NoAuPeerSet object.
    runTestPutNoAuPeers(AUID_1, null, null, null, HttpStatus.UNAUTHORIZED);

    runTestPutNoAuPeers(AUID_1, null, MediaType.APPLICATION_JSON, ANYBODY,
	HttpStatus.UNAUTHORIZED);

    runTestPutNoAuPeers(AUID_1, null, MediaType.APPLICATION_JSON, CONTENT_ADMIN,
	HttpStatus.BAD_REQUEST);

    runTestPutNoAuPeers(AUID_1, EMPTY_STRING, null, null,
	HttpStatus.UNAUTHORIZED);

    runTestPutNoAuPeers(AUID_1, EMPTY_STRING, MediaType.APPLICATION_JSON,
	ANYBODY, HttpStatus.UNAUTHORIZED);

    runTestPutNoAuPeers(AUID_1, EMPTY_STRING, MediaType.APPLICATION_JSON,
	CONTENT_ADMIN, HttpStatus.BAD_REQUEST);

    // No AU NoAuPeerSet object using the REST service client.
    runTestPutNoAuPeersClient(AUID_1, null, null, null,
	HttpStatus.UNAUTHORIZED);

    runTestPutNoAuPeersClient(AUID_1, null, CONTENT_ADMIN, EMPTY_STRING,
	HttpStatus.BAD_REQUEST);

    runTestPutNoAuPeersClient(AUID_1, EMPTY_STRING, ANYBODY, COOKIE_1,
	HttpStatus.UNAUTHORIZED);

    runTestPutNoAuPeersClient(AUID_1, EMPTY_STRING, CONTENT_ADMIN, COOKIE_2,
	HttpStatus.BAD_REQUEST);

    DatedPeerIdSetImpl noAuPeers =
	createNoAuPeers(AUID_1, goodPeerIdentityIdStringList1);

    // No Content-Type header.
    runTestPutNoAuPeers(AUID_1, toJsonWithBadPids(noAuPeers), null, null,
	HttpStatus.UNAUTHORIZED);

    // Bad peer agreement ID.
    runTestPutNoAuPeers(AUID_1, toJsonWithBadPids(noAuPeers),
	MediaType.APPLICATION_JSON, ANYBODY, HttpStatus.UNAUTHORIZED);

    runTestPutNoAuPeers(AUID_1, toJsonWithBadPids(noAuPeers),
	MediaType.APPLICATION_JSON, CONTENT_ADMIN, HttpStatus.FORBIDDEN);

    // Bad peer agreement ID using the REST service client.
    runTestPutNoAuPeersClient(AUID_1, toJsonWithBadPids(noAuPeers), null, null,
	HttpStatus.UNAUTHORIZED);

    runTestPutNoAuPeersClient(AUID_1, toJsonWithBadPids(noAuPeers),
	CONTENT_ADMIN, EMPTY_STRING, HttpStatus.FORBIDDEN);

    // Update first AU.
    runTestPutNoAuPeers(AUID_1, noAuPeers.toJson(), MediaType.APPLICATION_JSON,
	null, HttpStatus.UNAUTHORIZED);

    // Update first AU using the REST service client.
    runTestPutNoAuPeersClient(AUID_1, noAuPeers.toJson(), ANYBODY, COOKIE_1,
	HttpStatus.UNAUTHORIZED);

    // Update second AU.
    runTestPutNoAuPeers(AUID_2, noAuPeers.toJson(), MediaType.APPLICATION_JSON,
	CONTENT_ADMIN, HttpStatus.FORBIDDEN);

    // Update second AU using the REST service client.
    runTestPutNoAuPeersClient(AUID_2, noAuPeers.toJson(), CONTENT_ADMIN,
	COOKIE_2, HttpStatus.FORBIDDEN);

    putNoAuPeersCommonTest();

    log.debug2("Done");
  }

  /**
   * Runs the putNoAuPeers()-related authentication-independent tests.
   */
  private void putNoAuPeersCommonTest() throws Exception {
    log.debug2("Invoked");

    StateManager stateManager =
	LockssDaemon.getLockssDaemon().getManagerByType(StateManager.class);

    // No AUId: Spring reports it cannot find a match to an endpoint.
    runTestPutNoAuPeers(null, null, null, AU_ADMIN, HttpStatus.NOT_FOUND);

    runTestPutNoAuPeers(null, null, MediaType.APPLICATION_JSON, USER_ADMIN,
	HttpStatus.NOT_FOUND);

    // No AUId using the REST service client.
    runTestPutNoAuPeersClient(null, null, AU_ADMIN, null, HttpStatus.NOT_FOUND);

    // Empty AUId: Spring reports it cannot find a match to an endpoint.
    runTestPutNoAuPeers(EMPTY_STRING, null, null, USER_ADMIN,
	HttpStatus.NOT_FOUND);

    runTestPutNoAuPeers(EMPTY_STRING, null, MediaType.APPLICATION_JSON,
	AU_ADMIN, HttpStatus.NOT_FOUND);

    // Empty AUId using the REST service client.
    runTestPutNoAuPeersClient(EMPTY_STRING, null, USER_ADMIN, EMPTY_STRING,
	HttpStatus.NOT_FOUND);

    // Bad AUId.
    runTestPutNoAuPeers(BAD_AUID, null, null, AU_ADMIN,
	HttpStatus.UNSUPPORTED_MEDIA_TYPE);

    runTestPutNoAuPeers(BAD_AUID, null, MediaType.APPLICATION_JSON, USER_ADMIN,
	HttpStatus.BAD_REQUEST);

    // Bad AUId using the REST service client.
    runTestPutNoAuPeersClient(BAD_AUID, null, AU_ADMIN, COOKIE_1,
	HttpStatus.BAD_REQUEST);

    // No AU NoAuPeerSet object.
    runTestPutNoAuPeers(AUID_1, null, null, USER_ADMIN,
	HttpStatus.UNSUPPORTED_MEDIA_TYPE);

    runTestPutNoAuPeers(AUID_1, null, MediaType.APPLICATION_JSON, AU_ADMIN,
	HttpStatus.BAD_REQUEST);

    runTestPutNoAuPeers(AUID_1, EMPTY_STRING, null, AU_ADMIN,
	HttpStatus.UNSUPPORTED_MEDIA_TYPE);

    runTestPutNoAuPeers(AUID_1, EMPTY_STRING, MediaType.APPLICATION_JSON,
	USER_ADMIN, HttpStatus.BAD_REQUEST);

    // No AU NoAuPeerSet object using the REST service client.
    runTestPutNoAuPeersClient(AUID_1, null, USER_ADMIN, COOKIE_2,
	HttpStatus.BAD_REQUEST);

    runTestPutNoAuPeersClient(AUID_1, EMPTY_STRING, AU_ADMIN, null,
	HttpStatus.BAD_REQUEST);

    DatedPeerIdSetImpl noAuPeers =
	createNoAuPeers(AUID_1, goodPeerIdentityIdStringList1);

    // No Content-Type header.
    runTestPutNoAuPeers(AUID_1, toJsonWithBadPids(noAuPeers), null, USER_ADMIN,
	HttpStatus.UNSUPPORTED_MEDIA_TYPE);

    // Bad peer agreement ID.
    runTestPutNoAuPeers(AUID_1, toJsonWithBadPids(noAuPeers),
	MediaType.APPLICATION_JSON, AU_ADMIN, HttpStatus.BAD_REQUEST);

    // Bad peer agreement ID using the REST service client.
    runTestPutNoAuPeersClient(AUID_1, toJsonWithBadPids(noAuPeers), USER_ADMIN,
	EMPTY_STRING, HttpStatus.BAD_REQUEST);

    // Get the current NoAuPeerSet object of the second AU.
    DatedPeerIdSetImpl noAuPeers2 = DatedPeerIdSetImpl.fromJson(AUID_2,
	runTestGetNoAuPeers(AUID_2, USER_ADMIN, HttpStatus.OK), daemon);

    // Verify.
    assertNoAuPeersMatch(
	(DatedPeerIdSetImpl)(stateManager.getNoAuPeerSet(AUID_2)), noAuPeers2);

    // Update first AU using the REST service client.
    noAuPeers = createNoAuPeers(AUID_1, goodPeerIdentityIdStringList1);

    runTestPutNoAuPeersClient(AUID_1, noAuPeers.toJson(), AU_ADMIN, COOKIE_1,
	HttpStatus.OK);

    // Verify.
    assertNoAuPeersMatch(noAuPeers,
	(DatedPeerIdSetImpl)(stateManager.getNoAuPeerSet(AUID_1)));
    DatedPeerIdSetImpl noAuPeers1 = noAuPeers;

    // Verify that the current NoAuPeerSet object of the second AU have not been
    // affected.
    assertNoAuPeersMatch(noAuPeers2,
	(DatedPeerIdSetImpl)(stateManager.getNoAuPeerSet(AUID_2)));

    // Update second AU.
    noAuPeers = createNoAuPeers(AUID_2, goodPeerIdentityIdStringList2);

    runTestPutNoAuPeers(AUID_2, noAuPeers.toJson(), MediaType.APPLICATION_JSON,
	USER_ADMIN, HttpStatus.OK);

    // Verify.
    assertNoAuPeersMatch(noAuPeers,
	(DatedPeerIdSetImpl)(stateManager.getNoAuPeerSet(AUID_2)));
    noAuPeers2 = noAuPeers;

    // Verify that the current NoAuPeerSet object of the first AU have not been
    // affected.
    assertNoAuPeersMatch(noAuPeers1,
	(DatedPeerIdSetImpl)(stateManager.getNoAuPeerSet(AUID_1)));

    // Update sixth AU using the REST service client.
    noAuPeers = createNoAuPeers(AUID_6, goodPeerIdentityIdStringList1);

    runTestPutNoAuPeersClient(AUID_6, noAuPeers.toJson(), USER_ADMIN, COOKIE_2,
	HttpStatus.OK);

    // Verify.
    assertNoAuPeersMatch(noAuPeers,
	(DatedPeerIdSetImpl)(stateManager.getNoAuPeerSet(AUID_6)));

    // Update sixth AU again with a subset of the same peers using the REST
    // service client.
    noAuPeers = createNoAuPeers(AUID_6, goodPeerIdentityIdStringList1s);

    runTestPutNoAuPeersClient(AUID_6, noAuPeers.toJson(), AU_ADMIN, null,
	HttpStatus.OK);

    // Verify.
    assertNoAuPeersMatch(noAuPeers,
	(DatedPeerIdSetImpl)(stateManager.getNoAuPeerSet(AUID_6)));

    // Update seventh AU.
    noAuPeers = createNoAuPeers(AUID_7, goodPeerIdentityIdStringList1);

    runTestPutNoAuPeers(AUID_7, noAuPeers.toJson(), MediaType.APPLICATION_JSON,
	USER_ADMIN, HttpStatus.OK);

    // Verify.
    assertNoAuPeersMatch(noAuPeers,
	(DatedPeerIdSetImpl)(stateManager.getNoAuPeerSet(AUID_7)));

    // Update seventh AU again with other peers.
    noAuPeers = createNoAuPeers(AUID_7, goodPeerIdentityIdStringList2);

    runTestPutNoAuPeers(AUID_7, noAuPeers.toJson(), MediaType.APPLICATION_JSON,
	USER_ADMIN, HttpStatus.OK);

    // Verify.
    assertNoAuPeersMatch(noAuPeers,
	(DatedPeerIdSetImpl)(stateManager.getNoAuPeerSet(AUID_7)));

    // Verify that the current NoAuPeerSet object of the first AU have not been
    // affected.
    assertNoAuPeersMatch(noAuPeers1,
	(DatedPeerIdSetImpl)(stateManager.getNoAuPeerSet(AUID_1)));

    // Verify that the current NoAuPeerSet object of the second AU have not been
    // affected.
    assertNoAuPeersMatch(noAuPeers2,
	(DatedPeerIdSetImpl)(stateManager.getNoAuPeerSet(AUID_2)));

    log.debug2("Done");
  }

  /**
   * Performs a PUT operation.
   * 
   * @param auId
   *          A String with the Archival Unit identifier.
   * @param noAuPeers
   *          A String with the Archival Unit NoAuPeerSet object to be saved.
   * @param contentType
   *          A MediaType with the content type of the request.
   * @param credentials
   *          A Credentials with the request credentials.
   * @param expectedStatus
   *          An HttpStatus with the HTTP status of the result.
   */
  private void runTestPutNoAuPeers(String auId, String noAuPeers,
      MediaType contentType, Credentials credentials, HttpStatus expectedStatus)
  {
    log.debug2("auId = {}", auId);
    log.debug2("noAuPeers = {}", noAuPeers);
    log.debug2("contentType = {}", contentType);
    log.debug2("credentials = {}", credentials);
    log.debug2("expectedStatus = {}", expectedStatus);

    // Get the test URL template.
    String template = getTestUrlTemplate("/noaupeers/{auid}");

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
      requestEntity = new HttpEntity<String>(noAuPeers, headers);
    }

    // Make the request and get the response. 
    ResponseEntity<String> response = new TestRestTemplate(restTemplate)
	.exchange(uri, HttpMethod.PUT, requestEntity, String.class);

    // Get the response status.
    HttpStatus statusCode = response.getStatusCode();
    assertEquals(expectedStatus, statusCode);
  }

  /**
   * Performs a PUT operation for an Archival Unit using the REST service
   * client.
   * 
   * @param auId
   *          A String with the Archival Unit identifier.
   * @param noAuPeers
   *          A String with the Archival Unit NoAuPeerSet object to be saved.
   * @param credentials
   *          A Credentials with the request credentials.
   * @param xLockssRequestCookie
   *          A String with the request cookie.
   * @param expectedStatus
   *          An HttpStatus with the HTTP status of the result.
   */
  private void runTestPutNoAuPeersClient(String auId, String noAuPeers,
      Credentials credentials, String xLockssRequestCookie,
      HttpStatus expectedStatus) {
    log.debug2("auId = {}", auId);
    log.debug2("noAuPeers = {}", noAuPeers);
    log.debug2("credentials = {}", credentials);
    log.debug2("xLockssRequestCookie = {}", xLockssRequestCookie);
    log.debug2("expectedStatus = {}", expectedStatus);

    try {
      // Make the request.
      getRestConfigClient(credentials).putNoAuPeers(auId, noAuPeers,
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
   * Creates a NoAuPeerSet object for an Archival Unit.
   * 
   * @param auId
   *          A String with the Archival Unit identifier.
   * @param peerIdentityIdStringList
   *          {@code A Collection<String>} with peer identities identifiers.
   * @return A DatedPeerIdSetImpl with the Archival Unit NoAuPeerSet object.
   */
  private DatedPeerIdSetImpl createNoAuPeers(String auId,
      Collection<String> peerIdentityIdStringList) throws Exception {
    log.debug2("auId = {}", auId);
    log.debug2("peerIdentityIdStringList = {}", peerIdentityIdStringList);

    // Create the NoAuPeerSet object.
    DatedPeerIdSetImpl dpis1 = DatedPeerIdSetImpl.make(auId, idMgr);

    // Populate the NoAuPeerSet object details.
    for (String peerIdentityIdString : peerIdentityIdStringList) {
      dpis1.add(new MockPeerIdentity(peerIdentityIdString));
    }

    log.debug2("dpis1 = {}", dpis1);
    return dpis1;
  }

  /**
   * Verifies that two DatedPeerIdSetImpl objects match.
   * 
   * @param dpis1
   *          A DatedPeerIdSetImpl with the first object to be matched.
   * @param dpis2
   *          A DatedPeerIdSetImpl with the second object to be matched.
   */
  private void assertNoAuPeersMatch(DatedPeerIdSetImpl dpis1,
      DatedPeerIdSetImpl dpis2) {
    // Verify that they involve the same AU.
    assertEquals(dpis1.getAuid(), dpis2.getAuid());

    // Verify that they involve the same peer identities, possibly in a
    // different order.
    Set<String> pis1 = new HashSet<>();
    Iterator<PeerIdentity> iter1 = dpis1.iterator();
    while (iter1.hasNext()) {
      pis1.add(iter1.next().getIdString());
    }

    Set<String> pis2 = new HashSet<>();
    Iterator<PeerIdentity> iter2 = dpis2.iterator();
    while (iter2.hasNext()) {
      pis2.add(iter2.next().getIdString());
    }

    assertEquals(pis1.size(), pis2.size());
    assertTrue(pis2.containsAll(pis1));

    // Verify that they involve the same date.
    assertEquals(dpis1.getDate(), dpis2.getDate());
  }

  /**
   * Returns a JSON string representing the DatedPeerIdSetImpl, but with
   * malformed PeerIdentities.
   * 
   * @param dpis
   *          A DatedPeerIdSetImpl to serialize.
   * @return a String with the JSON string with all the PeerIdentity strings
   *         replaced with a malformed one.
   */
  String toJsonWithBadPids(DatedPeerIdSetImpl dpis) throws Exception {
    String json = dpis.toJson();
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
