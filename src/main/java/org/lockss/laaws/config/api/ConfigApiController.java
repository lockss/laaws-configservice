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

import static org.lockss.config.ConfigManager.*;
import io.swagger.annotations.ApiParam;
import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.MalformedParametersException;
import java.security.AccessControlException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;
import org.lockss.alert.AlertManagerImpl;
import org.lockss.config.ConfigFile;
import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.daemon.Cron;
import org.lockss.laaws.config.security.SpringAuthenticationFilter;
import org.lockss.rs.auth.Roles;
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
public class ConfigApiController implements ConfigApi {
  private static Logger log = Logger.getLogger(ConfigApiController.class);

  @SuppressWarnings("serial")
  private static final Map<String, String> configSectionMap =
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
   * @return a ResponseEntity<MultiValueMap<String, Object>> with the section
   *         configuration file.
   */
  @Override
  @RequestMapping(value = "/config/file/{sectionName}",
  produces = { "multipart/form-data", "application/json" },
  method = RequestMethod.GET)
  public ResponseEntity<?> getConfig(
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

      ConfigManager configManager = getConfigManager();
      StringBuilder sb = new StringBuilder();
      String lastModified = null;
      HttpHeaders partHeaders = new HttpHeaders();

      // Check whether it's the virtual section "cluster".
      if (SECTION_NAME_CLUSTER.equals(canonicalSectionName)) {
	// Yes: Build it.
	sb.append("<lockss-config>").append(System.lineSeparator())
	.append("<property name=\"org.lockss.auxPropUrls\">")
	.append(System.lineSeparator())
	.append("<list append=\"false\">").append(System.lineSeparator())
	.append("<value>").append("/config/file/")
	.append(SECTION_NAME_PROXY_IP_ACCESS).append("</value>")
	.append(System.lineSeparator());

	lastModified = "0";
	String partialLastModified =
	    getConfigFileLastModifiedAsEtag(SECTION_NAME_PROXY_IP_ACCESS);
        if (log.isDebugEnabled())
          log.debug("partialLastModified = " + partialLastModified);
	partialLastModified =
	    partialLastModified.substring(1, partialLastModified.length()-1);
        if (log.isDebugEnabled())
          log.debug("partialLastModified = " + partialLastModified);

        if (Long.parseLong(partialLastModified) > Long.parseLong(lastModified))
        {
          lastModified = partialLastModified;
        }

        if (log.isDebugEnabled()) log.debug("lastModified = " + lastModified);

        sb.append("<value>").append("/config/file/").append(SECTION_NAME_EXPERT)
        .append("</value>").append(System.lineSeparator());

	partialLastModified =
	    getConfigFileLastModifiedAsEtag(SECTION_NAME_EXPERT);
        if (log.isDebugEnabled())
          log.debug("partialLastModified = " + partialLastModified);
	partialLastModified =
	    partialLastModified.substring(1, partialLastModified.length()-1);
        if (log.isDebugEnabled())
          log.debug("partialLastModified = " + partialLastModified);

        if (Long.parseLong(partialLastModified) > Long.parseLong(lastModified))
        {
          lastModified = partialLastModified;
        }

        if (log.isDebugEnabled()) log.debug("lastModified = " + lastModified);
        lastModified = "\"" + lastModified + "\"";
        if (log.isDebugEnabled()) log.debug("lastModified = " + lastModified);

        sb.append("</list>");
        sb.append("</property>");
        sb.append("</lockss-config>");

        // Check whether the modification timestamp matches the passed eTag.
        if (eTag != null && eTag.equals(lastModified)) {
	  // Yes: Return.
	  return new ResponseEntity<MultiValueMap<String, Object>>(null, null,
	      HttpStatus.NOT_MODIFIED);
	}

	// No: Set the returned content type.
	partHeaders.setContentType(MediaType.TEXT_XML);
      } else {
	// No: Get the name of the file to be returned.
	String filename = configSectionMap.get(canonicalSectionName);
	if (log.isDebugEnabled()) log.debug("filename = " + filename);

	// Get the file last modification timestamp.
        lastModified = getConfigFileLastModifiedAsEtag(filename);
        if (log.isDebugEnabled()) log.debug("lastModified = " + lastModified);

        // Check whether the modification timestamp matches the passed eTag.
	if (eTag != null && eTag.equals(lastModified)) {
	  // Yes: Return.
	  return new ResponseEntity<MultiValueMap<String, Object>>(null, null,
	      HttpStatus.NOT_MODIFIED);
	}

	// No: Set the returned content type.
	if (filename.toLowerCase().endsWith(".xml")) {
	  partHeaders.setContentType(MediaType.TEXT_XML);
	} else {
	  partHeaders.setContentType(MediaType.TEXT_PLAIN);
	}

	// TODO: Stream the file from disk.
	// Read the file.
	Configuration config = null;

	try {
	  config = configManager
	      .readCacheConfigFile(configSectionMap.get(canonicalSectionName));
	} catch (FileNotFoundException fnfe) {
	  config = ConfigManager.newConfiguration();
	}

	if (log.isDebugEnabled()) log.debug("config = " + config);

	for (String key : config.keySet()) {
	  sb.append(key).append("=").append(config.get(key))
	  .append(System.lineSeparator());
	}
      }

      // Check whether the request specified the appropriate "Accept" header.
      if (accept.indexOf(MediaType.MULTIPART_FORM_DATA_VALUE) >= 0) {
	// Yes: Save the file last modification timestamp in the response.
	partHeaders.setETag(lastModified);
	if (log.isDebugEnabled()) log.debug("partHeaders = " + partHeaders);

	// Specify the response content type.
	HttpHeaders responseHeaders = new HttpHeaders();
	responseHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);
	if (log.isDebugEnabled())
	  log.debug("responseHeaders = " + responseHeaders);

	// Build the response entity.
	String partContent = sb.toString();
	if (log.isDebugEnabled())
	  log.debug("partContent = '" + partContent + "'");

	MultiValueMap<String, Object> parts =
	    new LinkedMultiValueMap<String, Object>();

	parts.add("config-data",
	    new HttpEntity<String>(partContent, partHeaders));
	if (log.isDebugEnabled()) log.debug("parts = " + parts);

	return new ResponseEntity<MultiValueMap<String, Object>>(parts,
	    responseHeaders, HttpStatus.OK);
      } else {
	// No: Report the problem.
	String message = "Accept header does not include '"
	    + MediaType.MULTIPART_FORM_DATA_VALUE + "'";
	log.warn(message);
	return new ResponseEntity<String>(message, HttpStatus.NOT_ACCEPTABLE);
      }
    } catch (Exception e) {
      String message =
	  "Cannot getConfig() for sectionName = '" + sectionName + "'";
      log.error(message, e);
      return new ResponseEntity<String>(message,
	  HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Provides the timestamp of the last time the configuration was updated.
   * 
   * @return a ResponseEntity<Date> with the timestamp.
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
   * @return a ResponseEntity<List<String>> with the URLs.
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
   * @return a ResponseEntity<Void>.
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
      String cacheConfigFileName = configSectionMap.get(canonicalSectionName);
      if (log.isDebugEnabled())
	log.debug("cacheConfigFileName = " + cacheConfigFileName);

      // Check whether the file has been modified since the passed cutoff
      // timestamp.
      if (!eTag.equals(getConfigFileLastModifiedAsEtag(cacheConfigFileName))) {
	// Yes: Return.
	return new ResponseEntity<Void>(HttpStatus.PRECONDITION_FAILED);
      }

      // Write the file.
      String textToWrite =
	  StringUtil.fromInputStream(configFile.getInputStream());
      if (log.isDebugEnabled())
	log.debug("textToWrite = '" + textToWrite + "'");

      configManager.writeCacheConfigFile(textToWrite, cacheConfigFileName,
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
   * @return a ResponseEntity<Void> with the status.
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

  /**
   * Provides a validated canonical version of the passed section name.
   * 
   * @param sectionName
   *          A String with the section name.
   * @param clusterAllowed
   *          A boolean indicating whether the cluster "section" is valid or
   *          not.
   * @return a String with the validated canonical version of the section name.
   * @throws MalformedParametersException
   *           if validation fails.
   */
  private String validateSectionName(String sectionName,
      boolean clusterAllowed) throws MalformedParametersException {
    if (log.isDebugEnabled()) {
      log.debug("sectionName = " + sectionName);
      log.debug("clusterAllowed = " + clusterAllowed);
    }

    if (sectionName == null || sectionName.isEmpty()) {
      String message = "Invalid sectionName = '" + sectionName + "'";
      log.warn(message);
      throw new MalformedParametersException(message);
    }

    String canonicalVersion = sectionName.toLowerCase();

    if ((!clusterAllowed || !SECTION_NAME_CLUSTER.equals(canonicalVersion))
	&& configSectionMap.get(canonicalVersion) == null) {
      String message = "Invalid section name '" + sectionName + "'";
      log.warn(message);
      throw new MalformedParametersException(message);
    }

    if (log.isDebugEnabled())
      log.debug("canonicalVersion = " + canonicalVersion);
    return canonicalVersion;
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
   * @param cacheConfigFileName
   *          A String with the name of the configuration file.
   * @return a long with the last modification time of a configuration file.
   */
  private String getConfigFileLastModifiedAsEtag(String cacheConfigFileName) {
    if (log.isDebugEnabled())
      log.debug("cacheConfigFileName = " + cacheConfigFileName);
    ConfigManager configManager = getConfigManager();

    // Get the file path name.
    String fileName = new File(configManager.getCacheConfigDir(),
	cacheConfigFileName).toString();
    if (log.isDebugEnabled()) log.debug("fileName = " + fileName);

    // Get the cached configuration file.
    ConfigFile cacheConfigFile = configManager.getConfigCache().find(fileName);
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
