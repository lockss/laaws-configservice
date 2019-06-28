package org.lockss.laaws.mdq.api;

import org.lockss.laaws.mdq.model.AuMetadataPageInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Optional;

/**
 * A delegate to be called by the {@link MetadataApiController}}.
 * Implement this interface with a {@link org.springframework.stereotype.Service} annotated class.
 */

public interface MetadataApiDelegate {

    Logger log = LoggerFactory.getLogger(MetadataApi.class);

    default Optional<ObjectMapper> getObjectMapper() {
        return Optional.empty();
    }

    default Optional<HttpServletRequest> getRequest() {
        return Optional.empty();
    }

    default Optional<String> getAcceptHeader() {
        return getRequest().map(r -> r.getHeader("Accept"));
    }

    /**
     * @see MetadataApi#getMetadataAusAuid
     */
    default ResponseEntity<AuMetadataPageInfo> getMetadataAusAuid( String  auid,
         Integer  limit,
         String  continuationToken) {
        if(getObjectMapper().isPresent() && getAcceptHeader().isPresent()) {
            if (getAcceptHeader().get().contains("application/json")) {
                try {
                    return new ResponseEntity<>(getObjectMapper().get().readValue("{  \"pageInfo\" : {    \"curLink\" : \"curLink\",    \"resultsPerPage\" : 6,    \"totalCount\" : 0,    \"continuationToken\" : \"continuationToken\",    \"nextLink\" : \"nextLink\"  },  \"items\" : [ {    \"scalarMap\" : {      \"key\" : \"scalarMap\"    },    \"listMap\" : {      \"key\" : [ \"listMap\", \"listMap\" ]    },    \"setMap\" : {      \"key\" : [ \"setMap\", \"setMap\" ]    },    \"mapMap\" : {      \"key\" : {        \"key\" : \"mapMap\"      }    }  }, {    \"scalarMap\" : {      \"key\" : \"scalarMap\"    },    \"listMap\" : {      \"key\" : [ \"listMap\", \"listMap\" ]    },    \"setMap\" : {      \"key\" : [ \"setMap\", \"setMap\" ]    },    \"mapMap\" : {      \"key\" : {        \"key\" : \"mapMap\"      }    }  } ]}", AuMetadataPageInfo.class), HttpStatus.NOT_IMPLEMENTED);
                } catch (IOException e) {
                    log.error("Couldn't serialize response for content type application/json", e);
                    return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
                }
            }
        } else {
            log.warn("ObjectMapper or HttpServletRequest not configured in default MetadataApi interface so no example is generated");
        }
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

}
