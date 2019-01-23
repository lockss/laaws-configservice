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

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.lockss.app.LockssDaemon;
import org.lockss.log.L4JLogger;
import org.lockss.plugin.AuUtil;
import org.lockss.protocol.AgreementType;
import org.lockss.protocol.AuAgreements;
import org.lockss.protocol.MockIdentityManager;
import org.lockss.protocol.MockPeerIdentity;
import org.lockss.protocol.PeerAgreement;
import org.lockss.protocol.PeerIdentity;
import org.lockss.state.StateManager;
import org.lockss.test.SpringLockssTestCase;
import org.lockss.util.ListUtil;
import org.lockss.util.time.TimeBase;
import org.skyscreamer.jsonassert.JSONAssert;
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

  // The identifier of an AU that exists in the test system.
  private static final String GOOD_AUID_1 =
      "org|lockss|plugin|pensoft|oai|PensoftOaiPlugin"
      + "&au_oai_date~2014&au_oai_set~biorisk"
      + "&base_url~http%3A%2F%2Fbiorisk%2Epensoft%2Enet%2F";

  // The identifier of another AU that exists in the test system.
  private static final String GOOD_AUID_2 =
      "org|lockss|plugin|pensoft|oai|PensoftOaiPlugin"
      + "&au_oai_date~2015&au_oai_set~biorisk"
      + "&base_url~http%3A%2F%2Fbiorisk%2Epensoft%2Enet%2F";

  // The identifiers of AUs that do not exist in the test system.
  private static final String UNKNOWN_AUID_1 ="unknown1&auid1";
  private static final String UNKNOWN_AUID_2 ="unknown2&auid2";
  private static final String UNKNOWN_AUID_3 ="unknown3&auid3";
  private static final String UNKNOWN_AUID_4 ="unknown4&auid4";
  private static final String UNKNOWN_AUID_5 ="unknown5&auid5";
  private static final String UNKNOWN_AUID_6 ="unknown6&auid6";
  private static final String UNKNOWN_AUID_7 ="unknown7&auid7";
  private static final String UNKNOWN_AUID_8 ="unknown8&auid8";

  // Credentials.
  private final Credentials USER_ADMIN =
      this.new Credentials("lockss-u", "lockss-p");
  private final Credentials AU_ADMIN =
      this.new Credentials("au-admin", "I'mAuAdmin");
  private final Credentials CONTENT_ADMIN =
      this.new Credentials("content-admin", "I'mContentAdmin");
  private final Credentials ANYBODY =
      this.new Credentials("someUser", "somePassword");

  // Bad peer identity list.
  private List<MockPeerIdentity> badPeerIdentityList = ListUtil.list(
      new MockPeerIdentity("id0"),
      new MockPeerIdentity("id1"),
      new MockPeerIdentity("id2"));

  // Good peer identity list.
  private List<MockPeerIdentity> goodPeerIdentityList = ListUtil.list(
      new MockPeerIdentity("TCP:[1.2.3.4]:111"),
      new MockPeerIdentity("TCP:[2.3.4.5]:222"),
      new MockPeerIdentity("TCP:[3.4.5.6]:333"));

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
    setUpTempDirectory(TestAuagreementsApiServiceImpl.class.getCanonicalName());

    // Copy the necessary files to the test temporary directory.
    final File srcTree1 = new File(new File("test"), "cache");
    log.trace("srcTree1 = {}", () -> srcTree1.getAbsolutePath());

    copyToTempDir(srcTree1);

    final File srcTree2 = new File(new File("test"), "tdbxml");
    log.trace("srcTree2 = {}", () -> srcTree2.getAbsolutePath());

    copyToTempDir(srcTree2);

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

    getSwaggerDocsTest();
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

    getSwaggerDocsTest();
    getAuAgreementsAuthenticatedTest();
    patchAuAgreementsAuthenticatedTest();

    log.debug2("Done");
  }

  /**
   * Provides the standard command line arguments to start the server.
   * 
   * @return a List<String> with the command line arguments.
   */
  private List<String> getCommandLineArguments() {
    log.debug2("Invoked");

    List<String> cmdLineArgs = new ArrayList<String>();
    cmdLineArgs.add("-p");
    cmdLineArgs.add(getPlatformDiskSpaceConfigPath());
    cmdLineArgs.add("-p");
    cmdLineArgs.add("config/common.xml");

    File folder =
	new File(new File(new File(getTempDirPath()), "tdbxml"), "prod");
    log.trace("folder = {}", folder);

    cmdLineArgs.add("-x");
    cmdLineArgs.add(folder.getAbsolutePath());
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
   * Runs the Swagger-related tests.
   * 
   * @throws Exception
   *           if there are problems.
   */
  private void getSwaggerDocsTest() throws Exception {
    log.debug2("Invoked");

    ResponseEntity<String> successResponse = new TestRestTemplate().exchange(
	getTestUrlTemplate("/v2/api-docs"), HttpMethod.GET, null, String.class);

    HttpStatus statusCode = successResponse.getStatusCode();
    assertEquals(HttpStatus.OK, statusCode);

    String expectedBody = "{'swagger':'2.0',"
	+ "'info':{'description':'API of the LOCKSS Configuration REST Service'"
	+ "}}";

    JSONAssert.assertEquals(expectedBody, successResponse.getBody(), false);

    log.debug2("Done");
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

    // Empty AUId: Spring reports it cannot find a match to an endpoint.
    runTestGetAuAgreements(EMPTY_STRING, ANYBODY, HttpStatus.NOT_FOUND);

    // Non-existent AUId.
    String result = runTestGetAuAgreements(UNKNOWN_AUID_6, null, HttpStatus.OK);

    // Verify.
    assertEquals(stateManager.getAuAgreements(UNKNOWN_AUID_6).toJson(), result);

    // No credentials.
    result = runTestGetAuAgreements(GOOD_AUID_1, ANYBODY, HttpStatus.OK);

    // Verify.
    assertEquals(stateManager.getAuAgreements(GOOD_AUID_1).toJson(), result);

    // Bad credentials.
    result = runTestGetAuAgreements(GOOD_AUID_2, CONTENT_ADMIN, HttpStatus.OK);

    // Verify.
    assertEquals(stateManager.getAuAgreements(GOOD_AUID_2).toJson(), result);

    getAuAgreementsCommonTest();

    log.debug2("Done");
  }

  /**
   * Runs the getAuAgreements()-related authenticated-specific tests.
   */
  private void getAuAgreementsAuthenticatedTest() throws Exception {
    log.debug2("Invoked");

    // No AUId.
    runTestGetAuAgreements(null, null, HttpStatus.UNAUTHORIZED);

    // Empty AUId.
    runTestGetAuAgreements(EMPTY_STRING, null, HttpStatus.UNAUTHORIZED);

    // Non-existent AUId.
    runTestGetAuAgreements(UNKNOWN_AUID_8, ANYBODY, HttpStatus.UNAUTHORIZED);

    // No credentials.
    runTestGetAuAgreements(GOOD_AUID_1, null, HttpStatus.UNAUTHORIZED);

    // Bad credentials.
    runTestGetAuAgreements(GOOD_AUID_2, ANYBODY, HttpStatus.UNAUTHORIZED);

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

    // Empty AUId: Spring reports it cannot find a match to an endpoint.
    runTestGetAuAgreements(EMPTY_STRING, AU_ADMIN, HttpStatus.NOT_FOUND);

    // Non-existent AUId.
    String result =
	runTestGetAuAgreements(UNKNOWN_AUID_7, USER_ADMIN, HttpStatus.OK);

    // Verify.
    assertEquals(stateManager.getAuAgreements(UNKNOWN_AUID_7).toJson(), result);

    // Good AUId.
    result = runTestGetAuAgreements(GOOD_AUID_1, AU_ADMIN, HttpStatus.OK);

    // Verify.
    assertEquals(stateManager.getAuAgreements(GOOD_AUID_1).toJson(), result);

    // Good AUId.
    result = runTestGetAuAgreements(GOOD_AUID_2, USER_ADMIN, HttpStatus.OK);

    // Verify.
    assertEquals(stateManager.getAuAgreements(GOOD_AUID_2).toJson(), result);

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
   * @return a String with the stored Archival Unit Agreements.
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

    if (isSuccess(statusCode)) {
      result = response.getBody();
    }

    log.debug2("result = {}", result);
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

    runTestPatchAuAgreements(null, null, MediaType.APPLICATION_JSON, null,
	HttpStatus.NOT_FOUND);

    runTestPatchAuAgreements(null, null, MediaType.APPLICATION_JSON, ANYBODY,
	HttpStatus.NOT_FOUND);

    runTestPatchAuAgreements(null, null, MediaType.APPLICATION_JSON,
	CONTENT_ADMIN, HttpStatus.NOT_FOUND);

    // Empty AUId: Spring reports it cannot find a match to an endpoint.
    runTestPatchAuAgreements(EMPTY_STRING, null, null, null,
	HttpStatus.NOT_FOUND);

    runTestPatchAuAgreements(EMPTY_STRING, null, MediaType.APPLICATION_JSON,
	null, HttpStatus.NOT_FOUND);

    runTestPatchAuAgreements(EMPTY_STRING, null, MediaType.APPLICATION_JSON,
	ANYBODY, HttpStatus.NOT_FOUND);

    runTestPatchAuAgreements(EMPTY_STRING, null, MediaType.APPLICATION_JSON,
	CONTENT_ADMIN, HttpStatus.NOT_FOUND);

    // No AU agreements.
    runTestPatchAuAgreements(GOOD_AUID_1, null, null, null,
	HttpStatus.UNSUPPORTED_MEDIA_TYPE);

    runTestPatchAuAgreements(GOOD_AUID_1, null, MediaType.APPLICATION_JSON,
	null, HttpStatus.BAD_REQUEST);

    runTestPatchAuAgreements(GOOD_AUID_1, null, MediaType.APPLICATION_JSON,
	ANYBODY, HttpStatus.BAD_REQUEST);

    runTestPatchAuAgreements(GOOD_AUID_1, null, MediaType.APPLICATION_JSON,
	CONTENT_ADMIN, HttpStatus.BAD_REQUEST);

    runTestPatchAuAgreements(GOOD_AUID_1, EMPTY_STRING, null, null,
	HttpStatus.UNSUPPORTED_MEDIA_TYPE);

    runTestPatchAuAgreements(GOOD_AUID_1, EMPTY_STRING,
	MediaType.APPLICATION_JSON, null, HttpStatus.BAD_REQUEST);

    runTestPatchAuAgreements(GOOD_AUID_1, EMPTY_STRING,
	MediaType.APPLICATION_JSON, ANYBODY, HttpStatus.BAD_REQUEST);

    runTestPatchAuAgreements(GOOD_AUID_1, EMPTY_STRING,
	MediaType.APPLICATION_JSON, CONTENT_ADMIN, HttpStatus.BAD_REQUEST);

    AuAgreements auAgreements = createAuAgreements(GOOD_AUID_1,
	badPeerIdentityList, AgreementType.POR, 0.1f);

    // No Content-Type header.
    runTestPatchAuAgreements(GOOD_AUID_1, auAgreements.toJson(), null, null,
	HttpStatus.UNSUPPORTED_MEDIA_TYPE);

    // Bad peer agreement ID.
    runTestPatchAuAgreements(GOOD_AUID_1, auAgreements.toJson(),
	MediaType.APPLICATION_JSON, ANYBODY, HttpStatus.BAD_REQUEST);

    // Get the current poll agreements of the second AU.
    AuAgreements auAgreements2 = stateManager.getAuAgreements(GOOD_AUID_2);
    assertEquals(auAgreements2,	AuUtil.auAgreementsFromJson(
	runTestGetAuAgreements(GOOD_AUID_2, ANYBODY, HttpStatus.OK)));

    // Patch first AU.
    auAgreements = createAuAgreements(GOOD_AUID_1, goodPeerIdentityList,
	AgreementType.POP, 0.2f);

    runTestPatchAuAgreements(GOOD_AUID_1, auAgreements.toJson(),
	MediaType.APPLICATION_JSON, null, HttpStatus.OK);

    // Verify.
    auAgreementsMatch(auAgreements, stateManager.getAuAgreements(GOOD_AUID_1));
    AuAgreements auAgreements1 = auAgreements;

    // Verify that the current poll agreements of the second AU have not been
    // affected.
    auAgreementsMatch(auAgreements2, stateManager.getAuAgreements(GOOD_AUID_2));

    // Patch second AU.
    auAgreements = createAuAgreements(GOOD_AUID_2, goodPeerIdentityList,
	AgreementType.SYMMETRIC_POR, 0.3f);

    runTestPatchAuAgreements(GOOD_AUID_2, auAgreements.toJson(),
	MediaType.APPLICATION_JSON, CONTENT_ADMIN, HttpStatus.OK);

    // Verify.
    auAgreementsMatch(auAgreements, stateManager.getAuAgreements(GOOD_AUID_2));
    auAgreements2 = auAgreements;

    // Verify that the current poll agreements of the first AU have not been
    // affected.
    auAgreementsMatch(auAgreements1, stateManager.getAuAgreements(GOOD_AUID_1));

    // Patch first non-existent AU.
    auAgreements = createAuAgreements(UNKNOWN_AUID_1, goodPeerIdentityList,
	AgreementType.SYMMETRIC_POP, 0.4f);

    runTestPatchAuAgreements(UNKNOWN_AUID_1, auAgreements.toJson(),
	MediaType.APPLICATION_JSON, null, HttpStatus.OK);

    // Verify.
    auAgreementsMatch(auAgreements,
	stateManager.getAuAgreements(UNKNOWN_AUID_1));

    // Patch second non-existent AU.
    auAgreements = createAuAgreements(UNKNOWN_AUID_2, goodPeerIdentityList,
	AgreementType.POR_HINT, 0.5f);

    runTestPatchAuAgreements(UNKNOWN_AUID_2, auAgreements.toJson(),
	MediaType.APPLICATION_JSON, ANYBODY, HttpStatus.OK);

    // Verify.
    auAgreementsMatch(auAgreements,
	stateManager.getAuAgreements(UNKNOWN_AUID_2));

    // Patch third non-existent AU.
    auAgreements = createAuAgreements(UNKNOWN_AUID_3, goodPeerIdentityList,
	AgreementType.POP_HINT, 0.6f);

    runTestPatchAuAgreements(UNKNOWN_AUID_3, auAgreements.toJson(),
	MediaType.APPLICATION_JSON, CONTENT_ADMIN, HttpStatus.OK);

    // Verify.
    auAgreementsMatch(auAgreements,
	stateManager.getAuAgreements(UNKNOWN_AUID_3));

    // Verify that the current poll agreements of the first AU have not been
    // affected.
    auAgreementsMatch(auAgreements1, stateManager.getAuAgreements(GOOD_AUID_1));

    // Verify that the current poll agreements of the second AU have not been
    // affected.
    auAgreementsMatch(auAgreements2, stateManager.getAuAgreements(GOOD_AUID_2));

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

    runTestPatchAuAgreements(null, null, MediaType.APPLICATION_JSON, null,
	HttpStatus.UNAUTHORIZED);

    runTestPatchAuAgreements(null, null, MediaType.APPLICATION_JSON, ANYBODY,
	HttpStatus.UNAUTHORIZED);

    // Spring reports it cannot find a match to an endpoint.
    runTestPatchAuAgreements(null, null, MediaType.APPLICATION_JSON,
	CONTENT_ADMIN, HttpStatus.NOT_FOUND);

    // Empty AUId.
    runTestPatchAuAgreements(EMPTY_STRING, null, null, null,
	HttpStatus.UNAUTHORIZED);

    runTestPatchAuAgreements(EMPTY_STRING, null, MediaType.APPLICATION_JSON,
	null, HttpStatus.UNAUTHORIZED);

    runTestPatchAuAgreements(EMPTY_STRING, null, MediaType.APPLICATION_JSON,
	ANYBODY, HttpStatus.UNAUTHORIZED);

    // Spring reports it cannot find a match to an endpoint.
    runTestPatchAuAgreements(EMPTY_STRING, null, MediaType.APPLICATION_JSON,
	CONTENT_ADMIN, HttpStatus.NOT_FOUND);

    // No AU state.
    runTestPatchAuAgreements(GOOD_AUID_1, null, null, null,
	HttpStatus.UNAUTHORIZED);

    runTestPatchAuAgreements(GOOD_AUID_1, null, MediaType.APPLICATION_JSON,
	null, HttpStatus.UNAUTHORIZED);

    runTestPatchAuAgreements(GOOD_AUID_1, null, MediaType.APPLICATION_JSON,
	ANYBODY, HttpStatus.UNAUTHORIZED);

    runTestPatchAuAgreements(GOOD_AUID_1, null, MediaType.APPLICATION_JSON,
	CONTENT_ADMIN, HttpStatus.BAD_REQUEST);

    runTestPatchAuAgreements(GOOD_AUID_1, EMPTY_STRING, null, null,
	HttpStatus.UNAUTHORIZED);

    runTestPatchAuAgreements(GOOD_AUID_1, EMPTY_STRING,
	MediaType.APPLICATION_JSON, null, HttpStatus.UNAUTHORIZED);

    runTestPatchAuAgreements(GOOD_AUID_1, EMPTY_STRING,
	MediaType.APPLICATION_JSON, ANYBODY, HttpStatus.UNAUTHORIZED);

    runTestPatchAuAgreements(GOOD_AUID_1, EMPTY_STRING,
	MediaType.APPLICATION_JSON, CONTENT_ADMIN, HttpStatus.BAD_REQUEST);

    AuAgreements auAgreements = createAuAgreements(GOOD_AUID_1,
	badPeerIdentityList, AgreementType.SYMMETRIC_POR_HINT, 0.1f);

    // No Content-Type header.
    runTestPatchAuAgreements(GOOD_AUID_1, auAgreements.toJson(), null, null,
	HttpStatus.UNAUTHORIZED);

    // Bad peer agreement ID.
    runTestPatchAuAgreements(GOOD_AUID_1, auAgreements.toJson(),
	MediaType.APPLICATION_JSON, ANYBODY, HttpStatus.UNAUTHORIZED);

    // Patch first AU.
    runTestPatchAuAgreements(GOOD_AUID_1, auAgreements.toJson(),
	MediaType.APPLICATION_JSON, null, HttpStatus.UNAUTHORIZED);

    // Patch second AU.
    runTestPatchAuAgreements(GOOD_AUID_2, auAgreements.toJson(),
	MediaType.APPLICATION_JSON, ANYBODY, HttpStatus.UNAUTHORIZED);

    // Non-existent AUId.
    auAgreements = createAuAgreements(UNKNOWN_AUID_8, goodPeerIdentityList,
	AgreementType.SYMMETRIC_POP_HINT, 0.2f);

    runTestPatchAuAgreements(UNKNOWN_AUID_8, auAgreements.toJson(),
	MediaType.APPLICATION_JSON, CONTENT_ADMIN, HttpStatus.FORBIDDEN);

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
    runTestPatchAuAgreements(null, null, null, USER_ADMIN,
	HttpStatus.NOT_FOUND);

    runTestPatchAuAgreements(null, null, MediaType.APPLICATION_JSON, AU_ADMIN,
	HttpStatus.NOT_FOUND);

    runTestPatchAuAgreements(null, null, MediaType.APPLICATION_JSON, USER_ADMIN,
	HttpStatus.NOT_FOUND);

    // Empty AUId: Spring reports it cannot find a match to an endpoint.
    runTestPatchAuAgreements(EMPTY_STRING, null, null, AU_ADMIN,
	HttpStatus.NOT_FOUND);

    runTestPatchAuAgreements(EMPTY_STRING, null, null, USER_ADMIN,
	HttpStatus.NOT_FOUND);

    runTestPatchAuAgreements(EMPTY_STRING, null, MediaType.APPLICATION_JSON,
	AU_ADMIN, HttpStatus.NOT_FOUND);

    runTestPatchAuAgreements(EMPTY_STRING, null, MediaType.APPLICATION_JSON,
	USER_ADMIN, HttpStatus.NOT_FOUND);

    // No AU state.
    runTestPatchAuAgreements(GOOD_AUID_1, null, null, AU_ADMIN,
	HttpStatus.UNSUPPORTED_MEDIA_TYPE);

    runTestPatchAuAgreements(GOOD_AUID_1, null, null, USER_ADMIN,
	HttpStatus.UNSUPPORTED_MEDIA_TYPE);

    runTestPatchAuAgreements(GOOD_AUID_1, null, MediaType.APPLICATION_JSON,
	AU_ADMIN, HttpStatus.BAD_REQUEST);

    runTestPatchAuAgreements(GOOD_AUID_1, null, MediaType.APPLICATION_JSON,
	USER_ADMIN, HttpStatus.BAD_REQUEST);

    runTestPatchAuAgreements(GOOD_AUID_1, EMPTY_STRING, null, AU_ADMIN,
	HttpStatus.UNSUPPORTED_MEDIA_TYPE);

    runTestPatchAuAgreements(GOOD_AUID_1, EMPTY_STRING, null, USER_ADMIN,
	HttpStatus.UNSUPPORTED_MEDIA_TYPE);

    runTestPatchAuAgreements(GOOD_AUID_1, EMPTY_STRING,
	MediaType.APPLICATION_JSON, AU_ADMIN, HttpStatus.BAD_REQUEST);

    runTestPatchAuAgreements(GOOD_AUID_1, EMPTY_STRING,
	MediaType.APPLICATION_JSON, USER_ADMIN, HttpStatus.BAD_REQUEST);

    AuAgreements auAgreements = createAuAgreements(GOOD_AUID_1,
	badPeerIdentityList, AgreementType.SYMMETRIC_POP_HINT, 0.7f);

    // No Content-Type header.
    runTestPatchAuAgreements(GOOD_AUID_1, auAgreements.toJson(), null, AU_ADMIN,
	HttpStatus.UNSUPPORTED_MEDIA_TYPE);

    runTestPatchAuAgreements(GOOD_AUID_1, auAgreements.toJson(), null,
	USER_ADMIN, HttpStatus.UNSUPPORTED_MEDIA_TYPE);

    // Bad peer agreement ID.
    runTestPatchAuAgreements(GOOD_AUID_1, auAgreements.toJson(),
	MediaType.APPLICATION_JSON, AU_ADMIN, HttpStatus.BAD_REQUEST);

    runTestPatchAuAgreements(GOOD_AUID_1, auAgreements.toJson(),
	MediaType.APPLICATION_JSON, USER_ADMIN, HttpStatus.BAD_REQUEST);

    // Get the current poll agreements of the second AU.
    AuAgreements auAgreements2 = AuUtil.auAgreementsFromJson(
	runTestGetAuAgreements(GOOD_AUID_2, USER_ADMIN, HttpStatus.OK));

    // Verify.
    auAgreementsMatch(stateManager.getAuAgreements(GOOD_AUID_2), auAgreements2);

    // Patch first AU.
    auAgreements = createAuAgreements(GOOD_AUID_1, goodPeerIdentityList,
	AgreementType.SYMMETRIC_POR_HINT, 0.8f);

    runTestPatchAuAgreements(GOOD_AUID_1, auAgreements.toJson(),
	MediaType.APPLICATION_JSON, AU_ADMIN, HttpStatus.OK);

    // Verify.
    auAgreementsMatch(auAgreements, stateManager.getAuAgreements(GOOD_AUID_1));
    AuAgreements auAgreements1 = auAgreements;

    // Verify that the current poll agreements of the second AU have not been
    // affected.
    auAgreementsMatch(auAgreements2, stateManager.getAuAgreements(GOOD_AUID_2));

    // Patch second AU.
    auAgreements = createAuAgreements(GOOD_AUID_2, goodPeerIdentityList,
	AgreementType.POP_HINT, 0.9f);

    runTestPatchAuAgreements(GOOD_AUID_2, auAgreements.toJson(),
	MediaType.APPLICATION_JSON, USER_ADMIN, HttpStatus.OK);

    // Verify.
    auAgreementsMatch(auAgreements, stateManager.getAuAgreements(GOOD_AUID_2));
    auAgreements2 = auAgreements;

    // Verify that the current poll agreements of the first AU have not been
    // affected.
    auAgreementsMatch(auAgreements1, stateManager.getAuAgreements(GOOD_AUID_1));

    // Patch fourth non-existent AU.
    auAgreements = createAuAgreements(UNKNOWN_AUID_4, goodPeerIdentityList,
	AgreementType.POR_HINT, 0.99f);

    runTestPatchAuAgreements(UNKNOWN_AUID_4, auAgreements.toJson(),
	MediaType.APPLICATION_JSON, AU_ADMIN, HttpStatus.OK);

    // Verify.
    auAgreementsMatch(auAgreements,
	stateManager.getAuAgreements(UNKNOWN_AUID_4));

    // Patch fifth non-existent AU.
    auAgreements = createAuAgreements(UNKNOWN_AUID_5, goodPeerIdentityList,
	AgreementType.SYMMETRIC_POP, 0.999f);

    runTestPatchAuAgreements(UNKNOWN_AUID_5, auAgreements.toJson(),
	MediaType.APPLICATION_JSON, USER_ADMIN, HttpStatus.OK);

    // Verify.
    auAgreementsMatch(auAgreements,
	stateManager.getAuAgreements(UNKNOWN_AUID_5));

    // Verify that the current poll agreements of the first AU have not been
    // affected.
    auAgreementsMatch(auAgreements1, stateManager.getAuAgreements(GOOD_AUID_1));

    // Verify that the current poll agreements of the second AU have not been
    // affected.
    auAgreementsMatch(auAgreements2, stateManager.getAuAgreements(GOOD_AUID_2));

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
	  throws Exception {
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
   * Creates poll agreements for an Archival Unit.
   * 
   * @param auId
   *          A String with the Archival Unit identifier.
   * @param peerIdentityList
   *          A List<MockPeerIdentity> with peer identities.
   * @param agreementType
   *          An AgreementType with the type agreement.
   * @param agreementPercent
   *          A float with the agreement percent.
   * @return An AuAgreements with the Archival Unit poll agreements.
   */
  private AuAgreements createAuAgreements(String auId,
      List<MockPeerIdentity> peerIdentityList, AgreementType agreementType,
      float agreementPercent) {
    log.debug2("auId = {}", auId);
    log.debug2("peerIdentityList = {}", peerIdentityList);
    log.debug2("agreementType = {}", agreementType);
    log.debug2("agreementPercent = {}", agreementPercent);

    // Create the identity manager.
    MockIdentityManager idMgr = new MockIdentityManager();

    // Populate the identity manager with the passed peer identities.
    for (PeerIdentity pid : peerIdentityList) {
      idMgr.addPeerIdentity(pid.getIdString(), pid);
    }

    // Create the agreements.
    AuAgreements auAgreements = AuAgreements.make(auId, idMgr);

    // Populate the agreements details.
    for (int i = 0; i < peerIdentityList.size(); i++) {
      auAgreements.signalPartialAgreement(peerIdentityList.get(i),
	  agreementType, agreementPercent, TimeBase.nowMs() + i);
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
  private void auAgreementsMatch(AuAgreements aua1, AuAgreements aua2) {
    // Get the AuAgreements objects maps.
    Map<PeerIdentity, PeerAgreement> aua1Map =
	aua1.getAgreements(AgreementType.POR);
    //log.error("aua1Map = " + aua1Map);
    Map<PeerIdentity, PeerAgreement> aua2Map =
	aua2.getAgreements(AgreementType.POR);
    //log.error("aua2Map = " + aua2Map);

    // Verify that they have the same number of entries.
    assertEquals(aua1Map.size(), aua2Map.size());

    // Iterate through the first object map entries.
    Iterator<PeerIdentity> aua1Iterator = aua1Map.keySet().iterator();

    while (aua1Iterator.hasNext()) {
      // Assume that there is no match.
      boolean matched = false;

      // Get this first object peer identity.
      PeerIdentity aua1pid = aua1Iterator.next();
      //log.error("aua1pid = " + aua1pid);

      // Iterate through the second object map entries.
      Iterator<PeerIdentity> aua2Iterator = aua2Map.keySet().iterator();

      while (aua2Iterator.hasNext()) {
	// Get this second object peer identity.
	PeerIdentity aua2pid = aua2Iterator.next();
	//log.error("aua2pid = " + aua2pid);

	// Check whether the peer identities from both iterators match.
	if (aua2pid.getIdString().equals(aua1pid.getIdString())) {
	  // Yes: Verify that the agreements match.
	  assertEquals(aua1Map.get(aua1pid), aua2Map.get(aua2pid));

	  // Remember the match.
	  matched = true;
	  break;
	}
      }

      // Verify that this first object map entry has a match in the second
      // object map.
      assertTrue(matched);
    }
  }

  /**
   * Provides an indication of whether a successful response has been obtained.
   * 
   * @param statusCode
   *          An HttpStatus with the response status code.
   * @return a boolean with <code>true</code> if a successful response has been
   *         obtained, <code>false</code> otherwise.
   */
  private boolean isSuccess(HttpStatus statusCode) {
    return statusCode.is2xxSuccessful();
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
}
