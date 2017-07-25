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
import org.lockss.laaws.config.model.ConfigExchange;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Provider of access to the configuration information of Archival Units.
 */
@Api(value = "aus")
public interface AusApi  {

  /**
   * Deletes the configuration for an AU given the AU identifier.
   * 
   * @param auid
   *          A String with the AU identifier.
   * @return ResponseEntity<ConfigExchange> with the deleted configuration.
   */
  @ApiOperation(value = "Delete the configuration of an AU",
  notes = "Delete the configuration of an AU given the AU identifier",
  response = ConfigExchange.class,
  authorizations = {@Authorization(value = "basicAuth")}, tags={ "aus", })
  @ApiResponses(value = { 
      @ApiResponse(code = 200,
	  message = "The deleted configuration of the specified AU",
	  response = ConfigExchange.class),
      @ApiResponse(code = 400, message = "Bad Request",
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
  @RequestMapping(value = "/aus/{auid}",
  produces = { "application/json" }, consumes = { "application/json" },
  method = RequestMethod.DELETE)
  default ResponseEntity<ConfigExchange> deleteAuConfig(
      @ApiParam(value =
      "The identifier of the AU for which the configuration is to be deleted",
      required=true) @PathVariable("auid") String auid) {
    return new ResponseEntity<ConfigExchange>(HttpStatus.NOT_IMPLEMENTED);
  }

  /**
   * Provides the configuration for all AUs.
   * 
   * @return a ResponseEntity<ConfigExchange> with the configuration for all AUs.
   */
  @ApiOperation(value = "Get the configuration of all AUs",
  notes = "Get the configuration of all AUs",
  response = ConfigExchange.class,
  authorizations = {@Authorization(value = "basicAuth")}, tags={ "aus", })
  @ApiResponses(value = { 
      @ApiResponse(code = 200,
	  message = "The configuration of all AUs",
	  response = ConfigExchange.class),
      @ApiResponse(code = 401, message = "Unauthorized request",
      response = ConfigExchange.class),
      @ApiResponse(code = 500, message = "Internal server error",
      response = ConfigExchange.class),
      @ApiResponse(code = 503,
      message = "Some or all of the system is not available",
      response = ConfigExchange.class) })
  @RequestMapping(value = "/aus",
  produces = { "application/json" }, consumes = { "application/json" },
  method = RequestMethod.GET)
  default ResponseEntity<ConfigExchange> getAllAuConfig() {
    return new ResponseEntity<ConfigExchange>(HttpStatus.NOT_IMPLEMENTED);
  }

  /**
   * Provides the configuration for an AU given the AU identifier.
   * 
   * @param auid
   *          A String with the AU identifier.
   * @return a ResponseEntity<ConfigExchange> with the AU configuration.
   */
  @ApiOperation(value = "Get the configuration of an AU",
  notes = "Get the configuration of an AU given the AU identifier",
  response = ConfigExchange.class,
  authorizations = {@Authorization(value = "basicAuth")}, tags={ "aus", })
  @ApiResponses(value = { 
      @ApiResponse(code = 200,
	  message = "The configuration of the specified AU",
	  response = ConfigExchange.class),
      @ApiResponse(code = 400, message = "Bad Request",
      response = ConfigExchange.class),
      @ApiResponse(code = 401, message = "Unauthorized request",
      response = ConfigExchange.class),
      @ApiResponse(code = 500, message = "Internal server error",
      response = ConfigExchange.class),
      @ApiResponse(code = 503,
      message = "Some or all of the system is not available",
      response = ConfigExchange.class) })
  @RequestMapping(value = "/aus/{auid}",
  produces = { "application/json" }, consumes = { "application/json" },
  method = RequestMethod.GET)
  default ResponseEntity<ConfigExchange> getAuConfig(
      @ApiParam(value =
      "The identifier of the AU for which the configuration is requested",
      required=true) @PathVariable("auid") String auid) {
    return new ResponseEntity<ConfigExchange>(HttpStatus.NOT_IMPLEMENTED);
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
  @ApiOperation(value = "Store the configuration of an AU",
  notes = "Store the configuration of an AU given the AU identifier",
  response = ConfigExchange.class,
  authorizations = {@Authorization(value = "basicAuth")}, tags={ "aus", })
  @ApiResponses(value = { 
      @ApiResponse(code = 200,
	  message = "The configuration of the specified AU",
	  response = ConfigExchange.class),
      @ApiResponse(code = 400, message = "Bad Request",
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
  @RequestMapping(value = "/aus/{auid}",
  produces = { "application/json" }, consumes = { "application/json" },
  method = RequestMethod.PUT)
  default ResponseEntity<ConfigExchange> putAuConfig(
      @ApiParam(value =
      "The identifier of the AU for which the configuration is to be stored",
      required=true) @PathVariable("auid") String auid,
      @ApiParam(value = "The configuration items to be stored" ,required=true)
      @RequestBody ConfigExchange configExchange) {
    return new ResponseEntity<ConfigExchange>(HttpStatus.NOT_IMPLEMENTED);
  }
}
