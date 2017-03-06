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

import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import org.apache.log4j.Logger;
import org.lockss.app.LockssDaemon;
import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.laaws.config.api.ApiException;
import org.lockss.laaws.config.api.ConfigApiService;
import org.lockss.laaws.config.model.ConfigurationMap;

/**
 * Implementation of the base provider of access to the configuration.
 */
public class ConfigApiServiceImpl extends ConfigApiService {
  private static Logger log = Logger.getLogger(ConfigApiServiceImpl.class);

  /**
   * Provides the full stored configuration.
   * @param securityContext
   *          A SecurityContext providing access to security related
   *          information.
   * @return a Response with any data that needs to be returned to the runtime.
   * @throws ApiException
   *           if there are other problems.
   */
  @Override
  public Response getConfig(SecurityContext securityContext)
      throws ApiException {
    if (log.isDebugEnabled()) log.debug("Invoked");

    try {
      ConfigurationMap result =
	  convertConfig(ConfigManager.getCurrentConfig());
      if (log.isDebugEnabled()) log.debug("result = " + result);
      return Response.ok().entity(result).build();
    } catch (Exception e) {
      String message = "Cannot getConfig()";
      log.error(message, e);
      throw new ApiException(1, message + ": " + e.getMessage());
    }
  }

  /**
   * Stores a configuration item.
   * 
   * @param configuration
   *          A ConfigurationMap with the configuration to be stored.
   * @param securityContext
   *          A SecurityContext providing access to security related
   *          information.
   * @return a Response with any data that needs to be returned to the runtime.
   * @throws ApiException
   *           if there are problems.
   */
  @Override
  public Response putConfig(ConfigurationMap configuration,
      SecurityContext securityContext) throws ApiException {
    if (log.isDebugEnabled()) log.debug("Invoked");

    try {
      Configuration updates = ConfigManager.newConfiguration();

      for (String key : configuration.keySet()) {
	updates.put(key, configuration.get(key));
      }

      if (log.isDebugEnabled()) log.debug("updates = " + updates);

      Configuration currentConfig = ConfigManager.getCurrentConfig();
      updates.copyFrom(currentConfig);

      ConfigManager.getConfigManager().setCurrentConfig(updates);

      ConfigurationMap result = convertConfig(currentConfig);
      if (log.isDebugEnabled()) log.debug("result = " + result);
      return Response.ok().entity(result).build();
    } catch (Exception e) {
      String message =
	  "Cannot putConfig() for entries = '" + configuration + "'";
      log.error(message, e);
      throw new ApiException(1, message + ": " + e.getMessage());
    }
  }

  /**
   * Provides the full stored configuration.
   * @param securityContext
   *          A SecurityContext providing access to security related
   *          information.
   * @return a Response with any data that needs to be returned to the runtime.
   * @throws ApiException
   *           if there are other problems.
   */
  private ConfigurationMap convertConfig(Configuration config) {
    if (log.isDebugEnabled()) log.debug("Invoked");

    ConfigurationMap result = new ConfigurationMap();

    for (String key : config.keySet()) {
      String value = config.get(key);
      result.put(key,  value);
    }

    if (log.isDebugEnabled()) log.debug("result = " + result);
    return result;
  }
}
