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
import org.lockss.laaws.config.api.factories.AusApiServiceFactory;
import org.lockss.laaws.config.model.ConfigExchange;
import org.lockss.rs.auth.Roles;

/**
 * Provider of access to the configuration information of Archival Units.
 */
@Path("/aus")
@Consumes({ "application/json" })
@Produces({ "application/json" })
@Api(value = "/aus")
public class AusApi  {
  private final AusApiService delegate = AusApiServiceFactory.getAusApi();

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
  @DELETE
  @Path("/{auid}")
  @Consumes({ "application/json" })
  @Produces({ "application/json" })
  @ApiOperation(value = "Delete the configuration of an AU",
  notes = "Delete the configuration of an AU given the AU identifier",
  response = ConfigExchange.class,
  authorizations = {@Authorization(value = "basicAuth")}, tags={ "aus", })
  @ApiResponses(value = { 
      @ApiResponse(code = 200,
	  message = "The deleted configuration of the specified AU",
	  response = ConfigExchange.class),
      @ApiResponse(code = 404, message = "AU not found",
      response = ConfigExchange.class),
      @ApiResponse(code = 500, message = "Internal server error",
      response = ConfigExchange.class),
      @ApiResponse(code = 503,
      message = "Some or all of the system is not available",
      response = ConfigExchange.class) })
  @RolesAllowed(Roles.ROLE_AU_ADMIN) // Allow an AU administrative user.
  public Response deleteAuConfig(
      @ApiParam(value =
      "The identifier of the AU for which the configuration is to be deleted",
      required=true) @PathParam("auid") String auid,
      @Context SecurityContext securityContext) throws ApiException {
    return delegate.deleteAuConfig(auid, securityContext);
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
  @GET
  @Path("/")
  @Consumes({ "application/json" })
  @Produces({ "application/json" })
  @ApiOperation(value = "Get the configuration of all AUs",
  notes = "Get the configuration of all AUs",
  response = ConfigExchange.class,
  authorizations = {@Authorization(value = "basicAuth")}, tags={ "aus", })
  @ApiResponses(value = { 
      @ApiResponse(code = 200,
	  message = "The configuration of all AUs",
	  response = ConfigExchange.class),
      @ApiResponse(code = 500, message = "Internal server error",
      response = ConfigExchange.class),
      @ApiResponse(code = 503,
      message = "Some or all of the system is not available",
      response = ConfigExchange.class) })
  @RolesAllowed(Roles.ROLE_ANY) // Allow any authenticated user.
  public Response getAllAuConfig(@Context SecurityContext securityContext)
      throws ApiException {
    return delegate.getAllAuConfig(securityContext);
  }

  /**
   * Provides the configuration for an AU given the AU identifier.
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
  @GET
  @Path("/{auid}")
  @Consumes({ "application/json" })
  @Produces({ "application/json" })
  @ApiOperation(value = "Get the configuration of an AU",
  notes = "Get the configuration of an AU given the AU identifier",
  response = ConfigExchange.class,
  authorizations = {@Authorization(value = "basicAuth")}, tags={ "aus", })
  @ApiResponses(value = { 
      @ApiResponse(code = 200,
	  message = "The configuration of the specified AU",
	  response = ConfigExchange.class),
      @ApiResponse(code = 404, message = "AU not found",
      response = ConfigExchange.class),
      @ApiResponse(code = 500, message = "Internal server error",
      response = ConfigExchange.class),
      @ApiResponse(code = 503,
      message = "Some or all of the system is not available",
      response = ConfigExchange.class) })
  @RolesAllowed(Roles.ROLE_ANY) // Allow any authenticated user.
  public Response getAuConfig(
      @ApiParam(value =
      "The identifier of the AU for which the configuration is requested",
      required=true) @PathParam("auid") String auid,
      @Context SecurityContext securityContext) throws ApiException {
    return delegate.getAuConfig(auid, securityContext);
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
  @PUT
  @Path("/{auid}")
  @Consumes({ "application/json" })
  @Produces({ "application/json" })
  @ApiOperation(value = "Store the configuration of an AU",
  notes = "Store the configuration of an AU given the AU identifier",
  response = ConfigExchange.class,
  authorizations = {@Authorization(value = "basicAuth")}, tags={ "aus", })
  @ApiResponses(value = { 
      @ApiResponse(code = 200,
	  message = "The configuration of the specified AU",
	  response = ConfigExchange.class),
      @ApiResponse(code = 404, message = "AU not found",
      response = ConfigExchange.class),
      @ApiResponse(code = 500, message = "Internal server error",
      response = ConfigExchange.class),
      @ApiResponse(code = 503,
      message = "Some or all of the system is not available",
      response = ConfigExchange.class) })
  @RolesAllowed(Roles.ROLE_AU_ADMIN) // Allow an AU administrative user.
  public Response putAuConfig(
      @ApiParam(value =
      "The identifier of the AU for which the configuration is to be stored",
      required=true) @PathParam("auid") String auid,
      @ApiParam(value = "The configuration items to be stored" ,required=true)
      ConfigExchange configExchange, @Context SecurityContext securityContext)
	  throws ApiException {
      return delegate.putAuConfig(auid, configExchange, securityContext);
  }
}
