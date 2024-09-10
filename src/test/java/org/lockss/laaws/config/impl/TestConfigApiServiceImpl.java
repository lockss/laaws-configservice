/*

 Copyright (c) 2017-2020 Board of Trustees of Leland Stanford Jr. University,
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
package org.lockss.laaws.config.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.lockss.config.ConfigManager;
import org.lockss.config.HttpRequestPreconditions;
import org.lockss.config.RestConfigClient;
import org.lockss.config.RestConfigSection;
import org.lockss.laaws.config.ConfigApplication;
import org.lockss.log.L4JLogger;
import org.lockss.spring.auth.SpringAuthenticationFilter;
import org.lockss.spring.test.SpringLockssTestCase4;
import org.lockss.test.ConfigurationUtil;
import org.lockss.util.AccessType;
import org.lockss.util.HeaderUtil;
import org.lockss.util.ListUtil;
import org.lockss.util.StringUtil;
import org.lockss.util.rest.LockssResponseErrorHandler;
import org.lockss.util.rest.RestUtil;
import org.lockss.util.rest.exception.LockssRestHttpException;
import org.lockss.util.rest.multipart.MultipartMessage;
import org.lockss.util.rest.multipart.MultipartMessageHttpMessageConverter;
import org.lockss.util.rest.multipart.MultipartResponse;
import org.lockss.util.rest.multipart.MultipartResponse.Part;
import org.lockss.util.rest.multipart.NamedByteArrayResource;
import org.lockss.util.time.TimeBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.MalformedParametersException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.*;

import static org.lockss.config.RestConfigClient.CONFIG_PART_NAME;
import static org.lockss.laaws.config.impl.ConfigApiServiceImpl.*;

/**
 * Test class for org.lockss.laaws.config.api.ConfigApiController.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(
    classes = {ConfigApplication.class},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class TestConfigApiServiceImpl extends SpringLockssTestCase4 {
  private static final L4JLogger log = L4JLogger.getLogger();

  private static final String EMPTY_STRING = "";
  private static final String NUMBER = "1234567890";

  // Preconditions.
  private static final String EMPTY_PRECONDITION = "\"\"";
  private static final String ZERO_PRECONDITION = "\"0\"";
  private static final String NUMERIC_PRECONDITION = "\"" + NUMBER + "\"";
  private static final String ALPHA_PRECONDITION = "\"ABCD\"";

  private static final String ASTERISK_PRECONDITION = "*";

  private static final List<String> EMPTY_PRECONDITION_LIST =
      new ArrayList<String>();

  // Section names.
  private static final String UIIPACCESS = "UI_IP_ACCESS";
  private static final String CLUSTER = "CLUSTER";
  private static final String BAD_SN = "badSectionName";

  // Credentials.
  private final Credentials USER_ADMIN =
      this.new Credentials("lockss-u", "lockss-p");
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
    setUpTempDirectory(TestConfigApiServiceImpl.class.getCanonicalName());

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
   * Runs the tests for the validateSectionName() method.
   * 
   * @throws Exception
   *           if there are problems.
   */
  @Test
  public void validateSectionNameTest() throws Exception {
    log.debug2("Invoked");

    ConfigApiServiceImpl controller = new ConfigApiServiceImpl();

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

    assertEquals(SECTION_NAME_UI_IP_ACCESS,
	controller.validateSectionName(UIIPACCESS, AccessType.WRITE));
    assertEquals(SECTION_NAME_UI_IP_ACCESS,
	controller.validateSectionName(UIIPACCESS.toLowerCase(),
	    AccessType.WRITE));
    assertEquals(SECTION_NAME_UI_IP_ACCESS,
	controller.validateSectionName(UIIPACCESS, AccessType.READ));
    assertEquals(SECTION_NAME_UI_IP_ACCESS,
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

    assertEquals(SECTION_NAME_CLUSTER,
	controller.validateSectionName(CLUSTER, AccessType.READ));
    assertEquals(SECTION_NAME_CLUSTER,
	controller.validateSectionName(CLUSTER.toLowerCase(), AccessType.READ));

    log.debug2("Done");
  }

  /**
   * Runs the full controller tests with authentication turned off.
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

    runGetSwaggerDocsTest(getTestUrlTemplate("/v3/api-docs"));
    runMethodsNotAllowedUnAuthenticatedTest();
    getConfigSectionUnAuthenticatedTest();
    getConfigUrlUnAuthenticatedTest();
    getLastUpdateTimeUnAuthenticatedTest();
    getLoadedUrlListUnAuthenticatedTest();
    putConfigUnAuthenticatedTest();
    putConfigReloadUnAuthenticatedTest();

    log.debug2("Done");
  }

  /**
   * Runs the full controller tests with authentication turned on.
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

     runGetSwaggerDocsTest(getTestUrlTemplate("/v3/api-docs"));
    runMethodsNotAllowedAuthenticatedTest();
    getConfigSectionAuthenticatedTest();
    getConfigUrlAuthenticatedTest();
    getLastUpdateTimeAuthenticatedTest();
    getLoadedUrlListAuthenticatedTest();
    putConfigAuthenticatedTest();
    putConfigReloadAuthenticatedTest();

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

    // No section: Spring reports it cannot find a match to an endpoint.
    runTestMethodNotAllowed(null, null, HttpMethod.POST, HttpStatus.NOT_FOUND);

    // Empty section: Spring reports it cannot find a match to an endpoint.
    runTestMethodNotAllowed(EMPTY_STRING, ANYBODY, HttpMethod.PATCH,
	HttpStatus.NOT_FOUND);

    // Bad section.
    runTestMethodNotAllowed(BAD_SN, ANYBODY, HttpMethod.POST,
	HttpStatus.METHOD_NOT_ALLOWED);

    runTestMethodNotAllowed(BAD_SN, null, HttpMethod.PATCH,
	HttpStatus.METHOD_NOT_ALLOWED);

    // Good section.
    runTestMethodNotAllowed(UIIPACCESS, null, HttpMethod.PATCH,
	HttpStatus.METHOD_NOT_ALLOWED);

    runTestMethodNotAllowed(CLUSTER, ANYBODY, HttpMethod.POST,
	HttpStatus.METHOD_NOT_ALLOWED);

    runMethodsNotAllowedCommonTest();

    log.debug2("Done");
  }

  /**
   * Runs the invalid method-related authenticated-specific tests.
   */
  private void runMethodsNotAllowedAuthenticatedTest() {
    log.debug2("Invoked");

    // No section.
    runTestMethodNotAllowed(null, ANYBODY, HttpMethod.POST,
	HttpStatus.UNAUTHORIZED);

    // Empty section.
    runTestMethodNotAllowed(EMPTY_STRING, null, HttpMethod.PATCH,
	HttpStatus.UNAUTHORIZED);

    // Bad section.
    runTestMethodNotAllowed(BAD_SN, ANYBODY, HttpMethod.POST,
	HttpStatus.UNAUTHORIZED);

    // No credentials.
    runTestMethodNotAllowed(UIIPACCESS, null, HttpMethod.PATCH,
	HttpStatus.UNAUTHORIZED);

    // Bad credentials.
    runTestMethodNotAllowed(CLUSTER, ANYBODY, HttpMethod.POST,
	HttpStatus.UNAUTHORIZED);

    runMethodsNotAllowedCommonTest();

    log.debug2("Done");
  }

  /**
   * Runs the invalid method-related authentication-independent tests.
   */
  private void runMethodsNotAllowedCommonTest() {
    log.debug2("Invoked");

    // No section: Spring reports it cannot find a match to an endpoint.
    runTestMethodNotAllowed(null, USER_ADMIN, HttpMethod.POST,
	HttpStatus.NOT_FOUND);

    // Empty section: Spring reports it cannot find a match to an endpoint.
    runTestMethodNotAllowed(EMPTY_STRING, USER_ADMIN, HttpMethod.PATCH,
	HttpStatus.NOT_FOUND);

    // Bad section.
    runTestMethodNotAllowed(BAD_SN, USER_ADMIN, HttpMethod.PATCH,
	HttpStatus.METHOD_NOT_ALLOWED);

    runTestMethodNotAllowed(BAD_SN, USER_ADMIN, HttpMethod.POST,
	HttpStatus.METHOD_NOT_ALLOWED);

    // Good section.
    runTestMethodNotAllowed(UIIPACCESS, USER_ADMIN, HttpMethod.PATCH,
	HttpStatus.METHOD_NOT_ALLOWED);

    runTestMethodNotAllowed(CLUSTER, USER_ADMIN, HttpMethod.POST,
	HttpStatus.METHOD_NOT_ALLOWED);

    log.debug2("Done");
  }

  /**
   * Performs an operation using a method that is not allowed.
   * 
   * @param snId
   *          A String with the configuration section name.
   * @param credentials
   *          A Credentials with the request credentials.
   * @param method
   *          An HttpMethod with the request method.
   * @param expectedStatus
   *          An HttpStatus with the HTTP status of the result.
   */
  private void runTestMethodNotAllowed(String snId, Credentials credentials,
      HttpMethod method, HttpStatus expectedStatus) {
    log.debug2("snId = {}", snId);
    log.debug2("credentials = {}", credentials);
    log.debug2("method = {}", method);
    log.debug2("expectedStatus = {}", expectedStatus);

    // Get the test URL template.
    String template = getTestUrlTemplate("/config/file/{snid}");

    // Create the URI of the request to the REST service.
    UriComponents uriComponents = UriComponentsBuilder.fromUriString(template)
	.build().expand(Collections.singletonMap("snid", snId));

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
    TestRestTemplate testRestTemplate = new TestRestTemplate(templateBuilder);

    try {
      ResponseEntity<String> response = testRestTemplate
          .exchange(uri, method, requestEntity, String.class);

    } catch (LockssResponseErrorHandler.WrappedLockssRestHttpException e) {
      // Get the response status.
      LockssRestHttpException lhre = e.getLRHE();
      HttpStatus statusCode = lhre.getHttpStatus();
      assertFalse(RestUtil.isSuccess(statusCode));
      assertEquals(expectedStatus, statusCode);
    }
  }

  /**
   * Runs the getConfigSection()-related un-authenticated-specific tests.
   * 
   * @throws Exception
   *           if there are problems.
   */
  private void getConfigSectionUnAuthenticatedTest() throws Exception {
    log.debug2("Invoked");

    HttpRequestPreconditions hrp;

    // No section.
    runTestGetConfigSection(null, null, null, null, HttpStatus.NOT_FOUND);

    // Empty section.
    runTestGetConfigSection(EMPTY_STRING, null, null, null,
	HttpStatus.NOT_FOUND);

    hrp = new HttpRequestPreconditions(null, null, null, null);

    runTestGetConfigSection(EMPTY_STRING, null, hrp, null,
	HttpStatus.NOT_FOUND);

    // Use defaults for all headers.
    runTestGetConfigSection(SECTION_NAME_ALERT, null, null, null,
	HttpStatus.NOT_FOUND);

    // Bad Accept header content type.
    runTestGetConfigSection(SECTION_NAME_ALERT,
	MediaType.APPLICATION_JSON, null, null, HttpStatus.NOT_ACCEPTABLE);

    hrp = new HttpRequestPreconditions(EMPTY_PRECONDITION_LIST, EMPTY_STRING,
	EMPTY_PRECONDITION_LIST, EMPTY_STRING);

    runTestGetConfigSection(SECTION_NAME_ALERT,
	MediaType.APPLICATION_JSON, hrp, null, HttpStatus.NOT_ACCEPTABLE);

    // Good Accept header content type.
    runTestGetConfigSection(SECTION_NAME_ALERT,
	MediaType.MULTIPART_FORM_DATA, null, null, HttpStatus.NOT_FOUND);

    List<String> IfMatchNoMatch = ListUtil.list(EMPTY_PRECONDITION);
    hrp = new HttpRequestPreconditions(IfMatchNoMatch, EMPTY_STRING, null,
	EMPTY_STRING);

    runTestGetConfigSection(SECTION_NAME_ALERT,
	MediaType.MULTIPART_FORM_DATA, hrp, null, HttpStatus.NOT_FOUND);

    // Bad Accept header content type.
    runTestGetConfigSection(SECTION_NAME_ALERT, null, null, ANYBODY,
	HttpStatus.NOT_ACCEPTABLE);

    hrp = new HttpRequestPreconditions(null, EMPTY_STRING, IfMatchNoMatch,
	EMPTY_STRING);

    runTestGetConfigSection(SECTION_NAME_ALERT, null, null, ANYBODY,
	HttpStatus.NOT_ACCEPTABLE);

    // Bad Accept header content type.
    runTestGetConfigSection(SECTION_NAME_ALERT,
	MediaType.APPLICATION_JSON, null, ANYBODY, HttpStatus.NOT_ACCEPTABLE);

    // Good Accept header content type.
    runTestGetConfigSection(SECTION_NAME_ALERT,
	MediaType.MULTIPART_FORM_DATA, null, ANYBODY, HttpStatus.NOT_FOUND);

    // Use defaults for all headers.
    MultipartResponse configOutput = runTestGetConfigSection(
	SECTION_NAME_CLUSTER, null, null, null, HttpStatus.OK);

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

    Part part = verifyMultipartResponse(configOutput, MediaType.TEXT_XML,
	expectedPayloads);

    // Verify the part last modification timestamps.
    verifyPartModificationTimestamps(part, null);

    // Bad Accept header content type.
    runTestGetConfigSection(SECTION_NAME_CLUSTER,
	MediaType.APPLICATION_JSON, null, null, HttpStatus.NOT_ACCEPTABLE);

    // Good Accept header content type.
    configOutput = runTestGetConfigSection(SECTION_NAME_CLUSTER,
	MediaType.MULTIPART_FORM_DATA, null, null, HttpStatus.OK);

    Part part2 = verifyMultipartResponse(configOutput, MediaType.TEXT_XML,
	expectedPayloads);

    // Verify the part last modification timestamps.
    verifyPartModificationTimestamps(part2, part);

    // Bad Accept header content type.
    List<String> ifNoneMatch = ListUtil.list(part2.getEtag());
    hrp = new HttpRequestPreconditions(null, null, ifNoneMatch, null);

    runTestGetConfigSection(SECTION_NAME_CLUSTER, null, hrp, null,
	HttpStatus.NOT_ACCEPTABLE);

    // Bad Accept header content type.
    runTestGetConfigSection(SECTION_NAME_CLUSTER,
	MediaType.APPLICATION_JSON, hrp, null, HttpStatus.NOT_ACCEPTABLE);

    // Not modified since last read.
    runTestGetConfigSection(SECTION_NAME_CLUSTER,
	MediaType.MULTIPART_FORM_DATA, hrp, null, HttpStatus.NOT_MODIFIED);

    hrp = new HttpRequestPreconditions(null, part.getLastModified(),
	ifNoneMatch, null);
    runTestGetConfigSection(SECTION_NAME_CLUSTER,
	MediaType.MULTIPART_FORM_DATA, hrp, null, HttpStatus.NOT_MODIFIED);

    hrp =
	new HttpRequestPreconditions(null, part.getLastModified(), null, null);
    runTestGetConfigSection(SECTION_NAME_CLUSTER,
	MediaType.MULTIPART_FORM_DATA, hrp, null, HttpStatus.NOT_MODIFIED);

    // Bad Accept header content type.
    runTestGetConfigSection(SECTION_NAME_CLUSTER, null, null, ANYBODY,
	HttpStatus.NOT_ACCEPTABLE);

    // Bad Accept header content type.
    runTestGetConfigSection(SECTION_NAME_CLUSTER,
	MediaType.APPLICATION_JSON, null, ANYBODY, HttpStatus.NOT_ACCEPTABLE);

    // Good Accept header content type.
    configOutput = runTestGetConfigSection(SECTION_NAME_CLUSTER,
	MediaType.MULTIPART_FORM_DATA, null, ANYBODY, HttpStatus.OK);

    part2 = verifyMultipartResponse(configOutput, MediaType.TEXT_XML,
	expectedPayloads);

    // Verify the part last modification timestamps.
    verifyPartModificationTimestamps(part2, part);

    // Bad Accept header content type.
    hrp = new HttpRequestPreconditions(null, null, ifNoneMatch, null);
    runTestGetConfigSection(SECTION_NAME_CLUSTER, null, hrp, ANYBODY,
	HttpStatus.NOT_ACCEPTABLE);

    // Bad Accept header content type.
    runTestGetConfigSection(SECTION_NAME_CLUSTER,
	MediaType.APPLICATION_JSON, hrp, ANYBODY, HttpStatus.NOT_ACCEPTABLE);

    // Not modified since last read.
    runTestGetConfigSection(SECTION_NAME_CLUSTER,
	MediaType.MULTIPART_FORM_DATA, hrp, ANYBODY, HttpStatus.NOT_MODIFIED);

    // Not modified since last read using the REST service client.
    runTestGetConfigSectionClient(SECTION_NAME_CLUSTER, hrp, ANYBODY,
	HttpStatus.NOT_MODIFIED, HttpStatus.NOT_MODIFIED.toString());

    hrp = new HttpRequestPreconditions(null, part.getLastModified(),
	ifNoneMatch, null);
    runTestGetConfigSectionClient(SECTION_NAME_CLUSTER, hrp, null,
	HttpStatus.NOT_MODIFIED, HttpStatus.NOT_MODIFIED.toString());

    hrp =
	new HttpRequestPreconditions(null, part.getLastModified(), null, null);
    runTestGetConfigSectionClient(SECTION_NAME_CLUSTER, hrp, ANYBODY,
	HttpStatus.NOT_MODIFIED, HttpStatus.NOT_MODIFIED.toString());

    // Unconditional read using the REST service client.
    configOutput = runTestGetConfigSectionClient(SECTION_NAME_CLUSTER,
	null, null, HttpStatus.OK, null);

    part2 = verifyMultipartResponse(configOutput, MediaType.TEXT_XML,
	expectedPayloads);

    // Verify the part last modification timestamps.
    verifyPartModificationTimestamps(part2, part);

    // Not modified since last read using the REST service client.
    hrp = new HttpRequestPreconditions(null, null, ifNoneMatch, null);
    runTestGetConfigSectionClient(SECTION_NAME_CLUSTER, hrp, ANYBODY,
	HttpStatus.NOT_MODIFIED, HttpStatus.NOT_MODIFIED.toString());

    hrp = new HttpRequestPreconditions(null, part.getLastModified(),
	ifNoneMatch, null);
    runTestGetConfigSectionClient(SECTION_NAME_CLUSTER, hrp, null,
	HttpStatus.NOT_MODIFIED, HttpStatus.NOT_MODIFIED.toString());

    hrp =
	new HttpRequestPreconditions(null, part.getLastModified(), null, null);
    runTestGetConfigSectionClient(SECTION_NAME_CLUSTER, hrp, ANYBODY,
	HttpStatus.NOT_MODIFIED, HttpStatus.NOT_MODIFIED.toString());

    // Not modified since last read.
    runTestGetConfigSection(SECTION_NAME_CLUSTER,
	MediaType.MULTIPART_FORM_DATA, hrp, null, HttpStatus.NOT_MODIFIED);

    hrp = new HttpRequestPreconditions(null, null, ifNoneMatch, null);
    runTestGetConfigSection(SECTION_NAME_CLUSTER,
	MediaType.MULTIPART_FORM_DATA, hrp, null, HttpStatus.NOT_MODIFIED);

    hrp = new HttpRequestPreconditions(null, part.getLastModified(),
	ifNoneMatch, null);
    runTestGetConfigSection(SECTION_NAME_CLUSTER,
	MediaType.MULTIPART_FORM_DATA, hrp, null, HttpStatus.NOT_MODIFIED);

    // File already exists.
    ifNoneMatch = ListUtil.list(ASTERISK_PRECONDITION);
    hrp = new HttpRequestPreconditions(null, null, ifNoneMatch, null);
    runTestGetConfigSection(SECTION_NAME_CLUSTER,
	MediaType.MULTIPART_FORM_DATA, hrp, null, HttpStatus.NOT_MODIFIED);

    getConfigSectionCommonTest();

    log.debug2("Done");
  }

  /**
   * Runs the getConfigSection()-related authenticated-specific tests.
   * 
   * @throws Exception
   *           if there are problems.
   */
  private void getConfigSectionAuthenticatedTest() throws Exception {
    log.debug2("Invoked");

    // No section: Spring checks the Accept header before credentials.
    runTestGetConfigSection(null, null, null, null, HttpStatus.UNAUTHORIZED);

    // Empty section: Spring checks the Accept header before credentials.
    runTestGetConfigSection(EMPTY_STRING, null, null, null,
	HttpStatus.UNAUTHORIZED);

    HttpRequestPreconditions hrp =
	new HttpRequestPreconditions(null, null, null, null);

    runTestGetConfigSection(EMPTY_STRING, null, hrp, null,
	HttpStatus.UNAUTHORIZED);

    // Missing Accept header: Spring checks the Accept header before
    // credentials.
    runTestGetConfigSection(SECTION_NAME_ALERT, null, null, null,
	HttpStatus.UNAUTHORIZED);

    hrp = new HttpRequestPreconditions(EMPTY_PRECONDITION_LIST, EMPTY_STRING,
	EMPTY_PRECONDITION_LIST, EMPTY_STRING);

    runTestGetConfigSection(SECTION_NAME_ALERT, null, hrp, null,
	HttpStatus.UNAUTHORIZED);

    // Missing credentials.
    runTestGetConfigSection(SECTION_NAME_ALERT,
	MediaType.APPLICATION_JSON, null, null, HttpStatus.UNAUTHORIZED);

    List<String> IfMatchNoMatch = ListUtil.list(EMPTY_PRECONDITION);
    hrp = new HttpRequestPreconditions(IfMatchNoMatch, EMPTY_STRING, null,
	EMPTY_STRING);

    runTestGetConfigSection(SECTION_NAME_ALERT,
	MediaType.APPLICATION_JSON, hrp, null, HttpStatus.UNAUTHORIZED);

    // Missing credentials.
    runTestGetConfigSection(SECTION_NAME_ALERT,
	MediaType.MULTIPART_FORM_DATA, null, null, HttpStatus.UNAUTHORIZED);

    hrp = new HttpRequestPreconditions(null, EMPTY_STRING, IfMatchNoMatch,
	EMPTY_STRING);

    runTestGetConfigSection(SECTION_NAME_ALERT,
	MediaType.MULTIPART_FORM_DATA, hrp, null, HttpStatus.UNAUTHORIZED);

    // Missing credentials.
    List<String> ifNoneMatch = ListUtil.list(ZERO_PRECONDITION);
    hrp = new HttpRequestPreconditions(null, null, ifNoneMatch, null);
    runTestGetConfigSection(SECTION_NAME_ALERT, null, hrp, null,
	HttpStatus.UNAUTHORIZED);

    // Missing credentials.
    runTestGetConfigSection(SECTION_NAME_ALERT,
	MediaType.APPLICATION_JSON, hrp, null, HttpStatus.UNAUTHORIZED);

    // Missing credentials.
    runTestGetConfigSection(SECTION_NAME_ALERT,
	MediaType.MULTIPART_FORM_DATA, hrp, null, HttpStatus.UNAUTHORIZED);

    // Bad credentials.
    runTestGetConfigSection(SECTION_NAME_ALERT, null, hrp, ANYBODY,
	HttpStatus.UNAUTHORIZED);

    // Bad credentials.
    runTestGetConfigSection(SECTION_NAME_ALERT,
	MediaType.APPLICATION_JSON, hrp, ANYBODY, HttpStatus.UNAUTHORIZED);

    // Bad credentials.
    runTestGetConfigSection(SECTION_NAME_ALERT,
	MediaType.MULTIPART_FORM_DATA, hrp, ANYBODY, HttpStatus.UNAUTHORIZED);

    // Bad credentials.
    runTestGetConfigSection(SECTION_NAME_ALERT, null, null, ANYBODY,
	HttpStatus.UNAUTHORIZED);

    // Bad credentials.
    runTestGetConfigSection(SECTION_NAME_ALERT,
	MediaType.APPLICATION_JSON, null, ANYBODY, HttpStatus.UNAUTHORIZED);

    // Bad credentials.
    runTestGetConfigSection(SECTION_NAME_ALERT,
	MediaType.MULTIPART_FORM_DATA, null, ANYBODY, HttpStatus.UNAUTHORIZED);

    getConfigSectionCommonTest();

    log.debug2("Done");
  }

  /**
   * Runs the getConfigSection()-related authentication-independent tests.
   * 
   * @throws Exception
   *           if there are problems.
   */
  private void getConfigSectionCommonTest() throws Exception {
    log.debug2("Invoked");

    // No section: Spring reports it cannot find a match to an endpoint.
    runTestGetConfigSection(null, null, null, USER_ADMIN, HttpStatus.NOT_FOUND);

    HttpRequestPreconditions hrp =
	new HttpRequestPreconditions(null, null, null, null);

    runTestGetConfigSection(null, null, hrp, CONTENT_ADMIN,
	HttpStatus.NOT_FOUND);

    // Bad section name using the REST service client.
    try {
      runTestGetConfigSectionClient(null, null, null, null, null);
      fail("Should have thrown IllegalArgumentException");
    } catch (IllegalArgumentException iae) {
      assertEquals("Invalid section name 'null'", iae.getMessage());
    }

    try {
      hrp = new HttpRequestPreconditions(EMPTY_PRECONDITION_LIST, EMPTY_STRING,
	  EMPTY_PRECONDITION_LIST, EMPTY_STRING);

      runTestGetConfigSectionClient(null, hrp, null, null, null);
      fail("Should have thrown IllegalArgumentException");
    } catch (IllegalArgumentException iae) {
      assertEquals("Invalid section name 'null'", iae.getMessage());
    }

    // Empty section: Spring reports it cannot find a match to an endpoint.
    runTestGetConfigSection(EMPTY_STRING, null, null, USER_ADMIN,
	HttpStatus.NOT_FOUND);

    List<String> IfMatchNoMatch = ListUtil.list(EMPTY_PRECONDITION);
    hrp = new HttpRequestPreconditions(IfMatchNoMatch, EMPTY_STRING, null,
	EMPTY_STRING);

    runTestGetConfigSection(EMPTY_STRING, null, hrp, CONTENT_ADMIN,
	HttpStatus.NOT_FOUND);

    try {
      runTestGetConfigSectionClient(EMPTY_STRING, null, null, null, null);
      fail("Should have thrown IllegalArgumentException");
    } catch (IllegalArgumentException iae) {
      assertEquals("Invalid section name ''", iae.getMessage());
    }

    // Bad Accept header content type.
    runTestGetConfigSection(SECTION_NAME_ALERT, null, null,
	USER_ADMIN, HttpStatus.NOT_ACCEPTABLE);

    hrp = new HttpRequestPreconditions(null, EMPTY_STRING, IfMatchNoMatch,
	EMPTY_STRING);

    runTestGetConfigSection(SECTION_NAME_ALERT, null, hrp,
	CONTENT_ADMIN, HttpStatus.NOT_ACCEPTABLE);

    // Bad Accept header content type.
    runTestGetConfigSection(SECTION_NAME_ALERT,
	MediaType.APPLICATION_JSON, null, USER_ADMIN,
	HttpStatus.NOT_ACCEPTABLE);

    // Bad Accept header content type.
    List<String> ifNoneMatch = ListUtil.list(ZERO_PRECONDITION);
    hrp = new HttpRequestPreconditions(null, null, ifNoneMatch, null);

    runTestGetConfigSection(SECTION_NAME_ALERT,
	MediaType.APPLICATION_JSON, hrp, CONTENT_ADMIN,
	HttpStatus.NOT_ACCEPTABLE);

    // Bad Accept header content type.
    runTestGetConfigSection(SECTION_NAME_ALERT, null, hrp, USER_ADMIN,
	HttpStatus.NOT_ACCEPTABLE);

    // Not found.
    runTestGetConfigSection(SECTION_NAME_ALERT,
	MediaType.MULTIPART_FORM_DATA, null, CONTENT_ADMIN,
	HttpStatus.NOT_FOUND);

    // Not found using the REST service client.
    runTestGetConfigSectionClient(SECTION_NAME_ALERT, null,
	USER_ADMIN, HttpStatus.NOT_FOUND, HttpStatus.NOT_FOUND.toString());

    // Not found.
    runTestGetConfigSection(SECTION_NAME_ALERT,
	MediaType.MULTIPART_FORM_DATA, hrp, USER_ADMIN, HttpStatus.NOT_FOUND);

    // Not found using the REST service client.
    runTestGetConfigSectionClient(SECTION_NAME_ALERT, hrp,
	CONTENT_ADMIN, HttpStatus.NOT_FOUND, HttpStatus.NOT_FOUND.toString());

    // Not found.
    ifNoneMatch = ListUtil.list(ALPHA_PRECONDITION);
    hrp = new HttpRequestPreconditions(null, null, ifNoneMatch, null);
    runTestGetConfigSection(SECTION_NAME_ALERT,
	MediaType.MULTIPART_FORM_DATA, hrp, USER_ADMIN, HttpStatus.NOT_FOUND);

    // Not found using the REST service client.
    runTestGetConfigSectionClient(SECTION_NAME_ALERT, hrp,
	CONTENT_ADMIN, HttpStatus.NOT_FOUND, HttpStatus.NOT_FOUND.toString());

    ifNoneMatch = ListUtil.list(ZERO_PRECONDITION, NUMERIC_PRECONDITION);
    hrp = new HttpRequestPreconditions(null, null, ifNoneMatch, null);
    runTestGetConfigSection(SECTION_NAME_ALERT,
	MediaType.MULTIPART_FORM_DATA, hrp, USER_ADMIN, HttpStatus.NOT_FOUND);

    ifNoneMatch = ListUtil.list(ZERO_PRECONDITION, ALPHA_PRECONDITION);
    hrp = new HttpRequestPreconditions(null, null, ifNoneMatch, null);
    runTestGetConfigSection(SECTION_NAME_ALERT,
	MediaType.MULTIPART_FORM_DATA, hrp, CONTENT_ADMIN,
	HttpStatus.NOT_FOUND);

    ifNoneMatch = ListUtil.list(ZERO_PRECONDITION, ALPHA_PRECONDITION,
	NUMERIC_PRECONDITION);
    hrp = new HttpRequestPreconditions(null, null, ifNoneMatch, null);
    runTestGetConfigSection(SECTION_NAME_ALERT,
	MediaType.MULTIPART_FORM_DATA, hrp, USER_ADMIN, HttpStatus.NOT_FOUND);

    ifNoneMatch = ListUtil.list(NUMERIC_PRECONDITION, ZERO_PRECONDITION,
	ALPHA_PRECONDITION);
    hrp = new HttpRequestPreconditions(null, null, ifNoneMatch, null);
    runTestGetConfigSection(SECTION_NAME_ALERT,
	MediaType.MULTIPART_FORM_DATA, hrp, CONTENT_ADMIN,
	HttpStatus.NOT_FOUND);

    ifNoneMatch = ListUtil.list(ALPHA_PRECONDITION, ZERO_PRECONDITION);
    hrp = new HttpRequestPreconditions(null, null, ifNoneMatch, null);
    runTestGetConfigSection(SECTION_NAME_ALERT,
	MediaType.MULTIPART_FORM_DATA, hrp, USER_ADMIN, HttpStatus.NOT_FOUND);

    ifNoneMatch = ListUtil.list(NUMERIC_PRECONDITION, ALPHA_PRECONDITION,
	ZERO_PRECONDITION);
    hrp = new HttpRequestPreconditions(null, null, ifNoneMatch, null);
    runTestGetConfigSection(SECTION_NAME_ALERT,
	MediaType.MULTIPART_FORM_DATA, hrp, CONTENT_ADMIN,
	HttpStatus.NOT_FOUND);

    ifNoneMatch = ListUtil.list(NUMERIC_PRECONDITION, ALPHA_PRECONDITION);
    hrp = new HttpRequestPreconditions(null, null, ifNoneMatch, null);
    runTestGetConfigSection(SECTION_NAME_ALERT,
	MediaType.MULTIPART_FORM_DATA, hrp, USER_ADMIN, HttpStatus.NOT_FOUND);

    ifNoneMatch = ListUtil.list(ALPHA_PRECONDITION, NUMERIC_PRECONDITION);
    hrp = new HttpRequestPreconditions(null, null, ifNoneMatch, null);
    runTestGetConfigSection(SECTION_NAME_ALERT,
	MediaType.MULTIPART_FORM_DATA, hrp, CONTENT_ADMIN,
	HttpStatus.NOT_FOUND);

    // Bad section name.
    runTestGetConfigSection(BAD_SN, null, null, USER_ADMIN,
	HttpStatus.NOT_ACCEPTABLE);

    // Bad section name using the REST service client.
    runTestGetConfigSectionClient(BAD_SN, null, CONTENT_ADMIN,
	HttpStatus.BAD_REQUEST, HttpStatus.BAD_REQUEST.toString());

    // Bad section name.
    runTestGetConfigSection(BAD_SN, MediaType.MULTIPART_FORM_DATA, null,
	USER_ADMIN, HttpStatus.BAD_REQUEST);

    // Bad section name.
    ifNoneMatch = ListUtil.list(ZERO_PRECONDITION);
    hrp = new HttpRequestPreconditions(null, null, ifNoneMatch, null);
    runTestGetConfigSection(BAD_SN, MediaType.MULTIPART_FORM_DATA, hrp,
	CONTENT_ADMIN, HttpStatus.BAD_REQUEST);

    // Bad section name using the REST service client.
    runTestGetConfigSectionClient(BAD_SN, hrp, CONTENT_ADMIN,
	HttpStatus.BAD_REQUEST, HttpStatus.BAD_REQUEST.toString());

    // Bad section name.
    ifNoneMatch = ListUtil.list(ZERO_PRECONDITION, NUMERIC_PRECONDITION);
    hrp = new HttpRequestPreconditions(null, null, ifNoneMatch, null);
    runTestGetConfigSection(BAD_SN, MediaType.MULTIPART_FORM_DATA, hrp,
	USER_ADMIN, HttpStatus.BAD_REQUEST);

    // Bad section name using the REST service client.
    runTestGetConfigSectionClient(BAD_SN, hrp, CONTENT_ADMIN,
	HttpStatus.BAD_REQUEST, HttpStatus.BAD_REQUEST.toString());

    // Cluster.
    MultipartResponse configOutput = runTestGetConfigSection(
	SECTION_NAME_CLUSTER, MediaType.MULTIPART_FORM_DATA, null,
	USER_ADMIN, HttpStatus.OK);

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

    Part part = verifyMultipartResponse(configOutput, MediaType.TEXT_XML,
	expectedPayloads);

    // Verify the part last modification timestamps.
    verifyPartModificationTimestamps(part, null);

    // Independent verification.
    assertTrue(StringUtil.fromInputStream(ConfigManager.getConfigManager()
	.conditionallyReadCacheConfigFile("dyn:cluster.xml", null)
	.getInputStream()).indexOf(StringUtil
	    .separatedString(expectedPayloads, "\n")) > 0);

    // Successful using the REST service client.
    configOutput = runTestGetConfigSectionClient(SECTION_NAME_CLUSTER,
	null, CONTENT_ADMIN, HttpStatus.OK, null);

    Part part2 = verifyMultipartResponse(configOutput, MediaType.TEXT_XML,
	expectedPayloads);

    // Get the part last modification timestamp.
    verifyPartModificationTimestamps(part2, part);

    // Independent verification.
    assertTrue(StringUtil.fromInputStream(ConfigManager.getConfigManager()
	.conditionallyReadCacheConfigFile("dyn:cluster.xml", null)
	.getInputStream()).indexOf(StringUtil
	    .separatedString(expectedPayloads, "\n")) > 0);

    // Not modified since last read.
    ifNoneMatch = ListUtil.list(part2.getEtag());
    hrp = new HttpRequestPreconditions(null, null, ifNoneMatch, null);
    runTestGetConfigSection(SECTION_NAME_CLUSTER,
	MediaType.MULTIPART_FORM_DATA, hrp, USER_ADMIN,
	HttpStatus.NOT_MODIFIED);

    ifNoneMatch = ListUtil.list(ZERO_PRECONDITION, part2.getEtag());
    hrp = new HttpRequestPreconditions(null, null, ifNoneMatch, null);
    runTestGetConfigSection(SECTION_NAME_CLUSTER,
	MediaType.MULTIPART_FORM_DATA, hrp, CONTENT_ADMIN,
	HttpStatus.NOT_MODIFIED);

    ifNoneMatch = ListUtil.list(ALPHA_PRECONDITION, part2.getEtag());
    hrp = new HttpRequestPreconditions(null, null, ifNoneMatch, null);
    runTestGetConfigSection(SECTION_NAME_CLUSTER,
	MediaType.MULTIPART_FORM_DATA, hrp, CONTENT_ADMIN,
	HttpStatus.NOT_MODIFIED);

    // Not modified since last read, using the REST service client.
    ifNoneMatch = ListUtil.list(part2.getEtag());
    hrp = new HttpRequestPreconditions(null, null, ifNoneMatch, null);
    runTestGetConfigSectionClient(SECTION_NAME_CLUSTER, hrp,
	USER_ADMIN, HttpStatus.NOT_MODIFIED,
	HttpStatus.NOT_MODIFIED.toString());

    ifNoneMatch = ListUtil.list(ZERO_PRECONDITION, part2.getEtag());
    hrp = new HttpRequestPreconditions(null, null, ifNoneMatch, null);
    runTestGetConfigSectionClient(SECTION_NAME_CLUSTER, hrp,
	CONTENT_ADMIN, HttpStatus.NOT_MODIFIED,
	HttpStatus.NOT_MODIFIED.toString());

    ifNoneMatch = ListUtil.list(ALPHA_PRECONDITION, part2.getEtag());
    hrp = new HttpRequestPreconditions(null, null, ifNoneMatch, null);
    runTestGetConfigSectionClient(SECTION_NAME_CLUSTER, hrp,
	USER_ADMIN, HttpStatus.NOT_MODIFIED,
	HttpStatus.NOT_MODIFIED.toString());

    // Modified since creation.
    ifNoneMatch = ListUtil.list(ZERO_PRECONDITION);
    hrp = new HttpRequestPreconditions(null, null, ifNoneMatch, null);
    configOutput = runTestGetConfigSection(SECTION_NAME_CLUSTER,
	MediaType.MULTIPART_FORM_DATA, hrp, CONTENT_ADMIN, HttpStatus.OK);

    part2 = verifyMultipartResponse(configOutput, MediaType.TEXT_XML,
	expectedPayloads);

    // Get the part last modification timestamp.
    verifyPartModificationTimestamps(part2, part);

    // Modified since creation, using the REST service client.
    configOutput = runTestGetConfigSectionClient(SECTION_NAME_CLUSTER,
	hrp, USER_ADMIN, HttpStatus.OK, null);

    part2 = verifyMultipartResponse(configOutput, MediaType.TEXT_XML,
	expectedPayloads);

    // Get the part last modification timestamp.
    verifyPartModificationTimestamps(part2, part);

    // Successful.
    ifNoneMatch = ListUtil.list(ZERO_PRECONDITION, ALPHA_PRECONDITION);
    hrp = new HttpRequestPreconditions(null, null, ifNoneMatch, null);
    configOutput = runTestGetConfigSection(SECTION_NAME_CLUSTER,
	MediaType.MULTIPART_FORM_DATA, hrp, CONTENT_ADMIN, HttpStatus.OK);

    part2 = verifyMultipartResponse(configOutput, MediaType.TEXT_XML,
	expectedPayloads);

    // Get the part last modification timestamp.
    verifyPartModificationTimestamps(part2, part);

    // Successful using the REST service client.
    configOutput = runTestGetConfigSectionClient(SECTION_NAME_CLUSTER,
	hrp, USER_ADMIN, HttpStatus.OK, null);

    part2 = verifyMultipartResponse(configOutput, MediaType.TEXT_XML,
	expectedPayloads);

    // Get the part last modification timestamp.
    verifyPartModificationTimestamps(part2, part);

    // No If-None-Match header.
    configOutput = runTestGetConfigSection(SECTION_NAME_CLUSTER,
	MediaType.MULTIPART_FORM_DATA, null, CONTENT_ADMIN, HttpStatus.OK);

    part2 = verifyMultipartResponse(configOutput, MediaType.TEXT_XML,
	expectedPayloads);

    // Get the part last modification timestamp.
    verifyPartModificationTimestamps(part2, part);

    // No If-None-Match header using the REST service client.
    configOutput = runTestGetConfigSectionClient(SECTION_NAME_CLUSTER,
	null, USER_ADMIN, HttpStatus.OK, null);

    part2 = verifyMultipartResponse(configOutput, MediaType.TEXT_XML,
	expectedPayloads);

    // Get the part last modification timestamp.
    verifyPartModificationTimestamps(part2, part);

    // Not modified since last read.
    ifNoneMatch = ListUtil.list(part2.getEtag());
    hrp = new HttpRequestPreconditions(null, null, ifNoneMatch, null);
    runTestGetConfigSection(SECTION_NAME_CLUSTER,
	MediaType.MULTIPART_FORM_DATA, hrp, CONTENT_ADMIN,
	HttpStatus.NOT_MODIFIED);

    // Not modified since last read, using the REST service client.
    runTestGetConfigSectionClient(SECTION_NAME_CLUSTER, hrp,
	USER_ADMIN, HttpStatus.NOT_MODIFIED,
	HttpStatus.NOT_MODIFIED.toString());

    // Not modified since last read.
    ifNoneMatch = ListUtil.list(part.getEtag(), NUMERIC_PRECONDITION);
    hrp = new HttpRequestPreconditions(null, null, ifNoneMatch, null);
    runTestGetConfigSection(SECTION_NAME_CLUSTER,
	MediaType.MULTIPART_FORM_DATA, hrp, CONTENT_ADMIN,
	HttpStatus.NOT_MODIFIED);

    // Not modified since last read, using the REST service client.
    runTestGetConfigSectionClient(SECTION_NAME_CLUSTER, hrp,
	USER_ADMIN, HttpStatus.NOT_MODIFIED,
	HttpStatus.NOT_MODIFIED.toString());

    // Not modified since last read.
    ifNoneMatch = ListUtil.list(part.getEtag(), ALPHA_PRECONDITION);
    hrp = new HttpRequestPreconditions(null, null, ifNoneMatch, null);
    runTestGetConfigSection(SECTION_NAME_CLUSTER,
	MediaType.MULTIPART_FORM_DATA, hrp, CONTENT_ADMIN,
	HttpStatus.NOT_MODIFIED);

    // Not modified since last read, using the REST service client.
    runTestGetConfigSectionClient(SECTION_NAME_CLUSTER, hrp,
	CONTENT_ADMIN, HttpStatus.NOT_MODIFIED,
	HttpStatus.NOT_MODIFIED.toString());

    // No match.
    ifNoneMatch = ListUtil.list(NUMERIC_PRECONDITION);
    hrp = new HttpRequestPreconditions(null, null, ifNoneMatch, null);
    configOutput = runTestGetConfigSection(SECTION_NAME_CLUSTER,
	MediaType.MULTIPART_FORM_DATA, hrp, USER_ADMIN, HttpStatus.OK);

    part2 = verifyMultipartResponse(configOutput, MediaType.TEXT_XML,
	expectedPayloads);

    // Get the part last modification timestamp.
    verifyPartModificationTimestamps(part2, part);

    // No match using the REST service client.
    configOutput = runTestGetConfigSectionClient(SECTION_NAME_CLUSTER,
	hrp, CONTENT_ADMIN, HttpStatus.OK, null);

    part2 = verifyMultipartResponse(configOutput, MediaType.TEXT_XML,
	expectedPayloads);

    // Get the part last modification timestamp.
    verifyPartModificationTimestamps(part2, part);

    // No match.
    ifNoneMatch = ListUtil.list(NUMERIC_PRECONDITION, ALPHA_PRECONDITION);
    hrp = new HttpRequestPreconditions(null, null, ifNoneMatch, null);
    configOutput = runTestGetConfigSection(SECTION_NAME_CLUSTER,
	MediaType.MULTIPART_FORM_DATA, hrp, CONTENT_ADMIN, HttpStatus.OK);

    part2 = verifyMultipartResponse(configOutput, MediaType.TEXT_XML,
	expectedPayloads);

    // Get the part last modification timestamp.
    verifyPartModificationTimestamps(part2, part);

    // No match using the REST service client.
    configOutput = runTestGetConfigSectionClient(SECTION_NAME_CLUSTER,
	hrp, USER_ADMIN, HttpStatus.OK, null);

    part2 = verifyMultipartResponse(configOutput, MediaType.TEXT_XML,
	expectedPayloads);

    // Get the part last modification timestamp.
    verifyPartModificationTimestamps(part2, part);

    // File already exists.
    ifNoneMatch = ListUtil.list(ASTERISK_PRECONDITION);
    hrp = new HttpRequestPreconditions(null, null, ifNoneMatch, null);
    runTestGetConfigSection(SECTION_NAME_CLUSTER,
	MediaType.MULTIPART_FORM_DATA, hrp, USER_ADMIN,
	HttpStatus.NOT_MODIFIED);

    // File already exists, using the REST service client.
    runTestGetConfigSectionClient(SECTION_NAME_CLUSTER, hrp,
	CONTENT_ADMIN, HttpStatus.NOT_MODIFIED,
	HttpStatus.NOT_MODIFIED.toString());

    // Match of the If-Match precondition.
    List<String> ifMatch = ListUtil.list(part.getEtag());
    hrp = new HttpRequestPreconditions(ifMatch, null, null, null);
    configOutput = runTestGetConfigSection(SECTION_NAME_CLUSTER,
	MediaType.MULTIPART_FORM_DATA, hrp, CONTENT_ADMIN, HttpStatus.OK);

    part2 = verifyMultipartResponse(configOutput, MediaType.TEXT_XML,
	expectedPayloads);

    // Get the part last modification timestamp.
    verifyPartModificationTimestamps(part2, part);

    // Match of the If-Match precondition, using the REST service client.
    configOutput = runTestGetConfigSectionClient(SECTION_NAME_CLUSTER,
	hrp, USER_ADMIN, HttpStatus.OK, null);

    part2 = verifyMultipartResponse(configOutput, MediaType.TEXT_XML,
	expectedPayloads);

    // Get the part last modification timestamp.
    verifyPartModificationTimestamps(part2, part);

    // Mismatch of the If-Match precondition.
    ifMatch = ListUtil.list(NUMERIC_PRECONDITION);
    hrp = new HttpRequestPreconditions(ifMatch, null, null, null);
    runTestGetConfigSection(SECTION_NAME_CLUSTER,
	MediaType.MULTIPART_FORM_DATA, hrp, USER_ADMIN,
	HttpStatus.PRECONDITION_FAILED);

    // Mismatch of the If-Match precondition, using the REST service client.
    runTestGetConfigSectionClient(SECTION_NAME_CLUSTER, hrp,
	CONTENT_ADMIN, HttpStatus.PRECONDITION_FAILED,
	HttpStatus.PRECONDITION_FAILED.toString());

    log.debug2("Done");
  }

  /**
   * Performs a GET operation for a configuration section.
   * 
   * @param snId
   *          A String with the configuration section name.
   * @param acceptContentType
   *          A MediaType with the content type to be added to the request
   *          "Accept" header.
   * @param preconditions
   *          An HttpRequestPreconditions with the request preconditions to be
   *          met.
   * @param credentials
   *          A Credentials with the request credentials.
   * @param expectedStatus
   *          An HttpStatus with the HTTP status of the result.
   * @return a MultipartResponse with the multipart response.
   * @throws Exception
   *           if there are problems.
   */
  private MultipartResponse runTestGetConfigSection(String snId,
      MediaType acceptContentType, HttpRequestPreconditions preconditions,
      Credentials credentials, HttpStatus expectedStatus) throws Exception {
    log.debug2("snId = {}", snId);
    log.debug2("acceptContentType = {}", acceptContentType);
    log.debug2("preconditions = {}", preconditions);
    log.debug2("credentials = {}", credentials);
    log.debug2("expectedStatus = {}", expectedStatus);

    // Get the test URL template.
    String template = getTestUrlTemplate("/config/file/{snid}");

    // Create the URI of the request to the REST service.
    UriComponents uriComponents = UriComponentsBuilder.fromUriString(template)
	.build().expand(Collections.singletonMap("snid", snId));

    URI uri = UriComponentsBuilder.newInstance().uriComponents(uriComponents)
	.build().encode().toUri();
    log.trace("uri = {}", uri);

    // Initialize the request to the REST service.
    RestTemplateBuilder templateBuilder = RestUtil.getRestTemplateBuilder(0, 0);

    // Add our MultipartMessageHttpMessageConverter
//    templateBuilder.additionalMessageConverters(new MultipartMessageHttpMessageConverter());
    List<HttpMessageConverter<?>> messageConverters = new ArrayList<>();
    messageConverters.add(new MultipartMessageHttpMessageConverter());
    messageConverters.addAll(new RestTemplate().getMessageConverters());
    templateBuilder = templateBuilder.messageConverters(messageConverters);

    HttpEntity<String> requestEntity = null;

    // Get the individual preconditions.
    List<String> ifMatch = null;
    String ifModifiedSince = null;
    List<String> ifNoneMatch = null;
    String ifUnmodifiedSince = null;

    if (preconditions != null) {
      ifMatch = preconditions.getIfMatch();
      ifModifiedSince = preconditions.getIfModifiedSince();
      ifNoneMatch = preconditions.getIfNoneMatch();
      ifUnmodifiedSince = preconditions.getIfUnmodifiedSince();
    }

    // Get the individual credentials elements.
    String user = null;
    String password = null;

    if (credentials != null) {
      user = credentials.getUser();
      password = credentials.getPassword();
    }

    // Check whether there are any custom headers to be specified in the
    // request.
    if (acceptContentType != null || (ifMatch != null && !ifMatch.isEmpty())
	|| (ifModifiedSince != null && !ifModifiedSince.isEmpty())
	|| (ifNoneMatch != null && !ifNoneMatch.isEmpty())
	|| (ifUnmodifiedSince != null && !ifUnmodifiedSince.isEmpty())
	|| user != null || password != null) {
      // Yes: Initialize the request headers.
      HttpHeaders headers = new HttpHeaders();

      // Check whether there is a custom "Accept" header.
      if (acceptContentType != null) {
        // Yes: Set it.
        headers.setAccept(Arrays.asList(acceptContentType, MediaType.APPLICATION_JSON));
      } else {
        // No: Set it to accept errors at least.
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
      }

      // Check whether there is a custom If-Match header.
      if (ifMatch != null) {
	// Yes: Set the If-Match header.
	headers.setIfMatch(ifMatch);
      }

      // Check whether there is a custom If-Modified-Since header.
      if (ifModifiedSince != null) {
	// Yes: Set the If-Modified-Since header.
	headers.set(HttpHeaders.IF_MODIFIED_SINCE, ifModifiedSince);
      }

      // Check whether there is a custom If-None-Match header.
      if (ifNoneMatch != null) {
	// Yes: Set the If-None-Match header.
	headers.setIfNoneMatch(ifNoneMatch);
      }

      // Check whether there is a custom If-Unmodified-Since header.
      if (ifUnmodifiedSince != null) {
	// Yes: Set the If-Unmodified-Since header.
	headers.set(HttpHeaders.IF_UNMODIFIED_SINCE, ifUnmodifiedSince);
      }

      // Set up the authentication credentials, if necessary.
      if (credentials != null) {
	credentials.setUpBasicAuthentication(headers);
      }

      log.trace("requestHeaders = {}", () -> headers.toSingleValueMap());

      // Create the request entity.
      requestEntity = new HttpEntity<String>(null, headers);
    }

    // Make the request and get the response.
    TestRestTemplate testTemplate = new TestRestTemplate(templateBuilder);

    try {
      ResponseEntity<MultipartMessage> response = testTemplate
          .exchange(uri, HttpMethod.GET, requestEntity, MultipartMessage.class);

      // Get the response status.
      HttpStatusCode statusCode = response.getStatusCode();
      HttpStatus status = HttpStatus.valueOf(statusCode.value());
      assertEquals(expectedStatus, status);

      MultipartResponse parsedResponse = null;

      // Check whether it is a success response.
      if (isSuccess(status)) {
        // Yes: Parse it.
        parsedResponse = new MultipartResponse(response);
      }

      // Return the parsed response.
      if (log.isDebug2Enabled())
        log.debug2("parsedResponse = {}", parsedResponse);

      return parsedResponse;

    } catch (LockssResponseErrorHandler.WrappedLockssRestHttpException e) {
      LockssRestHttpException lhre = e.getLRHE();
      HttpStatus statusCode = lhre.getHttpStatus();
      assertFalse(RestUtil.isSuccess(statusCode));
      assertEquals(expectedStatus, statusCode);
    }

    return null;
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
   * Sanity check on the response part modification timestamps.
   * 
   * @param part
   *          A Part with the response part to be checked.
   * @param matchingPart
   *          A Part with another optional response part that is expected to
   *          have matching timestamps.
   */
  private void verifyPartModificationTimestamps(Part part, Part matchingPart) {
    log.debug2("part = {}", part);
    log.debug2("matchingPart = {}", matchingPart);

    // Verify the part last modified header.
    long lastModified = Long.parseLong(part.getLastModified());
    assertTrue(lastModified <= TimeBase.nowMs());

    // Verify the part ETag.
    String etag = part.getEtag();
    long etagContents = Long.parseLong(parseEtag(etag));
    assertTrue(etagContents >= lastModified);
    assertTrue(etagContents < lastModified + 1000L);

    if (matchingPart != null) {
      assertEquals(Long.parseLong(matchingPart.getLastModified()),
	  lastModified);
      assertEquals(matchingPart.getEtag(), etag);
    }

    log.debug2("Done");
  }

  /**
   * Performs a GET operation for a configuration section using the REST service
   * client.
   * 
   * @param snId
   *          A String with the configuration section name.
   * @param preconditions
   *          An HttpRequestPreconditions with the request preconditions to be
   *          met.
   * @param credentials
   *          A Credentials with the request credentials.
   * @param expectedStatus
   *          An HttpStatus with the HTTP status of the result.
   * @param expectedErrorMessagePrefix
   *          A String with the beginning of the error message.
   * @return a MultipartResponse with the multipart response.
   */
  private MultipartResponse runTestGetConfigSectionClient(String snId,
      HttpRequestPreconditions preconditions, Credentials credentials,
      HttpStatus expectedStatus, String expectedErrorMessagePrefix) {
    log.debug2("snId = {}", snId);
    log.debug2("preconditions = {}", preconditions);
    log.debug2("credentials = {}", credentials);
    log.debug2("expectedStatus = {}", expectedStatus);
    log.debug2("expectedErrorMessagePrefix = {}", expectedErrorMessagePrefix);

    RestConfigSection input = new RestConfigSection();
    input.setSectionName(snId);
    input.setHttpRequestPreconditions(preconditions);

    // Make the request and get the result.
    RestConfigSection output =
	getRestConfigClient(credentials).getConfigSection(input);

    // Check the response status.
    assertEquals(expectedStatus, output.getStatus());

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
    log.debug2("parsedResponse = {}", parsedResponse);
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
   * @return a Part with the response payload.
   * @throws Exception
   *           if there are problems.
   */
  private Part verifyMultipartResponse(MultipartResponse response,
      MediaType expectedContentType, List<String> expectedPayloads)
	  throws Exception {
    log.debug2("response = {}", response);
    log.debug2("expectedContentType = {}", expectedContentType);
    log.debug2("expectedPayloads = {}", expectedPayloads);

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
    HttpHeaders partHeaders = part.getHeaders();
    assertTrue(partHeaders.containsKey(HttpHeaders.CONTENT_TYPE));
    assertEquals(expectedContentType.toString(),
	HeaderUtil.getMimeTypeFromContentType(partHeaders.getFirst(
	    HttpHeaders.CONTENT_TYPE)));

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

    // Validate the part last modification timestamp headers.
    assertTrue(partHeaders.containsKey(HttpHeaders.LAST_MODIFIED));
    assertTrue(partHeaders.containsKey(HttpHeaders.ETAG));

    log.debug2("part = {}", part);
    return part;
  }

  /**
   * Parses an incoming ETag.
   * 
   * @param eTag
   *          A String with the incoming ETag.
   * @return a String with the parsed ETag.
   */
  private String parseEtag(String eTag) {
    log.debug2("eTag = {}", eTag);

    String parsedEtag = eTag;

    // Check whether the raw eTag has content and it is surrounded by double
    // quotes.
    if (eTag != null && eTag.startsWith("\"") && eTag.endsWith("\"")) {
      // Yes: Remove the surrounding double quotes left by Spring.
      parsedEtag = eTag.substring(1, eTag.length()-1);
    }

    if (log.isDebug2Enabled()) log.debug2("parsedEtag = {}", parsedEtag);
    return parsedEtag;
  }

  /**
   * Runs the getConfigUrl()-related un-authenticated-specific tests.
   * 
   * @throws Exception
   *           if there are problems.
   */
  private void getConfigUrlUnAuthenticatedTest() throws Exception {
    log.debug2("Invoked");

    // No URL: Spring reports it cannot find a match to an endpoint.
    runTestGetConfigUrl(null, null, null, null, HttpStatus.NOT_FOUND);

    HttpRequestPreconditions hrp =
	new HttpRequestPreconditions(null, null, null, null);

    runTestGetConfigUrl(null, null, hrp, null, HttpStatus.NOT_FOUND);

    // Empty URL: Spring reports it cannot find a match to an endpoint.
    runTestGetConfigUrl(EMPTY_STRING, null, null, null, HttpStatus.NOT_FOUND);

    hrp = new HttpRequestPreconditions(EMPTY_PRECONDITION_LIST, EMPTY_STRING,
	EMPTY_PRECONDITION_LIST, EMPTY_STRING);

    runTestGetConfigUrl(EMPTY_STRING, null, hrp, null, HttpStatus.NOT_FOUND);

    String url = "http://something";

    // Nothing there.
    runTestGetConfigUrl(url, null, null, null, HttpStatus.NOT_FOUND);

    // Bad Accept header content type.
    List<String> IfMatchNoMatch = ListUtil.list(EMPTY_PRECONDITION);
    hrp = new HttpRequestPreconditions(IfMatchNoMatch, EMPTY_STRING, null,
	EMPTY_STRING);

    runTestGetConfigUrl(url, null, hrp, null, HttpStatus.NOT_ACCEPTABLE);

    // Bad Accept header content type.
    hrp = new HttpRequestPreconditions(null, EMPTY_STRING, IfMatchNoMatch,
	EMPTY_STRING);

    runTestGetConfigUrl(url, null, hrp, null, HttpStatus.NOT_ACCEPTABLE);

    // Bad Accept header content type.
    List<String> ifNoneMatch = ListUtil.list(ZERO_PRECONDITION);
    hrp = new HttpRequestPreconditions(null, null, ifNoneMatch, null);
    runTestGetConfigUrl(url, null, hrp, null, HttpStatus.NOT_ACCEPTABLE);

    // Bad Accept header content type.
    runTestGetConfigUrl(url, MediaType.APPLICATION_JSON, null, null,
	HttpStatus.NOT_ACCEPTABLE);

    // Bad Accept header content type.
    runTestGetConfigUrl(url, MediaType.APPLICATION_JSON, hrp, null,
	HttpStatus.NOT_ACCEPTABLE);

    // Nothing there.
    runTestGetConfigUrl(url, MediaType.MULTIPART_FORM_DATA, null, null,
	HttpStatus.NOT_FOUND);

    // Bad Accept header content type.
    runTestGetConfigUrl(url, null, null, ANYBODY, HttpStatus.NOT_ACCEPTABLE);

    // Bad Accept header content type.
    runTestGetConfigUrl(url, null, hrp, ANYBODY, HttpStatus.NOT_ACCEPTABLE);

    // Bad Accept header content type.
    runTestGetConfigUrl(url, MediaType.APPLICATION_JSON, null, ANYBODY,
	HttpStatus.NOT_ACCEPTABLE);

    // Bad Accept header content type.
    runTestGetConfigUrl(url, MediaType.APPLICATION_JSON, hrp, ANYBODY,
	HttpStatus.NOT_ACCEPTABLE);

    // Nothing there.
    runTestGetConfigUrl(url, MediaType.MULTIPART_FORM_DATA, null, ANYBODY,
	HttpStatus.NOT_FOUND);

    url = "http://localhost:12345";

    // Nothing there.
    runTestGetConfigUrl(url, null, null, null, HttpStatus.NOT_FOUND);

    // Bad Accept header content type.
    runTestGetConfigUrl(url, null, hrp, null, HttpStatus.NOT_ACCEPTABLE);

    // Bad Accept header content type.
    runTestGetConfigUrl(url, MediaType.APPLICATION_JSON, null, null,
	HttpStatus.NOT_ACCEPTABLE);

    // Bad Accept header content type.
    runTestGetConfigUrl(url, MediaType.APPLICATION_JSON, hrp, null,
	HttpStatus.NOT_ACCEPTABLE);

    // Nothing there.
    runTestGetConfigUrl(url, MediaType.MULTIPART_FORM_DATA, null, null,
	HttpStatus.NOT_FOUND);

    // Bad Accept header content type.
    runTestGetConfigUrl(url, null, null, ANYBODY, HttpStatus.NOT_ACCEPTABLE);

    // Bad Accept header content type.
    runTestGetConfigUrl(url, null, hrp, ANYBODY, HttpStatus.NOT_ACCEPTABLE);

    // Bad Accept header content type.
    runTestGetConfigUrl(url, MediaType.APPLICATION_JSON, null, ANYBODY,
	HttpStatus.NOT_ACCEPTABLE);

    // Bad Accept header content type.
    runTestGetConfigUrl(url, MediaType.APPLICATION_JSON, hrp, ANYBODY,
	HttpStatus.NOT_ACCEPTABLE);

    // Nothing there.
    runTestGetConfigUrl(url, MediaType.MULTIPART_FORM_DATA, null, ANYBODY,
	HttpStatus.NOT_FOUND);

    url = "http://example.com";

    // Success.
    MultipartResponse configOutput =
	runTestGetConfigUrl(url, null, null, null, HttpStatus.OK);

    List<String> expectedPayloads =
	ListUtil.list("<title>Example Domain</title>");

    Part part = verifyMultipartResponse(configOutput, MediaType.TEXT_HTML,
	expectedPayloads);

    // Get the part last modification timestamp.
    String lastModifiedHeaderValue = part.getLastModified();

    // Bad Accept header content type.
    runTestGetConfigUrl(url, null, hrp, null, HttpStatus.NOT_ACCEPTABLE);

    // Bad Accept header content type.
    runTestGetConfigUrl(url, MediaType.APPLICATION_JSON, null, null,
	HttpStatus.NOT_ACCEPTABLE);

    // Bad Accept header content type.
    runTestGetConfigUrl(url, MediaType.APPLICATION_JSON, hrp, null,
	HttpStatus.NOT_ACCEPTABLE);

    // Success.
    configOutput = runTestGetConfigUrl(url, MediaType.MULTIPART_FORM_DATA, null,
	null, HttpStatus.OK);

    part = verifyMultipartResponse(configOutput, MediaType.TEXT_HTML,
	expectedPayloads);

    // Verify the part last modification timestamp.
    assertEquals(lastModifiedHeaderValue, part.getLastModified());

    // Bad Accept header content type.
    runTestGetConfigUrl(url, null, null, ANYBODY, HttpStatus.NOT_ACCEPTABLE);

    // Bad Accept header content type.
    runTestGetConfigUrl(url, null, hrp, ANYBODY, HttpStatus.NOT_ACCEPTABLE);

    // Bad Accept header content type.
    runTestGetConfigUrl(url, MediaType.APPLICATION_JSON, null, ANYBODY,
	HttpStatus.NOT_ACCEPTABLE);

    // Bad Accept header content type.
    runTestGetConfigUrl(url, MediaType.APPLICATION_JSON, hrp, ANYBODY,
	HttpStatus.NOT_ACCEPTABLE);

    // Not modified since last read.
    hrp =
	new HttpRequestPreconditions(null, lastModifiedHeaderValue, null, null);

    configOutput = runTestGetConfigUrl(url, MediaType.MULTIPART_FORM_DATA, hrp,
	null, HttpStatus.NOT_MODIFIED);

    url = "dyn:cluster.xml";

    configOutput = runTestGetConfigUrl(url, MediaType.MULTIPART_FORM_DATA, null,
	null, HttpStatus.OK);

    expectedPayloads = ListUtil.list(
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

    part = verifyMultipartResponse(configOutput, MediaType.TEXT_XML,
	expectedPayloads);

    // Verify the part last modification timestamps.
    verifyPartModificationTimestamps(part, null);

    // Not modified since last read.
    hrp =
	new HttpRequestPreconditions(null, part.getLastModified(), null, null);

    runTestGetConfigUrl(url, MediaType.MULTIPART_FORM_DATA, hrp, null,
	HttpStatus.NOT_MODIFIED);

    // Not modified since last read.
    ifNoneMatch = ListUtil.list(part.getEtag());
    hrp = new HttpRequestPreconditions(null, null, ifNoneMatch, null);

    runTestGetConfigUrl(url, MediaType.MULTIPART_FORM_DATA, hrp, null,
	HttpStatus.NOT_MODIFIED);

    // Not modified since last read.
    hrp = new HttpRequestPreconditions(null, part.getLastModified(),
	ifNoneMatch, null);

    runTestGetConfigUrl(url, MediaType.MULTIPART_FORM_DATA, hrp, null,
	HttpStatus.NOT_MODIFIED);

    // Success.
    ifNoneMatch = ListUtil.list(ZERO_PRECONDITION);
    hrp = new HttpRequestPreconditions(null, null, ifNoneMatch, null);

    configOutput = runTestGetConfigUrl(url, MediaType.MULTIPART_FORM_DATA, hrp,
	null, HttpStatus.OK);

    Part part2 = verifyMultipartResponse(configOutput, MediaType.TEXT_XML,
	expectedPayloads);

    // Verify the part last modification timestamps.
    verifyPartModificationTimestamps(part2, part);

    // Success.
    ifNoneMatch = ListUtil.list(NUMERIC_PRECONDITION);
    hrp = new HttpRequestPreconditions(null, null, ifNoneMatch, null);

    configOutput = runTestGetConfigUrl(url, MediaType.MULTIPART_FORM_DATA, hrp,
	null, HttpStatus.OK);

    part2 = verifyMultipartResponse(configOutput, MediaType.TEXT_XML,
	expectedPayloads);

    // Verify the part last modification timestamps.
    verifyPartModificationTimestamps(part2, part);

    // Not modified since last read.
    ifNoneMatch = ListUtil.list(ASTERISK_PRECONDITION);
    hrp = new HttpRequestPreconditions(null, null, ifNoneMatch, null);

    runTestGetConfigUrl(url, MediaType.MULTIPART_FORM_DATA, hrp, null,
	HttpStatus.NOT_MODIFIED);

    // Success.
    configOutput = runTestGetConfigUrl(url, MediaType.MULTIPART_FORM_DATA, null,
	ANYBODY, HttpStatus.OK);

    part2 = verifyMultipartResponse(configOutput, MediaType.TEXT_XML,
	expectedPayloads);

    // Verify the part last modification timestamps.
    verifyPartModificationTimestamps(part2, part);

    // Not modified since last read.
    hrp =
	new HttpRequestPreconditions(null, part.getLastModified(), null, null);

    runTestGetConfigUrl(url, MediaType.MULTIPART_FORM_DATA, hrp, ANYBODY,
	HttpStatus.NOT_MODIFIED);

    // Not modified since last read.
    ifNoneMatch = ListUtil.list(part2.getEtag());
    hrp = new HttpRequestPreconditions(null, null, ifNoneMatch, null);

    runTestGetConfigUrl(url, MediaType.MULTIPART_FORM_DATA, hrp, ANYBODY,
	HttpStatus.NOT_MODIFIED);

    // Not modified since last read.
    hrp = new HttpRequestPreconditions(null, part.getLastModified(),
	ifNoneMatch, null);

    runTestGetConfigUrl(url, MediaType.MULTIPART_FORM_DATA, hrp, ANYBODY,
	HttpStatus.NOT_MODIFIED);

    // Success.
    ifNoneMatch = ListUtil.list(ZERO_PRECONDITION);
    hrp = new HttpRequestPreconditions(null, null, ifNoneMatch, null);

    configOutput = runTestGetConfigUrl(url, MediaType.MULTIPART_FORM_DATA, hrp,
	ANYBODY, HttpStatus.OK);

    part2 = verifyMultipartResponse(configOutput, MediaType.TEXT_XML,
	expectedPayloads);

    // Verify the part last modification timestamps.
    verifyPartModificationTimestamps(part2, part);

    // Success.
    ifNoneMatch = ListUtil.list(NUMERIC_PRECONDITION);
    hrp = new HttpRequestPreconditions(null, null, ifNoneMatch, null);

    configOutput = runTestGetConfigUrl(url, MediaType.MULTIPART_FORM_DATA, hrp,
	ANYBODY, HttpStatus.OK);

    part2 = verifyMultipartResponse(configOutput, MediaType.TEXT_XML,
	expectedPayloads);

    // Verify the part last modification timestamps.
    verifyPartModificationTimestamps(part2, part);

    // Not modified since last read.
    ifNoneMatch = ListUtil.list(ASTERISK_PRECONDITION);
    hrp = new HttpRequestPreconditions(null, null, ifNoneMatch, null);

    runTestGetConfigUrl(url, MediaType.MULTIPART_FORM_DATA, hrp, ANYBODY,
	HttpStatus.NOT_MODIFIED);

    // Match of the If-Match precondition.
    List<String> ifMatch = ListUtil.list(part2.getEtag());
    hrp = new HttpRequestPreconditions(ifMatch, null, null, null);

    configOutput = runTestGetConfigUrl(url, MediaType.MULTIPART_FORM_DATA, hrp,
	null, HttpStatus.OK);

    part2 = verifyMultipartResponse(configOutput, MediaType.TEXT_XML,
	expectedPayloads);

    // Verify the part last modification timestamps.
    verifyPartModificationTimestamps(part2, part);

    configOutput = runTestGetConfigUrl(url, MediaType.MULTIPART_FORM_DATA, hrp,
	ANYBODY, HttpStatus.OK);

    part2 = verifyMultipartResponse(configOutput, MediaType.TEXT_XML,
	expectedPayloads);

    // Verify the part last modification timestamps.
    verifyPartModificationTimestamps(part2, part);

    // Mismatch of the If-Match precondition.
    ifMatch = ListUtil.list(NUMERIC_PRECONDITION);
    hrp = new HttpRequestPreconditions(ifMatch, null, null, null);

    runTestGetConfigUrl(url, MediaType.MULTIPART_FORM_DATA, hrp, null,
	HttpStatus.PRECONDITION_FAILED);

    runTestGetConfigUrl(url, MediaType.MULTIPART_FORM_DATA, hrp, ANYBODY,
	HttpStatus.PRECONDITION_FAILED);

    getConfigUrlCommonTest();

    log.debug2("Done");
  }

  /**
   * Runs the getConfigUrl()-related authenticated-specific tests.
   * 
   * @throws Exception
   *           if there are problems.
   */
  private void getConfigUrlAuthenticatedTest() throws Exception {
    log.debug2("Invoked");

    // No URL: Spring checks the Accept header before credentials.
    runTestGetConfigUrl(null, null, null, null, HttpStatus.UNAUTHORIZED);

    // Empty URL: Spring checks the Accept header before credentials.
    runTestGetConfigUrl(EMPTY_STRING, null, null, null,
	HttpStatus.UNAUTHORIZED);

    HttpRequestPreconditions hrp =
	new HttpRequestPreconditions(null, null, null, null);

    runTestGetConfigUrl(EMPTY_STRING, null, hrp, null,
	HttpStatus.UNAUTHORIZED);

    String url = "http://something";

    // Missing Accept header: Spring checks the Accept header
    // before credentials.
    runTestGetConfigUrl(url, null, null, null, HttpStatus.UNAUTHORIZED);

    hrp = new HttpRequestPreconditions(EMPTY_PRECONDITION_LIST, EMPTY_STRING,
	EMPTY_PRECONDITION_LIST, EMPTY_STRING);

    runTestGetConfigUrl(url, null, hrp, null, HttpStatus.UNAUTHORIZED);

    // Missing credentials.
    runTestGetConfigUrl(url, MediaType.APPLICATION_JSON, null, null,
	HttpStatus.UNAUTHORIZED);

    List<String> IfMatchNoMatch = ListUtil.list(EMPTY_PRECONDITION);
    hrp = new HttpRequestPreconditions(IfMatchNoMatch, EMPTY_STRING, null,
	EMPTY_STRING);

    runTestGetConfigUrl(url, MediaType.APPLICATION_JSON, hrp, null,
	HttpStatus.UNAUTHORIZED);

    // Missing credentials.
    runTestGetConfigUrl(url, MediaType.MULTIPART_FORM_DATA, null, null,
	HttpStatus.UNAUTHORIZED);

    hrp = new HttpRequestPreconditions(null, EMPTY_STRING, IfMatchNoMatch,
	EMPTY_STRING);

    runTestGetConfigUrl(url, MediaType.MULTIPART_FORM_DATA, hrp, null,
	HttpStatus.UNAUTHORIZED);

    // Missing credentials.
    List<String> ifNoneMatch = ListUtil.list(ZERO_PRECONDITION);
    hrp = new HttpRequestPreconditions(null, null, ifNoneMatch, null);
    runTestGetConfigUrl(url, null, hrp, null, HttpStatus.UNAUTHORIZED);

    // Missing credentials.
    runTestGetConfigUrl(url, MediaType.APPLICATION_JSON, hrp, null,
	HttpStatus.UNAUTHORIZED);

    // Missing credentials.
    runTestGetConfigUrl(url, MediaType.MULTIPART_FORM_DATA, hrp, null,
	HttpStatus.UNAUTHORIZED);

    // Bad credentials.
    runTestGetConfigUrl(url, null, null, ANYBODY, HttpStatus.UNAUTHORIZED);

    // Bad credentials.
    runTestGetConfigUrl(url, null, hrp, ANYBODY, HttpStatus.UNAUTHORIZED);

    // Bad credentials.
    runTestGetConfigUrl(url, MediaType.APPLICATION_JSON, null, ANYBODY,
	HttpStatus.UNAUTHORIZED);

    // Bad credentials.
    runTestGetConfigUrl(url, MediaType.APPLICATION_JSON, hrp, ANYBODY,
	HttpStatus.UNAUTHORIZED);

    // Bad credentials.
    runTestGetConfigUrl(url, MediaType.MULTIPART_FORM_DATA, null, ANYBODY,
	HttpStatus.UNAUTHORIZED);

    // Bad credentials.
    runTestGetConfigUrl(url, MediaType.MULTIPART_FORM_DATA, hrp, ANYBODY,
	HttpStatus.UNAUTHORIZED);

    url = "http://localhost:12345";

    // Missing Accept header for UNAUTHORIZED response.
    runTestGetConfigUrl(url, null, null, null, HttpStatus.UNAUTHORIZED);

    // Missing credentials.
    runTestGetConfigUrl(url, MediaType.APPLICATION_JSON, null, null,
	HttpStatus.UNAUTHORIZED);

    // Missing credentials.
    runTestGetConfigUrl(url, MediaType.MULTIPART_FORM_DATA, null, null,
	HttpStatus.UNAUTHORIZED);

    // Missing credentials.
    runTestGetConfigUrl(url, null, hrp, null, HttpStatus.UNAUTHORIZED);

    // Missing credentials.
    runTestGetConfigUrl(url, MediaType.APPLICATION_JSON, hrp, null,
	HttpStatus.UNAUTHORIZED);

    // Missing credentials.
    runTestGetConfigUrl(url, MediaType.MULTIPART_FORM_DATA, hrp, null,
	HttpStatus.UNAUTHORIZED);

    // Bad credentials.
    runTestGetConfigUrl(url, null, null, ANYBODY, HttpStatus.UNAUTHORIZED);

    // Bad credentials.
    runTestGetConfigUrl(url, null, hrp, ANYBODY, HttpStatus.UNAUTHORIZED);

    // Bad credentials.
    runTestGetConfigUrl(url, MediaType.APPLICATION_JSON, null, ANYBODY,
	HttpStatus.UNAUTHORIZED);

    // Bad credentials.
    runTestGetConfigUrl(url, MediaType.APPLICATION_JSON, hrp, ANYBODY,
	HttpStatus.UNAUTHORIZED);

    // Bad credentials.
    runTestGetConfigUrl(url, MediaType.MULTIPART_FORM_DATA, null, ANYBODY,
	HttpStatus.UNAUTHORIZED);

    // Bad credentials.
    runTestGetConfigUrl(url, MediaType.MULTIPART_FORM_DATA, hrp, ANYBODY,
	HttpStatus.UNAUTHORIZED);

    url = "http://example.com";

    // Missing Accept header for UNAUTHORIZED response.
    runTestGetConfigUrl(url, null, null, null, HttpStatus.UNAUTHORIZED);

    // Missing credentials.
    runTestGetConfigUrl(url, MediaType.APPLICATION_JSON, null, null,
	HttpStatus.UNAUTHORIZED);

    // Missing credentials.
    runTestGetConfigUrl(url, MediaType.MULTIPART_FORM_DATA, null, null,
	HttpStatus.UNAUTHORIZED);

    // Missing credentials.
    runTestGetConfigUrl(url, null, hrp, null, HttpStatus.UNAUTHORIZED);

    // Missing credentials.
    runTestGetConfigUrl(url, MediaType.APPLICATION_JSON, hrp, null,
	HttpStatus.UNAUTHORIZED);

    // Missing credentials.
    runTestGetConfigUrl(url, MediaType.MULTIPART_FORM_DATA, hrp, null,
	HttpStatus.UNAUTHORIZED);

    // Bad credentials.
    runTestGetConfigUrl(url, null, null, ANYBODY, HttpStatus.UNAUTHORIZED);

    // Bad credentials.
    runTestGetConfigUrl(url, null, hrp, ANYBODY, HttpStatus.UNAUTHORIZED);

    // Bad credentials.
    runTestGetConfigUrl(url, MediaType.APPLICATION_JSON, null, ANYBODY,
	HttpStatus.UNAUTHORIZED);

    // Bad credentials.
    runTestGetConfigUrl(url, MediaType.APPLICATION_JSON, hrp, ANYBODY,
	HttpStatus.UNAUTHORIZED);

    // Bad credentials.
    runTestGetConfigUrl(url, MediaType.MULTIPART_FORM_DATA, null, ANYBODY,
	HttpStatus.UNAUTHORIZED);

    // Bad credentials.
    runTestGetConfigUrl(url, MediaType.MULTIPART_FORM_DATA, hrp, ANYBODY,
	HttpStatus.UNAUTHORIZED);

    url = "dyn:cluster.xml";

    runTestGetConfigUrl(url, MediaType.MULTIPART_FORM_DATA, null, null,
	HttpStatus.UNAUTHORIZED);

    runTestGetConfigUrl(url, MediaType.MULTIPART_FORM_DATA, hrp, null,
	HttpStatus.UNAUTHORIZED);

    ifNoneMatch = ListUtil.list(NUMERIC_PRECONDITION);
    hrp = new HttpRequestPreconditions(null, null, ifNoneMatch, null);
    runTestGetConfigUrl(url, MediaType.MULTIPART_FORM_DATA, hrp, null,
	HttpStatus.UNAUTHORIZED);

    ifNoneMatch = ListUtil.list(ASTERISK_PRECONDITION);
    hrp = new HttpRequestPreconditions(null, null, ifNoneMatch, null);
    runTestGetConfigUrl(url, MediaType.MULTIPART_FORM_DATA, hrp, null,
	HttpStatus.UNAUTHORIZED);

    runTestGetConfigUrl(url, MediaType.MULTIPART_FORM_DATA, null, ANYBODY,
	HttpStatus.UNAUTHORIZED);

    ifNoneMatch = ListUtil.list(ZERO_PRECONDITION);
    hrp = new HttpRequestPreconditions(null, null, ifNoneMatch, null);
    runTestGetConfigUrl(url, MediaType.MULTIPART_FORM_DATA, hrp, ANYBODY,
	HttpStatus.UNAUTHORIZED);

    ifNoneMatch = ListUtil.list(NUMERIC_PRECONDITION);
    hrp = new HttpRequestPreconditions(null, null, ifNoneMatch, null);
    runTestGetConfigUrl(url, MediaType.MULTIPART_FORM_DATA, hrp, ANYBODY,
	HttpStatus.UNAUTHORIZED);

    ifNoneMatch = ListUtil.list(ASTERISK_PRECONDITION);
    hrp = new HttpRequestPreconditions(null, null, ifNoneMatch, null);
    runTestGetConfigUrl(url, MediaType.MULTIPART_FORM_DATA, hrp, ANYBODY,
	HttpStatus.UNAUTHORIZED);

    List<String> ifMatch = ListUtil.list(NUMERIC_PRECONDITION);
    hrp = new HttpRequestPreconditions(ifMatch, null, null, null);
    runTestGetConfigUrl(url, MediaType.MULTIPART_FORM_DATA, hrp, null,
	HttpStatus.UNAUTHORIZED);

    runTestGetConfigUrl(url, MediaType.MULTIPART_FORM_DATA, hrp, ANYBODY,
	HttpStatus.UNAUTHORIZED);

    getConfigUrlCommonTest();

    log.debug2("Done");
  }

  /**
   * Runs the getConfigUrl()-related authentication-independent tests.
   * 
   * @throws Exception
   *           if there are problems.
   */
  private void getConfigUrlCommonTest() throws Exception {
    log.debug2("Invoked");

    // No URL.
    runTestGetConfigUrl(null, null, null, USER_ADMIN,
	HttpStatus.NOT_ACCEPTABLE);

    HttpRequestPreconditions hrp =
	new HttpRequestPreconditions(null, null, null, null);

    runTestGetConfigUrl(null, null, hrp, CONTENT_ADMIN,
	HttpStatus.NOT_ACCEPTABLE);

    // Empty URL.
    runTestGetConfigUrl(EMPTY_STRING, null, null, USER_ADMIN,
	HttpStatus.NOT_ACCEPTABLE);

    hrp = new HttpRequestPreconditions(EMPTY_PRECONDITION_LIST, EMPTY_STRING,
	  EMPTY_PRECONDITION_LIST, EMPTY_STRING);

    runTestGetConfigUrl(EMPTY_STRING, null, hrp, CONTENT_ADMIN,
	HttpStatus.NOT_ACCEPTABLE);

    String url = "http://something";

    // Bad Accept header content type.
    runTestGetConfigUrl(url, null, null, USER_ADMIN, HttpStatus.NOT_ACCEPTABLE);

    List<String> IfMatchNoMatch = ListUtil.list(EMPTY_PRECONDITION);
    hrp = new HttpRequestPreconditions(IfMatchNoMatch, EMPTY_STRING, null,
	EMPTY_STRING);

    runTestGetConfigUrl(url, null, hrp, CONTENT_ADMIN,
	HttpStatus.NOT_ACCEPTABLE);

    // Bad Accept header content type.
    runTestGetConfigUrl(url, MediaType.APPLICATION_JSON, null, USER_ADMIN,
	HttpStatus.NOT_ACCEPTABLE);

    hrp = new HttpRequestPreconditions(null, EMPTY_STRING, IfMatchNoMatch,
	EMPTY_STRING);

    runTestGetConfigUrl(url, MediaType.APPLICATION_JSON, hrp, CONTENT_ADMIN,
	HttpStatus.NOT_ACCEPTABLE);

    // Bad Accept header content type.
    List<String> ifNoneMatch = ListUtil.list(ZERO_PRECONDITION);
    hrp = new HttpRequestPreconditions(null, null, ifNoneMatch, null);
    runTestGetConfigUrl(url, MediaType.APPLICATION_JSON, hrp, USER_ADMIN,
	HttpStatus.NOT_ACCEPTABLE);

    // Bad Accept header content type.
    runTestGetConfigUrl(url, null, hrp, CONTENT_ADMIN,
	HttpStatus.NOT_ACCEPTABLE);

    // Nothing there.
    runTestGetConfigUrl(url, MediaType.MULTIPART_FORM_DATA, hrp, USER_ADMIN,
	HttpStatus.NOT_FOUND);

    url = "http://example.com";

    // Bad Accept header content type.
    MultipartResponse configOutput = runTestGetConfigUrl(url, null, null,
	CONTENT_ADMIN, HttpStatus.NOT_ACCEPTABLE);

    // Bad Accept header content type.
    runTestGetConfigUrl(url, null, hrp, USER_ADMIN, HttpStatus.NOT_ACCEPTABLE);

    // Bad Accept header content type.
    runTestGetConfigUrl(url, MediaType.APPLICATION_JSON, null, CONTENT_ADMIN,
	HttpStatus.NOT_ACCEPTABLE);

    // Bad Accept header content type.
    runTestGetConfigUrl(url, MediaType.APPLICATION_JSON, hrp, USER_ADMIN,
	HttpStatus.NOT_ACCEPTABLE);

    // Success.
    configOutput = runTestGetConfigUrl(url, MediaType.MULTIPART_FORM_DATA, null,
	CONTENT_ADMIN, HttpStatus.OK);

    List<String> expectedPayloads =
	ListUtil.list("<title>Example Domain</title>");

    Part part = verifyMultipartResponse(configOutput, MediaType.TEXT_HTML,
	expectedPayloads);

    // Verify the part last modification timestamp.
    String lastModifiedHeaderValue = part.getLastModified();

    // Not modified since last read.
    hrp =
	new HttpRequestPreconditions(null, lastModifiedHeaderValue, null, null);

    configOutput = runTestGetConfigUrl(url, MediaType.MULTIPART_FORM_DATA, hrp,
	USER_ADMIN, HttpStatus.NOT_MODIFIED);

    url = "dyn:cluster.xml";

    configOutput = runTestGetConfigUrl(url, MediaType.MULTIPART_FORM_DATA, null,
	CONTENT_ADMIN, HttpStatus.OK);

    expectedPayloads = ListUtil.list(
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

    part = verifyMultipartResponse(configOutput, MediaType.TEXT_XML,
	expectedPayloads);

    // Verify the part last modification timestamps.
    verifyPartModificationTimestamps(part, null);

    // Independent verification.
    assertTrue(StringUtil.fromInputStream(ConfigManager.getConfigManager()
	.conditionallyReadCacheConfigFile(url, null).getInputStream())
	.indexOf(StringUtil.separatedString(expectedPayloads, "\n")) > 0);

    // Not modified since last read.
    hrp =
	new HttpRequestPreconditions(null, part.getLastModified(), null, null);

    runTestGetConfigUrl(url, MediaType.MULTIPART_FORM_DATA, hrp, USER_ADMIN,
	HttpStatus.NOT_MODIFIED);

    // Not modified since last read.
    ifNoneMatch = ListUtil.list(part.getEtag());
    hrp = new HttpRequestPreconditions(null, null, ifNoneMatch, null);
    runTestGetConfigUrl(url, MediaType.MULTIPART_FORM_DATA, hrp, CONTENT_ADMIN,
	HttpStatus.NOT_MODIFIED);

    // Not modified since last read.
    hrp = new HttpRequestPreconditions(null, part.getLastModified(),
	ifNoneMatch, null);

    runTestGetConfigUrl(url, MediaType.MULTIPART_FORM_DATA, hrp, USER_ADMIN,
	HttpStatus.NOT_MODIFIED);

    // Success.
    ifNoneMatch = ListUtil.list(ZERO_PRECONDITION);
    hrp = new HttpRequestPreconditions(null, null, ifNoneMatch, null);

    configOutput = runTestGetConfigUrl(url, MediaType.MULTIPART_FORM_DATA, hrp,
	CONTENT_ADMIN, HttpStatus.OK);

    Part part2 = verifyMultipartResponse(configOutput, MediaType.TEXT_XML,
	expectedPayloads);

    // Verify the part last modification timestamps.
    verifyPartModificationTimestamps(part2, part);

    // Success.
    ifNoneMatch = ListUtil.list(NUMERIC_PRECONDITION);
    hrp = new HttpRequestPreconditions(null, null, ifNoneMatch, null);

    configOutput = runTestGetConfigUrl(url, MediaType.MULTIPART_FORM_DATA, hrp,
	USER_ADMIN, HttpStatus.OK);

    part2 = verifyMultipartResponse(configOutput, MediaType.TEXT_XML,
	expectedPayloads);

    // Verify the part last modification timestamps.
    verifyPartModificationTimestamps(part2, part);

    // Not modified since last read.
    ifNoneMatch = ListUtil.list(ASTERISK_PRECONDITION);
    hrp = new HttpRequestPreconditions(null, null, ifNoneMatch, null);

    runTestGetConfigUrl(url, MediaType.MULTIPART_FORM_DATA, hrp, CONTENT_ADMIN,
	HttpStatus.NOT_MODIFIED);

    // Match of the If-Match precondition.
    List<String> ifMatch = ListUtil.list(part2.getEtag());
    hrp = new HttpRequestPreconditions(ifMatch, null, null, null);

    configOutput = runTestGetConfigUrl(url, MediaType.MULTIPART_FORM_DATA, hrp,
	USER_ADMIN, HttpStatus.OK);

    part2 = verifyMultipartResponse(configOutput, MediaType.TEXT_XML,
	expectedPayloads);

    // Verify the part last modification timestamps.
    verifyPartModificationTimestamps(part2, part);

    // Mismatch of the If-Match precondition.
    ifMatch = ListUtil.list(NUMERIC_PRECONDITION);
    hrp = new HttpRequestPreconditions(ifMatch, null, null, null);
    runTestGetConfigUrl(url, MediaType.MULTIPART_FORM_DATA, hrp, CONTENT_ADMIN,
	HttpStatus.PRECONDITION_FAILED);

    log.debug2("Done");

    // IP access control tests

    // No IP filters are defined; preceding tests are allowed because they
    // come from loopback address.  Disabling special allowance for
    // loopback should result in 403

    ConfigurationUtil.addFromArgs(SpringAuthenticationFilter.PARAM_ALLOW_LOOPBACK,
				  "false");
    runTestGetConfigUrl(url, MediaType.MULTIPART_FORM_DATA, hrp, CONTENT_ADMIN,
	HttpStatus.FORBIDDEN);


    // Now specifially allow 127.0.0.1, should work
    ConfigurationUtil.addFromArgs(SpringAuthenticationFilter.PARAM_IP_INCLUDE,
				  "127.0.0.1");
    runTestGetConfigUrl(url, MediaType.MULTIPART_FORM_DATA, hrp, CONTENT_ADMIN,
	HttpStatus.PRECONDITION_FAILED);

    // Restore default config as multiple tests are run in a single testcase
    ConfigurationUtil.addFromArgs(SpringAuthenticationFilter.PARAM_ALLOW_LOOPBACK,
				  "false",
				  SpringAuthenticationFilter.PARAM_IP_INCLUDE,
				  "127.0.0.1");

  }

  /**
   * Performs a GET operation for a configuration URL.
   * 
   * @param url
   *          A String with the configuration URL.
   * @param acceptContentType
   *          A MediaType with the content type to be added to the request
   *          "Accept" header.
   * @param preconditions
   *          An HttpRequestPreconditions with the request preconditions to be
   *          met.
   * @param credentials
   *          A Credentials with the request credentials.
   * @param expectedStatus
   *          An HttpStatus with the HTTP status of the result.
   * @return a MultipartResponse with the multipart response.
   * @throws Exception
   *           if there are problems.
   */
  private MultipartResponse runTestGetConfigUrl(String url,
      MediaType acceptContentType, HttpRequestPreconditions preconditions,
      Credentials credentials, HttpStatus expectedStatus) throws Exception {
    log.debug2("url = {}", url);
    log.debug2("acceptContentType = {}", acceptContentType);
    log.debug2("preconditions = {}", preconditions);
    log.debug2("credentials = {}", credentials);
    log.debug2("expectedStatus = {}", expectedStatus);

    // Get the test URL template.
    String template = getTestUrlTemplate("/config/url");

    // Create the URI of the request to the REST service.
    URI uri = UriComponentsBuilder.fromUriString(template)
	.queryParam("url", url).build().encode().toUri();
    log.trace("uri = {}", uri);

    // Initialize the request to the REST service.
    RestTemplateBuilder templateBuilder = RestUtil.getRestTemplateBuilder(0, 0);

    // Add our MultipartMessageHttpMessageConverter
//    templateBuilder.additionalMessageConverters(new MultipartMessageHttpMessageConverter());
    List<HttpMessageConverter<?>> messageConverters = new ArrayList<>();
    messageConverters.add(new MultipartMessageHttpMessageConverter());
    messageConverters.addAll(new RestTemplate().getMessageConverters());
    templateBuilder = templateBuilder.messageConverters(messageConverters);

    HttpEntity<String> requestEntity = null;

    // Get the individual preconditions.
    List<String> ifMatch = null;
    String ifModifiedSince = null;
    List<String> ifNoneMatch = null;
    String ifUnmodifiedSince = null;

    if (preconditions != null) {
      ifMatch = preconditions.getIfMatch();
      ifModifiedSince = preconditions.getIfModifiedSince();
      ifNoneMatch = preconditions.getIfNoneMatch();
      ifUnmodifiedSince = preconditions.getIfUnmodifiedSince();
    }

    // Get the individual credentials elements.
    String user = null;
    String password = null;

    if (credentials != null) {
      user = credentials.getUser();
      password = credentials.getPassword();
    }

    // Check whether there are any custom headers to be specified in the
    // request.
    if (acceptContentType != null || (ifMatch != null && !ifMatch.isEmpty())
	|| (ifModifiedSince != null && !ifModifiedSince.isEmpty())
	|| (ifNoneMatch != null && !ifNoneMatch.isEmpty())
	|| (ifUnmodifiedSince != null && !ifUnmodifiedSince.isEmpty())
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
	headers.setAccept(List.of(MediaType.APPLICATION_JSON));
      }

      // Check whether there is a custom If-Match header.
      if (ifMatch != null) {
	// Yes: Set the If-Match header.
	headers.setIfMatch(ifMatch);
      }

      // Check whether there is a custom If-Modified-Since header.
      if (ifModifiedSince != null) {
	// Yes: Set the If-Modified-Since header.
	headers.set(HttpHeaders.IF_MODIFIED_SINCE, ifModifiedSince);
      }

      // Check whether there is a custom If-None-Match header.
      if (ifNoneMatch != null) {
	// Yes: Set the If-None-Match header.
	headers.setIfNoneMatch(ifNoneMatch);
      }

      // Check whether there is a custom If-Unmodified-Since header.
      if (ifUnmodifiedSince != null) {
	// Yes: Set the If-Unmodified-Since header.
	headers.set(HttpHeaders.IF_UNMODIFIED_SINCE, ifUnmodifiedSince);
      }

      // Set up the authentication credentials, if necessary.
      if (credentials != null) {
	credentials.setUpBasicAuthentication(headers);
      }

      log.trace("requestHeaders = {}", () -> headers.toSingleValueMap());

      // Create the request entity.
      requestEntity = new HttpEntity<String>(null, headers);
    }

    try {
      // Make the request and get the response.
      TestRestTemplate testTemplate = new TestRestTemplate(templateBuilder);
//      templateBuilder.setErrorHandler(new LockssResponseErrorHandler(templateBuilder.getMessageConverters()));

      ResponseEntity<MultipartMessage> response = testTemplate
          .exchange(uri, HttpMethod.GET, requestEntity, MultipartMessage.class);

    // Get the response status.
    HttpStatusCode statusCode = response.getStatusCode();
    HttpStatus status = HttpStatus.valueOf(statusCode.value());
    assertEquals(expectedStatus, status);

    MultipartResponse parsedResponse = null;

    // Check whether it is a success response.
    if (isSuccess(status)) {
      // Yes: Parse it.
      parsedResponse = new MultipartResponse(response);
    }

    // Return the parsed response.
    if (log.isDebug2Enabled())
      log.debug2("parsedResponse = {}", parsedResponse);
    return parsedResponse;

    } catch (LockssResponseErrorHandler.WrappedLockssRestHttpException e) {
      LockssRestHttpException lhre = e.getLRHE();

      HttpStatus statusCode = lhre.getHttpStatus();
      assertFalse(RestUtil.isSuccess(statusCode));
      assertEquals(expectedStatus, statusCode);
    }

    return null;
  }

  /**
   * Runs the getLastUpdateTime()-related un-authenticated-specific tests.
   */
  private void getLastUpdateTimeUnAuthenticatedTest() {
    log.debug2("Invoked");

    runTestGetLastUpdateTime(null, HttpStatus.OK);
    runTestGetLastUpdateTime(ANYBODY, HttpStatus.OK);

    getLastUpdateTimeCommonTest();

    log.debug2("Done");
  }

  /**
   * Runs the getLastUpdateTime()-related authenticated-specific tests.
   */
  private void getLastUpdateTimeAuthenticatedTest() {
    log.debug2("Invoked");

    runTestGetLastUpdateTime(null, HttpStatus.UNAUTHORIZED);
    runTestGetLastUpdateTime(ANYBODY, HttpStatus.UNAUTHORIZED);

    getLastUpdateTimeCommonTest();

    log.debug2("Done");
  }

  /**
   * Runs the getLastUpdateTime()-related authentication-independent tests.
   */
  private void getLastUpdateTimeCommonTest() {
    log.debug2("Invoked");

    runTestGetLastUpdateTime(USER_ADMIN, HttpStatus.OK);
    runTestGetLastUpdateTime(CONTENT_ADMIN, HttpStatus.OK);

    log.debug2("Done");
  }

  /**
   * Performs a GET lastupdatetime operation.
   * 
   * @param credentials
   *          A Credentials with the request credentials.
   * @param expectedStatus
   *          An HttpStatus with the HTTP status of the result.
   */
  private void runTestGetLastUpdateTime(Credentials credentials,
      HttpStatus expectedStatus) {
    log.debug2("credentials = {}", credentials);
    log.debug2("expectedStatus = {}", expectedStatus);

    // Get the test URL template.
    String template = getTestUrlTemplate("/config/lastupdatetime");

    // Create the URI of the request to the REST service.
    UriComponents uriComponents =
	UriComponentsBuilder.fromUriString(template).build();

    URI uri = UriComponentsBuilder.newInstance().uriComponents(uriComponents)
	.build().encode().toUri();
    log.trace("uri = {}", uri);

    // Initialize the request to the REST service.
    RestTemplateBuilder templateBuilder = RestUtil.getRestTemplateBuilder(0, 0);

    HttpEntity<String> requestEntity = null;

    // Check whether there are any custom headers to be specified in the
    // request.
    if (credentials != null) {
      // Yes: Initialize the request headers.
      HttpHeaders headers = new HttpHeaders();

      // Set up the authentication credentials, if necessary.
      credentials.setUpBasicAuthentication(headers);

      log.trace("requestHeaders = {}", () -> headers.toSingleValueMap());

      // Create the request entity.
      requestEntity = new HttpEntity<String>(null, headers);
    }

    // Make the request and get the response. 
    ResponseEntity<OffsetDateTime> response = new TestRestTemplate(templateBuilder)
	.exchange(uri, HttpMethod.GET, requestEntity, OffsetDateTime.class);

    // Get the response status.
    HttpStatusCode statusCode = response.getStatusCode();
    HttpStatus status = HttpStatus.valueOf(statusCode.value());
    assertEquals(expectedStatus, status);

    // Check whether it is a success response.
    if (isSuccess(status)) {
      // Yes: Convert OffsetDateTime to milliseconds since epoch
      long lastUpdateTime = response.getBody().toInstant().toEpochMilli();

      // Validate it.
      assertEquals(ConfigManager.getConfigManager().getLastUpdateTime(),
	  lastUpdateTime);

      long now = TimeBase.nowMs();
      assertTrue(now > lastUpdateTime);
      assertTrue(now - lastUpdateTime < 100000);
    }

    log.debug2("Done");
  }

  /**
   * Runs the getLoadedUrlList()-related un-authenticated-specific tests.
   * 
   * @throws Exception
   *           if there are problems.
   */
  private void getLoadedUrlListUnAuthenticatedTest() throws Exception {
    log.debug2("Invoked");

    runTestGetLoadedUrlList(null, HttpStatus.OK);
    runTestGetLoadedUrlList(ANYBODY, HttpStatus.OK);

    getLoadedUrlListCommonTest();

    log.debug2("Done");
  }

  /**
   * Runs the getLoadedUrlList()-related authenticated-specific tests.
   * 
   * @throws Exception
   *           if there are problems.
   */
  private void getLoadedUrlListAuthenticatedTest() throws Exception {
    log.debug2("Invoked");

    runTestGetLoadedUrlList(null, HttpStatus.UNAUTHORIZED);
    runTestGetLoadedUrlList(ANYBODY, HttpStatus.UNAUTHORIZED);

    getLoadedUrlListCommonTest();

    log.debug2("Done");
  }

  /**
   * Runs the getLoadedUrlList()-related authentication-independent tests.
   * 
   * @throws Exception
   *           if there are problems.
   */
  private void getLoadedUrlListCommonTest() throws Exception {
    log.debug2("Invoked");

    runTestGetLoadedUrlList(USER_ADMIN, HttpStatus.OK);
    runTestGetLoadedUrlList(CONTENT_ADMIN, HttpStatus.OK);

    log.debug2("Done");
  }

  /**
   * Performs a GET loadedurls operation.
   * 
   * @param credentials
   *          A Credentials with the request credentials.
   * @param expectedStatus
   *          An HttpStatus with the HTTP status of the result.
   * 
   * @throws Exception
   *           if there are problems.
   */
  private void runTestGetLoadedUrlList(Credentials credentials,
      HttpStatus expectedStatus) throws Exception {
    log.debug2("credentials = {}", credentials);
    log.debug2("expectedStatus = {}", expectedStatus);

    // Get the test URL template.
    String template = getTestUrlTemplate("/config/loadedurls");

    // Create the URI of the request to the REST service.
    UriComponents uriComponents =
	UriComponentsBuilder.fromUriString(template).build();

    URI uri = UriComponentsBuilder.newInstance().uriComponents(uriComponents)
	.build().encode().toUri();
    log.trace("uri = {}", uri);

    // Initialize the request to the REST service.
    RestTemplateBuilder templateBuilder = RestUtil.getRestTemplateBuilder(0, 0);

    HttpEntity<String> requestEntity = null;

    // Check whether there are any custom headers to be specified in the
    // request.
    if (credentials != null) {
      // Yes: Initialize the request headers.
      HttpHeaders headers = new HttpHeaders();

      // Set up the authentication credentials, if necessary.
      credentials.setUpBasicAuthentication(headers);

      log.trace("requestHeaders = {}", () -> headers.toSingleValueMap());

      // Create the request entity.
      requestEntity = new HttpEntity<String>(null, headers);
    }

    // Make the request and get the response. 
    ResponseEntity<?> response = new TestRestTemplate(templateBuilder)
	.exchange(uri, HttpMethod.GET, requestEntity, String.class);

    // Get the response status.
    HttpStatusCode statusCode = response.getStatusCode();
    HttpStatus status = HttpStatus.valueOf(statusCode.value());
    assertEquals(expectedStatus, status);

    // Check whether it is a success response.
    if (isSuccess(status)) {
      // Yes: Parse it.
      ObjectMapper mapper = new ObjectMapper();
      List<String> result = mapper.readValue((String)response.getBody(),
	  new TypeReference<List<String>>(){});

      assertEquals(ConfigManager.getConfigManager().getLoadedUrlList(), result);
    }

    log.debug2("Done");
  }

  /**
   * Runs the putConfig()-related un-authenticated-specific tests.
   */
  private void putConfigUnAuthenticatedTest() throws Exception {
    log.debug2("Invoked");

    // No section: Spring reports it cannot find a match to an endpoint.
    runTestPutConfig(null, null, null, null, null, HttpStatus.NOT_FOUND);

    HttpRequestPreconditions hrp =
	new HttpRequestPreconditions(null, null, null, null);

    runTestPutConfig(null, null, null, hrp, null, HttpStatus.NOT_FOUND);

    // Empty section: Spring reports it cannot find a match to an endpoint.
    runTestPutConfig(null, EMPTY_STRING, null, null, null,
	HttpStatus.NOT_FOUND);

    hrp = new HttpRequestPreconditions(EMPTY_PRECONDITION_LIST, EMPTY_STRING,
	EMPTY_PRECONDITION_LIST, EMPTY_STRING);

    runTestPutConfig(null, EMPTY_STRING, null, hrp, null, HttpStatus.NOT_FOUND);

    // Missing Content-Type header.
    runTestPutConfig(null, SECTION_NAME_PLUGIN, null, null, null,
	HttpStatus.UNSUPPORTED_MEDIA_TYPE);

    List<String> IfMatchNoMatch = ListUtil.list(EMPTY_PRECONDITION);
    hrp = new HttpRequestPreconditions(IfMatchNoMatch, EMPTY_STRING, null,
	EMPTY_STRING);

    runTestPutConfig(null, SECTION_NAME_PLUGIN, null, hrp, null,
	HttpStatus.UNSUPPORTED_MEDIA_TYPE);

    // Missing Content-Type header.
    runTestPutConfig(null, SECTION_NAME_PLUGIN, null, null, ANYBODY,
	HttpStatus.UNSUPPORTED_MEDIA_TYPE);

    hrp = new HttpRequestPreconditions(null, EMPTY_STRING, IfMatchNoMatch,
	EMPTY_STRING);

    runTestPutConfig(null, SECTION_NAME_PLUGIN, null, hrp, ANYBODY,
	HttpStatus.UNSUPPORTED_MEDIA_TYPE);

    // Missing Content-Type header.
    runTestPutConfig(null, SECTION_NAME_PLUGIN, null, null,
	CONTENT_ADMIN, HttpStatus.UNSUPPORTED_MEDIA_TYPE);

    hrp = new HttpRequestPreconditions(null, EMPTY_STRING, IfMatchNoMatch,
	EMPTY_STRING);

    runTestPutConfig(null, SECTION_NAME_PLUGIN, null, hrp,
	CONTENT_ADMIN, HttpStatus.UNSUPPORTED_MEDIA_TYPE);

    // Bad Content-Type header.
    runTestPutConfig(null, SECTION_NAME_PLUGIN,
	MediaType.APPLICATION_JSON, null, null,
	HttpStatus.UNSUPPORTED_MEDIA_TYPE);

    runTestPutConfig(null, SECTION_NAME_PLUGIN,
	MediaType.APPLICATION_JSON, null, ANYBODY,
	HttpStatus.UNSUPPORTED_MEDIA_TYPE);

    runTestPutConfig(null, SECTION_NAME_PLUGIN,
	MediaType.APPLICATION_JSON, null, CONTENT_ADMIN,
	HttpStatus.UNSUPPORTED_MEDIA_TYPE);

    // Missing payload (This should return HttpStatus.BAD_REQUEST, but Spring
    // returns HttpStatus.INTERNAL_SERVER_ERROR).
    runTestPutConfig(null, SECTION_NAME_PLUGIN,
	MediaType.MULTIPART_FORM_DATA, null, null,
	HttpStatus.INTERNAL_SERVER_ERROR);

    runTestPutConfig(null, SECTION_NAME_PLUGIN,
	MediaType.MULTIPART_FORM_DATA, null, ANYBODY,
	HttpStatus.INTERNAL_SERVER_ERROR);

    runTestPutConfig(null, SECTION_NAME_PLUGIN,
	MediaType.MULTIPART_FORM_DATA, null, CONTENT_ADMIN,
	HttpStatus.INTERNAL_SERVER_ERROR);

    // Success.
    long beforeWrite = TimeBase.nowMs();

    runTestPutConfig("a1=b1", SECTION_NAME_PLUGIN, null, null, null,
	HttpStatus.OK);

    MultipartResponse configOutput = runTestGetConfigSection(
	SECTION_NAME_PLUGIN, MediaType.MULTIPART_FORM_DATA, null,
	null, HttpStatus.OK);

    Part part = verifyMultipartResponse(configOutput, MediaType.TEXT_PLAIN,
	ListUtil.list("a1=b1"));

    // Verify the part last modification timestamps.
    verifyPartModificationTimestamps(part, null);
    assertTrue(beforeWrite <= Long.parseLong(part.getLastModified()));

    // Success.
    hrp = new HttpRequestPreconditions(null, null, null, null);
    beforeWrite = TimeBase.nowMs();

    runTestPutConfig("a1=b2", SECTION_NAME_PLUGIN, null, hrp, ANYBODY,
	HttpStatus.OK);

    configOutput = runTestGetConfigSection(SECTION_NAME_PLUGIN,
	MediaType.MULTIPART_FORM_DATA, hrp, null, HttpStatus.OK);

    part = verifyMultipartResponse(configOutput, MediaType.TEXT_PLAIN,
	ListUtil.list("a1=b2"));

    // Verify the part last modification timestamps.
    verifyPartModificationTimestamps(part, null);
    assertTrue(beforeWrite <= Long.parseLong(part.getLastModified()));

    // Bad Content-Type header.
    try {
      runTestPutConfig("a1=b3", SECTION_NAME_PLUGIN,
	  MediaType.APPLICATION_JSON, null, null, HttpStatus.BAD_REQUEST);
      fail("Should have thrown HttpMessageConversionException");
    } catch (HttpMessageConversionException e) {
      // FIXME
      assertMatchesRE("Type definition error", e.getMessage());
    }

    try {
      runTestPutConfig("a1=b3", SECTION_NAME_PLUGIN,
	  MediaType.APPLICATION_JSON, null, ANYBODY, HttpStatus.BAD_REQUEST);
      fail("Should have thrown HttpMessageConversionException");
    } catch (HttpMessageConversionException e) {
      // FIXME
      assertMatchesRE("Type definition error", e.getMessage());
    }

    try {
      runTestPutConfig("a1=b3", SECTION_NAME_PLUGIN,
	  MediaType.APPLICATION_JSON, null, CONTENT_ADMIN,
	  HttpStatus.BAD_REQUEST);
      fail("Should have thrown HttpMessageConversionException");
    } catch (HttpMessageConversionException e) {
      // FIXME
      assertMatchesRE("Type definition error", e.getMessage());
    }

    // Success.
    hrp = new HttpRequestPreconditions(EMPTY_PRECONDITION_LIST, EMPTY_STRING,
	EMPTY_PRECONDITION_LIST, EMPTY_STRING);
    beforeWrite = TimeBase.nowMs();

    runTestPutConfig("a1=b3", SECTION_NAME_PLUGIN,
	MediaType.MULTIPART_FORM_DATA, hrp, CONTENT_ADMIN, HttpStatus.OK);

    configOutput = runTestGetConfigSection(SECTION_NAME_PLUGIN,
	MediaType.MULTIPART_FORM_DATA, hrp, CONTENT_ADMIN, HttpStatus.OK);

    part = verifyMultipartResponse(configOutput, MediaType.TEXT_PLAIN,
	ListUtil.list("a1=b3"));

    // Verify the part last modification timestamps.
    verifyPartModificationTimestamps(part, null);
    assertTrue(beforeWrite <= Long.parseLong(part.getLastModified()));

    // File has been changed.
    List<String> ifMatch = ListUtil.list(ZERO_PRECONDITION);
    hrp = new HttpRequestPreconditions(ifMatch, null, null, null);

    runTestPutConfig("a1=b4", SECTION_NAME_PLUGIN, null, hrp, null,
	HttpStatus.PRECONDITION_FAILED);

    runTestPutConfig("a1=b4", SECTION_NAME_PLUGIN, null, hrp, ANYBODY,
	HttpStatus.PRECONDITION_FAILED);

    runTestPutConfig("a1=b4", SECTION_NAME_PLUGIN, null, hrp,
	CONTENT_ADMIN, HttpStatus.PRECONDITION_FAILED);

    ifMatch = ListUtil.list(ZERO_PRECONDITION, NUMERIC_PRECONDITION);
    hrp = new HttpRequestPreconditions(ifMatch, null, null,
	part.getLastModified());

    runTestPutConfig("a1=b4", SECTION_NAME_PLUGIN, null, hrp, null,
	HttpStatus.PRECONDITION_FAILED);

    runTestPutConfig("a1=b4", SECTION_NAME_PLUGIN, null, hrp, ANYBODY,
	HttpStatus.PRECONDITION_FAILED);

    runTestPutConfig("a1=b4", SECTION_NAME_PLUGIN, null, hrp,
	CONTENT_ADMIN, HttpStatus.PRECONDITION_FAILED);

    ifMatch = ListUtil.list(ZERO_PRECONDITION, NUMERIC_PRECONDITION,
	ALPHA_PRECONDITION);
    hrp = new HttpRequestPreconditions(ifMatch, null, null, null);

    runTestPutConfig("a1=b4", SECTION_NAME_PLUGIN, null, hrp, null,
	HttpStatus.PRECONDITION_FAILED);

    runTestPutConfig("a1=b4", SECTION_NAME_PLUGIN, null, hrp, ANYBODY,
	HttpStatus.PRECONDITION_FAILED);

    runTestPutConfig("a1=b4", SECTION_NAME_PLUGIN, null, hrp,
	CONTENT_ADMIN, HttpStatus.PRECONDITION_FAILED);

    // Verify that nothing was written when the preconditions failed.
    List<String> ifNoneMatch = ListUtil.list(ZERO_PRECONDITION);
    hrp = new HttpRequestPreconditions(null, null, ifNoneMatch, null);

    configOutput = runTestGetConfigSection(SECTION_NAME_PLUGIN,
	MediaType.MULTIPART_FORM_DATA, hrp, null, HttpStatus.OK);

    Part part2 = verifyMultipartResponse(configOutput, MediaType.TEXT_PLAIN,
	ListUtil.list("a1=b3"));

    // Verify the part last modification timestamps.
    verifyPartModificationTimestamps(part2, part);

    ifMatch = ListUtil.list(part2.getEtag());
    hrp = new HttpRequestPreconditions(ifMatch, null, null, null);
    beforeWrite = TimeBase.nowMs();

    runTestPutConfig("a2=b2", SECTION_NAME_PLUGIN, null, hrp, null,
	HttpStatus.OK);

    configOutput = runTestGetConfigSection(SECTION_NAME_PLUGIN,
	MediaType.MULTIPART_FORM_DATA, null, null, HttpStatus.OK);

    part = verifyMultipartResponse(configOutput, MediaType.TEXT_PLAIN,
	ListUtil.list("a2=b2"));

    // Verify the part last modification timestamps.
    verifyPartModificationTimestamps(part, null);
    assertTrue(beforeWrite <= Long.parseLong(part.getLastModified()));

    // Bad Content-Type header.
    try {
      ifMatch = ListUtil.list(ZERO_PRECONDITION);
      hrp = new HttpRequestPreconditions(ifMatch, null, null, null);
      runTestPutConfig("a3=b3", SECTION_NAME_PLUGIN,
	  MediaType.APPLICATION_JSON, hrp, null, HttpStatus.BAD_REQUEST);
      fail("Should have thrown HttpMessageConversionException");
    } catch (HttpMessageConversionException e) {
      // FIXME
      assertMatchesRE("Type definition error", e.getMessage());
    }

    try {
      ifMatch = ListUtil.list(ZERO_PRECONDITION);
      hrp = new HttpRequestPreconditions(ifMatch, null, null, null);
      runTestPutConfig("a3=b3", SECTION_NAME_PLUGIN,
	  MediaType.APPLICATION_JSON, hrp, ANYBODY, HttpStatus.BAD_REQUEST);
      fail("Should have thrown HttpMessageConversionException");
    } catch (HttpMessageConversionException e) {
      // FIXME
      assertMatchesRE("Type definition error", e.getMessage());
    }

    try {
      ifMatch = ListUtil.list(ZERO_PRECONDITION);
      hrp = new HttpRequestPreconditions(ifMatch, null, null, null);
      runTestPutConfig("a3=b3", SECTION_NAME_PLUGIN,
	  MediaType.APPLICATION_JSON, hrp, CONTENT_ADMIN,
	  HttpStatus.BAD_REQUEST);
      fail("Should have thrown HttpMessageConversionException");
    } catch (HttpMessageConversionException e) {
      // FIXME
      assertMatchesRE("Type definition error", e.getMessage());
    }

    // Modified since creation time.
    ifMatch = ListUtil.list(ZERO_PRECONDITION);
    hrp = new HttpRequestPreconditions(ifMatch, null, null, null);

    runTestPutConfig("a3=b3", SECTION_NAME_PLUGIN,
	MediaType.MULTIPART_FORM_DATA, hrp, ANYBODY,
	HttpStatus.PRECONDITION_FAILED);

    runTestPutConfig("a3=b3", SECTION_NAME_PLUGIN,
	MediaType.MULTIPART_FORM_DATA, hrp, CONTENT_ADMIN,
	HttpStatus.PRECONDITION_FAILED);

    // Matching the passed if-Match ETag.
    ifMatch = ListUtil.list(part.getEtag());
    hrp = new HttpRequestPreconditions(ifMatch, null, null, null);
    beforeWrite = TimeBase.nowMs();

    runTestPutConfig("a3=b3", SECTION_NAME_PLUGIN, null, hrp, ANYBODY,
	HttpStatus.OK);

    configOutput = runTestGetConfigSection(SECTION_NAME_PLUGIN,
	MediaType.MULTIPART_FORM_DATA, null, ANYBODY, HttpStatus.OK);

    part = verifyMultipartResponse(configOutput, MediaType.TEXT_PLAIN,
	ListUtil.list("a3=b3"));

    // Verify the part last modification timestamps.
    verifyPartModificationTimestamps(part, null);
    assertTrue(beforeWrite <= Long.parseLong(part.getLastModified()));

    // Matching one of the passed if-Match ETags.
    ifMatch = ListUtil.list(part.getEtag(), NUMERIC_PRECONDITION);
    hrp = new HttpRequestPreconditions(ifMatch, null, null, null);
    beforeWrite = TimeBase.nowMs();

    runTestPutConfig("a4=b4", SECTION_NAME_PLUGIN, null, hrp,
	CONTENT_ADMIN, HttpStatus.OK);

    configOutput = runTestGetConfigSection(SECTION_NAME_PLUGIN,
	MediaType.MULTIPART_FORM_DATA, null, CONTENT_ADMIN, HttpStatus.OK);

    part = verifyMultipartResponse(configOutput, MediaType.TEXT_PLAIN,
	ListUtil.list("a4=b4"));

    // Verify the part last modification timestamps.
    verifyPartModificationTimestamps(part, null);
    assertTrue(beforeWrite <= Long.parseLong(part.getLastModified()));

    // Matching one of the passed if-Match ETags and the If-Unmodified-Since
    // header.
    ifMatch = ListUtil.list(part.getEtag(), NUMERIC_PRECONDITION);
    hrp = new HttpRequestPreconditions(ifMatch, null, null,
	part.getLastModified());
    beforeWrite = TimeBase.nowMs();

    runTestPutConfig("a5=b5", SECTION_NAME_PLUGIN, null, hrp, null,
	HttpStatus.OK);

    configOutput = runTestGetConfigSection(SECTION_NAME_PLUGIN,
	MediaType.MULTIPART_FORM_DATA, null, null, HttpStatus.OK);

    part = verifyMultipartResponse(configOutput, MediaType.TEXT_PLAIN,
	ListUtil.list("a5=b5"));

    // Verify the part last modification timestamps.
    verifyPartModificationTimestamps(part, null);
    assertTrue(beforeWrite <= Long.parseLong(part.getLastModified()));

    // The file exists.
    ifMatch = ListUtil.list(ASTERISK_PRECONDITION);
    hrp = new HttpRequestPreconditions(ifMatch, null, null, null);
    beforeWrite = TimeBase.nowMs();

    runTestPutConfig("a6=b6", SECTION_NAME_PLUGIN,
	MediaType.MULTIPART_FORM_DATA, hrp, ANYBODY, HttpStatus.OK);

    configOutput = runTestGetConfigSection(SECTION_NAME_PLUGIN,
	MediaType.MULTIPART_FORM_DATA, null, ANYBODY, HttpStatus.OK);

    part = verifyMultipartResponse(configOutput, MediaType.TEXT_PLAIN,
	ListUtil.list("a6=b6"));

    // Verify the part last modification timestamps.
    verifyPartModificationTimestamps(part, null);
    assertTrue(beforeWrite <= Long.parseLong(part.getLastModified()));

    // The file exists.
    ifNoneMatch = ListUtil.list(ASTERISK_PRECONDITION);
    hrp = new HttpRequestPreconditions(null, null, ifNoneMatch, null);

    runTestPutConfig("a7=b7", SECTION_NAME_PLUGIN,
	MediaType.MULTIPART_FORM_DATA, hrp, CONTENT_ADMIN,
	HttpStatus.PRECONDITION_FAILED);

    putConfigCommonTest();

    log.debug2("Done");
  }

  /**
   * Runs the putConfig()-related authenticated-specific tests.
   */
  private void putConfigAuthenticatedTest() throws Exception {
    log.debug2("Invoked");

    // No section.
    runTestPutConfig(null, null, null, null, null, HttpStatus.UNAUTHORIZED);

    runTestPutConfig(null, null, null, null, CONTENT_ADMIN,
	HttpStatus.NOT_FOUND);

    // Empty section.
    runTestPutConfig(null, EMPTY_STRING, null, null, null,
	HttpStatus.UNAUTHORIZED);

    runTestPutConfig(null, EMPTY_STRING, null, null, CONTENT_ADMIN,
	HttpStatus.NOT_FOUND);

    HttpRequestPreconditions hrp =
	new HttpRequestPreconditions(null, null, null, null);

    runTestPutConfig(null, EMPTY_STRING, null, hrp, null,
	HttpStatus.UNAUTHORIZED);

    runTestPutConfig(null, EMPTY_STRING, null, hrp, CONTENT_ADMIN,
	HttpStatus.NOT_FOUND);

    // Missing credentials.
    hrp = new HttpRequestPreconditions(EMPTY_PRECONDITION_LIST, EMPTY_STRING,
	EMPTY_PRECONDITION_LIST, EMPTY_STRING);

    runTestPutConfig(null, SECTION_NAME_PLUGIN, null, hrp, null,
	HttpStatus.UNAUTHORIZED);

    // Bad credentials.
    List<String> IfMatchNoMatch = ListUtil.list(EMPTY_PRECONDITION);
    hrp = new HttpRequestPreconditions(IfMatchNoMatch, EMPTY_STRING, null,
	EMPTY_STRING);

    runTestPutConfig(null, SECTION_NAME_PLUGIN, null, hrp, ANYBODY,
	HttpStatus.UNAUTHORIZED);

    // Unauthorized credentials.
    hrp = new HttpRequestPreconditions(null, EMPTY_STRING, IfMatchNoMatch,
	EMPTY_STRING);

    runTestPutConfig(null, SECTION_NAME_PLUGIN,
	MediaType.APPLICATION_JSON, hrp, CONTENT_ADMIN,
	HttpStatus.UNSUPPORTED_MEDIA_TYPE);

    // Missing credentials.
    runTestPutConfig(null, SECTION_NAME_PLUGIN,
	MediaType.APPLICATION_JSON, null, null, HttpStatus.UNAUTHORIZED);

    // Bad credentials.
    runTestPutConfig(null, SECTION_NAME_PLUGIN,
	MediaType.MULTIPART_FORM_DATA, null, ANYBODY, HttpStatus.UNAUTHORIZED);

    // Unauthorized credentials.
    runTestPutConfig(null, SECTION_NAME_PLUGIN,
	MediaType.MULTIPART_FORM_DATA, null, CONTENT_ADMIN,
	HttpStatus.INTERNAL_SERVER_ERROR);

    // Missing credentials.
    runTestPutConfig("a=b", SECTION_NAME_PLUGIN, null, null, null,
	HttpStatus.UNAUTHORIZED);

    // Bad credentials.
    runTestPutConfig("a=b", SECTION_NAME_PLUGIN, null, null, ANYBODY,
	HttpStatus.UNAUTHORIZED);

    // Unauthorized credentials.
    runTestPutConfig("a=b", SECTION_NAME_PLUGIN, null, null,
	CONTENT_ADMIN, HttpStatus.FORBIDDEN);

    // Bad Content-Type header.
    try {
      runTestPutConfig("a=b", SECTION_NAME_PLUGIN,
	  MediaType.APPLICATION_JSON, null, null, HttpStatus.UNAUTHORIZED);
      fail("Should have thrown HttpMessageConversionException");
    } catch (HttpMessageConversionException e) {
      // FIXME
      assertMatchesRE("Type definition error", e.getMessage());
    }

    // Bad Content-Type header.
    try {
      runTestPutConfig("a=b", SECTION_NAME_PLUGIN,
	  MediaType.APPLICATION_JSON, null, ANYBODY, HttpStatus.UNAUTHORIZED);
      fail("Should have thrown HttpMessageConversionException");
    } catch (HttpMessageConversionException e) {
      // FIXME
      assertMatchesRE("Type definition error", e.getMessage());
    }

    // Bad Content-Type header.
    try {
      runTestPutConfig("a=b", SECTION_NAME_PLUGIN,
	  MediaType.APPLICATION_JSON, null, CONTENT_ADMIN,
	  HttpStatus.UNAUTHORIZED);
      fail("Should have thrown HttpMessageConversionException");
    } catch (HttpMessageConversionException e) {
      // FIXME
      assertMatchesRE("Type definition error", e.getMessage());
    }

    // Missing credentials.
    runTestPutConfig("a=b", SECTION_NAME_PLUGIN,
	MediaType.MULTIPART_FORM_DATA, null, null, HttpStatus.UNAUTHORIZED);

    // Bad credentials.
    runTestPutConfig("a=b", SECTION_NAME_PLUGIN,
	MediaType.MULTIPART_FORM_DATA, null, ANYBODY, HttpStatus.UNAUTHORIZED);

    // Unauthorized credentials.
    runTestPutConfig("a=b", SECTION_NAME_PLUGIN,
	MediaType.MULTIPART_FORM_DATA, null, CONTENT_ADMIN,
	HttpStatus.FORBIDDEN);

    putConfigCommonTest();

    log.debug2("Done");
  }

  /**
   * Runs the putConfig()-related authentication-independent tests.
   */
  private void putConfigCommonTest() throws Exception {
    log.debug2("Invoked");

    // No section: Spring reports it cannot find a match to an endpoint.
    runTestPutConfig(null, null, null, null, USER_ADMIN, HttpStatus.NOT_FOUND);

    HttpRequestPreconditions hrp =
	new HttpRequestPreconditions(null, null, null, null);

    runTestPutConfig(null, null, null, hrp, USER_ADMIN, HttpStatus.NOT_FOUND);

    // No section name using the REST service client.
    try {
      runTestPutConfigSectionClient("testKey=testValue", null, null, null, null,
	  null);
      fail("Should have thrown IllegalArgumentException");
    } catch (IllegalArgumentException iae) {
      assertEquals("Invalid section name 'null'", iae.getMessage());
    }

    try {
      hrp = new HttpRequestPreconditions(EMPTY_PRECONDITION_LIST, EMPTY_STRING,
	  EMPTY_PRECONDITION_LIST, EMPTY_STRING);

      runTestPutConfigSectionClient("testKey=testValue", null, hrp, null, null,
	  null);
      fail("Should have thrown IllegalArgumentException");
    } catch (IllegalArgumentException iae) {
      assertEquals("Invalid section name 'null'", iae.getMessage());
    }

    // Empty section: Spring reports it cannot find a match to an endpoint.
    List<String> IfMatchNoMatch = ListUtil.list(EMPTY_PRECONDITION);
    hrp = new HttpRequestPreconditions(IfMatchNoMatch, EMPTY_STRING, null,
	EMPTY_STRING);

    runTestPutConfig(null, EMPTY_STRING, null, hrp, USER_ADMIN,
	HttpStatus.NOT_FOUND);

    // Empty section name using the REST service client.
    try {
      hrp = new HttpRequestPreconditions(null, EMPTY_STRING, IfMatchNoMatch,
	  EMPTY_STRING);

      runTestPutConfigSectionClient("testKey=testValue", EMPTY_STRING, hrp,
	  null, null, null);
      fail("Should have thrown IllegalArgumentException");
    } catch (IllegalArgumentException iae) {
      assertEquals("Invalid section name ''", iae.getMessage());
    }

    // Missing Content-Type header.
    runTestPutConfig(null, SECTION_NAME_EXPERT, null, null,
	USER_ADMIN, HttpStatus.UNSUPPORTED_MEDIA_TYPE);

    // Bad Content-Type header.
    runTestPutConfig(null, SECTION_NAME_EXPERT,
	MediaType.APPLICATION_JSON, null, USER_ADMIN,
	HttpStatus.UNSUPPORTED_MEDIA_TYPE);

    // Missing payload (This should return HttpStatus.BAD_REQUEST, but Spring
    // returns HttpStatus.INTERNAL_SERVER_ERROR).
    runTestPutConfig(null, SECTION_NAME_EXPERT,
	MediaType.MULTIPART_FORM_DATA, null, USER_ADMIN,
	HttpStatus.INTERNAL_SERVER_ERROR);

    // Missing payload using the REST service client.
    try {
      List<String> ifMatch = ListUtil.list(NUMERIC_PRECONDITION);
      hrp =
	  new HttpRequestPreconditions(ifMatch, null, null, null);
      runTestPutConfigSectionClient(null, SECTION_NAME_EXPERT, hrp,
	  null, null, "Configuration input stream is null");
      fail("Should have thrown IllegalArgumentException");
    } catch (IllegalArgumentException iae) {}

    // Success.
    long beforeWrite = TimeBase.nowMs();

    runTestPutConfig("testKey=testValue", SECTION_NAME_EXPERT,
	MediaType.MULTIPART_FORM_DATA, null, USER_ADMIN, HttpStatus.OK);

    MultipartResponse configOutput = runTestGetConfigSection(
	SECTION_NAME_EXPERT, MediaType.MULTIPART_FORM_DATA, null,
	USER_ADMIN, HttpStatus.OK);

    Part part = verifyMultipartResponse(configOutput, MediaType.TEXT_PLAIN,
	ListUtil.list("testKey=testValue"));

    // Verify the part last modification timestamps.
    verifyPartModificationTimestamps(part, null);
    assertTrue(beforeWrite <= Long.parseLong(part.getLastModified()));

    // Using the REST service client.
    beforeWrite = TimeBase.nowMs();

    RestConfigSection output = runTestPutConfigSectionClient(
	"testKey1=testValue1", SECTION_NAME_EXPERT, null, USER_ADMIN,
	HttpStatus.OK, null);

    // Verify the REST web service configuration section last modification
    // timestamps.
    verifyRestConfigSectionModificationTimestamps(output, null);
    assertTrue(beforeWrite <= Long.parseLong(output.getLastModified()));

    // Read the file.
    configOutput = runTestGetConfigSection(SECTION_NAME_EXPERT,
	MediaType.MULTIPART_FORM_DATA, null, USER_ADMIN, HttpStatus.OK);

    part = verifyMultipartResponse(configOutput, MediaType.TEXT_PLAIN,
	ListUtil.list("testKey1=testValue1"));

    // Verify the part last modification timestamps.
    verifyPartModificationTimestamps(part, null);
    assertTrue(beforeWrite <= Long.parseLong(part.getLastModified()));
    assertEquals(output.getLastModified(), part.getLastModified());
    assertEquals(output.getEtag(), part.getEtag());

    // Modified at different time than passed If-Match ETag.
    List<String> ifMatch = ListUtil.list(NUMERIC_PRECONDITION);
    hrp = new HttpRequestPreconditions(ifMatch, null, null, null);

    runTestPutConfig("testKey2=testValue2", SECTION_NAME_EXPERT,
	MediaType.MULTIPART_FORM_DATA, hrp, USER_ADMIN,
	HttpStatus.PRECONDITION_FAILED);

    // Using the REST service client.
    runTestPutConfigSectionClient("testKey2=testValue2",
	SECTION_NAME_EXPERT, hrp, USER_ADMIN,
	HttpStatus.PRECONDITION_FAILED,
	HttpStatus.PRECONDITION_FAILED.getReasonPhrase());

    // Modified at different time than passed If-Match ETags.
    ifMatch = ListUtil.list(NUMERIC_PRECONDITION, ALPHA_PRECONDITION);
    hrp = new HttpRequestPreconditions(ifMatch, null, null, null);

    runTestPutConfig("testKey2=testValue2", SECTION_NAME_EXPERT,
	MediaType.MULTIPART_FORM_DATA, hrp, USER_ADMIN,
	HttpStatus.PRECONDITION_FAILED);

    // Using the REST service client.
    runTestPutConfigSectionClient("testKey2=testValue2",
	SECTION_NAME_EXPERT, hrp, USER_ADMIN,
	HttpStatus.PRECONDITION_FAILED,
	HttpStatus.PRECONDITION_FAILED.getReasonPhrase());

    // Modified at different time than passed If-Match ETag.
    ifMatch = ListUtil.list(ZERO_PRECONDITION);
    hrp = new HttpRequestPreconditions(ifMatch, null, null, null);

    runTestPutConfig("testKey2=testValue2", SECTION_NAME_EXPERT,
	MediaType.MULTIPART_FORM_DATA, hrp, USER_ADMIN,
	HttpStatus.PRECONDITION_FAILED);

    // Using the REST service client.
    runTestPutConfigSectionClient("testKey2=testValue2",
	SECTION_NAME_EXPERT, hrp, USER_ADMIN,
	HttpStatus.PRECONDITION_FAILED,
	HttpStatus.PRECONDITION_FAILED.getReasonPhrase());

    // Modified at different time than passed If-Match ETag and same time as
    // passed If-Unmodified-Since header.
    ifMatch = ListUtil.list(ZERO_PRECONDITION);
    hrp = new HttpRequestPreconditions(ifMatch, null, null,
	part.getLastModified());

    runTestPutConfig("testKey2=testValue2", SECTION_NAME_EXPERT,
	MediaType.MULTIPART_FORM_DATA, hrp, USER_ADMIN,
	HttpStatus.PRECONDITION_FAILED);

    // Using the REST service client.
    runTestPutConfigSectionClient("testKey2=testValue2",
	SECTION_NAME_EXPERT, hrp, USER_ADMIN,
	HttpStatus.PRECONDITION_FAILED,
	HttpStatus.PRECONDITION_FAILED.getReasonPhrase());

    // Bad Accept header content type.
    List<String> ifNoneMatch = ListUtil.list(ZERO_PRECONDITION);
    hrp = new HttpRequestPreconditions(null, null, ifNoneMatch, null);
    runTestGetConfigSection(SECTION_NAME_EXPERT, null, hrp,
	USER_ADMIN, HttpStatus.NOT_ACCEPTABLE);

    // Verify that nothing was written when the preconditions failed.
    configOutput = runTestGetConfigSection(SECTION_NAME_EXPERT,
	MediaType.MULTIPART_FORM_DATA, hrp, USER_ADMIN, HttpStatus.OK);

    Part part2 = verifyMultipartResponse(configOutput, MediaType.TEXT_PLAIN,
	ListUtil.list("testKey1=testValue1"));

    // Verify the part last modification timestamps.
    verifyPartModificationTimestamps(part2, part);

    // Independent verification.
    String filePath =
	getTempDirPath() + "/cache/config/" + ConfigManager.CONFIG_FILE_EXPERT_CLUSTER;

    assertEquals(0, StringUtil.fromInputStream(ConfigManager.getConfigManager()
        .conditionallyReadCacheConfigFile(filePath, null)
        .getInputStream()).indexOf("testKey1=testValue1"));

    // Modified at different time than passed If-Unmodified-Since header.
    hrp = new HttpRequestPreconditions(null, null, null, NUMBER);

    runTestPutConfig("testKey1=testValue1\ntestKey2=testValue2",
	SECTION_NAME_EXPERT, MediaType.MULTIPART_FORM_DATA, hrp,
	USER_ADMIN, HttpStatus.PRECONDITION_FAILED);

    // Using the REST service client.
    runTestPutConfigSectionClient("testKey1=testValue1\ntestKey2=testValue2",
	SECTION_NAME_EXPERT, hrp, USER_ADMIN,
	HttpStatus.PRECONDITION_FAILED,
	HttpStatus.PRECONDITION_FAILED.getReasonPhrase());

    // Modified at same time as passed If-Match ETag and different time than
    // passed If-Unmodified-Since header.
    ifMatch = ListUtil.list(part2.getEtag());
    hrp = new HttpRequestPreconditions(ifMatch, null, null, NUMBER);
    runTestPutConfig("testKey1=testValue1\ntestKey2=testValue2",
	SECTION_NAME_EXPERT, MediaType.MULTIPART_FORM_DATA, hrp,
	USER_ADMIN, HttpStatus.PRECONDITION_FAILED);

    // Using the REST service client.
    runTestPutConfigSectionClient("testKey1=testValue1\ntestKey2=testValue2",
	SECTION_NAME_EXPERT, hrp, USER_ADMIN,
	HttpStatus.PRECONDITION_FAILED,
	HttpStatus.PRECONDITION_FAILED.getReasonPhrase());

    // Verify that nothing was written when the preconditions failed.
    hrp = new HttpRequestPreconditions(ifMatch, null, null,
	part.getLastModified());

    configOutput = runTestGetConfigSection(SECTION_NAME_EXPERT,
	MediaType.MULTIPART_FORM_DATA, hrp, USER_ADMIN, HttpStatus.OK);

    part2 = verifyMultipartResponse(configOutput, MediaType.TEXT_PLAIN,
	ListUtil.list("testKey1=testValue1"));

    // Verify the part last modification timestamps.
    verifyPartModificationTimestamps(part2, part);

    // Modified at same time as passed If-Match ETag.
    ifMatch = ListUtil.list(part2.getEtag());
    hrp = new HttpRequestPreconditions(ifMatch, null, null, null);
    beforeWrite = TimeBase.nowMs();

    runTestPutConfig("testKey1=testValue1\ntestKey2=testValue2",
	SECTION_NAME_EXPERT, MediaType.MULTIPART_FORM_DATA, hrp,
	USER_ADMIN, HttpStatus.OK);

    // Read file with the old ETag.
    ifNoneMatch = ListUtil.list(part2.getEtag());
    hrp = new HttpRequestPreconditions(null, null, ifNoneMatch, null);

    configOutput = runTestGetConfigSection(SECTION_NAME_EXPERT,
	MediaType.MULTIPART_FORM_DATA, hrp, USER_ADMIN, HttpStatus.OK);

    part = verifyMultipartResponse(configOutput, MediaType.TEXT_PLAIN,
	ListUtil.list("testKey1=testValue1\ntestKey2=testValue2"));

    // Verify the part last modification timestamps.
    verifyPartModificationTimestamps(part, null);
    assertTrue(beforeWrite <= Long.parseLong(part.getLastModified()));

    // Independent verification.
    assertEquals(0, StringUtil.fromInputStream(ConfigManager.getConfigManager()
            .conditionallyReadCacheConfigFile(filePath, null).getInputStream())
        .indexOf("testKey1=testValue1\ntestKey2=testValue2"));

    // Modified at same time as passed If-Match ETag and same time as passed
    // If-Unmodified-Since header using the REST service client..
    ifMatch = ListUtil.list(part.getEtag());
    hrp = new HttpRequestPreconditions(ifMatch, null, null,
	part.getLastModified());
    beforeWrite = TimeBase.nowMs();

    output = runTestPutConfigSectionClient(
	"testKey2=testValue2\ntestKey3=testValue3",
	SECTION_NAME_EXPERT, hrp, USER_ADMIN, HttpStatus.OK, null);

    // Verify the REST web service configuration section last modification
    // timestamps.
    verifyRestConfigSectionModificationTimestamps(output, null);
    assertTrue(beforeWrite <= Long.parseLong(output.getLastModified()));

    // Read file using the REST service client.
    RestConfigSection input = new RestConfigSection();
    input.setSectionName(SECTION_NAME_EXPERT);

    RestConfigClient restConfigClient = getRestConfigClient(USER_ADMIN);
    RestConfigSection output2 = restConfigClient.getConfigSection(input);
    assertEquals(HttpStatus.OK, output2.getStatus());
    assertEquals("testKey2=testValue2\ntestKey3=testValue3",
	StringUtil.fromInputStream(output2.getInputStream()));

    // Verify the part last modification timestamps.
    verifyRestConfigSectionModificationTimestamps(output2, output);
    assertTrue(beforeWrite <= Long.parseLong(output2.getLastModified()));

    // Independent verification.
    assertEquals(0, StringUtil.fromInputStream(ConfigManager.getConfigManager()
            .conditionallyReadCacheConfigFile(filePath, null).getInputStream())
        .indexOf("testKey2=testValue2\ntestKey3=testValue3"));

    // Write file with matching timestamp using the REST service client.
    HttpRequestPreconditions preconditions = new HttpRequestPreconditions();
    preconditions.setIfMatch(ListUtil.list(output2.getEtag(),
	ZERO_PRECONDITION));
    output2.setHttpRequestPreconditions(preconditions);

    String content = "testKey3=testValue3";
    output2.setInputStream(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
    output2.setContentLength(content.length());
    beforeWrite = TimeBase.nowMs();

    RestConfigSection output3 = restConfigClient.putConfigSection(output2);
    assertEquals(HttpStatus.OK, output3.getStatus());

    // Verify the part last modification timestamps.
    verifyRestConfigSectionModificationTimestamps(output3, null);
    assertTrue(beforeWrite <= Long.parseLong(output3.getLastModified()));

    // Independent verification.
    assertEquals(0, StringUtil.fromInputStream(ConfigManager.getConfigManager()
            .conditionallyReadCacheConfigFile(filePath, null).getInputStream())
        .indexOf("testKey3=testValue3"));

    // Cannot modify virtual sections.
    ifMatch = ListUtil.list(ZERO_PRECONDITION);
    hrp = new HttpRequestPreconditions(ifMatch, null, null, null);

    runTestPutConfig("testKey=testValue", SECTION_NAME_CLUSTER,
	MediaType.MULTIPART_FORM_DATA, hrp, USER_ADMIN, HttpStatus.BAD_REQUEST);

    // Using the REST service client.
    runTestPutConfigSectionClient("testKey=testValue",
	SECTION_NAME_CLUSTER, hrp, USER_ADMIN, HttpStatus.BAD_REQUEST,
	HttpStatus.BAD_REQUEST.getReasonPhrase());

    // Bad section name.
    runTestPutConfig("testKey=testValue", BAD_SN, MediaType.MULTIPART_FORM_DATA,
	hrp, USER_ADMIN, HttpStatus.BAD_REQUEST);

    // Using the REST service client.
    runTestPutConfigSectionClient("testKey=testValue", BAD_SN, hrp, USER_ADMIN,
	HttpStatus.BAD_REQUEST, HttpStatus.BAD_REQUEST.getReasonPhrase());

    // Missing file.
    ifMatch = ListUtil.list(ASTERISK_PRECONDITION);
    hrp = new HttpRequestPreconditions(ifMatch, null, null, null);

    runTestPutConfig("testKey=testValue", SECTION_NAME_ICP_SERVER,
	MediaType.MULTIPART_FORM_DATA, hrp, USER_ADMIN,
	HttpStatus.PRECONDITION_FAILED);

    // Using the REST service client.
    runTestPutConfigSectionClient("testKey=testValue",
	SECTION_NAME_ICP_SERVER, hrp, USER_ADMIN,
	HttpStatus.PRECONDITION_FAILED,
	HttpStatus.PRECONDITION_FAILED.getReasonPhrase());

    // Write non-existent file.
    ifNoneMatch = ListUtil.list(ASTERISK_PRECONDITION);
    hrp = new HttpRequestPreconditions(null, null, ifNoneMatch, null);
    beforeWrite = TimeBase.nowMs();

    runTestPutConfig("testKey4=testValue4", SECTION_NAME_ICP_SERVER,
	MediaType.MULTIPART_FORM_DATA, hrp, USER_ADMIN, HttpStatus.OK);

    // Read file.
    configOutput = runTestGetConfigSection(SECTION_NAME_ICP_SERVER,
	MediaType.MULTIPART_FORM_DATA, null, USER_ADMIN, HttpStatus.OK);

    part = verifyMultipartResponse(configOutput, MediaType.TEXT_PLAIN,
	ListUtil.list("testKey4=testValue4"));

    // Verify the part last modification timestamps.
    verifyPartModificationTimestamps(part, null);
    assertTrue(beforeWrite <= Long.parseLong(part.getLastModified()));

    // Write again the now-existing file.
    runTestPutConfig("testKey4a=testValue4a", SECTION_NAME_ICP_SERVER,
	MediaType.MULTIPART_FORM_DATA, hrp, USER_ADMIN,
	HttpStatus.PRECONDITION_FAILED);

    // Write non-existent file using the REST service client.
    beforeWrite = TimeBase.nowMs();

    output3 = runTestPutConfigSectionClient(
	"testKey5=testValue5", SECTION_NAME_ACCESS_GROUPS, hrp,
	USER_ADMIN, HttpStatus.OK, null);
    assertEquals(HttpStatus.OK, output3.getStatus());

    // Verify the part last modification timestamps.
    verifyRestConfigSectionModificationTimestamps(output3, null);
    assertTrue(beforeWrite <= Long.parseLong(output3.getLastModified()));

    // Read file with matching timestamp using the REST service client.
    preconditions = new HttpRequestPreconditions();
    preconditions.setIfNoneMatch(ListUtil.list(output3.getEtag(),
	NUMERIC_PRECONDITION));
    output3.setHttpRequestPreconditions(preconditions);

    RestConfigSection output4 = restConfigClient.getConfigSection(output3);
    assertEquals(HttpStatus.NOT_MODIFIED, output4.getStatus());

    // Read file with non-matching timestamp using the REST service client.
    preconditions.setIfNoneMatch(ListUtil.list(
	ALPHA_PRECONDITION, NUMERIC_PRECONDITION));

    output4 = restConfigClient.getConfigSection(output3);
    assertEquals(HttpStatus.OK, output4.getStatus());

    // Verify the part last modification timestamps.
    verifyRestConfigSectionModificationTimestamps(output4, null);
    assertTrue(beforeWrite <= Long.parseLong(output4.getLastModified()));

    part = verifyMultipartResponse(output4.getResponse(), MediaType.TEXT_PLAIN,
	ListUtil.list("testKey5=testValue5"));

    // Verify the part last modification timestamps.
    verifyPartModificationTimestamps(part, null);
    assertTrue(beforeWrite <= Long.parseLong(part.getLastModified()));

    // Write again the now-existing file using the REST service client.
    runTestPutConfigSectionClient("testKey5a=testValue5a",
	SECTION_NAME_ACCESS_GROUPS, hrp, USER_ADMIN,
	HttpStatus.PRECONDITION_FAILED,
	HttpStatus.PRECONDITION_FAILED.getReasonPhrase());

    // Cannot modify virtual sections.
    ifMatch = ListUtil.list(ASTERISK_PRECONDITION);
    hrp = new HttpRequestPreconditions(ifMatch, null, null, null);
    runTestPutConfig("testKey=testValue", SECTION_NAME_CLUSTER,
	MediaType.MULTIPART_FORM_DATA, hrp, USER_ADMIN, HttpStatus.BAD_REQUEST);

    // Using the REST service client.
    runTestPutConfigSectionClient("testKey=testValue",
	SECTION_NAME_CLUSTER, hrp, USER_ADMIN, HttpStatus.BAD_REQUEST,
	HttpStatus.BAD_REQUEST.getReasonPhrase());

    log.debug2("Done");
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
   * @param preconditions
   *          An HttpRequestPreconditions with the request preconditions to be
   *          met.
   * @param credentials
   *          A Credentials with the request credentials.
   * @param expectedStatus
   *          An HttpStatus with the HTTP status of the result.
   */
  private void runTestPutConfig(String config, String snId,
      MediaType contentType, HttpRequestPreconditions preconditions,
      Credentials credentials, HttpStatus expectedStatus) {
    log.debug2("config = {}", config);
    log.debug2("snId = {}", snId);
    log.debug2("contentType = {}", contentType);
    log.debug2("preconditions = {}", preconditions);
    log.debug2("credentials = {}", credentials);
    log.debug2("expectedStatus = {}", expectedStatus);

    // Get the test URL template.
    String template = getTestUrlTemplate("/config/file/{snid}");

    // Create the URI of the request to the REST service.
    UriComponents uriComponents = UriComponentsBuilder.fromUriString(template)
	.build().expand(Collections.singletonMap("snid", snId));

    URI uri = UriComponentsBuilder.newInstance().uriComponents(uriComponents)
	.build().encode().toUri();
    log.trace("uri = {}", uri);

    // Initialize the request to the REST service.
    RestTemplateBuilder templateBuilder = RestUtil.getRestTemplateBuilder(0, 0);

    HttpEntity<MultiValueMap<String, Object>> requestEntity = null;

    // Get the individual preconditions.
    List<String> ifMatch = null;
    String ifModifiedSince = null;
    List<String> ifNoneMatch = null;
    String ifUnmodifiedSince = null;

    if (preconditions != null) {
      ifMatch = preconditions.getIfMatch();
      ifModifiedSince = preconditions.getIfModifiedSince();
      ifNoneMatch = preconditions.getIfNoneMatch();
      ifUnmodifiedSince = preconditions.getIfUnmodifiedSince();
    }

    // Get the individual credentials elements.
    String user = null;
    String password = null;

    if (credentials != null) {
      user = credentials.getUser();
      password = credentials.getPassword();
    }

    // Check whether there are any custom headers to be specified in the
    // request.
    if (config != null || contentType != null
	|| (ifMatch != null && !ifMatch.isEmpty())
	|| (ifModifiedSince != null && !ifModifiedSince.isEmpty())
	|| (ifNoneMatch != null && !ifNoneMatch.isEmpty())
	|| (ifUnmodifiedSince != null && !ifUnmodifiedSince.isEmpty())
	|| user != null || password != null) {
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
      headers.setAccept(List.of(MediaType.APPLICATION_JSON));

      // Check whether there is a custom If-Match header.
      if (ifMatch != null) {
	// Yes: Set the If-Match header.
	headers.setIfMatch(ifMatch);
      }

      // Check whether there is a custom If-Modified-Since header.
      if (ifModifiedSince != null) {
	// Yes: Set the If-Modified-Since header.
	headers.set(HttpHeaders.IF_MODIFIED_SINCE, ifModifiedSince);
      }

      // Check whether there is a custom If-None-Match header.
      if (ifNoneMatch != null) {
	// Yes: Set the If-None-Match header.
	headers.setIfNoneMatch(ifNoneMatch);
      }

      // Check whether there is a custom If-Unmodified-Since header.
      if (ifUnmodifiedSince != null) {
	// Yes: Set the If-Unmodified-Since header.
	headers.set(HttpHeaders.IF_UNMODIFIED_SINCE, ifUnmodifiedSince);
      }

      // Set up the authentication credentials, if necessary.
      if (credentials != null) {
	credentials.setUpBasicAuthentication(headers);
      }

      log.trace("requestHeaders = {}", () -> headers.toSingleValueMap());

      // Create the request entity.
      requestEntity =
	  new HttpEntity<MultiValueMap<String, Object>>(parts, headers);
    }

    // Make the request and get the response. 
    ResponseEntity<?> response = new TestRestTemplate(templateBuilder)
	.exchange(uri, HttpMethod.PUT, requestEntity, Void.class);

    // Get the response status.
    HttpStatusCode statusCode = response.getStatusCode();
    HttpStatus status = HttpStatus.valueOf(statusCode.value());
    assertEquals(expectedStatus, status);

    log.debug2("Done");
  }

  /**
   * Performs a PUT operation for a configuration section using the REST service
   * client.
   * 
   * @param config
   *          A String with the configuration to be written.
   * @param snId
   *          A String with the configuration section name.
   * @param preconditions
   *          An HttpRequestPreconditions with the request preconditions to be
   *          met.
   * @param credentials
   *          A Credentials with the request credentials.
   * @param expectedStatus
   *          An HttpStatus with the HTTP status of the result.
   * @param expectedErrorMessagePrefix
   *          A String with the beginning of the error message.
   * @return a RestConfigSection with the result of the operation.
   * @throws Exception
   *           if there are problems.
   */
  private RestConfigSection runTestPutConfigSectionClient(String config,
      String snId, HttpRequestPreconditions preconditions,
      Credentials credentials, HttpStatus expectedStatus,
      String expectedErrorMessagePrefix) throws Exception {
    log.debug2("config = {}", config);
    log.debug2("snId = {}", snId);
    log.debug2("preconditions = {}", preconditions);
    log.debug2("credentials = {}", credentials);
    log.debug2("expectedStatus = {}", expectedStatus);
    log.debug2("expectedErrorMessagePrefix = {}", expectedErrorMessagePrefix);

    RestConfigSection input = new RestConfigSection();
    input.setSectionName(snId);
    input.setHttpRequestPreconditions(preconditions);

    if (config != null) {
      input.setInputStream(
	  new ByteArrayInputStream(config.getBytes(StandardCharsets.UTF_8)));
      input.setContentLength(config.length());
    }

    input.setContentType(MediaType.TEXT_PLAIN_VALUE);

    // Make the request and get the result;
    RestConfigSection output =
	getRestConfigClient(credentials).putConfigSection(input);

    // Check the response status.
    assertEquals(expectedStatus, output.getStatus());

    // Check the error message.
    String errorMessage = output.getErrorMessage();

    if (expectedErrorMessagePrefix == null) {
      assertNull(errorMessage);
    } else {
      assertNotNull(errorMessage);
      assertTrue(errorMessage.startsWith(expectedErrorMessagePrefix));
    }

    // Check the response etag.
    if (preconditions != null && preconditions.getIfMatch() != null
	&& !ASTERISK_PRECONDITION.equals(preconditions.getIfMatch().get(0))) {
      assertFalse(preconditions.getIfMatch().contains(output.getEtag()));
    }

    log.debug2("output = {}", output);
    return output;
  }

  /**
   * Sanity check on the response REST web service configuration section
   * modification timestamps.
   * 
   * @param rcs
   *          A RestConfigSection with the response REST web service
   *          configuration section to be checked.
   * @param matchingRcs
   *          A RestConfigSection with another optional response REST web
   *          service configuration section that is expected to have matching
   *          timestamps.
   */
  private void verifyRestConfigSectionModificationTimestamps(
      RestConfigSection rcs, RestConfigSection matchingRcs) {
    log.debug2("rcs = {}", rcs);
    log.debug2("matchingRcs = {}", matchingRcs);

    // Verify the REST web service configuration section last modified header.
    long lastModified = Long.parseLong(rcs.getLastModified());
    assertTrue(lastModified <= TimeBase.nowMs());

    // Verify the REST web service configuration section ETag.
    String etag = rcs.getEtag();
    long etagContents = Long.parseLong(parseEtag(etag));
    assertTrue(etagContents >= lastModified);
    assertTrue(etagContents < lastModified + 1000L);

    if (matchingRcs != null) {
      assertEquals(Long.parseLong(matchingRcs.getLastModified()), lastModified);
      assertEquals(matchingRcs.getEtag(), etag);
    }

    log.debug2("Done");
  }

  /**
   * Runs the putConfigReload()-related un-authenticated-specific tests.
   */
  private void putConfigReloadUnAuthenticatedTest() {
    log.debug2("Invoked");

    runTestPutConfigReload(null, HttpStatus.OK);
    runTestPutConfigReload(ANYBODY, HttpStatus.OK);
    runTestPutConfigReload(CONTENT_ADMIN, HttpStatus.OK);

    putConfigReloadCommonTest();

    log.debug2("Done");
  }

  /**
   * Runs the putConfigReload()-related authenticated-specific tests.
   */
  private void putConfigReloadAuthenticatedTest() {
    log.debug2("Invoked");

    runTestPutConfigReload(null, HttpStatus.UNAUTHORIZED);
    runTestPutConfigReload(ANYBODY, HttpStatus.UNAUTHORIZED);
    runTestPutConfigReload(CONTENT_ADMIN, HttpStatus.FORBIDDEN);

    putConfigReloadCommonTest();

    log.debug2("Done");
  }

  /**
   * Runs the putConfigReload()-related authentication-independent tests.
   */
  private void putConfigReloadCommonTest() {
    log.debug2("Invoked");

    runTestPutConfigReload(USER_ADMIN, HttpStatus.OK);

    log.debug2("Done");
  }

  /**
   * Performs a PUT config reload operation.
   * 
   * @param credentials
   *          A Credentials with the request credentials.
   * @param expectedStatus
   *          An HttpStatus with the HTTP status of the result.
   */
  private void runTestPutConfigReload(Credentials credentials,
      HttpStatus expectedStatus) {
    log.debug2("credentials = {}", credentials);
    log.debug2("expectedStatus = {}", expectedStatus);

    // Get the test URL template.
    String template = getTestUrlTemplate("/config/reload");

    // Create the URI of the request to the REST service.
    UriComponents uriComponents =
	UriComponentsBuilder.fromUriString(template).build();

    URI uri = UriComponentsBuilder.newInstance().uriComponents(uriComponents)
	.build().encode().toUri();
    log.trace("uri = {}", uri);

    // Initialize the request to the REST service.
    RestTemplateBuilder templateBuilder = RestUtil.getRestTemplateBuilder(0, 0);

    HttpEntity<String> requestEntity = null;

    // Check whether there are any custom headers to be specified in the
    // request.
    if (credentials != null) {
      // Yes: Initialize the request headers.
      HttpHeaders headers = new HttpHeaders();

      // Set up the authentication credentials, if necessary.
      credentials.setUpBasicAuthentication(headers);

      log.trace("requestHeaders = {}", () -> headers.toSingleValueMap());

      // Create the request entity.
      requestEntity = new HttpEntity<String>(null, headers);
    }

    ConfigManager configManager = ConfigManager.getConfigManager();

    // Get the count of configuration reloading requests before this one.
    int configReloadRequestCounter =
	configManager.getConfigReloadRequestCounter();

    // Make the request and get the response. 
    ResponseEntity<?> response = new TestRestTemplate(templateBuilder)
	.exchange(uri, HttpMethod.PUT, requestEntity, Void.class);

    // Get the response status.
    HttpStatusCode statusCode = response.getStatusCode();
    HttpStatus status = HttpStatus.valueOf(statusCode.value());
    assertEquals(expectedStatus, status);

    // Check whether it is a success response.
    if (isSuccess(status)) {
      // Yes: The count of configuration reloading requests should have been
      // increased by 1.
      assertEquals(configReloadRequestCounter + 1,
	  configManager.getConfigReloadRequestCounter());
    }

    log.debug2("Done");
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
