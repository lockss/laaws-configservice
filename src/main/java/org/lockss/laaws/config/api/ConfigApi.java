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

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;
import java.util.Date;
import java.util.List;
import org.lockss.laaws.config.model.ConfigExchange;
import org.lockss.laaws.config.model.ConfigModSpec;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Provider of access to the system configuration.
 */
@Api(value = "config")
public interface ConfigApi {
  public static final String SECTION_NAME_CLUSTER = "cluster";
  public static final String SECTION_NAME_UI_IP_ACCESS = "ui_ip_access";
  public static final String SECTION_NAME_PROXY_IP_ACCESS = "proxy_ip_access";
  public static final String SECTION_NAME_PLUGIN = "plugin";
  public static final String SECTION_NAME_AU = "au";
  public static final String SECTION_NAME_TITLE_DB = "titledb";
  public static final String SECTION_NAME_ICP_SERVER = "icp_server";
  public static final String SECTION_NAME_AUDIT_PROXY = "audit_proxy";
  public static final String SECTION_NAME_CONTENT_SERVERS = "content_servers";
  public static final String SECTION_NAME_ACCESS_GROUPS = "access_groups";
  public static final String SECTION_NAME_CRAWL_PROXY = "crawl_proxy";
  public static final String SECTION_NAME_EXPERT = "expert";
  public static final String SECTION_NAME_ALERT = "alert";
  public static final String SECTION_NAME_CRONSTATE = "cronstate";

  /**
   * Deletes the configuration for a section given the section name.
   * 
   * @param sectionName
   *          A String with the section name.
   * @return ResponseEntity<ConfigExchange> with the deleted configuration.
   */
  @ApiOperation(value = "Delete the named configuration",
  notes = "Delete the configuration for a given name",
  response = ConfigExchange.class,
  authorizations = {@Authorization(value = "basicAuth")}, tags={ "config", })
  @ApiResponses(value = { 
      @ApiResponse(code = 200,
	  message = "The named configuration that has been deleted",
	  response = ConfigExchange.class),
      @ApiResponse(code = 400, message = "Bad request",
      response = ConfigExchange.class),
      @ApiResponse(code = 401, message = "Unauthorized request",
      response = ConfigExchange.class),
      @ApiResponse(code = 403, message = "Forbidden request",
      response = ConfigExchange.class),
      @ApiResponse(code = 500, message = "Internal server error",
      response = ConfigExchange.class),
      @ApiResponse(code = 503,
      message = "Some or all of the system is not available",
      response = ConfigExchange.class) })
  @RequestMapping(value = "/config/{sectionName}",
  produces = { "application/json" }, consumes = { "application/json" },
  method = RequestMethod.DELETE)
  default ResponseEntity<ConfigExchange> deleteConfig(
      @ApiParam(value =
      "The name of the section for which the configuration is to be deleted",
      required=true) @PathVariable("sectionName") String sectionName) {
    return new ResponseEntity<ConfigExchange>(HttpStatus.NOT_IMPLEMENTED);
  }

  /**
   * Provides the configuration for a section given the section name.
   * 
   * @param sectionName
   *          A String with the section name.
   * @return a ResponseEntity<Void> with the section configuration.
   */
  @ApiOperation(value = "Get the named configuration",
  notes = "Get the configuration items stored for a given name",
  response = Void.class,
  authorizations = {@Authorization(value = "basicAuth")}, tags={ "config", })
  @ApiResponses(value = { 
      @ApiResponse(code = 200, message = "The named configuration",
	  response = Void.class),
      @ApiResponse(code = 400, message = "Bad request",
      response = Void.class),
      @ApiResponse(code = 401, message = "Unauthorized request",
      response = Void.class),
      @ApiResponse(code = 500, message = "Internal server error",
      response = Void.class),
      @ApiResponse(code = 503,
      message = "Some or all of the system is not available",
      response = Void.class) })
  @RequestMapping(value = "/config/{sectionName}",
  produces = { "multipart/form-data" }, method = RequestMethod.GET)
  default ResponseEntity<MultiValueMap<String, Object>> getConfig(
      @ApiParam(value =
      "The name of the section for which the configuration is requested",
      required=true) @PathVariable("sectionName") String sectionName,
      @ApiParam(value = "Content type to return",
      allowableValues="multipart/related", defaultValue="multipart/related")
      @RequestHeader(value="Accept", required=false) String accept) {
    return new ResponseEntity<MultiValueMap<String, Object>>(
	HttpStatus.NOT_IMPLEMENTED);
  }

  /**
   * Provides the timestamp of the last time the configuration was updated.
   * 
   * @return a ResponseEntity<Date> with the timestamp.
   */
  @ApiOperation(
  value = "Get the timestamp when the configuration was last updated",
  notes = "Get the timestamp when the configuration was last updated",
  response = Date.class,
  authorizations = {@Authorization(value = "basicAuth")}, tags={ "config", })
  @ApiResponses(value = { 
      @ApiResponse(code = 200,
	  message = "The timestamp when the configuration was last updated",
	  response = Date.class),
      @ApiResponse(code = 401, message = "Unauthorized request",
      response = Date.class),
      @ApiResponse(code = 500, message = "Internal server error",
      response = Date.class),
      @ApiResponse(code = 503,
      message = "Some or all of the system is not available",
      response = Date.class) })
  @RequestMapping(value = "/config/lastupdatetime",
  produces = { "application/json" }, consumes = { "application/json" },
  method = RequestMethod.GET)
  default ResponseEntity<Date> getLastUpdateTime() {
    return new ResponseEntity<Date>(HttpStatus.NOT_IMPLEMENTED);
  }

  /**
   * Provides the URLs from which the configuration was loaded.
   * 
   * @return a ResponseEntity<String> with the URLs.
   */
  @ApiOperation(value = "Get the URLs from which the configuration was loaded",
  notes =
  "Get the URLs from which the configuration was actually loaded, reflecting any failover to local copies",
  response = String.class, responseContainer = "List",
  authorizations = {@Authorization(value = "basicAuth")}, tags={ "config", })
  @ApiResponses(value = { 
      @ApiResponse(code = 200,
	  message = "The URLs from which the configuration was loaded",
	  response = String.class, responseContainer = "List"),
      @ApiResponse(code = 401, message = "Unauthorized request",
      response = String.class, responseContainer = "List"),
      @ApiResponse(code = 500, message = "Internal server error",
      response = String.class, responseContainer = "List"),
      @ApiResponse(code = 503,
      message = "Some or all of the system is not available",
      response = String.class, responseContainer = "List") })
  @RequestMapping(value = "/config/loadedurls",
  produces = { "application/json" }, consumes = { "application/json" },
  method = RequestMethod.GET)
  default ResponseEntity<List<String>> getLoadedUrlList() {
    return new ResponseEntity<List<String>>(HttpStatus.NOT_IMPLEMENTED);
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
  @ApiOperation(value = "Modify the named configuration",
  notes = "Modify the configuration properties for a given name",
  response = ConfigExchange.class,
  authorizations = {@Authorization(value = "basicAuth")}, tags={ "config", })
  @ApiResponses(value = { 
      @ApiResponse(code = 200,
	  message = "The named configuration after it's modified",
	  response = ConfigExchange.class),
      @ApiResponse(code = 400, message = "Bad request",
      response = ConfigExchange.class),
      @ApiResponse(code = 401, message = "Unauthorized request",
      response = ConfigExchange.class),
      @ApiResponse(code = 403, message = "Forbidden request",
      response = ConfigExchange.class),
      @ApiResponse(code = 500, message = "Internal server error",
      response = ConfigExchange.class),
      @ApiResponse(code = 503,
      message = "Some or all of the system is not available",
      response = ConfigExchange.class) })
  @RequestMapping(value = "/config/{sectionName}",
  produces = { "application/json" }, consumes = { "application/json" },
  method = RequestMethod.PUT)
  default ResponseEntity<ConfigExchange> putConfig(
      @ApiParam(value =
      "The name of the section for which the configuration is to be modified",
      required=true) @PathVariable("sectionName") String sectionName,
      @ApiParam(value = "The configuration properties to be modified",
      required=true) @RequestBody ConfigModSpec configModSpec) {
    return new ResponseEntity<ConfigExchange>(HttpStatus.NOT_IMPLEMENTED);
  }

  /**
   * Requests a reload of the configuration.
   * 
   * @return a ResponseEntity<void> with the status.
   */
  @ApiOperation(value = "Request a configuration reload",
  notes = "Request that the stored configuration is reloaded",
  response = void.class,
  authorizations = {@Authorization(value = "basicAuth")}, tags={ "config", })
  @ApiResponses(value = { 
      @ApiResponse(code = 200, message = "OK", response = void.class),
      @ApiResponse(code = 401, message = "Unauthorized request",
      response = void.class),
      @ApiResponse(code = 403, message = "Forbidden request",
      response = void.class),
      @ApiResponse(code = 500, message = "Internal server error",
      response = void.class),
      @ApiResponse(code = 503,
      message = "Some or all of the system is not available",
      response = void.class) })
  @RequestMapping(value = "/config/reload",
  produces = { "application/json" }, consumes = { "application/json" },
  method = RequestMethod.PUT)
  default ResponseEntity<Void> putConfigReload() {
    return new ResponseEntity<Void>(HttpStatus.NOT_IMPLEMENTED);
  }
}
