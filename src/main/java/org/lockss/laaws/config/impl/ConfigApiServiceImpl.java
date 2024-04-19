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

import static org.lockss.config.ConfigManager.*;
import static org.lockss.config.RestConfigClient.CONFIG_PART_NAME;
import static org.lockss.util.BuildInfo.BUILD_HOST;
import static org.lockss.util.BuildInfo.BUILD_TIMESTAMP;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.MalformedParametersException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.security.AccessControlException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.lockss.alert.AlertManagerImpl;
import org.lockss.app.LockssDaemon;
import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.config.HttpRequestPreconditions;
import org.lockss.config.ConfigFileReadWriteResult;
import org.lockss.daemon.Cron;
import org.lockss.spring.auth.Roles;
import org.lockss.spring.auth.AuthUtil;
import org.lockss.spring.base.*;
import org.lockss.laaws.config.api.ConfigApiDelegate;
import org.lockss.util.rest.repo.util.NamedInputStreamResource;
import org.lockss.log.L4JLogger;
import org.lockss.spring.error.LockssRestServiceException;
import org.lockss.util.AccessType;
import org.lockss.util.BuildInfo;
import org.lockss.util.DaemonVersion;
import org.lockss.util.PlatformVersion;
import org.lockss.util.StringUtil;
import org.lockss.util.os.PlatformUtil;
import org.lockss.util.time.TimeBase;
import org.lockss.ws.entities.DaemonVersionWsResult;
import org.lockss.ws.entities.JavaVersionWsResult;
import org.lockss.ws.entities.PlatformConfigurationWsResult;
import org.lockss.ws.entities.PlatformWsResult;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;

/**
 * Service for accessing the system configuration.
 */
@Service
public class ConfigApiServiceImpl
  extends BaseSpringApiServiceImpl
  implements ConfigApiDelegate {

  static final String SECTION_NAME_CLUSTER = "cluster";
  static final String SECTION_NAME_USER_CONFIG = "user-config";
  static final String SECTION_NAME_PROPSLOCKSS = "props_lockss";
  static final String SECTION_NAME_UI_IP_ACCESS = "ui_ip_access";
  static final String SECTION_NAME_PROXY_IP_ACCESS = "proxy_ip_access";
  static final String SECTION_NAME_PLUGIN = "plugin";
  static final String SECTION_NAME_AU = "au";
  static final String SECTION_NAME_TITLE_DB = "titledb";
  static final String SECTION_NAME_ICP_SERVER = "icp_server";
  static final String SECTION_NAME_AUDIT_PROXY = "audit_proxy";
  static final String SECTION_NAME_CONTENT_SERVERS = "content_servers";
  static final String SECTION_NAME_ACCESS_GROUPS = "access_groups";
  static final String SECTION_NAME_CRAWL_PROXY = "crawl_proxy";
  static final String SECTION_NAME_EXPERT = "expert";
  static final String SECTION_NAME_ALERT = "alert";
  static final String SECTION_NAME_CRONSTATE = "cronstate";
  static final String SECTION_NAME_MIGRATION = "migration";

  private static L4JLogger log = L4JLogger.getLogger();

  // The map of writable configuration file sections.
  @SuppressWarnings("serial")
  private static final Map<String, String> configWritableSectionMap =
      new HashMap<String, String>() {{
	put(SECTION_NAME_UI_IP_ACCESS, CONFIG_FILE_UI_IP_ACCESS);
	put(SECTION_NAME_PROXY_IP_ACCESS, CONFIG_FILE_PROXY_IP_ACCESS);
	put(SECTION_NAME_PLUGIN, CONFIG_FILE_PLUGIN_CONFIG);
	put(SECTION_NAME_AU, CONFIG_FILE_AU_CONFIG);
	put(SECTION_NAME_TITLE_DB, CONFIG_FILE_BUNDLED_TITLE_DB);
	put(SECTION_NAME_ICP_SERVER, CONFIG_FILE_ICP_SERVER);
	put(SECTION_NAME_AUDIT_PROXY, CONFIG_FILE_AUDIT_PROXY);
	put(SECTION_NAME_CONTENT_SERVERS, CONFIG_FILE_CONTENT_SERVERS);
	put(SECTION_NAME_ACCESS_GROUPS, CONFIG_FILE_ACCESS_GROUPS);
	put(SECTION_NAME_CRAWL_PROXY, CONFIG_FILE_CRAWL_PROXY);
	put(SECTION_NAME_EXPERT, CONFIG_FILE_EXPERT_CLUSTER);
	put(SECTION_NAME_ALERT, AlertManagerImpl.CONFIG_FILE_ALERT_CONFIG);
	put(SECTION_NAME_CRONSTATE, Cron.CONFIG_FILE_CRON_STATE);
	put(SECTION_NAME_MIGRATION, CONFIG_FILE_MIGRATION);
      }
  };

  private static final String BUILD_TIMESTAMP_FORMAT = "dd-MMM-yy HH:mm:ss zzz";

  // The map of read-only configuration file sections.
  private Map<String, String> configReadOnlySectionMap = null;

  /**
   * Provides the configuration file for a section given the section name.
   *
   * @param sectionName
   *          A String with the section name.
   * @param accept
   *          A String with the value of the "Accept" request header.
   * @param ifMatch
   *          A List<String> with the "If-Match" request header, containing an
   *          asterisk or values equivalent to the "If-Unmodified-Since" request
   *          header but with a granularity of 1 ms.
   * @param ifModifiedSince
   *          A String with "If-Modified-Since" request header.
   * @param ifNoneMatch
   *          A List<String> with the "If-None-Match" request header, containing
   *          an asterisk or values equivalent to the "If-Modified-Since"
   *          request header but with a granularity of 1 ms.
   * @param ifUnmodifiedSince
   *          A String with the "If-Unmodified-Since" request header.
   * @return a {@code ResponseEntity<MultiValueMap<String, Object>>} with the
   *         section configuration file contents.
   */
  @Override
  public ResponseEntity getSectionConfig(
      String sectionName, String ifMatch,
      String ifModifiedSince, String ifNoneMatch, String ifUnmodifiedSince) {

    log.debug2("sectionName = {}", () -> sectionName);
    log.debug2("ifMatch = {}", () -> ifMatch);
    log.debug2("ifModifiedSince = {}", () -> ifModifiedSince);
    log.debug2("ifNoneMatch = {}", () -> ifNoneMatch);
    log.debug2("ifUnmodifiedSince = {}", () -> ifUnmodifiedSince);

    String parsedRequest = String.format(
        "sectionName: %s, ifMatch: %s, ifModifiedSince: %s, ifNoneMatch: %s, ifUnmodifiedSince: %s",
        sectionName, ifMatch, ifModifiedSince, ifNoneMatch, ifUnmodifiedSince
    );

    log.debug2("Parsed request: {}", parsedRequest);

    if (!waitConfig()) {
      throw new LockssRestServiceException(HttpStatus.SERVICE_UNAVAILABLE, "Not ready", parsedRequest);
    }

      HttpRequestPreconditions preconditions;

      // Validate the precondition headers.
      try {
        preconditions = new HttpRequestPreconditions(
            StringUtil.breakAt(ifMatch, ",", true), ifModifiedSince,
            StringUtil.breakAt(ifNoneMatch, ",", true), ifUnmodifiedSince
        );

        log.trace("preconditions = {}", () -> preconditions);
      } catch (IllegalArgumentException iae) {
        throw new LockssRestServiceException(HttpStatus.BAD_REQUEST, iae.getMessage(), parsedRequest);
      }

      // Validate the name of the section to be obtained.
      String canonicalSectionName;
      try {
        canonicalSectionName = validateSectionName(sectionName, AccessType.READ);
        log.trace("canonicalSectionName = {}", () -> canonicalSectionName);
      } catch (MalformedParametersException mpe) {
        throw new LockssRestServiceException(HttpStatus.BAD_REQUEST, mpe.getMessage(), parsedRequest);
      }

      ConfigManager configManager = getConfigManager();

      // Try to get the name of the read-only configuration file to be returned.
      String sectionUrl = getConfigReadOnlySectionMap().get(canonicalSectionName);

      if (log.isTraceEnabled()) {
        log.trace("Read-Only sectionUrl = {}", sectionUrl);
      }

      // Check whether no read-only configuration file was found.
      if (sectionUrl == null) {
        // Yes: Try to get the name of the writable configuration file to be returned.
        sectionUrl = new File(
            configManager.getCacheConfigDir(),
            configWritableSectionMap.get(canonicalSectionName)
        ).toString();

        if (log.isTraceEnabled()) {
          log.trace("Writable sectionUrl = {}", sectionUrl);
        }
      }

      try {
        return buildGetUrlResponse(
            sectionUrl,
            preconditions,
            configManager.conditionallyReadCacheConfigFile(sectionUrl, preconditions)
        );

      } catch (FileNotFoundException fnfe) {

        String message = "Can't get the content for sectionName '" + sectionName + "'";
        log.error(message, fnfe);
        throw new LockssRestServiceException(HttpStatus.NOT_FOUND, message, parsedRequest);

      } catch (IOException ioe) {

        String message = "Can't get the content for URL '" + sectionUrl + "'";
        log.error(message, ioe);
        throw new LockssRestServiceException(HttpStatus.INTERNAL_SERVER_ERROR, message, parsedRequest);

      }
  }

  /**
   * Provides the configuration file for a given URL.
   *
   * @param url
   *          A String with the url.
   * @param accept
   *          A String with the value of the "Accept" request header.
   * @param ifMatch
   *          A List<String> with the "If-Match" request header, containing an
   *          asterisk or values equivalent to the "If-Unmodified-Since" request
   *          header but with a granularity of 1 ms.
   * @param ifModifiedSince
   *          A String with "If-Modified-Since" request header.
   * @param ifNoneMatch
   *          A List<String> with the "If-None-Match" request header, containing
   *          an asterisk or values equivalent to the "If-Modified-Since"
   *          request header but with a granularity of 1 ms.
   * @param ifUnmodifiedSince
   *          A String with the "If-Unmodified-Since" request header.
   * @return a {@code ResponseEntity<MultiValueMap<String, Object>>} with the
   *         section configuration file.
   */
  @Override
  public ResponseEntity getUrlConfig(String url,
                                     String ifMatch, String ifModifiedSince, String ifNoneMatch,
                                     String ifUnmodifiedSince) {

    log.debug2("url = {}", () -> url);
    log.debug2("ifMatch = {}", () -> ifMatch);
    log.debug2("ifModifiedSince = {}", () -> ifModifiedSince);
    log.debug2("ifNoneMatch = {}", () -> ifNoneMatch);
    log.debug2("ifUnmodifiedSince = {}", () -> ifUnmodifiedSince);

    String parsedRequest = String.format(
        "url: %s, ifMatch: %s, ifModifiedSince: %s, ifNoneMatch: %s, ifUnmodifiedSince: %s",
        url, ifMatch, ifModifiedSince, ifNoneMatch, ifUnmodifiedSince
    );

    log.debug2("Parsed request: {}", parsedRequest);

    if (!waitConfig()) {
      throw new LockssRestServiceException(HttpStatus.SERVICE_UNAVAILABLE, "Not ready", parsedRequest);
    }

      HttpRequestPreconditions preconditions;

      // Validate the precondition headers.
      try {
        preconditions = new HttpRequestPreconditions(
            StringUtil.breakAt(ifMatch, ",", true), ifModifiedSince,
            StringUtil.breakAt(ifNoneMatch, ",", true), ifUnmodifiedSince
        );

        log.trace("preconditions = {}", () -> preconditions);
      } catch (IllegalArgumentException iae) {
        return new ResponseEntity<String>(iae.getMessage(), HttpStatus.BAD_REQUEST);
      }

      try {
        return buildGetUrlResponse(
            url,
            preconditions,
            getConfigManager().conditionallyReadCacheConfigFile(url, preconditions)
        );
      } catch (FileNotFoundException | UnknownHostException | ConnectException e) {
        String message = "Can't get the content for url '" + url + "'";
        log.debug(message + ": " + e);
        throw new LockssRestServiceException(HttpStatus.NOT_FOUND, message, parsedRequest);
      } catch (UnsupportedOperationException uoe) {
        String message = "Can't get the content for url '" + url + "'";
        log.error(message, uoe);
        return new ResponseEntity<String>(message, HttpStatus.BAD_REQUEST);

      } catch (IOException ioe) {
        String message = "Can't get the content for URL '" + url + "'";
        log.error(message, ioe);
        return new ResponseEntity<String>(message,
            HttpStatus.INTERNAL_SERVER_ERROR);
      }
  }

  /**
   * Provides the timestamp of the last time the configuration was updated.
   *
   * @return a {@code ResponseEntity<Date>} with the timestamp.
   */
  @Override
  public ResponseEntity<OffsetDateTime> getLastUpdateTime() {
    log.debug2("Invoked");

    // Check whether the service has not been fully initialized.
    if (!waitReady()) {
      // Yes: Notify the client.
      return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
    }

    try {
      long millis = getConfigManager().getLastUpdateTime();
      log.trace("millis = {}", () -> millis);

      Date result = new Date(millis);
      log.debug2("result = {}", () -> result);
      return new ResponseEntity<OffsetDateTime>(OffsetDateTime.ofInstant(
	  Instant.ofEpochMilli(millis), ZoneId.systemDefault()), HttpStatus.OK);
    } catch (Exception e) {
      String message = "Cannot getLastUpdateTime()";
      log.error(message, e);
      return new ResponseEntity<OffsetDateTime>(OffsetDateTime.ofInstant(null,
	  null), HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Provides the URLs from which the configuration was loaded.
   *
   * @return a {@code ResponseEntity<List<String>>} with the URLs.
   */
  @Override
  public ResponseEntity<List<String>> getLoadedUrlList() {
    log.debug2("Invoked");

    // Check whether the service has not been fully initialized.
    if (!waitReady()) {
      // Yes: Notify the client.
      return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
    }

    try {
      List<String> result = (List<String>)getConfigManager().getLoadedUrlList();
      log.debug2("result = {}", () -> result);
      return new ResponseEntity<List<String>>(result, HttpStatus.OK);
    } catch (Exception e) {
      String message = "Cannot getLoadedUrlList()";
      log.error(message, e);
      return new ResponseEntity<List<String>>(new ArrayList<String>(),
	  HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Stores the configuration file for a section given the section name.
   *
   * @param sectionName
   *          A String with the section name.
   * @param configFile
   *          A MultipartFile with the configuration file to be stored.
   * @param ifMatch
   *          A List<String> with the "If-Match" request header, containing an
   *          asterisk or values equivalent to the "If-Unmodified-Since" request
   *          header but with a granularity of 1 ms.
   * @param ifModifiedSince
   *          A String with "If-Modified-Since" request header.
   * @param ifNoneMatch
   *          A List<String> with the "If-None-Match" request header, containing
   *          an asterisk or values equivalent to the "If-Modified-Since"
   *          request header but with a granularity of 1 ms.
   * @param ifUnmodifiedSince
   *          A String with the "If-Unmodified-Since" request header.
   * @return a {@code ResponseEntity<Void>}.
   */
  @Override
  public ResponseEntity putConfig(String sectionName,
                                  MultipartFile configFile, String ifMatch, String ifModifiedSince,
                                  String ifNoneMatch, String ifUnmodifiedSince) {

    log.debug2("sectionName = {}", () -> sectionName);
    log.debug2("configFile = {}", () -> configFile);
    log.debug2("ifMatch = {}", () -> ifMatch);
    log.debug2("ifModifiedSince = {}", () -> ifModifiedSince);
    log.debug2("ifNoneMatch = {}", () -> ifNoneMatch);
    log.debug2("ifUnmodifiedSince = {}", () -> ifUnmodifiedSince);

    if (!waitConfig(0)) {
      return new ResponseEntity<String>("Not Ready", HttpStatus.SERVICE_UNAVAILABLE);
    }

    // Check for required role.
    try {
      AuthUtil.checkHasRole(Roles.ROLE_USER_ADMIN);
    } catch (AccessControlException ace) {
      log.warn(ace.getMessage());
      return new ResponseEntity<Void>(HttpStatus.FORBIDDEN);
    }

    HttpRequestPreconditions preconditions;

    // Validate the precondition headers.
    try {
      preconditions = new HttpRequestPreconditions(
          StringUtil.breakAt(ifMatch, ",", true), ifModifiedSince,
          StringUtil.breakAt(ifNoneMatch, ",", true), ifUnmodifiedSince);
      log.trace("preconditions = {}", () -> preconditions);
    } catch (IllegalArgumentException iae) {
      return new ResponseEntity<Void>(HttpStatus.BAD_REQUEST);
    }

    if (configFile == null) {
      String message = "Invalid metadata modification specification: null";
      log.warn(message);
      return new ResponseEntity<Void>(HttpStatus.BAD_REQUEST);
    }

    // Validate the name of the section to be obtained.
    String canonicalSectionName;

    try {
      canonicalSectionName = validateSectionName(sectionName, AccessType.WRITE);
      log.trace("canonicalSectionName = {}", () -> canonicalSectionName);
    } catch (MalformedParametersException mpe) {
      return new ResponseEntity<Void>(HttpStatus.BAD_REQUEST);
    }

    try {
      ConfigManager configManager = getConfigManager();

      // Get the name of the file to be stored.
      String sectionUrl = configWritableSectionMap.get(canonicalSectionName);
      log.trace("sectionUrl = {}", () -> sectionUrl);

      String filename =
          new File(configManager.getCacheConfigDir(), sectionUrl).toString();
      log.trace("filename = {}", () -> filename);

      // Write the file.
      ConfigFileReadWriteResult writeResult = configManager
          .conditionallyWriteCacheConfigFile(filename, preconditions,
              configFile.getInputStream());

      // Check whether the preconditions have not been met.
      if (!writeResult.isPreconditionsMet()) {
        // Yes: Return no content, just a Precondition-Failed status.
        return new ResponseEntity<Void>(HttpStatus.PRECONDITION_FAILED);
      }

      String lastModified = writeResult.getLastModified();
      log.trace("lastModified = {}", () -> lastModified);

      String etag = writeResult.getEtag();
      log.trace("etag = {}", () -> etag);

      // Return the new file entity tag in the response.
      HttpHeaders responseHeaders = new HttpHeaders();
      setLastModified(responseHeaders, lastModified);
      setETag(responseHeaders, etag);
      log.trace("responseHeaders = {}", () -> responseHeaders);

      return new ResponseEntity<Void>(null, responseHeaders, HttpStatus.OK);
    } catch (Exception e) {
      String message = "Cannot putConfig() for sectionName = '" + sectionName
          + "', configFile = '" + configFile + "'";
      log.error(message, e);
      return new ResponseEntity<Void>(HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Requests a reload of the configuration.
   *
   * @return a {@code ResponseEntity<Void>} with the status.
   */
  @Override
  public ResponseEntity<Void> putConfigReload() {
    log.debug2("Invoked");

    // Check whether the service has not been fully initialized.
    if (!waitReady()) {
      // Yes: Notify the client.
      return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
    }

    // Check for required role
    try {
      AuthUtil.checkHasRole(Roles.ROLE_USER_ADMIN);
    } catch (AccessControlException ace) {
      log.warn(ace.getMessage());
      return new ResponseEntity<Void>(HttpStatus.FORBIDDEN);
    }

    try {
      getConfigManager().requestReload();
      log.debug2("Done");
      return new ResponseEntity<Void>(HttpStatus.OK);
    } catch (Exception e) {
      String message = "Cannot requestReload()";
      log.error(message, e);
      return new ResponseEntity<Void>(HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Provides the platform configuration.
   *
   * @return a {@code ResponseEntity<PlatformConfigurationWsResult>} with the
   *         platform configuration.
   */
  @Override
  public ResponseEntity getPlatformConfig() {
    log.debug2("Invoked.");

    if (!waitConfig()) {
      return new ResponseEntity<String>("Not Ready",
					HttpStatus.SERVICE_UNAVAILABLE);
    }

    PlatformConfigurationWsResult result = new PlatformConfigurationWsResult();

    try {
      LockssDaemon theDaemon = LockssDaemon.getLockssDaemon();
      Configuration config = ConfigManager.getCurrentConfig();

      result.setHostName(config.get(PARAM_PLATFORM_FQDN));

      result.setIpAddress(config.get(PARAM_PLATFORM_IP_ADDRESS));

      result.setGroups((List<String>)(config.getPlatformGroupList()));
      result.setProject(config.get(PARAM_PLATFORM_PROJECT));
      result.setV3Identity(config.get(PARAM_PLATFORM_LOCAL_V3_IDENTITY));

      String smtpHost = config.get(PARAM_PLATFORM_SMTP_HOST);

      if (smtpHost != null) {
	int smtpPort = config.getInt(PARAM_PLATFORM_SMTP_PORT,
	    org.lockss.mail.SmtpMailService.DEFAULT_SMTPPORT);
	result.setMailRelay(smtpHost + ":" + smtpPort);
      }

      result.setAdminEmail(config.get(PARAM_PLATFORM_ADMIN_EMAIL));
      result.setDisks((List<String>)
	  (config.getList(PARAM_PLATFORM_DISK_SPACE_LIST)));

      result.setCurrentTime(TimeBase.nowMs());
      result.setUptime(TimeBase.msSince(theDaemon.getStartDate().getTime()));

      DaemonVersion daemonVersion = ConfigManager.getDaemonVersion();
      DaemonVersionWsResult daemonVersionResult = new DaemonVersionWsResult();

      daemonVersionResult.setFullVersion(daemonVersion.displayString());
      daemonVersionResult.setMajorVersion(daemonVersion.getMajorVersion());
      daemonVersionResult.setMinorVersion(daemonVersion.getMinorVersion());
      daemonVersionResult.setBuildVersion(daemonVersion.getBuildVersion());

      result.setDaemonVersion(daemonVersionResult);

      JavaVersionWsResult javaVersionResult = new JavaVersionWsResult();

      Properties sprops = System.getProperties();
      javaVersionResult.setVersion(sprops.getProperty("java.version"));
      javaVersionResult.setSpecificationVersion(
	  sprops.getProperty("java.specification.version"));
      javaVersionResult.setRuntimeVersion(
	  sprops.getProperty("java.runtime.version"));
      javaVersionResult.setRuntimeName(sprops.getProperty("java.runtime.name"));

      result.setJavaVersion(javaVersionResult);

      PlatformVersion platformVersion = Configuration.getPlatformVersion();
      PlatformWsResult platform = new PlatformWsResult();

      if (platformVersion != null) {
	platform.setName(platformVersion.getName());
	platform.setVersion(platformVersion.getVersion());
	platform.setSuffix(platformVersion.getSuffix());
	result.setPlatform(platform);
      }

      result.setCurrentWorkingDirectory(PlatformUtil.getCwd());

      result.setProperties((List<String>)
	  (ConfigManager.getConfigManager().getConfigUrlList()));

      result.setBuildHost(BuildInfo.getBuildProperty(BUILD_HOST));
      result.setBuildTimestamp(getBuildTimestamp());

      log.debug2("result = {}", result);
      return new ResponseEntity<PlatformConfigurationWsResult>(result,
	    HttpStatus.OK);
    } catch (Exception e) {
      String message = "Cannot getPlatformConfig()";
      log.error(message, e);
      return new ResponseEntity<Void>(HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Provides a validated canonical version of the passed section name.
   *
   * @param sectionName
   *          A String with the section name.
   * @param access
   *          An AccessType indicating whether this is for a reading or writing
   *          operation.
   * @return a String with the validated canonical version of the section name.
   * @throws MalformedParametersException
   *           if validation fails.
   */
  protected String validateSectionName(String sectionName, AccessType access)
      throws MalformedParametersException {
    log.debug2("sectionName = {}", () -> sectionName);
    log.debug2("access = {}", () -> access);

    // Verify that some section name has been passed.
    if (sectionName == null || sectionName.isEmpty()) {
      String message = "Invalid sectionName '" + sectionName + "'";
      log.warn(message);
      throw new MalformedParametersException(message);
    }

    String canonicalVersion = sectionName.toLowerCase();
    log.trace("canonicalVersion = {}", () -> canonicalVersion);

    // Verify that the passed section name is known.
    if (!configWritableSectionMap.containsKey(canonicalVersion)
	&& !getConfigReadOnlySectionMap().containsKey(canonicalVersion)) {
      String message = "Invalid sectionName '" + sectionName + "'";
      log.warn(message);
      throw new MalformedParametersException(message);
    }

    // Verify that the section is writable, if needed.
    if (access == AccessType.WRITE
	&& !configWritableSectionMap.containsKey(canonicalVersion)) {
      String message =
	  "Invalid writing operation on sectionName '" + sectionName + "'";
      log.warn(message);
      throw new MalformedParametersException(message);
    }

    return canonicalVersion;
  }

  /**
   * Provides a lazy-loaded copy of the map of read-only configuration file
   * sections.
   *
   * @return a {@code Map<String, String>} with the map of read-only
   *         configuration file sections.
   */
  private Map<String, String> getConfigReadOnlySectionMap() {
    if (configReadOnlySectionMap == null) {
      Map<String, String> tmpmap = new HashMap<>();
      tmpmap.put(SECTION_NAME_CLUSTER, "dyn:cluster.xml");
      tmpmap.put(SECTION_NAME_USER_CONFIG, "dyn:user-config.xml");
      configReadOnlySectionMap = tmpmap;
      log.trace("configReadOnlySectionMap = {}",
	  () -> configReadOnlySectionMap);
    }

    return configReadOnlySectionMap;
  }

  /**
   * Provides the response for a request to get the content at a URL.
   *
   * @param url
   *          A String with the URL where to get the content.
   * @param preconditions
   *          An HttpRequestPreconditions with the request preconditions to be
   *          met.
   * @param readResult
   *          A ConfigFileReadWriteResult with an indication of whether the
   *          preconditions are met and the input stream, entity tag and content
   *          type and length to be included in the response.
   * @return a ResponseEntity<?> with the response for the request to get the
   *         content at a URL.
   */
  private ResponseEntity<?> buildGetUrlResponse(String url,
      HttpRequestPreconditions preconditions,
      ConfigFileReadWriteResult readResult) {
    log.debug2("url = {}", () -> url);
    log.debug2("preconditions = {}", () -> preconditions);
    log.debug2("readResult = {}", () -> readResult);

    HttpStatus status;

    // Get the last modification token of the file.
    String lastModified = readResult.getLastModified();
    log.trace("lastModified = {}", () -> lastModified);

    // Get the entity tag of the file.
    String etag = readResult.getEtag();
    log.trace("etag = {}", () -> etag);

    // Check whether the preconditions have not been met.
    if (!readResult.isPreconditionsMet()) {
      // Yes: Check whether an If-Modified-Since header or an If-None-Match
      // header were passed.
      if ((preconditions.getIfModifiedSince() != null
	  && !preconditions.getIfModifiedSince().isEmpty())
	  ||
	  (preconditions.getIfNoneMatch() != null
	  && !preconditions.getIfNoneMatch().isEmpty())) {
	// Yes: Return no content, just a Not-Modified status.
	HttpHeaders responseHeaders = new HttpHeaders();
	setLastModified(responseHeaders, lastModified);
	setETag(responseHeaders, etag);
	log.trace("responseHeaders = {}", () -> responseHeaders);

	status = HttpStatus.NOT_MODIFIED;
	log.trace("status = {}", () -> status);

	return new ResponseEntity<String>(null, responseHeaders, status);
      } else {
	// No: Return no content, just a Precondition-Failed status.
	status = HttpStatus.PRECONDITION_FAILED;
	log.trace("status = {}", () -> status);

	return new ResponseEntity<String>(null, null, status);
      }
    }

    // Save the version unique identifier header in the part of the response.
    HttpHeaders partHeaders = new HttpHeaders();
    setLastModified(partHeaders, lastModified);
    setETag(partHeaders, etag);

    // Save the content type header in the part of the response.
    MediaType contentType = readResult.getContentType();
    log.trace("contentType = {}", () -> contentType);
    partHeaders.setContentType(contentType);

    // This must be set or else AbstractResource#contentLength will read the
    // entire InputStream to determine the content length, which will exhaust
    // the InputStream.
    long contentLength = readResult.getContentLength();
    log.trace("contentLength = {}", () -> contentLength);
    partHeaders.setContentLength(contentLength);

    log.trace("partHeaders = {}", () -> partHeaders);

    // Build the response entity.
    MultiValueMap<String, Object> parts =
	new LinkedMultiValueMap<String, Object>();

    Resource resource = new NamedInputStreamResource(CONFIG_PART_NAME,
	readResult.getInputStream());
    parts.add(CONFIG_PART_NAME, new HttpEntity<>(resource, partHeaders));
    log.trace("parts = {}", () -> parts);

    // Specify the response content type.
    HttpHeaders responseHeaders = new HttpHeaders();
    responseHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);
    log.trace("responseHeaders = {}", () -> responseHeaders);

    status = HttpStatus.OK;
    log.trace("status = {}", () -> status);

    return new ResponseEntity<MultiValueMap<String, Object>>(parts,
	  responseHeaders, status);
  }

  protected void setETag(HttpHeaders hdrs, String etag) {
    if (etag != null) {
      hdrs.setETag(etag);
    }
  }

  protected void setLastModified(HttpHeaders hdrs, String last) {
    if (last != null) {
      hdrs.set(HttpHeaders.LAST_MODIFIED, last);
    }
  }

  /**
   * Provides the build timestamp.
   *
   * @return A long with the build timestamp.
   * @throws ParseException if there are problems parsing the timestamp.
   */
  protected long getBuildTimestamp() throws ParseException {
    log.debug2("Invoked.");

    try {
      long timestamp = (new SimpleDateFormat(BUILD_TIMESTAMP_FORMAT))
	  .parse(BuildInfo.getBuildProperty(BUILD_TIMESTAMP)).getTime();

      log.debug2("timestamp = {}", timestamp);
      return timestamp;
    } catch (ParseException pe) {
      log.error("Caught ParseException", pe);
      log.error("BuildInfo.getBuildProperty(BUILD_TIMESTAMP)) = '"
	  + BuildInfo.getBuildProperty(BUILD_TIMESTAMP) + "'");
      throw pe;
    }
  }
}
