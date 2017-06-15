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

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import org.apache.log4j.Logger;
import org.lockss.app.LockssDaemon;
import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.laaws.config.api.ApiException;
import org.lockss.laaws.config.api.AusApiService;
import org.lockss.laaws.config.model.ConfigExchange;
import org.lockss.plugin.PluginManager;

/**
 * Implementation of the base provider of access to Archival Unit
 * configurations.
 */
public class AusApiServiceImpl extends AusApiService {
  private static Logger log = Logger.getLogger(AusApiServiceImpl.class);

  /**
   * Deletes the configuration for an AU given the AU identifier.
   * 
   * @param auid
   *          A String with the AU identifier.
   * @param securityContext
   *          A SecurityContext providing access to security related
   *          information.
   * @return a Response with any data that needs to be returned to the runtime.
   * @throws ApiException
   *           if there are problems.
   */
  @Override
  public Response deleteAuConfig(String auid, SecurityContext securityContext)
      throws ApiException {
    if (log.isDebugEnabled()) log.debug("auid = " + auid);

    try {
      if (auid == null || auid.isEmpty()) {
	String message = "Invalid auid = '" + auid + "'";
	log.error(message);
	return getErrorResponse(Response.Status.BAD_REQUEST, message);
      }

      PluginManager pluginManager =
	  LockssDaemon.getLockssDaemon().getPluginManager();

      ConfigExchange result =
	  convertConfig(pluginManager.getCurrentAuConfiguration(auid));
      if (log.isDebugEnabled()) log.debug("result = " + result);

      pluginManager.deleteAuConfiguration(auid);
      return Response.ok().entity(result).build();
    } catch (IllegalArgumentException iae) {
      String message = "No Archival Unit found for auid = '" + auid + "'";
      log.error(message);
      return getErrorResponse(Response.Status.NOT_FOUND, message);
    } catch (Exception e) {
      String message = "Cannot deleteAuConfig() for auid = '" + auid + "'";
      log.error(message, e);
      return getErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, message);
    }
  }

  /**
   * Provides the configuration for all AUs.
   * 
   * @param securityContext
   *          A SecurityContext providing access to security related
   *          information.
   * @return a Response with any data that needs to be returned to the runtime.
   * @throws ApiException
   *           if there are problems.
   */
  @Override
  public Response getAllAuConfig(SecurityContext securityContext)
      throws ApiException {
    if (log.isDebugEnabled()) log.debug("Invoked");

    try {
      ConfigExchange result = convertConfig(ConfigManager.getCurrentConfig()
	  .getConfigTree(PluginManager.PARAM_AU_TREE));
      if (log.isDebugEnabled()) log.debug("result = " + result);
      return Response.ok().entity(result).build();
    } catch (Exception e) {
      String message = "Cannot getAllAuConfig()";
      log.error(message, e);
      return getErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, message);
    }
  }

  /**
   * Provides the configuration of an AU given the AU identifier.
   * 
   * @param auid
   *          A String with the AU identifier.
   * @param securityContext
   *          A SecurityContext providing access to security related
   *          information.
   * @return a Response with any data that needs to be returned to the runtime.
   * @throws ApiException
   *           if there are problems.
   */
  @Override
  public Response getAuConfig(String auid, SecurityContext securityContext)
      throws ApiException {
    if (log.isDebugEnabled()) log.debug("auid = " + auid);

    try {
      if (auid == null || auid.isEmpty()) {
	String message = "Invalid auid = '" + auid + "'";
	log.error(message);
	return getErrorResponse(Response.Status.BAD_REQUEST, message);
      }

      ConfigExchange result = convertConfig(LockssDaemon.getLockssDaemon()
	  .getPluginManager().getCurrentAuConfiguration(auid));
      if (log.isDebugEnabled()) log.debug("result = " + result);
      return Response.ok().entity(result).build();
    } catch (IllegalArgumentException iae) {
      String message = "No Archival Unit found for auid = '" + auid + "'";
      log.error(message);
      return getErrorResponse(Response.Status.NOT_FOUND, message);
    } catch (Exception e) {
      String message = "Cannot getAuConfig()";
      log.error(message, e);
      return getErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, message);
    }
  }

  /**
   * Stores the provided configuration for an AU given the AU identifier.
   * 
   * @param auid
   *          A String with the AU identifier.
   * @param configExchange
   *          A ConfigExchange with the AU configuration.
   * @param securityContext
   *          A SecurityContext providing access to security related
   *          information.
   * @return a Response with any data that needs to be returned to the runtime.
   * @throws ApiException
   *           if there are problems.
   */
  @Override
  public Response putAuConfig(String auid, ConfigExchange configExchange,
      SecurityContext securityContext) throws ApiException {
    if (log.isDebugEnabled())
      log.debug("auid = " + auid + ", configExchange = " + configExchange);

    try {
      if (auid == null || auid.isEmpty()) {
	String message = "Invalid auid = '" + auid + "'";
	log.error(message);
	return getErrorResponse(Response.Status.BAD_REQUEST, message);
      }

      if (configExchange == null) {
	String message = "Configuration to be stored is not allowed to be null";
	log.error(message);
	return getErrorResponse(Response.Status.BAD_REQUEST, message);
      }

      PluginManager pluginManager =
	  LockssDaemon.getLockssDaemon().getPluginManager();
      pluginManager.updateAuConfigFile(auid, extractConfig(configExchange));

      ConfigExchange result =
	  convertConfig(pluginManager.getCurrentAuConfiguration(auid));
      if (log.isDebugEnabled()) log.debug("result = " + result);
      return Response.ok().entity(result).build();
    } catch (IllegalArgumentException iae) {
      String message = "No Archival Unit found for auid = '" + auid + "'";
      log.error(message);
      return getErrorResponse(Response.Status.NOT_FOUND, message);
    } catch (Exception e) {
      String message = "Cannot getAuConfig()";
      log.error(message, e);
      return getErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, message);
    }
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

  /**
   * Extracts a LOCKSS configuration object from an object ready to be
   * transmitted.
   * 
   * @param configExchange
   *          A ConfigExchange object from where to extract the LOCKSS
   *          configuration object.
   * @return a Configuration object with the LOCKSS configuration.
   */
  private Configuration extractConfig(ConfigExchange configExchange) {
    if (log.isDebugEnabled()) log.debug("configExchange = " + configExchange);

    Properties props = new Properties();
    props.putAll(configExchange.getProps());

    Configuration result = ConfigManager.fromProperties(props);
    if (log.isDebugEnabled()) log.debug("result = " + result);
    return result;
  }
}
