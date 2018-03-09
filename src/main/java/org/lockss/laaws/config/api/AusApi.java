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
   * @return {@code ResponseEntity<ConfigExchange>} with the deleted
   *         configuration.
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
   * @return a {@code ResponseEntity<ConfigExchange>} with the configuration for
   *         all AUs.
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
  produces = { "application/json" },
  method = RequestMethod.GET)
  default ResponseEntity<ConfigExchange> getAllAuConfig() {
    return new ResponseEntity<ConfigExchange>(HttpStatus.NOT_IMPLEMENTED);
  }

  /**
   * Provides the configuration for an AU given the AU identifier.
   * 
   * @param auid
   *          A String with the AU identifier.
   * @return a {@code ResponseEntity<ConfigExchange>} with the AU configuration.
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
  produces = { "application/json" },
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
   * @return a {@code ResponseEntity<ConfigExchange>} with the AU configuration.
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
