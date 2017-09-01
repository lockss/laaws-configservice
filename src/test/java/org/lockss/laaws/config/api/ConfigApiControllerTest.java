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
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.lockss.laaws.config.model.ConfigExchange;
import org.lockss.laaws.config.model.ConfigModSpec;
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
 * Test class for org.lockss.laaws.config.api.ConfigApiController.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ConfigApiControllerTest {
  private static final Logger logger =
      LoggerFactory.getLogger(ConfigApiControllerTest.class);

  // The port that Tomcat is using during this test.
  @LocalServerPort
  private int port;

  // The application Context used to specify the command line arguments to be
  // used for the tests.
  @Autowired
  ApplicationContext appCtx;

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
    cmdLineArgs.add("config/configApiControllerTestAuthOff.opt");

    CommandLineRunner runner = appCtx.getBean(CommandLineRunner.class);
    runner.run(cmdLineArgs.toArray(new String[cmdLineArgs.size()]));

    getSwaggerDocsTest();
    putConfigUnAuthenticatedTest();
    putConfigReloadUnAuthenticatedTest();
    getConfigUnAuthenticatedTest();
    getLastUpdateTimeUnAuthenticatedTest();
    getLoadedUrlListUnAuthenticatedTest();
    deleteConfigUnAuthenticatedTest();

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
    cmdLineArgs.add("config/configApiControllerTestAuthOn.opt");

    CommandLineRunner runner = appCtx.getBean(CommandLineRunner.class);
    runner.run(cmdLineArgs.toArray(new String[cmdLineArgs.size()]));

    getSwaggerDocsTest();
    putConfigAuthenticatedTest();
    putConfigReloadAuthenticatedTest();
    getConfigAuthenticatedTest();
    getLastUpdateTimeAuthenticatedTest();
    getLoadedUrlListAuthenticatedTest();
    deleteConfigAuthenticatedTest();

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
   * Runs the putConfig()-related un-authenticated-specific tests.
   */
  private void putConfigUnAuthenticatedTest() {
    if (logger.isDebugEnabled()) logger.debug("Invoked.");

    String template = getTestUrlTemplate("/config/{snid}");

    // Create the URI of the request to the REST service.
    UriComponents uriComponents = UriComponentsBuilder.fromUriString(template)
	.build().expand(Collections.singletonMap("snid",
	    ConfigApi.SECTION_NAME_EXPERT));

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

    putConfigCommonTest();

    if (logger.isDebugEnabled()) logger.debug("Done.");
  }

  /**
   * Runs the putConfig()-related authenticated-specific tests.
   */
  private void putConfigAuthenticatedTest() {
    if (logger.isDebugEnabled()) logger.debug("Invoked.");

    String template = getTestUrlTemplate("/config/{snid}");

    // Create the URI of the request to the REST service.
    UriComponents uriComponents = UriComponentsBuilder.fromUriString(template)
	.build().expand(Collections.singletonMap("snid",
	    ConfigApi.SECTION_NAME_EXPERT));

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

    putConfigCommonTest();

    if (logger.isDebugEnabled()) logger.debug("Done.");
  }

  /**
   * Runs the putConfig()-related authentication-independent tests.
   */
  private void putConfigCommonTest() {
    if (logger.isDebugEnabled()) logger.debug("Invoked.");

    String template = getTestUrlTemplate("/config/{snid}");

    // Create the URI of the request to the REST service.
    UriComponents uriComponents = UriComponentsBuilder.fromUriString(template)
	.build().expand(Collections.singletonMap("snid",
	    ConfigApi.SECTION_NAME_EXPERT));

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

    Map<String, String> result =
	putConfig(uri, new ConfigModSpec(), HttpStatus.OK).getProps();
    assertTrue(result.isEmpty());

    Map<String, String> props = new HashMap<String, String>();
    props.put("testKey1", "testValue1");
    props.put("testKey2", "testValue2");

    ConfigModSpec configInput = new ConfigModSpec();
    configInput.setUpdates(props);

    result = putConfig(uri, configInput, HttpStatus.OK).getProps();
    assertEquals(2, result.size());
    assertEquals("testValue1", result.get("testKey1"));
    assertEquals("testValue2", result.get("testKey2"));

    configInput = new ConfigModSpec();
    configInput.setDeletes(new ArrayList<String>(props.keySet()));

    result = putConfig(uri, configInput, HttpStatus.OK).getProps();
    assertTrue(result.isEmpty());

    ResponseEntity<ConfigExchange> successResponse =
	new TestRestTemplate("lockss-u", "lockss-p").exchange(uri,
	    HttpMethod.DELETE, new HttpEntity<String>(null, headers),
	    ConfigExchange.class);

    statusCode = successResponse.getStatusCode();
    assertEquals(HttpStatus.OK, statusCode);

    result = successResponse.getBody().getProps();
    assertTrue(result.isEmpty());

    String url = getTestUrlTemplate("/config/fakesectionname");

    errorResponse = new TestRestTemplate("lockss-u", "lockss-p").exchange(url,
	HttpMethod.PUT, new HttpEntity<ConfigModSpec>(new ConfigModSpec(),
	    headers), String.class);

    statusCode = errorResponse.getStatusCode();
    assertEquals(HttpStatus.BAD_REQUEST, statusCode);

    if (logger.isDebugEnabled()) logger.debug("Done.");
  }

  /**
   * Performs a PUT operation.
   * 
   * @param uri
   *          A URI with the URI of the operation.
   * @param config
   *          A ConfigModSpec with the configuration modification specification.
   * @param expectedStatus
   *          An HttpStatus with the HTTP status of the result.
   * @return a ConfigExchange with the Archival Unit configuration.
   */
  private ConfigExchange putConfig(URI uri, ConfigModSpec config,
      HttpStatus expectedStatus) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    ResponseEntity<ConfigExchange> response = new TestRestTemplate("lockss-u",
	"lockss-p").exchange(uri, HttpMethod.PUT,
	    new HttpEntity<ConfigModSpec>(config, headers),
	    ConfigExchange.class);

    HttpStatus statusCode = response.getStatusCode();
    assertEquals(expectedStatus, statusCode);

    return response.getBody();
  }

  /**
   * Runs the putConfigReload()-related un-authenticated-specific tests.
   */
  private void putConfigReloadUnAuthenticatedTest() {
    if (logger.isDebugEnabled()) logger.debug("Invoked.");

    String url = getTestUrlTemplate("/config/reload");

    ResponseEntity<String> errorResponse = new TestRestTemplate().exchange(url,
	HttpMethod.PUT, null, String.class);

    HttpStatus statusCode = errorResponse.getStatusCode();
    assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, statusCode);

    errorResponse = new TestRestTemplate("fakeUser", "fakePassword")
	.exchange(url, HttpMethod.PUT, null, String.class);

    statusCode = errorResponse.getStatusCode();
    assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, statusCode);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    ResponseEntity<String> successResponse = new TestRestTemplate("fakeUser",
	"fakePassword").exchange(url, HttpMethod.PUT,
	    new HttpEntity<String>(null, headers), String.class);

    statusCode = successResponse.getStatusCode();
    assertEquals(HttpStatus.OK, statusCode);

    putConfigReloadCommonTest();

    if (logger.isDebugEnabled()) logger.debug("Done.");
  }

  /**
   * Runs the putConfigReload()-related authenticated-specific tests.
   */
  private void putConfigReloadAuthenticatedTest() {
    if (logger.isDebugEnabled()) logger.debug("Invoked.");

    String url = getTestUrlTemplate("/config/reload");

    ResponseEntity<String> errorResponse = new TestRestTemplate().exchange(url,
	HttpMethod.PUT, null, String.class);

    HttpStatus statusCode = errorResponse.getStatusCode();
    assertEquals(HttpStatus.UNAUTHORIZED, statusCode);

    errorResponse = new TestRestTemplate("fakeUser", "fakePassword")
	.exchange(url, HttpMethod.PUT, null, String.class);

    statusCode = errorResponse.getStatusCode();
    assertEquals(HttpStatus.UNAUTHORIZED, statusCode);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    errorResponse = new TestRestTemplate().exchange(url, HttpMethod.PUT,
	new HttpEntity<String>(null, headers), String.class);

    statusCode = errorResponse.getStatusCode();
    assertEquals(HttpStatus.UNAUTHORIZED, statusCode);

    headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    errorResponse = new TestRestTemplate("fakeUser", "fakePassword")
	.exchange(url, HttpMethod.PUT, new HttpEntity<String>(null, headers),
	    String.class);

    statusCode = errorResponse.getStatusCode();
    assertEquals(HttpStatus.UNAUTHORIZED, statusCode);

    putConfigReloadCommonTest();

    if (logger.isDebugEnabled()) logger.debug("Done.");
  }

  /**
   * Runs the putConfigReload()-related authentication-independent tests.
   */
  private void putConfigReloadCommonTest() {
    if (logger.isDebugEnabled()) logger.debug("Invoked.");

    String url = getTestUrlTemplate("/config/reload");

    ResponseEntity<String> errorResponse = new TestRestTemplate("lockss-u",
	"lockss-p").exchange(url, HttpMethod.PUT, null, String.class);

    HttpStatus statusCode = errorResponse.getStatusCode();
    assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, statusCode);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    ResponseEntity<String> successResponse = new TestRestTemplate("lockss-u",
	"lockss-p").exchange(url, HttpMethod.PUT,
	    new HttpEntity<String>(null, headers), String.class);

    statusCode = successResponse.getStatusCode();
    assertEquals(HttpStatus.OK, statusCode);

    putConfigReload(HttpStatus.OK);

    if (logger.isDebugEnabled()) logger.debug("Done.");
  }

  /**
   * Performs a PUT config reload operation.
   * 
   * @param expectedStatus
   *          An HttpStatus with the HTTP status of the result.
   * @return a ConfigExchange with the Archival Unit configuration.
   */
  private void putConfigReload(HttpStatus expectedStatus) {
    String url = getTestUrlTemplate("/config/reload");

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    ResponseEntity<Void> response = new TestRestTemplate("lockss-u", "lockss-p")
	.exchange(url, HttpMethod.PUT,
	    new HttpEntity<ConfigExchange>(null, headers), Void.class);

    HttpStatus statusCode = response.getStatusCode();
    assertEquals(expectedStatus, statusCode);
  }

  /**
   * Runs the getConfig()-related un-authenticated-specific tests.
   */
  private void getConfigUnAuthenticatedTest() {
    if (logger.isDebugEnabled()) logger.debug("Invoked.");

    String template = getTestUrlTemplate("/config/{snid}");

    // Create the URI of the request to the REST service.
    UriComponents uriComponents = UriComponentsBuilder.fromUriString(template)
	.build().expand(Collections.singletonMap("snid",
	    ConfigApi.SECTION_NAME_EXPERT));

    URI uri = UriComponentsBuilder.newInstance().uriComponents(uriComponents)
	.build().encode().toUri();
    if (logger.isDebugEnabled()) logger.debug("uri = " + uri);

    ResponseEntity<ConfigExchange> errorResponse = new TestRestTemplate()
	.exchange(uri, HttpMethod.GET, null, ConfigExchange.class);

    HttpStatus statusCode = errorResponse.getStatusCode();
    assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, statusCode);

    errorResponse = new TestRestTemplate("fakeUser", "fakePassword")
	.exchange(uri, HttpMethod.GET, null, ConfigExchange.class);

    statusCode = errorResponse.getStatusCode();
    assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, statusCode);

    getConfigCommonTest();

    if (logger.isDebugEnabled()) logger.debug("Done.");
  }

  /**
   * Runs the getConfig()-related authenticated-specific tests.
   */
  private void getConfigAuthenticatedTest() {
    if (logger.isDebugEnabled()) logger.debug("Invoked.");

    String template = getTestUrlTemplate("/config/{snid}");

    // Create the URI of the request to the REST service.
    UriComponents uriComponents = UriComponentsBuilder.fromUriString(template)
	.build().expand(Collections.singletonMap("snid",
	    ConfigApi.SECTION_NAME_EXPERT));

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

    getConfigCommonTest();

    if (logger.isDebugEnabled()) logger.debug("Done.");
  }

  /**
   * Runs the getConfig()-related authentication-independent tests.
   */
  private void getConfigCommonTest() {
    if (logger.isDebugEnabled()) logger.debug("Invoked.");

    String template = getTestUrlTemplate("/config/{snid}");

    // Create the URI of the request to the REST service.
    UriComponents uriComponents = UriComponentsBuilder.fromUriString(template)
	.build().expand(Collections.singletonMap("snid",
	    ConfigApi.SECTION_NAME_EXPERT));

    URI uri = UriComponentsBuilder.newInstance().uriComponents(uriComponents)
	.build().encode().toUri();
    if (logger.isDebugEnabled()) logger.debug("uri = " + uri);

    ResponseEntity<ConfigExchange> errorResponse =
	new TestRestTemplate("lockss-u", "lockss-p").exchange(uri,
	    HttpMethod.GET, null, ConfigExchange.class);

    HttpStatus statusCode = errorResponse.getStatusCode();
    assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, statusCode);

    ConfigExchange configOutput = getConfig(HttpStatus.OK);
    assertTrue(configOutput.getProps().isEmpty());

    String url = getTestUrlTemplate("/config/fakesectionname");

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    errorResponse = new TestRestTemplate("lockss-u", "lockss-p").exchange(url,
	HttpMethod.PUT, new HttpEntity<ConfigModSpec>(new ConfigModSpec(),
	    headers), ConfigExchange.class);

    statusCode = errorResponse.getStatusCode();
    assertEquals(HttpStatus.BAD_REQUEST, statusCode);

    if (logger.isDebugEnabled()) logger.debug("Done.");
  }

  /**
   * Performs a GET operation for an Archival Unit.
   * 
   * @param expectedStatus
   *          An HttpStatus with the HTTP status of the result.
   * @return a ConfigExchange with the Archival Unit configuration.
   */
  private ConfigExchange getConfig(HttpStatus expectedStatus) {
    String template = getTestUrlTemplate("/config/{snid}");

    // Create the URI of the request to the REST service.
    UriComponents uriComponents = UriComponentsBuilder.fromUriString(template)
	.build().expand(Collections.singletonMap("snid",
	    ConfigApi.SECTION_NAME_EXPERT));

    URI uri = UriComponentsBuilder.newInstance().uriComponents(uriComponents)
	.build().encode().toUri();
    if (logger.isDebugEnabled()) logger.debug("uri = " + uri);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    ResponseEntity<ConfigExchange> response = new TestRestTemplate("lockss-u",
	"lockss-p").exchange(uri, HttpMethod.GET, new HttpEntity<String>(null,
	    headers), ConfigExchange.class);

    HttpStatus statusCode = response.getStatusCode();
    assertEquals(expectedStatus, statusCode);

    return response.getBody();
  }

  /**
   * Runs the getLastUpdateTime()-related un-authenticated-specific tests.
   */
  private void getLastUpdateTimeUnAuthenticatedTest() {
    if (logger.isDebugEnabled()) logger.debug("Invoked.");

    String url = getTestUrlTemplate("/config/lastupdatetime");

    ResponseEntity<String> errorResponse = new TestRestTemplate().exchange(url,
	HttpMethod.GET, null, String.class);

    HttpStatus statusCode = errorResponse.getStatusCode();
    assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, statusCode);

    errorResponse = new TestRestTemplate("fakeUser", "fakePassword")
	.exchange(url, HttpMethod.GET, null, String.class);

    statusCode = errorResponse.getStatusCode();
    assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, statusCode);

    getLastUpdateTimeCommonTest();

    if (logger.isDebugEnabled()) logger.debug("Done.");
  }

  /**
   * Runs the getLastUpdateTime()-related authenticated-specific tests.
   */
  private void getLastUpdateTimeAuthenticatedTest() {
    if (logger.isDebugEnabled()) logger.debug("Invoked.");

    String url = getTestUrlTemplate("/config/lastupdatetime");

    ResponseEntity<String> errorResponse = new TestRestTemplate().exchange(url,
	HttpMethod.GET, null, String.class);

    HttpStatus statusCode = errorResponse.getStatusCode();
    assertEquals(HttpStatus.UNAUTHORIZED, statusCode);

    errorResponse = new TestRestTemplate("fakeUser", "fakePassword")
	.exchange(url, HttpMethod.GET, null, String.class);

    statusCode = errorResponse.getStatusCode();
    assertEquals(HttpStatus.UNAUTHORIZED, statusCode);

    getLastUpdateTimeCommonTest();

    if (logger.isDebugEnabled()) logger.debug("Done.");
  }

  /**
   * Runs the getLastUpdateTime()-related authentication-independent tests.
   */
  private void getLastUpdateTimeCommonTest() {
    if (logger.isDebugEnabled()) logger.debug("Invoked.");

    String url = getTestUrlTemplate("/config/lastupdatetime");

    ResponseEntity<String> errorResponse = new TestRestTemplate("lockss-u",
	"lockss-p").exchange(url, HttpMethod.GET, null, String.class);

    HttpStatus statusCode = errorResponse.getStatusCode();
    assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, statusCode);

    Date lastUpdateTime = getLastUpdateTime(HttpStatus.OK);
    Date now = new Date();
    assertTrue(now.getTime() > lastUpdateTime.getTime());
    assertTrue(now.getTime() - lastUpdateTime.getTime() < 30000);

    if (logger.isDebugEnabled()) logger.debug("Done.");
  }

  /**
   * Performs a GET lastupdatetime operation.
   * 
   * @param expectedStatus
   *          An HttpStatus with the HTTP status of the result.
   * @return a Date with the configuration last update time.
   */
  private Date getLastUpdateTime(HttpStatus expectedStatus) {
    String url = getTestUrlTemplate("/config/lastupdatetime");

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    ResponseEntity<Date> response = new TestRestTemplate("lockss-u", "lockss-p")
	.exchange(url, HttpMethod.GET, new HttpEntity<String>(null, headers),
	    Date.class);

    HttpStatus statusCode = response.getStatusCode();
    assertEquals(expectedStatus, statusCode);

    return response.getBody();
  }

  /**
   * Runs the getLoadedUrlList()-related un-authenticated-specific tests.
   */
  private void getLoadedUrlListUnAuthenticatedTest() {
    if (logger.isDebugEnabled()) logger.debug("Invoked.");

    String url = getTestUrlTemplate("/config/loadedurls");

    ResponseEntity<String> errorResponse = new TestRestTemplate().exchange(url,
	HttpMethod.GET, null, String.class);

    HttpStatus statusCode = errorResponse.getStatusCode();
    assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, statusCode);

    errorResponse = new TestRestTemplate("fakeUser", "fakePassword")
	.exchange(url, HttpMethod.GET, null, String.class);

    statusCode = errorResponse.getStatusCode();
    assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, statusCode);

    getLoadedUrlListCommonTest();

    if (logger.isDebugEnabled()) logger.debug("Done.");
  }

  /**
   * Runs the getLoadedUrlList()-related authenticated-specific tests.
   */
  private void getLoadedUrlListAuthenticatedTest() {
    if (logger.isDebugEnabled()) logger.debug("Invoked.");

    String url = getTestUrlTemplate("/config/loadedurls");

    ResponseEntity<String> errorResponse = new TestRestTemplate().exchange(url,
	HttpMethod.GET, null, String.class);

    HttpStatus statusCode = errorResponse.getStatusCode();
    assertEquals(HttpStatus.UNAUTHORIZED, statusCode);

    errorResponse = new TestRestTemplate("fakeUser", "fakePassword")
	.exchange(url, HttpMethod.GET, null, String.class);

    statusCode = errorResponse.getStatusCode();
    assertEquals(HttpStatus.UNAUTHORIZED, statusCode);

    getLoadedUrlListCommonTest();

    if (logger.isDebugEnabled()) logger.debug("Done.");
  }

  /**
   * Runs the getLoadedUrlList()-related authentication-independent tests.
   */
  private void getLoadedUrlListCommonTest() {
    if (logger.isDebugEnabled()) logger.debug("Invoked.");

    String url = getTestUrlTemplate("/config/loadedurls");

    ResponseEntity<String> errorResponse = new TestRestTemplate("lockss-u",
	"lockss-p").exchange(url, HttpMethod.GET, null, String.class);

    HttpStatus statusCode = errorResponse.getStatusCode();
    assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, statusCode);

    List<String> result = getLoadedUrlList(HttpStatus.OK);
    assertTrue(result.contains("config/common.xml"));

    if (logger.isDebugEnabled()) logger.debug("Done.");
  }

  /**
   * Performs a GET loadedurls operation.
   * 
   * @param expectedStatus
   *          An HttpStatus with the HTTP status of the result.
   * @return a ConfigExchange with the Archival Unit configuration.
   */
  private List<String> getLoadedUrlList(HttpStatus expectedStatus) {
    String url = getTestUrlTemplate("/config/loadedurls");

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    @SuppressWarnings("rawtypes")
    ResponseEntity<List> response = new TestRestTemplate("lockss-u", "lockss-p")
	.exchange(url, HttpMethod.GET, new HttpEntity<String>(null, headers),
	    List.class);

    HttpStatus statusCode = response.getStatusCode();
    assertEquals(expectedStatus, statusCode);

    @SuppressWarnings("unchecked")
    List<String> result =(List<String>)response.getBody();
    return result;
  }

  /**
   * Runs the deleteConfig()-related un-authenticated-specific tests.
   */
  private void deleteConfigUnAuthenticatedTest() {
    if (logger.isDebugEnabled()) logger.debug("Invoked.");

    String template = getTestUrlTemplate("/config/{snid}");

    // Create the URI of the request to the REST service.
    UriComponents uriComponents = UriComponentsBuilder.fromUriString(template)
	.build().expand(Collections.singletonMap("snid",
	    ConfigApi.SECTION_NAME_EXPERT));

    URI uri = UriComponentsBuilder.newInstance().uriComponents(uriComponents)
	.build().encode().toUri();
    if (logger.isDebugEnabled()) logger.debug("uri = " + uri);

    ResponseEntity<String> errorResponse = new TestRestTemplate().exchange(uri,
	HttpMethod.DELETE, null, String.class);

    HttpStatus statusCode = errorResponse.getStatusCode();
    assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, statusCode);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    ResponseEntity<ConfigExchange> successResponse = new TestRestTemplate()
	.exchange(uri, HttpMethod.DELETE, new HttpEntity<String>(null, headers),
	    ConfigExchange.class);

    statusCode = successResponse.getStatusCode();
    assertEquals(HttpStatus.OK, statusCode);

    ConfigExchange result = successResponse.getBody();
    assertTrue(result.getProps().isEmpty());

    headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    successResponse = new TestRestTemplate("fakeUser", "fakePassword")
	.exchange(uri, HttpMethod.DELETE, new HttpEntity<String>(null, headers),
	    ConfigExchange.class);

    statusCode = successResponse.getStatusCode();
    assertEquals(HttpStatus.OK, statusCode);

    result = successResponse.getBody();
    assertTrue(result.getProps().isEmpty());

    deleteConfigCommonTest();

    if (logger.isDebugEnabled()) logger.debug("Done.");
  }

  /**
   * Runs the deleteConfig()-related authenticated-specific tests.
   */
  private void deleteConfigAuthenticatedTest() {
    if (logger.isDebugEnabled()) logger.debug("Invoked.");

    String template = getTestUrlTemplate("/config/{snid}");

    // Create the URI of the request to the REST service.
    UriComponents uriComponents = UriComponentsBuilder.fromUriString(template)
	.build().expand(Collections.singletonMap("snid",
	    ConfigApi.SECTION_NAME_EXPERT));

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

    deleteConfigCommonTest();

    if (logger.isDebugEnabled()) logger.debug("Done.");
  }

  /**
   * Runs the deleteConfig()-related authenticated-independent tests.
   */
  private void deleteConfigCommonTest() {
    if (logger.isDebugEnabled()) logger.debug("Invoked.");

    String template = getTestUrlTemplate("/config/{snid}");

    // Create the URI of the request to the REST service.
    UriComponents uriComponents = UriComponentsBuilder.fromUriString(template)
	.build().expand(Collections.singletonMap("snid", "fakesectionname"));

    URI uri = UriComponentsBuilder.newInstance().uriComponents(uriComponents)
	.build().encode().toUri();
    if (logger.isDebugEnabled()) logger.debug("uri = " + uri);

    ResponseEntity<String> errorResponse = new TestRestTemplate("lockss-u",
	"lockss-p").exchange(uri, HttpMethod.DELETE, null, String.class);

    HttpStatus statusCode = errorResponse.getStatusCode();
    assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, statusCode);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    errorResponse = new TestRestTemplate("lockss-u", "lockss-p").exchange(uri,
	HttpMethod.DELETE, new HttpEntity<String>(null, headers), String.class);

    statusCode = errorResponse.getStatusCode();
    assertEquals(HttpStatus.BAD_REQUEST, statusCode);

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
