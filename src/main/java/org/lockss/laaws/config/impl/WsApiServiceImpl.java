package org.lockss.laaws.config.impl;

import org.josql.Query;
import org.josql.QueryExecutionException;
import org.josql.QueryResults;
import org.lockss.laaws.config.api.WsApiDelegate;
import org.lockss.log.L4JLogger;
import org.lockss.spring.base.BaseSpringApiServiceImpl;
import org.lockss.util.josql.JosqlUtil;
import org.lockss.ws.entities.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

public class WsApiServiceImpl extends BaseSpringApiServiceImpl
    implements WsApiDelegate {

  private static L4JLogger log = L4JLogger.getLogger();

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
