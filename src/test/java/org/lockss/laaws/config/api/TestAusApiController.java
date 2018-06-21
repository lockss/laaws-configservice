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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
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
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Test class for org.lockss.laaws.config.api.AusApiController.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class TestAusApiController extends SpringLockssTestCase {
  private static final Logger logger =
      LoggerFactory.getLogger(TestAusApiController.class);

  // The port that Tomcat is using during this test.
  @LocalServerPort
  private int port;

  // The application Context used to specify the command line arguments to be
  // used for the tests.
  @Autowired
  ApplicationContext appCtx;

  // The identifier of an AU that exists in the test system.
  String goodAuid = "org|lockss|plugin|pensoft|oai|PensoftOaiPlugin"
      + "&au_oai_date~2014&au_oai_set~biorisk"
      + "&base_url~http%3A%2F%2Fbiorisk%2Epensoft%2Enet%2F";

  // The name of an AU that exists in the test system.
  String goodAuName = "BioRisk Volume 2014";

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
    cmdLineArgs.add("test/config/ausApiControllerTestAuthOff.opt");

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
    cmdLineArgs.add("test/config/ausApiControllerTestAuthOn.opt");

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

    String template = getTestUrlTemplate("/aus/{auid}");

    // Create the URI of the request to the REST service.
    UriComponents uriComponents = UriComponentsBuilder.fromUriString(template)
	.build().expand(Collections.singletonMap("auid", goodAuid));

    URI uri = UriComponentsBuilder.newInstance().uriComponents(uriComponents)
	.build().encode().toUri();
    if (logger.isDebugEnabled()) logger.debug("uri = " + uri);

    ResponseEntity<String> errorResponse = new TestRestTemplate().exchange(uri,
	HttpMethod.PUT, null, String.class);

    HttpStatus statusCode = errorResponse.getStatusCode();
    assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, statusCode);

    errorResponse = new TestRestTemplate("fakeUser", "fakePassword")
	.exchange(uri, HttpMethod.PUT, null, String.class);

    statusCode = errorResponse.getStatusCode();
    assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, statusCode);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    errorResponse = new TestRestTemplate().exchange(uri, HttpMethod.PUT,
	new HttpEntity<String>(null, headers), String.class);

    statusCode = errorResponse.getStatusCode();
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, statusCode);

    headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    errorResponse = new TestRestTemplate("fakeUser", "fakePassword")
	.exchange(uri, HttpMethod.PUT, new HttpEntity<String>(null, headers),
	    String.class);

    statusCode = errorResponse.getStatusCode();
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, statusCode);

    putAuConfigCommonTest();

    if (logger.isDebugEnabled()) logger.debug("Done.");
  }

  /**
   * Runs the putAuConfig()-related authenticated-specific tests.
   */
  private void putAuConfigAuthenticatedTest() {
    if (logger.isDebugEnabled()) logger.debug("Invoked.");

    String template = getTestUrlTemplate("/aus/{auid}");

    // Create the URI of the request to the REST service.
    UriComponents uriComponents = UriComponentsBuilder.fromUriString(template)
	.build().expand(Collections.singletonMap("auid", goodAuid));

    URI uri = UriComponentsBuilder.newInstance().uriComponents(uriComponents)
	.build().encode().toUri();
    if (logger.isDebugEnabled()) logger.debug("uri = " + uri);

    ResponseEntity<String> errorResponse = new TestRestTemplate().exchange(uri,
	HttpMethod.PUT, null, String.class);

    HttpStatus statusCode = errorResponse.getStatusCode();
    assertEquals(HttpStatus.UNAUTHORIZED, statusCode);

    errorResponse = new TestRestTemplate("fakeUser", "fakePassword")
	.exchange(uri, HttpMethod.PUT, null, String.class);

    statusCode = errorResponse.getStatusCode();
    assertEquals(HttpStatus.UNAUTHORIZED, statusCode);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    errorResponse = new TestRestTemplate().exchange(uri, HttpMethod.PUT,
	new HttpEntity<String>(null, headers), String.class);

    statusCode = errorResponse.getStatusCode();
    assertEquals(HttpStatus.UNAUTHORIZED, statusCode);

    headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    errorResponse = new TestRestTemplate("fakeUser", "fakePassword")
	.exchange(uri, HttpMethod.PUT, new HttpEntity<String>(null, headers),
	    String.class);

    statusCode = errorResponse.getStatusCode();
    assertEquals(HttpStatus.UNAUTHORIZED, statusCode);

    putAuConfigCommonTest();

    if (logger.isDebugEnabled()) logger.debug("Done.");
  }

  /**
   * Runs the putAuConfig()-related authentication-independent tests.
   */
  private void putAuConfigCommonTest() {
    if (logger.isDebugEnabled()) logger.debug("Invoked.");

    ConfigExchange backupConfig = getAuConfig(HttpStatus.OK);
    Map<String, String> backupProps = backupConfig.getProps();
    assertEquals("2014", backupProps.get("au_oai_date"));
    assertEquals("biorisk", backupProps.get("au_oai_set"));
    assertNull(backupProps.get("testKey1"));
    assertNull(backupProps.get("testKey2"));

    String template = getTestUrlTemplate("/aus/{auid}");

    // Create the URI of the request to the REST service.
    UriComponents uriComponents = UriComponentsBuilder.fromUriString(template)
	.build().expand(Collections.singletonMap("auid", goodAuid));

    URI uri = UriComponentsBuilder.newInstance().uriComponents(uriComponents)
	.build().encode().toUri();
    if (logger.isDebugEnabled()) logger.debug("uri = " + uri);

    ResponseEntity<String> errorResponse = new TestRestTemplate("lockss-u",
	"lockss-p").exchange(uri, HttpMethod.PUT, null, String.class);

    HttpStatus statusCode = errorResponse.getStatusCode();
    assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, statusCode);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    errorResponse = new TestRestTemplate("lockss-u", "lockss-p").exchange(uri,
	HttpMethod.PUT, new HttpEntity<String>(null, headers), String.class);

    statusCode = errorResponse.getStatusCode();
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, statusCode);

    Map<String, String> props = new HashMap<String, String>();
    props.put("testKey1", "testValue1");
    props.put("testKey2", "testValue2");

    ConfigExchange configInput = new ConfigExchange();
    configInput.setProps(props);

    Map<String, String> result =
	putAuConfig(uri, configInput, HttpStatus.OK).getProps();
    assertNull(result.get("journal_id"));
    assertNull(result.get("volume_name"));
    assertEquals("testValue1", result.get("testKey1"));
    assertEquals("testValue2", result.get("testKey2"));

    result = putAuConfig(uri, backupConfig, HttpStatus.OK).getProps();
    assertEquals("2014", backupProps.get("au_oai_date"));
    assertEquals("biorisk", backupProps.get("au_oai_set"));
    assertNull(result.get("testKey1"));
    assertNull(result.get("testKey2"));

    // Create the URI of the request to the REST service.
    uriComponents = UriComponentsBuilder.fromUriString(template).build()
	.expand(Collections.singletonMap("auid", "fakeauid"));

    uri = UriComponentsBuilder.newInstance().uriComponents(uriComponents)
	.build().encode().toUri();
    if (logger.isDebugEnabled()) logger.debug("uri = " + uri);

    result = putAuConfig(uri, configInput, HttpStatus.OK).getProps();
    assertEquals("testValue1", result.get("testKey1"));
    assertEquals("testValue2", result.get("testKey2"));

    result = putAuConfig(uri, new ConfigExchange(), HttpStatus.OK).getProps();
    assertTrue(result.keySet().isEmpty());

    ResponseEntity<ConfigExchange> successResponse =
	new TestRestTemplate("lockss-u", "lockss-p").exchange(uri,
	    HttpMethod.DELETE, new HttpEntity<String>(null, headers),
	    ConfigExchange.class);

    statusCode = successResponse.getStatusCode();
    assertEquals(HttpStatus.OK, statusCode);

    result = successResponse.getBody().getProps();
    assertTrue(result.keySet().isEmpty());

    if (logger.isDebugEnabled()) logger.debug("Done.");
  }

  /**
   * Performs a PUT operation.
   * 
   * @param uri
   *          A URI with the URI of the operation.
   * @param config
   *          A ConfigExchange with the Archival Unit configuration.
   * @param expectedStatus
   *          An HttpStatus with the HTTP status of the result.
   * @return a ConfigExchange with the Archival Unit configuration.
   */
  private ConfigExchange putAuConfig(URI uri, ConfigExchange config,
      HttpStatus expectedStatus) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    ResponseEntity<ConfigExchange> response =
	new TestRestTemplate("lockss-u", "lockss-p").exchange(uri,
	    HttpMethod.PUT, new HttpEntity<ConfigExchange>(config, headers),
	    ConfigExchange.class);

    HttpStatus statusCode = response.getStatusCode();
    assertEquals(expectedStatus, statusCode);

    return response.getBody();
  }

  /**
   * Runs the getAuConfig()-related un-authenticated-specific tests.
   */
  private void getAuConfigUnAuthenticatedTest() {
    if (logger.isDebugEnabled()) logger.debug("Invoked.");

    String template = getTestUrlTemplate("/aus/{auid}");

    // Create the URI of the request to the REST service.
    UriComponents uriComponents = UriComponentsBuilder.fromUriString(template)
	.build().expand(Collections.singletonMap("auid", goodAuid));

    URI uri = UriComponentsBuilder.newInstance().uriComponents(uriComponents)
	.build().encode().toUri();
    if (logger.isDebugEnabled()) logger.debug("uri = " + uri);

    ResponseEntity<ConfigExchange> successResponse = new TestRestTemplate()
	.exchange(uri, HttpMethod.GET, null, ConfigExchange.class);

    HttpStatus statusCode = successResponse.getStatusCode();
    assertEquals(HttpStatus.OK, statusCode);

    ConfigExchange configOutput = successResponse.getBody();
    assertEquals("2014", configOutput.getProps().get("au_oai_date"));
    assertEquals("biorisk", configOutput.getProps().get("au_oai_set"));

    successResponse = new TestRestTemplate("fakeUser", "fakePassword")
	.exchange(uri, HttpMethod.GET, null, ConfigExchange.class);

    statusCode = successResponse.getStatusCode();
    assertEquals(HttpStatus.OK, statusCode);

    configOutput = successResponse.getBody();
    assertEquals("2014", configOutput.getProps().get("au_oai_date"));
    assertEquals("biorisk", configOutput.getProps().get("au_oai_set"));

    getAuConfigCommonTest();

    if (logger.isDebugEnabled()) logger.debug("Done.");
  }

  /**
   * Runs the getAuConfig()-related authenticated-specific tests.
   */
  private void getAuConfigAuthenticatedTest() {
    if (logger.isDebugEnabled()) logger.debug("Invoked.");

    String template = getTestUrlTemplate("/aus/{auid}");

    // Create the URI of the request to the REST service.
    UriComponents uriComponents = UriComponentsBuilder.fromUriString(template)
	.build().expand(Collections.singletonMap("auid", goodAuid));

    URI uri = UriComponentsBuilder.newInstance().uriComponents(uriComponents)
	.build().encode().toUri();
    if (logger.isDebugEnabled()) logger.debug("uri = " + uri);

    ResponseEntity<ConfigExchange> errorResponse = new TestRestTemplate()
	.exchange(uri, HttpMethod.GET, null, ConfigExchange.class);

    HttpStatus statusCode = errorResponse.getStatusCode();
    assertEquals(HttpStatus.UNAUTHORIZED, statusCode);

    errorResponse = new TestRestTemplate("fakeUser", "fakePassword")
	.exchange(uri, HttpMethod.GET, null, ConfigExchange.class);

    statusCode = errorResponse.getStatusCode();
    assertEquals(HttpStatus.UNAUTHORIZED, statusCode);

    getAuConfigCommonTest();

    if (logger.isDebugEnabled()) logger.debug("Done.");
  }

  /**
   * Runs the getAuConfig()-related authentication-independent tests.
   */
  private void getAuConfigCommonTest() {
    if (logger.isDebugEnabled()) logger.debug("Invoked.");

    String template = getTestUrlTemplate("/aus/{auid}");

    // Create the URI of the request to the REST service.
    UriComponents uriComponents = UriComponentsBuilder.fromUriString(template)
	.build().expand(Collections.singletonMap("auid", goodAuid));

    URI uri = UriComponentsBuilder.newInstance().uriComponents(uriComponents)
	.build().encode().toUri();
    if (logger.isDebugEnabled()) logger.debug("uri = " + uri);

    ResponseEntity<ConfigExchange> successResponse =
	new TestRestTemplate("lockss-u", "lockss-p").exchange(uri,
	    HttpMethod.GET, null, ConfigExchange.class);

    HttpStatus statusCode = successResponse.getStatusCode();
    assertEquals(HttpStatus.OK, statusCode);

    ConfigExchange configOutput = successResponse.getBody();
    assertEquals("2014", configOutput.getProps().get("au_oai_date"));
    assertEquals("biorisk", configOutput.getProps().get("au_oai_set"));

    configOutput = getAuConfig(HttpStatus.OK);

    assertEquals("2014", configOutput.getProps().get("au_oai_date"));
    assertEquals("biorisk", configOutput.getProps().get("au_oai_set"));

    // Create the URI of the request to the REST service.
    uriComponents = UriComponentsBuilder.fromUriString(template).build()
	.expand(Collections.singletonMap("auid", "fakeauid"));

    uri = UriComponentsBuilder.newInstance().uriComponents(uriComponents)
	.build().encode().toUri();
    if (logger.isDebugEnabled()) logger.debug("uri = " + uri);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    successResponse = new TestRestTemplate("lockss-u", "lockss-p")
	.exchange(uri, HttpMethod.GET, new HttpEntity<String>(null, headers),
	    ConfigExchange.class);

    statusCode = successResponse.getStatusCode();
    assertEquals(HttpStatus.OK, statusCode);

    configOutput = successResponse.getBody();
    assertTrue(configOutput.getProps().isEmpty());

    if (logger.isDebugEnabled()) logger.debug("Done.");
  }

  /**
   * Performs a GET operation for an Archival Unit.
   * 
   * @param expectedStatus
   *          An HttpStatus with the HTTP status of the result.
   * @return a ConfigExchange with the Archival Unit configuration.
   */
  private ConfigExchange getAuConfig(HttpStatus expectedStatus) {
    String template = getTestUrlTemplate("/aus/{auid}");

    // Create the URI of the request to the REST service.
    UriComponents uriComponents = UriComponentsBuilder.fromUriString(template)
	.build().expand(Collections.singletonMap("auid", goodAuid));

    URI uri = UriComponentsBuilder.newInstance().uriComponents(uriComponents)
	.build().encode().toUri();
    if (logger.isDebugEnabled()) logger.debug("uri = " + uri);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    ResponseEntity<ConfigExchange> response = new TestRestTemplate("lockss-u",
	"lockss-p").exchange(uri, HttpMethod.GET,
	    new HttpEntity<String>(null, headers), ConfigExchange.class);

    HttpStatus statusCode = response.getStatusCode();
    assertEquals(expectedStatus, statusCode);

    return response.getBody();
  }

  /**
   * Runs the getAllAuConfig()-related un-authenticated-specific tests.
   */
  private void getAllAuConfigUnAuthenticatedTest() {
    if (logger.isDebugEnabled()) logger.debug("Invoked.");

    String url = getTestUrlTemplate("/aus");

    ResponseEntity<ConfigExchange> successResponse = new TestRestTemplate()
	.exchange(url, HttpMethod.GET, null, ConfigExchange.class);

    HttpStatus statusCode = successResponse.getStatusCode();
    assertEquals(HttpStatus.OK, statusCode);

    String configKey = PluginManager.configKeyFromAuId(goodAuid);

    Map<String, String> result = successResponse.getBody().getProps();
    assertEquals("2014", result.get(configKey + ".au_oai_date"));
    assertEquals("biorisk", result.get(configKey + ".au_oai_set"));

    successResponse = new TestRestTemplate("fakeUser", "fakePassword")
	.exchange(url, HttpMethod.GET, null, ConfigExchange.class);

    statusCode = successResponse.getStatusCode();
    assertEquals(HttpStatus.OK, statusCode);

    result = successResponse.getBody().getProps();
    assertEquals("2014", result.get(configKey + ".au_oai_date"));
    assertEquals("biorisk", result.get(configKey + ".au_oai_set"));

    getAllAuConfigCommonTest();

    if (logger.isDebugEnabled()) logger.debug("Done.");
  }

  /**
   * Runs the getAllAuConfig()-related authenticated-specific tests.
   */
  private void getAllAuConfigAuthenticatedTest() {
    if (logger.isDebugEnabled()) logger.debug("Invoked.");

    String url = getTestUrlTemplate("/aus");

    ResponseEntity<ConfigExchange> errorResponse = new TestRestTemplate()
	.exchange(url, HttpMethod.GET, null, ConfigExchange.class);

    HttpStatus statusCode = errorResponse.getStatusCode();
    assertEquals(HttpStatus.UNAUTHORIZED, statusCode);

    errorResponse = new TestRestTemplate("fakeUser", "fakePassword")
	.exchange(url, HttpMethod.GET, null, ConfigExchange.class);

    statusCode = errorResponse.getStatusCode();
    assertEquals(HttpStatus.UNAUTHORIZED, statusCode);

    getAllAuConfigCommonTest();

    if (logger.isDebugEnabled()) logger.debug("Done.");
  }

  /**
   * Runs the getAllAuConfig()-related authentication-independent tests.
   */
  private void getAllAuConfigCommonTest() {
    if (logger.isDebugEnabled()) logger.debug("Invoked.");

    String url = getTestUrlTemplate("/aus");

    ResponseEntity<ConfigExchange> successResponse =
	new TestRestTemplate("lockss-u", "lockss-p").exchange(url,
	    HttpMethod.GET, null, ConfigExchange.class);

    HttpStatus statusCode = successResponse.getStatusCode();
    assertEquals(HttpStatus.OK, statusCode);

    String configKey = PluginManager.configKeyFromAuId(goodAuid);

    Map<String, String> result = successResponse.getBody().getProps();
    assertEquals("2014", result.get(configKey + ".au_oai_date"));
    assertEquals("biorisk", result.get(configKey + ".au_oai_set"));

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    result = getAllAuConfig(HttpStatus.OK).getProps();
    assertEquals("2014", result.get(configKey + ".au_oai_date"));
    assertEquals("biorisk", result.get(configKey + ".au_oai_set"));

    if (logger.isDebugEnabled()) logger.debug("Done.");
  }

  /**
   * Performs a GET operation for all Archival Units.
   * 
   * @param expectedStatus
   *          An HttpStatus with the HTTP status of the result.
   * @return a ConfigExchange with the Archival Unit configuration.
   */
  private ConfigExchange getAllAuConfig(HttpStatus expectedStatus) {
    String url = getTestUrlTemplate("/aus");

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    ResponseEntity<ConfigExchange> response = new TestRestTemplate("lockss-u",
	"lockss-p").exchange(url, HttpMethod.GET,
	    new HttpEntity<String>(null, headers), ConfigExchange.class);

    HttpStatus statusCode = response.getStatusCode();
    assertEquals(expectedStatus, statusCode);

    return response.getBody();
  }

  /**
   * Runs the deleteAus()-related un-authenticated-specific tests.
   */
  private void deleteAusUnAuthenticatedTest() {
    if (logger.isDebugEnabled()) logger.debug("Invoked.");

    ConfigExchange backupConfig = getAuConfig(HttpStatus.OK);
    Map<String, String> backupProps = backupConfig.getProps();
    assertEquals("2014", backupProps.get("au_oai_date"));
    assertEquals("biorisk", backupProps.get("au_oai_set"));

    String template = getTestUrlTemplate("/aus/{auid}");

    // Create the URI of the request to the REST service.
    UriComponents uriComponents = UriComponentsBuilder.fromUriString(template)
	.build().expand(Collections.singletonMap("auid", goodAuid));

    URI uri = UriComponentsBuilder.newInstance().uriComponents(uriComponents)
	.build().encode().toUri();
    if (logger.isDebugEnabled()) logger.debug("uri = " + uri);

    ResponseEntity<String> errorResponse = new TestRestTemplate().exchange(uri,
	HttpMethod.DELETE, null, String.class);

    HttpStatus statusCode = errorResponse.getStatusCode();
    assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, statusCode);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    ResponseEntity<ConfigExchange> successResponse =
	new TestRestTemplate().exchange(uri, HttpMethod.DELETE,
	    new HttpEntity<String>(null, headers), ConfigExchange.class);

    statusCode = successResponse.getStatusCode();
    assertEquals(HttpStatus.OK, statusCode);

    ConfigExchange result = successResponse.getBody();
    Map<String, String> props = result.getProps();
    assertEquals(backupProps, props);

    headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    successResponse = new TestRestTemplate("fakeUser", "fakePassword")
	.exchange(uri, HttpMethod.DELETE, new HttpEntity<String>(null, headers),
	    ConfigExchange.class);

    statusCode = successResponse.getStatusCode();
    assertEquals(HttpStatus.OK, statusCode);

    result = successResponse.getBody();
    assertTrue(result.getProps().keySet().isEmpty());

    props = putAuConfig(uri, backupConfig, HttpStatus.OK).getProps();
    assertEquals("2014", props.get("au_oai_date"));
    assertEquals("biorisk", props.get("au_oai_set"));

    deleteAusCommonTest();

    if (logger.isDebugEnabled()) logger.debug("Done.");
  }

  /**
   * Runs the deleteAus()-related authenticated-specific tests.
   */
  private void deleteAusAuthenticatedTest() {
    if (logger.isDebugEnabled()) logger.debug("Invoked.");

    ConfigExchange backupConfig = getAuConfig(HttpStatus.OK);
    Map<String, String> backupProps = backupConfig.getProps();
    assertEquals("2014", backupProps.get("au_oai_date"));
    assertEquals("biorisk", backupProps.get("au_oai_set"));

    String template = getTestUrlTemplate("/aus/{auid}");

    // Create the URI of the request to the REST service.
    UriComponents uriComponents = UriComponentsBuilder.fromUriString(template)
	.build().expand(Collections.singletonMap("auid", goodAuid));

    URI uri = UriComponentsBuilder.newInstance().uriComponents(uriComponents)
	.build().encode().toUri();
    if (logger.isDebugEnabled()) logger.debug("uri = " + uri);

    ResponseEntity<String> errorResponse = new TestRestTemplate().exchange(uri,
	HttpMethod.DELETE, null, String.class);

    HttpStatus statusCode = errorResponse.getStatusCode();
    assertEquals(HttpStatus.UNAUTHORIZED, statusCode);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    errorResponse = new TestRestTemplate().exchange(uri, HttpMethod.DELETE,
	new HttpEntity<String>(null, headers), String.class);

    statusCode = errorResponse.getStatusCode();
    assertEquals(HttpStatus.UNAUTHORIZED, statusCode);

    headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    errorResponse = new TestRestTemplate("fakeUser", "fakePassword")
	.exchange(uri, HttpMethod.DELETE, new HttpEntity<String>(null, headers),
	    String.class);

    statusCode = errorResponse.getStatusCode();
    assertEquals(HttpStatus.UNAUTHORIZED, statusCode);

    ResponseEntity<ConfigExchange> successResponse =
	new TestRestTemplate("lockss-u", "lockss-p").exchange(uri,
	    HttpMethod.DELETE, new HttpEntity<String>(null, headers),
	    ConfigExchange.class);

    statusCode = successResponse.getStatusCode();
    assertEquals(HttpStatus.OK, statusCode);

    ConfigExchange result = successResponse.getBody();
    Map<String, String> props = result.getProps();
    assertEquals(backupProps, props);

    headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    successResponse = new TestRestTemplate("lockss-u", "lockss-p").exchange(uri,
	HttpMethod.DELETE, new HttpEntity<String>(null, headers),
	ConfigExchange.class);

    statusCode = successResponse.getStatusCode();
    assertEquals(HttpStatus.OK, statusCode);

    result = successResponse.getBody();
    assertTrue(result.getProps().keySet().isEmpty());

    props = putAuConfig(uri, backupConfig, HttpStatus.OK).getProps();
    assertEquals("2014", props.get("au_oai_date"));
    assertEquals("biorisk", props.get("au_oai_set"));

    deleteAusCommonTest();

    if (logger.isDebugEnabled()) logger.debug("Done.");
  }

  /**
   * Runs the deleteAus()-related authenticated-independent tests.
   */
  private void deleteAusCommonTest() {
    if (logger.isDebugEnabled()) logger.debug("Invoked.");

    String url = getTestUrlTemplate("/aus/fakeauid");

    ResponseEntity<String> errorResponse = new TestRestTemplate("lockss-u",
	"lockss-p").exchange(url, HttpMethod.DELETE, null, String.class);

    HttpStatus statusCode = errorResponse.getStatusCode();
    assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, statusCode);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    ResponseEntity<ConfigExchange> successResponse =
	new TestRestTemplate("lockss-u", "lockss-p").exchange(url,
	    HttpMethod.DELETE, new HttpEntity<String>(null, headers),
	    ConfigExchange.class);

    statusCode = successResponse.getStatusCode();
    assertEquals(HttpStatus.OK, statusCode);

    ConfigExchange result = successResponse.getBody();
    assertTrue(result.getProps().keySet().isEmpty());

    if (logger.isDebugEnabled()) logger.debug("Done.");
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