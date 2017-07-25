/*

 Copyright (c) 2017 Board of Trustees of Leland Stanford Jr. University,
 all rights reserved.

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in
 all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL
 STANFORD UNIVERSITY BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

 Except as contained in this notice, the name of Stanford University shall not
 be used in advertising or otherwise to promote the sale, use or other dealings
 in this Software without prior written authorization from Stanford University.

 */
package org.lockss.laaws.config.api;

import static org.junit.Assert.*;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.lockss.laaws.config.model.ConfigExchange;
import org.lockss.plugin.PluginManager;
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

/**
 * Test class for org.lockss.laaws.config.api.AusApiController.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AusApiControllerTest {
  private static final Logger logger =
      LoggerFactory.getLogger(AusApiControllerTest.class);

  // The port that Tomcat is using during this test.
  @LocalServerPort
  private int port;

  // The application Context used to specify the command line arguments to be
  // used for the tests.
  @Autowired
  ApplicationContext appCtx;

  String goodAuid = "org|lockss|plugin|taylorandfrancis|"
      + "TaylorAndFrancisPlugin&base_url~http%3A%2F%2Fwww%2Etandfonline"
      + "%2Ecom%2F&journal_id~rafr20&volume_name~8";

  String goodAuName = "Africa Review Volume 8";
  /**
   * Runs the tests with authentication turned off.
   * 
   * @throws Exception
   *           if there are problems.
   */
  @Test
  public void runUnAuthenticatedTests() throws Exception {
    if (logger.isDebugEnabled()) logger.debug("port = " + port);

    // Specify the command line parameters to be used for the tests.
    List<String> cmdLineArgs = getCommandLineArguments();
    cmdLineArgs.add("-p");
    cmdLineArgs.add("config/ausApiControllerTestAuthOff.opt");

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
    if (logger.isDebugEnabled()) logger.debug("port = " + port);

    // Specify the command line parameters to be used for the tests.
    List<String> cmdLineArgs = getCommandLineArguments();
    cmdLineArgs.add("-p");
    cmdLineArgs.add("config/ausApiControllerTestAuthOn.opt");

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
    cmdLineArgs.add("config/common.xml");

    File folder = new File("tdbxml/prod");
    File[] listOfFiles = folder.listFiles();

    for (File file : listOfFiles) {
      String fileName = file.toString();

      if (fileName.endsWith(".xml")) {
	cmdLineArgs.add("-p");
	cmdLineArgs.add(fileName);
      }
    }

    cmdLineArgs.add("-p");
    cmdLineArgs.add("config/lockss.txt");
    cmdLineArgs.add("-p");
    cmdLineArgs.add("config/lockss.opt");

    return cmdLineArgs;
  }

  /**
   * Runs the Swagger-related tests.
   * 
   * @throws Exception
   *           if there are problems.
   */
  private void getSwaggerDocsTest() throws Exception {
    ResponseEntity<String> successResponse = new TestRestTemplate().exchange(
	getTestUrl("/v2/api-docs"), HttpMethod.GET, null, String.class);

    HttpStatus statusCode = successResponse.getStatusCode();
    assertEquals(HttpStatus.OK, statusCode);

    String expectedBody = "{'swagger':'2.0',"
	+ "'info':{'description':'API of Configuration Service for LAAWS'}}";

    JSONAssert.assertEquals(expectedBody, successResponse.getBody(), false);
  }

  /**
   * Runs the putAuConfig()-related un-authenticated-specific tests.
   */
  private void putAuConfigUnAuthenticatedTest() {
    String uri = "/aus/" + goodAuid;

    ResponseEntity<String> errorResponse = new TestRestTemplate()
	.exchange(getTestUrl(uri), HttpMethod.PUT, null, String.class);

    HttpStatus statusCode = errorResponse.getStatusCode();
    assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, statusCode);

    errorResponse = new TestRestTemplate("fakeUser", "fakePassword")
	.exchange(getTestUrl(uri), HttpMethod.PUT, null, String.class);

    statusCode = errorResponse.getStatusCode();
    assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, statusCode);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    errorResponse = new TestRestTemplate().exchange(getTestUrl(uri),
	HttpMethod.PUT, new HttpEntity<String>(null, headers), String.class);

    statusCode = errorResponse.getStatusCode();
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, statusCode);

    headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    errorResponse = new TestRestTemplate("fakeUser", "fakePassword")
	.exchange(getTestUrl(uri), HttpMethod.PUT,
	    new HttpEntity<String>(null, headers), String.class);

    statusCode = errorResponse.getStatusCode();
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, statusCode);

    putAuConfigCommonTest();
  }

  /**
   * Runs the putAuConfig()-related authenticated-specific tests.
   */
  private void putAuConfigAuthenticatedTest() {
    String uri = "/aus/" + goodAuid;

    ResponseEntity<String> errorResponse = new TestRestTemplate()
	.exchange(getTestUrl(uri), HttpMethod.PUT, null, String.class);

    HttpStatus statusCode = errorResponse.getStatusCode();
    assertEquals(HttpStatus.UNAUTHORIZED, statusCode);

    errorResponse = new TestRestTemplate("fakeUser", "fakePassword")
	.exchange(getTestUrl(uri), HttpMethod.PUT, null, String.class);

    statusCode = errorResponse.getStatusCode();
    assertEquals(HttpStatus.UNAUTHORIZED, statusCode);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    errorResponse = new TestRestTemplate().exchange(getTestUrl(uri),
	HttpMethod.PUT, new HttpEntity<String>(null, headers), String.class);

    statusCode = errorResponse.getStatusCode();
    assertEquals(HttpStatus.UNAUTHORIZED, statusCode);

    headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    errorResponse = new TestRestTemplate("fakeUser", "fakePassword")
	.exchange(getTestUrl(uri), HttpMethod.PUT,
	    new HttpEntity<String>(null, headers), String.class);

    statusCode = errorResponse.getStatusCode();
    assertEquals(HttpStatus.UNAUTHORIZED, statusCode);

    putAuConfigCommonTest();
  }

  /**
   * Runs the putAuConfig()-related authentication-independent tests.
   */
  private void putAuConfigCommonTest() {
    String uri = "/aus/" + goodAuid;

    ConfigExchange backupConfig = getAuConfig(HttpStatus.OK);
    Map<String, String> backupProps = backupConfig.getProps();
    assertEquals("rafr20", backupProps.get("journal_id"));
    assertEquals("8", backupProps.get("volume_name"));
    assertNull(backupProps.get("testKey1"));
    assertNull(backupProps.get("testKey2"));

    ResponseEntity<String> errorResponse =
	new TestRestTemplate("lockss-u", "lockss-p")
	.exchange(getTestUrl(uri), HttpMethod.PUT, null, String.class);

    HttpStatus statusCode = errorResponse.getStatusCode();
    assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, statusCode);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    errorResponse = new TestRestTemplate("lockss-u", "lockss-p")
	.exchange(getTestUrl(uri), HttpMethod.PUT,
	    new HttpEntity<String>(null, headers), String.class);

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
    assertEquals("rafr20", result.get("journal_id"));
    assertEquals("8", result.get("volume_name"));
    assertNull(result.get("testKey1"));
    assertNull(result.get("testKey2"));

    uri = "/aus/fakeauid";

    result = putAuConfig(uri, configInput, HttpStatus.OK).getProps();
    assertEquals("testValue1", result.get("testKey1"));
    assertEquals("testValue2", result.get("testKey2"));

    result = putAuConfig(uri, new ConfigExchange(), HttpStatus.OK).getProps();
    assertTrue(result.keySet().isEmpty());

    ResponseEntity<ConfigExchange> successResponse =
	new TestRestTemplate("lockss-u", "lockss-p").exchange(getTestUrl(uri),
	    HttpMethod.DELETE, new HttpEntity<String>(null, headers),
	    ConfigExchange.class);

    statusCode = successResponse.getStatusCode();
    assertEquals(HttpStatus.OK, statusCode);

    result = successResponse.getBody().getProps();
    assertTrue(result.keySet().isEmpty());
  }

  /**
   * Performs a PUT operation.
   * 
   * @param uri
   *          A String with the URI of the operation.
   * @param config
   *          A ConfigExchange with the Archival Unit configuration.
   * @param expectedStatus
   *          An HttpStatus with the HTTP status of the result.
   * @return a ConfigExchange with the Archival Unit configuration.
   */
  private ConfigExchange putAuConfig(String uri, ConfigExchange config,
      HttpStatus expectedStatus) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    ResponseEntity<ConfigExchange> response =
	new TestRestTemplate("lockss-u", "lockss-p").exchange(getTestUrl(uri),
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
    String uri = "/aus/" + goodAuid;

    ResponseEntity<ConfigExchange> errorResponse = new TestRestTemplate()
	.exchange(getTestUrl(uri), HttpMethod.GET, null, ConfigExchange.class);

    HttpStatus statusCode = errorResponse.getStatusCode();
    assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, statusCode);

    errorResponse = new TestRestTemplate("fakeUser", "fakePassword")
	.exchange(getTestUrl(uri), HttpMethod.GET, null, ConfigExchange.class);

    statusCode = errorResponse.getStatusCode();
    assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, statusCode);

    getAuConfigCommonTest();
  }

  /**
   * Runs the getAuConfig()-related authenticated-specific tests.
   */
  private void getAuConfigAuthenticatedTest() {
    String uri = "/aus/" + goodAuid;

    ResponseEntity<ConfigExchange> errorResponse = new TestRestTemplate()
	.exchange(getTestUrl(uri), HttpMethod.GET, null, ConfigExchange.class);

    HttpStatus statusCode = errorResponse.getStatusCode();
    assertEquals(HttpStatus.UNAUTHORIZED, statusCode);

    errorResponse = new TestRestTemplate("fakeUser", "fakePassword")
	.exchange(getTestUrl(uri), HttpMethod.GET, null, ConfigExchange.class);

    statusCode = errorResponse.getStatusCode();
    assertEquals(HttpStatus.UNAUTHORIZED, statusCode);

    getAuConfigCommonTest();
  }

  /**
   * Runs the getAuConfig()-related authentication-independent tests.
   */
  private void getAuConfigCommonTest() {
    String uri = "/aus/" + goodAuid;

    ResponseEntity<ConfigExchange> errorResponse =
	new TestRestTemplate("lockss-u", "lockss-p").exchange(getTestUrl(uri),
	    HttpMethod.GET, null, ConfigExchange.class);

    HttpStatus statusCode = errorResponse.getStatusCode();
    assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, statusCode);

    ConfigExchange configOutput = getAuConfig(HttpStatus.OK);

    assertEquals("rafr20", configOutput.getProps().get("journal_id"));
    assertEquals("8", configOutput.getProps().get("volume_name"));

    uri = "/aus/fakeauid";

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    ResponseEntity<ConfigExchange> successResponse =
	new TestRestTemplate("lockss-u", "lockss-p").exchange(getTestUrl(uri),
	    HttpMethod.GET, new HttpEntity<String>(null, headers),
	    ConfigExchange.class);

    statusCode = successResponse.getStatusCode();
    assertEquals(HttpStatus.OK, statusCode);

    configOutput = successResponse.getBody();
    assertTrue(configOutput.getProps().isEmpty());
  }

  /**
   * Performs a GET operation for an Archival Unit.
   * 
   * @param expectedStatus
   *          An HttpStatus with the HTTP status of the result.
   * @return a ConfigExchange with the Archival Unit configuration.
   */
  private ConfigExchange getAuConfig(HttpStatus expectedStatus) {
    String uri = "/aus/" + goodAuid;

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    ResponseEntity<ConfigExchange> response =
	new TestRestTemplate("lockss-u", "lockss-p").exchange(getTestUrl(uri),
	    HttpMethod.GET, new HttpEntity<String>(null, headers),
	    ConfigExchange.class);

    HttpStatus statusCode = response.getStatusCode();
    assertEquals(expectedStatus, statusCode);

    return response.getBody();
  }

  /**
   * Runs the getAllAuConfig()-related un-authenticated-specific tests.
   */
  private void getAllAuConfigUnAuthenticatedTest() {
    String uri = "/aus";

    ResponseEntity<ConfigExchange> errorResponse = new TestRestTemplate()
	.exchange(getTestUrl(uri), HttpMethod.GET, null, ConfigExchange.class);

    HttpStatus statusCode = errorResponse.getStatusCode();
    assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, statusCode);

    errorResponse = new TestRestTemplate("fakeUser", "fakePassword")
	.exchange(getTestUrl(uri), HttpMethod.GET, null, ConfigExchange.class);

    statusCode = errorResponse.getStatusCode();
    assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, statusCode);

    getAllAuConfigCommonTest();
  }

  /**
   * Runs the getAllAuConfig()-related authenticated-specific tests.
   */
  private void getAllAuConfigAuthenticatedTest() {
    String uri = "/aus";

    ResponseEntity<ConfigExchange> errorResponse = new TestRestTemplate()
	.exchange(getTestUrl(uri), HttpMethod.GET, null, ConfigExchange.class);

    HttpStatus statusCode = errorResponse.getStatusCode();
    assertEquals(HttpStatus.UNAUTHORIZED, statusCode);

    errorResponse = new TestRestTemplate("fakeUser", "fakePassword")
	.exchange(getTestUrl(uri), HttpMethod.GET, null, ConfigExchange.class);

    statusCode = errorResponse.getStatusCode();
    assertEquals(HttpStatus.UNAUTHORIZED, statusCode);

    getAllAuConfigCommonTest();
  }

  /**
   * Runs the getAllAuConfig()-related authentication-independent tests.
   */
  private void getAllAuConfigCommonTest() {
    String uri = "/aus";

    ResponseEntity<ConfigExchange> errorResponse =
	new TestRestTemplate("lockss-u", "lockss-p").exchange(getTestUrl(uri),
	    HttpMethod.GET, null, ConfigExchange.class);

    HttpStatus statusCode = errorResponse.getStatusCode();
    assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, statusCode);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    Map<String, String> result = getAllAuConfig(HttpStatus.OK).getProps();

    String configKey = PluginManager.configKeyFromAuId(goodAuid);
    assertEquals("rafr20", result.get(configKey + ".journal_id"));
    assertEquals("8", result.get(configKey + ".volume_name"));
  }

  /**
   * Performs a GET operation for all Archival Units.
   * 
   * @param expectedStatus
   *          An HttpStatus with the HTTP status of the result.
   * @return a ConfigExchange with the Archival Unit configuration.
   */
  private ConfigExchange getAllAuConfig(HttpStatus expectedStatus) {
    String uri = "/aus";

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    ResponseEntity<ConfigExchange> response =
	new TestRestTemplate("lockss-u", "lockss-p").exchange(getTestUrl(uri),
	    HttpMethod.GET, new HttpEntity<String>(null, headers),
	    ConfigExchange.class);

    HttpStatus statusCode = response.getStatusCode();
    assertEquals(expectedStatus, statusCode);

    return response.getBody();
  }

  /**
   * Runs the deleteAus()-related un-authenticated-specific tests.
   */
  private void deleteAusUnAuthenticatedTest() {
    String uri = "/aus/" + goodAuid;

    ConfigExchange backupConfig = getAuConfig(HttpStatus.OK);
    Map<String, String> backupProps = backupConfig.getProps();
    assertEquals("rafr20", backupProps.get("journal_id"));
    assertEquals("8", backupProps.get("volume_name"));

    ResponseEntity<String> errorResponse = new TestRestTemplate()
	.exchange(getTestUrl(uri), HttpMethod.DELETE, null, String.class);

    HttpStatus statusCode = errorResponse.getStatusCode();
    assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, statusCode);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    ResponseEntity<ConfigExchange> successResponse =
	new TestRestTemplate().exchange(getTestUrl(uri), HttpMethod.DELETE,
	    new HttpEntity<String>(null, headers), ConfigExchange.class);

    statusCode = successResponse.getStatusCode();
    assertEquals(HttpStatus.OK, statusCode);

    ConfigExchange result = successResponse.getBody();
    Map<String, String> props = result.getProps();
    assertEquals(backupProps, props);

    headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    successResponse = new TestRestTemplate("fakeUser", "fakePassword")
	.exchange(getTestUrl(uri), HttpMethod.DELETE,
	    new HttpEntity<String>(null, headers), ConfigExchange.class);

    statusCode = successResponse.getStatusCode();
    assertEquals(HttpStatus.OK, statusCode);

    result = successResponse.getBody();
    assertTrue(result.getProps().keySet().isEmpty());

    props = putAuConfig(uri, backupConfig, HttpStatus.OK).getProps();
    assertEquals("rafr20", props.get("journal_id"));
    assertEquals("8", props.get("volume_name"));

    deleteAusCommonTest();
  }

  /**
   * Runs the deleteAus()-related authenticated-specific tests.
   */
  private void deleteAusAuthenticatedTest() {
    String uri = "/aus/" + goodAuid;

    ConfigExchange backupConfig = getAuConfig(HttpStatus.OK);
    Map<String, String> backupProps = backupConfig.getProps();
    assertEquals("rafr20", backupProps.get("journal_id"));
    assertEquals("8", backupProps.get("volume_name"));

    ResponseEntity<String> errorResponse = new TestRestTemplate()
	.exchange(getTestUrl(uri), HttpMethod.DELETE, null, String.class);

    HttpStatus statusCode = errorResponse.getStatusCode();
    assertEquals(HttpStatus.UNAUTHORIZED, statusCode);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    errorResponse =
	new TestRestTemplate().exchange(getTestUrl(uri), HttpMethod.DELETE,
	    new HttpEntity<String>(null, headers), String.class);

    statusCode = errorResponse.getStatusCode();
    assertEquals(HttpStatus.UNAUTHORIZED, statusCode);

    headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    errorResponse = new TestRestTemplate("fakeUser", "fakePassword")
	.exchange(getTestUrl(uri), HttpMethod.DELETE,
	    new HttpEntity<String>(null, headers), String.class);

    statusCode = errorResponse.getStatusCode();
    assertEquals(HttpStatus.UNAUTHORIZED, statusCode);

    ResponseEntity<ConfigExchange> successResponse =
	new TestRestTemplate("lockss-u", "lockss-p").exchange(getTestUrl(uri),
	    HttpMethod.DELETE, new HttpEntity<String>(null, headers),
	    ConfigExchange.class);

    statusCode = successResponse.getStatusCode();
    assertEquals(HttpStatus.OK, statusCode);

    ConfigExchange result = successResponse.getBody();
    Map<String, String> props = result.getProps();
    assertEquals(backupProps, props);

    headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    successResponse = new TestRestTemplate("lockss-u", "lockss-p")
	.exchange(getTestUrl(uri), HttpMethod.DELETE,
	    new HttpEntity<String>(null, headers), ConfigExchange.class);

    statusCode = successResponse.getStatusCode();
    assertEquals(HttpStatus.OK, statusCode);

    result = successResponse.getBody();
    assertTrue(result.getProps().keySet().isEmpty());

    props = putAuConfig(uri, backupConfig, HttpStatus.OK).getProps();
    assertEquals("rafr20", props.get("journal_id"));
    assertEquals("8", props.get("volume_name"));

    deleteAusCommonTest();
  }

  /**
   * Runs the deleteAus()-related authenticated-independent tests.
   */
  private void deleteAusCommonTest() {
    String uri = "/aus/fakeauid";

    ResponseEntity<String> errorResponse =
	new TestRestTemplate("lockss-u", "lockss-p").exchange(getTestUrl(uri),
	    HttpMethod.DELETE, null, String.class);

    HttpStatus statusCode = errorResponse.getStatusCode();
    assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, statusCode);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    ResponseEntity<ConfigExchange> successResponse =
	new TestRestTemplate("lockss-u", "lockss-p").exchange(getTestUrl(uri),
	    HttpMethod.DELETE, new HttpEntity<String>(null, headers),
	    ConfigExchange.class);

    statusCode = successResponse.getStatusCode();
    assertEquals(HttpStatus.OK, statusCode);

    ConfigExchange result = successResponse.getBody();
    assertTrue(result.getProps().keySet().isEmpty());
  }

  /**
   * Provides the URL to be tested.
   * 
   * @param uri
   *          A String with the URI of the URL to be tested.
   * @return a String with the URL to be tested.
   */
  private String getTestUrl(String uri) {
    return "http://localhost:" + port + uri;
  }
}
