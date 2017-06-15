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
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import org.lockss.laaws.config.api.factories.ConfigApiServiceFactory;
import org.lockss.laaws.config.model.ConfigExchange;
import org.lockss.laaws.config.model.ConfigModSpec;
import org.lockss.rs.auth.Roles;

/**
 * Provider of access to the system configuration.
 */
@Path("/config")
@Consumes({ "application/json" })
@Produces({ "application/json" })
@Api(value = "/config")
public class ConfigApi  {
  private final ConfigApiService delegate =
      ConfigApiServiceFactory.getConfigApi();

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
  @DELETE
  @Path("/{sectionName}")
  @Consumes({ "application/json" })
  @Produces({ "application/json" })
  @ApiOperation(value = "Delete the named configuration",
  notes = "Delete the configuration for a given name",
  response = ConfigExchange.class,
  authorizations = {@Authorization(value = "basicAuth")}, tags={ "config", })
  @ApiResponses(value = { 
      @ApiResponse(code = 200,
	  message = "The named configuration that has been deleted",
	  response = ConfigExchange.class),
      @ApiResponse(code = 404, message = "Configuration section not found",
      response = ConfigExchange.class),
      @ApiResponse(code = 500, message = "Internal server error",
      response = ConfigExchange.class),
      @ApiResponse(code = 503,
      message = "Some or all of the system is not available",
      response = ConfigExchange.class) })
  @RolesAllowed(Roles.ROLE_USER_ADMIN) // Allow an administrative user.
  public Response deleteConfig(
      @ApiParam(value =
      "The name of the section for which the configuration is to be deleted",
      required=true) @PathParam("sectionName") String sectionName,
      @Context SecurityContext securityContext) throws ApiException {
    return delegate.deleteConfig(sectionName, securityContext);
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
  @GET
  @Path("/{sectionName}")
  @Consumes({ "application/json" })
  @Produces({ "application/json" })
  @ApiOperation(value = "Get the named configuration",
  notes = "Get the configuration items stored for a given name",
  response = ConfigExchange.class,
  authorizations = {@Authorization(value = "basicAuth")}, tags={ "config", })
  @ApiResponses(value = { 
      @ApiResponse(code = 200, message = "The named configuration",
	  response = ConfigExchange.class),
      @ApiResponse(code = 404, message = "Configuration section not found",
      response = ConfigExchange.class),
      @ApiResponse(code = 500, message = "Internal server error",
      response = ConfigExchange.class),
      @ApiResponse(code = 503,
      message = "Some or all of the system is not available",
      response = ConfigExchange.class) })
  @RolesAllowed(Roles.ROLE_ANY) // Allow any authenticated user.
  public Response getConfig(
      @ApiParam(value =
      "The name of the section for which the configuration is requested",
      required=true) @PathParam("sectionName") String sectionName,
      @Context SecurityContext securityContext) throws ApiException {
    return delegate.getConfig(sectionName, securityContext);
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
  @GET
  @Path("/lastupdatetime")
  @Consumes({ "application/json" })
  @Produces({ "application/json" })
  @ApiOperation(
  value = "Get the timestamp when the configuration was last updated",
  notes = "Get the timestamp when the configuration was last updated",
  response = Date.class,
  authorizations = {@Authorization(value = "basicAuth")}, tags={ "config", })
  @ApiResponses(value = { 
      @ApiResponse(code = 200,
	  message = "The timestamp when the configuration was last updated",
	  response = Date.class),
      @ApiResponse(code = 500, message = "Internal server error",
      response = Date.class),
      @ApiResponse(code = 503,
      message = "Some or all of the system is not available",
      response = Date.class) })
  @RolesAllowed(Roles.ROLE_ANY) // Allow any authenticated user.
  public Response getLastUpdateTime(@Context SecurityContext securityContext)
  throws ApiException {
    return delegate.getLastUpdateTime(securityContext);
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
  @GET
  @Path("/loadedurls")
  @Consumes({ "application/json" })
  @Produces({ "application/json" })
  @ApiOperation(value = "Get the URLs from which the configuration was loaded",
  notes =
  "Get the URLs from which the configuration was actually loaded, reflecting any failover to local copies",
  response = String.class, responseContainer = "List",
  authorizations = {@Authorization(value = "basicAuth")}, tags={ "config", })
  @ApiResponses(value = { 
      @ApiResponse(code = 200,
	  message = "The URLs from which the configuration was loaded",
	  response = String.class, responseContainer = "List"),
      @ApiResponse(code = 500, message = "Internal server error",
      response = String.class, responseContainer = "List"),
      @ApiResponse(code = 503,
      message = "Some or all of the system is not available",
      response = String.class, responseContainer = "List") })
  @RolesAllowed(Roles.ROLE_ANY) // Allow any authenticated user.
  public Response getLoadedUrlList(@Context SecurityContext securityContext)
  throws ApiException {
    return delegate.getLoadedUrlList(securityContext);
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
  @PUT
  @Path("/{sectionName}")
  @Consumes({ "application/json" })
  @Produces({ "application/json" })
  @ApiOperation(value = "Modify the named configuration",
  notes = "Modify the configuration properties for a given name",
  response = ConfigExchange.class,
  authorizations = {@Authorization(value = "basicAuth")}, tags={ "config", })
  @ApiResponses(value = { 
      @ApiResponse(code = 200,
	  message = "The named configuration after it's modified",
	  response = ConfigExchange.class),
      @ApiResponse(code = 404, message = "Configuration section not found",
      response = ConfigExchange.class),
      @ApiResponse(code = 500, message = "Internal server error",
      response = ConfigExchange.class),
      @ApiResponse(code = 503,
      message = "Some or all of the system is not available",
      response = ConfigExchange.class) })
  @RolesAllowed(Roles.ROLE_USER_ADMIN) // Allow an administrative user.
  public Response putConfig(
      @ApiParam(value =
      "The name of the section for which the configuration is to be modified",
      required=true) @PathParam("sectionName") String sectionName,
      @ApiParam(value = "The configuration properties to be modified",
      required=true) ConfigModSpec configModSpec,
      @Context SecurityContext securityContext) throws ApiException {
    return delegate.putConfig(sectionName, configModSpec, securityContext);
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
  @PUT
  @Path("/reload")
  @Consumes({ "application/json" })
  @Produces({ "application/json" })
  @ApiOperation(value = "Request a configuration reload",
  notes = "Request that the stored configuration is reloaded",
  response = void.class,
  authorizations = {@Authorization(value = "basicAuth")}, tags={ "config", })
  @ApiResponses(value = { 
      @ApiResponse(code = 200, message = "OK", response = void.class),
      @ApiResponse(code = 500, message = "Internal server error",
      response = void.class),
      @ApiResponse(code = 503,
      message = "Some or all of the system is not available",
      response = void.class) })
  @RolesAllowed(Roles.ROLE_USER_ADMIN) // Allow an administrative user.
  public Response requestReload(@Context SecurityContext securityContext)
  throws ApiException {
    return delegate.requestReload(securityContext);
  }
}
