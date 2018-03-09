/*

Copyright (c) 2000-2018 Board of Trustees of Leland Stanford Jr. University,
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
   * @return {@code ResponseEntity<ConfigExchange>} with the deleted
   *         configuration.
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
   * @return a {@code ResponseEntity<ConfigExchange>} with the configuration for
   *         all AUs.
   */
  @Override
  @RequestMapping(value = "/aus",
  produces = { "application/json" },
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
   * @return a {@code ResponseEntity<ConfigExchange>} with the AU configuration.
   */
  @Override
  @RequestMapping(value = "/aus/{auid}",
  produces = { "application/json" },
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
   * @return a {@code ResponseEntity<ConfigExchange>} with the AU configuration.
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
