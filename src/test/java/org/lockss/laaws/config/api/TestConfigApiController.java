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

import static org.lockss.config.RestConfigClient.CONFIG_PART_NAME;
import static org.lockss.config.RestConfigClient.HTTP_WEAK_VALIDATOR_PREFIX;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
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
import org.lockss.config.ConfigManager;
import org.lockss.config.RestConfigClient;
import org.lockss.config.RestConfigSection;
import org.lockss.rs.multipart.MimeMultipartHttpMessageConverter;
import org.lockss.rs.multipart.NamedByteArrayResource;
import org.lockss.rs.multipart.MultipartResponse;
import org.lockss.rs.multipart.MultipartResponse.Part;
import org.lockss.test.SpringLockssTestCase;
import org.lockss.util.AccessType;
import org.lockss.util.ListUtil;
import org.lockss.util.StringUtil;
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
  private static final String UI_PORT_CONFIGURATION_TEMPLATE =
      "UiPortConfigTemplate.txt";
  private static final String UI_PORT_CONFIGURATION_FILE = "UiPort.opt";

  private static final String EMPTY_STRING = "";
  private static final String ZERO = "0";
  private static final String NUMBER = "9876543210";
  private static final String TEXT = "text";

  // Preconditions.
  private static final String EMPTY_PRECONDITION = "\"\"";
  private static final String NUMERIC_PRECONDITION = "\"1234567890\"";
  private static final String ALPHA_PRECONDITION = "\"ABCD\"";

  private static final String WEAK_PRECONDITION =
      HTTP_WEAK_VALIDATOR_PREFIX + NUMERIC_PRECONDITION;

  private static final String ASTERISK_PRECONDITION = "*";

  private static final List<String> EMPTY_PRECONDITION_LIST =
      new ArrayList<String>();

  // Section names.
  private static final String UIIPACCESS = "UI_IP_ACCESS";
  private static final String CLUSTER = "CLUSTER";
  private static final String BAD_SN = "badSectionName";

  // Credentials.
  private static final String GOOD_USER = "lockss-u";
  private static final String GOOD_PWD = "lockss-p";
  private static final String BAD_USER = "badUser";
  private static final String BAD_PWD = "badPassword";

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

    // Set up the UI port.
    setUpUiPort(UI_PORT_CONFIGURATION_TEMPLATE, UI_PORT_CONFIGURATION_FILE);
  }

  /**
   * Runs the tests for the validateIfMatchIfNoneMatchHeaders() method.
   * 
   * @throws Exception
   *           if there are problems.
   */
  @Test
  public void validateIfMatchIfNoneMatchHeaders() throws Exception {
    if (logger.isDebugEnabled()) logger.debug("Invoked.");

    ConfigApiController controller = new ConfigApiController();

    controller.validateIfMatchIfNoneMatchHeaders(null, null);
    controller.validateIfMatchIfNoneMatchHeaders(EMPTY_PRECONDITION_LIST, null);
    controller.validateIfMatchIfNoneMatchHeaders(null, EMPTY_PRECONDITION_LIST);

    controller.validateIfMatchIfNoneMatchHeaders(EMPTY_PRECONDITION_LIST,
	EMPTY_PRECONDITION_LIST);

    controller.validateIfMatchIfNoneMatchHeaders(
	ListUtil.list(EMPTY_PRECONDITION), EMPTY_PRECONDITION_LIST);

    controller.validateIfMatchIfNoneMatchHeaders(
	ListUtil.list(EMPTY_PRECONDITION, NUMERIC_PRECONDITION),
	EMPTY_PRECONDITION_LIST);

    try {
      controller.validateIfMatchIfNoneMatchHeaders(
	  ListUtil.list(EMPTY_PRECONDITION, NUMERIC_PRECONDITION),
	  ListUtil.list(EMPTY_PRECONDITION));
      fail("Should have thrown MalformedParametersException");
    } catch (MalformedParametersException mpe) {
      assertTrue(mpe.getMessage().equals(
	  "Invalid presence of both If-Match and If-None-Match headers"));
    }

    try {
      controller.validateIfMatchIfNoneMatchHeaders(
	  ListUtil.list(WEAK_PRECONDITION), null);
      fail("Should have thrown MalformedParametersException");
    } catch (MalformedParametersException mpe) {
      assertTrue(mpe.getMessage().equals("Invalid If-Match entity tag '"
	  + WEAK_PRECONDITION + "'"));
    }

    try {
      controller.validateIfMatchIfNoneMatchHeaders(ListUtil.list(EMPTY_STRING),
	  null);
      fail("Should have thrown MalformedParametersException");
    } catch (MalformedParametersException mpe) {
      assertTrue(mpe.getMessage().equals("Invalid If-Match entity tag '"
	  + EMPTY_STRING + "'"));
    }

    try {
      controller.validateIfMatchIfNoneMatchHeaders(ListUtil.list(NUMBER), null);
      fail("Should have thrown MalformedParametersException");
    } catch (MalformedParametersException mpe) {
      assertTrue(mpe.getMessage().equals("Invalid If-Match entity tag '"
	  + NUMBER + "'"));
    }

    try {
      controller.validateIfMatchIfNoneMatchHeaders(
	  ListUtil.list(NUMERIC_PRECONDITION, ASTERISK_PRECONDITION), null);
      fail("Should have thrown MalformedParametersException");
    } catch (MalformedParametersException mpe) {
      assertTrue(mpe.getMessage().equals("Invalid If-Match entity tag mix"));
    }

    controller.validateIfMatchIfNoneMatchHeaders(null,
	ListUtil.list(EMPTY_PRECONDITION));

    controller.validateIfMatchIfNoneMatchHeaders(null,
	ListUtil.list(EMPTY_PRECONDITION, NUMERIC_PRECONDITION,
	    ALPHA_PRECONDITION));

    try {
      controller.validateIfMatchIfNoneMatchHeaders(null,
	  ListUtil.list(EMPTY_PRECONDITION, NUMERIC_PRECONDITION,
	      WEAK_PRECONDITION, ALPHA_PRECONDITION));
      fail("Should have thrown MalformedParametersException");
    } catch (MalformedParametersException mpe) {
      assertTrue(mpe.getMessage().equals("Invalid If-None-Match entity tag '"
	  + WEAK_PRECONDITION + "'"));
    }

    try {
      controller.validateIfMatchIfNoneMatchHeaders(null,
	  ListUtil.list(ASTERISK_PRECONDITION, EMPTY_STRING));
      fail("Should have thrown MalformedParametersException");
    } catch (MalformedParametersException mpe) {
      assertTrue(mpe.getMessage()
	  .equals("Invalid If-None-Match entity tag mix"));
    }

    try {
      controller.validateIfMatchIfNoneMatchHeaders(null,
	  ListUtil.list(EMPTY_STRING));
      fail("Should have thrown MalformedParametersException");
    } catch (MalformedParametersException mpe) {
      assertTrue(mpe.getMessage().equals("Invalid If-None-Match entity tag '"
	  + EMPTY_STRING + "'"));
    }

    try {
      controller.validateIfMatchIfNoneMatchHeaders(null, ListUtil.list(NUMBER));
      fail("Should have thrown MalformedParametersException");
    } catch (MalformedParametersException mpe) {
      assertTrue(mpe.getMessage().equals("Invalid If-None-Match entity tag '"
	  + NUMBER + "'"));
    }

    if (logger.isDebugEnabled()) logger.debug("Done.");
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
      controller.validateSectionName(null, AccessType.WRITE);
      fail("Should have thrown MalformedParametersException");
    } catch (MalformedParametersException mpe) {
      assertTrue(mpe.getMessage().startsWith("Invalid sectionName 'null'"));
    }

    try {
      controller.validateSectionName(BAD_SN, AccessType.WRITE);
      fail("Should have thrown MalformedParametersException");
    } catch (MalformedParametersException mpe) {
      assertTrue(mpe.getMessage().startsWith("Invalid sectionName '" + BAD_SN
	  + "'"));
    }

    assertEquals(ConfigApi.SECTION_NAME_UI_IP_ACCESS,
	controller.validateSectionName(UIIPACCESS, AccessType.WRITE));
    assertEquals(ConfigApi.SECTION_NAME_UI_IP_ACCESS,
	controller.validateSectionName(UIIPACCESS.toLowerCase(),
	    AccessType.WRITE));
    assertEquals(ConfigApi.SECTION_NAME_UI_IP_ACCESS,
	controller.validateSectionName(UIIPACCESS, AccessType.READ));
    assertEquals(ConfigApi.SECTION_NAME_UI_IP_ACCESS,
	controller.validateSectionName(UIIPACCESS.toLowerCase(),
	    AccessType.READ));

    try {
      controller.validateSectionName(CLUSTER, AccessType.WRITE);
      fail("Should have thrown MalformedParametersException");
    } catch (MalformedParametersException mpe) {
      assertTrue(mpe.getMessage().startsWith(
	  "Invalid writing operation on sectionName '" + CLUSTER + "'"));
    }

    try {
      controller.validateSectionName(CLUSTER.toLowerCase(), AccessType.WRITE);
      fail("Should have thrown MalformedParametersException");
    } catch (MalformedParametersException mpe) {
      assertTrue(mpe.getMessage().startsWith(
	  "Invalid writing operation on sectionName '" + CLUSTER.toLowerCase()
	  + "'"));
    }

    assertEquals(ConfigApi.SECTION_NAME_CLUSTER,
	controller.validateSectionName(CLUSTER, AccessType.READ));
    assertEquals(ConfigApi.SECTION_NAME_CLUSTER,
	controller.validateSectionName(CLUSTER.toLowerCase(), AccessType.READ));

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
    cmdLineArgs.add("test/config/testAuthOff.opt");

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
    cmdLineArgs.add("test/config/testAuthOn.opt");

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

    // No section.
    runTestGetConfigSection(null, null, null, null, null, null,
	HttpStatus.NOT_ACCEPTABLE);

    // Empty section.
    runTestGetConfigSection(EMPTY_STRING, null, null, null, null, null,
	HttpStatus.NOT_ACCEPTABLE);

    // Use defaults for all headers.
    runTestGetConfigSection(ConfigApi.SECTION_NAME_ALERT, null, null, null,
	null, null, HttpStatus.NOT_FOUND);

    // Bad Accept header content type.
    runTestGetConfigSection(ConfigApi.SECTION_NAME_ALERT,
	MediaType.APPLICATION_JSON, null, null, null, null,
	HttpStatus.NOT_ACCEPTABLE);

    // Good Accept header content type.
    runTestGetConfigSection(ConfigApi.SECTION_NAME_ALERT,
	MediaType.MULTIPART_FORM_DATA, null, null, null, null,
	HttpStatus.NOT_FOUND);

    // Bad Accept header content type.
    runTestGetConfigSection(ConfigApi.SECTION_NAME_ALERT, null, null, null,
	BAD_USER, BAD_PWD, HttpStatus.NOT_ACCEPTABLE);

    // Bad Accept header content type.
    runTestGetConfigSection(ConfigApi.SECTION_NAME_ALERT,
	MediaType.APPLICATION_JSON, null, null, BAD_USER, BAD_PWD,
	HttpStatus.NOT_ACCEPTABLE);

    // Good Accept header content type.
    runTestGetConfigSection(ConfigApi.SECTION_NAME_ALERT,
	MediaType.MULTIPART_FORM_DATA, null, null, BAD_USER, BAD_PWD,
	HttpStatus.NOT_FOUND);

    // Use defaults for all headers.
    MultipartResponse configOutput = runTestGetConfigSection(
	ConfigApi.SECTION_NAME_CLUSTER, null, null, null, null, null,
	HttpStatus.OK);

    List<String> expectedPayloads = ListUtil.list(
	"<lockss-config>",
	"  <property name=\"org.lockss.auxPropUrls\">",
	"    <list append=\"false\">",
	"",
	"      <!-- Put static URLs here -->",
	"",
	"    </list>",
	"  </property>",
	"</lockss-config>"
	);

    String etag = verifyMultipartResponse(configOutput, MediaType.TEXT_XML,
	expectedPayloads);
    assertTrue(Long.parseLong(etag) <= TimeBase.nowMs());

    // Bad Accept header content type.
    runTestGetConfigSection(ConfigApi.SECTION_NAME_CLUSTER,
	MediaType.APPLICATION_JSON, null, null, null, null,
	HttpStatus.NOT_ACCEPTABLE);

    // Good Accept header content type.
    configOutput = runTestGetConfigSection(ConfigApi.SECTION_NAME_CLUSTER,
	MediaType.MULTIPART_FORM_DATA, null, null, null, null, HttpStatus.OK);

    String etag2 = verifyMultipartResponse(configOutput, MediaType.TEXT_XML,
	expectedPayloads);
    assertEquals(etag, etag2);

    // Bad Accept header content type.
    runTestGetConfigSection(ConfigApi.SECTION_NAME_CLUSTER, null, null, etag,
	null, null, HttpStatus.NOT_ACCEPTABLE);

    // Bad Accept header content type.
    runTestGetConfigSection(ConfigApi.SECTION_NAME_CLUSTER,
	MediaType.APPLICATION_JSON, null, etag, null, null,
	HttpStatus.NOT_ACCEPTABLE);

    // Not modified since last read.
    runTestGetConfigSection(ConfigApi.SECTION_NAME_CLUSTER,
	MediaType.MULTIPART_FORM_DATA, null, etag2, null, null,
	HttpStatus.NOT_MODIFIED);

    // Bad Accept header content type.
    runTestGetConfigSection(ConfigApi.SECTION_NAME_CLUSTER, null, null, null,
	BAD_USER, BAD_PWD, HttpStatus.NOT_ACCEPTABLE);

    // Bad Accept header content type.
    runTestGetConfigSection(ConfigApi.SECTION_NAME_CLUSTER,
	MediaType.APPLICATION_JSON, null, null, BAD_USER, BAD_PWD,
	HttpStatus.NOT_ACCEPTABLE);

    // Good Accept header content type.
    configOutput = runTestGetConfigSection(ConfigApi.SECTION_NAME_CLUSTER,
	MediaType.MULTIPART_FORM_DATA, null, null, BAD_USER, BAD_PWD,
	HttpStatus.OK);

    etag2 = verifyMultipartResponse(configOutput, MediaType.TEXT_XML,
	expectedPayloads);
    assertEquals(etag, etag2);

    // Bad Accept header content type.
    runTestGetConfigSection(ConfigApi.SECTION_NAME_CLUSTER, null, null, etag,
	BAD_USER, BAD_PWD, HttpStatus.NOT_ACCEPTABLE);

    // Bad Accept header content type.
    runTestGetConfigSection(ConfigApi.SECTION_NAME_CLUSTER,
	MediaType.APPLICATION_JSON, null, etag, BAD_USER, BAD_PWD,
	HttpStatus.NOT_ACCEPTABLE);

    // Not modified since last read.
    runTestGetConfigSection(ConfigApi.SECTION_NAME_CLUSTER,
	MediaType.MULTIPART_FORM_DATA, null, etag2, BAD_USER, BAD_PWD,
	HttpStatus.NOT_MODIFIED);

    // Not modified since last read using the REST service client.
    runTestGetConfigSectionClient(ConfigApi.SECTION_NAME_CLUSTER, null, etag2,
	HttpStatus.NOT_MODIFIED, HttpStatus.NOT_MODIFIED.toString());

    configOutput = runTestGetConfigSectionClient(ConfigApi.SECTION_NAME_CLUSTER,
	null, null, HttpStatus.OK, null);

    etag = verifyMultipartResponse(configOutput, MediaType.TEXT_XML,
	expectedPayloads);
    assertTrue(Long.parseLong(etag) <= TimeBase.nowMs());

    // Not modified since last read using the REST service client.
    runTestGetConfigSectionClient(ConfigApi.SECTION_NAME_CLUSTER, null, etag,
	HttpStatus.NOT_MODIFIED, HttpStatus.NOT_MODIFIED.toString());

    // Not modified since last read.
    runTestGetConfigSection(ConfigApi.SECTION_NAME_CLUSTER,
	MediaType.MULTIPART_FORM_DATA, null, etag, null, null,
	HttpStatus.NOT_MODIFIED);

    // File already exists.
    runTestGetConfigSection(ConfigApi.SECTION_NAME_CLUSTER,
	MediaType.MULTIPART_FORM_DATA, null, ASTERISK_PRECONDITION, null, null,
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

    // No section: Spring checks the Accept header before credentials.
    runTestGetConfigSection(null, null, null, null, null, null,
	HttpStatus.NOT_ACCEPTABLE);

    // Empty section: Spring checks the Accept header before credentials.
    runTestGetConfigSection(EMPTY_STRING, null, null, null, null, null,
	HttpStatus.NOT_ACCEPTABLE);

    // Missing Accept header: Spring checks the Accept header before
    // credentials.
    runTestGetConfigSection(ConfigApi.SECTION_NAME_ALERT, null, null, null,
	null, null, HttpStatus.NOT_ACCEPTABLE);

    // Missing credentials.
    runTestGetConfigSection(ConfigApi.SECTION_NAME_ALERT,
	MediaType.APPLICATION_JSON, null, null, null, null,
	HttpStatus.UNAUTHORIZED);

    // Missing credentials.
    runTestGetConfigSection(ConfigApi.SECTION_NAME_ALERT,
	MediaType.MULTIPART_FORM_DATA, null, null, null, null,
	HttpStatus.UNAUTHORIZED);

    // Missing credentials.
    runTestGetConfigSection(ConfigApi.SECTION_NAME_ALERT, null, null, ZERO,
	null, null, HttpStatus.UNAUTHORIZED);

    // Missing credentials.
    runTestGetConfigSection(ConfigApi.SECTION_NAME_ALERT,
	MediaType.APPLICATION_JSON, null, ZERO, null, null,
	HttpStatus.UNAUTHORIZED);

    // Missing credentials.
    runTestGetConfigSection(ConfigApi.SECTION_NAME_ALERT,
	MediaType.MULTIPART_FORM_DATA, null, ZERO, null, null,
	HttpStatus.UNAUTHORIZED);

    // Bad credentials.
    runTestGetConfigSection(ConfigApi.SECTION_NAME_ALERT, null, null, ZERO,
	BAD_USER, BAD_PWD, HttpStatus.UNAUTHORIZED);

    // Bad credentials.
    runTestGetConfigSection(ConfigApi.SECTION_NAME_ALERT,
	MediaType.APPLICATION_JSON, null, ZERO, BAD_USER, BAD_PWD,
	HttpStatus.UNAUTHORIZED);

    // Bad credentials.
    runTestGetConfigSection(ConfigApi.SECTION_NAME_ALERT,
	MediaType.MULTIPART_FORM_DATA, null, ZERO, BAD_USER, BAD_PWD,
	HttpStatus.UNAUTHORIZED);

    // Bad credentials.
    runTestGetConfigSection(ConfigApi.SECTION_NAME_ALERT, null, null, null,
	BAD_USER, BAD_PWD, HttpStatus.UNAUTHORIZED);

    // Bad credentials.
    runTestGetConfigSection(ConfigApi.SECTION_NAME_ALERT,
	MediaType.APPLICATION_JSON, null, null, BAD_USER, BAD_PWD,
	HttpStatus.UNAUTHORIZED);

    // Bad credentials.
    runTestGetConfigSection(ConfigApi.SECTION_NAME_ALERT,
	MediaType.MULTIPART_FORM_DATA, null, null, BAD_USER, BAD_PWD,
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

    // No section.
    runTestGetConfigSection(null, null, null, null, GOOD_USER, GOOD_PWD,
	HttpStatus.NOT_FOUND);

    // Bad section name using the REST service client.
    try {
      runTestGetConfigSectionClient(null, null, null, null, null);
      fail("Should have thrown IllegalArgumentException");
    } catch (IllegalArgumentException iae) {
      assertEquals("Invalid section name 'null'", iae.getMessage());
    }

    // Empty section.
    runTestGetConfigSection(EMPTY_STRING, null, null, null, GOOD_USER, GOOD_PWD,
	HttpStatus.NOT_FOUND);

    try {
      runTestGetConfigSectionClient(EMPTY_STRING, null, null, null, null);
      fail("Should have thrown IllegalArgumentException");
    } catch (IllegalArgumentException iae) {
      assertEquals("Invalid section name ''", iae.getMessage());
    }

    // Bad preconditions using the REST service client.
    try {
      runTestGetConfigSectionClient(ConfigApi.SECTION_NAME_ALERT, null,
	  WEAK_PRECONDITION, null, null);
      fail("Should have thrown IllegalArgumentException");
    } catch (IllegalArgumentException iae) {
      assertEquals("Invalid If-None-Match entity tag '" + WEAK_PRECONDITION
	  + "'", iae.getMessage());
    }

    try {
      runTestGetConfigSectionClient(ConfigApi.SECTION_NAME_ALERT, null,
	  ASTERISK_PRECONDITION + "," + NUMBER, null, null);
      fail("Should have thrown IllegalArgumentException");
    } catch (IllegalArgumentException iae) {
      assertEquals("Invalid If-None-Match entity tag mix", iae.getMessage());
    }

    try {
      runTestGetConfigSectionClient(ConfigApi.SECTION_NAME_ALERT, null,
	  ASTERISK_PRECONDITION + "," + ASTERISK_PRECONDITION, null, null);
      fail("Should have thrown IllegalArgumentException");
    } catch (IllegalArgumentException iae) {
      assertEquals("Invalid If-None-Match entity tag mix", iae.getMessage());
    }

    // Bad Accept header content type.
    runTestGetConfigSection(ConfigApi.SECTION_NAME_ALERT, null, null, null,
	GOOD_USER, GOOD_PWD, HttpStatus.NOT_ACCEPTABLE);

    // Bad Accept header content type.
    runTestGetConfigSection(ConfigApi.SECTION_NAME_ALERT,
	MediaType.APPLICATION_JSON, null, null, GOOD_USER, GOOD_PWD,
	HttpStatus.NOT_ACCEPTABLE);

    // Bad Accept header content type.
    runTestGetConfigSection(ConfigApi.SECTION_NAME_ALERT,
	MediaType.APPLICATION_JSON, null, ZERO, GOOD_USER, GOOD_PWD,
	HttpStatus.NOT_ACCEPTABLE);

    // Bad Accept header content type.
    runTestGetConfigSection(ConfigApi.SECTION_NAME_ALERT, null, null, ZERO,
	GOOD_USER, GOOD_PWD, HttpStatus.NOT_ACCEPTABLE);

    // Not found.
    runTestGetConfigSection(ConfigApi.SECTION_NAME_ALERT,
	MediaType.MULTIPART_FORM_DATA, null, null, GOOD_USER, GOOD_PWD,
	HttpStatus.NOT_FOUND);

    // Not found using the REST service client.
    runTestGetConfigSectionClient(ConfigApi.SECTION_NAME_ALERT, null, null,
	HttpStatus.NOT_FOUND, HttpStatus.NOT_FOUND.toString());

    // Not found.
    runTestGetConfigSection(ConfigApi.SECTION_NAME_ALERT,
	MediaType.MULTIPART_FORM_DATA, null, ZERO, GOOD_USER, GOOD_PWD,
	HttpStatus.NOT_FOUND);

    // Not found using the REST service client.
    runTestGetConfigSectionClient(ConfigApi.SECTION_NAME_ALERT, null, ZERO,
	HttpStatus.NOT_FOUND, HttpStatus.NOT_FOUND.toString());

    // Not found.
    runTestGetConfigSection(ConfigApi.SECTION_NAME_ALERT,
	MediaType.MULTIPART_FORM_DATA, null, TEXT, GOOD_USER, GOOD_PWD,
	HttpStatus.NOT_FOUND);

    // Not found using the REST service client.
    runTestGetConfigSectionClient(ConfigApi.SECTION_NAME_ALERT, null, TEXT,
	HttpStatus.NOT_FOUND, HttpStatus.NOT_FOUND.toString());

    runTestGetConfigSection(ConfigApi.SECTION_NAME_ALERT,
	MediaType.MULTIPART_FORM_DATA, null, ZERO + "," + NUMBER, GOOD_USER,
	GOOD_PWD, HttpStatus.NOT_FOUND);

    runTestGetConfigSection(ConfigApi.SECTION_NAME_ALERT,
	MediaType.MULTIPART_FORM_DATA, null, ZERO + "," + TEXT, GOOD_USER,
	GOOD_PWD, HttpStatus.NOT_FOUND);

    runTestGetConfigSection(ConfigApi.SECTION_NAME_ALERT,
	MediaType.MULTIPART_FORM_DATA, null, ZERO + "," + TEXT + "," + NUMBER,
	GOOD_USER, GOOD_PWD, HttpStatus.NOT_FOUND);

    runTestGetConfigSection(ConfigApi.SECTION_NAME_ALERT,
	MediaType.MULTIPART_FORM_DATA, null, NUMBER + "," + ZERO + "," + TEXT,
	GOOD_USER, GOOD_PWD, HttpStatus.NOT_FOUND);

    runTestGetConfigSection(ConfigApi.SECTION_NAME_ALERT,
	MediaType.MULTIPART_FORM_DATA, null, TEXT + "," + ZERO, GOOD_USER,
	GOOD_PWD, HttpStatus.NOT_FOUND);

    runTestGetConfigSection(ConfigApi.SECTION_NAME_ALERT,
	MediaType.MULTIPART_FORM_DATA, null, NUMBER + "," + TEXT + "," + ZERO,
	GOOD_USER, GOOD_PWD, HttpStatus.NOT_FOUND);

    runTestGetConfigSection(ConfigApi.SECTION_NAME_ALERT,
	MediaType.MULTIPART_FORM_DATA, null, NUMBER + "," + TEXT, GOOD_USER,
	GOOD_PWD, HttpStatus.NOT_FOUND);

    runTestGetConfigSection(ConfigApi.SECTION_NAME_ALERT,
	MediaType.MULTIPART_FORM_DATA, null, TEXT + "," + NUMBER, GOOD_USER,
	GOOD_PWD, HttpStatus.NOT_FOUND);

    // Bad section name.
    runTestGetConfigSection(BAD_SN, null, null, null, GOOD_USER, GOOD_PWD,
	HttpStatus.BAD_REQUEST);

    // Bad section name using the REST service client.
    runTestGetConfigSectionClient(BAD_SN, null, null, HttpStatus.BAD_REQUEST,
	HttpStatus.BAD_REQUEST.toString());

    // Bad section name.
    runTestGetConfigSection(BAD_SN, MediaType.MULTIPART_FORM_DATA, null, null,
	GOOD_USER, GOOD_PWD, HttpStatus.BAD_REQUEST);

    // Bad section name.
    runTestGetConfigSection(BAD_SN, MediaType.MULTIPART_FORM_DATA, null, ZERO,
	GOOD_USER, GOOD_PWD, HttpStatus.BAD_REQUEST);

    // Bad section name using the REST service client.
    runTestGetConfigSectionClient(BAD_SN, null, ZERO, HttpStatus.BAD_REQUEST,
	HttpStatus.BAD_REQUEST.toString());

    // Bad section name.
    runTestGetConfigSection(BAD_SN, MediaType.MULTIPART_FORM_DATA, null,
	ZERO + "," + NUMBER, GOOD_USER, GOOD_PWD, HttpStatus.BAD_REQUEST);

    // Bad section name using the REST service client.
    runTestGetConfigSectionClient(BAD_SN, null, ZERO + "," + NUMBER,
	HttpStatus.BAD_REQUEST, HttpStatus.BAD_REQUEST.toString());

    // Cluster.
    MultipartResponse configOutput = runTestGetConfigSection(
	ConfigApi.SECTION_NAME_CLUSTER, MediaType.MULTIPART_FORM_DATA, null,
	null, GOOD_USER, GOOD_PWD, HttpStatus.OK);

    List<String> expectedPayloads = ListUtil.list(
	"<lockss-config>",
	"  <property name=\"org.lockss.auxPropUrls\">",
	"    <list append=\"false\">",
	"",
	"      <!-- Put static URLs here -->",
	"",
	"    </list>",
	"  </property>",
	"</lockss-config>"
	);

    String etag = verifyMultipartResponse(configOutput, MediaType.TEXT_XML,
	expectedPayloads);
    assertTrue(Long.parseLong(etag) <= TimeBase.nowMs());

    // Independent verification.
    assertTrue(StringUtil.fromInputStream(ConfigManager.getConfigManager()
	.conditionallyReadCacheConfigFile("dyn:cluster.xml", null, null)
	.getInputStream()).indexOf(StringUtil
	    .separatedString(expectedPayloads, "\n")) > 0);

    // Successful using the REST service client.
    configOutput = runTestGetConfigSectionClient(ConfigApi.SECTION_NAME_CLUSTER,
	null, null, HttpStatus.OK, null);

    etag = verifyMultipartResponse(configOutput, MediaType.TEXT_XML,
	expectedPayloads);
    assertTrue(Long.parseLong(etag) <= TimeBase.nowMs());

    // Independent verification.
    assertTrue(StringUtil.fromInputStream(ConfigManager.getConfigManager()
	.conditionallyReadCacheConfigFile("dyn:cluster.xml", null, null)
	.getInputStream()).indexOf(StringUtil
	    .separatedString(expectedPayloads, "\n")) > 0);

    // Not modified since last read.
    runTestGetConfigSection(ConfigApi.SECTION_NAME_CLUSTER,
	MediaType.MULTIPART_FORM_DATA, null, etag, GOOD_USER, GOOD_PWD,
	HttpStatus.NOT_MODIFIED);

    runTestGetConfigSection(ConfigApi.SECTION_NAME_CLUSTER,
	MediaType.MULTIPART_FORM_DATA, null, ZERO + "," + etag, GOOD_USER,
	GOOD_PWD, HttpStatus.NOT_MODIFIED);

    runTestGetConfigSection(ConfigApi.SECTION_NAME_CLUSTER,
	MediaType.MULTIPART_FORM_DATA, null, TEXT + "," + etag, GOOD_USER,
	GOOD_PWD, HttpStatus.NOT_MODIFIED);

    // Not modified since last read, using the REST service client.
    runTestGetConfigSectionClient(ConfigApi.SECTION_NAME_CLUSTER, null, etag,
	HttpStatus.NOT_MODIFIED, HttpStatus.NOT_MODIFIED.toString());

    runTestGetConfigSectionClient(ConfigApi.SECTION_NAME_CLUSTER, null,
	ZERO + "," + etag, HttpStatus.NOT_MODIFIED,
	HttpStatus.NOT_MODIFIED.toString());

    runTestGetConfigSectionClient(ConfigApi.SECTION_NAME_CLUSTER, null,
	TEXT + "," + etag, HttpStatus.NOT_MODIFIED,
	HttpStatus.NOT_MODIFIED.toString());

    // Not modified since creation.
    runTestGetConfigSection(ConfigApi.SECTION_NAME_CLUSTER,
	MediaType.MULTIPART_FORM_DATA, null, ZERO, GOOD_USER, GOOD_PWD,
	HttpStatus.OK);

    // Not modified since creation, using the REST service client.
    runTestGetConfigSectionClient(ConfigApi.SECTION_NAME_CLUSTER, null, ZERO,
	HttpStatus.OK, null);

    // Successful.
    runTestGetConfigSection(ConfigApi.SECTION_NAME_CLUSTER,
	MediaType.MULTIPART_FORM_DATA, null, ZERO + "," + TEXT, GOOD_USER,
	GOOD_PWD, HttpStatus.OK);

    // Successful using the REST service client.
    runTestGetConfigSectionClient(ConfigApi.SECTION_NAME_CLUSTER, null,
	ZERO + "," + TEXT, HttpStatus.OK, null);

    // No If-None-Match header.
    configOutput = runTestGetConfigSection(ConfigApi.SECTION_NAME_CLUSTER,
	MediaType.MULTIPART_FORM_DATA, null, null, GOOD_USER, GOOD_PWD,
	HttpStatus.OK);

    String etag2 = verifyMultipartResponse(configOutput, MediaType.TEXT_XML,
	expectedPayloads);
    assertEquals(etag, etag2);

    // No If-None-Match header using the REST service client.
    configOutput = runTestGetConfigSectionClient(ConfigApi.SECTION_NAME_CLUSTER,
	null, null, HttpStatus.OK, null);

    etag2 = verifyMultipartResponse(configOutput, MediaType.TEXT_XML,
	expectedPayloads);
    assertEquals(etag, etag2);

    // Not modified since last read.
    runTestGetConfigSection(ConfigApi.SECTION_NAME_CLUSTER,
	MediaType.MULTIPART_FORM_DATA, null, etag, GOOD_USER, GOOD_PWD,
	HttpStatus.NOT_MODIFIED);

    // Not modified since last read, using the REST service client.
    runTestGetConfigSectionClient(ConfigApi.SECTION_NAME_CLUSTER, null, etag,
	HttpStatus.NOT_MODIFIED, HttpStatus.NOT_MODIFIED.toString());

    // Not modified since last read.
    runTestGetConfigSection(ConfigApi.SECTION_NAME_CLUSTER,
	MediaType.MULTIPART_FORM_DATA, null, etag + "," + NUMBER, GOOD_USER,
	GOOD_PWD, HttpStatus.NOT_MODIFIED);

    // Not modified since last read, using the REST service client.
    runTestGetConfigSectionClient(ConfigApi.SECTION_NAME_CLUSTER, null,
	etag + "," + NUMBER, HttpStatus.NOT_MODIFIED,
	HttpStatus.NOT_MODIFIED.toString());

    // Not modified since last read.
    runTestGetConfigSection(ConfigApi.SECTION_NAME_CLUSTER,
	MediaType.MULTIPART_FORM_DATA, null, etag + "," + TEXT, GOOD_USER,
	GOOD_PWD, HttpStatus.NOT_MODIFIED);

    // Not modified since last read, using the REST service client.
    runTestGetConfigSectionClient(ConfigApi.SECTION_NAME_CLUSTER, null,
	etag + "," + TEXT, HttpStatus.NOT_MODIFIED,
	HttpStatus.NOT_MODIFIED.toString());

    // No match.
    runTestGetConfigSection(ConfigApi.SECTION_NAME_CLUSTER,
	MediaType.MULTIPART_FORM_DATA, null, NUMBER, GOOD_USER, GOOD_PWD,
	HttpStatus.OK);

    // No match using the REST service client.
    runTestGetConfigSectionClient(ConfigApi.SECTION_NAME_CLUSTER, null, NUMBER,
	HttpStatus.OK, null);

    // No match.
    runTestGetConfigSection(ConfigApi.SECTION_NAME_CLUSTER,
	MediaType.MULTIPART_FORM_DATA, null, NUMBER + "," + TEXT, GOOD_USER,
	GOOD_PWD, HttpStatus.OK);

    // No match using the REST service client.
    runTestGetConfigSectionClient(ConfigApi.SECTION_NAME_CLUSTER, null,
	NUMBER + "," + TEXT, HttpStatus.OK, null);

    // File already exists.
    runTestGetConfigSection(ConfigApi.SECTION_NAME_CLUSTER,
	MediaType.MULTIPART_FORM_DATA, null, ASTERISK_PRECONDITION, GOOD_USER,
	GOOD_PWD, HttpStatus.NOT_MODIFIED);

    // File already exists, using the REST service client.
    runTestGetConfigSectionClient(ConfigApi.SECTION_NAME_CLUSTER, null,
	ASTERISK_PRECONDITION, HttpStatus.NOT_MODIFIED,
	HttpStatus.NOT_MODIFIED.toString());

    // Match of the If-Match precondition.
    runTestGetConfigSection(ConfigApi.SECTION_NAME_CLUSTER,
	MediaType.MULTIPART_FORM_DATA, etag, null, GOOD_USER, GOOD_PWD,
	HttpStatus.OK);

    // Match of the If-Match precondition, using the REST service client.
    runTestGetConfigSectionClient(ConfigApi.SECTION_NAME_CLUSTER, etag, null,
	HttpStatus.OK, null);

    // Mismatch of the If-Match precondition.
    runTestGetConfigSection(ConfigApi.SECTION_NAME_CLUSTER,
	MediaType.MULTIPART_FORM_DATA, NUMBER, null, GOOD_USER, GOOD_PWD,
	HttpStatus.PRECONDITION_FAILED);

    // Mismatch of the If-Match precondition, using the REST service client.
    runTestGetConfigSectionClient(ConfigApi.SECTION_NAME_CLUSTER, NUMBER, null,
	HttpStatus.PRECONDITION_FAILED,
	HttpStatus.PRECONDITION_FAILED.toString());

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
   * @param ifMatch
   *          A String with the timestamps to be specified in the request
   *          If-Match header.
   * @param ifNoneMatch
   *          A String with the timestamps to be specified in the request
   *          If-None-Match header.
   * @param user
   *          A String with the request username.
   * @param password
   *          A String with the request password.
   * @param expectedStatus
   *          An HttpStatus with the HTTP status of the result.
   * @return a MultipartResponse with the multipart response.
   * @throws Exception
   *           if there are problems.
   */
  private MultipartResponse runTestGetConfigSection(String snId,
      MediaType acceptContentType, String ifMatch, String ifNoneMatch,
      String user, String password, HttpStatus expectedStatus)
	  throws Exception {
    if (logger.isDebugEnabled()) {
      logger.debug("snId = " + snId);
      logger.debug("acceptContentType = " + acceptContentType);
      logger.debug("ifMatch = " + ifMatch);
      logger.debug("ifNoneMatch = " + ifNoneMatch);
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
    if (acceptContentType != null || ifMatch != null || ifNoneMatch != null
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

      // Check whether there is a custom If-Match header.
      if (ifMatch != null) {
	// Yes: Set the If-Match header.
	headers.setIfMatch(makeIfMatchNoneMatchHeaderList(ifMatch));
      }

      // Check whether there is a custom If-None-Match header.
      if (ifNoneMatch != null) {
	// Yes: Set the If-None-Match header.
	headers.setIfNoneMatch(makeIfMatchNoneMatchHeaderList(ifNoneMatch));
      }

      // Set up  the authentication credentials, if necessary.
      setUpCredentials(user, password, headers);

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

    MultipartResponse parsedResponse = null;

    // Check whether it is a success response.
    if (isSuccess(statusCode)) {
      // Yes: Parse it.
      parsedResponse = new MultipartResponse(response);
    }

    // Return the parsed response.
    if (logger.isDebugEnabled())
      logger.debug("parsedResponse = " + parsedResponse);
    return parsedResponse;
  }

  /**
   * Converts into a list a comma-separated text string representing either an
   * If-Match or an If-None_match header.
   * 
   * @param headerString
   *          A String with the comma-separated text string.
   * @return a List<String> with the header entries.
   */
  private List<String> makeIfMatchNoneMatchHeaderList(String headerString) {
    if (logger.isDebugEnabled()) logger.debug("headerString = " + headerString);

    if (headerString == null) {
      return null;
    }

    List<String> headerList = new ArrayList<>();

    // Loop through all the comma-separated entries in the text string.
    for (String item : StringUtil.breakAt(headerString, ",")) {
      // Check whether it is an asterisk or a weak validator tag.
      if (ASTERISK_PRECONDITION.equals(item.trim())
	  || item.trim().startsWith(HTTP_WEAK_VALIDATOR_PREFIX)) {
	// Yes: Use it as-is.
	headerList.add(item.trim());
      } else {
	// No: Surround it with double quotes.
	headerList.add("\"" + item.trim() + "\"");
      }
    }

    if (logger.isDebugEnabled()) logger.debug("headerList = " + headerList);
    return headerList;
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
   * Performs a GET operation for a configuration section using the REST service
   * client.
   * 
   * @param snId
   *          A String with the configuration section name.
   * @param ifMatch
   *          A String with the timestamps to be specified in the request
   *          If-Match header.
   * @param ifNoneMatch
   *          A String with the timestamps to be specified in the request
   *          If-None-Match header.
   * @param expectedStatus
   *          An HttpStatus with the HTTP status of the result.
   * @param expectedErrorMessagePrefix
   *          A String with the beginning of the error message.
   * @return a MultipartResponse with the multipart response.
   */
  private MultipartResponse runTestGetConfigSectionClient(String snId,
      String ifMatch, String ifNoneMatch, HttpStatus expectedStatus,
      String expectedErrorMessagePrefix) {
    if (logger.isDebugEnabled()) {
      logger.debug("snId = " + snId);
      logger.debug("ifMatch = " + ifMatch);
      logger.debug("ifNoneMatch = " + ifNoneMatch);
      logger.debug("expectedStatus = " + expectedStatus);
      logger.debug("expectedErrorMessagePrefix = "
	  + expectedErrorMessagePrefix);
    }

    RestConfigSection input = new RestConfigSection();
    input.setSectionName(snId);

    if (ifMatch != null && !ifMatch.isEmpty()) {
      input.setIfMatch(makeIfMatchNoneMatchHeaderList(ifMatch));
    }

    if (ifNoneMatch != null && !ifNoneMatch.isEmpty()) {
      input.setIfNoneMatch(makeIfMatchNoneMatchHeaderList(ifNoneMatch));
    }

    // Make the request and get the result.
    RestConfigSection output = getRestConfigClient().getConfigSection(input);

    // Check the response status.
    assertEquals(expectedStatus, output.getStatusCode());

    // Check the error message.
    String errorMessage = output.getErrorMessage();

    if (expectedErrorMessagePrefix == null) {
      assertNull(errorMessage);
    } else {
      assertNotNull(errorMessage);
      assertTrue(errorMessage.startsWith(expectedErrorMessagePrefix));
    }

    MultipartResponse parsedResponse = output.getResponse();

    // Return the parsed response.
    if (logger.isDebugEnabled())
      logger.debug("parsedResponse = " + parsedResponse);
    return parsedResponse;
  }

  /**
   * Provides the etag containing the last modification timestamp of a
   * configuration file obtained in a response after validating the response.
   * 
   * @param response
   *          A MultipartResponse with the response.
   * @param expectedContentType
   *          A MediaType with the expected content type of the file.
   * @param expectedPayloads
   *          A List<String> with text expected to be part of the response
   *          payload.
   * @return a String with the etag containing the last modification timestamp
   *         of the configuration file.
   * @throws Exception
   *           if there are problems.
   */
  private String verifyMultipartResponse(MultipartResponse response,
      MediaType expectedContentType, List<String> expectedPayloads)
	  throws Exception {
    // Validate the response content type.
    HttpHeaders responseHeaders = response.getResponseHeaders();
    assertTrue(responseHeaders.containsKey(HttpHeaders.CONTENT_TYPE));

    assertTrue(responseHeaders.getContentType().toString()
	.startsWith(MediaType.MULTIPART_FORM_DATA_VALUE + ";boundary="));

    // Get the configuration file part.
    Map<String, Part> parts = response.getParts();
    assertTrue(parts.containsKey(CONFIG_PART_NAME));
    Part part = parts.get(CONFIG_PART_NAME);

    // Validate the part content type.
    Map<String, String> partHeaders = part.getHeaders();
    assertTrue(partHeaders.containsKey(HttpHeaders.CONTENT_TYPE));
    assertEquals(expectedContentType.toString(),
	partHeaders.get(HttpHeaders.CONTENT_TYPE));

    // Get the part payload content length.
    assertTrue(partHeaders.containsKey(HttpHeaders.CONTENT_LENGTH));
    long contentLength = part.getContentLength();

    // Get the part payload.
    String payload = StringUtil.fromInputStream(part.getInputStream());
    assertEquals(contentLength, payload.length());

    // Validate the part payload.
    if (expectedPayloads.size() > 0) {
      String expectedPayload =
	  StringUtil.separatedString(expectedPayloads, "\n");
      assertTrue(payload.indexOf(expectedPayload) >= 0);
    } else {
      assertEquals(0, contentLength);
    }

    // Get the part last modification timestamp.
    assertTrue(partHeaders.containsKey(HttpHeaders.ETAG));
    String etag = part.getEtag();

    if (logger.isDebugEnabled()) logger.debug("etag = " + etag);
    return etag;
  }

  /**
   * Runs the getConfigUrl()-related un-authenticated-specific tests.
   * 
   * @throws Exception
   *           if there are problems.
   */
  private void getConfigUrlUnAuthenticatedTest() throws Exception {
    if (logger.isDebugEnabled()) logger.debug("Invoked.");

    // No URL.
    runTestGetConfigUrl(null, null, null, null, null, null,
	HttpStatus.NOT_FOUND);

    // Empty URL.
    runTestGetConfigUrl(EMPTY_STRING, null, null, null, null, null,
	HttpStatus.NOT_FOUND);

    String url = "http://something";

    // Use defaults for all headers.
    runTestGetConfigUrl(url, null, null, null, null, null,
	HttpStatus.BAD_REQUEST);

    // Bad Accept header content type.
    runTestGetConfigUrl(url, null, null, ZERO, null, null,
	HttpStatus.NOT_ACCEPTABLE);

    // Bad Accept header content type.
    runTestGetConfigUrl(url, MediaType.APPLICATION_JSON, null, null, null, null,
	HttpStatus.NOT_ACCEPTABLE);

    // Bad Accept header content type.
    runTestGetConfigUrl(url, MediaType.APPLICATION_JSON, null, ZERO, null, null,
	HttpStatus.NOT_ACCEPTABLE);

    // Good Accept header content type.
    runTestGetConfigUrl(url, MediaType.MULTIPART_FORM_DATA, null, null, null,
	null, HttpStatus.BAD_REQUEST);

    // Bad Accept header content type.
    runTestGetConfigUrl(url, null, null, null, BAD_USER, BAD_PWD,
	HttpStatus.NOT_ACCEPTABLE);

    // Bad Accept header content type.
    runTestGetConfigUrl(url, null, null, ZERO, BAD_USER, BAD_PWD,
	HttpStatus.NOT_ACCEPTABLE);

    // Bad Accept header content type.
    runTestGetConfigUrl(url, MediaType.APPLICATION_JSON, null, null, BAD_USER,
	BAD_PWD, HttpStatus.NOT_ACCEPTABLE);

    // Bad Accept header content type.
    runTestGetConfigUrl(url, MediaType.APPLICATION_JSON, null, ZERO, BAD_USER,
	BAD_PWD, HttpStatus.NOT_ACCEPTABLE);

    // Good Accept header content type.
    runTestGetConfigUrl(url, MediaType.MULTIPART_FORM_DATA, null, null,
	BAD_USER, BAD_PWD, HttpStatus.BAD_REQUEST);

    url = "dyn:cluster.xml";

    MultipartResponse configOutput = runTestGetConfigUrl(url,
	MediaType.MULTIPART_FORM_DATA, null, null, null, null, HttpStatus.OK);

    List<String> expectedPayloads = ListUtil.list(
	"<lockss-config>",
	"  <property name=\"org.lockss.auxPropUrls\">",
	"    <list append=\"false\">",
	"",
	"      <!-- Put static URLs here -->",
	"",
	"    </list>",
	"  </property>",
	"</lockss-config>"
	);

    String etag = verifyMultipartResponse(configOutput, MediaType.TEXT_XML,
	expectedPayloads);
    assertTrue(Long.parseLong(etag) <= TimeBase.nowMs());

    // Not modified since last read.
    runTestGetConfigUrl(url, MediaType.MULTIPART_FORM_DATA, null, etag, null,
	null, HttpStatus.NOT_MODIFIED);

    configOutput = runTestGetConfigUrl(url, MediaType.MULTIPART_FORM_DATA, null,
	ZERO, null, null, HttpStatus.OK);

    String etag2 = verifyMultipartResponse(configOutput, MediaType.TEXT_XML,
	expectedPayloads);
    assertEquals(etag, etag2);
    assertTrue(Long.parseLong(etag2) <= TimeBase.nowMs());

    configOutput = runTestGetConfigUrl(url, MediaType.MULTIPART_FORM_DATA, null,
	NUMBER, null, null, HttpStatus.OK);

    etag2 = verifyMultipartResponse(configOutput, MediaType.TEXT_XML,
	expectedPayloads);
    assertEquals(etag, etag2);
    assertTrue(Long.parseLong(etag2) <= TimeBase.nowMs());

    runTestGetConfigUrl(url, MediaType.MULTIPART_FORM_DATA, null,
	ASTERISK_PRECONDITION, null, null, HttpStatus.NOT_MODIFIED);

    configOutput = runTestGetConfigUrl(url, MediaType.MULTIPART_FORM_DATA, null,
	null, BAD_USER, BAD_PWD, HttpStatus.OK);

    etag2 = verifyMultipartResponse(configOutput, MediaType.TEXT_XML,
	expectedPayloads);
    assertEquals(etag, etag2);
    assertTrue(Long.parseLong(etag2) <= TimeBase.nowMs());

    configOutput = runTestGetConfigUrl(url, MediaType.MULTIPART_FORM_DATA, null,
	ZERO, BAD_USER, BAD_PWD, HttpStatus.OK);

    etag2 = verifyMultipartResponse(configOutput, MediaType.TEXT_XML,
	expectedPayloads);
    assertEquals(etag, etag2);
    assertTrue(Long.parseLong(etag2) <= TimeBase.nowMs());

    configOutput = runTestGetConfigUrl(url, MediaType.MULTIPART_FORM_DATA, null,
	NUMBER, BAD_USER, BAD_PWD, HttpStatus.OK);

    etag2 = verifyMultipartResponse(configOutput, MediaType.TEXT_XML,
	expectedPayloads);
    assertEquals(etag, etag2);
    assertTrue(Long.parseLong(etag2) <= TimeBase.nowMs());

    runTestGetConfigUrl(url, MediaType.MULTIPART_FORM_DATA, null,
	ASTERISK_PRECONDITION, BAD_USER, BAD_PWD, HttpStatus.NOT_MODIFIED);

    // Match of the If-Match precondition.
    runTestGetConfigUrl(url, MediaType.MULTIPART_FORM_DATA, etag, null, null,
	null, HttpStatus.OK);

    runTestGetConfigUrl(url, MediaType.MULTIPART_FORM_DATA, etag, null,
	BAD_USER, BAD_PWD, HttpStatus.OK);

    // Mismatch of the If-Match precondition.
    runTestGetConfigUrl(url, MediaType.MULTIPART_FORM_DATA, NUMBER, null, null,
	null, HttpStatus.PRECONDITION_FAILED);

    runTestGetConfigUrl(url, MediaType.MULTIPART_FORM_DATA, NUMBER, null,
	BAD_USER, BAD_PWD, HttpStatus.PRECONDITION_FAILED);

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

    // No URL: Spring checks the Accept header before credentials.
    runTestGetConfigUrl(null, null, null, null, null, null,
	HttpStatus.NOT_ACCEPTABLE);

    // Empty URL: Spring checks the Accept header before credentials.
    runTestGetConfigUrl(EMPTY_STRING, null, null, null, null, null,
	HttpStatus.NOT_ACCEPTABLE);

    String url = "http://something";

    // Missing Accept header: Spring checks the Accept header
    // before credentials.
    runTestGetConfigUrl(url, null, null, null, null, null,
	HttpStatus.NOT_ACCEPTABLE);

    // Missing credentials.
    runTestGetConfigUrl(url, MediaType.APPLICATION_JSON, null, null, null, null,
	HttpStatus.UNAUTHORIZED);

    // Missing credentials.
    runTestGetConfigUrl(url, MediaType.MULTIPART_FORM_DATA, null, null, null,
	null, HttpStatus.UNAUTHORIZED);

    // Missing credentials.
    runTestGetConfigUrl(url, null, null, ZERO, null, null,
	HttpStatus.UNAUTHORIZED);

    // Missing credentials.
    runTestGetConfigUrl(url, MediaType.APPLICATION_JSON, null, ZERO, null, null,
	HttpStatus.UNAUTHORIZED);

    // Missing credentials.
    runTestGetConfigUrl(url, MediaType.MULTIPART_FORM_DATA, null, ZERO, null,
	null, HttpStatus.UNAUTHORIZED);

    // Bad credentials.
    runTestGetConfigUrl(url, null, null, null, BAD_USER, BAD_PWD,
	HttpStatus.UNAUTHORIZED);

    // Bad credentials.
    runTestGetConfigUrl(url, null, null, ZERO, BAD_USER, BAD_PWD,
	HttpStatus.UNAUTHORIZED);

    // Bad credentials.
    runTestGetConfigUrl(url, MediaType.APPLICATION_JSON, null, null, BAD_USER,
	BAD_PWD, HttpStatus.UNAUTHORIZED);

    // Bad credentials.
    runTestGetConfigUrl(url, MediaType.APPLICATION_JSON, null, ZERO, BAD_USER,
	BAD_PWD, HttpStatus.UNAUTHORIZED);

    // Bad credentials.
    runTestGetConfigUrl(url, MediaType.MULTIPART_FORM_DATA, null, null,
	BAD_USER, BAD_PWD, HttpStatus.UNAUTHORIZED);

    // Bad credentials.
    runTestGetConfigUrl(url, MediaType.MULTIPART_FORM_DATA, null, ZERO,
	BAD_USER, BAD_PWD, HttpStatus.UNAUTHORIZED);

    url = "http://localhost:12345";

    // Missing Accept header for UNAUTHORIZED response.
    runTestGetConfigUrl(url, null, null, null, null, null,
	HttpStatus.NOT_ACCEPTABLE);

    // Missing credentials.
    runTestGetConfigUrl(url, MediaType.APPLICATION_JSON, null, null, null, null,
	HttpStatus.UNAUTHORIZED);

    // Missing credentials.
    runTestGetConfigUrl(url, MediaType.MULTIPART_FORM_DATA, null, null, null,
	null, HttpStatus.UNAUTHORIZED);

    // Missing credentials.
    runTestGetConfigUrl(url, null, null, ZERO, null, null,
	HttpStatus.UNAUTHORIZED);

    // Missing credentials.
    runTestGetConfigUrl(url, MediaType.APPLICATION_JSON, null, ZERO, null, null,
	HttpStatus.UNAUTHORIZED);

    // Missing credentials.
    runTestGetConfigUrl(url, MediaType.MULTIPART_FORM_DATA, null, ZERO, null,
	null, HttpStatus.UNAUTHORIZED);

    // Bad credentials.
    runTestGetConfigUrl(url, null, null, null, BAD_USER, BAD_PWD,
	HttpStatus.UNAUTHORIZED);

    // Bad credentials.
    runTestGetConfigUrl(url, null, null, ZERO, BAD_USER, BAD_PWD,
	HttpStatus.UNAUTHORIZED);

    // Bad credentials.
    runTestGetConfigUrl(url, MediaType.APPLICATION_JSON, null, null, BAD_USER,
	BAD_PWD, HttpStatus.UNAUTHORIZED);

    // Bad credentials.
    runTestGetConfigUrl(url, MediaType.APPLICATION_JSON, null, ZERO, BAD_USER,
	BAD_PWD, HttpStatus.UNAUTHORIZED);

    // Bad credentials.
    runTestGetConfigUrl(url, MediaType.MULTIPART_FORM_DATA, null, null,
	BAD_USER, BAD_PWD, HttpStatus.UNAUTHORIZED);

    // Bad credentials.
    runTestGetConfigUrl(url, MediaType.MULTIPART_FORM_DATA, null, ZERO,
	BAD_USER, BAD_PWD, HttpStatus.UNAUTHORIZED);

    url = "http://example.com";

    // Missing Accept header for UNAUTHORIZED response.
    runTestGetConfigUrl(url, null, null, null, null, null,
	HttpStatus.NOT_ACCEPTABLE);

    // Missing credentials.
    runTestGetConfigUrl(url, MediaType.APPLICATION_JSON, null, null, null,
	null, HttpStatus.UNAUTHORIZED);

    // Missing credentials.
    runTestGetConfigUrl(url, MediaType.MULTIPART_FORM_DATA, null, null, null,
	null, HttpStatus.UNAUTHORIZED);

    // Missing credentials.
    runTestGetConfigUrl(url, null, null, ZERO, null, null,
	HttpStatus.UNAUTHORIZED);

    // Missing credentials.
    runTestGetConfigUrl(url, MediaType.APPLICATION_JSON, ZERO, null, null, null,
	HttpStatus.UNAUTHORIZED);

    // Missing credentials.
    runTestGetConfigUrl(url, MediaType.MULTIPART_FORM_DATA, null, ZERO, null,
	null, HttpStatus.UNAUTHORIZED);

    // Bad credentials.
    runTestGetConfigUrl(url, null, null, null, BAD_USER, BAD_PWD,
	HttpStatus.UNAUTHORIZED);

    // Bad credentials.
    runTestGetConfigUrl(url, null, null, ZERO, BAD_USER, BAD_PWD,
	HttpStatus.UNAUTHORIZED);

    // Bad credentials.
    runTestGetConfigUrl(url, MediaType.APPLICATION_JSON, null, null, BAD_USER,
	BAD_PWD, HttpStatus.UNAUTHORIZED);

    // Bad credentials.
    runTestGetConfigUrl(url, MediaType.APPLICATION_JSON, null, ZERO, BAD_USER,
	BAD_PWD, HttpStatus.UNAUTHORIZED);

    // Bad credentials.
    runTestGetConfigUrl(url, MediaType.MULTIPART_FORM_DATA, null, null,
	BAD_USER, BAD_PWD, HttpStatus.UNAUTHORIZED);

    // Bad credentials.
    runTestGetConfigUrl(url, MediaType.MULTIPART_FORM_DATA, null, ZERO,
	BAD_USER, BAD_PWD, HttpStatus.UNAUTHORIZED);

    url = "dyn:cluster.xml";

    runTestGetConfigUrl(url, MediaType.MULTIPART_FORM_DATA, null, null, null,
	null, HttpStatus.UNAUTHORIZED);

    runTestGetConfigUrl(url, MediaType.MULTIPART_FORM_DATA, null, ZERO, null,
	null, HttpStatus.UNAUTHORIZED);

    runTestGetConfigUrl(url, MediaType.MULTIPART_FORM_DATA, null, NUMBER, null,
	null, HttpStatus.UNAUTHORIZED);

    runTestGetConfigUrl(url, MediaType.MULTIPART_FORM_DATA, null,
	ASTERISK_PRECONDITION, null, null, HttpStatus.UNAUTHORIZED);

    runTestGetConfigUrl(url, MediaType.MULTIPART_FORM_DATA, null, null,
	BAD_USER, BAD_PWD, HttpStatus.UNAUTHORIZED);

    runTestGetConfigUrl(url, MediaType.MULTIPART_FORM_DATA, null, ZERO,
	BAD_USER, BAD_PWD, HttpStatus.UNAUTHORIZED);

    runTestGetConfigUrl(url, MediaType.MULTIPART_FORM_DATA, null, NUMBER,
	BAD_USER, BAD_PWD, HttpStatus.UNAUTHORIZED);

    runTestGetConfigUrl(url, MediaType.MULTIPART_FORM_DATA, null,
	ASTERISK_PRECONDITION, BAD_USER, BAD_PWD, HttpStatus.UNAUTHORIZED);

    runTestGetConfigUrl(url, MediaType.MULTIPART_FORM_DATA, NUMBER, null, null,
	null, HttpStatus.UNAUTHORIZED);

    runTestGetConfigUrl(url, MediaType.MULTIPART_FORM_DATA, NUMBER, null,
	BAD_USER, BAD_PWD, HttpStatus.UNAUTHORIZED);

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

    // No URL.
    runTestGetConfigUrl(null, null, null, null, GOOD_USER, GOOD_PWD,
	HttpStatus.NOT_ACCEPTABLE);

    // Empty URL.
    runTestGetConfigUrl(EMPTY_STRING, null, null, null, GOOD_USER, GOOD_PWD,
	HttpStatus.NOT_ACCEPTABLE);

    String url = "http://something";

    // Bad Accept header content type.
    runTestGetConfigUrl(url, null, null, null, GOOD_USER, GOOD_PWD,
	HttpStatus.NOT_ACCEPTABLE);

    // Bad Accept header content type.
    runTestGetConfigUrl(url, MediaType.APPLICATION_JSON, null, null, GOOD_USER,
	GOOD_PWD, HttpStatus.NOT_ACCEPTABLE);

    // Bad Accept header content type.
    runTestGetConfigUrl(url, MediaType.APPLICATION_JSON, null, ZERO, GOOD_USER,
	GOOD_PWD, HttpStatus.NOT_ACCEPTABLE);

    // Bad Accept header content type.
    runTestGetConfigUrl(url, null, null, ZERO, GOOD_USER, GOOD_PWD,
	HttpStatus.NOT_ACCEPTABLE);

    // Not found.
    runTestGetConfigUrl(url, MediaType.MULTIPART_FORM_DATA, null, ZERO,
	GOOD_USER, GOOD_PWD, HttpStatus.BAD_REQUEST);

    url = "dyn:cluster.xml";

    MultipartResponse configOutput = runTestGetConfigUrl(url,
	MediaType.MULTIPART_FORM_DATA, null, null, GOOD_USER, GOOD_PWD,
	HttpStatus.OK);

    List<String> expectedPayloads = ListUtil.list(
	"<lockss-config>",
	"  <property name=\"org.lockss.auxPropUrls\">",
	"    <list append=\"false\">",
	"",
	"      <!-- Put static URLs here -->",
	"",
	"    </list>",
	"  </property>",
	"</lockss-config>"
	);

    String etag = verifyMultipartResponse(configOutput, MediaType.TEXT_XML,
	expectedPayloads);
    assertTrue(Long.parseLong(etag) <= TimeBase.nowMs());

    // Independent verification.
    assertTrue(StringUtil.fromInputStream(ConfigManager.getConfigManager()
	.conditionallyReadCacheConfigFile(url, null, null).getInputStream())
	.indexOf(StringUtil.separatedString(expectedPayloads, "\n")) > 0);

    // Not modified since last read.
    runTestGetConfigUrl(url, MediaType.MULTIPART_FORM_DATA, null, etag,
	GOOD_USER, GOOD_PWD, HttpStatus.NOT_MODIFIED);

    configOutput = runTestGetConfigUrl(url, MediaType.MULTIPART_FORM_DATA, null,
	ZERO, GOOD_USER, GOOD_PWD, HttpStatus.OK);

    String etag2 = verifyMultipartResponse(configOutput, MediaType.TEXT_XML,
	expectedPayloads);
    assertEquals(etag, etag2);
    assertTrue(Long.parseLong(etag2) <= TimeBase.nowMs());

    configOutput = runTestGetConfigUrl(url, MediaType.MULTIPART_FORM_DATA, null,
	NUMBER, GOOD_USER, GOOD_PWD, HttpStatus.OK);

    etag2 = verifyMultipartResponse(configOutput, MediaType.TEXT_XML,
	expectedPayloads);
    assertEquals(etag, etag2);
    assertTrue(Long.parseLong(etag2) <= TimeBase.nowMs());

    runTestGetConfigUrl(url, MediaType.MULTIPART_FORM_DATA, null,
	ASTERISK_PRECONDITION, GOOD_USER, GOOD_PWD, HttpStatus.NOT_MODIFIED);

    // Match of the If-Match precondition.
    runTestGetConfigUrl(url, MediaType.MULTIPART_FORM_DATA, etag, null,
	GOOD_USER, GOOD_PWD, HttpStatus.OK);

    // Mismatch of the If-Match precondition.
    runTestGetConfigUrl(url, MediaType.MULTIPART_FORM_DATA, NUMBER, null,
	GOOD_USER, GOOD_PWD, HttpStatus.PRECONDITION_FAILED);

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
   * @param ifMatch
   *          A String with the timestamps to be specified in the request
   *          If-Match header.
   * @param ifNoneMatch
   *          A String with the timestamps to be specified in the request
   *          If-None-Match header.
   * @param user
   *          A String with the request username.
   * @param password
   *          A String with the request password.
   * @param expectedStatus
   *          An HttpStatus with the HTTP status of the result.
   * @return a MultipartResponse with the multipart response.
   * @throws Exception
   *           if there are problems.
   */
  private MultipartResponse runTestGetConfigUrl(String url,
      MediaType acceptContentType, String ifMatch, String ifNoneMatch,
      String user, String password, HttpStatus expectedStatus)
	  throws Exception {
    if (logger.isDebugEnabled()) {
      logger.debug("url = " + url);
      logger.debug("acceptContentType = " + acceptContentType);
      logger.debug("ifMatch = " + ifMatch);
      logger.debug("ifNoneMatch = " + ifNoneMatch);
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
    if (acceptContentType != null || ifMatch != null || ifNoneMatch != null
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

      // Check whether there is a custom If-Match header.
      if (ifMatch != null) {
	// Yes: Set the If-Match header.
	headers.setIfMatch(makeIfMatchNoneMatchHeaderList(ifMatch));
      }

      // Check whether there is a custom If-None-Match header.
      if (ifNoneMatch != null) {
	// Yes: Set the If-None-Match header.
	headers.setIfNoneMatch(makeIfMatchNoneMatchHeaderList(ifNoneMatch));
      }

      // Set up  the authentication credentials, if necessary.
      setUpCredentials(user, password, headers);

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

    MultipartResponse parsedResponse = null;

    // Check whether it is a success response.
    if (isSuccess(statusCode)) {
      // Yes: Parse it.
      parsedResponse = new MultipartResponse(response);
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

    runTestGetLastUpdateTime(null, null, HttpStatus.OK);
    runTestGetLastUpdateTime(BAD_USER, BAD_PWD, HttpStatus.OK);

    getLastUpdateTimeCommonTest();

    if (logger.isDebugEnabled()) logger.debug("Done.");
  }

  /**
   * Runs the getLastUpdateTime()-related authenticated-specific tests.
   */
  private void getLastUpdateTimeAuthenticatedTest() {
    if (logger.isDebugEnabled()) logger.debug("Invoked.");

    runTestGetLastUpdateTime(null, null, HttpStatus.UNAUTHORIZED);
    runTestGetLastUpdateTime(BAD_USER, BAD_PWD, HttpStatus.UNAUTHORIZED);

    getLastUpdateTimeCommonTest();

    if (logger.isDebugEnabled()) logger.debug("Done.");
  }

  /**
   * Runs the getLastUpdateTime()-related authentication-independent tests.
   */
  private void getLastUpdateTimeCommonTest() {
    if (logger.isDebugEnabled()) logger.debug("Invoked.");

    runTestGetLastUpdateTime(GOOD_USER, GOOD_PWD, HttpStatus.OK);

    if (logger.isDebugEnabled()) logger.debug("Done.");
  }

  /**
   * Performs a GET lastupdatetime operation.
   * 
   * @param user
   *          A String with the request username.
   * @param password
   *          A String with the request password.
   * @param expectedStatus
   *          An HttpStatus with the HTTP status of the result.
   * @return a Date with the configuration last update time.
   */
  private void runTestGetLastUpdateTime(String user, String password,
      HttpStatus expectedStatus) {
    if (logger.isDebugEnabled()) {
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
    if (user != null || password != null) {
      // Yes: Initialize the request headers.
      HttpHeaders headers = new HttpHeaders();

      // Set up  the authentication credentials, if necessary.
      setUpCredentials(user, password, headers);

      if (logger.isDebugEnabled())
	logger.debug("requestHeaders = " + headers.toSingleValueMap());

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
    if (isSuccess(statusCode)) {
      // Yes: Parse it.
      long lastUpdateTime =
	  new Date(Long.parseLong((String)response.getBody())).getTime();

      // Validate it.
      assertEquals(ConfigManager.getConfigManager().getLastUpdateTime(),
	  lastUpdateTime);

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

    runTestGetLoadedUrlList(null, null, HttpStatus.OK);
    runTestGetLoadedUrlList(BAD_USER, BAD_PWD, HttpStatus.OK);

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

    runTestGetLoadedUrlList(null, null, HttpStatus.UNAUTHORIZED);
    runTestGetLoadedUrlList(BAD_USER, BAD_PWD, HttpStatus.UNAUTHORIZED);

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

    runTestGetLoadedUrlList(GOOD_USER, GOOD_PWD, HttpStatus.OK);

    if (logger.isDebugEnabled()) logger.debug("Done.");
  }

  /**
   * Performs a GET loadedurls operation.
   * 
   * @param user
   *          A String with the request username.
   * @param password
   *          A String with the request password.
   * @param expectedStatus
   *          An HttpStatus with the HTTP status of the result.
   * @return a List<String> with the loaded URLs.
   * 
   * @throws Exception
   *           if there are problems.
   */
  private void runTestGetLoadedUrlList(String user, String password,
      HttpStatus expectedStatus) throws Exception {
    if (logger.isDebugEnabled()) {
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
    if (user != null || password != null) {
      // Yes: Initialize the request headers.
      HttpHeaders headers = new HttpHeaders();

      // Set up  the authentication credentials, if necessary.
      setUpCredentials(user, password, headers);

      if (logger.isDebugEnabled())
	logger.debug("requestHeaders = " + headers.toSingleValueMap());

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
    if (isSuccess(statusCode)) {
      // Yes: Parse it.
      ObjectMapper mapper = new ObjectMapper();
      List<String> result = mapper.readValue((String)response.getBody(),
	  new TypeReference<List<String>>(){});

      assertEquals(ConfigManager.getConfigManager().getLoadedUrlList(), result);
    }
  }

  /**
   * Runs the putConfig()-related un-authenticated-specific tests.
   */
  private void putConfigUnAuthenticatedTest() throws Exception {
    if (logger.isDebugEnabled()) logger.debug("Invoked.");

    // No section.
    runTestPutConfig(null, null, null, null, null, null, null,
	HttpStatus.NOT_FOUND);

    // Empty section.
    runTestPutConfig(null, EMPTY_STRING, null, null, null, null, null,
	HttpStatus.NOT_FOUND);

    // Missing Content-Type header.
    runTestPutConfig(null, ConfigApi.SECTION_NAME_PLUGIN, null, null, null,
	null, null, HttpStatus.UNSUPPORTED_MEDIA_TYPE);

    // Missing Content-Type header.
    runTestPutConfig(null, ConfigApi.SECTION_NAME_PLUGIN, null, null, null,
	BAD_USER, BAD_PWD, HttpStatus.UNSUPPORTED_MEDIA_TYPE);

    // Bad Content-Type header.
    runTestPutConfig(null, ConfigApi.SECTION_NAME_PLUGIN,
	MediaType.APPLICATION_JSON, null, null, null, null,
	HttpStatus.UNSUPPORTED_MEDIA_TYPE);

    // Missing payload (This should return HttpStatus.BAD_REQUEST, but Spring
    // returns HttpStatus.INTERNAL_SERVER_ERROR).
    runTestPutConfig(null, ConfigApi.SECTION_NAME_PLUGIN,
	MediaType.MULTIPART_FORM_DATA, null, null, null, null,
	HttpStatus.INTERNAL_SERVER_ERROR);

    // Missing payload (This should return HttpStatus.BAD_REQUEST, but Spring
    // returns HttpStatus.INTERNAL_SERVER_ERROR).
    runTestPutConfig(null, ConfigApi.SECTION_NAME_PLUGIN,
	MediaType.MULTIPART_FORM_DATA, null, null, BAD_USER, BAD_PWD,
	HttpStatus.INTERNAL_SERVER_ERROR);

    // Missing precondition.
    runTestPutConfig("a1=b1", ConfigApi.SECTION_NAME_PLUGIN, null, null, null,
	null, null, HttpStatus.OK);

    // Missing eTag.
    runTestPutConfig("a1=b2", ConfigApi.SECTION_NAME_PLUGIN, null, null, null,
	BAD_USER, BAD_PWD, HttpStatus.OK);

    // Bad Content-Type header.
    try {
      runTestPutConfig("a1=b3", ConfigApi.SECTION_NAME_PLUGIN,
	  MediaType.APPLICATION_JSON, null, null, null, null,
	  HttpStatus.BAD_REQUEST);
      fail("Should have thrown HttpMessageNotWritableException");
    } catch (HttpMessageNotWritableException hmnwe) {
      assertTrue(hmnwe.getMessage().startsWith("Could not write JSON: "
	  + "No serializer found for class java.io.ByteArrayInputStream"));
    }

    // Missing If-Match header.
    runTestPutConfig("a1=b3", ConfigApi.SECTION_NAME_PLUGIN,
	MediaType.MULTIPART_FORM_DATA, null, null, null, null, HttpStatus.OK);

    // Time before write.
    long beforeWrite = TimeBase.nowMs();

    // Missing If-Match header.
    runTestPutConfig("a1=b4", ConfigApi.SECTION_NAME_PLUGIN,
	MediaType.MULTIPART_FORM_DATA, null, null, BAD_USER, BAD_PWD,
	HttpStatus.OK);

    runTestPutConfig("a1=b5", ConfigApi.SECTION_NAME_PLUGIN, null, ZERO, null,
	null, null, HttpStatus.PRECONDITION_FAILED);

    runTestPutConfig("a1=b5", ConfigApi.SECTION_NAME_PLUGIN, null,
	ZERO + "," + NUMBER, null, null, null, HttpStatus.PRECONDITION_FAILED);

    runTestPutConfig("a1=b5", ConfigApi.SECTION_NAME_PLUGIN, null,
	ZERO + "," + NUMBER + "," + TEXT, null, null, null,
	HttpStatus.PRECONDITION_FAILED);

    MultipartResponse configOutput = runTestGetConfigSection(
	ConfigApi.SECTION_NAME_PLUGIN, MediaType.MULTIPART_FORM_DATA, null,
	ZERO, GOOD_USER, GOOD_PWD, HttpStatus.OK);

    // Time after write.
    long afterWrite = TimeBase.nowMs();

    String etag = verifyMultipartResponse(configOutput, MediaType.TEXT_PLAIN,
	ListUtil.list("a1=b4"));

    long writeTime = Long.parseLong(etag);
    assertTrue(beforeWrite <= writeTime);
    assertTrue(afterWrite >= writeTime);

    // Modified since passed timestamp.
    runTestPutConfig("a2=b2", ConfigApi.SECTION_NAME_PLUGIN, null, ZERO, null,
	BAD_USER, BAD_PWD, HttpStatus.PRECONDITION_FAILED);

    // Time before write.
    beforeWrite = TimeBase.nowMs();

    runTestPutConfig("a2=b2", ConfigApi.SECTION_NAME_PLUGIN, null, etag, null,
	BAD_USER, BAD_PWD, HttpStatus.OK);

    configOutput = runTestGetConfigSection(ConfigApi.SECTION_NAME_PLUGIN,
	MediaType.MULTIPART_FORM_DATA, null, etag, GOOD_USER, GOOD_PWD,
	HttpStatus.OK);

    // Time after write.
    afterWrite = TimeBase.nowMs();

    etag = verifyMultipartResponse(configOutput, MediaType.TEXT_PLAIN,
	ListUtil.list("a2=b2"));

    writeTime = Long.parseLong(etag);
    assertTrue(beforeWrite <= writeTime);
    assertTrue(afterWrite >= writeTime);

    // Bad Content-Type header.
    try {
      runTestPutConfig("a3=b3", ConfigApi.SECTION_NAME_PLUGIN,
	  MediaType.APPLICATION_JSON, ZERO, null, null, null,
	  HttpStatus.BAD_REQUEST);
      fail("Should have thrown HttpMessageNotWritableException");
    } catch (HttpMessageNotWritableException hmnwe) {
      assertTrue(hmnwe.getMessage().startsWith("Could not write JSON: "
	  + "No serializer found for class java.io.ByteArrayInputStream"));
    }

    // Modified since creation time.
    runTestPutConfig("a3=b3", ConfigApi.SECTION_NAME_PLUGIN,
	MediaType.MULTIPART_FORM_DATA, ZERO, null, null, null,
	HttpStatus.PRECONDITION_FAILED);

    // Modified since creation time.
    runTestPutConfig("a3=b3", ConfigApi.SECTION_NAME_PLUGIN,
	MediaType.MULTIPART_FORM_DATA, ZERO, null, BAD_USER, BAD_PWD,
	HttpStatus.PRECONDITION_FAILED);

    runTestPutConfig("a3=b3", ConfigApi.SECTION_NAME_PLUGIN, null,
	etag + "," + NUMBER, null, BAD_USER, BAD_PWD, HttpStatus.OK);

    // The file exists.
    runTestPutConfig("a3=b3", ConfigApi.SECTION_NAME_PLUGIN,
	MediaType.MULTIPART_FORM_DATA, ASTERISK_PRECONDITION, null, BAD_USER,
	BAD_PWD, HttpStatus.OK);

    putConfigCommonTest();

    if (logger.isDebugEnabled()) logger.debug("Done.");
  }

  /**
   * Runs the putConfig()-related authenticated-specific tests.
   */
  private void putConfigAuthenticatedTest() throws Exception {
    if (logger.isDebugEnabled()) logger.debug("Invoked.");

    // No section.
    runTestPutConfig(null, null, null, null, null, null, null,
	HttpStatus.UNAUTHORIZED);

    // Empty section.
    runTestPutConfig(null, EMPTY_STRING, null, null, null, null, null,
	HttpStatus.UNAUTHORIZED);

    // Missing credentials.
    runTestPutConfig(null, ConfigApi.SECTION_NAME_PLUGIN, null, null, null,
	null, null, HttpStatus.UNAUTHORIZED);

    // Bad credentials.
    runTestPutConfig(null, ConfigApi.SECTION_NAME_PLUGIN, null, null, null,
	BAD_USER, BAD_PWD, HttpStatus.UNAUTHORIZED);

    // Missing credentials.
    runTestPutConfig(null, ConfigApi.SECTION_NAME_PLUGIN,
	MediaType.APPLICATION_JSON, null, null, null, null,
	HttpStatus.UNAUTHORIZED);

    // Bad credentials.
    runTestPutConfig(null, ConfigApi.SECTION_NAME_PLUGIN,
	MediaType.APPLICATION_JSON, null, null, BAD_USER, BAD_PWD,
	HttpStatus.UNAUTHORIZED);

    // Missing credentials.
    runTestPutConfig(null, ConfigApi.SECTION_NAME_PLUGIN,
	MediaType.MULTIPART_FORM_DATA, null, null, null, null,
	HttpStatus.UNAUTHORIZED);

    // Bad credentials.
    runTestPutConfig(null, ConfigApi.SECTION_NAME_PLUGIN,
	MediaType.MULTIPART_FORM_DATA, null, null, BAD_USER, BAD_PWD,
	HttpStatus.UNAUTHORIZED);

    // Missing credentials.
    runTestPutConfig("a=b", ConfigApi.SECTION_NAME_PLUGIN, null, null, null,
	null, null, HttpStatus.UNAUTHORIZED);

    // Bad credentials.
    runTestPutConfig("a=b", ConfigApi.SECTION_NAME_PLUGIN, null, null, null,
	BAD_USER, BAD_PWD, HttpStatus.UNAUTHORIZED);

    // Bad Content-Type header.
    try {
      runTestPutConfig("a=b", ConfigApi.SECTION_NAME_PLUGIN,
	  MediaType.APPLICATION_JSON, null, null, null, null,
	  HttpStatus.UNAUTHORIZED);
      fail("Should have thrown HttpMessageNotWritableException");
    } catch (HttpMessageNotWritableException hmnwe) {
      assertTrue(hmnwe.getMessage().startsWith("Could not write JSON: "
	  + "No serializer found for class java.io.ByteArrayInputStream"));
    }

    // Bad Content-Type header.
    try {
      runTestPutConfig("a=b", ConfigApi.SECTION_NAME_PLUGIN,
	  MediaType.APPLICATION_JSON, null, null, BAD_USER, BAD_PWD,
	  HttpStatus.UNAUTHORIZED);
      fail("Should have thrown HttpMessageNotWritableException");
    } catch (HttpMessageNotWritableException hmnwe) {
      assertTrue(hmnwe.getMessage().startsWith("Could not write JSON: "
	  + "No serializer found for class java.io.ByteArrayInputStream"));
    }

    // Missing credentials.
    runTestPutConfig("a=b", ConfigApi.SECTION_NAME_PLUGIN,
	MediaType.MULTIPART_FORM_DATA, null, null, null, null,
	HttpStatus.UNAUTHORIZED);

    // Bad credentials.
    runTestPutConfig("a=b", ConfigApi.SECTION_NAME_PLUGIN,
	MediaType.MULTIPART_FORM_DATA, null, null, BAD_USER, BAD_PWD,
	HttpStatus.UNAUTHORIZED);

    putConfigCommonTest();

    if (logger.isDebugEnabled()) logger.debug("Done.");
  }

  /**
   * Runs the putConfig()-related authentication-independent tests.
   */
  private void putConfigCommonTest() throws Exception {
    if (logger.isDebugEnabled()) logger.debug("Invoked.");

    // No section.
    runTestPutConfig(null, null, null, null, null, GOOD_USER, GOOD_PWD,
	HttpStatus.NOT_FOUND);

    // No section name using the REST service client.
    try {
      runTestPutConfigSectionClient("testKey=testValue", null, null, null,
	  null, null);
      fail("Should have thrown IllegalArgumentException");
    } catch (IllegalArgumentException iae) {
      assertEquals("Invalid section name 'null'", iae.getMessage());
    }

    // Empty section.
    runTestPutConfig(null, EMPTY_STRING, null, null, null, GOOD_USER, GOOD_PWD,
	HttpStatus.NOT_FOUND);

    // Empty section name using the REST service client.
    try {
      runTestPutConfigSectionClient("testKey=testValue", EMPTY_STRING, null,
	  null, null, null);
      fail("Should have thrown IllegalArgumentException");
    } catch (IllegalArgumentException iae) {
      assertEquals("Invalid section name ''", iae.getMessage());
    }

    // Bad preconditions using the REST service client.
    try {
      runTestPutConfigSectionClient("testKey=testValue",
	  ConfigApi.SECTION_NAME_EXPERT, WEAK_PRECONDITION, null, null, null);
      fail("Should have thrown IllegalArgumentException");
    } catch (IllegalArgumentException iae) {
      assertEquals("Invalid If-Match entity tag '" + WEAK_PRECONDITION + "'",
	  iae.getMessage());
    }

    try {
      runTestPutConfigSectionClient("testKey=testValue",
	  ConfigApi.SECTION_NAME_EXPERT, ASTERISK_PRECONDITION + "," + NUMBER,
	  null, null, null);
      fail("Should have thrown IllegalArgumentException");
    } catch (IllegalArgumentException iae) {
      assertEquals("Invalid If-Match entity tag mix", iae.getMessage());
    }

    try {
      runTestPutConfigSectionClient("testKey=testValue",
	  ConfigApi.SECTION_NAME_EXPERT,
	  ASTERISK_PRECONDITION + "," + ASTERISK_PRECONDITION, null, null,
	  null);
      fail("Should have thrown IllegalArgumentException");
    } catch (IllegalArgumentException iae) {
      assertEquals("Invalid If-Match entity tag mix", iae.getMessage());
    }

    // Missing Content-Type header.
    runTestPutConfig(null, ConfigApi.SECTION_NAME_EXPERT, null, null, null,
	GOOD_USER, GOOD_PWD, HttpStatus.UNSUPPORTED_MEDIA_TYPE);

    // Bad Content-Type header.
    runTestPutConfig(null, ConfigApi.SECTION_NAME_EXPERT,
	MediaType.APPLICATION_JSON, null, null, GOOD_USER, GOOD_PWD,
	HttpStatus.UNSUPPORTED_MEDIA_TYPE);

    // Missing payload.
    runTestPutConfig(null, ConfigApi.SECTION_NAME_EXPERT,
	MediaType.MULTIPART_FORM_DATA, null, null, GOOD_USER, GOOD_PWD,
	HttpStatus.INTERNAL_SERVER_ERROR);

    // Missing payload using the REST service client.
    try {
      runTestPutConfigSectionClient(null, ConfigApi.SECTION_NAME_EXPERT, NUMBER,
	  null, null, "Configuration input stream is null");
      fail("Should have thrown IllegalArgumentException");
    } catch (IllegalArgumentException iae) {}

    // Time before write.
    long beforeWrite = TimeBase.nowMs();

    // Missing If-Match header.
    runTestPutConfig("testKey=testValue", ConfigApi.SECTION_NAME_EXPERT,
	MediaType.MULTIPART_FORM_DATA, null, null, GOOD_USER, GOOD_PWD,
	HttpStatus.OK);

    // Missing If-Match header using the REST service client.
    runTestPutConfigSectionClient("testKey=testValue",
	ConfigApi.SECTION_NAME_EXPERT, null, null, HttpStatus.OK,
	HttpStatus.OK.toString());

    // Modified at different time than passed timestamp.
    runTestPutConfig("testKey1=testValue1", ConfigApi.SECTION_NAME_EXPERT,
	MediaType.MULTIPART_FORM_DATA, NUMBER, null, GOOD_USER, GOOD_PWD,
	HttpStatus.PRECONDITION_FAILED);

    // Modified at different time than passed timestamp, using the REST service
    // client.
    runTestPutConfigSectionClient("testKey1=testValue1",
	ConfigApi.SECTION_NAME_EXPERT, NUMBER, null,
	HttpStatus.PRECONDITION_FAILED,
	HttpStatus.PRECONDITION_FAILED.toString());

    // Modified at different time than passed timestamps.
    runTestPutConfig("testKey1=testValue1", ConfigApi.SECTION_NAME_EXPERT,
	MediaType.MULTIPART_FORM_DATA, NUMBER + "," + TEXT, null, GOOD_USER,
	GOOD_PWD, HttpStatus.PRECONDITION_FAILED);

    // Modified at different time than passed timestamps, using the REST service
    // client.
    runTestPutConfigSectionClient("testKey1=testValue1",
	ConfigApi.SECTION_NAME_EXPERT, NUMBER + "," + TEXT, null,
	HttpStatus.PRECONDITION_FAILED,
	HttpStatus.PRECONDITION_FAILED.toString());

    // Modified at different time than passed timestamp.
    runTestPutConfig("testKey1=testValue1", ConfigApi.SECTION_NAME_EXPERT,
	MediaType.MULTIPART_FORM_DATA, ZERO, null, GOOD_USER, GOOD_PWD,
	HttpStatus.PRECONDITION_FAILED);

    // Modified at different time than passed timestamp, using the REST service
    // client.
    runTestPutConfigSectionClient("testKey1=testValue1",
	ConfigApi.SECTION_NAME_EXPERT, ZERO, null,
	HttpStatus.PRECONDITION_FAILED,
	HttpStatus.PRECONDITION_FAILED.toString());

    // Bad Accept header content type.
    runTestGetConfigSection(ConfigApi.SECTION_NAME_EXPERT, null, null, ZERO,
	GOOD_USER, GOOD_PWD, HttpStatus.NOT_ACCEPTABLE);

    // Get the file written earlier..
    MultipartResponse configOutput = runTestGetConfigSection(
	ConfigApi.SECTION_NAME_EXPERT, MediaType.MULTIPART_FORM_DATA, null,
	ZERO, GOOD_USER, GOOD_PWD, HttpStatus.OK);

    // Time after subsequent read.
    long afterRead = TimeBase.nowMs();

    String etag = verifyMultipartResponse(configOutput, MediaType.TEXT_PLAIN,
	ListUtil.list("testKey=testValue"));

    long writeTime = Long.parseLong(etag);
    assertTrue(beforeWrite <= writeTime);
    assertTrue(afterRead >= writeTime);

    // Independent verification.
    String filePath =
	getTempDirPath() + "/cache/config/" + ConfigManager.CONFIG_FILE_EXPERT;

    assertTrue(StringUtil.fromInputStream(ConfigManager.getConfigManager()
	.conditionallyReadCacheConfigFile(filePath, null, null)
	.getInputStream()).indexOf("testKey=testValue") == 0);

    // Modified at different time than passed timestamp.
    runTestPutConfig("testKey1=testValue1\ntestKey2=testValue2",
	ConfigApi.SECTION_NAME_EXPERT, MediaType.MULTIPART_FORM_DATA, ZERO,
	null, GOOD_USER, GOOD_PWD, HttpStatus.PRECONDITION_FAILED);

    // Modified at different time than passed timestamp, using the REST service
    // client.
    runTestPutConfigSectionClient("testKey1=testValue1\ntestKey2=testValue2",
	ConfigApi.SECTION_NAME_EXPERT, ZERO, null,
	HttpStatus.PRECONDITION_FAILED,
	HttpStatus.PRECONDITION_FAILED.toString());

    // Modified at different time than passed timestamps.
    runTestPutConfig("testKey1=testValue1\ntestKey2=testValue2",
	ConfigApi.SECTION_NAME_EXPERT, MediaType.MULTIPART_FORM_DATA,
	ZERO + "," + NUMBER, null, GOOD_USER, GOOD_PWD,
	HttpStatus.PRECONDITION_FAILED);

    // Modified at different time than passed timestamps, using the REST service
    // client.
    runTestPutConfigSectionClient("testKey1=testValue1\ntestKey2=testValue2",
	ConfigApi.SECTION_NAME_EXPERT, ZERO + "," + NUMBER, null,
	HttpStatus.PRECONDITION_FAILED,
	HttpStatus.PRECONDITION_FAILED.toString());

    // Time before write.
    beforeWrite = TimeBase.nowMs();

    // Modified at the passed timestamp.
    runTestPutConfig("testKey1=testValue1\ntestKey2=testValue2",
	ConfigApi.SECTION_NAME_EXPERT, MediaType.MULTIPART_FORM_DATA, etag,
	null, GOOD_USER, GOOD_PWD, HttpStatus.OK);

    // Read file with the old timestamp.
    configOutput = runTestGetConfigSection(ConfigApi.SECTION_NAME_EXPERT,
	MediaType.MULTIPART_FORM_DATA, null, etag, GOOD_USER, GOOD_PWD,
	HttpStatus.OK);

    // Time after subsequent read.
    afterRead = TimeBase.nowMs();

    etag = verifyMultipartResponse(configOutput, MediaType.TEXT_PLAIN,
	ListUtil.list("testKey1=testValue1","testKey2=testValue2"));

    writeTime = Long.parseLong(etag);
    assertTrue(beforeWrite <= writeTime);
    assertTrue(afterRead >= writeTime);

    // Independent verification.
    assertTrue(StringUtil.fromInputStream(ConfigManager.getConfigManager()
	.conditionallyReadCacheConfigFile(filePath, null, null)
	.getInputStream())
	.indexOf("testKey1=testValue1\ntestKey2=testValue2") == 0);

    // Read file with no timestamp using the REST service client.
    RestConfigSection input = new RestConfigSection();
    input.setSectionName(ConfigApi.SECTION_NAME_EXPERT);

    RestConfigClient restConfigClient = getRestConfigClient();
    RestConfigSection output = restConfigClient.getConfigSection(input);

    // ETags should match.
    long writeTime2 = Long.parseLong(output.getEtag());
    assertEquals(writeTime, writeTime2);

    // Time before write.
    beforeWrite = TimeBase.nowMs();
    assertTrue(beforeWrite >= writeTime2);

    // Write file with matching timestamp using the REST service client.
    output.setIfMatch(makeIfMatchNoneMatchHeaderList(output.getEtag() + ","
	+ ZERO));

    String content = "testKey3=testValue3";
    output.setInputStream(new ByteArrayInputStream(content.getBytes("UTF-8")));
    output.setContentLength(content.length());

    RestConfigSection output2 = restConfigClient.putConfigSection(output);
    assertEquals(HttpStatus.OK, output2.getStatusCode());
    writeTime = Long.parseLong(output2.getEtag());
    assertTrue(writeTime >= beforeWrite);

    // Time after write.
    long afterWrite = TimeBase.nowMs();
    assertTrue(afterWrite >= writeTime);

    // Independent verification.
    assertTrue(StringUtil.fromInputStream(ConfigManager.getConfigManager()
	.conditionallyReadCacheConfigFile(filePath, null, null)
	.getInputStream()).indexOf("testKey3=testValue3") == 0);

    // Read file with matching timestamp using the REST service client.
    output2.setIfNoneMatch(makeIfMatchNoneMatchHeaderList(output2.getEtag()
	+ "," + TEXT));

    RestConfigSection output3 = restConfigClient.getConfigSection(output2);
    assertEquals(HttpStatus.NOT_MODIFIED, output3.getStatusCode());

    // Cannot modify virtual sections.
    runTestPutConfig("testKey=testValue", ConfigApi.SECTION_NAME_CLUSTER,
	MediaType.MULTIPART_FORM_DATA, ZERO, null, GOOD_USER, GOOD_PWD,
	HttpStatus.BAD_REQUEST);

    // Cannot modify virtual sections, using the REST service client.
    runTestPutConfigSectionClient("testKey=testValue",
	ConfigApi.SECTION_NAME_CLUSTER, ZERO, null, HttpStatus.BAD_REQUEST,
	HttpStatus.BAD_REQUEST.toString());

    // Bad section name.
    runTestPutConfig("testKey=testValue", BAD_SN, MediaType.MULTIPART_FORM_DATA,
	ZERO, null, GOOD_USER, GOOD_PWD, HttpStatus.BAD_REQUEST);

    // Bad section name using the REST service client.
    runTestPutConfigSectionClient("testKey=testValue", BAD_SN, ZERO, null,
	HttpStatus.BAD_REQUEST, HttpStatus.BAD_REQUEST.toString());

    // Missing file.
    runTestPutConfig("testKey=testValue", ConfigApi.SECTION_NAME_ICP_SERVER,
	MediaType.MULTIPART_FORM_DATA, ASTERISK_PRECONDITION, null, GOOD_USER,
	GOOD_PWD, HttpStatus.PRECONDITION_FAILED);

    // Missing file using the REST service client.
    runTestPutConfigSectionClient("testKey=testValue",
	ConfigApi.SECTION_NAME_ICP_SERVER, ASTERISK_PRECONDITION, null,
	HttpStatus.PRECONDITION_FAILED,
	HttpStatus.PRECONDITION_FAILED.toString());

    // Time before write.
    beforeWrite = TimeBase.nowMs();

    // Write non-existent file.
    runTestPutConfig("testKey4=testValue4", ConfigApi.SECTION_NAME_ICP_SERVER,
	MediaType.MULTIPART_FORM_DATA, null, ASTERISK_PRECONDITION, GOOD_USER,
	GOOD_PWD, HttpStatus.OK);

    // Read file with the old timestamp.
    configOutput = runTestGetConfigSection(ConfigApi.SECTION_NAME_ICP_SERVER,
	MediaType.MULTIPART_FORM_DATA, null, null, GOOD_USER, GOOD_PWD,
	HttpStatus.OK);

    // Time after subsequent read.
    afterRead = TimeBase.nowMs();

    etag = verifyMultipartResponse(configOutput, MediaType.TEXT_PLAIN,
	ListUtil.list("testKey4=testValue4"));

    writeTime = Long.parseLong(etag);
    assertTrue(beforeWrite <= writeTime);
    assertTrue(afterRead >= writeTime);

    // Write again the now-existing file.
    runTestPutConfig("testKey4a=testValue4a", ConfigApi.SECTION_NAME_ICP_SERVER,
	MediaType.MULTIPART_FORM_DATA, null, ASTERISK_PRECONDITION, GOOD_USER,
	GOOD_PWD, HttpStatus.PRECONDITION_FAILED);

    // Time before write.
    beforeWrite = TimeBase.nowMs();

    // Write non-existent file using the REST service client.
    RestConfigSection output4 =
	runTestPutConfigSectionClient("testKey5=testValue5",
	ConfigApi.SECTION_NAME_ACCESS_GROUPS, null, ASTERISK_PRECONDITION,
	HttpStatus.OK, HttpStatus.OK.toString());

    assertEquals(HttpStatus.OK, output4.getStatusCode());
    writeTime = Long.parseLong(output4.getEtag());
    assertTrue(writeTime >= beforeWrite);

    // Time after write.
    afterWrite = TimeBase.nowMs();
    assertTrue(afterWrite >= writeTime);

    // Read file with matching timestamp using the REST service client.
    output4.setIfNoneMatch(makeIfMatchNoneMatchHeaderList(output4.getEtag()
	+ "," + NUMBER));

    RestConfigSection output5 = restConfigClient.getConfigSection(output4);
    assertEquals(HttpStatus.NOT_MODIFIED, output5.getStatusCode());

    // Read file with non-matching timestamp using the REST service client.
    output4.setIfNoneMatch(makeIfMatchNoneMatchHeaderList(TEXT + "," + NUMBER));

    output5 = restConfigClient.getConfigSection(output4);
    assertEquals(HttpStatus.OK, output5.getStatusCode());

    etag = verifyMultipartResponse(output5.getResponse(), MediaType.TEXT_PLAIN,
	ListUtil.list("testKey5=testValue5"));
    assertTrue(Long.parseLong(etag) <= TimeBase.nowMs());

    // Write again the now-existing file using the REST service client.
    runTestPutConfigSectionClient("testKey5a=testValue5a",
	ConfigApi.SECTION_NAME_ACCESS_GROUPS, null, ASTERISK_PRECONDITION,
	HttpStatus.PRECONDITION_FAILED,
	HttpStatus.PRECONDITION_FAILED.toString());

    // Cannot modify virtual sections.
    runTestPutConfig("testKey=testValue", ConfigApi.SECTION_NAME_CLUSTER,
	MediaType.MULTIPART_FORM_DATA, ASTERISK_PRECONDITION, null, GOOD_USER,
	GOOD_PWD, HttpStatus.BAD_REQUEST);

    // Cannot modify virtual sections, using the REST service client.
    runTestPutConfigSectionClient("testKey=testValue",
	ConfigApi.SECTION_NAME_CLUSTER, ASTERISK_PRECONDITION, null,
	HttpStatus.BAD_REQUEST, HttpStatus.BAD_REQUEST.toString());

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
   * @param ifMatch
   *          A String with the timestamp to be specified in the request
   *          If-Match header.
   * @param ifNoneMatch
   *          A String with the timestamp to be specified in the request
   *          If-None-Match header.
   * @param user
   *          A String with the request username.
   * @param password
   *          A String with the request password.
   * @param expectedStatus
   *          An HttpStatus with the HTTP status of the result.
   */
  private void runTestPutConfig(String config, String snId,
      MediaType contentType, String ifMatch, String ifNoneMatch, String user,
      String password, HttpStatus expectedStatus) {
    if (logger.isDebugEnabled()) {
      logger.debug("config = " + config);
      logger.debug("snId = " + snId);
      logger.debug("contentType = " + contentType);
      logger.debug("ifMatch = " + ifMatch);
      logger.debug("ifNoneMatch = " + ifNoneMatch);
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
    if (config != null || contentType != null || ifMatch != null
	|| ifNoneMatch != null || user != null || password != null) {
      // Yes.
      MultiValueMap<String, Object> parts = null;

      // Check whether there is a payload.
      if (config != null) {
	// Yes: Build it.
	HttpHeaders partHeaders = new HttpHeaders();
	partHeaders.setContentType(MediaType.TEXT_PLAIN);

	parts = new LinkedMultiValueMap<String, Object>();

	NamedByteArrayResource resource =
	    new NamedByteArrayResource(CONFIG_PART_NAME, config.getBytes());

	parts.add(CONFIG_PART_NAME,
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

      // Check whether there is a custom If-Match header.
      if (ifMatch != null) {
	// Yes: Set the If-Match header.
	headers.setIfMatch(makeIfMatchNoneMatchHeaderList(ifMatch));
      }

      // Check whether there is a custom If-None-Match header.
      if (ifNoneMatch != null) {
	// Yes: Set the If-None-Match header.
	headers.setIfNoneMatch(makeIfMatchNoneMatchHeaderList(ifNoneMatch));
      }

      // Set up  the authentication credentials, if necessary.
      setUpCredentials(user, password, headers);

      if (logger.isDebugEnabled())
	logger.debug("requestHeaders = " + headers.toSingleValueMap());

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
   * Performs a PUT operation for a configuration section using the REST service
   * client.
   * 
   * @param config
   *          A String with the configuration to be written.
   * @param snId
   *          A String with the configuration section name.
   * @param ifMatch
   *          A String with the timestamp to be specified in the request
   *          If-Match header.
   * @param ifNoneMatch
   *          A String with the timestamp to be specified in the request
   *          If-None-Match header.
   * @param expectedStatus
   *          An HttpStatus with the HTTP status of the result.
   * @param expectedErrorMessagePrefix
   *          A String with the beginning of the error message.
   * @return a RestConfigSection with the result of the operation.
   * @throws Exception
   *           if there are problems.
   */
  private RestConfigSection runTestPutConfigSectionClient(String config,
      String snId, String ifMatch, String ifNoneMatch,
      HttpStatus expectedStatus, String expectedErrorMessagePrefix)
	  throws Exception {
    if (logger.isDebugEnabled()) {
      logger.debug("config = " + config);
      logger.debug("snId = " + snId);
      logger.debug("ifMatch = " + ifMatch);
      logger.debug("ifNoneMatch = " + ifNoneMatch);
      logger.debug("expectedStatus = " + expectedStatus);
      logger.debug("expectedErrorMessagePrefix = "
	  + expectedErrorMessagePrefix);
    }

    RestConfigSection input = new RestConfigSection();
    input.setSectionName(snId);

    if (ifMatch != null && !ifMatch.isEmpty()) {
      input.setIfMatch(makeIfMatchNoneMatchHeaderList(ifMatch));
    }

    if (ifNoneMatch != null && !ifNoneMatch.isEmpty()) {
      input.setIfNoneMatch(makeIfMatchNoneMatchHeaderList(ifNoneMatch));
    }

    if (config != null) {
      input.setInputStream(
	  new ByteArrayInputStream(config.getBytes("UTF-8")));
      input.setContentLength(config.length());
    }

    input.setContentType(MediaType.TEXT_PLAIN_VALUE);

    // Make the request and get the result;
    RestConfigSection output = getRestConfigClient().putConfigSection(input);

    // Check the response status.
    assertEquals(expectedStatus, output.getStatusCode());

    // Check the error message.
    String errorMessage = output.getErrorMessage();

    if (expectedErrorMessagePrefix == null) {
      assertNull(errorMessage);
    } else {
      assertNotNull(errorMessage);
      assertTrue(errorMessage.startsWith(expectedErrorMessagePrefix));
    }

    // Check the response etag.
    if (ifMatch != null && !ASTERISK_PRECONDITION.equals(ifMatch)) {
      assertNotEquals(ifMatch, output.getEtag());
    }

    return output;
  }

  /**
   * Runs the putConfigReload()-related un-authenticated-specific tests.
   */
  private void putConfigReloadUnAuthenticatedTest() {
    if (logger.isDebugEnabled()) logger.debug("Invoked.");

    runTestPutConfigReload(null, null, HttpStatus.OK);
    runTestPutConfigReload(BAD_USER, BAD_PWD, HttpStatus.OK);

    putConfigReloadCommonTest();

    if (logger.isDebugEnabled()) logger.debug("Done.");
  }

  /**
   * Runs the putConfigReload()-related authenticated-specific tests.
   */
  private void putConfigReloadAuthenticatedTest() {
    if (logger.isDebugEnabled()) logger.debug("Invoked.");

    runTestPutConfigReload(null, null, HttpStatus.UNAUTHORIZED);
    runTestPutConfigReload(BAD_USER, BAD_PWD, HttpStatus.UNAUTHORIZED);

    putConfigReloadCommonTest();

    if (logger.isDebugEnabled()) logger.debug("Done.");
  }

  /**
   * Runs the putConfigReload()-related authentication-independent tests.
   */
  private void putConfigReloadCommonTest() {
    if (logger.isDebugEnabled()) logger.debug("Invoked.");

    runTestPutConfigReload(GOOD_USER, GOOD_PWD, HttpStatus.OK);

    if (logger.isDebugEnabled()) logger.debug("Done.");
  }

  /**
   * Performs a PUT config reload operation.
   * 
   * @param user
   *          A String with the request username.
   * @param password
   *          A String with the request password.
   * @param expectedStatus
   *          An HttpStatus with the HTTP status of the result.
   */
  private void runTestPutConfigReload(String user, String password,
      HttpStatus expectedStatus) {
    if (logger.isDebugEnabled()) {
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
    if (user != null || password != null) {
      // Yes: Initialize the request headers.
      HttpHeaders headers = new HttpHeaders();

      // Set up  the authentication credentials, if necessary.
      setUpCredentials(user, password, headers);

      if (logger.isDebugEnabled())
	logger.debug("requestHeaders = " + headers.toSingleValueMap());

      // Create the request entity.
      requestEntity = new HttpEntity<String>(null, headers);
    }

    ConfigManager configManager = ConfigManager.getConfigManager();

    // Get the count of configuration reloading requests before this one.
    int configReloadRequestCounter =
	configManager.getConfigReloadRequestCounter();

    // Make the request and get the response. 
    ResponseEntity<?> response = new TestRestTemplate(restTemplate)
	.exchange(uri, HttpMethod.PUT, requestEntity, Void.class);

    // Get the response status.
    HttpStatus statusCode = response.getStatusCode();
    assertEquals(expectedStatus, statusCode);

    // Check whether it is a success response.
    if (isSuccess(statusCode)) {
      // Yes: The count of configuration reloading requests should have been
      // increased by 1.
      assertEquals(configReloadRequestCounter + 1,
	  configManager.getConfigReloadRequestCounter());
    }
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
   * Provides the REST Configuration service client to be tested.
   * 
   * @return a RestConfigClient with the REST Configuration service client.
   */
  private RestConfigClient getRestConfigClient() {
    return new RestConfigClient("http://" + GOOD_USER + ":" + GOOD_PWD
	+ "@localhost:" + port);
  }
}
