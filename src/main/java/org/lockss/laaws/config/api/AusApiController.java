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

import io.swagger.annotations.ApiParam;
import java.lang.reflect.MalformedParametersException;
import java.security.AccessControlException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.apache.log4j.Logger;
import org.lockss.app.LockssDaemon;
import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.laaws.config.model.ConfigExchange;
import org.lockss.plugin.PluginManager;
import org.lockss.rs.auth.Roles;
import org.lockss.rs.auth.SpringAuthenticationFilter;
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
 * Controller for access to Archival Unit configurations.
 */
@RestController
public class AusApiController implements AusApi {
  private static Logger log = Logger.getLogger(AusApiController.class);

  /**
   * Deletes the configuration for an AU given the AU identifier.
   * 
   * @param auid
   *          A String with the AU identifier.
   * @return ResponseEntity<ConfigExchange> with the deleted configuration.
   */
  @Override
  @RequestMapping(value = "/aus/{auid}",
  produces = { "application/json" }, consumes = { "application/json" },
  method = RequestMethod.DELETE)
  public ResponseEntity<ConfigExchange> deleteAuConfig(@PathVariable("auid")
  String auid) {
    if (log.isDebugEnabled()) log.debug("auid = " + auid);

    SpringAuthenticationFilter.checkAuthorization(Roles.ROLE_AU_ADMIN);

    try {
      if (auid == null || auid.isEmpty()) {
	String message = "Invalid auid = '" + auid + "'";
	log.error(message);
	throw new MalformedParametersException(message);
      }

      PluginManager pluginManager =
	  LockssDaemon.getLockssDaemon().getPluginManager();

      ConfigExchange result =
	  convertConfig(pluginManager.getStoredAuConfiguration(auid));
      if (log.isDebugEnabled()) log.debug("result = " + result);

      pluginManager.deleteAuConfiguration(auid);

      return new ResponseEntity<ConfigExchange>(result, HttpStatus.OK);
    } catch (MalformedParametersException mpe) {
      throw mpe;
    } catch (IllegalArgumentException iae) {
      String message = "No Archival Unit found for auid = '" + auid + "'";
      log.error(message);
      throw new IllegalArgumentException(message);
    } catch (Exception e) {
      String message = "Cannot deleteAuConfig() for auid = '" + auid + "'";
      log.error(message, e);
      throw new RuntimeException(message);
    }
  }

  /**
   * Provides the configuration for all AUs.
   * 
   * @return a ResponseEntity<ConfigExchange> with the configuration for all AUs.
   */
  @Override
  @RequestMapping(value = "/aus",
  produces = { "application/json" }, consumes = { "application/json" },
  method = RequestMethod.GET)
  public ResponseEntity<ConfigExchange> getAllAuConfig() {
    if (log.isDebugEnabled()) log.debug("Invoked");

    try {
      ConfigExchange result = convertConfig(ConfigManager.getCurrentConfig()
	  .getConfigTree(PluginManager.PARAM_AU_TREE));
      if (log.isDebugEnabled()) log.debug("result = " + result);
      return new ResponseEntity<ConfigExchange>(result, HttpStatus.OK);
    } catch (Exception e) {
      String message = "Cannot getAllAuConfig()";
      log.error(message, e);
      throw new RuntimeException(message);
    }
  }

  /**
   * Provides the configuration for an AU given the AU identifier.
   * 
   * @param auid
   *          A String with the AU identifier.
   * @return a ResponseEntity<ConfigExchange> with the AU configuration.
   */
  @Override
  @RequestMapping(value = "/aus/{auid}",
  produces = { "application/json" }, consumes = { "application/json" },
  method = RequestMethod.GET)
  public ResponseEntity<ConfigExchange> getAuConfig(@PathVariable("auid")
      String auid) {
    if (log.isDebugEnabled()) log.debug("auid = " + auid);

    try {
      if (auid == null || auid.isEmpty()) {
	String message = "Invalid auid = '" + auid + "'";
	log.error(message);
	throw new MalformedParametersException(message);
      }

      ConfigExchange result = convertConfig(LockssDaemon.getLockssDaemon()
	  .getPluginManager().getCurrentAuConfiguration(auid));
      if (log.isDebugEnabled()) log.debug("result = " + result);
      return new ResponseEntity<ConfigExchange>(result, HttpStatus.OK);
    } catch (MalformedParametersException mpe) {
      throw mpe;
    } catch (IllegalArgumentException iae) {
      String message = "No Archival Unit found for auid = '" + auid + "'";
      log.error(message);
      throw new IllegalArgumentException(message);
    } catch (Exception e) {
      String message = "Cannot getAuConfig() for auid = '" + auid + "'";
      log.error(message, e);
      throw new RuntimeException(message);
    }
  }

  /**
   * Stores the provided configuration for an AU given the AU identifier.
   * 
   * @param auid
   *          A String with the AU identifier.
   * @param configExchange
   *          A ConfigExchange with the AU configuration.
   * @return a ResponseEntity<ConfigExchange> with the AU configuration.
   */
  @Override
  @RequestMapping(value = "/aus/{auid}",
  produces = { "application/json" }, consumes = { "application/json" },
  method = RequestMethod.PUT)
  public ResponseEntity<ConfigExchange> putAuConfig(@PathVariable("auid")
      String auid, @ApiParam(required=true) @RequestBody
      ConfigExchange configExchange) {
    if (log.isDebugEnabled())
      log.debug("auid = " + auid + ", configExchange = " + configExchange);

    SpringAuthenticationFilter.checkAuthorization(Roles.ROLE_AU_ADMIN);

    try {
      if (auid == null || auid.isEmpty()) {
	String message = "Invalid auid = '" + auid + "'";
	log.error(message);
	throw new MalformedParametersException(message);
      }

      if (configExchange == null) {
	String message = "Configuration to be stored is not allowed to be null";
	log.error(message);
	throw new MalformedParametersException(message);
      }

      PluginManager pluginManager =
	  LockssDaemon.getLockssDaemon().getPluginManager();
      pluginManager.updateAuConfigFile(auid, extractConfig(configExchange));

      ConfigExchange result =
	  convertConfig(pluginManager.getStoredAuConfiguration(auid));
      if (log.isDebugEnabled()) log.debug("result = " + result);
      return new ResponseEntity<ConfigExchange>(result, HttpStatus.OK);
    } catch (MalformedParametersException mpe) {
      throw mpe;
    } catch (IllegalArgumentException iae) {
      String message = "No Archival Unit found for auid = '" + auid + "'";
      log.error(message);
      throw new IllegalArgumentException(message);
    } catch (Exception e) {
      String message = "Cannot putAuConfig()";
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
