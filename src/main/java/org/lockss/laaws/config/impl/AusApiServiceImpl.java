/*

Copyright (c) 2000-2022 Board of Trustees of Leland Stanford Jr. University,
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
package org.lockss.laaws.config.impl;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.collections4.map.PassiveExpiringMap;
import org.lockss.app.LockssDaemon;
import org.lockss.config.AuConfiguration;
import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.config.rest.AuConfigPageInfo;
import org.lockss.laaws.config.api.AusApiDelegate;
import org.lockss.log.L4JLogger;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.PluginManager;
import org.lockss.spring.auth.AuthUtil;
import org.lockss.spring.auth.Roles;
import org.lockss.spring.base.BaseSpringApiServiceImpl;
import org.lockss.spring.base.LockssConfigurableService;
import org.lockss.util.StringUtil;
import org.lockss.util.UrlUtil;
import org.lockss.util.rest.config.PageInfo;
import org.lockss.util.time.TimeUtil;
import org.lockss.ws.entities.RequestAuControlResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for accessing Archival Unit configurations.
 */
@Service
public class AusApiServiceImpl extends BaseSpringApiServiceImpl
    implements AusApiDelegate, LockssConfigurableService {
  private static L4JLogger log = L4JLogger.getLogger();

  private final HttpServletRequest request;

  // The AU configuration iterators used in pagination.
  private Map<String, Iterator<AuConfiguration>> auConfigIterators =
      new ConcurrentHashMap<>();

  @Autowired
  public AusApiServiceImpl(HttpServletRequest request) {
    this.request = request;
  }

  static final String MISSING_AU_ID_ERROR_MESSAGE = "Missing auId";
  static final String NO_SUCH_AU_ERROR_MESSAGE = "No such Archival Unit";
  static final String DISABLED_METADATA_PROCESSING_ERROR_MESSAGE =
      "Metadata processing is not enabled";
  static final String ACTION_DISABLE_METADATA_INDEXING = "Disable Indexing";
  static final String DISABLE_METADATA_INDEXING_ERROR_MESSAGE =
      "Cannot disable AU metadata indexing";
  static final String ACTION_ENABLE_METADATA_INDEXING = "Enable Indexing";
  static final String ENABLE_METADATA_INDEXING_ERROR_MESSAGE =
      "Cannot enable AU metadata indexing";

  // TODO: Avoid repeating here the values of the constants defined in
  // the not accessible MetadataExtractorManager.
  static final String PARAM_INDEXING_ENABLED =
      "org.lockss.metadataManager.indexing_enabled";
  static final boolean DEFAULT_INDEXING_ENABLED = false;

  ////////////////////////////////////////////////////////////////////////////////
  // PARAMS //////////////////////////////////////////////////////////////////////
  ////////////////////////////////////////////////////////////////////////////////

  public static final String PREFIX = "org.lockss.config.rest.";

  /**
   * Default number of items that will be returned in a single (paged)
   * response
   */
  public static final String PARAM_DEFAULT_PAGESIZE = PREFIX + "pagesize.default";
  public static final int DEFAULT_PAGESIZE = 1000;
  private int defaultPageSize = DEFAULT_PAGESIZE;

  /**
   * Max number of items that will be returned in a single (paged)
   * response
   */
  public static final String PARAM_MAX_PAGESIZE = PREFIX + "pagesize.max";
  public static final int DEFAULT_MAX_PAGESIZE = 2000;
  private int maxPageSize = DEFAULT_MAX_PAGESIZE;

  /**
   * Interval after which unused iterator continuations will
   * be discarded.  Change requires restart to take effect.
   */
  public static final String PARAM_ITERATOR_TIMEOUT = PREFIX + "iterator.timeout";
  public static final long DEFAULT_ITERATOR_TIMEOUT = 48 * TimeUtil.HOUR;
  private long iteratorTimeout = DEFAULT_ITERATOR_TIMEOUT;

  ////////////////////////////////////////////////////////////////////////////////
  // CONFIG //////////////////////////////////////////////////////////////////////
  ////////////////////////////////////////////////////////////////////////////////

  @Override
  public void setConfig(Configuration newConfig,
                        Configuration prevConfig,
                        Configuration.Differences changedKeys) {
    if (changedKeys.contains(PREFIX)) {
      defaultPageSize =
          newConfig.getInt(PARAM_DEFAULT_PAGESIZE, DEFAULT_PAGESIZE);
      maxPageSize = newConfig.getInt(PARAM_MAX_PAGESIZE, DEFAULT_MAX_PAGESIZE);

      // The first time setConfig() is called, replace the temporary
      // iterator continuation map with a PassiveExpiringMap.
      // PassiveExpiringMap is already thread-safe (uses ConcurrentHashMap)
      // and handles expiration automatically, so no manual timer is needed.
      if (!(auConfigIterators instanceof PassiveExpiringMap)) {
        auConfigIterators = new PassiveExpiringMap<>(iteratorTimeout);
      }
    }
  }

  ////////////////////////////////////////////////////////////////////////////////
  // REST ////////////////////////////////////////////////////////////////////////
  ////////////////////////////////////////////////////////////////////////////////

  /**
   * Deletes the configuration for an AU given the AU identifier.
   * 
   * @param auid
   *          A String with the AU identifier.
   * @return {@code ResponseEntity<AuConfiguration>} with the deleted
   *         configuration.
   */
  @Override
  public ResponseEntity deleteAuConfig(String auid) {
    if (log.isDebugEnabled()) log.debug("auid = " + auid);

    // Check whether the service has not been fully initialized.
    if (!waitReady()) {
      // Yes: Notify the client.
      return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
    }

    AuthUtil.checkHasRole(Roles.ROLE_AU_ADMIN);

    try {
      if (auid == null || auid.isEmpty()) {
	String message = "Invalid auid = '" + auid + "'";
	log.error(message);
	return new ResponseEntity<String>(message, HttpStatus.BAD_REQUEST);
      }

      PluginManager pluginManager = getPluginManager();

      AuConfiguration result = pluginManager.getStoredAuConfiguration(auid);
      if (log.isDebugEnabled()) log.debug("result = " + result);

      pluginManager.deleteAuConfiguration(auid);
      return new ResponseEntity<AuConfiguration>(result, HttpStatus.OK);
    } catch (IllegalArgumentException iae) {
      String message = "No Archival Unit found for auid = '" + auid + "'";
      log.error(message);
      return new ResponseEntity<String>(message, HttpStatus.NOT_FOUND);
    } catch (Exception e) {
      String message = "Cannot deleteAuConfig() for auid = '" + auid + "'";
      log.error(message, e);
      return new ResponseEntity<String>(message,
	  HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * GET /aus:
   * Get all AU configurations or a pageful of the list defined by the continuation token and size.
   *
   * @param limit             An Integer with the maximum number of AU configurations
   *                          to be returned.
   * @param continuationToken A String with the continuation token of the next
   *                          page of AU configurations to be returned.
   * @return a {@code ResponseEntity<AuConfigPageInfo>}.
   */
  @Override
  public ResponseEntity getAllAuConfig(Integer limit, String continuationToken) {
    log.debug2("limit = {}, continuationToken = {}", limit, continuationToken);

    // Check whether the service has not been fully initialized.
    if (!waitReady()) {
      // Yes: Notify the client.
      return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
    }

    AuthUtil.checkHasRole(Roles.ROLE_AU_ADMIN);

    Integer requestLimit = limit;
    limit = validateLimit(requestLimit, defaultPageSize, maxPageSize);

    // Parse the request continuation token.
    AuConfigContinuationToken requestAct = null;

    try {
      requestAct = new AuConfigContinuationToken(continuationToken);
      log.trace("requestAct = {}", requestAct);
    } catch (IllegalArgumentException iae) {
      String message = "Invalid continuation token '" + continuationToken + "'";
      log.warn(message);
      return new ResponseEntity<String>(message, HttpStatus.BAD_REQUEST);
    }

    try {
      List<AuConfiguration> auConfigs = new ArrayList<>();

      // Get or create the iterator (handles recovery if expired)
      Iterator<AuConfiguration> iterator = getOrCreateIterator(requestAct, auConfigs);

      // Populate the results for this response
      populateAuConfigs(iterator, limit, auConfigs);

      log.trace("auConfigs.size() = {}", auConfigs.size());

      // Handle iterator storage/removal and create continuation token if needed
      AuConfigContinuationToken responseAct = null;
      String iteratorId = requestAct.getIteratorId();

      if (iterator.hasNext()) {
        // More results exist: Store iterator and create continuation token
        // If iteratorId is null, this is a new iterator (first page), so generate a new UUID.
        // If iteratorId exists, this is an existing iterator (subsequent page), so reuse
        // the same ID to maintain continuity for the client.
        if (iteratorId == null) {
          iteratorId = UUID.randomUUID().toString();
        }
        auConfigIterators.put(iteratorId, iterator);

        responseAct = new AuConfigContinuationToken(
            auConfigs.get(auConfigs.size() - 1).getAuId(),
            iteratorId);
        log.trace("responseAct = {}", responseAct);
      } else {
        // No more results: Remove the iterator if it exists
        if (iteratorId != null) {
          auConfigIterators.remove(iteratorId);
        }
      }

      // Build the response
      PageInfo pageInfo = buildPageInfoWithLinks(auConfigs, responseAct, requestLimit);

      AuConfigPageInfo auConfigPageInfo = new AuConfigPageInfo();
      auConfigPageInfo.setAuConfigs(auConfigs);
      auConfigPageInfo.setPageInfo(pageInfo);
      log.trace("auConfigPageInfo = {}", auConfigPageInfo);

      log.debug2("Returning OK.");
      return new ResponseEntity<>(auConfigPageInfo, HttpStatus.OK);

    } catch (IllegalArgumentException iae) {
      String message = iae.getMessage();
      log.error(message, iae);
      return new ResponseEntity<String>(message, HttpStatus.BAD_REQUEST);
    } catch (Exception e) {
      String message = "Cannot getAllAuConfig()";
      log.error(message, e);
      return new ResponseEntity<String>(message,
	  HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Provides the configuration for an AU given the AU identifier.
   * 
   * @param auid
   *          A String with the AU identifier.
   * @return a {@code ResponseEntity<AuConfiguration>} with the AU
   *         configuration.
   */
  @Override
  public ResponseEntity getAuConfig(String auid) {
    if (log.isDebugEnabled()) log.debug("auid = " + auid);

    // Check whether the service has not been fully initialized.
    if (!waitReady()) {
      // Yes: Notify the client.
      return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
    }

    AuthUtil.checkHasRole(Roles.ROLE_AU_ADMIN);

    try {
      if (auid == null || auid.isEmpty()) {
	String message = "Invalid auid = '" + auid + "'";
	log.error(message);
	return new ResponseEntity<String>(message, HttpStatus.BAD_REQUEST);
      }

      AuConfiguration result =
	  getPluginManager().getStoredAuConfiguration(auid);
      if (log.isDebugEnabled()) log.debug("result = " + result);
      return new ResponseEntity<AuConfiguration>(result, HttpStatus.OK);
    } catch (IllegalArgumentException iae) {
      String message = "No Archival Unit found for auid = '" + auid + "'";
      log.error(message);
      return new ResponseEntity<String>(message, HttpStatus.NOT_FOUND);
    } catch (Exception e) {
      String message = "Cannot getAuConfig() for auid = '" + auid + "'";
      log.error(message, e);
      return new ResponseEntity<String>(message,
	  HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Stores the provided Archival Unit configuration.
   * 
   * @param auConfiguration
   *          An AuConfiguration with the Archival Unit configuration.
   * @param auidArg
   *          The auid passed in the request, currently ignored
   * @return a {@code ResponseEntity<Void>} with the Archival Unit
   *         configuration.
   */
  @Override
  public ResponseEntity putAuConfig(String auidArg,
                                    AuConfiguration auConfiguration) {
    if (log.isDebugEnabled()) log.debug("auConfiguration = " + auConfiguration);

    // Check whether the service has not been fully initialized.
    if (!waitReady()) {
      // Yes: Notify the client.
      return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
    }

    AuthUtil.checkHasRole(Roles.ROLE_AU_ADMIN);

    String auId = null;

    try {
      auId = auConfiguration.getAuId();

      if (auId == null || auId.isEmpty() || !auId.equals(auidArg)) {
	String message = "Invalid auId = '" + auId + "'";
	log.error(message);
	return new ResponseEntity<String>(message, HttpStatus.BAD_REQUEST);
      }

      Map<String, String> auConfig = auConfiguration.getAuConfig();
      if (auConfig == null || auConfig.isEmpty()) {
	String message =
	    "Configuration to be stored is not allowed to be null or empty";
	log.error(message);
	return new ResponseEntity<String>(message, HttpStatus.BAD_REQUEST);
      }

      // Update the Archival Unit configuration.
      getPluginManager().updateAuConfigFromExternalSource(auConfiguration);

      return new ResponseEntity<Void>(HttpStatus.OK);
    } catch (IllegalArgumentException iae) {
      String message = "No Archival Unit found for auid = '" + auId + "'";
      log.error(message);
      return new ResponseEntity<String>(message, HttpStatus.NOT_FOUND);
    } catch (Exception e) {
      String message = "Cannot putAuConfig()";
      log.error(message, e);
      return new ResponseEntity<String>(message,
	  HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Enables the metadata indexing of an archival unit.
   * 
   * @param auId A String with the identifier (auid) of the archival unit.
   * @return a {@code ResponseEntity<RequestAuControlResult>} with the result of
   *         the operation.
   */
  @Override
  public ResponseEntity putAusMdEnable(String auId) {
    log.debug2("auId = {}", auId);

    // Check whether the service has not been fully initialized.
    if (!waitReady()) {
      // Yes: Notify the client.
      return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
    }

    AuthUtil.checkHasRole(Roles.ROLE_AU_ADMIN);

    // Add to the audit log a reference to this operation, if necessary.
    audit(ACTION_ENABLE_METADATA_INDEXING, auId);

    RequestAuControlResult result = null;

    try {
      if (!ConfigManager.getCurrentConfig().getBoolean(
	  PARAM_INDEXING_ENABLED, DEFAULT_INDEXING_ENABLED)) {
	result = new RequestAuControlResult(auId, false,
	    DISABLED_METADATA_PROCESSING_ERROR_MESSAGE);
	log.debug2("result = {}", result);
	return new ResponseEntity<RequestAuControlResult>(result,
	    HttpStatus.CONFLICT);
      }

      // Handle a missing auId.
      if (StringUtil.isNullString(auId)) {
	result = new RequestAuControlResult(auId, false,
	    MISSING_AU_ID_ERROR_MESSAGE);
	log.debug2("result = {}", result);
	return new ResponseEntity<RequestAuControlResult>(result,
	    HttpStatus.BAD_REQUEST);
      }

      // Get the Archival Unit to have its metadata indexing enabled.
      ArchivalUnit au =
	  LockssDaemon.getLockssDaemon().getPluginManager().getAuFromId(auId);
      log.trace("au = {}", au);

      // Handle a missing Archival Unit.
      if (au == null) {
	result =
	    new RequestAuControlResult(auId, false, NO_SUCH_AU_ERROR_MESSAGE);
	log.debug2("result = {}", result);
	return new ResponseEntity<RequestAuControlResult>(result,
	    HttpStatus.BAD_REQUEST);
      }

      try {
	// TODO: Implement via AU state.
	//metadataMgr.enableAuIndexing(au);
	result = new RequestAuControlResult(auId, true, null);
      } catch (Exception e) {
	result = new RequestAuControlResult(auId, false,
	    ENABLE_METADATA_INDEXING_ERROR_MESSAGE + ": " + e.getMessage());
	return new ResponseEntity<RequestAuControlResult>(result,
	    HttpStatus.INTERNAL_SERVER_ERROR);
      }

      log.debug2("result = {}", result);
      return new ResponseEntity<RequestAuControlResult>(result, HttpStatus.OK);
    } catch (Exception e) {
      String message = "Cannot putAusReactivate()";
      log.error(message, e);
      return new ResponseEntity<String>(message,
  	  HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Disables the metadata indexing of an archival unit.
   * 
   * @param auId
   *          A String with the identifier (auid) of the archival unit.
   * @return a {@code ResponseEntity<RequestAuControlResult>} with the result of
   *         the operation.
   */
  @Override
  public ResponseEntity putAusMdDisable(String auId) {
    log.debug2("auId = {}", auId);

    // Check whether the service has not been fully initialized.
    if (!waitReady()) {
      // Yes: Notify the client.
      return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
    }

    AuthUtil.checkHasRole(Roles.ROLE_AU_ADMIN);

    // Add to the audit log a reference to this operation, if necessary.
    audit(ACTION_DISABLE_METADATA_INDEXING, auId);

    RequestAuControlResult result = null;

    try {
      if (!ConfigManager.getCurrentConfig().getBoolean(
	  PARAM_INDEXING_ENABLED, DEFAULT_INDEXING_ENABLED)) {
	result = new RequestAuControlResult(auId, false,
	    DISABLED_METADATA_PROCESSING_ERROR_MESSAGE);
	log.debug2("result = {}", result);
	return new ResponseEntity<RequestAuControlResult>(result,
	    HttpStatus.CONFLICT);
      }

      // Handle a missing auId.
      if (StringUtil.isNullString(auId)) {
	result = new RequestAuControlResult(auId, false,
	    MISSING_AU_ID_ERROR_MESSAGE);
	log.debug2("result = {}", result);
	return new ResponseEntity<RequestAuControlResult>(result,
	    HttpStatus.BAD_REQUEST);
      }

      // Get the Archival Unit to have its metadata indexing disabled.
      ArchivalUnit au =
	  LockssDaemon.getLockssDaemon().getPluginManager().getAuFromId(auId);
      log.trace("au = {}", au);

      // Handle a missing Archival Unit.
      if (au == null) {
	result =
	    new RequestAuControlResult(auId, false, NO_SUCH_AU_ERROR_MESSAGE);
	log.debug2("result = {}", result);
	return new ResponseEntity<RequestAuControlResult>(result,
	    HttpStatus.BAD_REQUEST);
      }

      try {
	// TODO: Implement via AU state.
	//metadataMgr.disableAuIndexing(au);
	result = new RequestAuControlResult(auId, true, null);
      } catch (Exception e) {
	result = new RequestAuControlResult(auId, false,
	    DISABLE_METADATA_INDEXING_ERROR_MESSAGE + ": " + e.getMessage());
	return new ResponseEntity<RequestAuControlResult>(result,
	    HttpStatus.INTERNAL_SERVER_ERROR);
      }

      log.debug2("result = {}", result);
      return new ResponseEntity<RequestAuControlResult>(result, HttpStatus.OK);
    } catch (Exception e) {
      String message = "Cannot putAusReactivate()";
      log.error(message, e);
      return new ResponseEntity<String>(message,
  	  HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  ////////////////////////////////////////////////////////////////////////////////
  ////////////////////////////////////////////////////////////////////////////////
  ////////////////////////////////////////////////////////////////////////////////

  /**
   * Validates the limit parameter for paginated responses.
   *
   * @param requestLimit  An Integer with the requested limit.
   * @param defaultValue  An int with the default value if no limit is specified.
   * @param maxValue      An int with the maximum allowed value.
   * @return an int with the validated limit to use.
   * @throws IllegalArgumentException if the limit is not a positive integer.
   */
  private int validateLimit(Integer requestLimit, int defaultValue, int maxValue) {
    log.debug2("requestLimit = {}, defaultValue = {}, maxValue = {}",
        requestLimit, defaultValue, maxValue);

    // Check whether it's not a positive integer.
    if (requestLimit != null && requestLimit.intValue() <= 0) {
      // Yes: Report the problem.
      String message =
          "Limit of requested items must be a positive integer; it was '"
              + requestLimit + "'";
      log.warn(message);
      throw new IllegalArgumentException(message);
    }

    // No: Get the result.
    int result = requestLimit == null ?
        Math.min(defaultValue, maxValue) : Math.min(requestLimit, maxValue);
    log.debug2("result = {}", result);
    return result;
  }

  /**
   * Populates the AU configurations to be included in the response.
   *
   * @param iterator   An Iterator<AuConfiguration> with the AU configuration source iterator.
   * @param limit      An Integer with the maximum number of AU configurations to be
   *                   included in the response.
   * @param auConfigs  A List<AuConfiguration> with the AU configurations to be included in the
   *                   response.
   */
  private void populateAuConfigs(Iterator<AuConfiguration> iterator, Integer limit,
                                  List<AuConfiguration> auConfigs) {
    log.debug2("limit = {}, auConfigs = {}", limit, auConfigs);
    int auConfigCount = auConfigs.size();

    // Loop through as many AU configurations that exist and are requested.
    while (auConfigCount < limit && iterator.hasNext()) {
      // Add this AU configuration to the results.
      auConfigs.add(iterator.next());
      auConfigCount++;
    }
  }

  /**
   * Gets or creates an iterator for AU configurations, handling recovery if the iterator expired.
   *
   * @param requestAct  The continuation token from the request
   * @param auConfigs   The list to populate with any AUs found during recovery
   * @return An iterator positioned appropriately for this request
   * @throws org.lockss.db.DbException if there's an error retrieving AU configurations
   * @throws java.io.IOException if there's an I/O error retrieving AU configurations
   */
  private Iterator<AuConfiguration> getOrCreateIterator(
      AuConfigContinuationToken requestAct, List<AuConfiguration> auConfigs)
      throws org.lockss.db.DbException, java.io.IOException {
    String iteratorId = requestAct.getIteratorId();

    // Check whether this request is for the first page.
    if (iteratorId == null) {
      // Yes: Get the iterator pointing to first page of results.
      Collection<AuConfiguration> allConfigs =
          getConfigManager().retrieveAllArchivalUnitConfiguration();
      return allConfigs.iterator();
    }

    // No: Get the iterator (if any) used to provide a previous page of results.
    Iterator<AuConfiguration> iterator = auConfigIterators.get(iteratorId);

    // Check whether the iterator was not found (expired).
    if (iterator == null) {
      // Iterator has been lost - recover by recreating and seeking to last position
      String lastAuid = requestAct.getAuid();

      // Get the iterator pointing to first page of results.
      Collection<AuConfiguration> allConfigs =
          getConfigManager().retrieveAllArchivalUnitConfiguration();
      iterator = allConfigs.iterator();

      // Loop through the AU configs skipping those already returned through a
      // previous response.
      while (iterator.hasNext()) {
        AuConfiguration auConfig = iterator.next();

        // Check whether this AU config comes after the last one returned on the
        // previous response for this operation.
        if (auConfig.getAuId().compareTo(lastAuid) > 0) {
          // Yes: Add this AU config to the results.
          auConfigs.add(auConfig);

          // Add the rest of the AU configs to the results for this response
          // separately.
          break;
        }
      }
    }

    return iterator;
  }

  /**
   * Builds PageInfo with curLink and nextLink populated.
   *
   * @param auConfigs         The AU configurations in this page
   * @param responseAct       The response continuation token (null if last page)
   * @param requestLimit      The limit requested by the user (null if not specified)
   * @return A PageInfo object with all fields populated
   */
  private PageInfo buildPageInfoWithLinks(List<AuConfiguration> auConfigs,
                                           AuConfigContinuationToken responseAct,
                                           Integer requestLimit) {
    PageInfo pageInfo = new PageInfo();
    pageInfo.setItemsInPage(auConfigs.size());

    // Build the current link from the request URL and query string
    StringBuffer curLinkBuffer = request.getRequestURL();
    if (request.getQueryString() != null
        && !request.getQueryString().trim().isEmpty()) {
      curLinkBuffer.append("?").append(request.getQueryString());
    }
    String curLink = curLinkBuffer.toString();
    log.trace("curLink = {}", curLink);
    pageInfo.setCurLink(curLink);

    // If there's a continuation token, build the nextLink
    if (responseAct != null) {
      String continuationToken = responseAct.toWebResponseContinuationToken();
      pageInfo.setContinuationToken(continuationToken);

      StringBuffer nextLinkBuffer = request.getRequestURL();
      boolean hasQueryParameters = false;

      // Add limit parameter if user specified one
      if (requestLimit != null) {
        nextLinkBuffer.append("?limit=").append(requestLimit);
        hasQueryParameters = true;
      }

      // Add continuation token
      if (continuationToken != null) {
        if (!hasQueryParameters) {
          nextLinkBuffer.append("?");
          hasQueryParameters = true;
        } else {
          nextLinkBuffer.append("&");
        }
        nextLinkBuffer.append("continuationToken=")
            .append(UrlUtil.encodeUrl(continuationToken));
      }

      String nextLink = nextLinkBuffer.toString();
      log.trace("nextLink = {}", nextLink);
      pageInfo.setNextLink(nextLink);
    }

    return pageInfo;
  }

  /**
   * Provides the configuration manager.
   *
   * @return a ConfigManager with the configuration manager.
   */
  private ConfigManager getConfigManager() {
    return ConfigManager.getConfigManager();
  }
}
