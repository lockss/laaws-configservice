package org.lockss.laaws.config.impl;

import org.josql.Query;
import org.josql.QueryExecutionException;
import org.josql.QueryResults;
import org.lockss.app.LockssDaemon;
import org.lockss.config.Configuration;
import org.lockss.daemon.TitleConfig;
import org.lockss.laaws.config.api.WsApiDelegate;
import org.lockss.log.L4JLogger;
import org.lockss.remote.RemoteApi;
import org.lockss.spring.auth.AuthUtil;
import org.lockss.spring.auth.Roles;
import org.lockss.spring.base.BaseSpringApiServiceImpl;
import org.lockss.util.StringUtil;
import org.lockss.util.josql.JosqlUtil;
import org.lockss.ws.entities.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.security.AccessControlException;
import java.util.*;

@Service
public class WsApiServiceImpl extends BaseSpringApiServiceImpl
    implements WsApiDelegate {

  private static L4JLogger log = L4JLogger.getLogger();

  /**
   * Configures the archival units defined by a list of their identifiers.
   *
   * @param auIds A {@code List<String>} with the identifiers (auids) of the
   *              archival units. The archival units to be added must already be
   *              in the title db that's loaded into the daemon.
   * @return a {@code ResponseEntity<List<ContentConfigurationResult>>} with the
   *         results of the operation.
   */
  @Override
  public ResponseEntity postAus(List<String> auIds) {
    log.debug2("auIds = " + auIds);

    // Check whether the service has not been fully initialized.
    if (!waitReady()) {
      // Yes: Notify the client.
      return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
    }

    // Check for required role
    try {
      AuthUtil.checkHasRole(Roles.ROLE_AU_ADMIN);
    } catch (AccessControlException ace) {
      log.warn(ace.getMessage());
      return new ResponseEntity<Void>(HttpStatus.FORBIDDEN);
    }

    try {
      List<ContentConfigurationResult> results =
          new ArrayList<ContentConfigurationResult>(auIds.size());

      RemoteApi remoteApi = LockssDaemon.getLockssDaemon().getRemoteApi();
      List<String> auids = new LinkedList<>();

      Map<String, Configuration> titleConfigs =
          new HashMap<String, Configuration>();

      // Loop  through all the Archival Unit identifiers.
      for (String auId : auIds) {

        // Get the configuration of the Archival Unit.
        TitleConfig titleConfig = remoteApi.findTitleConfig(auId);

        // Check whether the configuration was found.
        if (titleConfig != null) {
          // Populate the array of Archival Unit identifiers.
          auids.add(auId);

          // Yes: Add it to the map.
          titleConfigs.put(auId,  titleConfig.getConfig());
        }
      }

      String[] auIdArray = auids.toArray(new String[0]);

      // Add all the archival units.
      RemoteApi.BatchAuStatus status = remoteApi.batchAddAus(RemoteApi.BATCH_ADD_ADD,
          auIdArray, null, null, titleConfigs, new HashMap<String, String>(),
          null);

      int index = 0;

      // Loop through all the results.
      for (RemoteApi.BatchAuStatus.Entry entry : status.getUnsortedStatusList()) {
        RemoteApi.BatchAuStatus entryStatus = new RemoteApi.BatchAuStatus();
        entryStatus.add(entry);

        ContentConfigurationResult result = null;

        if (entry.isOk()) {
          log.debug("Success configuring AU '" + entry.getName() + "': "
              + entry.getExplanation());

          result = new ContentConfigurationResult(auIdArray[index++],
              entry.getName(), Boolean.TRUE, entry.getExplanation());
        } else {
          log.error("Error configuring AU '" + entry.getName() + "': "
              + entry.getExplanation());

          result = new ContentConfigurationResult(auIdArray[index++],
              entry.getName(), Boolean.FALSE, entry.getExplanation());
        }

        results.add(result);
      }

      log.debug2("results = " + results);
      return new ResponseEntity<List<ContentConfigurationResult>>(results,
          HttpStatus.OK);
    } catch (Exception e) {
      String message = "Cannot postAus()";
      log.error(message, e);
      return new ResponseEntity<String>(message,
          HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Deactivates the archival units defined by a list with their identifiers.
   *
   * @param auIds A {@code List<String>} with the identifiers (auids) of the
   *              archival units.
   * @return a {@code ResponseEntity<List<ContentConfigurationResult>>} with the
   *         results of the operation.
   */
  @Override
  public ResponseEntity putAusDeactivate(List<String> auIds) {
    log.debug2("auIds = " + auIds);

    // Check whether the service has not been fully initialized.
    if (!waitReady()) {
      // Yes: Notify the client.
      return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
    }

    // Check for required role
    try {
      AuthUtil.checkHasRole(Roles.ROLE_AU_ADMIN);
    } catch (AccessControlException ace) {
      log.warn(ace.getMessage());
      return new ResponseEntity<Void>(HttpStatus.FORBIDDEN);
    }

    try {
      List<ContentConfigurationResult> results =
          new ArrayList<ContentConfigurationResult>(auIds.size());

      // Deactivate the archival units.
      RemoteApi.BatchAuStatus status =
          LockssDaemon.getLockssDaemon().getRemoteApi().deactivateAus(auIds);

      // Loop through all the results.
      for (int i = 0; i < status.getUnsortedStatusList().size(); i++) {
        // Get the original Archival Unit identifier.
        String auId = auIds.get(i);

        // Handle the result.
        RemoteApi.BatchAuStatus.Entry statusEntry = status.getUnsortedStatusList().get(i);

        if (statusEntry.isOk()
            || "Deactivated".equals(statusEntry.getStatus())) {
          log.debug("Success deactivating AU '" + statusEntry.getName() + "': "
              + statusEntry.getExplanation());

          String explanation = statusEntry.getExplanation();
          if (StringUtil.isNullString(explanation)) {
            explanation = "Deactivated Archival Unit '" + auId + "'";
          }

          results.add(new ContentConfigurationResult(auId,
              statusEntry.getName(), Boolean.TRUE,
              statusEntry.getExplanation()));
        } else {
          log.error("Error deactivating AU '" + statusEntry.getName() + "': "
              + statusEntry.getExplanation());

          String explanation = statusEntry.getExplanation();
          if (StringUtil.isNullString(explanation)) {
            explanation = statusEntry.getStatus();
          }

          results.add(new ContentConfigurationResult(auId,
              statusEntry.getName(), Boolean.FALSE, explanation));
        }
      }

      log.debug2("results = " + results);
      return new ResponseEntity<List<ContentConfigurationResult>>(results,
          HttpStatus.OK);
    } catch (Exception e) {
      String message = "Cannot putAusDeactivate()";
      log.error(message, e);
      return new ResponseEntity<String>(message,
          HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Reactivates the archival units defined by a list with their identifiers.
   *
   * @param auIds
   *          A {@code List<String>} with the identifiers (auids) of the
   *          archival units.
   * @return a {@code ResponseEntity<List<ContentConfigurationResult>>} with the
   *         results of the operation.
   */
  @Override
  public ResponseEntity putAusReactivate(List<String> auIds) {
    log.debug2("auIds = " + auIds);

    // Check whether the service has not been fully initialized.
    if (!waitReady()) {
      // Yes: Notify the client.
      return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
    }

    // Check for required role
    try {
      AuthUtil.checkHasRole(Roles.ROLE_AU_ADMIN);
    } catch (AccessControlException ace) {
      log.warn(ace.getMessage());
      return new ResponseEntity<Void>(HttpStatus.FORBIDDEN);
    }

    try {
      List<ContentConfigurationResult> results =
          new ArrayList<ContentConfigurationResult>(auIds.size());

      RemoteApi.BatchAuStatus status = null;

      // Reactivate the archival units.
      status =
          LockssDaemon.getLockssDaemon().getRemoteApi().reactivateAus(auIds);

      // Loop through all the results.
      for (int i = 0; i < status.getUnsortedStatusList().size(); i++) {
        // Get the original Archival Unit identifier.
        String auId = auIds.get(i);

        // Handle the result.
        RemoteApi.BatchAuStatus.Entry statusEntry = status.getUnsortedStatusList().get(i);

        if (statusEntry.isOk() || "Added".equals(statusEntry.getStatus())) {
          log.debug("Success reactivating AU '" + statusEntry.getName()
              + "': " + statusEntry.getExplanation());

          String explanation = statusEntry.getExplanation();
          if (StringUtil.isNullString(explanation)) {
            explanation = "Reactivated Archival Unit '" + auId + "'";
          }

          results.add(new ContentConfigurationResult(auId,
              statusEntry.getName(), Boolean.TRUE,
              statusEntry.getExplanation()));
        } else {
          log.error("Error reactivating AU '" + statusEntry.getName() + "': "
              + statusEntry.getExplanation());

          String explanation = statusEntry.getExplanation();
          if (StringUtil.isNullString(explanation)) {
            explanation = statusEntry.getStatus();
          }

          results.add(new ContentConfigurationResult(auId,
              statusEntry.getName(), Boolean.FALSE, explanation));
        }
      }

      log.debug2("results = " + results);
      return new ResponseEntity<List<ContentConfigurationResult>>(results,
          HttpStatus.OK);
    } catch (Exception e) {
      String message = "Cannot putAusReactivate()";
      log.error(message, e);
      return new ResponseEntity<String>(message,
          HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Unconfigures the archival units defined by a list with their identifiers.
   *
   * @param auIds
   *          A {@code List<String>} with the identifiers (auids) of the
   *          archival units.
   * @return a {@code ResponseEntity<List<ContentConfigurationResult>>} with the
   *           results of the operation.
   */
  @Override
  public ResponseEntity deleteAusDelete(List<String> auIds) {
    log.debug2("auIds = " + auIds);

    // Check whether the service has not been fully initialized.
    if (!waitReady()) {
      // Yes: Notify the client.
      return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
    }

    // Check for required role
    try {
      AuthUtil.checkHasRole(Roles.ROLE_AU_ADMIN);
    } catch (AccessControlException ace) {
      log.warn(ace.getMessage());
      return new ResponseEntity<Void>(HttpStatus.FORBIDDEN);
    }

    try {
      List<ContentConfigurationResult> results =
          new ArrayList<ContentConfigurationResult>(auIds.size());

      // Delete the archival units.
      RemoteApi.BatchAuStatus status =
          LockssDaemon.getLockssDaemon().getRemoteApi().deleteAus(auIds);

      // Loop through all the results.
      for (int i = 0; i < status.getUnsortedStatusList().size(); i++) {
        // Get the original Archival Unit identifier.
        String auId = auIds.get(i);

        // Handle the result.
        RemoteApi.BatchAuStatus.Entry statusEntry = status.getUnsortedStatusList().get(i);

        if (statusEntry.isOk() || "Deleted".equals(statusEntry.getStatus())) {
          if (log.isDebugEnabled()) log.debug("Success unconfiguring AU '"
              + statusEntry.getName() + "': " + statusEntry.getExplanation());

          String explanation = statusEntry.getExplanation();
          if (StringUtil.isNullString(explanation)) {
            explanation = "Deleted Archival Unit '" + auId + "'";
          }

          results.add(new ContentConfigurationResult(auId, statusEntry.getName(),
              Boolean.TRUE, statusEntry.getExplanation()));
        } else {
          log.error("Error unconfiguring AU '" + statusEntry.getName() + "': "
              + statusEntry.getExplanation());

          String explanation = statusEntry.getExplanation();
          if (StringUtil.isNullString(explanation)) {
            explanation = statusEntry.getStatus();
          }

          results.add(new ContentConfigurationResult(auId,
              statusEntry.getName(), Boolean.FALSE, explanation));
        }
      }

      log.debug2("results = {}", results);
      return new ResponseEntity<List<ContentConfigurationResult>>(results,
          HttpStatus.OK);
    } catch (Exception e) {
      String message = "Cannot putAusReactivate()";
      log.error(message, e);
      return new ResponseEntity<String>(message,
          HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Provides the selected properties of selected archival units.
   *
   * @param auQuery A String with the
   *                <a href="package-summary.html#SQL-Like_Query">SQL-like
   *                query</a> used to specify what properties to retrieve from
   *                which archival units.
   * @return a {@code ResponseEntity<List<AuWsResult>>} with the results.
   */
  @Override
  public ResponseEntity getAuqueries(String auQuery) {
    log.debug2("auQuery = {}", auQuery);

    AuHelper auHelper = new AuHelper();
    List<AuWsResult> results = null;

    try {
      // Create the full query.
      String fullQuery = JosqlUtil.createFullQuery(auQuery,
          AuHelper.SOURCE_FQCN, AuHelper.PROPERTY_NAMES, AuHelper.RESULT_FQCN);
      log.trace("fullQuery = {}", fullQuery);

      // Create a new JoSQL query.
      Query q = new Query();

      try {
        // Parse the SQL-like query.
        q.parse(fullQuery);

        // Execute the query.
        QueryResults qr = q.execute(auHelper.createUniverse());

        // Get the query results.
        results = (List<AuWsResult>)qr.getResults();
        log.trace("results.size() = {}", results.size());
        log.trace("results = {}", auHelper.nonDefaultToString(results));
        return new ResponseEntity<List<AuWsResult>>(results, HttpStatus.OK);
      } catch (QueryExecutionException qee) {
        String message =
            "Cannot getAuqueries() for auQuery = '" + auQuery + "'";
        log.error(message, qee);
        return new ResponseEntity<String>(message,
            HttpStatus.INTERNAL_SERVER_ERROR);
      }
    } catch (Exception e) {
      String message = "Cannot getAuqueries() for auQuery = '" + auQuery + "'";
      log.error(message, e);
      return new ResponseEntity<String>(message,
          HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Provides the selected properties of selected plugins.
   *
   * @param pluginQuery A String with the
   *                    <a href="package-summary.html#SQL-Like_Query">SQL-like
   *                    query</a> used to specify what properties to retrieve
   *                    from which plugins.
   * @return a {@code ResponseEntity<List<PluginWsResult>>} with the results.
   */
  @Override
  public ResponseEntity getPlugins(String pluginQuery) {
    log.debug2("pluginQuery = {}", pluginQuery);

    PluginHelper pluginHelper = new PluginHelper();
    List<PluginWsResult> results = null;

    try {
      // Create the full query.
      String fullQuery = JosqlUtil.createFullQuery(pluginQuery,
          PluginHelper.SOURCE_FQCN, PluginHelper.PROPERTY_NAMES,
          PluginHelper.RESULT_FQCN);
      log.trace("fullQuery = {}", fullQuery);

      // Create a new JoSQL query.
      Query q = new Query();

      try {
        // Parse the SQL-like query.
        q.parse(fullQuery);

        // Execute the query.
        QueryResults qr = q.execute(pluginHelper.createUniverse());

        // Get the query results.
        results = (List<PluginWsResult>)qr.getResults();
        log.trace("results.size() = {}" + results.size());
        log.trace("results = {}", pluginHelper.nonDefaultToString(results));
        return new ResponseEntity<List<PluginWsResult>>(results,
            HttpStatus.OK);
      } catch (QueryExecutionException qee) {
        String message =
            "Cannot getTdbTitles() for pluginQuery = '" + pluginQuery + "'";
        log.error(message, qee);
        return new ResponseEntity<String>(message,
            HttpStatus.INTERNAL_SERVER_ERROR);
      }
    } catch (Exception e) {
      String message =
          "Cannot getTdbTitles() for pluginQuery = '" + pluginQuery + "'";
      log.error(message, e);
      return new ResponseEntity<String>(message,
          HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Provides the selected properties of selected title database archival units.
   *
   * @param tdbAuQuery A String with the
   *                   <a href="package-summary.html#SQL-Like_Query">SQL-like
   *                   query</a> used to specify what properties to retrieve
   *                   from which title database archival units.
   * @return a {@code ResponseEntity<List<TdbAuWsResult>>} with the results.
   */
  @Override
  public ResponseEntity getTdbAus(String tdbAuQuery) {
    log.debug2("tdbAuQuery = {}", tdbAuQuery);

    // Check whether the service has not been fully initialized.
    if (!waitReady()) {
      // Yes: Notify the client.
      return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
    }

    TdbAuHelper tdbAuHelper = new TdbAuHelper();
    List<TdbAuWsResult> results = null;

    try {
      // Create the full query.
      String fullQuery = JosqlUtil.createFullQuery(tdbAuQuery,
          TdbAuHelper.SOURCE_FQCN, TdbAuHelper.PROPERTY_NAMES,
          TdbAuHelper.RESULT_FQCN);
      log.trace("fullQuery = {}", fullQuery);

      // Create a new JoSQL query.
      Query q = new Query();

      try {
        // Parse the SQL-like query.
        q.parse(fullQuery);

        // Execute the query.
        QueryResults qr = q.execute(tdbAuHelper.createUniverse());

        // Get the query results.
        results = (List<TdbAuWsResult>)qr.getResults();
        log.trace("results.size() = {}", results.size());
        log.trace("results = {}", tdbAuHelper.nonDefaultToString(results));
        return new ResponseEntity<List<TdbAuWsResult>>(results, HttpStatus.OK);
      } catch (QueryExecutionException qee) {
        String message =
            "Cannot getTdbAus() for tdbAuQuery = '" + tdbAuQuery + "'";
        log.error(message, qee);
        return new ResponseEntity<String>(message,
            HttpStatus.INTERNAL_SERVER_ERROR);
      }
    } catch (Exception e) {
      String message =
          "Cannot getTdbAus() for tdbAuQuery = '" + tdbAuQuery + "'";
      log.error(message, e);
      return new ResponseEntity<String>(message,
          HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Provides the selected properties of selected title database publishers.
   *
   * @param tdbPublisherQuery A String with the
   *                      <a href="package-summary.html#SQL-Like_Query">SQL-like
   *                      query</a> used to specify what properties to retrieve
   *                      from which title database publishers.
   * @return a {@code List<TdbPublisherWsResult>} with the results.
   */
  @Override
  public ResponseEntity getTdbPublishers(String tdbPublisherQuery) {
    log.debug2("tdbPublisherQuery = {}", tdbPublisherQuery);

    // Check whether the service has not been fully initialized.
    if (!waitReady()) {
      // Yes: Notify the client.
      return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
    }

    TdbPublisherHelper tdbPublisherHelper = new TdbPublisherHelper();
    List<TdbPublisherWsResult> results = null;

    try {
      // Create the full query.
      String fullQuery = JosqlUtil.createFullQuery(tdbPublisherQuery,
          TdbPublisherHelper.SOURCE_FQCN, TdbPublisherHelper.PROPERTY_NAMES,
          TdbPublisherHelper.RESULT_FQCN);
      log.trace("fullQuery = {}", fullQuery);

      // Create a new JoSQL query.
      Query q = new Query();

      try {
        // Parse the SQL-like query.
        q.parse(fullQuery);

        // Execute the query.
        QueryResults qr = q.execute(tdbPublisherHelper.createUniverse());

        // Get the query results.
        results = (List<TdbPublisherWsResult>)qr.getResults();
        log.trace("results.size() = {}" + results.size());
        log.trace("results = {}",
            tdbPublisherHelper.nonDefaultToString(results));
        return new ResponseEntity<List<TdbPublisherWsResult>>(results,
            HttpStatus.OK);
      } catch (QueryExecutionException qee) {
        String message = "Cannot getTdbPublishers() for tdbPublisherQuery = '"
            + tdbPublisherQuery + "'";
        log.error(message, qee);
        return new ResponseEntity<String>(message,
            HttpStatus.INTERNAL_SERVER_ERROR);
      }
    } catch (Exception e) {
      String message = "Cannot getTdbPublishers() for tdbPublisherQuery = '"
          + tdbPublisherQuery + "'";
      log.error(message, e);
      return new ResponseEntity<String>(message,
          HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Provides the selected properties of selected title database titles.
   *
   * @param tdbTitleQuery A String with the
   *                      <a href="package-summary.html#SQL-Like_Query">SQL-like
   *                      query</a> used to specify what properties to retrieve
   *                      from which title database titles.
   * @return a {@code ResponseEntity<List<TdbTitleWsResult>>} with the results.
   */
  @Override
  public ResponseEntity getTdbTitles(String tdbTitleQuery) {
    log.debug2("tdbTitleQuery = {}", tdbTitleQuery);

    // Check whether the service has not been fully initialized.
    if (!waitReady()) {
      // Yes: Notify the client.
      return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
    }

    TdbTitleHelper tdbTitleHelper = new TdbTitleHelper();
    List<TdbTitleWsResult> results = null;

    try {
      // Create the full query.
      String fullQuery = JosqlUtil.createFullQuery(tdbTitleQuery,
          TdbTitleHelper.SOURCE_FQCN, TdbTitleHelper.PROPERTY_NAMES,
          TdbTitleHelper.RESULT_FQCN);
      log.trace("fullQuery = {}", fullQuery);

      // Create a new JoSQL query.
      Query q = new Query();

      try {
        // Parse the SQL-like query.
        q.parse(fullQuery);

        // Execute the query.
        QueryResults qr = q.execute(tdbTitleHelper.createUniverse());

        // Get the query results.
        results = (List<TdbTitleWsResult>)qr.getResults();
        log.trace("results.size() = {}" + results.size());
        log.trace("results = {}", tdbTitleHelper.nonDefaultToString(results));
        return new ResponseEntity<List<TdbTitleWsResult>>(results,
            HttpStatus.OK);
      } catch (QueryExecutionException qee) {
        String message =
            "Cannot getTdbTitles() for tdbTitleQuery = '" + tdbTitleQuery + "'";
        log.error(message, qee);
        return new ResponseEntity<String>(message,
            HttpStatus.INTERNAL_SERVER_ERROR);
      }
    } catch (Exception e) {
      String message =
          "Cannot getTdbTitles() for tdbTitleQuery = '" + tdbTitleQuery + "'";
      log.error(message, e);
      return new ResponseEntity<String>(message,
          HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }
}
