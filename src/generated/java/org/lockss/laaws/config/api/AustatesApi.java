/**
 * NOTE: This class is auto generated by the swagger code generator program (2.4.0).
 * https://github.com/swagger-api/swagger-codegen
 * Do not edit the class manually.
 */
package org.lockss.laaws.config.api;

import io.swagger.annotations.*;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import javax.validation.constraints.*;
import java.util.List;

@Api(value = "austates", description = "the austates API")
public interface AustatesApi {

    AustatesApiDelegate getDelegate();

    @ApiOperation(value = "Get the state of an AU", nickname = "getAuState", notes = "Get the state of an AU given the AU identifier", response = String.class, authorizations = {
        @Authorization(value = "basicAuth")
    }, tags={ "aus", })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "The state of the specified AU", response = String.class),
        @ApiResponse(code = 400, message = "Bad Request"),
        @ApiResponse(code = 401, message = "Unauthorized"),
        @ApiResponse(code = 404, message = "Not Found"),
        @ApiResponse(code = 500, message = "Internal Server Error") })
    @RequestMapping(value = "/austates/{auid}",
        produces = { "application/json" }, 
        method = RequestMethod.GET)
    default ResponseEntity<String> getAuState(@ApiParam(value = "The identifier of the AU for which the state is requested",required=true) @PathVariable("auid") String auid) {
        return getDelegate().getAuState(auid);
    }


    @ApiOperation(value = "Update the state of an AU", nickname = "patchAuState", notes = "Update the state of an AU given the AU identifier", authorizations = {
        @Authorization(value = "basicAuth")
    }, tags={ "aus", })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "OK"),
        @ApiResponse(code = 400, message = "Bad Request"),
        @ApiResponse(code = 401, message = "Unauthorized"),
        @ApiResponse(code = 403, message = "Forbidden"),
        @ApiResponse(code = 404, message = "Not Found"),
        @ApiResponse(code = 415, message = "Unsupported Media Type"),
        @ApiResponse(code = 500, message = "Internal Server Error") })
    @RequestMapping(value = "/austates/{auid}",
        produces = { "application/json" }, 
        consumes = { "application/json" },
        method = RequestMethod.PATCH)
    default ResponseEntity<Void> patchAuState(@ApiParam(value = "The identifier of the AU for which the state is to be\\ \\ updated",required=true) @PathVariable("auid") String auid,@ApiParam(value = "The parts of the Archival Unit state to be updated" ,required=true )  @Valid @RequestBody String auState,@ApiParam(value = "The LOCKSS-specific request cookie header" ) @RequestHeader(value="X-Lockss-Request-Cookie", required=false) String xLockssRequestCookie) {
        return getDelegate().patchAuState(auid, auState, xLockssRequestCookie);
    }

}
