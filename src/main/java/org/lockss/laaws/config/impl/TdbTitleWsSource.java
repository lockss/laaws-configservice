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
import java.util.Arrays;
import java.util.List;
import org.lockss.config.TdbTitle;
import org.lockss.ws.entities.TdbPublisherWsResult;
import org.lockss.ws.entities.TdbTitleWsResult;

/**
 * Container for the information that is used as the source for a query related
 * to title database titles.
 */
public class TdbTitleWsSource extends TdbTitleWsResult {
  private TdbTitle tdbTitle;

  private boolean namePopulated = false;
  private boolean tdbPublisherPopulated = false;
  private boolean idPopulated = false;
  private boolean proprietaryIdsPopulated = false;
  private boolean publicationTypePopulated = false;
  private boolean issnPopulated = false;
  private boolean issnLPopulated = false;
  private boolean eIssnPopulated = false;
  private boolean printIssnPopulated = false;
  private boolean issnsPopulated = false;

  /**
   * Constructor.
   * @param tdbTitle A TdbTitle with the TDB title data.
   */
  public TdbTitleWsSource(TdbTitle tdbTitle) {
    this.tdbTitle = tdbTitle;
  }

  @Override
  public String getName() {
    if (!namePopulated) {
      setName(tdbTitle.getName());
      namePopulated = true;
    }

    return super.getName();
  }

  @Override
  public TdbPublisherWsResult getTdbPublisher() {
    if (!tdbPublisherPopulated) {
      setTdbPublisher(new TdbPublisherWsSource(tdbTitle.getTdbPublisher()));
      tdbPublisherPopulated = true;
    }

    return super.getTdbPublisher();
  }

  @Override
  public String getId() {
    if (!idPopulated) {
      setId(tdbTitle.getId());
      idPopulated = true;
    }

    return super.getId();
  }

  /**
   * @deprecated Replaced by {@link #getProprietaryIds()}
   */
  @Override
  @Deprecated public String getProprietaryId() {
    setUpProprietaryIds();

    return super.getProprietaryId();
  }

  @Override
  public List<String> getProprietaryIds() {
    setUpProprietaryIds();

    return super.getProprietaryIds();
  }

  private void setUpProprietaryIds() {
    if (!proprietaryIdsPopulated) {
      List<String> ids = Arrays.asList(tdbTitle.getProprietaryIds());
      setProprietaryIds(ids);

      if (ids.size() > 0) {
	setProprietaryId(ids.get(0));
      }

      proprietaryIdsPopulated = true;
    }
  }

  @Override
  public String getPublicationType() {
    if (!publicationTypePopulated) {
      setPublicationType(tdbTitle.getPublicationType());
      publicationTypePopulated = true;
    }

    return super.getPublicationType();
  }

  @Override
  public String getIssn() {
    if (!issnPopulated) {
      setIssn(tdbTitle.getIssn());
      issnPopulated = true;
    }

    return super.getIssn();
  }

  @Override
  public String getIssnL() {
    if (!issnLPopulated) {
      setIssnL(tdbTitle.getIssnL());
      issnLPopulated = true;
    }

    return super.getIssnL();
  }

  @Override
  public String getEIssn() {
    if (!eIssnPopulated) {
      setEIssn(tdbTitle.getEissn());
      eIssnPopulated = true;
    }

    return super.getEIssn();
  }

  @Override
  public String getPrintIssn() {
    if (!printIssnPopulated) {
      setPrintIssn(tdbTitle.getPrintIssn());
      printIssnPopulated = true;
    }

    return super.getPrintIssn();
  }

  @Override
  public List<String> getIssns() {
    if (!issnsPopulated) {
      setIssns(new ArrayList<String>(Arrays.asList(tdbTitle.getIssns())));
      issnsPopulated = true;
    }

    return super.getIssns();
  }
}
