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
import java.io.FileNotFoundException;
import java.lang.reflect.MalformedParametersException;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;
import org.lockss.alert.AlertManagerImpl;
import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.daemon.Cron;
import org.lockss.laaws.config.model.ConfigExchange;
import org.lockss.laaws.config.model.ConfigModSpec;
import org.lockss.laaws.config.security.SpringAuthenticationFilter;
import org.lockss.rs.auth.Roles;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

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
   * Deletes the configuration for a section given the section name.
   * 
   * @param sectionName
   *          A String with the section name.
   * @return ResponseEntity<ConfigExchange> with the deleted configuration.
   */
  @Override
  @RequestMapping(value = "/config/{sectionName}",
  produces = { "application/json" }, consumes = { "application/json" },
  method = RequestMethod.DELETE)
  public ResponseEntity<ConfigExchange> deleteConfig(
      @PathVariable("sectionName") String sectionName) {
    if (log.isDebugEnabled()) log.debug("sectionName = " + sectionName);

    SpringAuthenticationFilter.checkAuthorization(Roles.ROLE_USER_ADMIN);

    String cacheConfigFileName =
	configSectionMap.get(validateSectionName(sectionName, false));
    if (log.isDebugEnabled())
      log.debug("cacheConfigFileName = " + cacheConfigFileName);

    try {
      ConfigManager configManager = ConfigManager.getConfigManager();
      Configuration config = null;

      try {
	config = configManager.readCacheConfigFile(cacheConfigFileName);
      } catch (FileNotFoundException fnfe) {
	config = ConfigManager.newConfiguration();
      }

      configManager.deleteCacheConfigFile(cacheConfigFileName);

      ConfigExchange result = convertConfig(config);
      if (log.isDebugEnabled()) log.debug("result = " + result);
      return new ResponseEntity<ConfigExchange>(result, HttpStatus.OK);
    } catch (Exception e) {
      String message =
	  "Cannot deleteConfig() for sectionName = '" + sectionName + "'";
      log.error(message, e);
      throw new RuntimeException(message);
    }
  }

  /**
   * Provides the configuration for a section given the section name.
   * 
   * @param sectionName
   *          A String with the section name.
   * @return a ResponseEntity<ConfigExchange> with the section configuration.
   */
  @Override
  @RequestMapping(value = "/config/{sectionName}",
  produces = { "application/json" }, consumes = { "application/json" },
  method = RequestMethod.GET)
  public ResponseEntity<ConfigExchange> getConfig(@PathVariable("sectionName")
  String sectionName) {
    if (log.isDebugEnabled()) log.debug("sectionName = " + sectionName);

    String canonicalSectionName = validateSectionName(sectionName, true);
    if (log.isDebugEnabled())
      log.debug("canonicalSectionName = " + canonicalSectionName);

    try {
      Configuration config = null;

      if (SECTION_NAME_CLUSTER.equals(canonicalSectionName)) {
	config = ConfigManager.getCurrentConfig();
      } else {
	try {
	  config = ConfigManager.getConfigManager()
	      .readCacheConfigFile(configSectionMap.get(canonicalSectionName));
	} catch (FileNotFoundException fnfe) {
	  config = ConfigManager.newConfiguration();
	}
      }

      ConfigExchange result = convertConfig(config);
      if (log.isDebugEnabled()) log.debug("result = " + result);
      return new ResponseEntity<ConfigExchange>(result, HttpStatus.OK);
    } catch (Exception e) {
      String message =
	  "Cannot getConfig() for sectionName = '" + sectionName + "'";
      log.error(message, e);
      throw new RuntimeException(message);
    }
  }

  /**
   * Provides the timestamp of the last time the configuration was updated.
   * 
   * @return a ResponseEntity<Date> with the timestamp.
   */
  @Override
  @RequestMapping(value = "/config/lastupdatetime",
  produces = { "application/json" }, consumes = { "application/json" },
  method = RequestMethod.GET)
  public ResponseEntity<Date> getLastUpdateTime() {
    if (log.isDebugEnabled()) log.debug("Invoked");

    try {
      long millis = ConfigManager.getConfigManager().getLastUpdateTime();
      if (log.isDebugEnabled()) log.debug("millis = " + millis);

      Date result = new Date(millis);
      if (log.isDebugEnabled()) log.debug("result = " + result);
      return new ResponseEntity<Date>(result, HttpStatus.OK);
    } catch (Exception e) {
      String message = "Cannot getLastUpdateTime()";
      log.error(message, e);
      throw new RuntimeException(message);
    }
  }

  /**
   * Provides the URLs from which the configuration was loaded.
   * 
   * @return a ResponseEntity<String> with the URLs.
   */
  @Override
  @RequestMapping(value = "/config/loadedurls",
  produces = { "application/json" }, consumes = { "application/json" },
  method = RequestMethod.GET)
  public ResponseEntity<List<String>> getLoadedUrlList() {
    if (log.isDebugEnabled()) log.debug("Invoked");

    try {
      @SuppressWarnings("unchecked")
      List<String> result =
	  (List<String>)ConfigManager.getConfigManager().getLoadedUrlList();
      if (log.isDebugEnabled()) log.debug("result = " + result);
      return new ResponseEntity<List<String>>(result, HttpStatus.OK);
    } catch (Exception e) {
      String message = "Cannot getLoadedUrlList()";
      log.error(message, e);
      throw new RuntimeException(message);
    }
  }

  /**
   * Modifies the configuration for a section given the section name.
   * 
   * @param sectionName
   *          A String with the section name.
   * @param configModSpec
   *          A ConfigModSpec with the configuration modifications.
   * @return a ResponseEntity<ConfigExchange> with the section configuration.
   */
  @Override
  @RequestMapping(value = "/config/{sectionName}",
  produces = { "application/json" }, consumes = { "application/json" },
  method = RequestMethod.PUT)
  public ResponseEntity<ConfigExchange> putConfig(
      @PathVariable("sectionName") String sectionName,
      @ApiParam(required=true) @RequestBody ConfigModSpec configModSpec) {
    if (log.isDebugEnabled()) log.debug("sectionName = " + sectionName
	+ ", configModSpec = " + configModSpec);

    SpringAuthenticationFilter.checkAuthorization(Roles.ROLE_USER_ADMIN);

    String canonicalSectionName = validateSectionName(sectionName, true);
    if (log.isDebugEnabled())
      log.debug("canonicalSectionName = " + canonicalSectionName);

    try {
      if (configModSpec == null) {
	String message = "Invalid metadata modification specification: null";
	log.error(message);
        throw new MalformedParametersException(message);
      }

      Configuration updateConfig = ConfigManager.newConfiguration();
      Map<String, String> updateMap = configModSpec.getUpdates();

      if (updateMap != null && !updateMap.isEmpty()) {
	for (String key : updateMap.keySet()) {
	  updateConfig.put(key, updateMap.get(key));
	}
      }

      if (log.isDebugEnabled()) log.debug("updateConfig = " + updateConfig);

      List<String> deleteList = configModSpec.getDeletes();

      if (deleteList == null) {
	deleteList = new ArrayList<String>();
      } else if (!deleteList.isEmpty()) {
	for (String key : deleteList) {
	  if (updateConfig.containsKey(key)) {
	    String message = "The key '" + key
		+ "' appears in both the update set and in the delete set";
	    log.error(message);
            throw new MalformedParametersException(message);
	  }
	}
      }

      if (log.isDebugEnabled()) log.debug("deleteList = " + deleteList);

      ConfigManager configManager = ConfigManager.getConfigManager();
      Configuration resultConfig = null;

      if (SECTION_NAME_CLUSTER.equals(canonicalSectionName)) {
	resultConfig = ConfigManager.newConfiguration();
	resultConfig.copyFrom(ConfigManager.getCurrentConfig());

	resultConfig.copyFrom(updateConfig);

	for (String key : deleteList) {
	  resultConfig.remove(key);
	}

	ConfigManager.getConfigManager().setCurrentConfig(resultConfig);

	resultConfig = ConfigManager.getCurrentConfig();
      } else {
	String cacheConfigFileName = configSectionMap.get(canonicalSectionName);
	if (log.isDebugEnabled())
	  log.debug("cacheConfigFileName = " + cacheConfigFileName);

	configManager.modifyCacheConfigFile(updateConfig,
	    new HashSet<String>(deleteList), cacheConfigFileName,
	    configModSpec.getHeader());

	resultConfig = configManager.readCacheConfigFile(cacheConfigFileName);
      }

      ConfigExchange result = convertConfig(resultConfig);
      if (log.isDebugEnabled()) log.debug("result = " + result);
      return new ResponseEntity<ConfigExchange>(result, HttpStatus.OK);
    } catch (Exception e) {
      String message = "Cannot putConfig() for sectionName = '" + sectionName
	  + "', configModSpec = '" + configModSpec + "'";
      log.error(message, e);
      throw new RuntimeException(message);
    }
  }

  /**
   * Requests a reload of the configuration.
   * 
   * @return a ResponseEntity<void> with the status.
   */
  @Override
  @RequestMapping(value = "/config/reload",
  produces = { "application/json" }, consumes = { "application/json" },
  method = RequestMethod.PUT)
  public ResponseEntity<Void> putConfigReload() {
    if (log.isDebugEnabled()) log.debug("Invoked");

    SpringAuthenticationFilter.checkAuthorization(Roles.ROLE_USER_ADMIN);

    try {
      ConfigManager.getConfigManager().requestReload();
      return new ResponseEntity<Void>(HttpStatus.OK);
    } catch (Exception e) {
      String message = "Cannot requestReload()";
      log.error(message, e);
      throw new RuntimeException(message);
    }
  }

  @ExceptionHandler(AccessControlException.class)
  @ResponseStatus(HttpStatus.FORBIDDEN)
  public ErrorResponse authorizationExceptionHandler(AccessControlException e) {
    return new ErrorResponse(e.getMessage()); 	
  }

  @ExceptionHandler(MalformedParametersException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ErrorResponse authorizationExceptionHandler(
      MalformedParametersException e) {
    return new ErrorResponse(e.getMessage()); 	
  }

  @ExceptionHandler(IllegalArgumentException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public ErrorResponse notFoundExceptionHandler(IllegalArgumentException e) {
    return new ErrorResponse(e.getMessage()); 	
  }

  @ExceptionHandler(RuntimeException.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public ErrorResponse internalExceptionHandler(RuntimeException e) {
    return new ErrorResponse(e.getMessage()); 	
  }

  /**
   * Provides a validated canonical version of the passed section name.
   * 
   * @param sectionName
   *          A String with the section name.
   * @param clusterAllowed
   *          A boolean indication whether the cluster "section" is valid or
   *          not.
   * @return a String with the validated canonical version of the section name.
   * @throws RuntimeException
   *           if validation fails.
   */
  private String validateSectionName(String sectionName,
      boolean clusterAllowed) throws MalformedParametersException {
    if (sectionName == null || sectionName.isEmpty()) {
      String message = "Invalid sectionName = '" + sectionName + "'";
      log.error(message);
      throw new MalformedParametersException(message);
    }

    String canonicalVersion = sectionName.toLowerCase();

    if ((!clusterAllowed || !SECTION_NAME_CLUSTER.equals(canonicalVersion))
	&& configSectionMap.get(canonicalVersion) == null) {
      String message = "Invalid section name '" + sectionName + "'";
      log.error(message);
      throw new MalformedParametersException(message);
    }

    return canonicalVersion;
  }

  /**
   * Converts a LOCKSS configuration object into an object ready to be
   * transmitted.
   * 
   * @param config
   *          A Configuration object to be transmitted.
   * @return a ConfigExchange object with the converted Configuration.
   */
  private ConfigExchange convertConfig(Configuration config) {
    if (log.isDebugEnabled()) log.debug("config = " + config);

    ConfigExchange result = new ConfigExchange();
    Map<String, String> props = new HashMap<String, String>();

    for (String key : config.keySet()) {
      String value = config.get(key);
      props.put(key,  value);
    }

    result.setProps(props);

    if (log.isDebugEnabled()) log.debug("result = " + result);
    return result;
  }
}
