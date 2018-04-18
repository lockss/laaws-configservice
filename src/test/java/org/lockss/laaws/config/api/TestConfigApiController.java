/*

 Copyright (c) 2017-2018 Board of Trustees of Leland Stanford Jr. University,
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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.MalformedParametersException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.mail.internet.MimeMultipart;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.lockss.rs.multipart.MimeMultipartHttpMessageConverter;
import org.lockss.rs.multipart.NamedByteArrayResource;
import org.lockss.rs.multipart.TextMultipartResponse;
import org.lockss.rs.multipart.TextMultipartResponse.Part;
import org.lockss.test.SpringLockssTestCase;
import org.lockss.util.TimeBase;
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
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Test class for org.lockss.laaws.config.api.ConfigApiController.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class TestConfigApiController extends SpringLockssTestCase {
  private static final Logger logger =
      LoggerFactory.getLogger(TestConfigApiController.class);

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
    setUpTempDirectory(TestConfigApiController.class.getCanonicalName());

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
   * Runs the tests for the validateSectionName() method.
   * 
   * @throws Exception
   *           if there are problems.
   */
  @Test
  public void validateSectionNameTest() throws Exception {
    if (logger.isDebugEnabled()) logger.debug("Invoked.");

    ConfigApiController controller = new ConfigApiController();

    try {
      controller.validateSectionName(null, false);
      fail("Should have thrown MalformedParametersException");
    } catch (MalformedParametersException mpe) {
      assertTrue(mpe.getMessage().startsWith("Invalid sectionName 'null'"));
    }

    try {
      controller.validateSectionName("fake", false);
      fail("Should have thrown MalformedParametersException");
    } catch (MalformedParametersException mpe) {
      assertTrue(mpe.getMessage().startsWith("Invalid sectionName 'fake'"));
    }

    assertEquals(ConfigApi.SECTION_NAME_UI_IP_ACCESS, controller
	.validateSectionName(ConfigApi.SECTION_NAME_UI_IP_ACCESS, false));
    assertEquals(ConfigApi.SECTION_NAME_UI_IP_ACCESS,
	controller.validateSectionName("UI_IP_ACCESS", false));
    assertEquals(ConfigApi.SECTION_NAME_UI_IP_ACCESS, controller
	.validateSectionName(ConfigApi.SECTION_NAME_UI_IP_ACCESS, true));
    assertEquals(ConfigApi.SECTION_NAME_UI_IP_ACCESS,
	controller.validateSectionName("UI_IP_ACCESS", true));

    try {
      controller.validateSectionName(ConfigApi.SECTION_NAME_CLUSTER, false);
      fail("Should have thrown MalformedParametersException");
    } catch (MalformedParametersException mpe) {
      assertTrue(mpe.getMessage().startsWith("Invalid sectionName 'cluster'"));
    }

    try {
      controller.validateSectionName("CLUSTER", false);
      fail("Should have thrown MalformedParametersException");
    } catch (MalformedParametersException mpe) {
      assertTrue(mpe.getMessage().startsWith("Invalid sectionName 'CLUSTER'"));
    }

    assertEquals(ConfigApi.SECTION_NAME_CLUSTER, controller
	.validateSectionName(ConfigApi.SECTION_NAME_CLUSTER, true));
    assertEquals(ConfigApi.SECTION_NAME_CLUSTER,
	controller.validateSectionName("CLUSTER", true));

    if (logger.isDebugEnabled()) logger.debug("Done.");
  }

  /**
   * Runs the full controller tests with authentication turned off.
   * 
   * @throws Exception
   *           if there are problems.
   */
  @Test
  public void runUnAuthenticatedTests() throws Exception {
    // Specify the command line parameters to be used for the tests.
    List<String> cmdLineArgs = getCommandLineArguments();
    cmdLineArgs.add("-p");
    cmdLineArgs.add("test/config/configApiControllerTestAuthOff.opt");

    CommandLineRunner runner = appCtx.getBean(CommandLineRunner.class);
    runner.run(cmdLineArgs.toArray(new String[cmdLineArgs.size()]));

    getSwaggerDocsTest();
    getStatusTest();
    getConfigSectionUnAuthenticatedTest();
    getConfigUrlUnAuthenticatedTest();
    getLastUpdateTimeUnAuthenticatedTest();
    getLoadedUrlListUnAuthenticatedTest();
    putConfigUnAuthenticatedTest();
    putConfigReloadUnAuthenticatedTest();

    if (logger.isDebugEnabled()) logger.debug("Done.");
  }

  /**
   * Runs the full controller tests with authentication turned on.
   * 
   * @throws Exception
   *           if there are problems.
   */
  @Test
  public void runAuthenticatedTests() throws Exception {
    // Specify the command line parameters to be used for the tests.
    List<String> cmdLineArgs = getCommandLineArguments();
    cmdLineArgs.add("-p");
    cmdLineArgs.add("test/config/configApiControllerTestAuthOn.opt");

    CommandLineRunner runner = appCtx.getBean(CommandLineRunner.class);
    runner.run(cmdLineArgs.toArray(new String[cmdLineArgs.size()]));

    getSwaggerDocsTest();
    getStatusTest();
    getConfigSectionAuthenticatedTest();
    getConfigUrlAuthenticatedTest();
    getLastUpdateTimeAuthenticatedTest();
    getLoadedUrlListAuthenticatedTest();
    putConfigAuthenticatedTest();
    putConfigReloadAuthenticatedTest();

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
   * Runs the status-related tests.
   * 
   * @throws Exception
   *           if there are problems.
   */
  private void getStatusTest() throws Exception {
    if (logger.isDebugEnabled()) logger.debug("Invoked.");

    ResponseEntity<String> successResponse = new TestRestTemplate().exchange(
	getTestUrlTemplate("/status"), HttpMethod.GET, null, String.class);

    HttpStatus statusCode = successResponse.getStatusCode();
    assertEquals(HttpStatus.OK, statusCode);

    String expectedBody = "{\"version\":\"1.0.0\",\"ready\":true}}";

    JSONAssert.assertEquals(expectedBody, successResponse.getBody(), false);
    if (logger.isDebugEnabled()) logger.debug("Done.");
  }

  /**
   * Runs the getConfigSection()-related un-authenticated-specific tests.
   * 
   * @throws Exception
   *           if there are problems.
   */
  private void getConfigSectionUnAuthenticatedTest() throws Exception {
    if (logger.isDebugEnabled()) logger.debug("Invoked.");

    // Use defaults for all headers.
    getConfigSection(ConfigApi.SECTION_NAME_ALERT, null, null, null, null,
	HttpStatus.NOT_FOUND);

    // Bad Accept header content type.
    getConfigSection(ConfigApi.SECTION_NAME_ALERT, MediaType.APPLICATION_JSON,
	null, null, null, HttpStatus.NOT_ACCEPTABLE);

    // Good Accept header content type.
    getConfigSection(ConfigApi.SECTION_NAME_ALERT,
	MediaType.MULTIPART_FORM_DATA, null, null, null, HttpStatus.NOT_FOUND);

    // Bad Accept header content type.
    getConfigSection(ConfigApi.SECTION_NAME_ALERT, null, null, "fakeUser",
	"fakePassword", HttpStatus.NOT_ACCEPTABLE);

    // Bad Accept header content type.
    getConfigSection(ConfigApi.SECTION_NAME_ALERT, MediaType.APPLICATION_JSON,
	null, "fakeUser", "fakePassword", HttpStatus.NOT_ACCEPTABLE);

    // Good Accept header content type.
    getConfigSection(ConfigApi.SECTION_NAME_ALERT,
	MediaType.MULTIPART_FORM_DATA, null, "fakeUser", "fakePassword",
	HttpStatus.NOT_FOUND);

    // Use defaults for all headers.
    TextMultipartResponse configOutput = getConfigSection(
	ConfigApi.SECTION_NAME_CLUSTER, null, null, null, null, HttpStatus.OK);

    List<String> expectedPayloads = new ArrayList<String>(1);
    expectedPayloads.add("<lockss-config>");
    expectedPayloads.add("<property name=\"org.lockss.auxPropUrls\">");
    expectedPayloads.add("<list append=\"false\">");
    expectedPayloads.add("</list>");
    expectedPayloads.add("</property>");
    expectedPayloads.add("</lockss-config>");

    String lastModified = verifyMultipartResponse(configOutput,
	MediaType.TEXT_XML, expectedPayloads);
    assertEquals("0", lastModified);

    // Bad Accept header content type.
    getConfigSection(ConfigApi.SECTION_NAME_CLUSTER, MediaType.APPLICATION_JSON,
	null, null, null, HttpStatus.NOT_ACCEPTABLE);

    // Good Accept header content type.
    configOutput = getConfigSection(ConfigApi.SECTION_NAME_CLUSTER,
	MediaType.MULTIPART_FORM_DATA, null, null, null, HttpStatus.OK);

    lastModified = verifyMultipartResponse(configOutput, MediaType.TEXT_XML,
	expectedPayloads);
    assertEquals("0", lastModified);

    // Bad Accept header content type.
    getConfigSection(ConfigApi.SECTION_NAME_CLUSTER, null, lastModified, null,
	null, HttpStatus.NOT_ACCEPTABLE);

    // Bad Accept header content type.
    getConfigSection(ConfigApi.SECTION_NAME_CLUSTER, MediaType.APPLICATION_JSON,
	lastModified, null, null, HttpStatus.NOT_ACCEPTABLE);

    // Not modified since last read.
    getConfigSection(ConfigApi.SECTION_NAME_CLUSTER,
	MediaType.MULTIPART_FORM_DATA, lastModified, null, null,
	HttpStatus.NOT_MODIFIED);

    // Bad Accept header content type.
    getConfigSection(ConfigApi.SECTION_NAME_CLUSTER, null, null, "fakeUser",
	"fakePassword", HttpStatus.NOT_ACCEPTABLE);

    // Bad Accept header content type.
    getConfigSection(ConfigApi.SECTION_NAME_CLUSTER, MediaType.APPLICATION_JSON,
	null, "fakeUser", "fakePassword", HttpStatus.NOT_ACCEPTABLE);

    // Good Accept header content type.
    configOutput = getConfigSection(ConfigApi.SECTION_NAME_CLUSTER,
	MediaType.MULTIPART_FORM_DATA, null, "fakeUser", "fakePassword",
	HttpStatus.OK);

    lastModified = verifyMultipartResponse(configOutput, MediaType.TEXT_XML,
	expectedPayloads);
    assertEquals("0", lastModified);

    // Bad Accept header content type.
    getConfigSection(ConfigApi.SECTION_NAME_CLUSTER, null, lastModified,
	"fakeUser", "fakePassword", HttpStatus.NOT_ACCEPTABLE);

    // Bad Accept header content type.
    getConfigSection(ConfigApi.SECTION_NAME_CLUSTER, MediaType.APPLICATION_JSON,
	lastModified, "fakeUser", "fakePassword", HttpStatus.NOT_ACCEPTABLE);

    // Not modified since last read.
    getConfigSection(ConfigApi.SECTION_NAME_CLUSTER,
	MediaType.MULTIPART_FORM_DATA, lastModified, "fakeUser", "fakePassword",
	HttpStatus.NOT_MODIFIED);

    getConfigSectionCommonTest();

    if (logger.isDebugEnabled()) logger.debug("Done.");
  }

  /**
   * Runs the getConfigSection()-related authenticated-specific tests.
   * 
   * @throws Exception
   *           if there are problems.
   */
  private void getConfigSectionAuthenticatedTest() throws Exception {
    if (logger.isDebugEnabled()) logger.debug("Invoked.");

    // Missing Accept header for UNAUTHORIZED response.
    getConfigSection(ConfigApi.SECTION_NAME_ALERT, null, null, null, null,
	HttpStatus.NOT_ACCEPTABLE);

    // Missing credentials.
    getConfigSection(ConfigApi.SECTION_NAME_ALERT, MediaType.APPLICATION_JSON,
	null, null, null, HttpStatus.UNAUTHORIZED);

    // Missing credentials.
    getConfigSection(ConfigApi.SECTION_NAME_ALERT,
	MediaType.MULTIPART_FORM_DATA, null, null, null,
	HttpStatus.UNAUTHORIZED);

    // Missing credentials.
    getConfigSection(ConfigApi.SECTION_NAME_ALERT, null, "0", null, null,
	HttpStatus.UNAUTHORIZED);

    // Missing credentials.
    getConfigSection(ConfigApi.SECTION_NAME_ALERT, MediaType.APPLICATION_JSON,
	"0", null, null, HttpStatus.UNAUTHORIZED);

    // Missing credentials.
    getConfigSection(ConfigApi.SECTION_NAME_ALERT,
	MediaType.MULTIPART_FORM_DATA, "0", null, null,
	HttpStatus.UNAUTHORIZED);

    // Bad credentials.
    getConfigSection(ConfigApi.SECTION_NAME_ALERT, null, "0", "fakeUser",
	"fakePassword", HttpStatus.UNAUTHORIZED);

    // Bad credentials.
    getConfigSection(ConfigApi.SECTION_NAME_ALERT, MediaType.APPLICATION_JSON,
	"0", "fakeUser", "fakePassword", HttpStatus.UNAUTHORIZED);

    // Bad credentials.
    getConfigSection(ConfigApi.SECTION_NAME_ALERT,
	MediaType.MULTIPART_FORM_DATA, "0", "fakeUser", "fakePassword",
	HttpStatus.UNAUTHORIZED);

    // Bad credentials.
    getConfigSection(ConfigApi.SECTION_NAME_ALERT, null, null, "fakeUser",
	"fakePassword", HttpStatus.UNAUTHORIZED);

    // Bad credentials.
    getConfigSection(ConfigApi.SECTION_NAME_ALERT, MediaType.APPLICATION_JSON,
	null, "fakeUser", "fakePassword", HttpStatus.UNAUTHORIZED);

    // Bad credentials.
    getConfigSection(ConfigApi.SECTION_NAME_ALERT,
	MediaType.MULTIPART_FORM_DATA, null, "fakeUser", "fakePassword",
	HttpStatus.UNAUTHORIZED);

    getConfigSectionCommonTest();

    if (logger.isDebugEnabled()) logger.debug("Done.");
  }

  /**
   * Runs the getConfigSection()-related authentication-independent tests.
   * 
   * @throws Exception
   *           if there are problems.
   */
  private void getConfigSectionCommonTest() throws Exception {
    if (logger.isDebugEnabled()) logger.debug("Invoked.");

    // Bad Accept header content type.
    getConfigSection(ConfigApi.SECTION_NAME_ALERT, null, null, "lockss-u",
	"lockss-p", HttpStatus.NOT_ACCEPTABLE);

    // Bad Accept header content type.
    getConfigSection(ConfigApi.SECTION_NAME_ALERT, MediaType.APPLICATION_JSON,
	null, "lockss-u", "lockss-p", HttpStatus.NOT_ACCEPTABLE);

    // Bad Accept header content type.
    getConfigSection(ConfigApi.SECTION_NAME_ALERT, MediaType.APPLICATION_JSON,
	"0", "lockss-u", "lockss-p", HttpStatus.NOT_ACCEPTABLE);

    // Bad Accept header content type.
    getConfigSection(ConfigApi.SECTION_NAME_ALERT, null, "0", "lockss-u",
	"lockss-p", HttpStatus.NOT_ACCEPTABLE);

    // Not found.
    getConfigSection(ConfigApi.SECTION_NAME_ALERT,
	MediaType.MULTIPART_FORM_DATA, null, "lockss-u", "lockss-p",
	HttpStatus.NOT_FOUND);

    // Not found.
    getConfigSection(ConfigApi.SECTION_NAME_ALERT,
	MediaType.MULTIPART_FORM_DATA, "0", "lockss-u", "lockss-p",
	HttpStatus.NOT_FOUND);

    // Bad section name.
    getConfigSection("fakesectionname", null, null, "lockss-u", "lockss-p",
	HttpStatus.BAD_REQUEST);

    // Bad section name.
    getConfigSection("fakesectionname", MediaType.MULTIPART_FORM_DATA, null,
	"lockss-u", "lockss-p", HttpStatus.BAD_REQUEST);

    // Bad section name.
    getConfigSection("fakesectionname", MediaType.MULTIPART_FORM_DATA, "0",
	"lockss-u", "lockss-p", HttpStatus.BAD_REQUEST);

    // Cluster.
    TextMultipartResponse configOutput = getConfigSection(
	ConfigApi.SECTION_NAME_CLUSTER, MediaType.MULTIPART_FORM_DATA, null,
	"lockss-u", "lockss-p", HttpStatus.OK);

    List<String> expectedPayloads = new ArrayList<String>(1);
    expectedPayloads.add("<lockss-config>");
    expectedPayloads.add("<property name=\"org.lockss.auxPropUrls\">");
    expectedPayloads.add("<list append=\"false\">");
    expectedPayloads.add("</list>");
    expectedPayloads.add("</property>");
    expectedPayloads.add("</lockss-config>");

    String lastModified = verifyMultipartResponse(configOutput,
	MediaType.TEXT_XML, expectedPayloads);
    assertTrue(Long.parseLong(lastModified) <= TimeBase.nowMs());

    // Not modified since last read.
    getConfigSection(ConfigApi.SECTION_NAME_CLUSTER,
	MediaType.MULTIPART_FORM_DATA, lastModified, "lockss-u", "lockss-p",
	HttpStatus.NOT_MODIFIED);

    // Not modified since creation.
    getConfigSection(ConfigApi.SECTION_NAME_CLUSTER,
	MediaType.MULTIPART_FORM_DATA, "0", "lockss-u", "lockss-p",
	HttpStatus.NOT_MODIFIED);

    // No eTag.
    configOutput = getConfigSection(ConfigApi.SECTION_NAME_CLUSTER,
	MediaType.MULTIPART_FORM_DATA, null, "lockss-u", "lockss-p",
	HttpStatus.OK);

    lastModified = verifyMultipartResponse(configOutput, MediaType.TEXT_XML,
	expectedPayloads);
    assertEquals("0", lastModified);

    // Not modified since last read.
    getConfigSection(ConfigApi.SECTION_NAME_CLUSTER,
	MediaType.MULTIPART_FORM_DATA, lastModified, "lockss-u", "lockss-p",
	HttpStatus.NOT_MODIFIED);

    if (logger.isDebugEnabled()) logger.debug("Done.");
  }

  /**
   * Performs a GET operation for a configuration section.
   * 
   * @param snId
   *          A String with the configuration section name.
   * @param acceptContentType
   *          A MediaType with the content type to be added to the request
   *          "Accept" header.
   * @param ifModifiedSince
   *          A String with the timestamp to be specified in the request eTag.
   * @param user
   *          A String with the request username.
   * @param password
   *          A String with the request password.
   * @param expectedStatus
   *          An HttpStatus with the HTTP status of the result.
   * @return a TextMultipartResponse with the multipart response.
   * @throws Exception
   *           if there are problems.
   */
  private TextMultipartResponse getConfigSection(String snId,
      MediaType acceptContentType, String ifModifiedSince, String user,
      String password, HttpStatus expectedStatus) throws Exception {
    if (logger.isDebugEnabled()) {
      logger.debug("snId = " + snId);
      logger.debug("acceptContentType = " + acceptContentType);
      logger.debug("ifModifiedSince = " + ifModifiedSince);
      logger.debug("user = " + user);
      logger.debug("password = " + password);
      logger.debug("expectedStatus = " + expectedStatus);
    }

    // Get the test URL template.
    String template = getTestUrlTemplate("/config/file/{snid}");

    // Create the URI of the request to the REST service.
    UriComponents uriComponents = UriComponentsBuilder.fromUriString(template)
	.build().expand(Collections.singletonMap("snid", snId));

    URI uri = UriComponentsBuilder.newInstance().uriComponents(uriComponents)
	.build().encode().toUri();

    // Initialize the request to the REST service.
    RestTemplate restTemplate = new RestTemplate();

    // Set the multipart/form-data converter as the only one.
    List<HttpMessageConverter<?>> messageConverters =
	new ArrayList<HttpMessageConverter<?>>();
    messageConverters.add(new MimeMultipartHttpMessageConverter());
    restTemplate.setMessageConverters(messageConverters);

    HttpEntity<String> requestEntity = null;

    // Check whether there are any custom headers to be specified in the
    // request.
    if (acceptContentType != null || ifModifiedSince != null
	|| user != null || password != null) {
      // Yes: Initialize the request headers.
      HttpHeaders headers = new HttpHeaders();

      // Check whether there is a custom "Accept" header.
      if (acceptContentType != null) {
	// Yes: Set it.
	headers.setAccept(Arrays.asList(acceptContentType,
	    MediaType.APPLICATION_JSON));
      } else {
	// No: Set it to accept errors at least.
	headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
      }

      // Check whether there is a custom eTag.
      if (ifModifiedSince != null) {
	// Yes: Set it.
	headers.setETag("\"" + ifModifiedSince + "\"");
      }

      // Check whether there are credentials to be sent with the request.
      if (user != null && password != null) {
	// Yes: Set the authentication credentials.
	String credentials = user + ":" + password;
	String authHeaderValue = "Basic " + Base64.getEncoder()
	.encodeToString(credentials.getBytes(Charset.forName("US-ASCII")));
	headers.set("Authorization", authHeaderValue);
      }

      if (logger.isDebugEnabled())
	logger.debug("requestHeaders = " + headers.toSingleValueMap());

      // Create the request entity.
      requestEntity = new HttpEntity<String>(null, headers);
    }

    // Make the request and get the response. 
    ResponseEntity<MimeMultipart> response = new TestRestTemplate(restTemplate)
	.exchange(uri, HttpMethod.GET, requestEntity, MimeMultipart.class);

    // Get the response status.
    HttpStatus statusCode = response.getStatusCode();
    assertEquals(expectedStatus, statusCode);

    TextMultipartResponse parsedResponse = null;

    // Check whether it is a success response.
    if (response.getStatusCodeValue() < HttpStatus.MULTIPLE_CHOICES.value()) {
      // Yes: Parse it.
      parsedResponse = new TextMultipartResponse(response);
    }

    // Return the parsed response.
    if (logger.isDebugEnabled())
      logger.debug("parsedResponse = " + parsedResponse);
    return parsedResponse;
  }

  /**
   * Provides the last modification timestamp of a configuration file obtained
   * in a response after validating the response.
   * 
   * @param response
   *          A TextMultipartResponse with the response.
   * @param expectedContentType
   *          A MediaType with the expected content type of the file.
   * @param expectedPayloads
   *          A List<String> with text expected to be part of the response
   *          payload.
   * @return a String with the last modification timestamp of a configuration
   *         file.
   * @throws Exception
   *           if there are problems.
   */
  private String verifyMultipartResponse(TextMultipartResponse response,
      MediaType expectedContentType, List<String> expectedPayloads)
	  throws Exception {
    // Validate the response content type.
    HttpHeaders responseHeaders = response.getResponseHeaders();
    assertTrue(responseHeaders.containsKey(HttpHeaders.CONTENT_TYPE));

    assertTrue(responseHeaders.getContentType().toString()
	.startsWith(MediaType.MULTIPART_FORM_DATA_VALUE + ";boundary="));

    // Get the configuration file part.
    Map<String, Part> parts = response.getParts();
    assertTrue(parts.containsKey("config-data"));
    Part part = parts.get("config-data");

    // Validate the part content type.
    Map<String, String> partHeaders = part.getHeaders();
    assertTrue(partHeaders.containsKey(HttpHeaders.CONTENT_TYPE));
    assertEquals(expectedContentType.toString(),
	partHeaders.get(HttpHeaders.CONTENT_TYPE));

    // Get the part payload content length.
    assertTrue(partHeaders.containsKey(HttpHeaders.CONTENT_LENGTH));
    long contentLength = part.getContentLength();

    // Get the part payload.
    String payload = part.getPayload();
    assertEquals(contentLength, payload.length());

    // Validate the part payload.
    if (expectedPayloads.size() > 0) {
      for (String expectedPayload : expectedPayloads) {
	assertTrue(payload.indexOf(expectedPayload) >= 0);
      }
    } else {
      assertEquals(0, contentLength);
    }

    // Get the part last modification timestamp.
    assertTrue(partHeaders.containsKey(HttpHeaders.ETAG));
    String lastModified = part.getLastModified();

    if (logger.isDebugEnabled()) logger.debug("lastModified = " + lastModified);
    return lastModified;
  }

  /**
   * Runs the getConfigUrl()-related un-authenticated-specific tests.
   * 
   * @throws Exception
   *           if there are problems.
   */
  private void getConfigUrlUnAuthenticatedTest() throws Exception {
    if (logger.isDebugEnabled()) logger.debug("Invoked.");

    String url = "http://something";

    // Use defaults for all headers.
    getConfigUrl(url, null, null, null, null, HttpStatus.NOT_FOUND);

    // Bad Accept header content type.
    getConfigUrl(url, null, "0", null, null, HttpStatus.NOT_ACCEPTABLE);

    // Bad Accept header content type.
    getConfigUrl(url, MediaType.APPLICATION_JSON, null, null, null,
	HttpStatus.NOT_ACCEPTABLE);

    // Bad Accept header content type.
    getConfigUrl(url, MediaType.APPLICATION_JSON, null, "0", null,
	HttpStatus.NOT_ACCEPTABLE);

    // Good Accept header content type.
    getConfigUrl(url, MediaType.MULTIPART_FORM_DATA, null, null, null,
	HttpStatus.NOT_FOUND);

    // Good Accept header content type.
    getConfigUrl(url, MediaType.MULTIPART_FORM_DATA, null, "0", null,
	HttpStatus.NOT_FOUND);

    // Bad Accept header content type.
    getConfigUrl(url, null, null, "fakeUser", "fakePassword",
	HttpStatus.NOT_ACCEPTABLE);

    // Bad Accept header content type.
    getConfigUrl(url, null, "0", "fakeUser", "fakePassword",
	HttpStatus.NOT_ACCEPTABLE);

    // Bad Accept header content type.
    getConfigUrl(url, MediaType.APPLICATION_JSON, null, "fakeUser",
	"fakePassword", HttpStatus.NOT_ACCEPTABLE);

    // Bad Accept header content type.
    getConfigUrl(url, MediaType.APPLICATION_JSON, "0", "fakeUser",
	"fakePassword", HttpStatus.NOT_ACCEPTABLE);

    // Good Accept header content type.
    getConfigUrl(url, MediaType.MULTIPART_FORM_DATA, null, "fakeUser",
	"fakePassword", HttpStatus.NOT_FOUND);

    // Good Accept header content type.
    getConfigUrl(url, MediaType.MULTIPART_FORM_DATA, "0", "fakeUser",
	"fakePassword", HttpStatus.NOT_FOUND);

    url = "http://localhost:12345";

    // Use defaults for all headers.
    getConfigUrl(url, null, null, null, null, HttpStatus.NOT_FOUND);

    // Bad Accept header content type.
    getConfigUrl(url, null, "0", null, null, HttpStatus.NOT_ACCEPTABLE);

    // Bad Accept header content type.
    getConfigUrl(url, MediaType.APPLICATION_JSON, null, null, null,
	HttpStatus.NOT_ACCEPTABLE);

    // Bad Accept header content type.
    getConfigUrl(url, MediaType.APPLICATION_JSON, null, "0", null,
	HttpStatus.NOT_ACCEPTABLE);

    // Good Accept header content type.
    getConfigUrl(url, MediaType.MULTIPART_FORM_DATA, null, null, null,
	HttpStatus.NOT_FOUND);

    // Good Accept header content type.
    getConfigUrl(url, MediaType.MULTIPART_FORM_DATA, null, "0", null,
	HttpStatus.NOT_FOUND);

    // Bad Accept header content type.
    getConfigUrl(url, null, null, "fakeUser", "fakePassword",
	HttpStatus.NOT_ACCEPTABLE);

    // Bad Accept header content type.
    getConfigUrl(url, null, "0", "fakeUser", "fakePassword",
	HttpStatus.NOT_ACCEPTABLE);

    // Bad Accept header content type.
    getConfigUrl(url, MediaType.APPLICATION_JSON, null, "fakeUser",
	"fakePassword", HttpStatus.NOT_ACCEPTABLE);

    // Bad Accept header content type.
    getConfigUrl(url, MediaType.APPLICATION_JSON, "0", "fakeUser",
	"fakePassword", HttpStatus.NOT_ACCEPTABLE);

    // Good Accept header content type.
    getConfigUrl(url, MediaType.MULTIPART_FORM_DATA, null, "fakeUser",
	"fakePassword", HttpStatus.NOT_FOUND);

    // Good Accept header content type.
    getConfigUrl(url, MediaType.MULTIPART_FORM_DATA, "0", "fakeUser",
	"fakePassword", HttpStatus.NOT_FOUND);

    url = "http://example.com";

    // Use defaults for all headers.
    getConfigUrl(url, null, null, null, null, HttpStatus.OK);

    // Bad Accept header content type.
    getConfigUrl(url, null, "0", null, null, HttpStatus.NOT_ACCEPTABLE);

    // Bad Accept header content type.
    getConfigUrl(url, MediaType.APPLICATION_JSON, null, null, null,
	HttpStatus.NOT_ACCEPTABLE);

    // Bad Accept header content type.
    getConfigUrl(url, MediaType.APPLICATION_JSON, null, "0", null,
	HttpStatus.NOT_ACCEPTABLE);

    // Good Accept header content type.
    getConfigUrl(url, MediaType.MULTIPART_FORM_DATA, null, null, null,
	HttpStatus.OK);

    // Good Accept header content type.
    getConfigUrl(url, MediaType.MULTIPART_FORM_DATA, null, "0", null,
	HttpStatus.OK);

    // Bad Accept header content type.
    getConfigUrl(url, null, null, "fakeUser", "fakePassword",
	HttpStatus.NOT_ACCEPTABLE);

    // Bad Accept header content type.
    getConfigUrl(url, null, "0", "fakeUser", "fakePassword",
	HttpStatus.NOT_ACCEPTABLE);

    // Bad Accept header content type.
    getConfigUrl(url, MediaType.APPLICATION_JSON, null, "fakeUser",
	"fakePassword", HttpStatus.NOT_ACCEPTABLE);

    // Bad Accept header content type.
    getConfigUrl(url, MediaType.APPLICATION_JSON, "0", "fakeUser",
	"fakePassword", HttpStatus.NOT_ACCEPTABLE);

    // Good Accept header content type.
    getConfigUrl(url, MediaType.MULTIPART_FORM_DATA, null, "fakeUser",
	"fakePassword", HttpStatus.OK);

    // Good Accept header content type.
    getConfigUrl(url, MediaType.MULTIPART_FORM_DATA, "0", "fakeUser",
	"fakePassword", HttpStatus.NOT_MODIFIED);

    getConfigUrlCommonTest();

    if (logger.isDebugEnabled()) logger.debug("Done.");
  }

  /**
   * Runs the getConfigUrl()-related authenticated-specific tests.
   * 
   * @throws Exception
   *           if there are problems.
   */
  private void getConfigUrlAuthenticatedTest() throws Exception {
    if (logger.isDebugEnabled()) logger.debug("Invoked.");

    String url = "http://something";

    // Missing Accept header for UNAUTHORIZED response.
    getConfigUrl(url, null, null, null, null, HttpStatus.NOT_ACCEPTABLE);

    // Missing credentials.
    getConfigUrl(url, MediaType.APPLICATION_JSON, null, null, null,
	HttpStatus.UNAUTHORIZED);

    // Missing credentials.
    getConfigUrl(url, MediaType.MULTIPART_FORM_DATA, null, null, null,
	HttpStatus.UNAUTHORIZED);

    // Missing credentials.
    getConfigUrl(url, null, "0", null, null, HttpStatus.UNAUTHORIZED);

    // Missing credentials.
    getConfigUrl(url, MediaType.APPLICATION_JSON, "0", null, null,
	HttpStatus.UNAUTHORIZED);

    // Missing credentials.
    getConfigUrl(url, MediaType.MULTIPART_FORM_DATA, "0", null, null,
	HttpStatus.UNAUTHORIZED);

    // Bad credentials.
    getConfigUrl(url, null, null, "fakeUser", "fakePassword",
	HttpStatus.UNAUTHORIZED);

    // Bad credentials.
    getConfigUrl(url, null, "0", "fakeUser", "fakePassword",
	HttpStatus.UNAUTHORIZED);

    // Bad credentials.
    getConfigUrl(url, MediaType.APPLICATION_JSON, null, "fakeUser",
	"fakePassword", HttpStatus.UNAUTHORIZED);

    // Bad credentials.
    getConfigUrl(url, MediaType.APPLICATION_JSON, "0", "fakeUser",
	"fakePassword", HttpStatus.UNAUTHORIZED);

    // Bad credentials.
    getConfigUrl(url, MediaType.MULTIPART_FORM_DATA, null, "fakeUser",
	"fakePassword", HttpStatus.UNAUTHORIZED);

    // Bad credentials.
    getConfigUrl(url, MediaType.MULTIPART_FORM_DATA, "0", "fakeUser",
	"fakePassword", HttpStatus.UNAUTHORIZED);

    url = "http://localhost:12345";

    // Missing Accept header for UNAUTHORIZED response.
    getConfigUrl(url, null, null, null, null, HttpStatus.NOT_ACCEPTABLE);

    // Missing credentials.
    getConfigUrl(url, MediaType.APPLICATION_JSON, null, null, null,
	HttpStatus.UNAUTHORIZED);

    // Missing credentials.
    getConfigUrl(url, MediaType.MULTIPART_FORM_DATA, null, null, null,
	HttpStatus.UNAUTHORIZED);

    // Missing credentials.
    getConfigUrl(url, null, "0", null, null, HttpStatus.UNAUTHORIZED);

    // Missing credentials.
    getConfigUrl(url, MediaType.APPLICATION_JSON, "0", null, null,
	HttpStatus.UNAUTHORIZED);

    // Missing credentials.
    getConfigUrl(url, MediaType.MULTIPART_FORM_DATA, "0", null, null,
	HttpStatus.UNAUTHORIZED);

    // Bad credentials.
    getConfigUrl(url, null, null, "fakeUser", "fakePassword",
	HttpStatus.UNAUTHORIZED);

    // Bad credentials.
    getConfigUrl(url, null, "0", "fakeUser", "fakePassword",
	HttpStatus.UNAUTHORIZED);

    // Bad credentials.
    getConfigUrl(url, MediaType.APPLICATION_JSON, null, "fakeUser",
	"fakePassword", HttpStatus.UNAUTHORIZED);

    // Bad credentials.
    getConfigUrl(url, MediaType.APPLICATION_JSON, "0", "fakeUser",
	"fakePassword", HttpStatus.UNAUTHORIZED);

    // Bad credentials.
    getConfigUrl(url, MediaType.MULTIPART_FORM_DATA, null, "fakeUser",
	"fakePassword", HttpStatus.UNAUTHORIZED);

    // Bad credentials.
    getConfigUrl(url, MediaType.MULTIPART_FORM_DATA, "0", "fakeUser",
	"fakePassword", HttpStatus.UNAUTHORIZED);

    url = "http://example.com";

    // Missing Accept header for UNAUTHORIZED response.
    getConfigUrl(url, null, null, null, null, HttpStatus.NOT_ACCEPTABLE);

    // Missing credentials.
    getConfigUrl(url, MediaType.APPLICATION_JSON, null, null, null,
	HttpStatus.UNAUTHORIZED);

    // Missing credentials.
    getConfigUrl(url, MediaType.MULTIPART_FORM_DATA, null, null, null,
	HttpStatus.UNAUTHORIZED);

    // Missing credentials.
    getConfigUrl(url, null, "0", null, null, HttpStatus.UNAUTHORIZED);

    // Missing credentials.
    getConfigUrl(url, MediaType.APPLICATION_JSON, "0", null, null,
	HttpStatus.UNAUTHORIZED);

    // Missing credentials.
    getConfigUrl(url, MediaType.MULTIPART_FORM_DATA, "0", null, null,
	HttpStatus.UNAUTHORIZED);

    // Bad credentials.
    getConfigUrl(url, null, null, "fakeUser", "fakePassword",
	HttpStatus.UNAUTHORIZED);

    // Bad credentials.
    getConfigUrl(url, null, "0", "fakeUser", "fakePassword",
	HttpStatus.UNAUTHORIZED);

    // Bad credentials.
    getConfigUrl(url, MediaType.APPLICATION_JSON, null, "fakeUser",
	"fakePassword", HttpStatus.UNAUTHORIZED);

    // Bad credentials.
    getConfigUrl(url, MediaType.APPLICATION_JSON, "0", "fakeUser",
	"fakePassword", HttpStatus.UNAUTHORIZED);

    // Bad credentials.
    getConfigUrl(url, MediaType.MULTIPART_FORM_DATA, null, "fakeUser",
	"fakePassword", HttpStatus.UNAUTHORIZED);

    // Bad credentials.
    getConfigUrl(url, MediaType.MULTIPART_FORM_DATA, "0", "fakeUser",
	"fakePassword", HttpStatus.UNAUTHORIZED);

    getConfigUrlCommonTest();

    if (logger.isDebugEnabled()) logger.debug("Done.");
  }

  /**
   * Runs the getConfigUrl()-related authentication-independent tests.
   * 
   * @throws Exception
   *           if there are problems.
   */
  private void getConfigUrlCommonTest() throws Exception {
    if (logger.isDebugEnabled()) logger.debug("Invoked.");

    String url = "http://something";

    // Bad Accept header content type.
    getConfigUrl(url, null, null, "lockss-u", "lockss-p",
	HttpStatus.NOT_ACCEPTABLE);

    // Bad Accept header content type.
    getConfigUrl(url, MediaType.APPLICATION_JSON, null, "lockss-u", "lockss-p",
	HttpStatus.NOT_ACCEPTABLE);

    // Bad Accept header content type.
    getConfigUrl(url, MediaType.APPLICATION_JSON, "0", "lockss-u", "lockss-p",
	HttpStatus.NOT_ACCEPTABLE);

    // Bad Accept header content type.
    getConfigUrl(url, null, "0", "lockss-u", "lockss-p",
	HttpStatus.NOT_ACCEPTABLE);

    // Not found.
    getConfigUrl(url, MediaType.MULTIPART_FORM_DATA, "0", "lockss-u",
	"lockss-p", HttpStatus.NOT_FOUND);

    // Not found.
    getConfigUrl(url, MediaType.MULTIPART_FORM_DATA, null, "lockss-u",
	"lockss-p", HttpStatus.NOT_FOUND);

    url = "http://localhost:12345";

    // Bad Accept header content type.
    getConfigUrl(url, null, null, "lockss-u", "lockss-p",
	HttpStatus.NOT_ACCEPTABLE);

    // Bad Accept header content type.
    getConfigUrl(url, MediaType.APPLICATION_JSON, null, "lockss-u", "lockss-p",
	HttpStatus.NOT_ACCEPTABLE);

    // Bad Accept header content type.
    getConfigUrl(url, MediaType.APPLICATION_JSON, "0", "lockss-u", "lockss-p",
	HttpStatus.NOT_ACCEPTABLE);

    // Bad Accept header content type.
    getConfigUrl(url, null, "0", "lockss-u", "lockss-p",
	HttpStatus.NOT_ACCEPTABLE);

    // Not found.
    getConfigUrl(url, MediaType.MULTIPART_FORM_DATA, "0", "lockss-u",
	"lockss-p", HttpStatus.NOT_FOUND);

    // Not found.
    getConfigUrl(url, MediaType.MULTIPART_FORM_DATA, null, "lockss-u",
	"lockss-p", HttpStatus.NOT_FOUND);

    url = "http://example.com";

    // Bad Accept header content type.
    getConfigUrl(url, null, null, "lockss-u", "lockss-p",
	HttpStatus.NOT_ACCEPTABLE);

    // Bad Accept header content type.
    getConfigUrl(url, MediaType.APPLICATION_JSON, null, "lockss-u", "lockss-p",
	HttpStatus.NOT_ACCEPTABLE);

    // Bad Accept header content type.
    getConfigUrl(url, MediaType.APPLICATION_JSON, "0", "lockss-u", "lockss-p",
	HttpStatus.NOT_ACCEPTABLE);

    // Bad Accept header content type.
    getConfigUrl(url, null, "0", "lockss-u", "lockss-p",
	HttpStatus.NOT_ACCEPTABLE);

    // Not modified.
    getConfigUrl(url, MediaType.MULTIPART_FORM_DATA, "0", "lockss-u",
	"lockss-p", HttpStatus.NOT_MODIFIED);

    // Success.
    TextMultipartResponse configOutput = getConfigUrl(url,
	MediaType.MULTIPART_FORM_DATA, null, "lockss-u", "lockss-p",
	HttpStatus.OK);

    List<String> expectedPayloads = new ArrayList<String>(1);
    expectedPayloads.add("<html>");
    expectedPayloads.add("<head>");
    expectedPayloads.add("<title>Example Domain</title>");
    expectedPayloads.add("</head>");
    expectedPayloads.add("<body>");
    expectedPayloads.add("</body>");
    expectedPayloads.add("</html>");

    String lastModified = verifyMultipartResponse(configOutput,
	MediaType.TEXT_PLAIN, expectedPayloads);
    assertTrue(Long.parseLong(lastModified) <= TimeBase.nowMs());

    // Not modified since last read.
    getConfigUrl(url, MediaType.MULTIPART_FORM_DATA, lastModified, "lockss-u",
	"lockss-p", HttpStatus.NOT_MODIFIED);

    // Not modified since creation.
    getConfigUrl(url, MediaType.MULTIPART_FORM_DATA, "0", "lockss-u",
	"lockss-p", HttpStatus.NOT_MODIFIED);

    // No eTag.
    configOutput = getConfigUrl(url, MediaType.MULTIPART_FORM_DATA, null,
	"lockss-u", "lockss-p", HttpStatus.OK);

    lastModified = verifyMultipartResponse(configOutput, MediaType.TEXT_PLAIN,
	expectedPayloads);
    assertEquals("0", lastModified);

    // Not modified since last read.
    getConfigUrl(url, MediaType.MULTIPART_FORM_DATA, lastModified, "lockss-u",
	"lockss-p", HttpStatus.NOT_MODIFIED);

    if (logger.isDebugEnabled()) logger.debug("Done.");
  }

  /**
   * Performs a GET operation for a configuration URL.
   * 
   * @param url
   *          A String with the configuration URL.
   * @param acceptContentType
   *          A MediaType with the content type to be added to the request
   *          "Accept" header.
   * @param ifModifiedSince
   *          A String with the timestamp to be specified in the request eTag.
   * @param user
   *          A String with the request username.
   * @param password
   *          A String with the request password.
   * @param expectedStatus
   *          An HttpStatus with the HTTP status of the result.
   * @return a TextMultipartResponse with the multipart response.
   * @throws Exception
   *           if there are problems.
   */
  private TextMultipartResponse getConfigUrl(String url,
      MediaType acceptContentType, String ifModifiedSince, String user,
      String password, HttpStatus expectedStatus) throws Exception {
    if (logger.isDebugEnabled()) {
      logger.debug("url = " + url);
      logger.debug("acceptContentType = " + acceptContentType);
      logger.debug("ifModifiedSince = " + ifModifiedSince);
      logger.debug("user = " + user);
      logger.debug("password = " + password);
      logger.debug("expectedStatus = " + expectedStatus);
    }

    // Get the test URL template.
    String template = getTestUrlTemplate("/config/url");

    // Create the URI of the request to the REST service.
    URI uri = UriComponentsBuilder.fromUriString(template)
	.queryParam("url", url).build().encode().toUri();

    // Initialize the request to the REST service.
    RestTemplate restTemplate = new RestTemplate();

    // Set the multipart/form-data converter as the only one.
    List<HttpMessageConverter<?>> messageConverters =
	new ArrayList<HttpMessageConverter<?>>();
    messageConverters.add(new MimeMultipartHttpMessageConverter());
    restTemplate.setMessageConverters(messageConverters);

    HttpEntity<String> requestEntity = null;

    // Check whether there are any custom headers to be specified in the
    // request.
    if (acceptContentType != null || ifModifiedSince != null
	|| user != null || password != null) {
      // Yes: Initialize the request headers.
      HttpHeaders headers = new HttpHeaders();

      // Check whether there is a custom "Accept" header.
      if (acceptContentType != null) {
	// Yes: Set it.
	headers.setAccept(Arrays.asList(acceptContentType,
	    MediaType.APPLICATION_JSON));
      } else {
	// No: Set it to accept errors at least.
	headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
      }

      // Check whether there is a custom eTag.
      if (ifModifiedSince != null) {
	// Yes: Set it.
	headers.setETag("\"" + ifModifiedSince + "\"");
      }

      // Check whether there are credentials to be sent with the request.
      if (user != null && password != null) {
	// Yes: Set the authentication credentials.
	String credentials = user + ":" + password;
	String authHeaderValue = "Basic " + Base64.getEncoder()
	.encodeToString(credentials.getBytes(Charset.forName("US-ASCII")));
	headers.set("Authorization", authHeaderValue);
      }

      if (logger.isDebugEnabled())
	logger.debug("requestHeaders = " + headers.toSingleValueMap());

      // Create the request entity.
      requestEntity = new HttpEntity<String>(null, headers);
    }

    // Make the request and get the response. 
    ResponseEntity<MimeMultipart> response = new TestRestTemplate(restTemplate)
	.exchange(uri, HttpMethod.GET, requestEntity, MimeMultipart.class);

    // Get the response status.
    HttpStatus statusCode = response.getStatusCode();
    assertEquals(expectedStatus, statusCode);

    TextMultipartResponse parsedResponse = null;

    // Check whether it is a success response.
    if (response.getStatusCodeValue() < HttpStatus.MULTIPLE_CHOICES.value()) {
      // Yes: Parse it.
      parsedResponse = new TextMultipartResponse(response);
    }

    // Return the parsed response.
    if (logger.isDebugEnabled())
      logger.debug("parsedResponse = " + parsedResponse);
    return parsedResponse;
  }

  /**
   * Runs the getLastUpdateTime()-related un-authenticated-specific tests.
   */
  private void getLastUpdateTimeUnAuthenticatedTest() {
    if (logger.isDebugEnabled()) logger.debug("Invoked.");

    getLastUpdateTime(null, null, null, HttpStatus.OK);

    getLastUpdateTime(null, "fakeUser", "fakePassword",	HttpStatus.OK);

    getLastUpdateTimeCommonTest();

    if (logger.isDebugEnabled()) logger.debug("Done.");
  }

  /**
   * Runs the getLastUpdateTime()-related authenticated-specific tests.
   */
  private void getLastUpdateTimeAuthenticatedTest() {
    if (logger.isDebugEnabled()) logger.debug("Invoked.");

    getLastUpdateTime(null, null, null, HttpStatus.UNAUTHORIZED);

    getLastUpdateTime(null, "fakeUser", "fakePassword",
	HttpStatus.UNAUTHORIZED);

    getLastUpdateTimeCommonTest();

    if (logger.isDebugEnabled()) logger.debug("Done.");
  }

  /**
   * Runs the getLastUpdateTime()-related authentication-independent tests.
   */
  private void getLastUpdateTimeCommonTest() {
    if (logger.isDebugEnabled()) logger.debug("Invoked.");

    getLastUpdateTime(null, "lockss-u", "lockss-p", HttpStatus.OK);

    getLastUpdateTime(MediaType.APPLICATION_JSON, "lockss-u", "lockss-p",
	HttpStatus.OK);

    if (logger.isDebugEnabled()) logger.debug("Done.");
  }

  /**
   * Performs a GET lastupdatetime operation.
   * 
   * @param acceptContentType
   *          A MediaType with the content type to be added to the request
   *          "Accept" header.
   * @param user
   *          A String with the request username.
   * @param password
   *          A String with the request password.
   * @param expectedStatus
   *          An HttpStatus with the HTTP status of the result.
   * @return a Date with the configuration last update time.
   */
  private void getLastUpdateTime(MediaType acceptContentType, String user,
      String password, HttpStatus expectedStatus) {
    if (logger.isDebugEnabled()) {
      logger.debug("acceptContentType = " + acceptContentType);
      logger.debug("user = " + user);
      logger.debug("password = " + password);
      logger.debug("expectedStatus = " + expectedStatus);
    }

    // Get the test URL template.
    String template = getTestUrlTemplate("/config/lastupdatetime");

    // Create the URI of the request to the REST service.
    UriComponents uriComponents =
	UriComponentsBuilder.fromUriString(template).build();

    URI uri = UriComponentsBuilder.newInstance().uriComponents(uriComponents)
	.build().encode().toUri();
    if (logger.isDebugEnabled()) logger.debug("uri = " + uri);

    // Initialize the request to the REST service.
    RestTemplate restTemplate = new RestTemplate();

    HttpEntity<String> requestEntity = null;

    // Check whether there are any custom headers to be specified in the
    // request.
    if (acceptContentType != null || user != null || password != null) {
      // Yes: Initialize the request headers.
      HttpHeaders headers = new HttpHeaders();

      // Check whether there is a custom "Accept" header.
      if (acceptContentType != null) {
	// Yes: Set it.
	headers.setAccept(Arrays.asList(acceptContentType,
	    MediaType.APPLICATION_JSON));
      } else {
	// No: Set it to accept errors at least.
	headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
      }

      // Check whether there are credentials to be sent with the request.
      if (user != null && password != null) {
	// Yes: Set the authentication credentials.
	String credentials = user + ":" + password;
	String authHeaderValue = "Basic " + Base64.getEncoder()
	.encodeToString(credentials.getBytes(Charset.forName("US-ASCII")));
	headers.set("Authorization", authHeaderValue);
      }

      // Create the request entity.
      requestEntity = new HttpEntity<String>(null, headers);
    }

    // Make the request and get the response. 
    ResponseEntity<?> response = new TestRestTemplate(restTemplate)
	.exchange(uri, HttpMethod.GET, requestEntity, String.class);

    // Get the response status.
    HttpStatus statusCode = response.getStatusCode();
    assertEquals(expectedStatus, statusCode);

    // Check whether it is a success response.
    if (response.getStatusCodeValue() < HttpStatus.MULTIPLE_CHOICES.value()) {
      // Yes: Parse it.
      long lastUpdateTime =
	  new Date(Long.parseLong((String)response.getBody())).getTime();

      // Validate it.
      long now = TimeBase.nowMs();
      assertTrue(now > lastUpdateTime);
      assertTrue(now - lastUpdateTime < 100000);
    }
  }

  /**
   * Runs the getLoadedUrlList()-related un-authenticated-specific tests.
   * 
   * @throws Exception
   *           if there are problems.
   */
  private void getLoadedUrlListUnAuthenticatedTest() throws Exception {
    if (logger.isDebugEnabled()) logger.debug("Invoked.");

    getLoadedUrlList(null, null, null, HttpStatus.OK);

    getLoadedUrlList(MediaType.APPLICATION_JSON, null, null, HttpStatus.OK);

    getLoadedUrlList(null, "fakeUser", "fakePassword", HttpStatus.OK);

    getLoadedUrlList(MediaType.APPLICATION_JSON, "fakeUser", "fakePassword",
	HttpStatus.OK);

    getLoadedUrlListCommonTest();

    if (logger.isDebugEnabled()) logger.debug("Done.");
  }

  /**
   * Runs the getLoadedUrlList()-related authenticated-specific tests.
   * 
   * @throws Exception
   *           if there are problems.
   */
  private void getLoadedUrlListAuthenticatedTest() throws Exception {
    if (logger.isDebugEnabled()) logger.debug("Invoked.");

    getLoadedUrlList(null, null, null, HttpStatus.UNAUTHORIZED);

    getLoadedUrlList(MediaType.APPLICATION_JSON, null, null,
	HttpStatus.UNAUTHORIZED);

    getLoadedUrlList(null, "fakeUser", "fakePassword", HttpStatus.UNAUTHORIZED);

    getLoadedUrlList(MediaType.APPLICATION_JSON, "fakeUser", "fakePassword",
	HttpStatus.UNAUTHORIZED);

    getLoadedUrlListCommonTest();

    if (logger.isDebugEnabled()) logger.debug("Done.");
  }

  /**
   * Runs the getLoadedUrlList()-related authentication-independent tests.
   * 
   * @throws Exception
   *           if there are problems.
   */
  private void getLoadedUrlListCommonTest() throws Exception {
    if (logger.isDebugEnabled()) logger.debug("Invoked.");

    getLoadedUrlList(null, "lockss-u", "lockss-p", HttpStatus.OK);

    getLoadedUrlList(MediaType.APPLICATION_JSON, "lockss-u", "lockss-p",
	HttpStatus.OK);

    if (logger.isDebugEnabled()) logger.debug("Done.");
  }

  /**
   * Performs a GET loadedurls operation.
   * 
   * @param expectedStatus
   *          An HttpStatus with the HTTP status of the result.
   * @return a List<String> with the loaded URLs.
   * 
   * @throws Exception
   *           if there are problems.
   */
  private void getLoadedUrlList(MediaType acceptContentType, String user,
      String password, HttpStatus expectedStatus) throws Exception {
    if (logger.isDebugEnabled()) {
      logger.debug("acceptContentType = " + acceptContentType);
      logger.debug("user = " + user);
      logger.debug("password = " + password);
      logger.debug("expectedStatus = " + expectedStatus);
    }

    // Get the test URL template.
    String template = getTestUrlTemplate("/config/loadedurls");

    // Create the URI of the request to the REST service.
    UriComponents uriComponents =
	UriComponentsBuilder.fromUriString(template).build();

    URI uri = UriComponentsBuilder.newInstance().uriComponents(uriComponents)
	.build().encode().toUri();
    if (logger.isDebugEnabled()) logger.debug("uri = " + uri);

    // Initialize the request to the REST service.
    RestTemplate restTemplate = new RestTemplate();

    HttpEntity<String> requestEntity = null;

    // Check whether there are any custom headers to be specified in the
    // request.
    if (acceptContentType != null || user != null || password != null) {
      // Yes: Initialize the request headers.
      HttpHeaders headers = new HttpHeaders();

      // Check whether there is a custom "Accept" header.
      if (acceptContentType != null) {
	// Yes: Set it.
	headers.setAccept(Arrays.asList(acceptContentType,
	    MediaType.APPLICATION_JSON));
      } else {
	// No: Set it to accept errors at least.
	headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
      }

      // Check whether there are credentials to be sent with the request.
      if (user != null && password != null) {
	// Yes: Set the authentication credentials.
	String credentials = user + ":" + password;
	String authHeaderValue = "Basic " + Base64.getEncoder()
	.encodeToString(credentials.getBytes(Charset.forName("US-ASCII")));
	headers.set("Authorization", authHeaderValue);
      }

      // Create the request entity.
      requestEntity = new HttpEntity<String>(null, headers);
    }

    // Make the request and get the response. 
    ResponseEntity<?> response = new TestRestTemplate(restTemplate)
	.exchange(uri, HttpMethod.GET, requestEntity, String.class);

    // Get the response status.
    HttpStatus statusCode = response.getStatusCode();
    assertEquals(expectedStatus, statusCode);

    // Check whether it is a success response.
    if (response.getStatusCodeValue() < HttpStatus.MULTIPLE_CHOICES.value()) {
      // Yes: Parse it.
      ObjectMapper mapper = new ObjectMapper();
      List<String> result = mapper.readValue((String)response.getBody(),
	  new TypeReference<List<String>>(){});
      assertTrue(result.contains("config/common.xml"));
    }
  }

  /**
   * Runs the putConfig()-related un-authenticated-specific tests.
   */
  private void putConfigUnAuthenticatedTest() throws Exception {
    if (logger.isDebugEnabled()) logger.debug("Invoked.");

    // Missing Content-Type header.
    putConfig(null, ConfigApi.SECTION_NAME_PLUGIN, null, null, null, null,
	HttpStatus.UNSUPPORTED_MEDIA_TYPE);

    // Missing Content-Type header.
    putConfig(null, ConfigApi.SECTION_NAME_PLUGIN, null, null, "fakeUser",
	"fakePassword", HttpStatus.UNSUPPORTED_MEDIA_TYPE);

    // Bad Content-Type header.
    putConfig(null, ConfigApi.SECTION_NAME_PLUGIN, MediaType.APPLICATION_JSON,
	null, null, null, HttpStatus.UNSUPPORTED_MEDIA_TYPE);

    // Missing payload.
    putConfig(null, ConfigApi.SECTION_NAME_PLUGIN,
	MediaType.MULTIPART_FORM_DATA, null, null, null,
	HttpStatus.INTERNAL_SERVER_ERROR);

    // Missing payload.
    putConfig(null, ConfigApi.SECTION_NAME_PLUGIN,
	MediaType.MULTIPART_FORM_DATA, null, "fakeUser", "fakePassword",
	HttpStatus.INTERNAL_SERVER_ERROR);

    // Missing eTag.
    putConfig("a1=b1", ConfigApi.SECTION_NAME_PLUGIN, null, null, null, null,
	HttpStatus.BAD_REQUEST);

    // Missing eTag.
    putConfig("a1=b1", ConfigApi.SECTION_NAME_PLUGIN, null, null, "fakeUser",
	"fakePassword", HttpStatus.BAD_REQUEST);

    // Bad Content-Type header.
    try {
      putConfig("a1=b1", ConfigApi.SECTION_NAME_PLUGIN,
	  MediaType.APPLICATION_JSON, null, null, null, HttpStatus.BAD_REQUEST);
      fail("Should have thrown HttpMessageNotWritableException");
    } catch (HttpMessageNotWritableException hmnwe) {
      assertTrue(hmnwe.getMessage().startsWith("Could not write JSON: "
	  + "No serializer found for class java.io.ByteArrayInputStream"));
    }

    // Missing eTag.
    putConfig("a1=b1", ConfigApi.SECTION_NAME_PLUGIN,
	MediaType.MULTIPART_FORM_DATA, null, null, null,
	HttpStatus.BAD_REQUEST);

    // Missing eTag.
    putConfig("a1=b1", ConfigApi.SECTION_NAME_PLUGIN,
	MediaType.MULTIPART_FORM_DATA, null, "fakeUser", "fakePassword",
	HttpStatus.BAD_REQUEST);

    // Time before write.
    long beforeWrite = TimeBase.nowMs();

    putConfig("a1=b1", ConfigApi.SECTION_NAME_PLUGIN, null, "0", null, null,
	HttpStatus.OK);

    TextMultipartResponse configOutput = getConfigSection(
	ConfigApi.SECTION_NAME_PLUGIN, MediaType.MULTIPART_FORM_DATA, "0",
	"lockss-u", "lockss-p", HttpStatus.OK);

    // Time after write.
    long afterWrite = TimeBase.nowMs();

    List<String> expectedPayloads = new ArrayList<String>(1);
    expectedPayloads.add("a1=b1");

    String lastModified = verifyMultipartResponse(configOutput,
	MediaType.TEXT_PLAIN, expectedPayloads);

    long writeTime = Long.parseLong(lastModified);
    assertTrue(beforeWrite <= writeTime);
    assertTrue(afterWrite >= writeTime);

    // Modified since passed timestamp.
    putConfig("a2=b2", ConfigApi.SECTION_NAME_PLUGIN, null, "0", "fakeUser",
	"fakePassword", HttpStatus.PRECONDITION_FAILED);

    // Time before write.
    beforeWrite = TimeBase.nowMs();

    putConfig("a2=b2", ConfigApi.SECTION_NAME_PLUGIN, null, lastModified,
	"fakeUser", "fakePassword", HttpStatus.OK);

    configOutput = getConfigSection(ConfigApi.SECTION_NAME_PLUGIN,
	MediaType.MULTIPART_FORM_DATA, lastModified, "lockss-u", "lockss-p",
	HttpStatus.OK);

    // Time after write.
    afterWrite = TimeBase.nowMs();

    expectedPayloads = new ArrayList<String>(1);
    expectedPayloads.add("a2=b2");

    lastModified = verifyMultipartResponse(configOutput, MediaType.TEXT_PLAIN,
	expectedPayloads);

    writeTime = Long.parseLong(lastModified);
    assertTrue(beforeWrite <= writeTime);
    assertTrue(afterWrite >= writeTime);

    // Bad Content-Type header.
    try {
      putConfig("a3=b3", ConfigApi.SECTION_NAME_PLUGIN,
	  MediaType.APPLICATION_JSON, "0", null, null, HttpStatus.BAD_REQUEST);
      fail("Should have thrown HttpMessageNotWritableException");
    } catch (HttpMessageNotWritableException hmnwe) {
      assertTrue(hmnwe.getMessage().startsWith("Could not write JSON: "
	  + "No serializer found for class java.io.ByteArrayInputStream"));
    }

    // Modified since creation time.
    putConfig("a3=b3", ConfigApi.SECTION_NAME_PLUGIN,
	MediaType.MULTIPART_FORM_DATA, "0", null, null,
	HttpStatus.PRECONDITION_FAILED);

    // Modified since creation time.
    putConfig("a3=b3", ConfigApi.SECTION_NAME_PLUGIN,
	MediaType.MULTIPART_FORM_DATA, "0", "fakeUser", "fakePassword",
	HttpStatus.PRECONDITION_FAILED);

    putConfigCommonTest();

    if (logger.isDebugEnabled()) logger.debug("Done.");
  }

  /**
   * Runs the putConfig()-related authenticated-specific tests.
   */
  private void putConfigAuthenticatedTest() throws Exception {
    if (logger.isDebugEnabled()) logger.debug("Invoked.");

    // Missing credentials.
    putConfig(null, ConfigApi.SECTION_NAME_PLUGIN, null, null, null, null,
	HttpStatus.UNAUTHORIZED);

    // Bad credentials.
    putConfig(null, ConfigApi.SECTION_NAME_PLUGIN, null, null, "fakeUser",
	"fakePassword", HttpStatus.UNAUTHORIZED);

    // Missing credentials.
    putConfig(null, ConfigApi.SECTION_NAME_PLUGIN, MediaType.APPLICATION_JSON,
	null, null, null, HttpStatus.UNAUTHORIZED);

    // Bad credentials.
    putConfig(null, ConfigApi.SECTION_NAME_PLUGIN, MediaType.APPLICATION_JSON,
	null, "fakeUser", "fakePassword", HttpStatus.UNAUTHORIZED);

    // Missing credentials.
    putConfig(null, ConfigApi.SECTION_NAME_PLUGIN,
	MediaType.MULTIPART_FORM_DATA, null, null, null,
	HttpStatus.UNAUTHORIZED);

    // Bad credentials.
    putConfig(null, ConfigApi.SECTION_NAME_PLUGIN,
	MediaType.MULTIPART_FORM_DATA, null, "fakeUser", "fakePassword",
	HttpStatus.UNAUTHORIZED);

    // Missing credentials.
    putConfig("a=b", ConfigApi.SECTION_NAME_PLUGIN, null, null, null, null,
	HttpStatus.UNAUTHORIZED);

    // Bad credentials.
    putConfig("a=b", ConfigApi.SECTION_NAME_PLUGIN, null, null, "fakeUser",
	"fakePassword", HttpStatus.UNAUTHORIZED);

    // Bad Content-Type header.
    try {
      putConfig("a=b", ConfigApi.SECTION_NAME_PLUGIN,
	  MediaType.APPLICATION_JSON, null, null, null,
	  HttpStatus.UNAUTHORIZED);
      fail("Should have thrown HttpMessageNotWritableException");
    } catch (HttpMessageNotWritableException hmnwe) {
      assertTrue(hmnwe.getMessage().startsWith("Could not write JSON: "
	  + "No serializer found for class java.io.ByteArrayInputStream"));
    }

    // Bad Content-Type header.
    try {
      putConfig("a=b", ConfigApi.SECTION_NAME_PLUGIN,
	  MediaType.APPLICATION_JSON, null, "fakeUser", "fakePassword",
	  HttpStatus.UNAUTHORIZED);
      fail("Should have thrown HttpMessageNotWritableException");
    } catch (HttpMessageNotWritableException hmnwe) {
      assertTrue(hmnwe.getMessage().startsWith("Could not write JSON: "
	  + "No serializer found for class java.io.ByteArrayInputStream"));
    }

    // Missing credentials.
    putConfig("a=b", ConfigApi.SECTION_NAME_PLUGIN,
	MediaType.MULTIPART_FORM_DATA, null, null, null,
	HttpStatus.UNAUTHORIZED);

    // Bad credentials.
    putConfig("a=b", ConfigApi.SECTION_NAME_PLUGIN,
	MediaType.MULTIPART_FORM_DATA, null, "fakeUser", "fakePassword",
	HttpStatus.UNAUTHORIZED);

    putConfigCommonTest();

    if (logger.isDebugEnabled()) logger.debug("Done.");
  }

  /**
   * Runs the putConfig()-related authentication-independent tests.
   */
  private void putConfigCommonTest() throws Exception {
    if (logger.isDebugEnabled()) logger.debug("Invoked.");

    // Missing Content-Type header.
    putConfig(null, ConfigApi.SECTION_NAME_EXPERT, null, null, "lockss-u",
	"lockss-p", HttpStatus.UNSUPPORTED_MEDIA_TYPE);

    // Bad Content-Type header.
    putConfig(null, ConfigApi.SECTION_NAME_EXPERT, MediaType.APPLICATION_JSON,
	null, "lockss-u", "lockss-p", HttpStatus.UNSUPPORTED_MEDIA_TYPE);

    // Missing payload.
    putConfig(null, ConfigApi.SECTION_NAME_EXPERT,
	MediaType.MULTIPART_FORM_DATA, null, "lockss-u", "lockss-p",
	HttpStatus.INTERNAL_SERVER_ERROR);

    // Missing eTag.
    putConfig("testKey=testValue", ConfigApi.SECTION_NAME_EXPERT,
	MediaType.MULTIPART_FORM_DATA, null, "lockss-u", "lockss-p",
	HttpStatus.BAD_REQUEST);

    // Modified at different time than passed timestamp.
    putConfig("testKey=testValue", ConfigApi.SECTION_NAME_EXPERT,
	MediaType.MULTIPART_FORM_DATA, "-1", "lockss-u", "lockss-p",
	HttpStatus.PRECONDITION_FAILED);

    // Time before write.
    long beforeWrite = TimeBase.nowMs();

    putConfig("testKey=testValue", ConfigApi.SECTION_NAME_EXPERT,
	MediaType.MULTIPART_FORM_DATA, "0", "lockss-u", "lockss-p",
	HttpStatus.OK);

    // Bad Accept header content type.
    getConfigSection(ConfigApi.SECTION_NAME_EXPERT, null, "0", "lockss-u",
	"lockss-p", HttpStatus.NOT_ACCEPTABLE);

    TextMultipartResponse configOutput = getConfigSection(
	ConfigApi.SECTION_NAME_EXPERT, MediaType.MULTIPART_FORM_DATA, "0",
	"lockss-u", "lockss-p", HttpStatus.OK);

    // Time after write.
    long afterWrite = TimeBase.nowMs();

    List<String> expectedPayloads = new ArrayList<String>(1);
    expectedPayloads.add("testKey=testValue");

    String lastModified = verifyMultipartResponse(configOutput,
	MediaType.TEXT_PLAIN, expectedPayloads);

    long writeTime = Long.parseLong(lastModified);
    assertTrue(beforeWrite <= writeTime);
    assertTrue(afterWrite >= writeTime);

    // Modified at different time than passed timestamp.
    putConfig("testKey1=testValue1\ntestKey2=testValue2",
	ConfigApi.SECTION_NAME_EXPERT, MediaType.MULTIPART_FORM_DATA, "0",
	"lockss-u", "lockss-p", HttpStatus.PRECONDITION_FAILED);

    // Time before write.
    beforeWrite = TimeBase.nowMs();

    putConfig("testKey1=testValue1\ntestKey2=testValue2",
	ConfigApi.SECTION_NAME_EXPERT, MediaType.MULTIPART_FORM_DATA,
	lastModified, "lockss-u", "lockss-p", HttpStatus.OK);

    configOutput = getConfigSection(ConfigApi.SECTION_NAME_EXPERT,
	MediaType.MULTIPART_FORM_DATA, lastModified, "lockss-u", "lockss-p",
	HttpStatus.OK);

    // Time after write.
    afterWrite = TimeBase.nowMs();

    expectedPayloads = new ArrayList<String>(2);
    expectedPayloads.add("testKey1=testValue1");
    expectedPayloads.add("testKey2=testValue2");

    lastModified = verifyMultipartResponse(configOutput, MediaType.TEXT_PLAIN,
	expectedPayloads);

    writeTime = Long.parseLong(lastModified);
    assertTrue(beforeWrite <= writeTime);
    assertTrue(afterWrite >= writeTime);

    // Cannot modify virtual sections.
    putConfig("testKey=testValue", ConfigApi.SECTION_NAME_CLUSTER,
	MediaType.MULTIPART_FORM_DATA, "0", "lockss-u", "lockss-p",
	HttpStatus.BAD_REQUEST);

    // Bad section name.
    putConfig("testKey=testValue", "fakesectionname",
	MediaType.MULTIPART_FORM_DATA, "0", "lockss-u", "lockss-p",
	HttpStatus.BAD_REQUEST);

    if (logger.isDebugEnabled()) logger.debug("Done.");
  }

  /**
   * Performs a PUT operation.
   * 
   * @param config
   *          A String with the configuration to be written.
   * @param snId
   *          A String with the configuration section name.
   * @param contentType
   *          A MediaType with the content type of the request.
   * @param ifUnmodifiedSince
   *          A String with the timestamp to be specified in the request eTag.
   * @param user
   *          A String with the request username.
   * @param password
   *          A String with the request password.
   * @param expectedStatus
   *          An HttpStatus with the HTTP status of the result.
   */
  private void putConfig(String config, String snId, MediaType contentType,
      String ifUnmodifiedSince, String user, String password,
      HttpStatus expectedStatus) {
    if (logger.isDebugEnabled()) {
      logger.debug("snId = " + snId);
      logger.debug("config = " + config);
      logger.debug("contentType = " + contentType);
      logger.debug("ifUnmodifiedSince = " + ifUnmodifiedSince);
      logger.debug("user = " + user);
      logger.debug("password = " + password);
      logger.debug("expectedStatus = " + expectedStatus);
    }

    // Get the test URL template.
    String template = getTestUrlTemplate("/config/file/{snid}");

    // Create the URI of the request to the REST service.
    UriComponents uriComponents = UriComponentsBuilder.fromUriString(template)
	.build().expand(Collections.singletonMap("snid", snId));

    URI uri = UriComponentsBuilder.newInstance().uriComponents(uriComponents)
	.build().encode().toUri();
    if (logger.isDebugEnabled()) logger.debug("uri = " + uri);

    // Initialize the request to the REST service.
    RestTemplate restTemplate = new RestTemplate();

    HttpEntity<MultiValueMap<String, Object>> requestEntity = null;

    // Check whether there are any custom headers to be specified in the
    // request.
    if (config != null || contentType != null
	|| ifUnmodifiedSince != null || user != null
	|| password != null) {
      // Yes.
      MultiValueMap<String, Object> parts = null;

      // Check whether there is a payload.
      if (config != null) {
	// Yes: Build it.
	HttpHeaders partHeaders = new HttpHeaders();
	partHeaders.setContentType(MediaType.TEXT_PLAIN);

	parts = new LinkedMultiValueMap<String, Object>();

	NamedByteArrayResource resource =
	    new NamedByteArrayResource("config-data", config.getBytes());

	parts.add("config-data",
	    new HttpEntity<NamedByteArrayResource>(resource, partHeaders));
      }

      // Initialize the request headers.
      HttpHeaders headers = new HttpHeaders();

      // Check whether there is a custom "Content-Type" header.
      if (contentType != null) {
	// Yes: Set it.
	headers.setContentType(contentType);
      }

      // Set the "Accept" header to accept errors at least.
      headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));

      // Check whether there is a custom eTag.
      if (ifUnmodifiedSince != null) {
	// Yes: Set it.
	headers.setETag("\"" + ifUnmodifiedSince + "\"");
      }

      // Check whether there are credentials to be sent with the request.
      if (user != null && password != null) {
	// Yes: Set the authentication credentials.
	String credentials = user + ":" + password;
	String authHeaderValue = "Basic " + Base64.getEncoder()
	.encodeToString(credentials.getBytes(Charset.forName("US-ASCII")));
	headers.set("Authorization", authHeaderValue);
      }

      // Create the request entity.
      requestEntity =
	  new HttpEntity<MultiValueMap<String, Object>>(parts, headers);
    }

    // Make the request and get the response. 
    ResponseEntity<?> response = new TestRestTemplate(restTemplate)
	.exchange(uri, HttpMethod.PUT, requestEntity, Void.class);

    // Get the response status.
    HttpStatus statusCode = response.getStatusCode();
    assertEquals(expectedStatus, statusCode);
  }

  /**
   * Runs the putConfigReload()-related un-authenticated-specific tests.
   */
  private void putConfigReloadUnAuthenticatedTest() {
    if (logger.isDebugEnabled()) logger.debug("Invoked.");

    putConfigReload(null, null, null, HttpStatus.OK);

    putConfigReload(null, "fakeUser", "fakePassword", HttpStatus.OK);

    putConfigReload(MediaType.APPLICATION_JSON, null, null, HttpStatus.OK);

    putConfigReload(MediaType.APPLICATION_JSON, "fakeUser", "fakePassword",
	HttpStatus.OK);

    putConfigReloadCommonTest();

    if (logger.isDebugEnabled()) logger.debug("Done.");
  }

  /**
   * Runs the putConfigReload()-related authenticated-specific tests.
   */
  private void putConfigReloadAuthenticatedTest() {
    if (logger.isDebugEnabled()) logger.debug("Invoked.");

    putConfigReload(null, null, null, HttpStatus.UNAUTHORIZED);

    putConfigReload(null, "fakeUser", "fakePassword", HttpStatus.UNAUTHORIZED);

    putConfigReload(MediaType.APPLICATION_JSON, null, null,
	HttpStatus.UNAUTHORIZED);

    putConfigReload(MediaType.APPLICATION_JSON, "fakeUser", "fakePassword",
	HttpStatus.UNAUTHORIZED);

    putConfigReloadCommonTest();

    if (logger.isDebugEnabled()) logger.debug("Done.");
  }

  /**
   * Runs the putConfigReload()-related authentication-independent tests.
   */
  private void putConfigReloadCommonTest() {
    if (logger.isDebugEnabled()) logger.debug("Invoked.");

    putConfigReload(null, "lockss-u", "lockss-p", HttpStatus.OK);

    putConfigReload(MediaType.APPLICATION_JSON, "lockss-u", "lockss-p",
	HttpStatus.OK);

    if (logger.isDebugEnabled()) logger.debug("Done.");
  }

  /**
   * Performs a PUT config reload operation.
   * 
   * @param acceptContentType
   *          A MediaType with the content type to be added to the request
   *          "Accept" header.
   * @param user
   *          A String with the request username.
   * @param password
   *          A String with the request password.
   * @param expectedStatus
   *          An HttpStatus with the HTTP status of the result.
   */
  private void putConfigReload(MediaType acceptContentType, String user,
      String password, HttpStatus expectedStatus) {
    if (logger.isDebugEnabled()) {
      logger.debug("acceptContentType = " + acceptContentType);
      logger.debug("user = " + user);
      logger.debug("password = " + password);
      logger.debug("expectedStatus = " + expectedStatus);
    }

    // Get the test URL template.
    String template = getTestUrlTemplate("/config/reload");

    // Create the URI of the request to the REST service.
    UriComponents uriComponents =
	UriComponentsBuilder.fromUriString(template).build();

    URI uri = UriComponentsBuilder.newInstance().uriComponents(uriComponents)
	.build().encode().toUri();
    if (logger.isDebugEnabled()) logger.debug("uri = " + uri);

    // Initialize the request to the REST service.
    RestTemplate restTemplate = new RestTemplate();

    HttpEntity<String> requestEntity = null;

    // Check whether there are any custom headers to be specified in the
    // request.
    if (acceptContentType != null || user != null || password != null) {
      // Yes: Initialize the request headers.
      HttpHeaders headers = new HttpHeaders();

      // Check whether there is a custom "Accept" header.
      if (acceptContentType != null) {
	// Yes: Set it.
	headers.setAccept(Arrays.asList(acceptContentType,
	    MediaType.APPLICATION_JSON));
      } else {
	// No: Set it to accept errors at least.
	headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
      }

      // Check whether there are credentials to be sent with the request.
      if (user != null && password != null) {
	// Yes: Set the authentication credentials.
	String credentials = user + ":" + password;
	String authHeaderValue = "Basic " + Base64.getEncoder()
	.encodeToString(credentials.getBytes(Charset.forName("US-ASCII")));
	headers.set("Authorization", authHeaderValue);
      }

      // Create the request entity.
      requestEntity = new HttpEntity<String>(null, headers);
    }

    // Make the request and get the response. 
    ResponseEntity<?> response = new TestRestTemplate(restTemplate)
	.exchange(uri, HttpMethod.PUT, requestEntity, Void.class);

    // Get the response status.
    HttpStatus statusCode = response.getStatusCode();
    assertEquals(expectedStatus, statusCode);
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
