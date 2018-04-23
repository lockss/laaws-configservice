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

import static org.lockss.config.ConfigManager.*;
import io.swagger.annotations.ApiParam;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.MalformedParametersException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.security.AccessControlException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;
import org.lockss.alert.AlertManagerImpl;
import org.lockss.app.LockssApp;
import org.lockss.config.ConfigFile;
import org.lockss.config.ConfigManager;
import org.lockss.config.CurrentConfig;
import org.lockss.daemon.Cron;
import org.lockss.spring.auth.Roles;
import org.lockss.spring.auth.SpringAuthenticationFilter;
import org.lockss.laaws.status.model.ApiStatus;
import org.lockss.spring.status.SpringLockssBaseApiController;
import org.lockss.util.IOUtil;
import org.lockss.util.StringUtil;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Controller for access to the system configuration.
 */
@RestController
public class ConfigApiController extends SpringLockssBaseApiController
implements ConfigApi {
  private static Logger log = Logger.getLogger(ConfigApiController.class);

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
	put(SECTION_NAME_EXPERT, CONFIG_FILE_EXPERT);
	put(SECTION_NAME_ALERT, AlertManagerImpl.CONFIG_FILE_ALERT_CONFIG);
	put(SECTION_NAME_CRONSTATE, Cron.CONFIG_FILE_CRON_STATE);
      }
  };

  // The map of read-only configuration file sections.
  private Map<String, String> configReadOnlySectionMap = null;

  /**
   * Provides the configuration file for a section given the section name.
   * 
   * @param sectionName
   *          A String with the section name.
   * @param accept
   *          A String with the value of the "Accept" request header.
   * @param eTag
   *          A String with a value equivalent to the "If-Modified-Since"
   *          request header but with a granularity of 1 ms.
   * @return a {@code ResponseEntity<MultiValueMap<String, Object>>} with the
   *         section configuration file contents.
   */
  @Override
  @RequestMapping(value = "/config/file/{sectionName}",
  produces = { "multipart/form-data", "application/json" },
  method = RequestMethod.GET)
  public ResponseEntity<?> getSectionConfig(
      @PathVariable("sectionName") String sectionName,
      @RequestHeader(value=HttpHeaders.ACCEPT, required=true) String accept,
      @RequestHeader(value=HttpHeaders.ETAG, required=false) String eTag) {
    if (log.isDebugEnabled()) {
      log.debug("sectionName = " + sectionName);
      log.debug("accept = " + accept);
      log.debug("eTag = " + eTag);
    }

    try {
      // Validate the name of the section to be obtained.
      String canonicalSectionName = null;

      try {
	canonicalSectionName = validateSectionName(sectionName, true);
	if (log.isDebugEnabled())
	  log.debug("canonicalSectionName = " + canonicalSectionName);
      } catch (MalformedParametersException mpe) {
	return new ResponseEntity<String>(mpe.getMessage(),
	    HttpStatus.BAD_REQUEST);
      }

      // Check whether the request did not specify the appropriate "Accept"
      // header.
      if (accept.indexOf(MediaType.MULTIPART_FORM_DATA_VALUE) < 0) {
	// Yes: Report the problem.
	String message = "Accept header does not include '"
	    + MediaType.MULTIPART_FORM_DATA_VALUE + "'";
	log.warn(message);
	return new ResponseEntity<String>(message, HttpStatus.NOT_ACCEPTABLE);
      }

      ConfigManager configManager = getConfigManager();
      String lastModified = null;
      HttpHeaders partHeaders = new HttpHeaders();

      // Try to get the name of the read-only configuration file to be returned.
      String sectionUrl =
	  getConfigReadOnlySectionMap().get(canonicalSectionName);
      if (log.isDebugEnabled()) log.debug("sectionUrl = " + sectionUrl);

      // Check whether no read-only configuration file was found.
      if (sectionUrl == null) {
	// Yes: Try to get the name of the writable configuration file to be
	// returned.
	sectionUrl = new File(configManager.getCacheConfigDir(),
	    configWritableSectionMap.get(canonicalSectionName)).toString();
	if (log.isDebugEnabled()) log.debug("sectionUrl = " + sectionUrl);
      }

      // Read the file.
      InputStream is = null;
      String partContent = null;

      try {
	is = configManager.getCacheConfigFileInputStream(sectionUrl);
	partContent = StringUtil.fromInputStream(is);
	if (log.isDebugEnabled())
	  log.debug("partContent = '" + partContent + "'");
      } catch (FileNotFoundException fnfe) {
	String message =
	    "Can't get the content for sectionName '" + sectionName + "'";
	log.error(message, fnfe);
	return new ResponseEntity<String>(message, HttpStatus.NOT_FOUND);
      } catch (IOException ioe) {
	String message = "Can't get the content for URL '" + sectionUrl + "'";
	log.error(message, ioe);
	return new ResponseEntity<String>(message,
	    HttpStatus.INTERNAL_SERVER_ERROR);
      } finally {
	IOUtil.safeClose(is);
      }

      // Get the file last modification timestamp.
      lastModified = getConfigFileLastModifiedAsEtag(sectionUrl);
      if (log.isDebugEnabled()) log.debug("lastModified = " + lastModified);

      // Check whether the modification timestamp matches the passed eTag.
      if (eTag != null && eTag.equals(lastModified)) {
	// Yes: Return.
	return new ResponseEntity<MultiValueMap<String, Object>>(null, null,
	    HttpStatus.NOT_MODIFIED);
      }

      // Save the file last modification timestamp in the response.
      partHeaders.setETag(lastModified);
      if (log.isDebugEnabled()) log.debug("partHeaders = " + partHeaders);

      // Set the returned content type.
      if (sectionUrl.toLowerCase().endsWith(".xml")) {
	partHeaders.setContentType(MediaType.TEXT_XML);
      } else {
	partHeaders.setContentType(MediaType.TEXT_PLAIN);
      }

      // Build the response entity.
      MultiValueMap<String, Object> parts =
	  new LinkedMultiValueMap<String, Object>();

      parts.add("config-data",
	  new HttpEntity<String>(partContent, partHeaders));
      if (log.isDebugEnabled()) log.debug("parts = " + parts);

      // Specify the response content type.
      HttpHeaders responseHeaders = new HttpHeaders();
      responseHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);
      if (log.isDebugEnabled())
	log.debug("responseHeaders = " + responseHeaders);

      return new ResponseEntity<MultiValueMap<String, Object>>(parts,
	  responseHeaders, HttpStatus.OK);
    } catch (Exception e) {
      String message =
	  "Cannot getSectionConfig() for sectionName = '" + sectionName + "'";
      log.error(message, e);
      return new ResponseEntity<String>(message,
	  HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Provides the configuration file for a given URL.
   * 
   * @param url
   *          A String with the url.
   * @param accept
   *          A String with the value of the "Accept" request header.
   * @param eTag
   *          A String with a value equivalent to the "If-Modified-Since"
   *          request header but with a granularity of 1 ms.
   * @return a {@code ResponseEntity<MultiValueMap<String, Object>>} with the
   *         section configuration file.
   */
  @Override
  @RequestMapping(value = "/config/url",
  produces = { "multipart/form-data", "application/json" },
  method = RequestMethod.GET)
  public ResponseEntity<?> getUrlConfig(
      @RequestParam("url") String url,
      @RequestHeader(value=HttpHeaders.ACCEPT, required=true) String accept,
      @RequestHeader(value=HttpHeaders.ETAG, required=false) String eTag) {
    if (log.isDebugEnabled()) {
      log.debug("url = " + url);
      log.debug("accept = " + accept);
      log.debug("eTag = " + eTag);
    }

    try {
      // Check whether the request did not specify the appropriate "Accept"
      // header.
      if (accept.indexOf(MediaType.MULTIPART_FORM_DATA_VALUE) < 0) {
	// Yes: Report the problem.
	String message = "Accept header does not include '"
	    + MediaType.MULTIPART_FORM_DATA_VALUE + "'";
	log.warn(message);
	return new ResponseEntity<String>(message, HttpStatus.NOT_ACCEPTABLE);
      }

      ConfigManager configManager = getConfigManager();
      String lastModified = null;
      HttpHeaders partHeaders = new HttpHeaders();

      // Read the file.
      InputStream is = null;
      String partContent = null;

      try {
	is = configManager.getCacheConfigFileInputStream(url);
	partContent = StringUtil.fromInputStream(is);
	if (log.isDebugEnabled())
	  log.debug("partContent = '" + partContent + "'");
      } catch (FileNotFoundException fnfe) {
	String message = "Can't get the content for url '" + url + "'";
	log.error(message, fnfe);
	return new ResponseEntity<String>(message, HttpStatus.NOT_FOUND);
      } catch (UnknownHostException uhe) {
	String message = "Can't get the content for url '" + url + "'";
	log.error(message, uhe);
	return new ResponseEntity<String>(message, HttpStatus.NOT_FOUND);
      } catch (ConnectException ce) {
	String message = "Can't get the content for url '" + url + "'";
	log.error(message, ce);
	return new ResponseEntity<String>(message, HttpStatus.NOT_FOUND);
      } catch (IOException ioe) {
	String message = "Can't get the content for URL '" + url + "'";
	log.error(message, ioe);
	return new ResponseEntity<String>(message,
	    HttpStatus.INTERNAL_SERVER_ERROR);
      } finally {
	IOUtil.safeClose(is);
      }

      // Get the file last modification timestamp.
      lastModified = getConfigFileLastModifiedAsEtag(url);
      if (log.isDebugEnabled()) log.debug("lastModified = " + lastModified);

      // Check whether the modification timestamp matches the passed eTag.
      if (eTag != null && eTag.equals(lastModified)) {
	// Yes: Return.
	return new ResponseEntity<MultiValueMap<String, Object>>(null, null,
	    HttpStatus.NOT_MODIFIED);
      }

      // Save the file last modification timestamp in the response.
      partHeaders.setETag(lastModified);
      if (log.isDebugEnabled()) log.debug("partHeaders = " + partHeaders);

      // Set the returned content type.
      if (url.toLowerCase().endsWith(".xml")) {
	partHeaders.setContentType(MediaType.TEXT_XML);
      } else {
	partHeaders.setContentType(MediaType.TEXT_PLAIN);
      }

      // Build the response entity.
      MultiValueMap<String, Object> parts =
	  new LinkedMultiValueMap<String, Object>();

      parts.add("config-data",
	  new HttpEntity<String>(partContent, partHeaders));
      if (log.isDebugEnabled()) log.debug("parts = " + parts);

      // Specify the response content type.
      HttpHeaders responseHeaders = new HttpHeaders();
      responseHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);
      if (log.isDebugEnabled())
	log.debug("responseHeaders = " + responseHeaders);

      return new ResponseEntity<MultiValueMap<String, Object>>(parts,
	  responseHeaders, HttpStatus.OK);
    } catch (Exception e) {
      String message = "Cannot getUrlConfig() for url = '" + url + "'";
      log.error(message, e);
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
  @RequestMapping(value = "/config/lastupdatetime",
  produces = { "application/json" }, method = RequestMethod.GET)
  public ResponseEntity<?> getLastUpdateTime() {
    if (log.isDebugEnabled()) log.debug("Invoked");

    try {
      long millis = getConfigManager().getLastUpdateTime();
      if (log.isDebugEnabled()) log.debug("millis = " + millis);

      Date result = new Date(millis);
      if (log.isDebugEnabled()) log.debug("result = " + result);
      return new ResponseEntity<Date>(result, HttpStatus.OK);
    } catch (Exception e) {
      String message = "Cannot getLastUpdateTime()";
      log.error(message, e);
      return new ResponseEntity<String>(message,
	  HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Provides the URLs from which the configuration was loaded.
   * 
   * @return a {@code ResponseEntity<List<String>>} with the URLs.
   */
  @Override
  @RequestMapping(value = "/config/loadedurls",
  produces = { "application/json" }, method = RequestMethod.GET)
  public ResponseEntity<?> getLoadedUrlList() {
    if (log.isDebugEnabled()) log.debug("Invoked");

    try {
      @SuppressWarnings("unchecked")
      List<String> result = (List<String>)getConfigManager().getLoadedUrlList();
      if (log.isDebugEnabled()) log.debug("result = " + result);
      return new ResponseEntity<List<String>>(result, HttpStatus.OK);
    } catch (Exception e) {
      String message = "Cannot getLoadedUrlList()";
      log.error(message, e);
      return new ResponseEntity<String>(message,
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
   * @param eTag
   *          A String with a value equivalent to the "If-Unmodified-Since"
   *          request header but with a granularity of 1 ms.
   * @return a {@code ResponseEntity<Void>}.
   */
  @Override
  @RequestMapping(value = "/config/file/{sectionName}",
  consumes = { "multipart/form-data" }, produces = { "application/json" },
  method = RequestMethod.PUT)
  public ResponseEntity<?> putConfig(
      @PathVariable("sectionName") String sectionName,
      @ApiParam(required=true) @RequestParam("config-data") MultipartFile
      configFile,
      @RequestHeader(value=HttpHeaders.ETAG, required=true) String eTag) {
    if (log.isDebugEnabled()) {
      log.debug("sectionName = " + sectionName);
      log.debug("configFile = " + configFile);
      log.debug("eTag = " + eTag);
    }

    // Check authorization.
    try {
      SpringAuthenticationFilter.checkAuthorization(Roles.ROLE_USER_ADMIN);
    } catch (AccessControlException ace) {
      log.warn(ace.getMessage());
      return new ResponseEntity<String>(ace.getMessage(), HttpStatus.FORBIDDEN);
    }

    if (configFile == null) {
      String message = "Invalid metadata modification specification: null";
      log.warn(message);
      return new ResponseEntity<String>(message, HttpStatus.BAD_REQUEST);
    }

    // Validate the name of the section to be obtained.
    String canonicalSectionName = null;

    try {
	canonicalSectionName = validateSectionName(sectionName, false);
	if (log.isDebugEnabled())
	  log.debug("canonicalSectionName = " + canonicalSectionName);
    } catch (MalformedParametersException mpe) {
	return new ResponseEntity<String>(mpe.getMessage(),
	    HttpStatus.BAD_REQUEST);
    }

    try {
      ConfigManager configManager = getConfigManager();

      // Get the name of the file to be stored.
      String sectionUrl = configWritableSectionMap.get(canonicalSectionName);
      if (log.isDebugEnabled()) log.debug("sectionUrl = " + sectionUrl);

      // Check whether the file has been modified since the passed cutoff
      // timestamp.
      if (!eTag.equals(getConfigFileLastModifiedAsEtag(
	  new File(configManager.getCacheConfigDir(), sectionUrl).toString())))
      {
	// Yes: Return.
	return new ResponseEntity<Void>(HttpStatus.PRECONDITION_FAILED);
      }

      // Write the file.
      String textToWrite =
	  StringUtil.fromInputStream(configFile.getInputStream());
      if (log.isDebugEnabled())
	log.debug("textToWrite = '" + textToWrite + "'");

      configManager.writeCacheConfigFile(textToWrite, sectionUrl,
	  false);

      return new ResponseEntity<Void>(HttpStatus.OK);
    } catch (Exception e) {
      String message = "Cannot putConfig() for sectionName = '" + sectionName
	  + "', configFile = '" + configFile + "'";
      log.error(message, e);
      return new ResponseEntity<String>(message,
	  HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Requests a reload of the configuration.
   * 
   * @return a {@code ResponseEntity<Void>} with the status.
   */
  @Override
  @RequestMapping(value = "/config/reload", produces = { "application/json" },
  method = RequestMethod.PUT)
  public ResponseEntity<?> putConfigReload() {
    if (log.isDebugEnabled()) log.debug("Invoked");

    // Check authorization.
    try {
      SpringAuthenticationFilter.checkAuthorization(Roles.ROLE_USER_ADMIN);
    } catch (AccessControlException ace) {
      log.warn(ace.getMessage());
      return new ResponseEntity<String>(ace.getMessage(), HttpStatus.FORBIDDEN);
    }

    try {
      getConfigManager().requestReload();
      return new ResponseEntity<Void>(HttpStatus.OK);
    } catch (Exception e) {
      String message = "Cannot requestReload()";
      log.error(message, e);
      return new ResponseEntity<String>(message,
	  HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  private static final String API_VERSION = "1.0.0";

  /**
   * Provides the status object.
   * 
   * @return an ApiStatus with the status.
   */
  @Override
  public ApiStatus getApiStatus() {
    return new ApiStatus()
      .setVersion(API_VERSION)
      .setReady(LockssApp.getLockssApp().isAppRunning());
  }

  /**
   * Provides a validated canonical version of the passed section name.
   * 
   * @param sectionName
   *          A String with the section name.
   * @param forReading
   *          A boolean indicating whether this is for a reading operation.
   * @return a String with the validated canonical version of the section name.
   * @throws MalformedParametersException
   *           if validation fails.
   */
  protected String validateSectionName(String sectionName, boolean forReading)
      throws MalformedParametersException {
    if (log.isDebugEnabled()) {
      log.debug("sectionName = " + sectionName);
      log.debug("forReading = " + forReading);
    }

    if (sectionName == null || sectionName.isEmpty()) {
      String message = "Invalid sectionName '" + sectionName + "'";
      log.warn(message);
      throw new MalformedParametersException(message);
    }

    String canonicalVersion = sectionName.toLowerCase();
    if (log.isDebugEnabled())
      log.debug("canonicalVersion = " + canonicalVersion);

    if ((!forReading
	|| !getConfigReadOnlySectionMap().containsKey(canonicalVersion))
	&& !configWritableSectionMap.containsKey(canonicalVersion)) {
      String message = "Invalid sectionName '" + sectionName + "'";
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
      configReadOnlySectionMap = new HashMap<String, String>();

      configReadOnlySectionMap.put(SECTION_NAME_CLUSTER, "cluster.xml");
      configReadOnlySectionMap.put(SECTION_NAME_PROPSLOCKSS,
	  CurrentConfig.getCurrentConfig().get(PARAM_PROPS_LOCKSS_XML_URL));

      if (log.isDebugEnabled())
	log.debug("configReadOnlySectionMap = " + configReadOnlySectionMap);
    }

    return configReadOnlySectionMap;
  }

  /**
   * Provides the configuration manager.
   *
   * @return a ConfigManager with the configuration manager.
   */
  private ConfigManager getConfigManager() {
    return ConfigManager.getConfigManager();
  }

  /**
   * Provides the last modification time of a configuration file.
   * 
   * @param sectionUrl
   *          A String with the section URL of the configuration file.
   * @return a long with the last modification time of a configuration file.
   */
  private String getConfigFileLastModifiedAsEtag(String sectionUrl) {
    if (log.isDebugEnabled()) log.debug("sectionUrl = " + sectionUrl);

    // Get the cached configuration file.
    ConfigFile cacheConfigFile =
	getConfigManager().getConfigCache().find(sectionUrl);
    if (log.isDebugEnabled()) log.debug("cacheConfigFile = " + cacheConfigFile);

    // Get the cached configuration file last modification time.
    String lastModified = cacheConfigFile.getLastModified();
    if (log.isDebugEnabled()) log.debug("lastModified = " + lastModified);

    // Format it as an ETag.
    if (lastModified == null) {
      lastModified = "\"0\"";
    } else {
      lastModified = "\"" + lastModified + "\"";
    }

    if (log.isDebugEnabled()) log.debug("lastModified = " + lastModified);
    return lastModified;
  }
}
