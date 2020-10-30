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
import org.lockss.config.TdbPublisher;
import org.lockss.config.TdbUtil;
import org.lockss.util.Logger;
import org.lockss.ws.entities.TdbPublisherWsResult;

/**
 * Helper of the web service implementation of title database publisher queries.
 */
public class TdbPublisherHelper {
  /**
   * The fully-qualified name of the class of the objects used as source in a
   * query.
   */
  static String SOURCE_FQCN = TdbPublisherWsSource.class.getCanonicalName();

  /**
   * The fully-qualified name of the class of the objects returned by the query.
   */
  static String RESULT_FQCN = TdbPublisherWsResult.class.getCanonicalName();

  //
  // Property names used in title database publisher queries.
  //
  static String NAME = "name";

  /**
   * All the property names used in title database publisher queries.
   */
  @SuppressWarnings("serial")
  static final Set<String> PROPERTY_NAMES = new HashSet<String>() {
    {
      add(NAME);
    }
  };

  private static Logger log = Logger.getLogger();

  /**
   * Provides the universe of title database publisher-related query objects
   * used as the source for a query.
   * 
   * @return a List<TdbPublisherWsSource> with the universe.
   */
  List<TdbPublisherWsSource> createUniverse() {
    final String DEBUG_HEADER = "createUniverse(): ";

    // Get all the title database publishers.
    Collection<TdbPublisher> allTdbPublishers =
	TdbUtil.getTdb().getAllTdbPublishers().values();
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "allTdbPublishers.size() = "
	+ allTdbPublishers.size());

    // Initialize the universe.
    List<TdbPublisherWsSource> universe =
	new ArrayList<TdbPublisherWsSource>(allTdbPublishers.size());

    // Loop through all the title database publishers.
    for (TdbPublisher tdbPublisher : allTdbPublishers) {
      // Add the object initialized with this title database publisher to the
      // universe of objects.
      universe.add(new TdbPublisherWsSource(tdbPublisher));
    }

    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "universe.size() = " + universe.size());
    return universe;
  }

  /**
   * Provides a printable copy of a collection of title database
   * publisher-related query results.
   * 
   * @param results
   *          A {@code Collection<TdbPublisherWsResult>} with the query results.
   * @return a String with the requested printable copy.
   */
  String nonDefaultToString(Collection<TdbPublisherWsResult> results) {
    StringBuilder builder = new StringBuilder("[");
    boolean isFirst = true;

    // Loop through through all the results in the collection.
    for (TdbPublisherWsResult result : results) {
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
   * Provides a printable copy of a title database publisher-related query
   * result.
   * 
   * @param result
   *          A TdbPublisherWsResult with the query result.
   * @return a String with the requested printable copy.
   */
  private String nonDefaultToString(TdbPublisherWsResult result) {
    StringBuilder builder = new StringBuilder("TdbPublisherWsResult [");

    if (result.getName() != null) {
      builder.append("name=").append(result.getName());
    }

    return builder.append("]").toString();
  }
}
