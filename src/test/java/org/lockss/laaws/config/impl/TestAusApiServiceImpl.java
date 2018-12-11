/*

Copyright (c) 2000-2018 Board of Trustees of Leland Stanford Jr. University,
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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.lockss.app.LockssDaemon;
import org.lockss.config.AuConfiguration;
import org.lockss.db.DbException;
import org.lockss.log.L4JLogger;
import org.lockss.plugin.PluginManager;
import org.lockss.test.SpringLockssTestCase;
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
 * Test class for org.lockss.laaws.config.api.AusApiController.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class TestAusApiServiceImpl extends SpringLockssTestCase {

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

  // The identifier of an AU that does not exist in the test system.
  private static final String UNKNOWN_AUID ="unknown&auid";

  // Credentials.
  private static final String GOOD_USER = "lockss-u";
  private static final String GOOD_PWD = "lockss-p";
  private static final String BAD_USER = "badUser";
  private static final String BAD_PWD = "badPassword";

  private static L4JLogger log = L4JLogger.getLogger();

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
    setUpTempDirectory(TestAusApiServiceImpl.class.getCanonicalName());

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
    putAuConfigUnAuthenticatedTest();
    getAuConfigUnAuthenticatedTest();
    getAllAuConfigUnAuthenticatedTest();
    deleteAusUnAuthenticatedTest();

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
    putAuConfigAuthenticatedTest();
    getAuConfigAuthenticatedTest();
    getAllAuConfigAuthenticatedTest();
    deleteAusAuthenticatedTest();

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
   * Runs the putAuConfig()-related un-authenticated-specific tests.
   */
  private void putAuConfigUnAuthenticatedTest() throws Exception {
    log.debug2("Invoked");

    PluginManager pluginManager =
	LockssDaemon.getLockssDaemon().getPluginManager();

    // No configuration.
    try {
      runTestPutAuConfig(null, null, null, null, HttpStatus.NOT_FOUND);
      fail("Should have thrown NullPointerException");
    } catch (NullPointerException npe) {
      // Expected.
    }

    try {
      runTestPutAuConfig(null, MediaType.APPLICATION_JSON, null, null,
	  HttpStatus.BAD_REQUEST);
      fail("Should have thrown NullPointerException");
    } catch (NullPointerException npe) {
      // Expected.
    }

    try {
      runTestPutAuConfig(null, MediaType.APPLICATION_JSON, BAD_USER, BAD_PWD,
	  HttpStatus.BAD_REQUEST);
      fail("Should have thrown NullPointerException");
    } catch (NullPointerException npe) {
      // Expected.
    }

    Map<String, String> auConfig = new HashMap<>();
    auConfig.put("testKey1", "testValue1");
    auConfig.put("testKey2", "testValue2");

    AuConfiguration auConfiguration1 =
	new AuConfiguration(GOOD_AUID_1, auConfig);
	
    // No Content-Type header.
    runTestPutAuConfig(auConfiguration1, null, null, null,
	HttpStatus.UNSUPPORTED_MEDIA_TYPE);

    // Get the current configuration of the first AU.
    AuConfiguration backupConfig1 =
	runTestGetAuConfig(GOOD_AUID_1, null, null, HttpStatus.OK);

    // Verify.
    assertEquals(pluginManager.getStoredAuConfiguration(GOOD_AUID_1),
	backupConfig1);

    // Get the current configuration of the second AU.
    AuConfiguration backupConfig2 =
	runTestGetAuConfig(GOOD_AUID_2, BAD_USER, BAD_PWD, HttpStatus.OK);

    // Verify.
    assertEquals(pluginManager.getStoredAuConfiguration(GOOD_AUID_2),
	backupConfig2);

    // Update the configuration of the first AU.
    AuConfiguration result = runTestPutAuConfig(auConfiguration1,
	MediaType.APPLICATION_JSON, null, null, HttpStatus.OK);

    // Verify.
    assertEquals(auConfiguration1, result);

    // Verify independently.
    assertEquals(auConfiguration1,
	pluginManager.getStoredAuConfiguration(GOOD_AUID_1));

    // Verify that the second AU is unaffected.
    assertEquals(pluginManager.getStoredAuConfiguration(GOOD_AUID_2),
	backupConfig2);

    // Restore the original configuration of the first AU.
    result = runTestPutAuConfig(backupConfig1, MediaType.APPLICATION_JSON,
	BAD_USER, BAD_PWD, HttpStatus.OK);

    // Verify.
    assertEquals(backupConfig1, result);

    // Verify that the second AU is unaffected.
    assertEquals(pluginManager.getStoredAuConfiguration(GOOD_AUID_2),
	backupConfig2);

    AuConfiguration auConfiguration2 =
	new AuConfiguration(GOOD_AUID_2, auConfig);

    // Update the configuration of the second AU.
    result = runTestPutAuConfig(auConfiguration2, null, BAD_USER, BAD_PWD,
	HttpStatus.OK);

    // Verify.
    assertEquals(auConfiguration2, result);

    // Verify independently.
    assertEquals(auConfiguration2,
	pluginManager.getStoredAuConfiguration(GOOD_AUID_2));

    // Verify that the first AU is unaffected.
    assertEquals(backupConfig1,
	runTestGetAuConfig(GOOD_AUID_1, null, null, HttpStatus.OK));

    // Restore the original configuration of the second AU.
    result = runTestPutAuConfig(backupConfig2, MediaType.APPLICATION_JSON,
	BAD_USER, BAD_PWD, HttpStatus.OK);

    // Verify.
    assertEquals(backupConfig2, result);

    // Verify that the first AU is unaffected.
    assertEquals(backupConfig1,
	runTestGetAuConfig(GOOD_AUID_1, null, null, HttpStatus.OK));

    // Update the AU configuration of a non-existent AU with some properties.
    result = runTestPutAuConfig(new AuConfiguration(UNKNOWN_AUID, auConfig),
	MediaType.APPLICATION_JSON, null, null, HttpStatus.OK);

    // Verify.
    assertEquals(auConfig, result.getAuConfig());

    // Verify independently.
    assertEquals(pluginManager.getStoredAuConfiguration(UNKNOWN_AUID), result);

    // Verify that the first AU is unaffected.
    assertEquals(backupConfig1,
	runTestGetAuConfig(GOOD_AUID_1, null, null, HttpStatus.OK));

    // Verify that the second AU is unaffected.
    assertEquals(backupConfig2,
	runTestGetAuConfig(GOOD_AUID_2, null, null, HttpStatus.OK));

    // Delete the configuration just added.
    assertEquals(auConfig, runTestDeleteAus(UNKNOWN_AUID, null, null,
	HttpStatus.OK).getAuConfig());

    // Verify that the first AU is unaffected.
    assertEquals(backupConfig1,
	runTestGetAuConfig(GOOD_AUID_1, null, null, HttpStatus.OK));

    // Verify that the second AU is unaffected.
    assertEquals(backupConfig2,
	runTestGetAuConfig(GOOD_AUID_2, null, null, HttpStatus.OK));

    putAuConfigCommonTest();

    log.debug2("Done");
  }

  /**
   * Runs the putAuConfig()-related authenticated-specific tests.
   */
  private void putAuConfigAuthenticatedTest() throws Exception {
    log.debug2("Invoked");

    // No configuration.
    try {
      runTestPutAuConfig(null, null, null, null, HttpStatus.UNAUTHORIZED);
      fail("Should have thrown NullPointerException");
    } catch (NullPointerException npe) {
      // Expected.
    }

    Map<String, String> auConfig = new HashMap<>();
    auConfig.put("testKey1", "testValue1");
    auConfig.put("testKey2", "testValue2");

    AuConfiguration auConfiguration1 =
	new AuConfiguration(GOOD_AUID_1, auConfig);

    // No Content-Type header.
    runTestPutAuConfig(auConfiguration1, null, null, null,
	HttpStatus.UNAUTHORIZED);

    AuConfiguration auConfiguration2 =
	new AuConfiguration(GOOD_AUID_2, auConfig);

    runTestPutAuConfig(auConfiguration2, null, BAD_USER, BAD_PWD,
	HttpStatus.UNAUTHORIZED);

    // No credentials.
    runTestPutAuConfig(auConfiguration1, MediaType.APPLICATION_JSON, null, null,
	HttpStatus.UNAUTHORIZED);

    // Bad credentials.
    runTestPutAuConfig(auConfiguration2, MediaType.APPLICATION_JSON, BAD_USER,
	BAD_PWD, HttpStatus.UNAUTHORIZED);

    putAuConfigCommonTest();

    log.debug2("Done");
  }

  /**
   * Runs the putAuConfig()-related authentication-independent tests.
   */
  private void putAuConfigCommonTest() throws Exception {
    log.debug2("Invoked");

    PluginManager pluginManager =
	LockssDaemon.getLockssDaemon().getPluginManager();

    // No configuration.
    try {
      runTestPutAuConfig(null, null, GOOD_USER, GOOD_PWD, HttpStatus.NOT_FOUND);
      fail("Should have thrown NullPointerException");
    } catch (NullPointerException npe) {
      // Expected.
    }

    try {
      runTestPutAuConfig(null, MediaType.APPLICATION_JSON, GOOD_USER, GOOD_PWD,
	  HttpStatus.BAD_REQUEST);
      fail("Should have thrown NullPointerException");
    } catch (NullPointerException npe) {
      // Expected.
    }

    // Get the current configuration of the first AU.
    AuConfiguration backupConfig1 =
	runTestGetAuConfig(GOOD_AUID_1, GOOD_USER, GOOD_PWD, HttpStatus.OK);

    // Verify.
    assertEquals(pluginManager.getStoredAuConfiguration(GOOD_AUID_1),
	backupConfig1);

    // Get the current configuration of the second AU.
    AuConfiguration backupConfig2 =
	runTestGetAuConfig(GOOD_AUID_2, GOOD_USER, GOOD_PWD, HttpStatus.OK);

    // Verify.
    assertEquals(pluginManager.getStoredAuConfiguration(GOOD_AUID_2),
	backupConfig2);

    Map<String, String> auConfig = new HashMap<>();
    auConfig.put("testKey1", "testValue1");
    auConfig.put("testKey2", "testValue2");

    AuConfiguration auConfiguration2 =
	new AuConfiguration(GOOD_AUID_2, auConfig);

    // Update the configuration of the second AU.
    AuConfiguration result = runTestPutAuConfig(auConfiguration2, null,
	GOOD_USER, GOOD_PWD, HttpStatus.OK);

    // Verify.
    assertEquals(auConfiguration2, result);

    // Verify independently.
    assertEquals(pluginManager.getStoredAuConfiguration(GOOD_AUID_2), result);

    // Verify that the first AU is unaffected.
    assertEquals(pluginManager.getStoredAuConfiguration(GOOD_AUID_1),
	backupConfig1);

    // Restore the original configuration of the second AU.
    result = runTestPutAuConfig(backupConfig2, MediaType.APPLICATION_JSON,
	GOOD_USER, GOOD_PWD, HttpStatus.OK);

    // Verify.
    assertEquals(backupConfig2, result);

    // Verify independently.
    assertEquals(pluginManager.getStoredAuConfiguration(GOOD_AUID_2), result);

    // Verify that the first AU is unaffected.
    assertEquals(pluginManager.getStoredAuConfiguration(GOOD_AUID_1),
	backupConfig1);

    AuConfiguration auConfiguration1 =
	new AuConfiguration(GOOD_AUID_1, auConfig);

    // Update the configuration of the first AU.
    result = runTestPutAuConfig(auConfiguration1, MediaType.APPLICATION_JSON,
	GOOD_USER, GOOD_PWD, HttpStatus.OK);

    // Verify.
    assertEquals(auConfiguration1, result);

    // Verify independently.
    assertEquals(pluginManager.getStoredAuConfiguration(GOOD_AUID_1), result);

    // Verify that the second AU is unaffected.
    assertEquals(backupConfig2, runTestGetAuConfig(GOOD_AUID_2, GOOD_USER,
	GOOD_PWD, HttpStatus.OK));

    // Restore the original configuration of the first AU.
    result = runTestPutAuConfig(backupConfig1, MediaType.APPLICATION_JSON,
	GOOD_USER, GOOD_PWD, HttpStatus.OK);

    // Verify.
    assertEquals(backupConfig1, result);

    // Verify that the second AU is unaffected.
    assertEquals(backupConfig2, runTestGetAuConfig(GOOD_AUID_2, GOOD_USER,
	GOOD_PWD, HttpStatus.OK));

    // Update the AU configuration of a non-existent AU with some properties.
    result = runTestPutAuConfig(new AuConfiguration(UNKNOWN_AUID, auConfig),
	null, GOOD_USER, GOOD_PWD, HttpStatus.OK);

    // Verify.
    assertEquals(auConfig, result.getAuConfig());

    // Verify independently.
    assertEquals(pluginManager.getStoredAuConfiguration(UNKNOWN_AUID)
	.getAuConfig(), auConfig);

    // Verify that the first AU is unaffected.
    assertEquals(backupConfig1, runTestGetAuConfig(GOOD_AUID_1, GOOD_USER,
	GOOD_PWD, HttpStatus.OK));

    // Verify that the second AU is unaffected.
    assertEquals(backupConfig2, runTestGetAuConfig(GOOD_AUID_2, GOOD_USER,
	GOOD_PWD, HttpStatus.OK));

    // Delete the configuration just added.
    assertEquals(auConfig, runTestDeleteAus(UNKNOWN_AUID, GOOD_USER, GOOD_PWD,
	HttpStatus.OK).getAuConfig());

    // Verify that the first AU is unaffected.
    assertEquals(backupConfig1, runTestGetAuConfig(GOOD_AUID_1, GOOD_USER,
	GOOD_PWD, HttpStatus.OK));

    // Verify that the second AU is unaffected.
    assertEquals(backupConfig2, runTestGetAuConfig(GOOD_AUID_2, GOOD_USER,
	GOOD_PWD, HttpStatus.OK));

    log.debug2("Done");
  }

  /**
   * Performs a PUT operation.
   * 
   * @param auConfiguration
   *          An AuConfiguration with the Archival Unit configuration.
   * @param contentType
   *          A MediaType with the content type of the request.
   * @param user
   *          A String with the request username.
   * @param password
   *          A String with the request password.
   * @param expectedStatus
   *          An HttpStatus with the HTTP status of the result.
   * @return an AuConfiguration with the stored Archival Unit configuration.
   */
  private AuConfiguration runTestPutAuConfig(AuConfiguration auConfiguration,
      MediaType contentType, String user, String password,
      HttpStatus expectedStatus) throws Exception {
    log.debug2("auConfiguration = {}", auConfiguration);
    log.debug2("contentType = {}", contentType);
    log.debug2("user = {}", user);
    log.debug2("password = {}", password);
    log.debug2("expectedStatus = {}", expectedStatus);

    // Get the test URL template.
    String template = getTestUrlTemplate("/aus/{auid}");

    // Create the URI of the request to the REST service.
    UriComponents uriComponents = UriComponentsBuilder.fromUriString(template)
	.build().expand(Collections.singletonMap("auid",
	    auConfiguration.getAuId()));

    URI uri = UriComponentsBuilder.newInstance().uriComponents(uriComponents)
	.build().encode().toUri();
    log.trace("uri = {}", uri);

    // Initialize the request to the REST service.
    RestTemplate restTemplate = new RestTemplate();

    HttpEntity<AuConfiguration> requestEntity = null;

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
      setUpCredentials(user, password, headers);

      log.trace("requestHeaders = {}", () -> headers.toSingleValueMap());

      // Create the request entity.
      requestEntity = new HttpEntity<AuConfiguration>(auConfiguration, headers);
    }

    // Make the request and get the response. 
    ResponseEntity<?> response = new TestRestTemplate(restTemplate)
	.exchange(uri, HttpMethod.PUT, requestEntity, String.class);

    // Get the response status.
    HttpStatus statusCode = response.getStatusCode();
    assertEquals(expectedStatus, statusCode);

    AuConfiguration result = null;

    // Check whether it is a success response.
    if (isSuccess(statusCode)) {
      // Yes: Parse it.
      ObjectMapper mapper = new ObjectMapper();
      result = mapper.readValue((String)response.getBody(),
	  AuConfiguration.class);
    }

    log.debug2("result = {}", result);
    return result;
  }

  /**
   * Runs the getAuConfig()-related un-authenticated-specific tests.
   */
  private void getAuConfigUnAuthenticatedTest() throws DbException {
    log.debug2("Invoked");

    PluginManager pluginManager =
	LockssDaemon.getLockssDaemon().getPluginManager();

    // No AUId: Spring reports it cannot find a match to an endpoint.
    runTestGetAuConfig(null, null, null, HttpStatus.NOT_FOUND);

    // Empty AUId: Spring reports it cannot find a match to an endpoint.
    runTestGetAuConfig(EMPTY_STRING, null, null, HttpStatus.NOT_FOUND);

    // No credentials.
    AuConfiguration result =
	runTestGetAuConfig(GOOD_AUID_1, null, null, HttpStatus.OK);

    // Verify.
    assertEquals(pluginManager.getStoredAuConfiguration(GOOD_AUID_1), result);

    // Bad credentials.
    result = runTestGetAuConfig(GOOD_AUID_2, BAD_USER, BAD_PWD, HttpStatus.OK);

    // Verify.
    assertEquals(pluginManager.getStoredAuConfiguration(GOOD_AUID_2), result);

    // Non-existent AUId.
    result = runTestGetAuConfig(UNKNOWN_AUID, null, null, HttpStatus.OK);

    // Verify.
    assertNull(result);

    // Verify independently.
    assertNull(pluginManager.getStoredAuConfiguration(UNKNOWN_AUID));

    getAuConfigCommonTest();

    log.debug2("Done");
  }

  /**
   * Runs the getAuConfig()-related authenticated-specific tests.
   */
  private void getAuConfigAuthenticatedTest() throws DbException {
    log.debug2("Invoked");

    // No AUId.
    runTestGetAuConfig(null, null, null, HttpStatus.UNAUTHORIZED);

    // Empty AUId.
    runTestGetAuConfig(EMPTY_STRING, null, null, HttpStatus.UNAUTHORIZED);

    // No credentials.
    runTestGetAuConfig(GOOD_AUID_2, null, null, HttpStatus.UNAUTHORIZED);
    runTestGetAuConfig(UNKNOWN_AUID, null, null, HttpStatus.UNAUTHORIZED);

    // Bad credentials.
    runTestGetAuConfig(GOOD_AUID_1, BAD_USER, BAD_PWD, HttpStatus.UNAUTHORIZED);
    runTestGetAuConfig(UNKNOWN_AUID, BAD_USER, BAD_PWD,
	HttpStatus.UNAUTHORIZED);

    getAuConfigCommonTest();

    log.debug2("Done");
  }

  /**
   * Runs the getAuConfig()-related authentication-independent tests.
   */
  private void getAuConfigCommonTest() throws DbException {
    log.debug2("Invoked");

    PluginManager pluginManager =
	LockssDaemon.getLockssDaemon().getPluginManager();

    // No AUId: Spring reports it cannot find a match to an endpoint.
    runTestGetAuConfig(null, GOOD_USER, GOOD_PWD, HttpStatus.NOT_FOUND);

    // Empty AUId: Spring reports it cannot find a match to an endpoint.
    runTestGetAuConfig(EMPTY_STRING, GOOD_USER, GOOD_PWD, HttpStatus.NOT_FOUND);

    // Good AUId.
    AuConfiguration result =
	runTestGetAuConfig(GOOD_AUID_1, GOOD_USER, GOOD_PWD, HttpStatus.OK);

    // Verify.
    assertEquals(pluginManager.getStoredAuConfiguration(GOOD_AUID_1), result);

    // Non-existent AUId.
    result =
	runTestGetAuConfig(UNKNOWN_AUID, GOOD_USER, GOOD_PWD, HttpStatus.OK);

    // Verify.
    assertNull(result);

    // Verify independently.
    assertNull(pluginManager.getStoredAuConfiguration(UNKNOWN_AUID));

    log.debug2("Done");
  }

  /**
   * Performs a GET operation for an Archival Unit.
   * 
   * @param auId
   *          A String with the identifier of the Archival Unit.
   * @param user
   *          A String with the request username.
   * @param password
   *          A String with the request password.
   * @param expectedStatus
   *          An HttpStatus with the HTTP status of the result.
   * @return an AuConfiguration with the Archival Unit configuration.
   */
  private AuConfiguration runTestGetAuConfig(String auId, String user,
      String password, HttpStatus expectedStatus) {
    log.debug2("auId = {}", auId);
    log.debug2("user = {}", user);
    log.debug2("password = {}", password);
    log.debug2("expectedStatus = {}", expectedStatus);

    // Get the test URL template.
    String template = getTestUrlTemplate("/aus/{auid}");

    // Create the URI of the request to the REST service.
    UriComponents uriComponents = UriComponentsBuilder.fromUriString(template)
	.build().expand(Collections.singletonMap("auid", auId));

    URI uri = UriComponentsBuilder.newInstance().uriComponents(uriComponents)
	.build().encode().toUri();
    log.trace("uri = {}", uri);

    // Initialize the request to the REST service.
    RestTemplate restTemplate = new RestTemplate();

    HttpEntity<AuConfiguration> requestEntity = null;

    // Check whether there are any custom headers to be specified in the
    // request.
    if (user != null || password != null) {

      // Initialize the request headers.
      HttpHeaders headers = new HttpHeaders();

      // Set up the authentication credentials, if necessary.
      setUpCredentials(user, password, headers);

      log.trace("requestHeaders = {}", () -> headers.toSingleValueMap());

      // Create the request entity.
      requestEntity = new HttpEntity<AuConfiguration>(null, headers);
    }

    // Make the request and get the response. 
    ResponseEntity<AuConfiguration> response =
	new TestRestTemplate(restTemplate). exchange(uri, HttpMethod.GET,
	    requestEntity, AuConfiguration.class);

    // Get the response status.
    HttpStatus statusCode = response.getStatusCode();
    assertEquals(expectedStatus, statusCode);

    AuConfiguration result = null;

    if (isSuccess(statusCode)) {
      result = response.getBody();
    }

    log.debug2("result = {}", result);
    return result;
  }

  /**
   * Runs the getAllAuConfig()-related un-authenticated-specific tests.
   */
  private void getAllAuConfigUnAuthenticatedTest() throws Exception {
    log.debug2("Invoked");

    PluginManager pluginManager =
	LockssDaemon.getLockssDaemon().getPluginManager();

    // No credentials.
    Collection<AuConfiguration> configOutput =
	runTestGetAllAuConfig(null, null, HttpStatus.OK);

    // Verify.
    configOutput.contains(
	runTestGetAuConfig(GOOD_AUID_1, null, null, HttpStatus.OK));
    configOutput.contains(
	runTestGetAuConfig(GOOD_AUID_2, null, null, HttpStatus.OK));

    // Verify independently.
    configOutput.contains(pluginManager.getStoredAuConfiguration(GOOD_AUID_1));
    configOutput.contains(pluginManager.getStoredAuConfiguration(GOOD_AUID_2));

    // Bad credentials.
    Collection<AuConfiguration> configOutput2 =
	runTestGetAllAuConfig(BAD_USER, BAD_PWD, HttpStatus.OK);

    // Verify.
    assertEquals(configOutput, configOutput2);

    getAllAuConfigCommonTest();

    log.debug2("Done");
  }

  /**
   * Runs the getAllAuConfig()-related authenticated-specific tests.
   */
  private void getAllAuConfigAuthenticatedTest() throws Exception {
    log.debug2("Invoked");

    // No credentials.
    runTestGetAllAuConfig(null, null, HttpStatus.UNAUTHORIZED);

    // Bad credentials.
    runTestGetAllAuConfig(BAD_USER, BAD_PWD, HttpStatus.UNAUTHORIZED);

    getAllAuConfigCommonTest();

    log.debug2("Done");
  }

  /**
   * Runs the getAllAuConfig()-related authentication-independent tests.
   */
  private void getAllAuConfigCommonTest() throws Exception {
    log.debug2("Invoked");

    PluginManager pluginManager =
	LockssDaemon.getLockssDaemon().getPluginManager();

    Collection<AuConfiguration> configOutput =
	runTestGetAllAuConfig(GOOD_USER, GOOD_PWD, HttpStatus.OK);
    log.error("configOutput = {}", configOutput);
    // Verify.
    configOutput.contains(
	runTestGetAuConfig(GOOD_AUID_1, GOOD_USER, GOOD_PWD, HttpStatus.OK));
    configOutput.contains(
	runTestGetAuConfig(GOOD_AUID_2, GOOD_USER, GOOD_PWD, HttpStatus.OK));

    // Verify independently.
    configOutput.contains(pluginManager.getStoredAuConfiguration(GOOD_AUID_1));
    configOutput.contains(pluginManager.getStoredAuConfiguration(GOOD_AUID_2));

    log.debug2("Done");
  }

  /**
   * Performs a GET operation for all Archival Units.
   * 
   * @param user
   *          A String with the request username.
   * @param password
   *          A String with the request password.
   * @param expectedStatus
   *          An HttpStatus with the HTTP status of the result.
   * @return a {@code Collection<AuConfiguration>} with the configuration of all
   *         Archival Units.
   * 
   * @throws Exception
   *           if there are problems.
   */
  private Collection<AuConfiguration> runTestGetAllAuConfig(String user,
      String password, HttpStatus expectedStatus) throws Exception {
    log.debug2("user = {}", user);
    log.debug2("password = {}", password);
    log.debug2("expectedStatus = {}", expectedStatus);

    // Get the test URL template.
    String template = getTestUrlTemplate("/aus");

    // Create the URI of the request to the REST service.
    UriComponents uriComponents =
	UriComponentsBuilder.fromUriString(template).build();

    URI uri = UriComponentsBuilder.newInstance().uriComponents(uriComponents)
	.build().encode().toUri();
    log.trace("uri = {}", uri);

    // Initialize the request to the REST service.
    RestTemplate restTemplate = new RestTemplate();

    HttpEntity<Collection<AuConfiguration>> requestEntity = null;

    // Check whether there are any custom headers to be specified in the
    // request.
    if (user != null || password != null) {

      // Initialize the request headers.
      HttpHeaders headers = new HttpHeaders();

      // Set up the authentication credentials, if necessary.
      setUpCredentials(user, password, headers);

      log.trace("requestHeaders = {}", () -> headers.toSingleValueMap());

      // Create the request entity.
      requestEntity =
	  new HttpEntity<Collection<AuConfiguration>>(null, headers);
    }

    // Make the request and get the response. 
    ResponseEntity<?> response = new TestRestTemplate(restTemplate)
	.exchange(uri, HttpMethod.GET, requestEntity, String.class);

    // Get the response status.
    HttpStatus statusCode = response.getStatusCode();
    assertEquals(expectedStatus, statusCode);

    Collection<AuConfiguration> result = null;

    // Check whether it is a success response.
    if (isSuccess(statusCode)) {
      // Yes: Parse it.
      ObjectMapper mapper = new ObjectMapper();
      result = mapper.readValue((String)response.getBody(),
	  new TypeReference<Collection<AuConfiguration>>(){});
    }

    log.debug2("result = {}", result);
    return result;
  }

  /**
   * Runs the deleteAus()-related un-authenticated-specific tests.
   */
  private void deleteAusUnAuthenticatedTest() throws Exception {
    log.debug2("Invoked");

    PluginManager pluginManager =
	LockssDaemon.getLockssDaemon().getPluginManager();

    // No AUId: Spring reports it cannot find a match to an endpoint.
    runTestDeleteAus(null, null, null, HttpStatus.NOT_FOUND);

    // Empty AUId: Spring reports it cannot find a match to an endpoint.
    runTestDeleteAus(EMPTY_STRING, null, null, HttpStatus.NOT_FOUND);

    // Get the current configuration of the second AU.
    AuConfiguration backupConfig2 =
	runTestGetAuConfig(GOOD_AUID_2, null, null, HttpStatus.OK);

    // Verify.
    assertEquals(pluginManager.getStoredAuConfiguration(GOOD_AUID_2),
	backupConfig2);

    // Get the current configuration of the first AU.
    AuConfiguration backupConfig1 =
	runTestGetAuConfig(GOOD_AUID_1, null, null, HttpStatus.OK);

    // Verify.
    assertEquals(pluginManager.getStoredAuConfiguration(GOOD_AUID_1),
	backupConfig1);

    // Delete the second AU with no credentials.
    AuConfiguration result =
	runTestDeleteAus(GOOD_AUID_2, null, null, HttpStatus.OK);

    // Verify.
    assertEquals(backupConfig2, result);
    assertNull(runTestGetAuConfig(GOOD_AUID_2, null, null, HttpStatus.OK));

    // Verify that the first AU is unaffected.
    assertEquals(backupConfig1,
	runTestGetAuConfig(GOOD_AUID_1, null, null, HttpStatus.OK));

    // Delete the second AU again with bad credentials.
    result = runTestDeleteAus(GOOD_AUID_2, BAD_USER, BAD_PWD, HttpStatus.OK);

    // Verify.
    assertNull(result);
    assertNull(runTestGetAuConfig(GOOD_AUID_2, null, null, HttpStatus.OK));

    // Verify that the first AU is unaffected.
    assertEquals(backupConfig1,
	runTestGetAuConfig(GOOD_AUID_1, null, null, HttpStatus.OK));

    // Delete the first AU with bad credentials.
    result = runTestDeleteAus(GOOD_AUID_1, BAD_USER, BAD_PWD, HttpStatus.OK);

    // Verify.
    assertEquals(backupConfig1, result);
    assertNull(runTestGetAuConfig(GOOD_AUID_1, null, null, HttpStatus.OK));

    // Verify that the second AU is unaffected.
    assertNull(runTestGetAuConfig(GOOD_AUID_2, null, null, HttpStatus.OK));

    // Delete the first AU again with no credentials.
    result = runTestDeleteAus(GOOD_AUID_1, null, null, HttpStatus.OK);

    // Verify.
    assertNull(result);
    assertNull(runTestGetAuConfig(GOOD_AUID_2, BAD_USER, BAD_PWD,
	HttpStatus.OK));

    // Verify that the second AU is unaffected.
    assertNull(runTestGetAuConfig(GOOD_AUID_2, null, null, HttpStatus.OK));

    // Restore the original configuration of the second AU.
    result = runTestPutAuConfig(backupConfig2, MediaType.APPLICATION_JSON, null,
	null, HttpStatus.OK);

    // Verify.
    assertEquals(backupConfig2, result);

    // Verify that the first AU is unaffected.
    assertNull(runTestGetAuConfig(GOOD_AUID_1, null, null, HttpStatus.OK));

    // Restore the original configuration of the first AU.
    result = runTestPutAuConfig(backupConfig1, MediaType.APPLICATION_JSON,
	GOOD_USER, GOOD_PWD, HttpStatus.OK);

    // Verify.
    assertEquals(backupConfig1, result);

    // Verify that the second AU is unaffected.
    assertEquals(backupConfig2,
	runTestGetAuConfig(GOOD_AUID_2, BAD_USER, BAD_PWD, HttpStatus.OK));

    deleteAusCommonTest();

    log.debug2("Done");
  }

  /**
   * Runs the deleteAus()-related authenticated-specific tests.
   */
  private void deleteAusAuthenticatedTest() throws Exception {
    log.debug2("Invoked");

    // No AUId.
    runTestDeleteAus(null, null, null, HttpStatus.UNAUTHORIZED);

    // Empty AUId.
    runTestDeleteAus(EMPTY_STRING, null, null, HttpStatus.UNAUTHORIZED);

    // No credentials.
    runTestDeleteAus(GOOD_AUID_2, null, null, HttpStatus.UNAUTHORIZED);
    runTestDeleteAus(UNKNOWN_AUID, null, null, HttpStatus.UNAUTHORIZED);

    // Bad credentials.
    runTestDeleteAus(GOOD_AUID_1, BAD_USER, BAD_PWD, HttpStatus.UNAUTHORIZED);
    runTestDeleteAus(UNKNOWN_AUID, BAD_USER, BAD_PWD, HttpStatus.UNAUTHORIZED);

    deleteAusCommonTest();

    log.debug2("Done");
  }

  /**
   * Runs the deleteAus()-related authenticated-independent tests.
   */
  private void deleteAusCommonTest() throws Exception {
    log.debug2("Invoked");

    PluginManager pluginManager =
	LockssDaemon.getLockssDaemon().getPluginManager();

    // No AUId: Spring reports it cannot find a match to an endpoint.
    runTestDeleteAus(null, GOOD_USER, GOOD_PWD, HttpStatus.NOT_FOUND);

    // No AUId: Spring reports it cannot find a match to an endpoint.
    runTestDeleteAus(EMPTY_STRING, GOOD_USER, GOOD_PWD, HttpStatus.NOT_FOUND);

    // Unknown AU.
    AuConfiguration configOutput =
	runTestDeleteAus(UNKNOWN_AUID, GOOD_USER, GOOD_PWD, HttpStatus.OK);

    // Verify.
    assertNull(configOutput);

    // Get the current configuration of the first good AU.
    AuConfiguration backupConfig1 =
	runTestGetAuConfig(GOOD_AUID_1, GOOD_USER, GOOD_PWD, HttpStatus.OK);

    // Verify.
    assertEquals(pluginManager.getStoredAuConfiguration(GOOD_AUID_1),
	backupConfig1);

    // Get the current configuration of the second good AU.
    AuConfiguration backupConfig2 =
	runTestGetAuConfig(GOOD_AUID_2, GOOD_USER, GOOD_PWD, HttpStatus.OK);

    // Verify.
    assertEquals(pluginManager.getStoredAuConfiguration(GOOD_AUID_2),
	backupConfig2);

    // Delete the first AU.
    AuConfiguration result = runTestDeleteAus(GOOD_AUID_1, GOOD_USER,
	GOOD_PWD, HttpStatus.OK);

    // Verify.
    assertEquals(backupConfig1, result);
    assertNull(runTestGetAuConfig(GOOD_AUID_1, GOOD_USER, GOOD_PWD,
	HttpStatus.OK));

    // Verify that the second AU is unaffected.
    assertEquals(pluginManager.getStoredAuConfiguration(GOOD_AUID_2),
	backupConfig2);

    // Restore the original configuration of the first AU.
    result = runTestPutAuConfig(backupConfig1, MediaType.APPLICATION_JSON,
	GOOD_USER, GOOD_PWD, HttpStatus.OK);

    // Verify.
    assertEquals(backupConfig1, result);

    // Verify independently.
    assertEquals(pluginManager.getStoredAuConfiguration(GOOD_AUID_1), result);

    // Delete the second AU.
    result = runTestDeleteAus(GOOD_AUID_2, GOOD_USER, GOOD_PWD, HttpStatus.OK);

    // Verify.
    assertEquals(backupConfig2, result);
    assertNull(runTestGetAuConfig(GOOD_AUID_2, GOOD_USER, GOOD_PWD,
	HttpStatus.OK));

    // Verify that the first AU is unaffected.
    assertEquals(pluginManager.getStoredAuConfiguration(GOOD_AUID_1),
	backupConfig1);

    // Restore the original configuration of the second AU.
    result = runTestPutAuConfig(backupConfig2, MediaType.APPLICATION_JSON,
	GOOD_USER, GOOD_PWD, HttpStatus.OK);

    // Verify.
    assertEquals(backupConfig2, result);

    // Verify independently.
    assertEquals(pluginManager.getStoredAuConfiguration(GOOD_AUID_2), result);

    log.debug2("Done");
  }

  /**
   * Performs a DELETE operation for an Archival Unit.
   * 
   * @param auId
   *          A String with the identifier of the Archival Unit.
   * @param user
   *          A String with the request username.
   * @param password
   *          A String with the request password.
   * @param expectedStatus
   *          An HttpStatus with the HTTP status of the result.
   * @return an AuConfiguration with the configuration of the Archival Unit that
   *         was deleted.
   */
  private AuConfiguration runTestDeleteAus(String auId, String user,
      String password, HttpStatus expectedStatus) throws DbException {
    log.debug2("auId = {}", auId);
    log.debug2("user = {}", user);
    log.debug2("password = {}", password);
    log.debug2("expectedStatus = {}", expectedStatus);

    // Get the test URL template.
    String template = getTestUrlTemplate("/aus/{auid}");

    // Create the URI of the request to the REST service.
    UriComponents uriComponents = UriComponentsBuilder.fromUriString(template)
	.build().expand(Collections.singletonMap("auid", auId));

    URI uri = UriComponentsBuilder.newInstance().uriComponents(uriComponents)
	.build().encode().toUri();
    log.trace("uri = {}", uri);

    // Initialize the request to the REST service.
    RestTemplate restTemplate = new RestTemplate();

    HttpEntity<AuConfiguration> requestEntity = null;

    // Check whether there are any custom headers to be specified in the
    // request.
    if (user != null || password != null) {

      // Initialize the request headers.
      HttpHeaders headers = new HttpHeaders();

      // Set up the authentication credentials, if necessary.
      setUpCredentials(user, password, headers);

      log.trace("requestHeaders = {}", () -> headers.toSingleValueMap());

      // Create the request entity.
      requestEntity = new HttpEntity<AuConfiguration>(null, headers);
    }

    // Make the request and get the response. 
    ResponseEntity<AuConfiguration> response =
	new TestRestTemplate(restTemplate). exchange(uri, HttpMethod.DELETE,
	    requestEntity, AuConfiguration.class);

    // Get the response status.
    HttpStatus statusCode = response.getStatusCode();
    assertEquals(expectedStatus, statusCode);

    AuConfiguration result = null;

    if (isSuccess(statusCode)) {
      // Verify independently.
      assertNull(LockssDaemon.getLockssDaemon().getPluginManager()
	  .getStoredAuConfiguration(auId));

      result = response.getBody();
    }

    log.debug2("result = {}", result);
    return result;
  }

  /**
   * Adds credentials to the HTTP headers, if necessary.
   * 
   * @param user
   *          A String with the credentials username.
   * @param password
   *          A String with the credentials password.
   * @param headers
   *          An HttpHeaders with the HTTP headers.
   */
  private void setUpCredentials(String user, String password,
      HttpHeaders headers) {
    // Check whether there are credentials to be added.
    if (user != null && password != null) {
      // Yes: Set the authentication credentials.
      String credentials = user + ":" + password;
      String authHeaderValue = "Basic " + Base64.getEncoder()
      .encodeToString(credentials.getBytes(StandardCharsets.US_ASCII));

      headers.set("Authorization", authHeaderValue);
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
