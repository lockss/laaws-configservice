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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
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
import org.lockss.config.RestConfigClient;
import org.lockss.db.DbException;
import org.lockss.log.L4JLogger;
import org.lockss.plugin.PluginManager;
import org.lockss.plugin.AuUtil;
import org.lockss.plugin.definable.NamedArchivalUnit;
import org.lockss.util.MapUtil;
import org.lockss.util.rest.RestUtil;
import org.lockss.util.rest.exception.LockssRestException;
import org.lockss.util.rest.exception.LockssRestHttpException;
import org.lockss.spring.test.SpringLockssTestCase4;
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
public class TestAusApiServiceImpl extends SpringLockssTestCase4 {
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

  // The improperly-formatted identifier of an AU.
  private static final String BAD_AUID = "badAuId";

  // The identifier of an AU that does not exist in the test system.
  private static final String UNKNOWN_AUID ="unknown&auid";

  // Credentials.
  private final Credentials USER_ADMIN =
      this.new Credentials("lockss-u", "lockss-p");
  private final Credentials AU_ADMIN =
      this.new Credentials("au-admin", "I'mAuAdmin");
  private final Credentials CONTENT_ADMIN =
      this.new Credentials("content-admin", "I'mContentAdmin");
  private final Credentials ANYBODY =
      this.new Credentials("someUser", "somePassword");

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

    runGetSwaggerDocsTest(getTestUrlTemplate("/v2/api-docs"));
    runMethodsNotAllowedUnAuthenticatedTest();
    putAuConfigUnAuthenticatedTest();
    getAuConfigUnAuthenticatedTest();
    getAllAuConfigUnAuthenticatedTest();
    deleteAusUnAuthenticatedTest();
    calculateAuidUnAuthenticatedTest();
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
    putAuConfigAuthenticatedTest();
    getAuConfigAuthenticatedTest();
    getAllAuConfigAuthenticatedTest();
    deleteAusAuthenticatedTest();
    calculateAuidAuthenticatedTest();

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
    runTestMethodNotAllowed(GOOD_AUID_1, null, HttpMethod.PATCH,
	HttpStatus.METHOD_NOT_ALLOWED);

    runTestMethodNotAllowed(GOOD_AUID_1, ANYBODY, HttpMethod.POST,
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
    runTestMethodNotAllowed(GOOD_AUID_1, null, HttpMethod.PATCH,
	HttpStatus.UNAUTHORIZED);

    // Bad credentials.
    runTestMethodNotAllowed(GOOD_AUID_2, ANYBODY, HttpMethod.POST,
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
    runTestMethodNotAllowed(GOOD_AUID_1, AU_ADMIN, HttpMethod.PATCH,
	HttpStatus.METHOD_NOT_ALLOWED);

    runTestMethodNotAllowed(GOOD_AUID_1, USER_ADMIN, HttpMethod.POST,
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
    String template = getTestUrlTemplate("/aus/{auid}");

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
   * Runs the putAuConfig()-related un-authenticated-specific tests.
   */
  private void putAuConfigUnAuthenticatedTest() throws Exception {
    log.debug2("Invoked");

    PluginManager pluginManager =
	LockssDaemon.getLockssDaemon().getPluginManager();

    // No configuration.
    try {
      runTestPutAuConfig(null, null, null, HttpStatus.NOT_FOUND);
      fail("Should have thrown NullPointerException");
    } catch (NullPointerException npe) {
      // Expected.
    }

    try {
      runTestPutAuConfig(null, MediaType.APPLICATION_JSON, ANYBODY,
	  HttpStatus.BAD_REQUEST);
      fail("Should have thrown NullPointerException");
    } catch (NullPointerException npe) {
      // Expected.
    }

    try {
      runTestPutAuConfig(null, MediaType.APPLICATION_JSON, CONTENT_ADMIN,
	  HttpStatus.BAD_REQUEST);
      fail("Should have thrown NullPointerException");
    } catch (NullPointerException npe) {
      // Expected.
    }

    // No configuration using the REST service client.
    try {
      runTestPutAuConfigClient(null, null, null);
      fail("Should have thrown NullPointerException");
    } catch (NullPointerException npe) {
      // Expected.
    }

    try {
      runTestPutAuConfigClient(null, ANYBODY, null);
      fail("Should have thrown NullPointerException");
    } catch (NullPointerException npe) {
      // Expected.
    }

    try {
      runTestPutAuConfigClient(null, CONTENT_ADMIN, null);
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
    runTestPutAuConfig(auConfiguration1, null, null,
	HttpStatus.UNSUPPORTED_MEDIA_TYPE);

    // Get the current configuration of the first AU.
    AuConfiguration backupConfig1 =
	runTestGetAuConfig(GOOD_AUID_1, ANYBODY, HttpStatus.OK);

    // Verify.
    assertEquals(pluginManager.getStoredAuConfiguration(GOOD_AUID_1),
	backupConfig1);

    // Get the current configuration of the second AU.
    AuConfiguration backupConfig2 =
	runTestGetAuConfig(GOOD_AUID_2, CONTENT_ADMIN, HttpStatus.OK);

    // Verify.
    assertEquals(pluginManager.getStoredAuConfiguration(GOOD_AUID_2),
	backupConfig2);

    // Update the configuration of the first AU.
    runTestPutAuConfig(auConfiguration1, MediaType.APPLICATION_JSON, null,
	HttpStatus.OK);

    // Verify.
    assertEquals(auConfiguration1,
	runTestGetAuConfig(GOOD_AUID_1, ANYBODY, HttpStatus.OK));

    // Verify independently.
    assertEquals(auConfiguration1,
	pluginManager.getStoredAuConfiguration(GOOD_AUID_1));

    // Verify that the second AU is unaffected.
    assertEquals(pluginManager.getStoredAuConfiguration(GOOD_AUID_2),
	backupConfig2);

    // Restore the original configuration of the first AU using the REST service
    // client.
    runTestPutAuConfigClient(backupConfig1, null, HttpStatus.OK);

    // Verify.
    assertEquals(backupConfig1,
	runTestGetAuConfig(GOOD_AUID_1, ANYBODY, HttpStatus.OK));

    // Verify that the second AU is unaffected.
    assertEquals(pluginManager.getStoredAuConfiguration(GOOD_AUID_2),
	backupConfig2);

    AuConfiguration auConfiguration2 =
	new AuConfiguration(GOOD_AUID_2, auConfig);

    // Update the configuration of the second AU.
    runTestPutAuConfig(auConfiguration2, null, CONTENT_ADMIN, HttpStatus.OK);

    // Verify.
    assertEquals(auConfiguration2,
	runTestGetAuConfig(GOOD_AUID_2, CONTENT_ADMIN, HttpStatus.OK));

    // Verify independently.
    assertEquals(auConfiguration2,
	pluginManager.getStoredAuConfiguration(GOOD_AUID_2));

    // Verify that the first AU is unaffected.
    assertEquals(backupConfig1,
	runTestGetAuConfig(GOOD_AUID_1, null, HttpStatus.OK));

    // Restore the original configuration of the second AU using the REST
    // service client.
    runTestPutAuConfigClient(backupConfig2, ANYBODY, HttpStatus.OK);

    // Verify.
    assertEquals(backupConfig2,
	runTestGetAuConfig(GOOD_AUID_2, ANYBODY, HttpStatus.OK));

    // Verify that the first AU is unaffected.
    assertEquals(backupConfig1,
	runTestGetAuConfig(GOOD_AUID_1, null, HttpStatus.OK));

    // Update the AU configuration of a non-existent AU with some properties.
    runTestPutAuConfig(new AuConfiguration(UNKNOWN_AUID, auConfig),
	MediaType.APPLICATION_JSON, CONTENT_ADMIN, HttpStatus.OK);

    // Verify.
    assertEquals(auConfig, runTestGetAuConfig(UNKNOWN_AUID, CONTENT_ADMIN,
	HttpStatus.OK).getAuConfig());

    // Verify independently.
    assertEquals(pluginManager.getStoredAuConfiguration(UNKNOWN_AUID),
	runTestGetAuConfig(UNKNOWN_AUID, CONTENT_ADMIN, HttpStatus.OK));

    // Verify that the first AU is unaffected.
    assertEquals(backupConfig1,
	runTestGetAuConfig(GOOD_AUID_1, ANYBODY, HttpStatus.OK));

    // Verify that the second AU is unaffected.
    assertEquals(backupConfig2,
	runTestGetAuConfig(GOOD_AUID_2, CONTENT_ADMIN, HttpStatus.OK));

    // Delete the configuration just added.
    assertEquals(auConfig,
	runTestDeleteAus(UNKNOWN_AUID, null, HttpStatus.OK).getAuConfig());

    // Verify that the first AU is unaffected.
    assertEquals(backupConfig1,
	runTestGetAuConfig(GOOD_AUID_1, ANYBODY, HttpStatus.OK));

    // Verify that the second AU is unaffected.
    assertEquals(backupConfig2,
	runTestGetAuConfig(GOOD_AUID_2, CONTENT_ADMIN, HttpStatus.OK));

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
      runTestPutAuConfig(null, null, null, HttpStatus.UNAUTHORIZED);
      fail("Should have thrown NullPointerException");
    } catch (NullPointerException npe) {
      // Expected.
    }

    // No configuration using the REST service client.
    try {
      runTestPutAuConfigClient(null, null, null);
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
    runTestPutAuConfig(auConfiguration1, null, null, HttpStatus.UNAUTHORIZED);

    AuConfiguration auConfiguration2 =
	new AuConfiguration(GOOD_AUID_2, auConfig);

    runTestPutAuConfig(auConfiguration2, null, ANYBODY,
	HttpStatus.UNAUTHORIZED);

    // No credentials.
    runTestPutAuConfig(auConfiguration1, MediaType.APPLICATION_JSON, null,
	HttpStatus.UNAUTHORIZED);

    // No credentials using the REST service client.
    runTestPutAuConfigClient(auConfiguration2, null, HttpStatus.UNAUTHORIZED);

    // Bad credentials.
    runTestPutAuConfig(auConfiguration2, MediaType.APPLICATION_JSON, ANYBODY,
	HttpStatus.UNAUTHORIZED);

    runTestPutAuConfig(auConfiguration2, MediaType.APPLICATION_JSON,
	CONTENT_ADMIN, HttpStatus.FORBIDDEN);

    // Bad credentials using the REST service client.
    runTestPutAuConfigClient(auConfiguration2, ANYBODY,
	HttpStatus.UNAUTHORIZED);

    runTestPutAuConfigClient(auConfiguration2, CONTENT_ADMIN,
	HttpStatus.FORBIDDEN);

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
      runTestPutAuConfig(null, null, USER_ADMIN, HttpStatus.NOT_FOUND);
      fail("Should have thrown NullPointerException");
    } catch (NullPointerException npe) {
      // Expected.
    }

    try {
      runTestPutAuConfig(null, MediaType.APPLICATION_JSON, USER_ADMIN,
	  HttpStatus.BAD_REQUEST);
      fail("Should have thrown NullPointerException");
    } catch (NullPointerException npe) {
      // Expected.
    }

    // No configuration using the REST service client.
    try {
      runTestPutAuConfigClient(null, USER_ADMIN, null);
      fail("Should have thrown NullPointerException");
    } catch (NullPointerException npe) {
      // Expected.
    }

    // Get the current configuration of the first AU.
    AuConfiguration backupConfig1 =
	runTestGetAuConfig(GOOD_AUID_1, USER_ADMIN, HttpStatus.OK);

    // Verify.
    assertEquals(pluginManager.getStoredAuConfiguration(GOOD_AUID_1),
	backupConfig1);

    // Get the current configuration of the second AU.
    AuConfiguration backupConfig2 =
	runTestGetAuConfig(GOOD_AUID_2, AU_ADMIN, HttpStatus.OK);

    // Verify.
    assertEquals(pluginManager.getStoredAuConfiguration(GOOD_AUID_2),
	backupConfig2);

    Map<String, String> auConfig = new HashMap<>();
    auConfig.put("testKey1", "testValue1");
    auConfig.put("testKey2", "testValue2");

    AuConfiguration auConfiguration2 =
	new AuConfiguration(GOOD_AUID_2, auConfig);

    // Update the configuration of the second AU.
    runTestPutAuConfig(auConfiguration2, null, USER_ADMIN, HttpStatus.OK);

    // Verify.
    assertEquals(auConfiguration2,
	runTestGetAuConfig(GOOD_AUID_2, AU_ADMIN, HttpStatus.OK));

    // Verify independently.
    assertEquals(pluginManager.getStoredAuConfiguration(GOOD_AUID_2),
	auConfiguration2);

    // Verify that the first AU is unaffected.
    assertEquals(pluginManager.getStoredAuConfiguration(GOOD_AUID_1),
	backupConfig1);

    // Restore the original configuration of the second AU using the REST
    // service client.
    runTestPutAuConfigClient(backupConfig2, AU_ADMIN, HttpStatus.OK);

    // Verify.
    assertEquals(backupConfig2,
	runTestGetAuConfig(GOOD_AUID_2, USER_ADMIN, HttpStatus.OK));

    // Verify independently.
    assertEquals(pluginManager.getStoredAuConfiguration(GOOD_AUID_2),
	backupConfig2);

    // Verify that the first AU is unaffected.
    assertEquals(pluginManager.getStoredAuConfiguration(GOOD_AUID_1),
	backupConfig1);

    AuConfiguration auConfiguration1 =
	new AuConfiguration(GOOD_AUID_1, auConfig);

    // Update the configuration of the first AU using the REST service client.
    runTestPutAuConfigClient(auConfiguration1, USER_ADMIN, HttpStatus.OK);

    // Verify.
    assertEquals(auConfiguration1, runTestGetAuConfig(GOOD_AUID_1, USER_ADMIN,
	HttpStatus.OK));

    // Verify independently.
    assertEquals(pluginManager.getStoredAuConfiguration(GOOD_AUID_1),
	auConfiguration1);

    // Verify that the second AU is unaffected.
    assertEquals(backupConfig2, runTestGetAuConfig(GOOD_AUID_2, AU_ADMIN,
	HttpStatus.OK));

    // Restore the original configuration of the first AU.
    runTestPutAuConfig(backupConfig1, MediaType.APPLICATION_JSON, AU_ADMIN,
	HttpStatus.OK);

    // Verify.
    assertEquals(backupConfig1, runTestGetAuConfig(GOOD_AUID_1, USER_ADMIN,
	HttpStatus.OK));

    // Verify that the second AU is unaffected.
    assertEquals(backupConfig2, runTestGetAuConfig(GOOD_AUID_2, AU_ADMIN,
	HttpStatus.OK));

    AuConfiguration unknownAuConfiguration =
	new AuConfiguration(UNKNOWN_AUID, auConfig);

    // Update the AU configuration of a non-existent AU with some properties.
    runTestPutAuConfig(unknownAuConfiguration, null, USER_ADMIN, HttpStatus.OK);

    // Verify.
    assertEquals(auConfig, runTestGetAuConfig(UNKNOWN_AUID, AU_ADMIN,
	HttpStatus.OK).getAuConfig());

    // Verify independently.
    assertEquals(pluginManager.getStoredAuConfiguration(UNKNOWN_AUID)
	.getAuConfig(), auConfig);

    // Verify that the first AU is unaffected.
    assertEquals(backupConfig1, runTestGetAuConfig(GOOD_AUID_1, AU_ADMIN,
	HttpStatus.OK));

    // Verify that the second AU is unaffected.
    assertEquals(backupConfig2, runTestGetAuConfig(GOOD_AUID_2, USER_ADMIN,
	HttpStatus.OK));

    // Delete the configuration just added.
    assertEquals(unknownAuConfiguration,
	runTestDeleteAusClient(UNKNOWN_AUID, AU_ADMIN,HttpStatus.OK));

    // Verify that the first AU is unaffected.
    assertEquals(backupConfig1, runTestGetAuConfig(GOOD_AUID_1, USER_ADMIN,
	HttpStatus.OK));

    // Verify that the second AU is unaffected.
    assertEquals(backupConfig2, runTestGetAuConfig(GOOD_AUID_2, AU_ADMIN,
	HttpStatus.OK));

    log.debug2("Done");
  }

  /**
   * Performs a PUT operation for an Archival Unit.
   * 
   * @param auConfiguration
   *          An AuConfiguration with the Archival Unit configuration.
   * @param contentType
   *          A MediaType with the content type of the request.
   * @param credentials
   *          A Credentials with the request credentials.
   * @param expectedStatus
   *          An HttpStatus with the HTTP status of the result.
   */
  private void runTestPutAuConfig(AuConfiguration auConfiguration,
      MediaType contentType, Credentials credentials, HttpStatus expectedStatus)
  {
    log.debug2("auConfiguration = {}", auConfiguration);
    log.debug2("contentType = {}", contentType);
    log.debug2("credentials = {}", credentials);
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
    RestTemplate restTemplate = RestUtil.getRestTemplate();

    HttpEntity<AuConfiguration> requestEntity = null;

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
      requestEntity = new HttpEntity<AuConfiguration>(auConfiguration, headers);
    }

    // Make the request and get the response. 
    ResponseEntity<?> response = new TestRestTemplate(restTemplate)
	.exchange(uri, HttpMethod.PUT, requestEntity, String.class);

    // Get the response status.
    HttpStatus statusCode = response.getStatusCode();
    assertEquals(expectedStatus, statusCode);
  }

  /**
   * Performs a PUT operation for an Archival Unit using the REST service
   * client.
   * 
   * @param auConfiguration
   *          An AuConfiguration with the Archival Unit configuration.
   * @param credentials
   *          A Credentials with the request credentials.
   * @param expectedStatus
   *          An HttpStatus with the HTTP status of the result.
   */
  private void runTestPutAuConfigClient(AuConfiguration auConfiguration,
      Credentials credentials, HttpStatus expectedStatus) {
    log.debug2("auConfiguration = {}", auConfiguration);
    log.debug2("credentials = {}", credentials);
    log.debug2("expectedStatus = {}", expectedStatus);

    try {
      // Make the request and get the result.
      getRestConfigClient(credentials)
      .putArchivalUnitConfiguration(auConfiguration);

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
   * Runs the calculateAuid()-related un-authenticated-specific tests.
   */
  private void calculateAuidUnAuthenticatedTest() throws Exception {
    log.debug2("calculateAuidUnAuthenticatedTest Invoked");

    PluginManager pluginManager =
	LockssDaemon.getLockssDaemon().getPluginManager();

    assertMatchesRE("Must supply",
                    runTestCalculateAuid(null, null, null,
                                         null, HttpStatus.BAD_REQUEST));
    assertMatchesRE("Must supply",
                    runTestCalculateAuid("noplug", null, null,
                                         null, HttpStatus.BAD_REQUEST));
    assertMatchesRE("Must not supply",
                    runTestCalculateAuid(null, "handle",
                                         MapUtil.map("foo", "bar"),
                                         null, HttpStatus.BAD_REQUEST));
    assertMatchesRE("Plugin not found: noplug",
                    runTestCalculateAuid("noplug", null, MapUtil.map("a", "b"),
                                         null, HttpStatus.NOT_FOUND));

    String foo;

    foo = runTestCalculateAuid(null, "handlehandle", null,
                               null, HttpStatus.OK);
    assertEquals("org|lockss|plugin|NamedPlugin&handle~handlehandle",
                 AuUtil.jsonToMap(foo).get("auid"));

    foo = runTestCalculateAuid(NamedArchivalUnit.NAMED_PLUGIN_NAME,
                               null,
                               MapUtil.map("handle", "second|handle"),
                               null, HttpStatus.OK);
    assertEquals("org|lockss|plugin|NamedPlugin&handle~second%7Chandle",
                 AuUtil.jsonToMap(foo).get("auid"));

    calculateAuidCommonTest();
  }

  /**
   * Runs the calculateAuid()-related authenticated-specific tests.
   */
  private void calculateAuidAuthenticatedTest() throws Exception {

    PluginManager pluginManager =
	LockssDaemon.getLockssDaemon().getPluginManager();

    runTestCalculateAuid(null, null, null, null, HttpStatus.UNAUTHORIZED);
    runTestCalculateAuid("plug", null, null, null, HttpStatus.UNAUTHORIZED);
    runTestCalculateAuid(null, "handle", null, null, HttpStatus.UNAUTHORIZED);
    runTestCalculateAuid("plug", null, MapUtil.map("a", "b"),
                         null, HttpStatus.UNAUTHORIZED);
    calculateAuidCommonTest();
  }

  private void calculateAuidCommonTest() throws IOException {

    assertMatchesRE("Must supply",
                    runTestCalculateAuid(null, null, null,
                                         USER_ADMIN,
                                         HttpStatus.BAD_REQUEST));


    assertMatchesRE("Must supply",
                    runTestCalculateAuid("noplug", null, null,
                                         CONTENT_ADMIN, HttpStatus.BAD_REQUEST));
    assertMatchesRE("Must not supply",
                    runTestCalculateAuid(null, "handle",
                                         MapUtil.map("foo", "bar"),
                                         CONTENT_ADMIN, HttpStatus.BAD_REQUEST));
    assertMatchesRE("Plugin not found: noplug",
                    runTestCalculateAuid("noplug", null, MapUtil.map("a", "b"),
                                         CONTENT_ADMIN, HttpStatus.NOT_FOUND));

    String foo;

    foo = runTestCalculateAuid(null, "handlehandle", null,
                               CONTENT_ADMIN, HttpStatus.OK);
    assertEquals("org|lockss|plugin|NamedPlugin&handle~handlehandle",
                 AuUtil.jsonToMap(foo).get("auid"));

    foo = runTestCalculateAuid(NamedArchivalUnit.NAMED_PLUGIN_NAME,
                               null,
                               MapUtil.map("handle", "second|handle"),
                               CONTENT_ADMIN, HttpStatus.OK);
    assertEquals("org|lockss|plugin|NamedPlugin&handle~second%7Chandle",
                 AuUtil.jsonToMap(foo).get("auid"));

    foo = runTestCalculateAuid(NamedArchivalUnit.NAMED_PLUGIN_NAME,
                               null,
                               MapUtil.map("handle", "second|handle",
                                           "nondefparam", "foo"),
                               CONTENT_ADMIN, HttpStatus.OK);
    assertEquals("org|lockss|plugin|NamedPlugin&handle~second%7Chandle",
                 AuUtil.jsonToMap(foo).get("auid"));

    foo = runTestCalculateAuid("org.lockss.plugin.definable.GoodPlugin",
                               null,
                               MapUtil.map("missing", "params"),
                               CONTENT_ADMIN, HttpStatus.BAD_REQUEST);
    assertEquals("Illegal AU config: base_url is null in: {missing=params}",
                 foo);

    foo = runTestCalculateAuid("org.lockss.plugin.definable.GoodPlugin",
                               null,
                               MapUtil.map("base_url", "http://x.y/z",
                                           "num_issue_range", "2-7"),
                               CONTENT_ADMIN, HttpStatus.OK);

    assertEquals("org|lockss|plugin|definable|GoodPlugin&base_url~http%3A%2F%2Fx%2Ey%2Fz&num_issue_range~2-7",
                 AuUtil.jsonToMap(foo).get("auid"));
  }

  /**
   * Performs a POST to calculate an AUID
   *
   * @param pluginId
   *          plugin ID
   * @param handle
   *          A handle for a NamedArchivalUnit
   * @param auConfig
   *          A Map with the Archival Unit configuration.
   * @param credentials
   *          A Credentials with the request credentials.
   * @param expectedStatus
   *          An HttpStatus with the HTTP status of the result.
   */
  private String runTestCalculateAuid(String pluginId, String handle,
                                      Map<String,String> auConfig,
                                      Credentials credentials,
                                      HttpStatus expectedStatus)
      throws IOException {
    log.debug2("pluginId = {}", pluginId);
    log.debug2("handle = {}", handle);
    log.debug2("auConfig = {}", auConfig);
    log.debug2("credentials = {}", credentials);
    log.debug2("expectedStatus = {}", expectedStatus);

    try {
      // Make the request and get the result.
      String res = getRestConfigClient(credentials)
        .calculateAuid(pluginId, handle, auConfig);

      if (!RestUtil.isSuccess(expectedStatus)) {
	fail("Should have thrown LockssRestHttpException");
      }
      return res;
    } catch (LockssRestHttpException lrhe) {
      assertEquals(expectedStatus.value(), lrhe.getHttpStatusCode());
      assertEquals(expectedStatus.getReasonPhrase(),
                   lrhe.getHttpStatusMessage());
      return lrhe.getServerErrorMessage();
    } catch (LockssRestException lre) {
      fail("Should have thrown LockssRestHttpException");
    }
    return null;
  }

  /**
   * Runs the getAuConfig()-related un-authenticated-specific tests.
   */
  private void getAuConfigUnAuthenticatedTest()
      throws DbException, LockssRestException {
    log.debug2("Invoked");

    PluginManager pluginManager =
	LockssDaemon.getLockssDaemon().getPluginManager();

    // No AUId: Spring reports it cannot find a match to an endpoint.
    runTestGetAuConfig(null, null, HttpStatus.NOT_FOUND);

    // No AUId using the REST service client.
      runTestGetAuConfigClient(null, null, HttpStatus.NOT_FOUND);

    // Empty AUId: Spring reports it cannot find a match to an endpoint.
    runTestGetAuConfig(EMPTY_STRING, ANYBODY, HttpStatus.NOT_FOUND);

    // Empty AUId using the REST service client.
    runTestGetAuConfigClient(EMPTY_STRING, ANYBODY, HttpStatus.NOT_FOUND);

    // No credentials.
    AuConfiguration result =
	runTestGetAuConfig(GOOD_AUID_1, null, HttpStatus.OK);

    // Verify.
    assertEquals(pluginManager.getStoredAuConfiguration(GOOD_AUID_1), result);

    // No credentials using the REST service client.
    assertEquals(result,
	runTestGetAuConfigClient(GOOD_AUID_1, null, HttpStatus.OK));

    // Bad credentials.
    result = runTestGetAuConfig(GOOD_AUID_2, ANYBODY, HttpStatus.OK);

    // Verify.
    assertEquals(pluginManager.getStoredAuConfiguration(GOOD_AUID_2), result);

    // Bad credentials using the REST service client.
    assertEquals(result,
	runTestGetAuConfigClient(GOOD_AUID_2, ANYBODY, HttpStatus.OK));

    // Non-existent AUId.
    result = runTestGetAuConfig(UNKNOWN_AUID, CONTENT_ADMIN, HttpStatus.OK);

    // Verify.
    assertNull(result);

    // Verify independently.
    assertNull(pluginManager.getStoredAuConfiguration(UNKNOWN_AUID));

    // Non-existent AUId using the REST service client.
    assertEquals(result,
	runTestGetAuConfigClient(UNKNOWN_AUID, CONTENT_ADMIN, HttpStatus.OK));

    getAuConfigCommonTest();

    log.debug2("Done");
  }

  /**
   * Runs the getAuConfig()-related authenticated-specific tests.
   */
  private void getAuConfigAuthenticatedTest()
      throws DbException, LockssRestException {
    log.debug2("Invoked");

    PluginManager pluginManager =
	LockssDaemon.getLockssDaemon().getPluginManager();

    // No AUId.
    runTestGetAuConfig(null, null, HttpStatus.UNAUTHORIZED);

    // No AUId using the REST service client.
      runTestGetAuConfigClient(null, null, HttpStatus.UNAUTHORIZED);

    // Empty AUId.
    runTestGetAuConfig(EMPTY_STRING, null, HttpStatus.UNAUTHORIZED);

    // Empty AUId using the REST service client.
    runTestGetAuConfigClient(EMPTY_STRING, null, HttpStatus.UNAUTHORIZED);

    // No credentials.
    runTestGetAuConfig(GOOD_AUID_2, null, HttpStatus.UNAUTHORIZED);
    runTestGetAuConfig(UNKNOWN_AUID, null, HttpStatus.UNAUTHORIZED);

    // No credentials using the REST service client.
    runTestGetAuConfigClient(GOOD_AUID_2, null, HttpStatus.UNAUTHORIZED);
    runTestGetAuConfigClient(UNKNOWN_AUID, null, HttpStatus.UNAUTHORIZED);

    // Bad credentials.
    runTestGetAuConfig(GOOD_AUID_1, ANYBODY, HttpStatus.UNAUTHORIZED);
    runTestGetAuConfig(UNKNOWN_AUID, ANYBODY, HttpStatus.UNAUTHORIZED);

    // Bad credentials using the REST service client.
    runTestGetAuConfigClient(GOOD_AUID_1, ANYBODY, HttpStatus.UNAUTHORIZED);
    runTestGetAuConfigClient(UNKNOWN_AUID, ANYBODY, HttpStatus.UNAUTHORIZED);

    AuConfiguration result =
	runTestGetAuConfig(GOOD_AUID_1, CONTENT_ADMIN, HttpStatus.OK);

    // Verify.
    assertEquals(pluginManager.getStoredAuConfiguration(GOOD_AUID_1), result);

    // Using the REST service client.
    assertEquals(result,
	runTestGetAuConfigClient(GOOD_AUID_1, CONTENT_ADMIN, HttpStatus.OK));

    result = runTestGetAuConfig(UNKNOWN_AUID, CONTENT_ADMIN, HttpStatus.OK);

    // Verify.
    assertNull(result);

    // Verify independently.
    assertNull(pluginManager.getStoredAuConfiguration(UNKNOWN_AUID));

    // Using the REST service client.
    assertEquals(result,
	runTestGetAuConfigClient(UNKNOWN_AUID, CONTENT_ADMIN, HttpStatus.OK));

    getAuConfigCommonTest();

    log.debug2("Done");
  }

  /**
   * Runs the getAuConfig()-related authentication-independent tests.
   */
  private void getAuConfigCommonTest() throws DbException, LockssRestException {
    log.debug2("Invoked");

    PluginManager pluginManager =
	LockssDaemon.getLockssDaemon().getPluginManager();

    // No AUId: Spring reports it cannot find a match to an endpoint.
    runTestGetAuConfig(null, USER_ADMIN, HttpStatus.NOT_FOUND);

    // No AUId using the REST service client.
    runTestGetAuConfigClient(null, USER_ADMIN, HttpStatus.NOT_FOUND);

    // Empty AUId: Spring reports it cannot find a match to an endpoint.
    runTestGetAuConfig(EMPTY_STRING, AU_ADMIN, HttpStatus.NOT_FOUND);

    // Empty AUId using the REST service client.
    runTestGetAuConfigClient(EMPTY_STRING, AU_ADMIN, HttpStatus.NOT_FOUND);

    // Good AUId.
    AuConfiguration result =
	runTestGetAuConfig(GOOD_AUID_1, USER_ADMIN, HttpStatus.OK);

    // Verify.
    assertEquals(pluginManager.getStoredAuConfiguration(GOOD_AUID_1), result);

    // Good AUId using the REST service client.
    assertEquals(result,
	runTestGetAuConfigClient(GOOD_AUID_1, USER_ADMIN, HttpStatus.OK));

    // Non-existent AUId.
    result =
	runTestGetAuConfig(UNKNOWN_AUID, AU_ADMIN, HttpStatus.OK);

    // Verify.
    assertNull(result);

    // Verify independently.
    assertNull(pluginManager.getStoredAuConfiguration(UNKNOWN_AUID));

    // Non-existent AUId using the REST service client.
    assertEquals(result,
	runTestGetAuConfigClient(UNKNOWN_AUID, AU_ADMIN, HttpStatus.OK));

    log.debug2("Done");
  }

  /**
   * Performs a GET operation for an Archival Unit.
   * 
   * @param auId
   *          A String with the identifier of the Archival Unit.
   * @param credentials
   *          A Credentials with the request credentials.
   * @param expectedStatus
   *          An HttpStatus with the HTTP status of the result.
   * @return an AuConfiguration with the Archival Unit configuration.
   */
  private AuConfiguration runTestGetAuConfig(String auId,
      Credentials credentials, HttpStatus expectedStatus) {
    log.debug2("auId = {}", auId);
    log.debug2("credentials = {}", credentials);
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
    RestTemplate restTemplate = RestUtil.getRestTemplate();

    HttpEntity<AuConfiguration> requestEntity = null;

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

    if (RestUtil.isSuccess(statusCode)) {
      result = response.getBody();
    }

    log.debug2("result = {}", result);
    return result;
  }

  /**
   * Performs a GET operation for an Archival Unit using the REST service
   * client.
   * 
   * @param auId
   *          A String with the identifier of the Archival Unit.
   * @param credentials
   *          A Credentials with the request credentials.
   * @param expectedStatus
   *          An HttpStatus with the HTTP status of the result.
   * @return an AuConfiguration with the Archival Unit configuration.
   */
  private AuConfiguration runTestGetAuConfigClient(String auId,
      Credentials credentials, HttpStatus expectedStatus) {
    log.debug2("auId = {}", auId);
    log.debug2("credentials = {}", credentials);
    log.debug2("expectedStatus = {}", expectedStatus);

    AuConfiguration result = null;

    try {
      // Make the request and get the result.
      result =
	  getRestConfigClient(credentials).getArchivalUnitConfiguration(auId);
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
   * Runs the getAllAuConfig()-related un-authenticated-specific tests.
   */
  private void getAllAuConfigUnAuthenticatedTest() throws Exception {
    log.debug2("Invoked");

    PluginManager pluginManager =
	LockssDaemon.getLockssDaemon().getPluginManager();

    // No credentials.
    Collection<AuConfiguration> configOutput =
	runTestGetAllAuConfig(null, HttpStatus.OK);

    // Verify.
    assertEquals(2, configOutput.size());

    assertTrue(configOutput.contains(
	runTestGetAuConfig(GOOD_AUID_1, null, HttpStatus.OK)));

    assertTrue(configOutput.contains(
	runTestGetAuConfig(GOOD_AUID_2, null, HttpStatus.OK)));

    // Verify independently.
    assertTrue(configOutput.contains(
	pluginManager.getStoredAuConfiguration(GOOD_AUID_1)));

    assertTrue(configOutput.contains(
	pluginManager.getStoredAuConfiguration(GOOD_AUID_2)));

    // No credentials using the REST service client.
    assertEquals(configOutput,
	runTestGetAllAuConfigClient(null, HttpStatus.OK));

    // Bad credentials.
    Collection<AuConfiguration> configOutput2 =
	runTestGetAllAuConfig(ANYBODY, HttpStatus.OK);

    // Verify.
    assertEquals(configOutput, configOutput2);

    // Bad credentials using the REST service client.
    assertEquals(configOutput2,
	runTestGetAllAuConfigClient(ANYBODY, HttpStatus.OK));

    getAllAuConfigCommonTest();

    log.debug2("Done");
  }

  /**
   * Runs the getAllAuConfig()-related authenticated-specific tests.
   */
  private void getAllAuConfigAuthenticatedTest() throws Exception {
    log.debug2("Invoked");

    PluginManager pluginManager =
	LockssDaemon.getLockssDaemon().getPluginManager();

    // No credentials.
    runTestGetAllAuConfig(null, HttpStatus.UNAUTHORIZED);

    // No credentials using the REST service client.
    runTestGetAllAuConfigClient(null, HttpStatus.UNAUTHORIZED);

    // Bad credentials.
    runTestGetAllAuConfig(ANYBODY, HttpStatus.UNAUTHORIZED);

    // Bad credentials using the REST service client.
    runTestGetAllAuConfigClient(ANYBODY, HttpStatus.UNAUTHORIZED);

    Collection<AuConfiguration> configOutput =
	runTestGetAllAuConfig(CONTENT_ADMIN, HttpStatus.OK);

    // Verify.
    assertEquals(2, configOutput.size());

    assertTrue(configOutput.contains(
	runTestGetAuConfig(GOOD_AUID_1, CONTENT_ADMIN, HttpStatus.OK)));

    assertTrue(configOutput.contains(
	runTestGetAuConfig(GOOD_AUID_2, CONTENT_ADMIN, HttpStatus.OK)));

    // Verify independently.
    assertTrue(configOutput.contains(
	pluginManager.getStoredAuConfiguration(GOOD_AUID_1)));

    assertTrue(configOutput.contains(
	pluginManager.getStoredAuConfiguration(GOOD_AUID_2)));

    // Using the REST service client.
    assertEquals(configOutput,
	runTestGetAllAuConfigClient(CONTENT_ADMIN, HttpStatus.OK));

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
	runTestGetAllAuConfig(USER_ADMIN, HttpStatus.OK);

    // Verify.
    assertEquals(2, configOutput.size());

    assertTrue(configOutput.contains(
	runTestGetAuConfig(GOOD_AUID_1, AU_ADMIN, HttpStatus.OK)));

    assertTrue(configOutput.contains(
	runTestGetAuConfig(GOOD_AUID_2, USER_ADMIN, HttpStatus.OK)));

    // Verify independently.
    assertTrue(configOutput.contains(
	pluginManager.getStoredAuConfiguration(GOOD_AUID_1)));

    assertTrue(configOutput.contains(
	pluginManager.getStoredAuConfiguration(GOOD_AUID_2)));

    // Using the REST service client.
    assertEquals(configOutput,
	runTestGetAllAuConfigClient(USER_ADMIN, HttpStatus.OK));

    configOutput = runTestGetAllAuConfig(AU_ADMIN, HttpStatus.OK);

    // Verify.
    assertEquals(2, configOutput.size());

    assertTrue(configOutput.contains(
	runTestGetAuConfig(GOOD_AUID_1, USER_ADMIN, HttpStatus.OK)));

    assertTrue(configOutput.contains(
	runTestGetAuConfig(GOOD_AUID_2, AU_ADMIN, HttpStatus.OK)));

    // Verify independently.
    assertTrue(configOutput.contains(
	pluginManager.getStoredAuConfiguration(GOOD_AUID_1)));

    assertTrue(configOutput.contains(
	pluginManager.getStoredAuConfiguration(GOOD_AUID_2)));

    // Using the REST service client.
    assertEquals(configOutput,
	runTestGetAllAuConfigClient(AU_ADMIN, HttpStatus.OK));

    log.debug2("Done");
  }

  /**
   * Performs a GET operation for all Archival Units.
   * 
   * @param credentials
   *          A Credentials with the request credentials.
   * @param expectedStatus
   *          An HttpStatus with the HTTP status of the result.
   * @return a {@code Collection<AuConfiguration>} with the configuration of all
   *         Archival Units.
   * 
   * @throws Exception
   *           if there are problems.
   */
  private Collection<AuConfiguration> runTestGetAllAuConfig(
      Credentials credentials, HttpStatus expectedStatus) throws Exception {
    log.debug2("credentials = {}", credentials);
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
    RestTemplate restTemplate = RestUtil.getRestTemplate();

    HttpEntity<Collection<AuConfiguration>> requestEntity = null;

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
    if (RestUtil.isSuccess(statusCode)) {
      // Yes: Parse it.
      ObjectMapper mapper = new ObjectMapper();
      result = mapper.readValue((String)response.getBody(),
	  new TypeReference<Collection<AuConfiguration>>(){});
    }

    log.debug2("result = {}", result);
    return result;
  }

  /**
   * Performs a GET operation for all Archival Units using the REST service
   * client.
   * 
   * @param credentials
   *          A Credentials with the request credentials.
   * @param expectedStatus
   *          An HttpStatus with the HTTP status of the result.
   * @return a {@code Collection<AuConfiguration>} with the configuration of all
   *         Archival Units.
   */
  private Collection<AuConfiguration> runTestGetAllAuConfigClient(
      Credentials credentials, HttpStatus expectedStatus) {
    log.debug2("credentials = {}", credentials);
    log.debug2("expectedStatus = {}", expectedStatus);

    Collection<AuConfiguration> result = null;

    try {
      // Make the request and get the result.
      result =
	  getRestConfigClient(credentials).getAllArchivalUnitConfiguration();
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
   * Runs the deleteAus()-related un-authenticated-specific tests.
   */
  private void deleteAusUnAuthenticatedTest() throws Exception {
    log.debug2("Invoked");

    PluginManager pluginManager =
	LockssDaemon.getLockssDaemon().getPluginManager();

    // No AUId: Spring reports it cannot find a match to an endpoint.
    runTestDeleteAus(null, null, HttpStatus.NOT_FOUND);

    // No AUId using the REST service client.
    runTestDeleteAusClient(null, null, HttpStatus.NOT_FOUND);

    // Empty AUId: Spring reports it cannot find a match to an endpoint.
    runTestDeleteAus(EMPTY_STRING, ANYBODY, HttpStatus.NOT_FOUND);

    // Empty AUId using the REST service client.
    runTestDeleteAusClient(EMPTY_STRING, ANYBODY, HttpStatus.NOT_FOUND);

    // Get the current configuration of the second AU.
    AuConfiguration backupConfig2 =
	runTestGetAuConfig(GOOD_AUID_2, null, HttpStatus.OK);

    // Verify.
    assertEquals(pluginManager.getStoredAuConfiguration(GOOD_AUID_2),
	backupConfig2);

    // Get the current configuration of the first AU.
    AuConfiguration backupConfig1 =
	runTestGetAuConfig(GOOD_AUID_1, ANYBODY, HttpStatus.OK);

    // Verify.
    assertEquals(pluginManager.getStoredAuConfiguration(GOOD_AUID_1),
	backupConfig1);

    // Delete the second AU with no credentials.
    AuConfiguration result =
	runTestDeleteAus(GOOD_AUID_2, null, HttpStatus.OK);

    // Verify.
    assertEquals(backupConfig2, result);
    assertNull(runTestGetAuConfig(GOOD_AUID_2, CONTENT_ADMIN, HttpStatus.OK));

    // Verify that the first AU is unaffected.
    assertEquals(backupConfig1,
	runTestGetAuConfig(GOOD_AUID_1, null, HttpStatus.OK));

    // Delete the second AU again with no credentials using the REST service
    // client.
    assertNull(runTestDeleteAusClient(GOOD_AUID_2, null, HttpStatus.OK));

    // Verify.
    assertNull(runTestGetAuConfig(GOOD_AUID_2, null, HttpStatus.OK));

    // Delete the second AU again with bad credentials.
    result = runTestDeleteAus(GOOD_AUID_2, ANYBODY, HttpStatus.OK);

    // Verify.
    assertNull(result);
    assertNull(runTestGetAuConfig(GOOD_AUID_2, ANYBODY, HttpStatus.OK));

    // Verify that the first AU is unaffected.
    assertEquals(backupConfig1,
	runTestGetAuConfig(GOOD_AUID_1, CONTENT_ADMIN, HttpStatus.OK));

    // Delete the second AU again with bad credentials using the REST service
    // client.
    assertNull(runTestDeleteAusClient(GOOD_AUID_2, ANYBODY, HttpStatus.OK));

    // Verify.
    assertNull(runTestGetAuConfig(GOOD_AUID_2, ANYBODY, HttpStatus.OK));

    // Delete the first AU with bad credentials using the REST service client.
    result = runTestDeleteAusClient(GOOD_AUID_1, ANYBODY, HttpStatus.OK);

    // Verify.
    assertEquals(backupConfig1, result);
    assertNull(runTestGetAuConfig(GOOD_AUID_1, null, HttpStatus.OK));

    // Verify that the second AU is unaffected.
    assertNull(runTestGetAuConfig(GOOD_AUID_2, ANYBODY, HttpStatus.OK));

    // Delete the first AU again with no credentials.
    result = runTestDeleteAus(GOOD_AUID_1, null, HttpStatus.OK);

    // Verify.
    assertNull(result);
    assertNull(runTestGetAuConfig(GOOD_AUID_2, CONTENT_ADMIN, HttpStatus.OK));

    // Verify that the second AU is unaffected.
    assertNull(runTestGetAuConfig(GOOD_AUID_2, null, HttpStatus.OK));

    // Restore the original configuration of the second AU.
    runTestPutAuConfig(backupConfig2, MediaType.APPLICATION_JSON, null,
	HttpStatus.OK);

    // Verify.
    assertEquals(backupConfig2,
	runTestGetAuConfig(GOOD_AUID_2, CONTENT_ADMIN, HttpStatus.OK));

    // Verify that the first AU is unaffected.
    assertNull(runTestGetAuConfig(GOOD_AUID_1, ANYBODY, HttpStatus.OK));

    // Restore the original configuration of the first AU.
    runTestPutAuConfigClient(backupConfig1, ANYBODY, HttpStatus.OK);

    // Verify.
    assertEquals(backupConfig1,
	runTestGetAuConfig(GOOD_AUID_1, ANYBODY, HttpStatus.OK));

    // Verify that the second AU is unaffected.
    assertEquals(backupConfig2,
	runTestGetAuConfig(GOOD_AUID_2, CONTENT_ADMIN, HttpStatus.OK));

    deleteAusCommonTest();

    log.debug2("Done");
  }

  /**
   * Runs the deleteAus()-related authenticated-specific tests.
   */
  private void deleteAusAuthenticatedTest() throws Exception {
    log.debug2("Invoked");

    // No AUId.
    runTestDeleteAus(null, null, HttpStatus.UNAUTHORIZED);

    // No AUId using the REST service client.
    runTestDeleteAusClient(null, null, HttpStatus.UNAUTHORIZED);

    // Empty AUId.
    runTestDeleteAus(EMPTY_STRING, null, HttpStatus.UNAUTHORIZED);

    // Empty AUId using the REST service client.
    runTestDeleteAusClient(EMPTY_STRING, null, HttpStatus.UNAUTHORIZED);

    // No credentials.
    runTestDeleteAus(GOOD_AUID_2, null, HttpStatus.UNAUTHORIZED);
    runTestDeleteAus(UNKNOWN_AUID, null, HttpStatus.UNAUTHORIZED);

    // No credentials using the REST service client.
    runTestDeleteAusClient(GOOD_AUID_2, null, HttpStatus.UNAUTHORIZED);
    runTestDeleteAusClient(UNKNOWN_AUID, null, HttpStatus.UNAUTHORIZED);

    // Bad credentials.
    runTestDeleteAus(GOOD_AUID_1, ANYBODY, HttpStatus.UNAUTHORIZED);
    runTestDeleteAus(UNKNOWN_AUID, ANYBODY, HttpStatus.UNAUTHORIZED);

    // No credentials using the REST service client.
    runTestDeleteAusClient(GOOD_AUID_1, ANYBODY, HttpStatus.UNAUTHORIZED);
    runTestDeleteAusClient(UNKNOWN_AUID, ANYBODY, HttpStatus.UNAUTHORIZED);

    runTestDeleteAus(GOOD_AUID_1, CONTENT_ADMIN, HttpStatus.FORBIDDEN);
    runTestDeleteAus(UNKNOWN_AUID, CONTENT_ADMIN, HttpStatus.FORBIDDEN);

    runTestDeleteAusClient(GOOD_AUID_1, CONTENT_ADMIN, HttpStatus.FORBIDDEN);
    runTestDeleteAusClient(UNKNOWN_AUID, CONTENT_ADMIN, HttpStatus.FORBIDDEN);

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
    runTestDeleteAus(null, USER_ADMIN, HttpStatus.NOT_FOUND);

    // No AUId using the REST service client.
    runTestDeleteAusClient(null, USER_ADMIN, HttpStatus.NOT_FOUND);

    // Empty AUId: Spring reports it cannot find a match to an endpoint.
    runTestDeleteAus(EMPTY_STRING, AU_ADMIN, HttpStatus.NOT_FOUND);

    // Empty AUId using the REST service client.
    runTestDeleteAusClient(EMPTY_STRING, AU_ADMIN, HttpStatus.NOT_FOUND);

    // Unknown AU.
    AuConfiguration configOutput =
	runTestDeleteAus(UNKNOWN_AUID, USER_ADMIN, HttpStatus.OK);

    // Verify.
    assertNull(configOutput);

    // Unknown AU using the REST service client.
    assertNull(runTestDeleteAusClient(UNKNOWN_AUID, USER_ADMIN, HttpStatus.OK));

    // Get the current configuration of the first good AU.
    AuConfiguration backupConfig1 =
	runTestGetAuConfig(GOOD_AUID_1, AU_ADMIN, HttpStatus.OK);

    // Verify.
    assertEquals(pluginManager.getStoredAuConfiguration(GOOD_AUID_1),
	backupConfig1);

    // Get the current configuration of the second good AU.
    AuConfiguration backupConfig2 =
	runTestGetAuConfig(GOOD_AUID_2, USER_ADMIN, HttpStatus.OK);

    // Verify.
    assertEquals(pluginManager.getStoredAuConfiguration(GOOD_AUID_2),
	backupConfig2);

    // Delete the first AU.
    AuConfiguration result =
	runTestDeleteAus(GOOD_AUID_1, AU_ADMIN, HttpStatus.OK);

    // Verify.
    assertEquals(backupConfig1, result);
    assertNull(runTestGetAuConfig(GOOD_AUID_1, USER_ADMIN, HttpStatus.OK));

    // Verify that the second AU is unaffected.
    assertEquals(pluginManager.getStoredAuConfiguration(GOOD_AUID_2),
	backupConfig2);

    // Restore the original configuration of the first AU.
    runTestPutAuConfig(backupConfig1, MediaType.APPLICATION_JSON, AU_ADMIN,
	HttpStatus.OK);

    // Verify.
    assertEquals(backupConfig1,
	runTestGetAuConfig(GOOD_AUID_1, USER_ADMIN, HttpStatus.OK));

    // Verify independently.
    assertEquals(pluginManager.getStoredAuConfiguration(GOOD_AUID_1),
	backupConfig1);

    // Delete the first AU using the REST service client.
    result = runTestDeleteAusClient(GOOD_AUID_1, AU_ADMIN, HttpStatus.OK);

    // Verify.
    assertEquals(backupConfig1, result);
    assertNull(runTestGetAuConfig(GOOD_AUID_1, USER_ADMIN, HttpStatus.OK));

    // Verify that the second AU is unaffected.
    assertEquals(pluginManager.getStoredAuConfiguration(GOOD_AUID_2),
	backupConfig2);

    // Restore the original configuration of the first AU using the REST service
    // client.
    runTestPutAuConfigClient(backupConfig1, AU_ADMIN, HttpStatus.OK);

    // Verify.
    assertEquals(backupConfig1,
	runTestGetAuConfig(GOOD_AUID_1, AU_ADMIN, HttpStatus.OK));

    // Verify independently.
    assertEquals(pluginManager.getStoredAuConfiguration(GOOD_AUID_1),
	backupConfig1);

    // Delete the second AU.
    result = runTestDeleteAus(GOOD_AUID_2, USER_ADMIN, HttpStatus.OK);

    // Verify.
    assertEquals(backupConfig2, result);
    assertNull(runTestGetAuConfig(GOOD_AUID_2, AU_ADMIN, HttpStatus.OK));

    // Verify that the first AU is unaffected.
    assertEquals(pluginManager.getStoredAuConfiguration(GOOD_AUID_1),
	backupConfig1);

    // Restore the original configuration of the second AU using the REST
    // service client.
    runTestPutAuConfigClient(backupConfig2, USER_ADMIN, HttpStatus.OK);

    // Verify.
    assertEquals(backupConfig2,
	runTestGetAuConfig(GOOD_AUID_2, USER_ADMIN, HttpStatus.OK));

    // Verify independently.
    assertEquals(pluginManager.getStoredAuConfiguration(GOOD_AUID_2),
	backupConfig2);

    // Delete the second AU using the REST service client.
    result = runTestDeleteAusClient(GOOD_AUID_2, USER_ADMIN, HttpStatus.OK);

    // Verify.
    assertEquals(backupConfig2, result);
    assertNull(runTestGetAuConfig(GOOD_AUID_2, AU_ADMIN, HttpStatus.OK));

    // Verify that the first AU is unaffected.
    assertEquals(pluginManager.getStoredAuConfiguration(GOOD_AUID_1),
	backupConfig1);

    // Restore the original configuration of the second AU.
    runTestPutAuConfig(backupConfig2, MediaType.APPLICATION_JSON, USER_ADMIN,
	HttpStatus.OK);

    // Verify.
    assertEquals(backupConfig2,
	runTestGetAuConfig(GOOD_AUID_2, USER_ADMIN, HttpStatus.OK));

    // Verify independently.
    assertEquals(pluginManager.getStoredAuConfiguration(GOOD_AUID_2),
	backupConfig2);

    log.debug2("Done");
  }

  /**
   * Performs a DELETE operation for an Archival Unit.
   * 
   * @param auId
   *          A String with the identifier of the Archival Unit.
   * @param credentials
   *          A Credentials with the request credentials.
   * @param expectedStatus
   *          An HttpStatus with the HTTP status of the result.
   * @return an AuConfiguration with the configuration of the Archival Unit that
   *         was deleted.
   */
  private AuConfiguration runTestDeleteAus(String auId, Credentials credentials,
      HttpStatus expectedStatus) throws DbException, LockssRestException {
    log.debug2("auId = {}", auId);
    log.debug2("credentials = {}", credentials);
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
    RestTemplate restTemplate = RestUtil.getRestTemplate();

    HttpEntity<AuConfiguration> requestEntity = null;

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

    if (RestUtil.isSuccess(statusCode)) {
      // Verify independently.
      assertNull(LockssDaemon.getLockssDaemon().getPluginManager()
	  .getStoredAuConfiguration(auId));

      result = response.getBody();
    }

    log.debug2("result = {}", result);
    return result;
  }

  /**
   * Performs a DELETE operation for an Archival Unit using the REST service
   * client.
   * 
   * @param auId
   *          A String with the identifier of the Archival Unit.
   * @param credentials
   *          A Credentials with the request credentials.
   * @param expectedStatus
   *          An HttpStatus with the HTTP status of the result.
   * @return an AuConfiguration with the configuration of the Archival Unit that
   *         was deleted.
   */
  private AuConfiguration runTestDeleteAusClient(String auId,
      Credentials credentials, HttpStatus expectedStatus) {
    log.debug2("auId = {}", auId);
    log.debug2("credentials = {}", credentials);
    log.debug2("expectedStatus = {}", expectedStatus);

    AuConfiguration result = null;

    try {
      // Make the request and get the result.
      result = getRestConfigClient(credentials)
	  .deleteArchivalUnitConfiguration(auId);
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
