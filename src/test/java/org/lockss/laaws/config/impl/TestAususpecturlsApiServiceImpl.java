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
import java.util.Collection;
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
import org.lockss.state.AuSuspectUrlVersions;
import org.lockss.state.StateManager;
import org.lockss.state.AuSuspectUrlVersions.SuspectUrlVersion;
import org.lockss.test.MockLockssDaemon;
import org.lockss.spring.test.SpringLockssTestCase4;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.server.LocalServerPort;
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
 * Test class for org.lockss.laaws.config.api.AususpecturlsApiServiceImpl.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class TestAususpecturlsApiServiceImpl extends SpringLockssTestCase4 {
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

  // Suspect URLs.
  private static String URL1 = "http://host.tld/path/to/file1.txt";
  private static String URL2 = "http://host.tld/path/to/file2.txt";
  private static String URL3 = "http://host.tld/path/to/file3.txt";
  private static String URL4 = "http://host.tld/path/to/file4.txt";
  private static String URL5 = "http://host.tld/path/to/file5.txt";
  private static String URL6 = "http://host.tld/path/to/file6.txt";

  // Suspect URL version sets.
  private Collection<SuspectUrlVersion> suvset1 = null;
  private Collection<SuspectUrlVersion> suvset1s = null;
  private Collection<SuspectUrlVersion> suvset2 = null;

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

  /**
   * Set up code to be run before each test.
   * 
   * @throws IOException if there are problems.
   */
  @Before
  public void setUpBeforeEachTest() throws IOException {
    log.debug2("port = {}", port);

    // Set up the temporary directory where the test data will reside.
    setUpTempDirectory(
	TestAususpecturlsApiServiceImpl.class.getCanonicalName());

    // Set up the UI port.
    setUpUiPort(UI_PORT_CONFIGURATION_TEMPLATE, UI_PORT_CONFIGURATION_FILE);

    daemon = getMockLockssDaemon();

    // Create the suspect URL version sets.
    AuSuspectUrlVersions asuv = AuSuspectUrlVersions.make(AUID_1);
    asuv.markAsSuspect(URL1, 1);
    asuv.markAsSuspect(URL2, 1);
    asuv.markAsSuspect(URL3, 1);
    asuv.markAsSuspect(URL3, 2);
    suvset1 = asuv.getSuspectList();

    asuv = AuSuspectUrlVersions.make(AUID_1);
    asuv.markAsSuspect(URL1, 1);
    asuv.markAsSuspect(URL3, 3);
    suvset1s = asuv.getSuspectList();

    asuv = AuSuspectUrlVersions.make(AUID_1);
    asuv.markAsSuspect(URL4, 4);
    asuv.markAsSuspect(URL5, 5);
    asuv.markAsSuspect(URL6, 6);
    suvset2 = asuv.getSuspectList();

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
    getAuSuspectUrlVersionsUnAuthenticatedTest();
    putAuSuspectUrlVersionsUnAuthenticatedTest();

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
    getAuSuspectUrlVersionsAuthenticatedTest();
    putAuSuspectUrlVersionsAuthenticatedTest();

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
    String template = getTestUrlTemplate("/aususpecturls/{auid}");

    // Create the URI of the request to the REST service.
    UriComponents uriComponents = UriComponentsBuilder.fromUriString(template)
	.build().expand(Collections.singletonMap("auid", auId));

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
    ResponseEntity<String> response = new TestRestTemplate(templateBuilder)
	.exchange(uri, method, requestEntity, String.class);

    // Get the response status.
    HttpStatus statusCode = response.getStatusCode();
    assertFalse(RestUtil.isSuccess(statusCode));
    assertEquals(expectedStatus, statusCode);
  }

  /**
   * Runs the getAuSuspectUrlVersions()-related un-authenticated-specific tests.
   */
  private void getAuSuspectUrlVersionsUnAuthenticatedTest() throws Exception {
    log.debug2("Invoked");

    StateManager stateManager =
	LockssDaemon.getLockssDaemon().getManagerByType(StateManager.class);

    // No AUId: Spring reports it cannot find a match to an endpoint.
    runTestGetAuSuspectUrlVersions(null, null, HttpStatus.NOT_FOUND);

    // No AUId using the REST service client.
    assertNull(runTestGetAuSuspectUrlVersionsClient(null, null,
	HttpStatus.NOT_FOUND));

    // Empty AUId: Spring reports it cannot find a match to an endpoint.
    runTestGetAuSuspectUrlVersions(EMPTY_STRING, ANYBODY, HttpStatus.NOT_FOUND);

    // Empty AUId using the REST service client.
    assertNull(runTestGetAuSuspectUrlVersionsClient(EMPTY_STRING, ANYBODY,
	HttpStatus.NOT_FOUND));

    // Bad AUId.
    runTestGetAuSuspectUrlVersions(BAD_AUID, null, HttpStatus.BAD_REQUEST);

    // No credentials.
    String result = runTestGetAuSuspectUrlVersions(AUID_1, null, HttpStatus.OK);

    // Verify.
    assertEquals(stateManager.getAuSuspectUrlVersions(AUID_1).toJson(), result);

    // No credentials using the REST service client.
    assertEquals(result,
	runTestGetAuSuspectUrlVersionsClient(AUID_1, null, HttpStatus.OK));

    // Bad credentials.
    result = runTestGetAuSuspectUrlVersions(AUID_2, ANYBODY, HttpStatus.OK);

    // Verify.
    assertEquals(stateManager.getAuSuspectUrlVersions(AUID_2).toJson(), result);

    // Bad credentials using the REST service client.
    assertEquals(result,
	runTestGetAuSuspectUrlVersionsClient(AUID_2, ANYBODY, HttpStatus.OK));

    getAuSuspectUrlVersionsCommonTest();

    log.debug2("Done");
  }

  /**
   * Runs the getAuSuspectUrlVersions()-related authenticated-specific tests.
   */
  private void getAuSuspectUrlVersionsAuthenticatedTest() throws Exception {
    log.debug2("Invoked");

    // No AUId.
    runTestGetAuSuspectUrlVersions(null, ANYBODY, HttpStatus.UNAUTHORIZED);

    // No AUId using the REST service client.
    assertNull(runTestGetAuSuspectUrlVersionsClient(null, null,
	HttpStatus.UNAUTHORIZED));

    // Empty AUId.
    runTestGetAuSuspectUrlVersions(EMPTY_STRING, null, HttpStatus.UNAUTHORIZED);

    // Empty AUId using the REST service client.
    assertNull(runTestGetAuSuspectUrlVersionsClient(EMPTY_STRING, null,
	HttpStatus.UNAUTHORIZED));

    // Bad AUId.
    runTestGetAuSuspectUrlVersions(BAD_AUID, null, HttpStatus.UNAUTHORIZED);

    // Bad AUId using the REST service client.
    assertNull(runTestGetAuSuspectUrlVersionsClient(BAD_AUID, null,
	HttpStatus.UNAUTHORIZED));

    // No credentials.
    runTestGetAuSuspectUrlVersions(AUID_1, null, HttpStatus.UNAUTHORIZED);

    // No credentials using the REST service client.
    assertNull(runTestGetAuSuspectUrlVersionsClient(AUID_1, null,
	HttpStatus.UNAUTHORIZED));

    // Bad credentials.
    runTestGetAuSuspectUrlVersions(AUID_2, ANYBODY, HttpStatus.UNAUTHORIZED);

    // Bad credentials using the REST service client.
    assertNull(runTestGetAuSuspectUrlVersionsClient(AUID_2, ANYBODY,
	HttpStatus.UNAUTHORIZED));

    getAuSuspectUrlVersionsCommonTest();

    log.debug2("Done");
  }

  /**
   * Runs the getAuSuspectUrlVersions()-related authentication-independent
   * tests.
   */
  private void getAuSuspectUrlVersionsCommonTest() throws Exception {
    log.debug2("Invoked");

    StateManager stateManager =
	LockssDaemon.getLockssDaemon().getManagerByType(StateManager.class);

    // No AUId: Spring reports it cannot find a match to an endpoint.
    runTestGetAuSuspectUrlVersions(null, USER_ADMIN, HttpStatus.NOT_FOUND);

    // No AUId using the REST service client.
    assertNull(runTestGetAuSuspectUrlVersionsClient(null, USER_ADMIN,
	HttpStatus.NOT_FOUND));

    // Empty AUId: Spring reports it cannot find a match to an endpoint.
    runTestGetAuSuspectUrlVersions(EMPTY_STRING, AU_ADMIN,
	HttpStatus.NOT_FOUND);

    // Empty AUId using the REST service client.
    assertNull(runTestGetAuSuspectUrlVersionsClient(EMPTY_STRING, AU_ADMIN,
	HttpStatus.NOT_FOUND));

    // Bad AUId.
    runTestGetAuSuspectUrlVersions(BAD_AUID, USER_ADMIN,
	HttpStatus.BAD_REQUEST);

    // Bad AUId using the REST service client.
    assertNull(runTestGetAuSuspectUrlVersionsClient(BAD_AUID, USER_ADMIN,
	HttpStatus.BAD_REQUEST));

    // Good AUId.
    String result =
	runTestGetAuSuspectUrlVersions(AUID_1, AU_ADMIN, HttpStatus.OK);

    // Verify
    assertEquals(stateManager.getAuSuspectUrlVersions(AUID_1).toJson(), result);

    // Good AUId using the REST service client.
    assertEquals(result,
	runTestGetAuSuspectUrlVersionsClient(AUID_1, AU_ADMIN, HttpStatus.OK));

    // Good AUId.
    result = runTestGetAuSuspectUrlVersions(AUID_2, USER_ADMIN, HttpStatus.OK);

    // Verify
    assertEquals(stateManager.getAuSuspectUrlVersions(AUID_2).toJson(), result);

    // Good AUId using the REST service client.
    assertEquals(result, runTestGetAuSuspectUrlVersionsClient(AUID_2,
	USER_ADMIN, HttpStatus.OK));

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
   * @return a String with the Archival Unit suspect URL versions.
   */
  private String runTestGetAuSuspectUrlVersions(String auId,
      Credentials credentials, HttpStatus expectedStatus) {
    log.debug2("auId = {}", auId);
    log.debug2("credentials = {}", credentials);
    log.debug2("expectedStatus = {}", expectedStatus);

    // Get the test URL template.
    String template = getTestUrlTemplate("/aususpecturls/{auid}");

    // Create the URI of the request to the REST service.
    UriComponents uriComponents = UriComponentsBuilder.fromUriString(template)
	.build().expand(Collections.singletonMap("auid", auId));

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
   * @return a String with the Archival Unit suspect URL versions.
   */
  private String runTestGetAuSuspectUrlVersionsClient(String auId,
      Credentials credentials, HttpStatus expectedStatus) {
    log.debug2("auId = {}", auId);
    log.debug2("credentials = {}", credentials);
    log.debug2("expectedStatus = {}", expectedStatus);

    String result = null;

    try {
      // Make the request and get the result.
      result = getRestConfigClient(credentials)
	  .getArchivalUnitSuspectUrlVersions(auId);
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
   * Runs the putAuSuspectUrlVersions()-related un-authenticated-specific tests.
   */
  private void putAuSuspectUrlVersionsUnAuthenticatedTest() throws Exception {
    log.debug2("Invoked");

    StateManager stateManager =
	LockssDaemon.getLockssDaemon().getManagerByType(StateManager.class);

    // No AUId: Spring reports it cannot find a match to an endpoint.
    runTestPutAuSuspectUrlVersions(null, null, null, null,
	HttpStatus.NOT_FOUND);

    runTestPutAuSuspectUrlVersions(null, null, MediaType.APPLICATION_JSON,
	ANYBODY, HttpStatus.NOT_FOUND);

    // No AUId using the REST service client.
    runTestPutAuSuspectUrlVersionsClient(null, null, null, null,
	HttpStatus.NOT_FOUND);

    // Empty AUId: Spring reports it cannot find a match to an endpoint.
    runTestPutAuSuspectUrlVersions(EMPTY_STRING, null, null, CONTENT_ADMIN,
	HttpStatus.NOT_FOUND);

    runTestPutAuSuspectUrlVersions(EMPTY_STRING, null,
	MediaType.APPLICATION_JSON, null, HttpStatus.NOT_FOUND);

    // Empty AUId using the REST service client.
    runTestPutAuSuspectUrlVersionsClient(EMPTY_STRING, null, ANYBODY,
	EMPTY_STRING, HttpStatus.NOT_FOUND);

    // Bad AUId.
    runTestPutAuSuspectUrlVersions(BAD_AUID, null, null, ANYBODY,
	HttpStatus.UNSUPPORTED_MEDIA_TYPE);

    runTestPutAuSuspectUrlVersions(BAD_AUID, null, MediaType.APPLICATION_JSON,
	CONTENT_ADMIN, HttpStatus.BAD_REQUEST);

    // Bad AUId using the REST service client.
    runTestPutAuSuspectUrlVersionsClient(BAD_AUID, null, CONTENT_ADMIN,
	COOKIE_1, HttpStatus.BAD_REQUEST);

    // No AU suspect URL versions.
    runTestPutAuSuspectUrlVersions(AUID_1, null, null, null,
	HttpStatus.UNSUPPORTED_MEDIA_TYPE);

    runTestPutAuSuspectUrlVersions(AUID_1, null, MediaType.APPLICATION_JSON,
	ANYBODY, HttpStatus.BAD_REQUEST);

    runTestPutAuSuspectUrlVersions(AUID_1, EMPTY_STRING, null, CONTENT_ADMIN,
	HttpStatus.UNSUPPORTED_MEDIA_TYPE);

    runTestPutAuSuspectUrlVersions(AUID_1, EMPTY_STRING,
	MediaType.APPLICATION_JSON, null, HttpStatus.BAD_REQUEST);

    // No AU suspect URL versions using the REST service client.
    runTestPutAuSuspectUrlVersionsClient(AUID_1, null, null, COOKIE_2,
	HttpStatus.BAD_REQUEST);

    runTestPutAuSuspectUrlVersionsClient(AUID_1, EMPTY_STRING, ANYBODY, null,
	HttpStatus.BAD_REQUEST);

    AuSuspectUrlVersions auSuspectUrlVersions =
	createAuSuspectUrlVersions(AUID_1, suvset1);

    // No Content-Type header.
    runTestPutAuSuspectUrlVersions(AUID_1, auSuspectUrlVersions.toJson(), null,
	ANYBODY, HttpStatus.UNSUPPORTED_MEDIA_TYPE);

    // Get the current suspect URL versions of the second AU.
    AuSuspectUrlVersions auSuspectUrlVersions2 =
	stateManager.getAuSuspectUrlVersions(AUID_2);

    assertEquals(auSuspectUrlVersions2,	AuSuspectUrlVersions.fromJson(AUID_2,
	runTestGetAuSuspectUrlVersions(AUID_2, ANYBODY, HttpStatus.OK),
	daemon));

    // Update first AU.
    runTestPutAuSuspectUrlVersions(AUID_1, auSuspectUrlVersions.toJson(),
	MediaType.APPLICATION_JSON, null, HttpStatus.OK);

    // Verify.
    assertEquals(auSuspectUrlVersions,
	stateManager.getAuSuspectUrlVersions(AUID_1));
    AuSuspectUrlVersions auSuspectUrlVersions1 = auSuspectUrlVersions;

    // Verify that the current suspect URL versions of the second AU have not
    // been affected.
    assertEquals(auSuspectUrlVersions2,
	stateManager.getAuSuspectUrlVersions(AUID_2));

    // Update second AU using the REST service client.
    auSuspectUrlVersions = createAuSuspectUrlVersions(AUID_2, suvset2);

    runTestPutAuSuspectUrlVersionsClient(AUID_2, auSuspectUrlVersions.toJson(),
	ANYBODY, COOKIE_1, HttpStatus.OK);

    // Verify.
    assertEquals(auSuspectUrlVersions,
	stateManager.getAuSuspectUrlVersions(AUID_2));
    auSuspectUrlVersions2 = auSuspectUrlVersions;

    // Verify that the current suspect URL versions of the first AU have not
    // been affected.
    assertEquals(auSuspectUrlVersions1,
	stateManager.getAuSuspectUrlVersions(AUID_1));

    // Update third AU.
    auSuspectUrlVersions = createAuSuspectUrlVersions(AUID_3, suvset1);

    runTestPutAuSuspectUrlVersions(AUID_3, auSuspectUrlVersions.toJson(),
	MediaType.APPLICATION_JSON, CONTENT_ADMIN, HttpStatus.OK);

    // Verify.
    assertEquals(auSuspectUrlVersions,
	stateManager.getAuSuspectUrlVersions(AUID_3));

    // Update fourth AU using the REST service client.
    auSuspectUrlVersions = createAuSuspectUrlVersions(AUID_4, suvset1);

    runTestPutAuSuspectUrlVersionsClient(AUID_4, auSuspectUrlVersions.toJson(),
	null, COOKIE_2, HttpStatus.OK);

    // Verify.
    assertEquals(auSuspectUrlVersions,
	stateManager.getAuSuspectUrlVersions(AUID_4));

    // Update fourth AU again with overlapping suspect URL versions using the
    // REST service client.
    auSuspectUrlVersions = createAuSuspectUrlVersions(AUID_4, suvset1s);

    runTestPutAuSuspectUrlVersionsClient(AUID_4, auSuspectUrlVersions.toJson(),
	CONTENT_ADMIN, null, HttpStatus.OK);

    // Verify.
    assertEquals(auSuspectUrlVersions,
	stateManager.getAuSuspectUrlVersions(AUID_4));

    // Update fifth AU.
    auSuspectUrlVersions = createAuSuspectUrlVersions(AUID_5, suvset1);

    runTestPutAuSuspectUrlVersions(AUID_5, auSuspectUrlVersions.toJson(),
	MediaType.APPLICATION_JSON, null, HttpStatus.OK);

    // Verify.
    assertEquals(auSuspectUrlVersions,
	stateManager.getAuSuspectUrlVersions(AUID_5));

    // Update fifth AU again with non-overlapping suspect URL versions.
    auSuspectUrlVersions = createAuSuspectUrlVersions(AUID_5, suvset2);

    runTestPutAuSuspectUrlVersions(AUID_5, auSuspectUrlVersions.toJson(),
	MediaType.APPLICATION_JSON, CONTENT_ADMIN, HttpStatus.OK);

    // Verify.
    assertEquals(auSuspectUrlVersions,
	stateManager.getAuSuspectUrlVersions(AUID_5));

    // Verify that the current suspect URL versions of the first AU have not
    // been affected.
    assertEquals(auSuspectUrlVersions1,
	stateManager.getAuSuspectUrlVersions(AUID_1));

    // Verify that the current suspect URL versions of the second AU have not
    // been affected.
    assertEquals(auSuspectUrlVersions2,
	stateManager.getAuSuspectUrlVersions(AUID_2));

    putAuSuspectUrlVersionsCommonTest();

    log.debug2("Done");
  }

  /**
   * Runs the putAuSuspectUrlVersions()-related authenticated-specific tests.
   */
  private void putAuSuspectUrlVersionsAuthenticatedTest() throws Exception {
    log.debug2("Invoked");

    // No AUId.
    runTestPutAuSuspectUrlVersions(null, null, null, null,
	HttpStatus.UNAUTHORIZED);

    runTestPutAuSuspectUrlVersions(null, null, MediaType.APPLICATION_JSON,
	ANYBODY, HttpStatus.UNAUTHORIZED);

    // Spring reports it cannot find a match to an endpoint.
    runTestPutAuSuspectUrlVersions(null, null, MediaType.APPLICATION_JSON,
	CONTENT_ADMIN, HttpStatus.NOT_FOUND);

    // No AUId using the REST service client.
    runTestPutAuSuspectUrlVersionsClient(null, null, null, null,
	HttpStatus.UNAUTHORIZED);

    // Empty AUId.
    runTestPutAuSuspectUrlVersions(EMPTY_STRING, null, null, ANYBODY,
	HttpStatus.UNAUTHORIZED);

    runTestPutAuSuspectUrlVersions(EMPTY_STRING, null,
	MediaType.APPLICATION_JSON, ANYBODY, HttpStatus.UNAUTHORIZED);

    // Spring reports it cannot find a match to an endpoint.
    runTestPutAuSuspectUrlVersions(EMPTY_STRING, null,
	MediaType.APPLICATION_JSON, CONTENT_ADMIN, HttpStatus.NOT_FOUND);

    // Empty AUId using the REST service client.
    runTestPutAuSuspectUrlVersionsClient(EMPTY_STRING, null, ANYBODY,
	EMPTY_STRING, HttpStatus.UNAUTHORIZED);

    // Bad AUId.
    runTestPutAuSuspectUrlVersions(BAD_AUID, null, MediaType.APPLICATION_JSON,
	null, HttpStatus.UNAUTHORIZED);

    runTestPutAuSuspectUrlVersions(BAD_AUID, null, MediaType.APPLICATION_JSON,
	CONTENT_ADMIN, HttpStatus.BAD_REQUEST);

    // Bad AUId using the REST service client.
    runTestPutAuSuspectUrlVersionsClient(BAD_AUID, null, ANYBODY, COOKIE_1,
	HttpStatus.UNAUTHORIZED);

    runTestPutAuSuspectUrlVersionsClient(BAD_AUID, null, CONTENT_ADMIN,
	COOKIE_2, HttpStatus.BAD_REQUEST);

    // No AU suspect URL versions.
    runTestPutAuSuspectUrlVersions(AUID_1, null, null, null,
	HttpStatus.UNAUTHORIZED);

    runTestPutAuSuspectUrlVersions(AUID_1, null, MediaType.APPLICATION_JSON,
	ANYBODY, HttpStatus.UNAUTHORIZED);

    runTestPutAuSuspectUrlVersions(AUID_1, null, MediaType.APPLICATION_JSON,
	CONTENT_ADMIN, HttpStatus.BAD_REQUEST);

    runTestPutAuSuspectUrlVersions(AUID_1, EMPTY_STRING, null, null,
	HttpStatus.UNAUTHORIZED);

    runTestPutAuSuspectUrlVersions(AUID_1, EMPTY_STRING,
	MediaType.APPLICATION_JSON, ANYBODY, HttpStatus.UNAUTHORIZED);

    runTestPutAuSuspectUrlVersions(AUID_1, EMPTY_STRING,
	MediaType.APPLICATION_JSON, CONTENT_ADMIN, HttpStatus.BAD_REQUEST);

    // No AU suspect URL versions using the REST service client.
    runTestPutAuSuspectUrlVersionsClient(AUID_1, null, null, null,
	HttpStatus.UNAUTHORIZED);

    runTestPutAuSuspectUrlVersionsClient(AUID_1, null, CONTENT_ADMIN,
	EMPTY_STRING, HttpStatus.BAD_REQUEST);

    runTestPutAuSuspectUrlVersionsClient(AUID_1, EMPTY_STRING, ANYBODY,
	COOKIE_1, HttpStatus.UNAUTHORIZED);

    runTestPutAuSuspectUrlVersionsClient(AUID_1, EMPTY_STRING, CONTENT_ADMIN,
	COOKIE_2, HttpStatus.BAD_REQUEST);

    AuSuspectUrlVersions auSuspectUrlVersions =
	createAuSuspectUrlVersions(AUID_1, suvset1);

    // No Content-Type header.
    runTestPutAuSuspectUrlVersions(AUID_1, auSuspectUrlVersions.toJson(), null,
	null, HttpStatus.UNAUTHORIZED);

    // Update first AU.
    runTestPutAuSuspectUrlVersions(AUID_1, auSuspectUrlVersions.toJson(),
	MediaType.APPLICATION_JSON, null, HttpStatus.UNAUTHORIZED);

    // Update first AU using the REST service client.
    runTestPutAuSuspectUrlVersionsClient(AUID_1, auSuspectUrlVersions.toJson(),
	ANYBODY, COOKIE_1, HttpStatus.UNAUTHORIZED);

    // Update second AU.
    runTestPutAuSuspectUrlVersions(AUID_2, auSuspectUrlVersions.toJson(),
	MediaType.APPLICATION_JSON, CONTENT_ADMIN, HttpStatus.FORBIDDEN);

    // Update second AU using the REST service client.
    runTestPutAuSuspectUrlVersionsClient(AUID_2, auSuspectUrlVersions.toJson(),
	CONTENT_ADMIN, COOKIE_2, HttpStatus.FORBIDDEN);

    putAuSuspectUrlVersionsCommonTest();

    log.debug2("Done");
  }

  /**
   * Runs the putAuSuspectUrlVersions()-related authentication-independent
   * tests.
   */
  private void putAuSuspectUrlVersionsCommonTest() throws Exception {
    log.debug2("Invoked");

    StateManager stateManager =
	LockssDaemon.getLockssDaemon().getManagerByType(StateManager.class);

    // No AUId: Spring reports it cannot find a match to an endpoint.
    runTestPutAuSuspectUrlVersions(null, null, null, AU_ADMIN,
	HttpStatus.NOT_FOUND);

    runTestPutAuSuspectUrlVersions(null, null, MediaType.APPLICATION_JSON,
	USER_ADMIN, HttpStatus.NOT_FOUND);

    // No AUId using the REST service client.
    runTestPutAuSuspectUrlVersionsClient(null, null, AU_ADMIN, null,
	HttpStatus.NOT_FOUND);

    // Empty AUId: Spring reports it cannot find a match to an endpoint.
    runTestPutAuSuspectUrlVersions(EMPTY_STRING, null, null, USER_ADMIN,
	HttpStatus.NOT_FOUND);

    runTestPutAuSuspectUrlVersions(EMPTY_STRING, null,
	MediaType.APPLICATION_JSON, AU_ADMIN, HttpStatus.NOT_FOUND);

    // Empty AUId using the REST service client.
    runTestPutAuSuspectUrlVersionsClient(EMPTY_STRING, null, USER_ADMIN,
	EMPTY_STRING, HttpStatus.NOT_FOUND);

    // Bad AUId.
    runTestPutAuSuspectUrlVersions(BAD_AUID, null, null, AU_ADMIN,
	HttpStatus.UNSUPPORTED_MEDIA_TYPE);

    runTestPutAuSuspectUrlVersions(BAD_AUID, null, MediaType.APPLICATION_JSON,
	USER_ADMIN, HttpStatus.BAD_REQUEST);

    // Bad AUId using the REST service client.
    runTestPutAuSuspectUrlVersionsClient(BAD_AUID, null, AU_ADMIN, COOKIE_1,
	HttpStatus.BAD_REQUEST);

    // No AU suspect URL versions.
    runTestPutAuSuspectUrlVersions(AUID_1, null, null, USER_ADMIN,
	HttpStatus.UNSUPPORTED_MEDIA_TYPE);

    runTestPutAuSuspectUrlVersions(AUID_1, null, MediaType.APPLICATION_JSON,
	AU_ADMIN, HttpStatus.BAD_REQUEST);

    runTestPutAuSuspectUrlVersions(AUID_1, EMPTY_STRING, null, AU_ADMIN,
	HttpStatus.UNSUPPORTED_MEDIA_TYPE);

    runTestPutAuSuspectUrlVersions(AUID_1, EMPTY_STRING,
	MediaType.APPLICATION_JSON, USER_ADMIN, HttpStatus.BAD_REQUEST);

    // No AU suspect URL versions using the REST service client.
    runTestPutAuSuspectUrlVersionsClient(AUID_1, null, USER_ADMIN, COOKIE_2,
	HttpStatus.BAD_REQUEST);

    runTestPutAuSuspectUrlVersionsClient(AUID_1, EMPTY_STRING, AU_ADMIN, null,
	HttpStatus.BAD_REQUEST);

    AuSuspectUrlVersions auSuspectUrlVersions =
	createAuSuspectUrlVersions(AUID_1, suvset1);

    // No Content-Type header.
    runTestPutAuSuspectUrlVersions(AUID_1, auSuspectUrlVersions.toJson(), null,
	USER_ADMIN, HttpStatus.UNSUPPORTED_MEDIA_TYPE);

    // Get the current suspect URL versions of the second AU.
    AuSuspectUrlVersions auSuspectUrlVersions2 =
	AuSuspectUrlVersions.fromJson(AUID_2, runTestGetAuSuspectUrlVersions(
	    AUID_2, USER_ADMIN, HttpStatus.OK), daemon);

    // Verify.
    assertEquals(stateManager.getAuSuspectUrlVersions(AUID_2),
	auSuspectUrlVersions2);

    // Update first AU using the REST service client.
    runTestPutAuSuspectUrlVersionsClient(AUID_1, auSuspectUrlVersions.toJson(),
	AU_ADMIN, COOKIE_1, HttpStatus.OK);

    // Verify.
    assertEquals(auSuspectUrlVersions,
	stateManager.getAuSuspectUrlVersions(AUID_1));
    AuSuspectUrlVersions auSuspectUrlVersions1 = auSuspectUrlVersions;

    // Verify that the current suspect URL versions of the second AU have not
    // been affected.
    assertEquals(auSuspectUrlVersions2,
	stateManager.getAuSuspectUrlVersions(AUID_2));

    // Update second AU.
    auSuspectUrlVersions = createAuSuspectUrlVersions(AUID_2, suvset2);

    runTestPutAuSuspectUrlVersions(AUID_2, auSuspectUrlVersions.toJson(),
	MediaType.APPLICATION_JSON, USER_ADMIN, HttpStatus.OK);

    // Verify.
    assertEquals(auSuspectUrlVersions,
	stateManager.getAuSuspectUrlVersions(AUID_2));
    auSuspectUrlVersions2 = auSuspectUrlVersions;

    // Verify that the current suspect URL versions of the first AU have not
    // been affected.
    assertEquals(auSuspectUrlVersions1,
	stateManager.getAuSuspectUrlVersions(AUID_1));

    // Update sixth AU using the REST service client.
    auSuspectUrlVersions = createAuSuspectUrlVersions(AUID_6, suvset1);

    runTestPutAuSuspectUrlVersionsClient(AUID_6, auSuspectUrlVersions.toJson(),
	USER_ADMIN, COOKIE_2, HttpStatus.OK);

    // Verify.
    assertEquals(auSuspectUrlVersions,
	stateManager.getAuSuspectUrlVersions(AUID_6));

    // Update sixth AU again with overlapping suspect URL versions using the
    // REST service client.
    auSuspectUrlVersions = createAuSuspectUrlVersions(AUID_6, suvset1s);

    runTestPutAuSuspectUrlVersionsClient(AUID_6, auSuspectUrlVersions.toJson(),
	AU_ADMIN, null, HttpStatus.OK);

    // Verify.
    assertEquals(auSuspectUrlVersions,
	stateManager.getAuSuspectUrlVersions(AUID_6));

    // Update seventh AU.
    auSuspectUrlVersions = createAuSuspectUrlVersions(AUID_7, suvset1);

    runTestPutAuSuspectUrlVersions(AUID_7, auSuspectUrlVersions.toJson(),
	MediaType.APPLICATION_JSON, USER_ADMIN, HttpStatus.OK);

    // Verify.
    assertEquals(auSuspectUrlVersions,
	stateManager.getAuSuspectUrlVersions(AUID_7));

    // Update seventh AU again with non-overlapping suspect URL versions.
    auSuspectUrlVersions = createAuSuspectUrlVersions(AUID_7, suvset2);

    runTestPutAuSuspectUrlVersions(AUID_7, auSuspectUrlVersions.toJson(),
	MediaType.APPLICATION_JSON, USER_ADMIN, HttpStatus.OK);

    // Verify.
    assertEquals(auSuspectUrlVersions,
	stateManager.getAuSuspectUrlVersions(AUID_7));

    // Verify that the current suspect URL versions of the first AU have not
    // been affected.
    assertEquals(auSuspectUrlVersions1,
	stateManager.getAuSuspectUrlVersions(AUID_1));

    // Verify that the current suspect URL versions of the second AU have not
    // been affected.
    assertEquals(auSuspectUrlVersions2,
	stateManager.getAuSuspectUrlVersions(AUID_2));

    log.debug2("Done");
  }

  /**
   * Performs a PUT operation.
   * 
   * @param auId
   *          A String with the Archival Unit identifier.
   * @param auSuspectUrlVersions
   *          A String with the Archival Unit suspect URL versions to be saved.
   * @param contentType
   *          A MediaType with the content type of the request.
   * @param credentials
   *          A Credentials with the request credentials.
   * @param expectedStatus
   *          An HttpStatus with the HTTP status of the result.
   */
  private void runTestPutAuSuspectUrlVersions(String auId,
      String auSuspectUrlVersions, MediaType contentType,
      Credentials credentials, HttpStatus expectedStatus) {
    log.debug2("auId = {}", auId);
    log.debug2("auSuspectUrlVersions = {}", auSuspectUrlVersions);
    log.debug2("contentType = {}", contentType);
    log.debug2("credentials = {}", credentials);
    log.debug2("expectedStatus = {}", expectedStatus);

    // Get the test URL template.
    String template = getTestUrlTemplate("/aususpecturls/{auid}");

    // Create the URI of the request to the REST service.
    UriComponents uriComponents = UriComponentsBuilder.fromUriString(template)
	.build().expand(Collections.singletonMap("auid", auId));

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
      requestEntity = new HttpEntity<String>(auSuspectUrlVersions, headers);
    }

    // Make the request and get the response. 
    ResponseEntity<String> response = new TestRestTemplate(templateBuilder)
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
   * @param auSuspectUrlVersions
   *          A String with the Archival Unit suspect URL versions to be saved.
   * @param credentials
   *          A Credentials with the request credentials.
   * @param xLockssRequestCookie
   *          A String with the request cookie.
   * @param expectedStatus
   *          An HttpStatus with the HTTP status of the result.
   */
  private void runTestPutAuSuspectUrlVersionsClient(String auId,
      String auSuspectUrlVersions, Credentials credentials,
      String xLockssRequestCookie, HttpStatus expectedStatus) {
    log.debug2("auId = {}", auId);
    log.debug2("auSuspectUrlVersions = {}", auSuspectUrlVersions);
    log.debug2("credentials = {}", credentials);
    log.debug2("xLockssRequestCookie = {}", xLockssRequestCookie);
    log.debug2("expectedStatus = {}", expectedStatus);

    try {
      // Make the request.
      getRestConfigClient(credentials).putArchivalUnitSuspectUrlVersions(auId,
	  auSuspectUrlVersions, xLockssRequestCookie);

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
   * Creates suspect URL versions for an Archival Unit.
   * 
   * @param auId
   *          A String with the Archival Unit identifier.
   * @param urls
   *          {@code A Collection<String>} with suspect URLs.
   * @param version
   *          An int with the suspect URL version.
   * @return An AuSuspectUrlVersions with the Archival Unit suspect URL
   *         versions.
   */
  private AuSuspectUrlVersions createAuSuspectUrlVersions(String auId,
      Collection<SuspectUrlVersion> suvs) throws Exception {
    log.debug2("auId = {}", auId);
    log.debug2("suvs = {}", suvs);

    // Create the suspect URL versions.
    AuSuspectUrlVersions auSuspectUrlVersions = AuSuspectUrlVersions.make(auId);

    // Populate the suspect URL versions details.
    for (SuspectUrlVersion suv : suvs) {
      auSuspectUrlVersions.markAsSuspect(suv.getUrl(), suv.getVersion());
    }

    log.debug2("auSuspectUrlVersions = {}", auSuspectUrlVersions);
    return auSuspectUrlVersions;
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
