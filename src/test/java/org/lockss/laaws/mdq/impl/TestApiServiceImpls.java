/*

Copyright (c) 2000-2019 Board of Trustees of Leland Stanford Jr. University,
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
package org.lockss.laaws.mdq.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.derby.jdbc.EmbeddedDataSource;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.lockss.app.LockssDaemon;
import org.lockss.config.Configuration;
import org.lockss.laaws.config.ComposedApplication;
import org.lockss.laaws.mdq.model.AuMetadataPageInfo;
import org.lockss.laaws.mdq.model.PageInfo;
import org.lockss.laaws.mdq.model.UrlInfo;
import org.lockss.log.L4JLogger;
import org.lockss.metadata.ItemMetadata;
import org.lockss.metadata.ItemMetadataContinuationToken;
import org.lockss.metadata.MetadataDbManager;
import org.lockss.metadata.query.MetadataQueryManager;
import org.lockss.plugin.Plugin;
import org.lockss.plugin.definable.DefinablePlugin;
import org.lockss.test.MockArchivalUnit;
import org.lockss.test.SpringLockssTestCase;
import org.lockss.util.ListUtil;
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
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Test class for org.lockss.laaws.mdq.api.MetadataApiServiceImpl and
 * org.lockss.laaws.mdq.api.UrlsApiServiceImpl.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = ComposedApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class TestApiServiceImpls extends SpringLockssTestCase {
  private static final L4JLogger log = L4JLogger.getLogger();

  private static final String UI_PORT_CONFIGURATION_TEMPLATE =
      "UiPortConfigTemplate.txt";
  private static final String UI_PORT_CONFIGURATION_FILE = "UiPort.txt";
  private static final String DB_CONFIGURATION_TEMPLATE =
      "DbConfigTemplate.txt";
  private static final String DB_CONFIGURATION_FILE = "DbConfig.txt";

  private static final String EMPTY_STRING = "";

  // The identifier of an AU that exists in the test system.
  private static final String AUID_1 =
      "org|lockss|plugin|pensoft|oai|PensoftOaiPlugin"
      + "&au_oai_date~2013&au_oai_set~biorisk"
      + "&base_url~http%3A%2F%2Fbiorisk%2Epensoft%2Enet%2F";

  // The identifier of another AU that exists in the test system.
  private static final String AUID_2 =
      "org|lockss|plugin|pensoft|oai|PensoftOaiPlugin"
      + "&au_oai_date~1954&au_oai_set~genealogy"
      + "&base_url~http%3A%2F%2Fexample%2Ecom%2F";

  // The identifier of an AU that does not exist in the test system.
  private static final String UNKNOWN_AUID ="unknown_auid";

  // A DOI that does not exist in the test system.
  private static final String UNKNOWN_DOI ="unknown_doi";

  // The metadata of the items in the first Archival Unit in the test system.
  private static ItemMetadata ITEM_METADATA_1_1 = null;
  private static ItemMetadata ITEM_METADATA_1_2 = null;
  private static ItemMetadata ITEM_METADATA_1_3 = null;
  private static ItemMetadata ITEM_METADATA_1_4 = null;
  private static ItemMetadata ITEM_METADATA_1_5 = null;

  private static List<ItemMetadata> AU_1_MD = null;

  // The metadata of the items in the second Archival Unit in the test system.
  private static ItemMetadata ITEM_METADATA_2_1 = null;

  private static List<ItemMetadata> AU_2_MD = null;

  // Credentials.
  private final Credentials USER_ADMIN =
      this.new Credentials("lockss-u", "lockss-p");
  private final Credentials CONTENT_ADMIN =
      this.new Credentials("content-admin", "I'mContentAdmin");
  private final Credentials ACCESS_CONTENT =
      this.new Credentials("access-content", "I'mAccessContent");
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
   * Set up code to be run before all tests.
   *
   * @throws IOException
   *           if there are problems.
   */
  @BeforeClass
  public static void setUpBeforeAllTests() throws IOException {
    ClassLoader cl = Thread.currentThread().getContextClassLoader();

    // Fill in the metadata of the first Archival Unit in the test system.
    ITEM_METADATA_1_1 = new ObjectMapper().readValue(
	cl.getResourceAsStream("metadata/AU_1_1.json"), ItemMetadata.class);
    ITEM_METADATA_1_2 = new ObjectMapper().readValue(
	cl.getResourceAsStream("metadata/AU_1_2.json"), ItemMetadata.class);
    ITEM_METADATA_1_3 = new ObjectMapper().readValue(
	cl.getResourceAsStream("metadata/AU_1_3.json"), ItemMetadata.class);
    ITEM_METADATA_1_4 = new ObjectMapper().readValue(
	cl.getResourceAsStream("metadata/AU_1_4.json"), ItemMetadata.class);
    ITEM_METADATA_1_5 = new ObjectMapper().readValue(
	cl.getResourceAsStream("metadata/AU_1_5.json"), ItemMetadata.class);

    AU_1_MD = ListUtil.list(ITEM_METADATA_1_1, ITEM_METADATA_1_2,
	ITEM_METADATA_1_3, ITEM_METADATA_1_4, ITEM_METADATA_1_5);

    // Fill in the metadata of the second Archival Unit in the test system.
    ITEM_METADATA_2_1 = new ObjectMapper().readValue(
	cl.getResourceAsStream("metadata/AU_2_1.json"), ItemMetadata.class);

    AU_2_MD = ListUtil.list(ITEM_METADATA_2_1);
  }

  /**
   * Set up code to be run before each test.
   * 
   * @throws Exception
   *           if there are problems.
   */
  @Before
  public void setUpBeforeEachTest() throws Exception {
    log.debug2("port = {}", () -> port);

    // Set up the temporary directory where the test data will reside.
    setUpTempDirectory(TestApiServiceImpls.class.getCanonicalName());

    // Copy the necessary files to the test temporary directory.
    File srcTree = new File(new File("test"), "cache");
    log.trace("srcTree = {}", () -> srcTree.getAbsolutePath());

    copyToTempDir(srcTree);

    // Set up the UI port.
    setUpUiPort(UI_PORT_CONFIGURATION_TEMPLATE, UI_PORT_CONFIGURATION_FILE);

    // Set up the database configuration.
    setUpDbConfig(DB_CONFIGURATION_TEMPLATE, DB_CONFIGURATION_FILE);

    // Create the embedded Derby database to use during the tests.
    TestDerbyMetadataDbManager testDbManager = new TestDerbyMetadataDbManager();
    testDbManager.startService();

    // Populate the test database.
    MetadataQueryManager mqm = new MetadataQueryManager(testDbManager);
    Plugin plugin = new DefinablePlugin();
    plugin.initPlugin(LockssDaemon.getLockssDaemon());

    populateMetadata(mqm, plugin, ITEM_METADATA_1_1);
    populateMetadata(mqm, plugin, ITEM_METADATA_1_2);
    populateMetadata(mqm, plugin, ITEM_METADATA_1_3);
    populateMetadata(mqm, plugin, ITEM_METADATA_1_4);
    populateMetadata(mqm, plugin, ITEM_METADATA_1_5);
    populateMetadata(mqm, plugin, ITEM_METADATA_2_1);

    log.debug2("Done");
  }

  /**
   * Populates in the database the metadata of one Archival Unit item.
   * 
   * @param mqm
   *          A MetadataQueryManager with the metadata query manager.
   * @param plugin
   *          A Plugin with the Archival Unit plugin.
   * @param itemMetadata
   *          An ItemMetadata with the metadata of the item.
   * @throws Exception
   *           if there are problems.
   */
  private void populateMetadata(MetadataQueryManager mqm, Plugin plugin,
      ItemMetadata itemMetadata) throws Exception {
    String auId = itemMetadata.getScalarMap().get("au_id");
    log.trace("auId = {}", auId);

    mqm.storeAuItemMetadataForTesting(itemMetadata,
	new MockArchivalUnit(plugin, auId));
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
    getMetadataAusAuidUnAuthenticatedTest();
    getUrlsDoiUnAuthenticatedTest();
    getUrlsOpenUrlUnAuthenticatedTest();

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
    getMetadataAusAuidAuthenticatedTest();
    getUrlsDoiAuthenticatedTest();
    getUrlsOpenUrlAuthenticatedTest();

    log.debug2("Done");
  }

  /**
   * Provides the standard command line arguments to start the server.
   * 
   * @return a List<String> with the command line arguments.
   * @throws IOException
   *           if there are problems.
   */
  private List<String> getCommandLineArguments() throws IOException {
    log.debug2("Invoked");

    List<String> cmdLineArgs = new ArrayList<String>();
    cmdLineArgs.add("-p");
    cmdLineArgs.add(getPlatformDiskSpaceConfigPath());
    cmdLineArgs.add("-p");
    cmdLineArgs.add("config/common.xml");
    cmdLineArgs.add("-p");
    cmdLineArgs.add(getUiPortConfigFile().getAbsolutePath());
    cmdLineArgs.add("-p");
    cmdLineArgs.add(getDbConfigFile().getAbsolutePath());
    cmdLineArgs.add("-p");
    cmdLineArgs.add("test/config/lockss.txt");
    cmdLineArgs.add("-p");
    cmdLineArgs.add("test/config/lockss.opt");

    log.debug2("cmdLineArgs = {}", () -> cmdLineArgs);
    return cmdLineArgs;
  }

  /**
   * Runs the getMetadataAusAuid()-related un-authenticated-specific tests.
   * 
   * @throws Exception
   *           if there are problems.
   */
  private void getMetadataAusAuidUnAuthenticatedTest() throws Exception {
    log.debug2("Invoked");

    // No AUId: Spring reports it cannot find a match to an endpoint.
    runTestGetMetadataAusAuid(null, null, null, null, HttpStatus.NOT_FOUND);
    runTestGetMetadataAusAuid(null, null, null, ANYBODY, HttpStatus.NOT_FOUND);
    runTestGetMetadataAusAuid(null, -1, null, null, HttpStatus.NOT_FOUND);
    runTestGetMetadataAusAuid(null, null, null, ANYBODY, HttpStatus.NOT_FOUND);

    // Empty AUId: Spring reports it cannot find a match to an endpoint.
    runTestGetMetadataAusAuid(EMPTY_STRING, null, null, null,
	HttpStatus.NOT_FOUND);
    runTestGetMetadataAusAuid(EMPTY_STRING, null, null, ANYBODY,
	HttpStatus.NOT_FOUND);
    runTestGetMetadataAusAuid(EMPTY_STRING, -1, null, null,
	HttpStatus.NOT_FOUND);
    runTestGetMetadataAusAuid(EMPTY_STRING, -1, null, ANYBODY,
	HttpStatus.NOT_FOUND);

    // Unknown AUId.
    runTestGetMetadataAusAuid(UNKNOWN_AUID, null, null, null,
	HttpStatus.NOT_FOUND);
    runTestGetMetadataAusAuid(UNKNOWN_AUID, null, null, ANYBODY,
	HttpStatus.NOT_FOUND);
    runTestGetMetadataAusAuid(UNKNOWN_AUID, 1, null, null,
	HttpStatus.NOT_FOUND);
    runTestGetMetadataAusAuid(UNKNOWN_AUID, 1, null, ANYBODY,
	HttpStatus.NOT_FOUND);

    // Success getting all with no credentials.
    verifyMetadata(AU_1_MD, null, runTestGetMetadataAusAuid(AUID_1, null, null,
	null, HttpStatus.OK));

    // Pagination with no credentials.
    runTestGetMetadataAusAuidPagination(null);

    // Success getting all with with bad credentials.
    verifyMetadata(AU_2_MD, null, runTestGetMetadataAusAuid(AUID_2, null, null,
	ANYBODY, HttpStatus.OK));

    // Pagination with bad credentials.
    runTestGetMetadataAusAuidPagination(ANYBODY);

    getMetadataAusAuidCommonTest();

    log.debug2("Done");
  }

  /**
   * Runs the getMetadataAusAuid()-related authenticated-specific tests.
   * 
   * @throws Exception
   *           if there are problems.
   */
  private void getMetadataAusAuidAuthenticatedTest() throws Exception {
    log.debug2("Invoked");

    // No AUId.
    runTestGetMetadataAusAuid(null, null, null, null, HttpStatus.UNAUTHORIZED);
    runTestGetMetadataAusAuid(null, null, null, ANYBODY,
	HttpStatus.UNAUTHORIZED);

    // Empty AUId.
    runTestGetMetadataAusAuid(EMPTY_STRING, null, null, null,
	HttpStatus.UNAUTHORIZED);
    runTestGetMetadataAusAuid(EMPTY_STRING, null, null, ANYBODY,
	HttpStatus.UNAUTHORIZED);

    // Unknown AUId.
    runTestGetMetadataAusAuid(UNKNOWN_AUID, null, null, null,
	HttpStatus.UNAUTHORIZED);
    runTestGetMetadataAusAuid(UNKNOWN_AUID, null, null, ANYBODY,
	HttpStatus.UNAUTHORIZED);

    // No credentials.
    runTestGetMetadataAusAuid(AUID_2, null, null, null,
	HttpStatus.UNAUTHORIZED);
    runTestGetMetadataAusAuid(AUID_2, 0, null, null, HttpStatus.UNAUTHORIZED);

    // Bad credentials.
    runTestGetMetadataAusAuid(AUID_1, null, null, ANYBODY,
	HttpStatus.UNAUTHORIZED);
    runTestGetMetadataAusAuid(AUID_1, -1, null, ANYBODY,
	HttpStatus.UNAUTHORIZED);

    getMetadataAusAuidCommonTest();

    log.debug2("Done");
  }

  /**
   * Runs the getMetadataAusAuid()-related authentication-independent tests.
   * 
   * @throws Exception
   *           if there are problems.
   */
  private void getMetadataAusAuidCommonTest() throws Exception {
    log.debug2("Invoked");

    // No AUId: Spring reports it cannot find a match to an endpoint.
    runTestGetMetadataAusAuid(null, null, null, USER_ADMIN,
	HttpStatus.NOT_FOUND);

    // Empty AUId: Spring reports it cannot find a match to an endpoint.
    runTestGetMetadataAusAuid(EMPTY_STRING, null, null, CONTENT_ADMIN,
	HttpStatus.NOT_FOUND);

    // Unknown AUId.
    runTestGetMetadataAusAuid(UNKNOWN_AUID, null, null, ACCESS_CONTENT,
	HttpStatus.NOT_FOUND);

    // Success.
    verifyMetadata(AU_1_MD, null, runTestGetMetadataAusAuid(AUID_1, null, null,
	USER_ADMIN, HttpStatus.OK));
    verifyMetadata(AU_2_MD, null, runTestGetMetadataAusAuid(AUID_2, null, null,
	CONTENT_ADMIN, HttpStatus.OK));

    // Pagination.
    runTestGetMetadataAusAuidPagination(USER_ADMIN);
    runTestGetMetadataAusAuidPagination(CONTENT_ADMIN);
    runTestGetMetadataAusAuidPagination(ACCESS_CONTENT);

    log.debug2("Done");
  }

  /**
   * Performs pagination tests.
   * 
   * @param user
   *          A String with the request username.
   * @param password
   *          A String with the request password.
   * @throws Exception
   *           if there are problems.
   */
  private void runTestGetMetadataAusAuidPagination(Credentials credentials)
      throws Exception {
    log.debug2("credentials = {}", () -> credentials);

    // Bad limit.
    int requestCount = -1;
    String continuationToken = null;
    runTestGetMetadataAusAuid(UNKNOWN_AUID, requestCount, continuationToken,
	credentials, HttpStatus.BAD_REQUEST);

    // Not found.
    requestCount = 0;
    runTestGetMetadataAusAuid(UNKNOWN_AUID, requestCount, continuationToken,
	credentials, HttpStatus.NOT_FOUND);

    // Get all the items.
    requestCount = 10;
    ItemMetadataContinuationToken expectedImct = null;
    verifyMetadata(AU_1_MD, expectedImct, runTestGetMetadataAusAuid(AUID_1,
	requestCount, continuationToken, credentials, HttpStatus.OK));

    // Get the first item.
    requestCount = 1;
    AuMetadataPageInfo aumpi = runTestGetMetadataAusAuid(AUID_1, requestCount,
	continuationToken, credentials, HttpStatus.OK);

    continuationToken = aumpi.getPageInfo().getContinuationToken();
    ItemMetadataContinuationToken firstImct =
	new ItemMetadataContinuationToken(continuationToken);
    Long auExtractionTimestamp = firstImct.getAuExtractionTimestamp();
    Long lastItemMdItemSeq = firstImct.getLastItemMdItemSeq();

    // The five items in this Archival Unit have primary keys ranging from 2 to
    // 6, both inclusive.
    assertEquals(2L, lastItemMdItemSeq.longValue());
    verifyMetadata(ListUtil.list(ITEM_METADATA_1_1), firstImct, aumpi);

    // Get the next one.
    aumpi = runTestGetMetadataAusAuid(AUID_1, requestCount, continuationToken,
	credentials, HttpStatus.OK);

    continuationToken = aumpi.getPageInfo().getContinuationToken();
    expectedImct = new ItemMetadataContinuationToken(auExtractionTimestamp,
	lastItemMdItemSeq.longValue() + requestCount);
    verifyMetadata(ListUtil.list(ITEM_METADATA_1_2), expectedImct, aumpi);
    lastItemMdItemSeq = expectedImct.getLastItemMdItemSeq();

    // Get the next two.
    requestCount = 2;
    aumpi = runTestGetMetadataAusAuid(AUID_1, requestCount, continuationToken,
	credentials, HttpStatus.OK);

    continuationToken = aumpi.getPageInfo().getContinuationToken();
    expectedImct = new ItemMetadataContinuationToken(
	auExtractionTimestamp, lastItemMdItemSeq.longValue() + requestCount);
    verifyMetadata(ListUtil.list(ITEM_METADATA_1_3, ITEM_METADATA_1_4),
	expectedImct, aumpi);

    // Get the last (partial) page.
    requestCount = 3;
    expectedImct = null;
    verifyMetadata(ListUtil.list(ITEM_METADATA_1_5), expectedImct,
	runTestGetMetadataAusAuid(AUID_1, requestCount, continuationToken,
	    credentials, HttpStatus.OK));

    // Get the first two items.
    requestCount = 2;
    continuationToken = null;
    aumpi = runTestGetMetadataAusAuid(AUID_1, requestCount, continuationToken,
	credentials, HttpStatus.OK);

    continuationToken = aumpi.getPageInfo().getContinuationToken();
    firstImct = new ItemMetadataContinuationToken(continuationToken);
    lastItemMdItemSeq = firstImct.getLastItemMdItemSeq();
    assertEquals(3L, lastItemMdItemSeq.longValue());
    verifyMetadata(ListUtil.list(ITEM_METADATA_1_1, ITEM_METADATA_1_2),
	firstImct, aumpi);

    // Get the next three (the rest).
    requestCount = 3;
    verifyMetadata(ListUtil.list(ITEM_METADATA_1_3, ITEM_METADATA_1_4,
	ITEM_METADATA_1_5), expectedImct,
	runTestGetMetadataAusAuid(AUID_1, requestCount, continuationToken,
	    credentials, HttpStatus.OK));

    // Get the first three items.
    requestCount = 3;
    continuationToken = null;
    aumpi = runTestGetMetadataAusAuid(AUID_1, requestCount, continuationToken,
	credentials, HttpStatus.OK);

    continuationToken = aumpi.getPageInfo().getContinuationToken();
    firstImct = new ItemMetadataContinuationToken(continuationToken);
    lastItemMdItemSeq = firstImct.getLastItemMdItemSeq();
    assertEquals(4L, lastItemMdItemSeq.longValue());
    verifyMetadata(ListUtil.list(ITEM_METADATA_1_1, ITEM_METADATA_1_2,
	ITEM_METADATA_1_3), firstImct, aumpi);

    // Get the next two (the rest).
    requestCount = 2;
    verifyMetadata(ListUtil.list(ITEM_METADATA_1_4, ITEM_METADATA_1_5),
	expectedImct, runTestGetMetadataAusAuid(AUID_1, requestCount,
	    continuationToken, credentials, HttpStatus.OK));

    // Get the first four items.
    requestCount = 4;
    continuationToken = null;
    aumpi = runTestGetMetadataAusAuid(AUID_1, requestCount, continuationToken,
	credentials, HttpStatus.OK);

    continuationToken = aumpi.getPageInfo().getContinuationToken();
    firstImct = new ItemMetadataContinuationToken(continuationToken);
    assertEquals(5L, firstImct.getLastItemMdItemSeq().longValue());
    verifyMetadata(ListUtil.list(ITEM_METADATA_1_1, ITEM_METADATA_1_2,
	ITEM_METADATA_1_3, ITEM_METADATA_1_4), firstImct, aumpi);

    // Get the last (partial) page.
    requestCount = 5;
    verifyMetadata(ListUtil.list(ITEM_METADATA_1_5), expectedImct,
	runTestGetMetadataAusAuid(AUID_1, requestCount, continuationToken,
	    credentials, HttpStatus.OK));

    // Get the first five (all) items.
    continuationToken = null;
    verifyMetadata(AU_1_MD, expectedImct,
	runTestGetMetadataAusAuid(AUID_1, requestCount, continuationToken,
	    credentials, HttpStatus.OK));

    // Try to get the first one with an incorrect Archival Unit metadata
    // extraction timestamp in the past.
    requestCount = 1;
    continuationToken = new ItemMetadataContinuationToken(
	auExtractionTimestamp - 1000000L, 0L).toWebResponseContinuationToken();
    runTestGetMetadataAusAuid(AUID_1, requestCount, continuationToken,
	credentials, HttpStatus.CONFLICT);

    // Try to get the first one with an incorrect Archival Unit metadata
    // extraction timestamp in the future.
    continuationToken = new ItemMetadataContinuationToken(
	auExtractionTimestamp + 1000000L, 0L).toWebResponseContinuationToken();
    runTestGetMetadataAusAuid(AUID_1, requestCount, continuationToken,
	credentials, HttpStatus.CONFLICT);

    log.debug2("Done");
  }

  /**
   * Performs a GET operation for the metadata of an Archival Unit.
   * 
   * @param auId
   *          A String with the identifier of the Archival Unit.
   * @param limit
   *          An Integer with the maximum number of AU metadata items to be
   *          returned.
   * @param continuationToken
   *          An ItemMetadataContinuationToken with the continuation token of
   *          the next page of metadata to be returned.
   * @param credentials
   *          A Credentials with the request credentials.
   * @param expectedStatus
   *          An HttpStatus with the HTTP status of the result.
   * @return an AuMetadataPageInfo with the Archival Unit metadata.
   * @throws Exception
   *           if there are problems.
   */
  private AuMetadataPageInfo runTestGetMetadataAusAuid(String auId,
      Integer limit, String continuationToken, Credentials credentials,
      HttpStatus expectedStatus) throws Exception {
    log.debug2("auId = {}", () -> auId);
    log.debug2("limit = {}", () -> limit);
    log.debug2("continuationToken = {}", () -> continuationToken);
    log.debug2("credentials = {}", () -> credentials);
    log.debug2("expectedStatus = {}", () -> expectedStatus);

    // Get the test URL template.
    String template = getTestUrlTemplate("/metadata/aus/{auid}");

    // Create the URI of the request to the REST service.
    UriComponents uriComponents = UriComponentsBuilder.fromUriString(template)
	.build().expand(Collections.singletonMap("auid", auId));

    UriComponentsBuilder ucb =
	UriComponentsBuilder.newInstance().uriComponents(uriComponents);

    if (limit != null) {
      ucb.queryParam("limit", limit);
    }

    if (continuationToken != null) {
      ucb.queryParam("continuationToken", continuationToken);
    }

    URI uri = ucb.build().encode().toUri();
    log.trace("uri = {}", () -> uri);

    // Initialize the request to the REST service.
    RestTemplate restTemplate = new RestTemplate();

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
    ResponseEntity<String> response =
	new TestRestTemplate(restTemplate).exchange(uri, HttpMethod.GET,
	    requestEntity, String.class);

    // Get the response status.
    HttpStatus statusCode = response.getStatusCode();
    assertEquals(expectedStatus, statusCode);

    AuMetadataPageInfo result = null;

    if (isSuccess(statusCode)) {
      result = new ObjectMapper().readValue(response.getBody(),
	  AuMetadataPageInfo.class);
    }

    if (log.isDebug2Enabled()) log.debug2("result = {}", result);
    return result;
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
   * Verifies that the passed metadata matches the expected items.
   * 
   * @param expectedItems
   *          A List<ItemMetadata> with the expected items to found.
   * @param expectedContinuationToken
   *          An ItemMetadataContinuationToken with the expected continuation
   *          token returned.
   * @param auMetadata
   *          A AuMetadataPageInfo with the Archival Unit metadata to be
   *          verified.
   */
  private void verifyMetadata(List<ItemMetadata> expectedItems,
      ItemMetadataContinuationToken expectedContinuationToken,
      AuMetadataPageInfo auMetadata) {
    log.debug2("expectedItems = {}", () -> expectedItems);
    log.debug2("expectedContinuationToken = {}",
	() -> expectedContinuationToken);
    log.debug2("auMetadata = {}", () -> auMetadata);

    PageInfo pageInfo = auMetadata.getPageInfo();
    assertNull(pageInfo.getTotalCount());
    assertEquals(expectedItems.size(), pageInfo.getResultsPerPage().intValue());

    if (expectedContinuationToken != null) {
      assertEquals(expectedContinuationToken.toWebResponseContinuationToken(),
	  pageInfo.getContinuationToken());
      assertEquals(expectedContinuationToken.getLastItemMdItemSeq(),
	  auMetadata.getItems().get(auMetadata.getItems().size()-1).getId());
      assertTrue(pageInfo.getNextLink().startsWith(getTestUrlTemplate("")));
    } else {
      assertNull(pageInfo.getContinuationToken());
    }

    assertTrue(pageInfo.getCurLink().startsWith(getTestUrlTemplate("")));
    assertEquals(expectedItems.size(), auMetadata.getItems().size());

    for (int i = 0; i < expectedItems.size(); i++) {
      expectedItems.get(i).setId(auMetadata.getItems().get(i).getId());
      assertEquals(expectedItems.get(i), auMetadata.getItems().get(i));
    }

    log.debug2("Done");
  }

  /**
   * Runs the getUrlsDoi()-related un-authenticated-specific tests.
   */
  private void getUrlsDoiUnAuthenticatedTest() {
    log.debug2("Invoked");

    // No DOI.
    runTestGetUrlsDoi(null, null, HttpStatus.OK, null);
    runTestGetUrlsDoi(null, ANYBODY, HttpStatus.OK, null);

    // Empty DOI.
    runTestGetUrlsDoi(EMPTY_STRING, null, HttpStatus.OK, null);
    runTestGetUrlsDoi(EMPTY_STRING, ANYBODY, HttpStatus.OK, null);

    // Unknown DOI.
    runTestGetUrlsDoi(UNKNOWN_DOI, null, HttpStatus.OK, null);
    runTestGetUrlsDoi(UNKNOWN_DOI, ANYBODY, HttpStatus.OK, null);

    // DOI of the first item of the first Archival Unit in the test system.
    String doi = ITEM_METADATA_1_1.getScalarMap().get("doi");
    String expectedUrl = ITEM_METADATA_1_1.getMapMap().get("url").get("Access");

    runTestGetUrlsDoi(doi, null, HttpStatus.OK, expectedUrl);
    runTestGetUrlsDoi(doi, ANYBODY, HttpStatus.OK, expectedUrl);

    // DOI of the second item of the first Archival Unit in the test system.
    doi = ITEM_METADATA_1_2.getScalarMap().get("doi");
    expectedUrl = ITEM_METADATA_1_2.getMapMap().get("url").get("Access");

    runTestGetUrlsDoi(doi, null, HttpStatus.OK, expectedUrl);
    runTestGetUrlsDoi(doi, ANYBODY, HttpStatus.OK, expectedUrl);

    getUrlsDoiCommonTest();

    log.debug2("Done");
  }

  /**
   * Runs the getUrlsDoi()-related authenticated-specific tests.
   */
  private void getUrlsDoiAuthenticatedTest() {
    log.debug2("Invoked");

    // No DOI.
    runTestGetUrlsDoi(null, null, HttpStatus.UNAUTHORIZED, null);
    runTestGetUrlsDoi(null, ANYBODY, HttpStatus.UNAUTHORIZED, null);

    // Empty DOI.
    runTestGetUrlsDoi(EMPTY_STRING, null, HttpStatus.UNAUTHORIZED, null);
    runTestGetUrlsDoi(EMPTY_STRING, ANYBODY, HttpStatus.UNAUTHORIZED,
	null);

    // Unknown DOI.
    runTestGetUrlsDoi(UNKNOWN_DOI, null, HttpStatus.UNAUTHORIZED, null);
    runTestGetUrlsDoi(UNKNOWN_DOI, ANYBODY, HttpStatus.UNAUTHORIZED,
	null);

    // DOI of the first Archival Unit in the test system.
    String doi = ITEM_METADATA_1_1.getScalarMap().get("doi");

    runTestGetUrlsDoi(doi, null, HttpStatus.UNAUTHORIZED, null);
    runTestGetUrlsDoi(doi, ANYBODY, HttpStatus.UNAUTHORIZED, null);

    getUrlsDoiCommonTest();

    log.debug2("Done");
  }

  /**
   * Runs the getUrlsDoi()-related authentication-independent tests.
   */
  private void getUrlsDoiCommonTest() {
    log.debug2("Invoked");

    // No DOI.
    runTestGetUrlsDoi(null, USER_ADMIN, HttpStatus.OK, null);

    // Empty DOI.
    runTestGetUrlsDoi(EMPTY_STRING, CONTENT_ADMIN, HttpStatus.OK, null);

    // Unknown DOI.
    runTestGetUrlsDoi(UNKNOWN_DOI, ACCESS_CONTENT, HttpStatus.OK, null);

    // DOI of the second item of the first Archival Unit in the test system.
    String doi = ITEM_METADATA_1_2.getScalarMap().get("doi");
    String expectedUrl = ITEM_METADATA_1_2.getMapMap().get("url").get("Access");

    runTestGetUrlsDoi(doi, USER_ADMIN, HttpStatus.OK, expectedUrl);

    // DOI of the fourth item of the first Archival Unit in the test system.
    doi = ITEM_METADATA_1_4.getScalarMap().get("doi");
    expectedUrl = ITEM_METADATA_1_4.getMapMap().get("url").get("Access");

    runTestGetUrlsDoi(doi, ACCESS_CONTENT, HttpStatus.OK, expectedUrl);

    log.debug2("Done");
  }

  /**
   * Performs a GET operation for the URL of a DOI.
   *
   * @param doi
   *          A String with the DOI.
   * @param credentials
   *          A Credentials with the request credentials.
   * @param expectedStatus
   *          An HttpStatus with the HTTP status of the result.
   * @param expectedUrl
   *          A String with the URL in the result.
   */
  private void runTestGetUrlsDoi(String doi, Credentials credentials,
      HttpStatus expectedStatus, String expectedUrl) {
    log.debug2("doi = {}", () -> doi);
    log.debug2("credentials = {}", () -> credentials);
    log.debug2("expectedStatus = {}", () -> expectedStatus);
    log.debug2("expectedUrl = {}", () -> expectedUrl);

    // Get the test URL template.
    String template = getTestUrlTemplate("/urls/doi");

    // Create the URI of the request to the REST service.
    URI uri = UriComponentsBuilder.fromHttpUrl(template).queryParam("doi", doi)
	.build().encode().toUri();
    log.trace("uri = {}", () -> uri);

    // Initialize the request to the REST service.
    RestTemplate restTemplate = new RestTemplate();

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
    ResponseEntity<UrlInfo> response = new TestRestTemplate(restTemplate)
	.exchange(uri, HttpMethod.GET, requestEntity, UrlInfo.class);

    // Get the response status.
    HttpStatus statusCode = response.getStatusCode();
    assertEquals(expectedStatus, statusCode);

    // Verify.
    if (isSuccess(statusCode)) {
      UrlInfo result = response.getBody();

      // Parameters.
      Map<String, String> params = result.getParams();
      assertEquals(1, params.size());

      if (doi == null) {
	assertEquals("info:doi/", params.get("rft_id"));
      } else {
	assertEquals("info:doi/" + doi, params.get("rft_id"));
      }

      // URLs.
      List<String> urls = result.getUrls();

      if (expectedUrl == null) {
	assertEquals(0, urls.size());
      } else {
	assertEquals(1, urls.size());
	assertEquals(expectedUrl, urls.get(0));
      }
    }

    log.debug2("Done");
  }

  /**
   * Runs the getUrlsOpenUrl()-related un-authenticated-specific tests.
   */
  private void getUrlsOpenUrlUnAuthenticatedTest() {
    log.debug2("Invoked");

    // No OpenURL params.
    runTestGetUrlsOpenUrl(null, null, HttpStatus.BAD_REQUEST, null);
    runTestGetUrlsOpenUrl(null, ANYBODY, HttpStatus.BAD_REQUEST, null);

    // Empty OpenURL params.
    List<String> params = new ArrayList<>();
    runTestGetUrlsOpenUrl(params, null, HttpStatus.BAD_REQUEST, null);
    runTestGetUrlsOpenUrl(params, ANYBODY, HttpStatus.BAD_REQUEST, null);

    // Unknown DOI.
    params = ListUtil.list("rft_id=info:doi/" + UNKNOWN_DOI);
    runTestGetUrlsOpenUrl(params, null, HttpStatus.OK, null);
    runTestGetUrlsOpenUrl(params, ANYBODY, HttpStatus.OK, null);

    // DOI of the first item of the first Archival Unit in the test system.
    params = ListUtil.list(
	"rft_id=info:doi/" + ITEM_METADATA_1_1.getScalarMap().get("doi"));
    String expectedUrl = ITEM_METADATA_1_1.getMapMap().get("url").get("Access");

    runTestGetUrlsOpenUrl(params, null, HttpStatus.OK, expectedUrl);
    runTestGetUrlsOpenUrl(params, ANYBODY, HttpStatus.OK, expectedUrl);

    // Multiple parameters for the second item of the first Archival Unit in the
    // test system.
    params = ListUtil.list(
	"rft.issn=" + ITEM_METADATA_1_2.getMapMap().get("issn").get("p_issn"),
	"rft.volume=" + ITEM_METADATA_1_2.getScalarMap().get("volume"),
	"rft.spage=" + ITEM_METADATA_1_2.getScalarMap().get("start_page"));
    expectedUrl = ITEM_METADATA_1_2.getMapMap().get("url").get("Access");

    runTestGetUrlsOpenUrl(params, null, HttpStatus.OK, expectedUrl);
    runTestGetUrlsOpenUrl(params, ANYBODY, HttpStatus.OK, expectedUrl);

    // Multiple parameters for the first item of the second Archival Unit in the
    // test system.
    params = ListUtil.list(
	"rft.issn=" + ITEM_METADATA_2_1.getMapMap().get("issn").get("p_issn"),
	"rft.volume=" + ITEM_METADATA_2_1.getScalarMap().get("volume"),
	"rft.spage=" + ITEM_METADATA_2_1.getScalarMap().get("start_page"));

    expectedUrl = ITEM_METADATA_2_1.getMapMap().get("url").get("Access");

    runTestGetUrlsOpenUrl(params, null, HttpStatus.OK, expectedUrl);
    runTestGetUrlsOpenUrl(params, ANYBODY, HttpStatus.OK, expectedUrl);

    getUrlsOpenUrlCommonTest();

    log.debug2("Done");
  }

  /**
   * Runs the getUrlsOpenUrl()-related authenticated-specific tests.
   */
  private void getUrlsOpenUrlAuthenticatedTest() {
    log.debug2("Invoked");

    // No OpenURL params.
    runTestGetUrlsOpenUrl(null, null, HttpStatus.UNAUTHORIZED, null);
    runTestGetUrlsOpenUrl(null, ANYBODY, HttpStatus.UNAUTHORIZED, null);

    // Empty OpenURL params.
    List<String> params = new ArrayList<>();
    runTestGetUrlsOpenUrl(params, null, HttpStatus.UNAUTHORIZED, null);
    runTestGetUrlsOpenUrl(params, ANYBODY, HttpStatus.UNAUTHORIZED, null);

    // DOI of the first Archival Unit in the test system.
    params = ListUtil.list(
	"rft_id=info:doi/" + ITEM_METADATA_1_1.getScalarMap().get("doi"));

    runTestGetUrlsOpenUrl(params, null, HttpStatus.UNAUTHORIZED, null);
    runTestGetUrlsOpenUrl(params, ANYBODY, HttpStatus.UNAUTHORIZED, null);

    // Unknown DOI.
    params = ListUtil.list("rft_id=info:doi/" + UNKNOWN_DOI);

    runTestGetUrlsOpenUrl(params, null, HttpStatus.UNAUTHORIZED, null);
    runTestGetUrlsOpenUrl(params, ANYBODY, HttpStatus.UNAUTHORIZED, null);

    // Multiple parameters for the first Archival Unit in the test system.
    params = ListUtil.list(
	"rft.issn=" + ITEM_METADATA_1_1.getMapMap().get("issn").get("p_issn"),
	"rft.volume=" + ITEM_METADATA_1_1.getScalarMap().get("volume"),
	"rft.spage=" + ITEM_METADATA_1_1.getScalarMap().get("start_page"));

    runTestGetUrlsOpenUrl(params, null, HttpStatus.UNAUTHORIZED, null);
    runTestGetUrlsOpenUrl(params, ANYBODY, HttpStatus.UNAUTHORIZED, null);

    // Multiple parameters for the second Archival Unit in the test system.
    params = ListUtil.list(
	"rft.issn=" + ITEM_METADATA_2_1.getMapMap().get("issn").get("p_issn"),
	"rft.volume=" + ITEM_METADATA_2_1.getScalarMap().get("volume"),
	"rft.spage=" + ITEM_METADATA_2_1.getScalarMap().get("start_page"));

    runTestGetUrlsOpenUrl(params, null, HttpStatus.UNAUTHORIZED, null);
    runTestGetUrlsOpenUrl(params, ANYBODY, HttpStatus.UNAUTHORIZED, null);

    getUrlsOpenUrlCommonTest();

    log.debug2("Done");
  }

  /**
   * Runs the getUrlsOpenUrl()-related authentication-independent tests.
   */
  private void getUrlsOpenUrlCommonTest() {
    log.debug2("Invoked");

    // No OpenURL params.
    runTestGetUrlsOpenUrl(null, USER_ADMIN, HttpStatus.BAD_REQUEST, null);

    // Empty OpenURL params.
    List<String> params = new ArrayList<>();
    runTestGetUrlsOpenUrl(params, CONTENT_ADMIN, HttpStatus.BAD_REQUEST, null);

    // Unknown DOI.
    params = ListUtil.list("rft_id=info:doi/" + UNKNOWN_DOI);
    runTestGetUrlsOpenUrl(params, ACCESS_CONTENT, HttpStatus.OK, null);

    // DOI of the first item of the first Archival Unit in the test system.
    params = ListUtil.list(
	"rft_id=info:doi/" + ITEM_METADATA_1_1.getScalarMap().get("doi"));
    String expectedUrl = ITEM_METADATA_1_1.getMapMap().get("url").get("Access");

    runTestGetUrlsOpenUrl(params, USER_ADMIN, HttpStatus.OK, expectedUrl);

    // Multiple parameters for the third item of the first Archival Unit in the
    // test system.
    params = ListUtil.list(
	"rft.issn=" + ITEM_METADATA_1_3.getMapMap().get("issn").get("p_issn"),
	"rft.volume=" + ITEM_METADATA_1_3.getScalarMap().get("volume"),
	"rft.spage=" + ITEM_METADATA_1_3.getScalarMap().get("start_page"));

    expectedUrl = ITEM_METADATA_1_3.getMapMap().get("url").get("Access");

    runTestGetUrlsOpenUrl(params, CONTENT_ADMIN, HttpStatus.OK, expectedUrl);

    // Multiple parameters for the fifth item of the first Archival Unit in the
    // test system.
    params = ListUtil.list(
	"rft.issn=" + ITEM_METADATA_1_5.getMapMap().get("issn").get("p_issn"),
	"rft.volume=" + ITEM_METADATA_1_5.getScalarMap().get("volume"),
	"rft.spage=" + ITEM_METADATA_1_5.getScalarMap().get("start_page"));
    expectedUrl = ITEM_METADATA_1_5.getMapMap().get("url").get("Access");

    runTestGetUrlsOpenUrl(params, ACCESS_CONTENT, HttpStatus.OK, expectedUrl);

    // Multiple parameters for the first item of the second Archival Unit in the
    // test system.
    params = ListUtil.list(
	"rft.issn=" + ITEM_METADATA_2_1.getMapMap().get("issn").get("p_issn"),
	"rft.volume=" + ITEM_METADATA_2_1.getScalarMap().get("volume"),
	"rft.spage=" + ITEM_METADATA_2_1.getScalarMap().get("start_page"));
    expectedUrl = ITEM_METADATA_2_1.getMapMap().get("url").get("Access");

    runTestGetUrlsOpenUrl(params, ACCESS_CONTENT, HttpStatus.OK, expectedUrl);

    log.debug2("Done");
  }

  /**
   * Performs a GET operation for the URL that results from performing an
   * OpenURL query.
   *
   * @param openUrlParams
   *          A List<String> with the parameters of the OpenURL query.
   * @param credentials
   *          A Credentials with the request credentials.
   * @param expectedStatus
   *          An HttpStatus with the HTTP status of the result.
   * @param expectedUrl
   *          A String with the URL in the result.
   */
  private void runTestGetUrlsOpenUrl(List<String> openUrlParams,
      Credentials credentials, HttpStatus expectedStatus, String expectedUrl) {
    log.debug2("openUrlParams = {}", () -> openUrlParams);
    log.debug2("credentials = {}", () -> credentials);
    log.debug2("expectedStatus = {}", () -> expectedStatus);
    log.debug2("expectedUrl = {}", () -> expectedUrl);

    // Get the test URL template.
    String template = getTestUrlTemplate("/urls/openurl");

    // Create the URI of the request to the REST service.
    UriComponentsBuilder ucb = UriComponentsBuilder.fromHttpUrl(template);

    if (openUrlParams != null) {
      for (String param : openUrlParams) {
	ucb = ucb.queryParam("params", param);
      }
    }

    URI uri = ucb.build().encode().toUri();
    log.trace("uri = {}", () -> uri);

    // Initialize the request to the REST service.
    RestTemplate restTemplate = new RestTemplate();

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
    ResponseEntity<UrlInfo> response = new TestRestTemplate(restTemplate)
	.exchange(uri, HttpMethod.GET, requestEntity, UrlInfo.class);

    // Get the response status.
    HttpStatus statusCode = response.getStatusCode();
    assertEquals(expectedStatus, statusCode);

    // Verify.
    if (isSuccess(statusCode)) {
      UrlInfo result = response.getBody();

      // Parameters.
      Map<String, String> params = result.getParams();
      assertEquals(openUrlParams.size(), params.size());

      for (Map.Entry<String, String> param : params.entrySet()) {
	assertTrue(openUrlParams.contains(
	    param.getKey() + "=" + param.getValue()));
      }

      // URLs.
      List<String> urls = result.getUrls();

      if (expectedUrl == null) {
	assertEquals(0, urls.size());
      } else {
	assertEquals(1, urls.size());
	assertEquals(expectedUrl, urls.get(0));
      }
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
   * The database manager used to create the test database.
   */
  private class TestDerbyMetadataDbManager extends MetadataDbManager {
    private String dbName = "MetadataDbManager";

    public TestDerbyMetadataDbManager() {
      super(true);
    }

    @Override
    protected String getDataSourceClassName(Configuration config) {
      return EmbeddedDataSource.class.getCanonicalName();
    }

    @Override
    protected String getDataSourceDatabaseName(Configuration config) {
      return getTempDirPath() + "/cache/db/" + dbName;
    }

    @Override
    protected String getVersionSubsystemName() {
      return dbName;
    }
  }
}
