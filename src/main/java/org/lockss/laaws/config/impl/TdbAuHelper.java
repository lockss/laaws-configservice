/*

Copyright (c) 2014-2020 Board of Trustees of Leland Stanford Jr. University,
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.lockss.config.TdbAu;
import org.lockss.config.TdbUtil;
import org.lockss.util.Logger;
import org.lockss.ws.entities.TdbAuWsResult;

/**
 * Helper of the web service implementation of title database archival unit
 * queries.
 */
public class TdbAuHelper {
  /**
   * The fully-qualified name of the class of the objects used as source in a
   * query.
   */
  static String SOURCE_FQCN = TdbAuWsSource.class.getCanonicalName();

  /**
   * The fully-qualified name of the class of the objects returned by the query.
   */
  static String RESULT_FQCN = TdbAuWsResult.class.getCanonicalName();

  //
  // Property names used in title database archival unit queries.
  //
  static String AU_ID = "auId";
  static String NAME = "name";
  static String PLUGIN_NAME = "pluginName";
  static String TDB_TITLE = "tdbTitle";
  static String TDB_PUBLISHER = "tdbPublisher";
  static String DOWN = "down";
  static String ACTIVE = "active";
  static String PARAMS = "params";
  static String ATTRS = "attrs";
  static String PROPS = "props";

  /**
   * All the property names used in title database archival unit queries.
   */
  @SuppressWarnings("serial")
  static final Set<String> PROPERTY_NAMES = new HashSet<String>() {
    {
      add(AU_ID);
      add(NAME);
      add(PLUGIN_NAME);
      add(TDB_TITLE);
      add(TDB_PUBLISHER);
      add(DOWN);
      add(ACTIVE);
      add(PARAMS);
      add(ATTRS);
      add(PROPS);
    }
  };

  private static Logger log = Logger.getLogger();

  /**
   * Provides the universe of title database archival unit-related query objects
   * used as the source for a query.
   * 
   * @return a List<TdbAuWsSource> with the universe.
   */
  List<TdbAuWsSource> createUniverse() {
    final String DEBUG_HEADER = "createUniverse(): ";

    // Get all the title database archival units.
    Set<TdbAu.Id> allTdbAuIds = TdbUtil.getTdb().getAllTdbAuIds();
    if (log.isDebug3())
      log.debug3(DEBUG_HEADER + "allTdbAuIds.size() = " + allTdbAuIds.size());

    // Initialize the universe.
    List<TdbAuWsSource> universe =
	new ArrayList<TdbAuWsSource>(allTdbAuIds.size());

    // Loop through all the title database archival units.
    for (TdbAu.Id tdbAuId : allTdbAuIds) {
      // Add the object initialized with this title database archival unit to
      // the universe of objects.
      universe.add(new TdbAuWsSource(tdbAuId));
    }

    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "universe.size() = " + universe.size());
    return universe;
  }

  /**
   * Provides a printable copy of a collection of title database archival
   * unit-related query results.
   * 
   * @param results
   *          A {@code Collection<TdbAuWsResult>} with the query results.
   * @return a String with the requested printable copy.
   */
  String nonDefaultToString(Collection<TdbAuWsResult> results) {
    StringBuilder builder = new StringBuilder("[");
    boolean isFirst = true;

    // Loop through through all the results in the collection.
    for (TdbAuWsResult result : results) {
      // Handle the first result differently.
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      // Add this result to the printable copy.
      builder.append(nonDefaultToString(result));
    }

    return builder.append("]").toString();
  }

  /**
   * Provides a printable copy of a title database archival unit-related query
   * result.
   * 
   * @param result
   *          A TdbAuWsResult with the query result.
   * @return a String with the requested printable copy.
   */
  private String nonDefaultToString(TdbAuWsResult result) {
    StringBuilder builder = new StringBuilder("TdbAuWsResult [");
    boolean isFirst = true;

    if (result.getAuId() != null) {
      builder.append("auId=").append(result.getAuId());
      isFirst = false;
    }

    if (result.getName() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("name=").append(result.getName());
    }

    if (result.getPluginName() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("pluginName=").append(result.getPluginName());
    }

    if (result.getTdbTitle() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("tdbTitle=").append(result.getTdbTitle());
    }

    if (result.getTdbPublisher() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("tdbPublisher=").append(result.getTdbPublisher());
    }

    if (result.getDown() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("down=").append(result.getDown());
    }

    if (result.getActive() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("active=").append(result.getActive());
    }

    if (result.getParams() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("params=").append(result.getParams());
    }

    if (result.getAttrs() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("attrs=").append(result.getAttrs());
    }

    if (result.getProps() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("props=").append(result.getProps());
    }

    return builder.append("]").toString();
  }
}
