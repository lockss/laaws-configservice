/*

Copyright (c) 2000-2022 Board of Trustees of Leland Stanford Jr. University,
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
import org.springframework.boot.web.server.LocalServerPort;
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
 * Test class for org.lockss.laaws.config.api.AuidsApiController.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class TestAuidsApiServiceImpl extends SpringLockssTestCase4 {
  private static L4JLogger log = L4JLogger.getLogger();

  private static final String EMPTY_STRING = "";

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
    setUpTempDirectory(TestAuidsApiServiceImpl.class.getCanonicalName());

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
    assertEquals("org|lockss|plugin|NamedPlugin&handle~handlehandle", foo);

    foo = runTestCalculateAuid(NamedArchivalUnit.NAMED_PLUGIN_NAME,
                               null,
                               MapUtil.map("handle", "second|handle"),
                               null, HttpStatus.OK);
    assertEquals("org|lockss|plugin|NamedPlugin&handle~second%7Chandle", foo);

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
                 foo);

    foo = runTestCalculateAuid(NamedArchivalUnit.NAMED_PLUGIN_NAME,
                               null,
                               MapUtil.map("handle", "second|handle"),
                               CONTENT_ADMIN, HttpStatus.OK);
    assertEquals("org|lockss|plugin|NamedPlugin&handle~second%7Chandle",
                 foo);

    foo = runTestCalculateAuid(NamedArchivalUnit.NAMED_PLUGIN_NAME,
                               null,
                               MapUtil.map("handle", "second|handle",
                                           "nondefparam", "foo"),
                               CONTENT_ADMIN, HttpStatus.OK);
    assertEquals("org|lockss|plugin|NamedPlugin&handle~second%7Chandle",
                 foo);

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
                 foo);
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
      Map<String,Object> res = getRestConfigClient(credentials)
        .calculateAuid(pluginId, handle, auConfig);

      if (!RestUtil.isSuccess(expectedStatus)) {
	fail("Should have thrown LockssRestHttpException");
      }
      return (String)res.get("auid");
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
