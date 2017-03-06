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
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import org.lockss.laaws.config.api.factories.ConfigApiServiceFactory;
import org.lockss.laaws.config.model.ConfigurationMap;
import org.lockss.rs.auth.Roles;

/**
 * Provider of access to the system configuration.
 */
@Path("/config")
@Produces({ "application/json" })
@Api(value = "/config")
public class ConfigApi  {
  private final ConfigApiService delegate =
      ConfigApiServiceFactory.getConfigApi();

  /**
   * Provides the full stored configuration.
   * @param securityContext
   *          A SecurityContext providing access to security related
   *          information.
   * @return a Response with any data that needs to be returned to the runtime.
   * @throws ApiException
   *           if there are other problems.
   */
  @GET
  @Path("/")
  @Produces({ "application/json" })
  @ApiOperation(value = "Get the configuration",
  notes = "Get the full stored configuration",
  response = ConfigurationMap.class,
  authorizations = {@Authorization(value = "basicAuth")}, tags={ "config", })
  @ApiResponses(value = { 
      @ApiResponse(code = 200, message = "The configuration items",
	  response = ConfigurationMap.class),
      @ApiResponse(code = 500, message = "Internal server error",
      response = ConfigurationMap.class),
      @ApiResponse(code = 503,
      message = "Some or all of the system is not available",
      response = ConfigurationMap.class) })
  @RolesAllowed(Roles.ROLE_ANY) // Allow any authenticated user.
  public Response getConfig(@Context SecurityContext securityContext)
    throws ApiException {
    return delegate.getConfig(securityContext);
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
  @PUT
  @Path("/")
  @Produces({ "application/json" })
  @ApiOperation(value = "Store a configuration item",
  notes = "Store the given key/value pair",
  response = void.class,
  authorizations = {@Authorization(value = "basicAuth")}, tags={ "config", })
  @ApiResponses(value = { 
      @ApiResponse(code = 200, message = "The configuration items",
	  response = ConfigurationMap.class),
      @ApiResponse(code = 500, message = "Internal server error",
      response = ConfigurationMap.class),
      @ApiResponse(code = 503,
      message = "Some or all of the system is not available",
      response = ConfigurationMap.class) })
  @RolesAllowed(Roles.ROLE_ANY) // Allow any authenticated user.
  public Response putConfig(
      @ApiParam(value = "The configuration item to be stored" ,required=true)
      ConfigurationMap configuration,
      @Context SecurityContext securityContext) throws ApiException {
        return delegate.putConfig(configuration,securityContext);
  }
}
