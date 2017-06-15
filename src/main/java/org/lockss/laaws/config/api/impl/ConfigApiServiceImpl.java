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
package org.lockss.laaws.config.api.impl;

import static org.lockss.config.ConfigManager.*;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import org.apache.log4j.Logger;
import org.lockss.alert.AlertManagerImpl;
import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.daemon.Cron;
import org.lockss.laaws.config.api.ApiException;
import org.lockss.laaws.config.api.ConfigApiService;
import org.lockss.laaws.config.model.ConfigExchange;
import org.lockss.laaws.config.model.ConfigModSpec;

/**
 * Implementation of the base provider of access to the configuration.
 */
public class ConfigApiServiceImpl extends ConfigApiService {
  private static Logger log = Logger.getLogger(ConfigApiServiceImpl.class);

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
   * @param securityContext
   *          A SecurityContext providing access to security related
   *          information.
   * @return a Response with any data that needs to be returned to the runtime.
   * @throws ApiException
   *           if there are problems.
   */
  @Override
  public Response deleteConfig(String sectionName,
      SecurityContext securityContext) throws ApiException {
    if (log.isDebugEnabled()) log.debug("sectionName = " + sectionName);

    String cacheConfigFileName = null;

    try {
      cacheConfigFileName =
	  configSectionMap.get(validateSectionName(sectionName, false));
      if (log.isDebugEnabled())
	log.debug("cacheConfigFileName = " + cacheConfigFileName);
    } catch (Exception e) {
      return getErrorResponse(Response.Status.BAD_REQUEST, e.getMessage());
    }

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
      return Response.ok().entity(result).build();
    } catch (Exception e) {
      String message =
	  "Cannot deleteConfig() for sectionName = '" + sectionName + "'";
      log.error(message, e);
      return getErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, message);
    }
  }

  /**
   * Provides the configuration for a section given the section name.
   * 
   * @param sectionName
   *          A String with the section name.
   * @param securityContext
   *          A SecurityContext providing access to security related
   *          information.
   * @return a Response with any data that needs to be returned to the runtime.
   * @throws ApiException
   *           if there are problems.
   */
  @Override
  public Response getConfig(String sectionName, SecurityContext securityContext)
      throws ApiException {
    if (log.isDebugEnabled()) log.debug("sectionName = " + sectionName);

    String canonicalSectionName = null;

    try {
      canonicalSectionName = validateSectionName(sectionName, true);
      if (log.isDebugEnabled())
	log.debug("canonicalSectionName = " + canonicalSectionName);
    } catch (Exception e) {
      return getErrorResponse(Response.Status.BAD_REQUEST, e.getMessage());
    }

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
      return Response.ok().entity(result).build();
    } catch (Exception e) {
      String message =
	  "Cannot getConfig() for sectionName = '" + sectionName + "'";
      log.error(message, e);
      return getErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, message);
    }
  }

  /**
   * Provides the timestamp of the last time the configuration was updated.
   * 
   * @param securityContext
   *          A SecurityContext providing access to security related
   *          information.
   * @return a Response with any data that needs to be returned to the runtime.
   * @throws ApiException
   *           if there are problems.
   */
  @Override
  public Response getLastUpdateTime(SecurityContext securityContext)
      throws ApiException {
    if (log.isDebugEnabled()) log.debug("Invoked");

    try {
      long millis = ConfigManager.getConfigManager().getLastUpdateTime();
      if (log.isDebugEnabled()) log.debug("millis = " + millis);

      Date result = new Date(millis);
      if (log.isDebugEnabled()) log.debug("result = " + result);
      return Response.ok().entity(result).build();
    } catch (Exception e) {
      String message = "Cannot getLastUpdateTime()";
      log.error(message, e);
      return getErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, message);
    }
  }

  /**
   * Provides the URLs from which the configuration was loaded.
   * 
   * @param securityContext
   *          A SecurityContext providing access to security related
   *          information.
   * @return a Response with any data that needs to be returned to the runtime.
   * @throws ApiException
   *           if there are problems.
   */
  @Override
  public Response getLoadedUrlList(SecurityContext securityContext)
      throws ApiException {
    if (log.isDebugEnabled()) log.debug("Invoked");

    try {
      List<String> result =
	  (List<String>)ConfigManager.getConfigManager().getLoadedUrlList();
      if (log.isDebugEnabled()) log.debug("result = " + result);
      return Response.ok().entity(result).build();
    } catch (Exception e) {
      String message = "Cannot getLoadedUrlList()";
      log.error(message, e);
      return getErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, message);
    }
  }

  /**
   * Modifies the configuration for a section given the section name.
   * 
   * @param sectionName
   *          A String with the section name.
   * @param configModSpec
   *          A ConfigModSpec with the configuration modifications.
   * @param securityContext
   *          A SecurityContext providing access to security related
   *          information.
   * @return a Response with any data that needs to be returned to the runtime.
   * @throws ApiException
   *           if there are problems.
   */
  @Override
  public Response putConfig(String sectionName, ConfigModSpec configModSpec,
      SecurityContext securityContext) throws ApiException {
    if (log.isDebugEnabled()) log.debug("sectionName = " + sectionName
	+ ", configModSpec = " + configModSpec);

    String canonicalSectionName = null;

    try {
      canonicalSectionName = validateSectionName(sectionName, true);
      if (log.isDebugEnabled())
	log.debug("canonicalSectionName = " + canonicalSectionName);
    } catch (Exception e) {
      return getErrorResponse(Response.Status.BAD_REQUEST, e.getMessage());
    }

    try {
      if (configModSpec == null) {
	String message = "Invalid metadata modification specification: null";
	log.error(message);
	return getErrorResponse(Response.Status.BAD_REQUEST, message);
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
	    return getErrorResponse(Response.Status.BAD_REQUEST, message);
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
      return Response.ok().entity(result).build();
    } catch (Exception e) {
      String message = "Cannot putConfig() for sectionName = '" + sectionName
	  + "', configModSpec = '" + configModSpec + "'";
      log.error(message, e);
      return getErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, message);
    }
  }

  /**
   * Requests a reload of the configuration.
   * 
   * @param securityContext
   *          A SecurityContext providing access to security related
   *          information.
   * @return a Response with any data that needs to be returned to the runtime.
   * @throws ApiException
   *           if there are problems.
   */
  @Override
  public Response requestReload(SecurityContext securityContext)
      throws ApiException {
    if (log.isDebugEnabled()) log.debug("Invoked");

    try {
      ConfigManager.getConfigManager().requestReload();
      return Response.ok().entity(toJsonMessage("Done")).build();
    } catch (Exception e) {
      String message = "Cannot requestReload()";
      log.error(message, e);
      return getErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, message);
    }
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
      boolean clusterAllowed) throws RuntimeException {
    if (sectionName == null || sectionName.isEmpty()) {
      String message = "Invalid sectionName = '" + sectionName + "'";
      log.error(message);
      throw new RuntimeException(message);
    }

    String canonicalVersion = sectionName.toLowerCase();

    if ((!clusterAllowed || !SECTION_NAME_CLUSTER.equals(canonicalVersion))
	&& configSectionMap.get(canonicalVersion) == null) {
      String message = "Invalid section name '" + sectionName + "'";
      log.error(message);
      throw new RuntimeException(message);
    }

    return canonicalVersion;
  }

  /**
   * Provides the appropriate response in case of an error.
   * 
   * @param statusCode
   *          A Response.Status with the error status code.
   * @param message
   *          A String with the error message.
   * @return a Response with the error response.
   */
  private Response getErrorResponse(Response.Status status, String message) {
    return Response.status(status).entity(toJsonMessage(message)).build();
  }

  /**
   * Formats to JSON any message to be returned.
   * 
   * @param message
   *          A String with the message to be formatted.
   * @return a String with the JSON-formatted message.
   */
  private String toJsonMessage(String message) {
    return "{\"message\":\"" + message + "\"}"; 
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
