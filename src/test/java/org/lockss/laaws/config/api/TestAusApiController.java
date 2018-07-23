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
package org.lockss.laaws.config.api;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.lockss.app.LockssDaemon;
import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.laaws.config.model.ConfigExchange;
import org.lockss.plugin.PluginManager;
import org.lockss.test.SpringLockssTestCase;
import org.skyscreamer.jsonassert.JSONAssert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class TestAusApiController extends SpringLockssTestCase {
  private static final String UI_PORT_CONFIGURATION_TEMPLATE =
      "UiPortConfigTemplate.txt";
  private static final String UI_PORT_CONFIGURATION_FILE = "UiPort.opt";

  private static final String EMPTY_STRING = "";

  // The identifier of an AU that exists in the test system.
  private static final String GOOD_AUID =
      "org|lockss|plugin|pensoft|oai|PensoftOaiPlugin"
      + "&au_oai_date~2014&au_oai_set~biorisk"
      + "&base_url~http%3A%2F%2Fbiorisk%2Epensoft%2Enet%2F";

  // The identifier of an AU that does not exist in the test system.
  private static final String UNKNOWN_AUID ="unknown_auid";

  // Credentials.
  private static final String GOOD_USER = "lockss-u";
  private static final String GOOD_PWD = "lockss-p";
  private static final String BAD_USER = "badUser";
  private static final String BAD_PWD = "badPassword";

  private static final Logger logger =
      LoggerFactory.getLogger(TestAusApiController.class);

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
    if (logger.isDebugEnabled()) logger.debug("port = " + port);

    // Set up the temporary directory where the test data will reside.
    setUpTempDirectory(TestAusApiController.class.getCanonicalName());

    // Copy the necessary files to the test temporary directory.
    File srcTree = new File(new File("test"), "cache");
    if (logger.isDebugEnabled())
      logger.debug("srcTree = " + srcTree.getAbsolutePath());

    copyToTempDir(srcTree);

    srcTree = new File(new File("test"), "tdbxml");
    if (logger.isDebugEnabled())
      logger.debug("srcTree = " + srcTree.getAbsolutePath());

    copyToTempDir(srcTree);

    // Set up the UI port.
    setUpUiPort(UI_PORT_CONFIGURATION_TEMPLATE, UI_PORT_CONFIGURATION_FILE);
  }

  /**
   * Runs the tests with authentication turned off.
   * 
   * @throws Exception
   *           if there are problems.
   */
  @Test
  public void runUnAuthenticatedTests() throws Exception {
    // Specify the command line parameters to be used for the tests.
    List<String> cmdLineArgs = getCommandLineArguments();
    cmdLineArgs.add("-p");
    cmdLineArgs.add("test/config/testAuthOff.opt");

    CommandLineRunner runner = appCtx.getBean(CommandLineRunner.class);
    runner.run(cmdLineArgs.toArray(new String[cmdLineArgs.size()]));

    getSwaggerDocsTest();
    putAuConfigUnAuthenticatedTest();
    getAuConfigUnAuthenticatedTest();
    getAllAuConfigUnAuthenticatedTest();
    deleteAusUnAuthenticatedTest();

    if (logger.isDebugEnabled()) logger.debug("Done.");
  }

  /**
   * Runs the tests with authentication turned on.
   * 
   * @throws Exception
   *           if there are problems.
   */
  @Test
  public void runAuthenticatedTests() throws Exception {
    // Specify the command line parameters to be used for the tests.
    List<String> cmdLineArgs = getCommandLineArguments();
    cmdLineArgs.add("-p");
    cmdLineArgs.add("test/config/testAuthOn.opt");

    CommandLineRunner runner = appCtx.getBean(CommandLineRunner.class);
    runner.run(cmdLineArgs.toArray(new String[cmdLineArgs.size()]));

    getSwaggerDocsTest();
    putAuConfigAuthenticatedTest();
    getAuConfigAuthenticatedTest();
    getAllAuConfigAuthenticatedTest();
    deleteAusAuthenticatedTest();

    if (logger.isDebugEnabled()) logger.debug("Done.");
  }

  /**
   * Provides the standard command line arguments to start the server.
   * 
   * @return a List<String> with the command line arguments.
   */
  private List<String> getCommandLineArguments() {
    List<String> cmdLineArgs = new ArrayList<String>();
    cmdLineArgs.add("-p");
    cmdLineArgs.add(getPlatformDiskSpaceConfigPath());
    cmdLineArgs.add("-p");
    cmdLineArgs.add("config/common.xml");

    File folder =
	new File(new File(new File(getTempDirPath()), "tdbxml"), "prod");
    if (logger.isDebugEnabled()) logger.debug("folder = " + folder);

    cmdLineArgs.add("-x");
    cmdLineArgs.add(folder.getAbsolutePath());
    cmdLineArgs.add("-p");
    cmdLineArgs.add(getUiPortConfigFile().getAbsolutePath());
    cmdLineArgs.add("-p");
    cmdLineArgs.add("test/config/lockss.txt");
    cmdLineArgs.add("-p");
    cmdLineArgs.add("test/config/lockss.opt");

    return cmdLineArgs;
  }

  /**
   * Runs the Swagger-related tests.
   * 
   * @throws Exception
   *           if there are problems.
   */
  private void getSwaggerDocsTest() throws Exception {
    if (logger.isDebugEnabled()) logger.debug("Invoked.");

    ResponseEntity<String> successResponse = new TestRestTemplate().exchange(
	getTestUrlTemplate("/v2/api-docs"), HttpMethod.GET, null, String.class);

    HttpStatus statusCode = successResponse.getStatusCode();
    assertEquals(HttpStatus.OK, statusCode);

    String expectedBody = "{'swagger':'2.0',"
	+ "'info':{'description':'API of Configuration Service for LAAWS'}}";

    JSONAssert.assertEquals(expectedBody, successResponse.getBody(), false);
    if (logger.isDebugEnabled()) logger.debug("Done.");
  }

  /**
   * Runs the putAuConfig()-related un-authenticated-specific tests.
   */
  private void putAuConfigUnAuthenticatedTest() {
    if (logger.isDebugEnabled()) logger.debug("Invoked.");

    // No AUId.
    runTestPutAuConfig(null, null, null, null, null, HttpStatus.NOT_FOUND);

    // Empty AUId.
    runTestPutAuConfig(null, EMPTY_STRING, null, null, null,
	HttpStatus.NOT_FOUND);

    // No Content-Type header.
    runTestPutAuConfig(null, GOOD_AUID, null, null, null,
	HttpStatus.UNSUPPORTED_MEDIA_TYPE);

    runTestPutAuConfig(null, GOOD_AUID, null, BAD_USER, BAD_PWD,
	HttpStatus.UNSUPPORTED_MEDIA_TYPE);

    // No payload.
    runTestPutAuConfig(null, GOOD_AUID, MediaType.APPLICATION_JSON, null, null,
	HttpStatus.INTERNAL_SERVER_ERROR);

    runTestPutAuConfig(null, GOOD_AUID, MediaType.APPLICATION_JSON, BAD_USER,
	BAD_PWD, HttpStatus.INTERNAL_SERVER_ERROR);

    // Get the current configuration.
    ConfigExchange backupConfig =
	runTestGetAuConfig(GOOD_AUID, null, null, HttpStatus.OK);

    // Verify.
    Map<String, String> backupProps = backupConfig.getProps();
    verifyGoodAuConfig(backupProps);

    // The payload to be sent.
    Map<String, String> props = new HashMap<String, String>();
    props.put("testKey1", "testValue1");
    props.put("testKey2", "testValue2");

    ConfigExchange configInput = new ConfigExchange();
    configInput.setProps(props);

    // Update the AU configuration.
    Map<String, String> result = runTestPutAuConfig(configInput, GOOD_AUID,
	MediaType.APPLICATION_JSON, null, null, HttpStatus.OK).getProps();

    // Verify.
    assertEquals(props, result);

    // Verify independently.
    Configuration config = LockssDaemon.getLockssDaemon().getPluginManager()
	.getStoredAuConfiguration(GOOD_AUID);

    assertEquals(config.keySet().size(), result.size());

    for (String key : config.keySet()) {
      assertEquals(config.get(key), result.get(key));
    }

    // Restore the original configuration.
    result = runTestPutAuConfig(backupConfig, GOOD_AUID,
	MediaType.APPLICATION_JSON, BAD_USER, BAD_PWD, HttpStatus.OK)
	.getProps();

    // Verify.
    assertEquals(backupProps, result);

    // Update the AU configuration of a non-existent AU with empty properties.
    result = runTestPutAuConfig(new ConfigExchange(), UNKNOWN_AUID,
	MediaType.APPLICATION_JSON, null, null, HttpStatus.OK).getProps();

    // Verify.
    assertTrue(result.keySet().isEmpty());

    // Verify independently.
    config = LockssDaemon.getLockssDaemon().getPluginManager()
	.getStoredAuConfiguration(UNKNOWN_AUID);

    assertTrue(config.keySet().isEmpty());

    // Update the AU configuration of a non-existent AU with a payload.
    result = runTestPutAuConfig(configInput, UNKNOWN_AUID,
	MediaType.APPLICATION_JSON, BAD_USER, BAD_PWD, HttpStatus.OK)
	.getProps();

    // Verify.
    assertEquals(props, result);

    // Verify independently.
    config = LockssDaemon.getLockssDaemon().getPluginManager()
	.getStoredAuConfiguration(UNKNOWN_AUID);

    assertEquals(config.keySet().size(), result.size());

    for (String key : config.keySet()) {
      assertEquals(config.get(key), result.get(key));
    }

    // Delete the configuration just added.
    assertEquals(props,
	runTestDeleteAus(UNKNOWN_AUID, null, null, HttpStatus.OK).getProps());

    putAuConfigCommonTest();

    if (logger.isDebugEnabled()) logger.debug("Done.");
  }

  /**
   * Runs the putAuConfig()-related authenticated-specific tests.
   */
  private void putAuConfigAuthenticatedTest() {
    if (logger.isDebugEnabled()) logger.debug("Invoked.");

    // No AUId.
    runTestPutAuConfig(null, null, null, null, null, HttpStatus.UNAUTHORIZED);

    // Empty AUId.
    runTestPutAuConfig(null, EMPTY_STRING, null, null, null,
	HttpStatus.UNAUTHORIZED);

    // No Content-Type header.
    runTestPutAuConfig(null, GOOD_AUID, null, null, null,
	HttpStatus.UNAUTHORIZED);

    runTestPutAuConfig(null, GOOD_AUID, null, BAD_USER, BAD_PWD,
	HttpStatus.UNAUTHORIZED);

    // No payload.
    runTestPutAuConfig(null, GOOD_AUID, MediaType.APPLICATION_JSON, null, null,
	HttpStatus.UNAUTHORIZED);

    runTestPutAuConfig(null, GOOD_AUID, MediaType.APPLICATION_JSON, BAD_USER,
	BAD_PWD, HttpStatus.UNAUTHORIZED);

    // The payload to be sent.
    Map<String, String> props = new HashMap<String, String>();
    props.put("testKey1", "testValue1");
    props.put("testKey2", "testValue2");

    ConfigExchange configInput = new ConfigExchange();
    configInput.setProps(props);

    // No credentials.
    runTestPutAuConfig(configInput, GOOD_AUID, MediaType.APPLICATION_JSON, null,
	null, HttpStatus.UNAUTHORIZED);

    // Bad credentials.
    runTestPutAuConfig(configInput, GOOD_AUID, MediaType.APPLICATION_JSON,
	BAD_USER, BAD_PWD, HttpStatus.UNAUTHORIZED);

    putAuConfigCommonTest();

    if (logger.isDebugEnabled()) logger.debug("Done.");
  }

  /**
   * Runs the putAuConfig()-related authentication-independent tests.
   */
  private void putAuConfigCommonTest() {
    if (logger.isDebugEnabled()) logger.debug("Invoked.");

    // No AUId.
    runTestPutAuConfig(null, null, null, GOOD_USER, GOOD_PWD,
	HttpStatus.NOT_FOUND);

    // Empty AUId.
    runTestPutAuConfig(null, EMPTY_STRING, null, GOOD_USER, GOOD_PWD,
	HttpStatus.NOT_FOUND);

    // No Content-Type header.
    runTestPutAuConfig(null, GOOD_AUID, null, GOOD_USER, GOOD_PWD,
	HttpStatus.UNSUPPORTED_MEDIA_TYPE);

    // No payload.
    runTestPutAuConfig(null, GOOD_AUID, MediaType.APPLICATION_JSON, GOOD_USER,
	GOOD_PWD, HttpStatus.INTERNAL_SERVER_ERROR);

    // Get the current configuration.
    ConfigExchange backupConfig =
	runTestGetAuConfig(GOOD_AUID, GOOD_USER, GOOD_PWD, HttpStatus.OK);

    // Verify.
    Map<String, String> backupProps = backupConfig.getProps();
    verifyGoodAuConfig(backupProps);

    // The payload to be sent.
    Map<String, String> props = new HashMap<String, String>();
    props.put("testKey1", "testValue1");
    props.put("testKey2", "testValue2");

    ConfigExchange configInput = new ConfigExchange();
    configInput.setProps(props);

    // Update the AU configuration.
    Map<String, String> result = runTestPutAuConfig(configInput, GOOD_AUID,
	MediaType.APPLICATION_JSON, GOOD_USER, GOOD_PWD, HttpStatus.OK)
	.getProps();

    // Verify.
    assertEquals(props, result);

    // Verify independently.
    Configuration config = LockssDaemon.getLockssDaemon().getPluginManager()
	.getStoredAuConfiguration(GOOD_AUID);

    assertEquals(config.keySet().size(), result.size());

    for (String key : config.keySet()) {
      assertEquals(config.get(key), result.get(key));
    }

    // Restore the original configuration.
    result = runTestPutAuConfig(backupConfig, GOOD_AUID,
	MediaType.APPLICATION_JSON, GOOD_USER, GOOD_PWD, HttpStatus.OK)
	.getProps();

    // Verify.
    assertEquals(backupProps, result);

    // Update the AU configuration of a non-existent AU with empty properties.
    result = runTestPutAuConfig(new ConfigExchange(), UNKNOWN_AUID,
	MediaType.APPLICATION_JSON, GOOD_USER, GOOD_PWD, HttpStatus.OK)
	.getProps();

    // Verify.
    assertTrue(result.keySet().isEmpty());

    // Verify independently.
    config = LockssDaemon.getLockssDaemon().getPluginManager()
	.getStoredAuConfiguration(UNKNOWN_AUID);

    assertTrue(config.keySet().isEmpty());

    // Update the AU configuration of a non-existent AU with a payload.
    result = runTestPutAuConfig(configInput, UNKNOWN_AUID,
	MediaType.APPLICATION_JSON, GOOD_USER, GOOD_PWD, HttpStatus.OK)
	.getProps();

    // Verify.
    assertEquals(props, result);

    // Verify independently.
    config = LockssDaemon.getLockssDaemon().getPluginManager()
	.getStoredAuConfiguration(UNKNOWN_AUID);

    assertEquals(config.keySet().size(), result.size());

    for (String key : config.keySet()) {
      assertEquals(config.get(key), result.get(key));
    }

    // Delete the configuration just added.
    assertEquals(props, runTestDeleteAus(UNKNOWN_AUID, GOOD_USER, GOOD_PWD,
	HttpStatus.OK).getProps());

    if (logger.isDebugEnabled()) logger.debug("Done.");
  }

  /**
   * Performs a PUT operation.
   * 
   * @param config
   *          A ConfigExchange with the Archival Unit configuration.
   * @param auId
   *          A String with the identifier of the Archival Unit.
   * @param contentType
   *          A MediaType with the content type of the request.
   * @param user
   *          A String with the request username.
   * @param password
   *          A String with the request password.
   * @param expectedStatus
   *          An HttpStatus with the HTTP status of the result.
   * @return a ConfigExchange with the Archival Unit configuration.
   */
  private ConfigExchange runTestPutAuConfig(ConfigExchange config, String auId,
      MediaType contentType, String user, String password,
      HttpStatus expectedStatus) {
    if (logger.isDebugEnabled()) {
      logger.debug("config = " + config);
      logger.debug("auId = " + auId);
      logger.debug("contentType = " + contentType);
      logger.debug("user = " + user);
      logger.debug("password = " + password);
      logger.debug("expectedStatus = " + expectedStatus);
    }

    // Get the test URL template.
    String template = getTestUrlTemplate("/aus/{auid}");

    // Create the URI of the request to the REST service.
    UriComponents uriComponents = UriComponentsBuilder.fromUriString(template)
	.build().expand(Collections.singletonMap("auid", auId));

    URI uri = UriComponentsBuilder.newInstance().uriComponents(uriComponents)
	.build().encode().toUri();
    if (logger.isDebugEnabled()) logger.debug("uri = " + uri);

    // Initialize the request to the REST service.
    RestTemplate restTemplate = new RestTemplate();

    HttpEntity<ConfigExchange> requestEntity = null;

    // Check whether there are any custom headers to be specified in the
    // request.
    if (config != null || contentType != null || user != null
	|| password != null) {

      // Initialize the request headers.
      HttpHeaders headers = new HttpHeaders();

      // Check whether there is a custom "Content-Type" header.
      if (contentType != null) {
	// Yes: Set it.
	headers.setContentType(contentType);
      }

      // Set up the authentication credentials, if necessary.
      setUpCredentials(user, password, headers);

      if (logger.isDebugEnabled())
	logger.debug("requestHeaders = " + headers.toSingleValueMap());

      // Create the request entity.
      requestEntity = new HttpEntity<ConfigExchange>(config, headers);
    }

    // Make the request and get the response. 
    ResponseEntity<ConfigExchange> response =
	new TestRestTemplate(restTemplate). exchange(uri, HttpMethod.PUT,
	    requestEntity, ConfigExchange.class);

    // Get the response status.
    HttpStatus statusCode = response.getStatusCode();
    assertEquals(expectedStatus, statusCode);

    return response.getBody();
  }

  /**
   * Runs the getAuConfig()-related un-authenticated-specific tests.
   */
  private void getAuConfigUnAuthenticatedTest() {
    if (logger.isDebugEnabled()) logger.debug("Invoked.");

    // No AUId.
    runTestGetAuConfig(null, null, null, HttpStatus.NOT_FOUND);

    // Empty AUId.
    runTestGetAuConfig(EMPTY_STRING, null, null, HttpStatus.NOT_FOUND);

    // No credentials.
    ConfigExchange configOutput =
	runTestGetAuConfig(GOOD_AUID, null, null, HttpStatus.OK);

    // Verify.
    Map<String, String> goodAuidProps = configOutput.getProps();
    verifyGoodAuConfig(goodAuidProps);

    // Bad credentials.
    configOutput =
	runTestGetAuConfig(GOOD_AUID, BAD_USER, BAD_PWD, HttpStatus.OK);

    // Verify.
    assertEquals(goodAuidProps, configOutput.getProps());

    // Non-existent AUId.
    configOutput = runTestGetAuConfig(UNKNOWN_AUID, null, null, HttpStatus.OK);

    // Verify.
    assertTrue(configOutput.getProps().isEmpty());

    // Verify independently.
    Configuration config = LockssDaemon.getLockssDaemon().getPluginManager()
	.getStoredAuConfiguration(UNKNOWN_AUID);

    assertTrue(config.keySet().isEmpty());

    getAuConfigCommonTest();

    if (logger.isDebugEnabled()) logger.debug("Done.");
  }

  /**
   * Runs the getAuConfig()-related authenticated-specific tests.
   */
  private void getAuConfigAuthenticatedTest() {
    if (logger.isDebugEnabled()) logger.debug("Invoked.");

    // No AUId.
    runTestGetAuConfig(null, null, null, HttpStatus.UNAUTHORIZED);

    // Empty AUId.
    runTestGetAuConfig(EMPTY_STRING, null, null, HttpStatus.UNAUTHORIZED);

    // No credentials.
    runTestGetAuConfig(GOOD_AUID, null, null, HttpStatus.UNAUTHORIZED);
    runTestGetAuConfig(UNKNOWN_AUID, null, null, HttpStatus.UNAUTHORIZED);

    // Bad credentials.
    runTestGetAuConfig(GOOD_AUID, BAD_USER, BAD_PWD, HttpStatus.UNAUTHORIZED);
    runTestGetAuConfig(UNKNOWN_AUID, BAD_USER, BAD_PWD,
	HttpStatus.UNAUTHORIZED);

    getAuConfigCommonTest();

    if (logger.isDebugEnabled()) logger.debug("Done.");
  }

  /**
   * Runs the getAuConfig()-related authentication-independent tests.
   */
  private void getAuConfigCommonTest() {
    if (logger.isDebugEnabled()) logger.debug("Invoked.");

    // No AUId.
    runTestGetAuConfig(null, GOOD_USER, GOOD_PWD, HttpStatus.NOT_FOUND);

    // Empty AUId.
    runTestGetAuConfig(EMPTY_STRING, GOOD_USER, GOOD_PWD, HttpStatus.NOT_FOUND);

    // Good AUId.
    ConfigExchange configOutput =
	runTestGetAuConfig(GOOD_AUID, GOOD_USER, GOOD_PWD, HttpStatus.OK);

    // Verify.
    verifyGoodAuConfig(configOutput.getProps());

    // Non-existent AUId.
    configOutput =
	runTestGetAuConfig(UNKNOWN_AUID, GOOD_USER, GOOD_PWD, HttpStatus.OK);

    // Verify.
    assertTrue(configOutput.getProps().isEmpty());

    // Verify independently.
    Configuration config = LockssDaemon.getLockssDaemon().getPluginManager()
	.getStoredAuConfiguration(UNKNOWN_AUID);

    assertTrue(config.keySet().isEmpty());

    if (logger.isDebugEnabled()) logger.debug("Done.");
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
   * @return a ConfigExchange with the Archival Unit configuration.
   */
  private ConfigExchange runTestGetAuConfig(String auId, String user,
      String password, HttpStatus expectedStatus) {
    if (logger.isDebugEnabled()) {
      logger.debug("auId = " + auId);
      logger.debug("user = " + user);
      logger.debug("password = " + password);
      logger.debug("expectedStatus = " + expectedStatus);
    }

    // Get the test URL template.
    String template = getTestUrlTemplate("/aus/{auid}");

    // Create the URI of the request to the REST service.
    UriComponents uriComponents = UriComponentsBuilder.fromUriString(template)
	.build().expand(Collections.singletonMap("auid", auId));

    URI uri = UriComponentsBuilder.newInstance().uriComponents(uriComponents)
	.build().encode().toUri();
    if (logger.isDebugEnabled()) logger.debug("uri = " + uri);

    // Initialize the request to the REST service.
    RestTemplate restTemplate = new RestTemplate();

    HttpEntity<ConfigExchange> requestEntity = null;

    // Check whether there are any custom headers to be specified in the
    // request.
    if (user != null || password != null) {

      // Initialize the request headers.
      HttpHeaders headers = new HttpHeaders();

      // Set up the authentication credentials, if necessary.
      setUpCredentials(user, password, headers);

      if (logger.isDebugEnabled())
	logger.debug("requestHeaders = " + headers.toSingleValueMap());

      // Create the request entity.
      requestEntity = new HttpEntity<ConfigExchange>(null, headers);
    }

    // Make the request and get the response. 
    ResponseEntity<ConfigExchange> response =
	new TestRestTemplate(restTemplate). exchange(uri, HttpMethod.GET,
	    requestEntity, ConfigExchange.class);

    // Get the response status.
    HttpStatus statusCode = response.getStatusCode();
    assertEquals(expectedStatus, statusCode);

    return response.getBody();
  }

  /**
   * Runs the getAllAuConfig()-related un-authenticated-specific tests.
   */
  private void getAllAuConfigUnAuthenticatedTest() {
    if (logger.isDebugEnabled()) logger.debug("Invoked.");

    // No credentials.
    ConfigExchange configOutput =
	runTestGetAllAuConfig(null, null, HttpStatus.OK);

    // The configuration key for the good AU.
    String configKey = PluginManager.configKeyFromAuId(GOOD_AUID);

    // Verify.
    Map<String, String> allAuProps = configOutput.getProps();
    assertEquals(6, allAuProps.size());
    assertEquals("2014", allAuProps.get(configKey + ".au_oai_date"));
    assertEquals("biorisk", allAuProps.get(configKey + ".au_oai_set"));
    assertEquals("BioRisk Volume 2014",
	allAuProps.get(configKey + ".reserved.displayName"));
    assertEquals("false", allAuProps.get(configKey + ".reserved.disabled"));
    assertEquals("http://biorisk.pensoft.net/",
	allAuProps.get(configKey + ".base_url"));
    assertEquals("local:./cache",
	allAuProps.get(configKey + ".reserved.repository"));

    // Verify independently.
    Configuration config = ConfigManager.getCurrentConfig()
	  .getConfigTree(PluginManager.PARAM_AU_TREE);

    assertEquals(config.keySet().size(), allAuProps.size());

    for (String key : config.keySet()) {
      assertEquals(config.get(key), allAuProps.get(key));
    }

    // Bad credentials.
    configOutput = runTestGetAllAuConfig(BAD_USER, BAD_PWD, HttpStatus.OK);

    // Verify.
    assertEquals(allAuProps, configOutput.getProps());

    // Verify independently.
    config = ConfigManager.getCurrentConfig()
	  .getConfigTree(PluginManager.PARAM_AU_TREE);

    assertEquals(config.keySet().size(), configOutput.getProps().size());

    for (String key : config.keySet()) {
      assertEquals(config.get(key), allAuProps.get(key));
    }

    getAllAuConfigCommonTest();

    if (logger.isDebugEnabled()) logger.debug("Done.");
  }

  /**
   * Runs the getAllAuConfig()-related authenticated-specific tests.
   */
  private void getAllAuConfigAuthenticatedTest() {
    if (logger.isDebugEnabled()) logger.debug("Invoked.");

    // No credentials.
    runTestGetAllAuConfig(null, null, HttpStatus.UNAUTHORIZED);

    // Bad credentials.
    runTestGetAllAuConfig(BAD_USER, BAD_PWD, HttpStatus.UNAUTHORIZED);

    getAllAuConfigCommonTest();

    if (logger.isDebugEnabled()) logger.debug("Done.");
  }

  /**
   * Runs the getAllAuConfig()-related authentication-independent tests.
   */
  private void getAllAuConfigCommonTest() {
    if (logger.isDebugEnabled()) logger.debug("Invoked.");

    ConfigExchange configOutput =
	runTestGetAllAuConfig(GOOD_USER, GOOD_PWD, HttpStatus.OK);

    String configKey = PluginManager.configKeyFromAuId(GOOD_AUID);

    // Verify.
    Map<String, String> allAuProps = configOutput.getProps();
    assertEquals(6, allAuProps.size());
    assertEquals("2014", allAuProps.get(configKey + ".au_oai_date"));
    assertEquals("biorisk", allAuProps.get(configKey + ".au_oai_set"));
    assertEquals("BioRisk Volume 2014",
	allAuProps.get(configKey + ".reserved.displayName"));
    assertEquals("false", allAuProps.get(configKey + ".reserved.disabled"));
    assertEquals("http://biorisk.pensoft.net/",
	allAuProps.get(configKey + ".base_url"));
    assertEquals("local:./cache",
	allAuProps.get(configKey + ".reserved.repository"));

    // Verify independently.
    Configuration config = ConfigManager.getCurrentConfig()
	  .getConfigTree(PluginManager.PARAM_AU_TREE);

    assertEquals(config.keySet().size(), allAuProps.size());

    for (String key : config.keySet()) {
      assertEquals(config.get(key), allAuProps.get(key));
    }

    if (logger.isDebugEnabled()) logger.debug("Done.");
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
   * @return a ConfigExchange with the Archival Unit configuration.
   */
  private ConfigExchange runTestGetAllAuConfig(String user, String password,
      HttpStatus expectedStatus) {
    if (logger.isDebugEnabled()) {
      logger.debug("user = " + user);
      logger.debug("password = " + password);
      logger.debug("expectedStatus = " + expectedStatus);
    }

    // Get the test URL template.
    String template = getTestUrlTemplate("/aus");

    // Create the URI of the request to the REST service.
    UriComponents uriComponents =
	UriComponentsBuilder.fromUriString(template).build();

    URI uri = UriComponentsBuilder.newInstance().uriComponents(uriComponents)
	.build().encode().toUri();
    if (logger.isDebugEnabled()) logger.debug("uri = " + uri);

    // Initialize the request to the REST service.
    RestTemplate restTemplate = new RestTemplate();

    HttpEntity<ConfigExchange> requestEntity = null;

    // Check whether there are any custom headers to be specified in the
    // request.
    if (user != null || password != null) {

      // Initialize the request headers.
      HttpHeaders headers = new HttpHeaders();

      // Set up the authentication credentials, if necessary.
      setUpCredentials(user, password, headers);

      if (logger.isDebugEnabled())
	logger.debug("requestHeaders = " + headers.toSingleValueMap());

      // Create the request entity.
      requestEntity = new HttpEntity<ConfigExchange>(null, headers);
    }

    // Make the request and get the response. 
    ResponseEntity<ConfigExchange> response =
	new TestRestTemplate(restTemplate). exchange(uri, HttpMethod.GET,
	    requestEntity, ConfigExchange.class);

    // Get the response status.
    HttpStatus statusCode = response.getStatusCode();
    assertEquals(expectedStatus, statusCode);

    return response.getBody();
  }

  /**
   * Runs the deleteAus()-related un-authenticated-specific tests.
   */
  private void deleteAusUnAuthenticatedTest() {
    if (logger.isDebugEnabled()) logger.debug("Invoked.");

    // Get the current configuration.
    ConfigExchange backupConfig =
	runTestGetAuConfig(GOOD_AUID, null, null, HttpStatus.OK);

    // Verify.
    Map<String, String> backupProps = backupConfig.getProps();
    verifyGoodAuConfig(backupProps);

    // No credentials.
    ConfigExchange result =
	runTestDeleteAus(GOOD_AUID, null, null, HttpStatus.OK);

    // Verify.
    Map<String, String> props = result.getProps();
    assertEquals(backupProps, props);

    // Bad credentials.
    result = runTestDeleteAus(GOOD_AUID, BAD_USER, BAD_PWD, HttpStatus.OK);

    // Verify.
    assertTrue(result.getProps().keySet().isEmpty());

    // Restore the original configuration.
    props = runTestPutAuConfig(backupConfig, GOOD_AUID,
	MediaType.APPLICATION_JSON, GOOD_USER, GOOD_PWD,
	HttpStatus.OK).getProps();

    // Verify.
    assertEquals(backupProps, props);

    deleteAusCommonTest();

    if (logger.isDebugEnabled()) logger.debug("Done.");
  }

  /**
   * Runs the deleteAus()-related authenticated-specific tests.
   */
  private void deleteAusAuthenticatedTest() {
    if (logger.isDebugEnabled()) logger.debug("Invoked.");

    // No AUId.
    runTestDeleteAus(null, null, null, HttpStatus.UNAUTHORIZED);

    // Empty AUId.
    runTestDeleteAus(EMPTY_STRING, null, null, HttpStatus.UNAUTHORIZED);

    // No credentials.
    runTestDeleteAus(GOOD_AUID, null, null, HttpStatus.UNAUTHORIZED);
    runTestDeleteAus(UNKNOWN_AUID, null, null, HttpStatus.UNAUTHORIZED);

    // Bad credentials.
    runTestDeleteAus(GOOD_AUID, BAD_USER, BAD_PWD, HttpStatus.UNAUTHORIZED);
    runTestDeleteAus(UNKNOWN_AUID, BAD_USER, BAD_PWD, HttpStatus.UNAUTHORIZED);

    deleteAusCommonTest();

    if (logger.isDebugEnabled()) logger.debug("Done.");
  }

  /**
   * Runs the deleteAus()-related authenticated-independent tests.
   */
  private void deleteAusCommonTest() {
    if (logger.isDebugEnabled()) logger.debug("Invoked.");

    // No AUId.
    runTestDeleteAus(null, GOOD_USER, GOOD_PWD, HttpStatus.NOT_FOUND);

    // Empty AUId.
    runTestDeleteAus(EMPTY_STRING, GOOD_USER, GOOD_PWD, HttpStatus.NOT_FOUND);

    // Unknown AU.
    ConfigExchange configOutput =
	runTestDeleteAus(UNKNOWN_AUID, GOOD_USER, GOOD_PWD, HttpStatus.OK);

    // Verify.
    assertTrue(configOutput.getProps().keySet().isEmpty());

    // Get the current configuration of the good AU.
    ConfigExchange backupConfig =
	runTestGetAuConfig(GOOD_AUID, GOOD_USER, GOOD_PWD, HttpStatus.OK);

    // Verify.
    Map<String, String> backupProps = backupConfig.getProps();
    verifyGoodAuConfig(backupProps);

    // Delete the AU.
    ConfigExchange result =
	runTestDeleteAus(GOOD_AUID, GOOD_USER, GOOD_PWD, HttpStatus.OK);
    Map<String, String> props = result.getProps();

    // Verify.
    assertEquals(backupProps, props);

    // Restore the original configuration.
    props = runTestPutAuConfig(backupConfig, GOOD_AUID,
	MediaType.APPLICATION_JSON, GOOD_USER, GOOD_PWD,
	HttpStatus.OK).getProps();

    // Verify.
    assertEquals(backupProps, props);

    // Verify independently.
    Configuration config = LockssDaemon.getLockssDaemon().getPluginManager()
	.getStoredAuConfiguration(GOOD_AUID);

    assertEquals(config.keySet().size(), props.size());

    for (String key : config.keySet()) {
      assertEquals(config.get(key), props.get(key));
    }

    if (logger.isDebugEnabled()) logger.debug("Done.");
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
   * @return a ConfigExchange with the configuration of the Archival Unit that
   *         was deleted.
   */
  private ConfigExchange runTestDeleteAus(String auId, String user,
      String password, HttpStatus expectedStatus) {
    if (logger.isDebugEnabled()) {
      logger.debug("auId = " + auId);
      logger.debug("user = " + user);
      logger.debug("password = " + password);
      logger.debug("expectedStatus = " + expectedStatus);
    }

    // Get the test URL template.
    String template = getTestUrlTemplate("/aus/{auid}");

    // Create the URI of the request to the REST service.
    UriComponents uriComponents = UriComponentsBuilder.fromUriString(template)
	.build().expand(Collections.singletonMap("auid", auId));

    URI uri = UriComponentsBuilder.newInstance().uriComponents(uriComponents)
	.build().encode().toUri();
    if (logger.isDebugEnabled()) logger.debug("uri = " + uri);

    // Initialize the request to the REST service.
    RestTemplate restTemplate = new RestTemplate();

    HttpEntity<ConfigExchange> requestEntity = null;

    // Check whether there are any custom headers to be specified in the
    // request.
    if (user != null || password != null) {

      // Initialize the request headers.
      HttpHeaders headers = new HttpHeaders();

      // Set up the authentication credentials, if necessary.
      setUpCredentials(user, password, headers);

      if (logger.isDebugEnabled())
	logger.debug("requestHeaders = " + headers.toSingleValueMap());

      // Create the request entity.
      requestEntity = new HttpEntity<ConfigExchange>(null, headers);
    }

    // Make the request and get the response. 
    ResponseEntity<ConfigExchange> response =
	new TestRestTemplate(restTemplate). exchange(uri, HttpMethod.DELETE,
	    requestEntity, ConfigExchange.class);

    // Get the response status.
    HttpStatus statusCode = response.getStatusCode();
    assertEquals(expectedStatus, statusCode);

    if (isSuccess(statusCode)) {
      // Verify independently.
      Configuration config = LockssDaemon.getLockssDaemon().getPluginManager()
	.getStoredAuConfiguration(auId);

      assertTrue(config.keySet().isEmpty());
    }

    return response.getBody();
  }

  /**
   * Verifies that the passed configuration matches that of the known good
   * Archival Unit.
   * 
   * @param auConfig
   *          A Map<String, String> with the configuration to be verified.
   */
  private void verifyGoodAuConfig(Map<String, String> auConfig) {
    assertEquals(6, auConfig.size());
    assertEquals("2014", auConfig.get("au_oai_date"));
    assertEquals("biorisk", auConfig.get("au_oai_set"));
    assertEquals("BioRisk Volume 2014", auConfig.get("reserved.displayName"));
    assertEquals("false", auConfig.get("reserved.disabled"));
    assertEquals("http://biorisk.pensoft.net/", auConfig.get("base_url"));
    assertEquals("local:./cache", auConfig.get("reserved.repository"));

    // Verify independently.
    Configuration config = LockssDaemon.getLockssDaemon().getPluginManager()
	.getStoredAuConfiguration(GOOD_AUID);

    assertEquals(config.keySet().size(), auConfig.size());

    for (String key : config.keySet()) {
      assertEquals(config.get(key), auConfig.get(key));
    }
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
      .encodeToString(credentials.getBytes(Charset.forName("US-ASCII")));

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
