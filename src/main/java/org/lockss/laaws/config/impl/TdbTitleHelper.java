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
import org.lockss.config.TdbTitle;
import org.lockss.config.TdbUtil;
import org.lockss.util.Logger;
import org.lockss.ws.entities.TdbTitleWsResult;

/**
 * Helper of the web service implementation of title database title queries.
 */
public class TdbTitleHelper {
  /**
   * The fully-qualified name of the class of the objects used as source in a
   * query.
   */
  static String SOURCE_FQCN = TdbTitleWsSource.class.getCanonicalName();

  /**
   * The fully-qualified name of the class of the objects returned by the query.
   */
  static String RESULT_FQCN = TdbTitleWsResult.class.getCanonicalName();

  //
  // Property names used in title database title queries.
  //
  static String NAME = "name";
  static String TDB_PUBLISHER = "tdbPublisher";
  static String ID = "id";
  static String PROPRIETARY_ID = "proprietaryId";
  static String PROPRIETARY_IDS = "proprietaryIds";
  static String PUBLICATION_TYPE = "publicationType";
  static String ISSN = "issn";
  static String ISSN_L = "issnL";
  static String E_ISSN = "eIssn";
  static String PRINT_ISSN = "printIssn";
  static String ISSNS = "issns";

  /**
   * All the property names used in title database title queries.
   */
  @SuppressWarnings("serial")
  static final Set<String> PROPERTY_NAMES = new HashSet<String>() {
    {
      add(NAME);
      add(TDB_PUBLISHER);
      add(ID);
      add(PROPRIETARY_ID);
      add(PROPRIETARY_IDS);
      add(PUBLICATION_TYPE);
      add(ISSN);
      add(ISSN_L);
      add(E_ISSN);
      add(PRINT_ISSN);
      add(ISSNS);
    }
  };

  private static Logger log = Logger.getLogger();

  /**
   * Provides the universe of title database title-related query objects used as
   * the source for a query.
   * 
   * @return a List<TdbTitleWsSource> with the universe.
   */
  List<TdbTitleWsSource> createUniverse() {
    final String DEBUG_HEADER = "createUniverse(): ";

    // Get all the title database titles.
    List<TdbTitle> allTdbTitles = TdbUtil.getAllTdbTitles();
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "allTdbTitles.size() = "
	+ allTdbTitles.size());

    // Initialize the universe.
    List<TdbTitleWsSource> universe =
	new ArrayList<TdbTitleWsSource>(allTdbTitles.size());

    // Loop through all the title database titles.
    for (TdbTitle tdbTitle : allTdbTitles) {
      // Add the object initialized with this title database title to the
      // universe of objects.
      universe.add(new TdbTitleWsSource(tdbTitle));
    }

    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "universe.size() = " + universe.size());
    return universe;
  }

  /**
   * Provides a printable copy of a collection of title database title-related
   * query results.
   * 
   * @param results
   *          A {@code Collection<TdbTitleWsResult>} with the query results.
   * @return a String with the requested printable copy.
   */
  String nonDefaultToString(Collection<TdbTitleWsResult> results) {
    StringBuilder builder = new StringBuilder("[");
    boolean isFirst = true;

    // Loop through through all the results in the collection.
    for (TdbTitleWsResult result : results) {
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
   * Provides a printable copy of a title database title-related query result.
   * 
   * @param result
   *          A TdbTitleWsResult with the query result.
   * @return a String with the requested printable copy.
   */
  private String nonDefaultToString(TdbTitleWsResult result) {
    StringBuilder builder = new StringBuilder("TdbTitleWsResult [");
    boolean isFirst = true;

    if (result.getName() != null) {
      builder.append("name=").append(result.getName());
      isFirst = false;
    }

    if (result.getTdbPublisher() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("tdbPublisher=").append(result.getTdbPublisher());
    }

    if (result.getId() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("id=").append(result.getId());
    }

    if (result.getProprietaryId() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("proprietaryId=").append(result.getProprietaryId());
    }

    if (result.getProprietaryIds() != null
	&& result.getProprietaryIds().size() > 0) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("proprietaryIds=").append(result.getProprietaryIds());
    }

    if (result.getPublicationType() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("publicationType=").append(result.getPublicationType());
    }

    if (result.getIssn() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("issn=").append(result.getIssn());
    }

    if (result.getIssnL() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("issnL=").append(result.getIssnL());
    }

    if (result.getEIssn() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("eIssn=").append(result.getEIssn());
    }

    if (result.getPrintIssn() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("printIssn=").append(result.getPrintIssn());
    }

    if (result.getIssns() != null && result.getIssns().size() > 0) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("issns=").append(result.getIssns());
    }

    return builder.append("]").toString();
  }
}
