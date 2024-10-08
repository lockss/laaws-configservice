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

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.lockss.app.LockssDaemon;
import org.lockss.config.AuConfiguration;
import org.lockss.config.ConfigManager;
import org.lockss.daemon.CrawlWindow;
import org.lockss.daemon.TitleConfig;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.AuUtil;
import org.lockss.plugin.CachedUrlSet;
import org.lockss.plugin.Plugin;
import org.lockss.plugin.PluginManager;
import org.lockss.state.AuState;
import org.lockss.state.StateManager;
import org.lockss.util.Logger;
import org.lockss.util.StringUtil;
import org.lockss.ws.entities.AuStatus;
import org.lockss.ws.entities.AuWsResult;

/**
 * Helper of the DaemonStatus web service implementation of Archival Unit
 * queries.
 */
public class AuHelper {
  /**
   * The fully-qualified name of the class of the objects used as source in a
   * query.
   */
  static String SOURCE_FQCN = AuWsSource.class.getCanonicalName();

  /**
   * The fully-qualified name of the class of the objects returned by the query.
   */
  static String RESULT_FQCN = AuWsResult.class.getCanonicalName();

  //
  // Property names used in Archival Unit queries.
  //
  static String AU_ID = "auId";
  static String NAME = "name";
  static String VOLUME = "volume";
  static String PLUGIN_NAME = "pluginName";
  static String TDB_YEAR = "tdbYear";
  static String ACCESS_TYPE = "accessType";
  static String CONTENT_SIZE = "contentSize";
  static String DISK_USAGE = "diskUsage";
  static String REPOSITORY_PATH = "repositoryPath";
  static String RECENT_POLL_AGREEMENT = "recentPollAgreement";
  static String HIGHEST_POLL_AGREEMENT = "highestPollAgreement";
  static String PUBLISHING_PLATFORM = "publishingPlatform";
  static String TDB_PUBLISHER = "tdbPublisher";
  static String AVAILABLE_FROM_PUBLISHER = "availableFromPublisher";
  static String SUBSTANCE_STATE = "substanceState";
  static String CREATION_TIME = "creationTime";
  static String CRAWL_PROXY = "crawlProxy";
  static String CRAWL_WINDOW = "crawlWindow";
  static String CRAWL_POOL = "crawlPool";
  static String LAST_COMPLETED_CRAWL = "lastCompletedCrawl";
  static String LAST_CRAWL = "lastCrawl";
  static String LAST_CRAWL_RESULT = "lastCrawlResult";
  static String LAST_COMPLETED_DEEP_CRAWL = "lastCompletedDeepCrawl";
  static String LAST_DEEP_CRAWL = "lastDeepCrawl";
  static String LAST_DEEP_CRAWL_RESULT = "lastDeepCrawlResult";
  static String LAST_COMPLETED_DEEP_CRAWL_DEPTH = "lastCompletedDeepCrawlDepth";
  static String LAST_METADATA_INDEX = "lastMetadataIndex";
  static String LAST_COMPLETED_POLL = "lastCompletedPoll";
  static String LAST_POLL = "lastPoll";
  static String LAST_POLL_RESULT = "lastPollResult";
  static String CURRENTLY_CRAWLING = "currentlyCrawling";
  static String CURRENTLY_POLLING = "currentlyPolling";
  static String SUBSCRIPTION_STATUS = "subscriptionStatus";
  static String AU_CONFIGURATION = "auConfiguration";
  static String NEW_CONTENT_CRAWL_URLS = "newContentCrawlUrls";
  static String URL_STEMS = "urlStems";
  static String IS_BULK_CONTENT = "isBulkContent";
  static String PEER_AGREEMENTS = "peerAgreements";
  static String URLS = "urls";
  static String ACCESS_URLS = "accessUrls";
  static String SUBSTANCE_URLS = "substanceUrls";
  static String ARTICLE_URLS = "articleUrls";
  static String JOURNAL_TITLE = "journalTitle";
  static String TDB_PROVIDER = "tdbProvider";

  /**
   * All the property names used in Archival Unit queries.
   */
  @SuppressWarnings("serial")
  static final Set<String> PROPERTY_NAMES = new HashSet<String>() {
    {
      add(AU_ID);
      add(NAME);
      add(VOLUME);
      add(PLUGIN_NAME);
      add(TDB_YEAR);
      add(ACCESS_TYPE);
      add(CONTENT_SIZE);
      add(DISK_USAGE);
      add(REPOSITORY_PATH);
      add(RECENT_POLL_AGREEMENT);
      add(HIGHEST_POLL_AGREEMENT);
      add(PUBLISHING_PLATFORM);
      add(TDB_PUBLISHER);
      add(AVAILABLE_FROM_PUBLISHER);
      add(SUBSTANCE_STATE);
      add(CREATION_TIME);
      add(CRAWL_PROXY);
      add(CRAWL_WINDOW);
      add(CRAWL_POOL);
      add(LAST_COMPLETED_CRAWL);
      add(LAST_CRAWL);
      add(LAST_CRAWL_RESULT);
      add(LAST_COMPLETED_DEEP_CRAWL);
      add(LAST_DEEP_CRAWL);
      add(LAST_DEEP_CRAWL_RESULT);
      add(LAST_COMPLETED_DEEP_CRAWL_DEPTH);
      add(LAST_METADATA_INDEX);
      add(LAST_COMPLETED_POLL);
      add(LAST_POLL);
      add(LAST_POLL_RESULT);
      add(CURRENTLY_CRAWLING);
      add(CURRENTLY_POLLING);
      add(SUBSCRIPTION_STATUS);
      add(AU_CONFIGURATION);
      add(NEW_CONTENT_CRAWL_URLS);
      add(URL_STEMS);
      add(IS_BULK_CONTENT);
      add(PEER_AGREEMENTS);
      add(URLS);
      add(ACCESS_URLS);
      add(SUBSTANCE_URLS);
      add(ARTICLE_URLS);
      add(JOURNAL_TITLE);
      add(TDB_PROVIDER);
    }
  };

  private static Logger log = Logger.getLogger();

  /**
   * Provides the status information of an archival unit in the system.
   * 
   * @param auId
   *          A String with the identifier of the archival unit.
   * @return an AuStatus with the status information of the archival unit.
   */
  AuStatus getAuStatus(String auId) {
    final String DEBUG_HEADER = "getAuStatus(): ";

    LockssDaemon theDaemon = LockssDaemon.getLockssDaemon();
    PluginManager pluginMgr = theDaemon.getPluginManager();
    ArchivalUnit au = pluginMgr.getAuFromIdIfExists(auId);

    if (au == null) {
      throw new IllegalArgumentException(
	  "No Archival Unit with provided identifier = " + auId);
    }

    AuStatus result = new AuStatus();
    result.setVolume(au.getName());

    TitleConfig tc = au.getTitleConfig();
    if (tc != null) {
      result.setJournalTitle(tc.getJournalTitle());
    }

    Plugin plugin = au.getPlugin();
    result.setPluginName(plugin.getPluginName());

    result.setYear(AuUtil.getTitleAttribute(au, "year"));

    AuState state =
      theDaemon.getManagerByType(StateManager.class).getAuState(au);
    AuState.AccessType atype = state.getAccessType();

    if (atype != null) {
      result.setAccessType(atype.toString());
    }

    long contentSize = AuUtil.getAuContentSize(au, false);

    if (contentSize != -1) {
      result.setContentSize(contentSize);
    }

    long du = AuUtil.getAuDiskUsage(au, false);

    if (du != -1) {
      result.setDiskUsage(du);
    }

    // XXXREPO
//     String spec = OldLockssRepositoryImpl.getRepositorySpec(au);
//     String repo = OldLockssRepositoryImpl.mapAuToFileLocation(
// 	OldLockssRepositoryImpl.getLocalRepositoryPath(spec), au);
//     result.setRepository(repo);

    CachedUrlSet auCus = au.getAuCachedUrlSet();
    if (state.getV3Agreement() < 0) {
      if (state.getLastCrawlTime() < 0) {
	result.setStatus("Waiting for Crawl");
      } else {
	result.setStatus("Waiting for Poll");
      }
    } else {
      result.setStatus(doubleToPercent(state.getHighestV3Agreement())
		       + "% Agreement");
      if (state.getHighestV3Agreement() != state.getV3Agreement()) {
	result.setRecentPollAgreement(state.getV3Agreement());
      }
    }


    String publishingPlatform = plugin.getPublishingPlatform();

    if (!StringUtil.isNullString(publishingPlatform)) {
      result.setPublishingPlatform(publishingPlatform);
    }

    String publisher = AuUtil.getTitleAttribute(au, "publisher");

    if (!StringUtil.isNullString(publisher)) {
      result.setPublisher(publisher);
    }

    result.setAvailableFromPublisher(!AuUtil.isPubDown(au));
    result.setSubstanceState(state.getSubstanceState().toString());
    result.setCreationTime(state.getAuCreationTime());

    AuUtil.AuProxyInfo aupinfo = AuUtil.getAuProxyInfo(au);

    if (aupinfo.isInvalidAuOverride()) {
      result.setCrawlProxy("Invalid AU proxy spec: " + aupinfo.getAuSpec());
    } else if (aupinfo.isAuOverride()) {
      String disp =
	(aupinfo.getHost() == null
	 ? "Direct connection" : aupinfo.getHost() + ":" + aupinfo.getPort());
      result.setCrawlProxy(disp);
    }

    CrawlWindow window = au.getCrawlWindow();

    if (window != null) {
      String wmsg = window.toString();

      if (wmsg.length() > 140) {
	wmsg = "(not displayable)";
      }

      if (!window.canCrawl()) {
	wmsg = "Currently closed: " + wmsg;
      }

      result.setCrawlWindow(wmsg);
    }

    String crawlPool = au.getFetchRateLimiterKey();

    if (crawlPool == null) {
      crawlPool = "(none)";
    }

    result.setCrawlPool(crawlPool);

    result.setLastCompletedCrawl(state.getLastCrawlTime());
    result.setLastCompletedDeepCrawl(state.getLastDeepCrawlTime());

    long lastCrawlAttempt = state.getLastCrawlAttempt();
    long lastDeepCrawlAttempt = state.getLastDeepCrawlAttempt();

    if (lastCrawlAttempt > 0) {
      result.setLastCrawl(lastCrawlAttempt);
      result.setLastCrawlResult(state.getLastCrawlResultMsg());
    }
    if (lastDeepCrawlAttempt > 0) {
      result.setLastDeepCrawl(lastDeepCrawlAttempt);
      result.setLastDeepCrawlResult(state.getLastDeepCrawlResultMsg());
      result.setLastCompletedDeepCrawlDepth(state.getLastDeepCrawlDepth());
    }

    result.setLastMetadataIndex(state.getLastMetadataIndex());

    long lastTopLevelPollTime = state.getLastTopLevelPollTime();

    if (lastTopLevelPollTime > 0) {
      result.setLastCompletedPoll(lastTopLevelPollTime);
    }

    long lastPollStart = state.getLastPollStart();

    if (lastPollStart > 0) {
      result.setLastPoll(lastPollStart);
      String pollResult = state.getLastPollResultMsg();

      if (!StringUtil.isNullString(pollResult)) {
	result.setLastPollResult(state.getLastPollResultMsg());
      }
    }

    result.setCurrentlyCrawling(theDaemon.getCrawlManager().getStatusSource()
	.getStatus().isRunningNCCrawl(au));

//  TBD: Implement when there is a way to get the information.
//    result.setCurrentlyPolling(theDaemon.getPollManager().isPollRunning(au));

    if (theDaemon.isDetectClockssSubscription()) {
      result.setSubscriptionStatus(AuUtil.getAuState(au)
	  .getClockssSubscriptionStatusString());
    }

    String provider = AuUtil.getTitleAttribute(au, "provider");

    if (!StringUtil.isNullString(provider)) {
      result.setProvider(provider);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result = " + result);
    return result;
  }

  /**
   * Provides the universe of Archival Unit-related query objects used as the
   * source for a query.
   * 
   * @return a List<AuWsSource> with the universe.
   */
  List<AuWsSource> createUniverse() {
    final String DEBUG_HEADER = "createUniverse(): ";

    try {
      // Get the configurations of all the configured Archival Units.
      Collection<AuConfiguration> allAuConfigs = ConfigManager
	  .getConfigManager().retrieveAllArchivalUnitConfiguration();
      if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "allAuConfigs = " + allAuConfigs);

      // Initialize the universe.
      List<AuWsSource> universe =
	  new ArrayList<AuWsSource>(allAuConfigs.size());

      // Loop through all the configured Archival Unit configurations.
      for (AuConfiguration auConfig : allAuConfigs) {
	String auId = auConfig.getAuId();
	if (log.isDebug3())
	  log.debug3(DEBUG_HEADER + "auId = " + auId);

	// Get the Archival Unit.
	ArchivalUnit au =
	    LockssDaemon.getLockssDaemon().getPluginManager().getAuFromId(auId);
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "au = " + au);

	// Add the object initialized with this Archival Unit to the universe of
	// objects.
	universe.add(new AuWsSource(au));
      }

      if (log.isDebug2())
	log.debug2(DEBUG_HEADER + "universe.size() = " + universe.size());
      return universe;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Provides a printable copy of a collection of Archival Unit-related query
   * results.
   * 
   * @param results
   *          A {@code Collection<AuWsResult>} with the query results.
   * @return a String with the requested printable copy.
   */
  String nonDefaultToString(Collection<AuWsResult> results) {
    StringBuilder builder = new StringBuilder("[");
    boolean isFirst = true;

    // Loop through through all the results in the collection.
    for (AuWsResult result : results) {
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
   * Formats 100 times a double to print as a percentage. An input value of 1.0
   * will produce "100.00".
   * 
   * @param d
   *          A double with the value to convert.
   * @return a String representing the double.
   */
  private String doubleToPercent(double d) {
    int i = (int) (d * 10000);
    double pc = i / 100.0;
    return new DecimalFormat("0.00").format(pc);
  }

  /**
   * Provides a printable copy of an Archival Unit-related query result.
   * 
   * @param result
   *          An AuWsResult with the query result.
   * @return a String with the requested printable copy.
   */
  private String nonDefaultToString(AuWsResult result) {
    StringBuilder builder = new StringBuilder("AuWsResult [");
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

    if (result.getVolume() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("volume=").append(result.getVolume());
    }

    if (result.getPluginName() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("pluginName=").append(result.getPluginName());
    }

    if (result.getTdbYear() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("tdbYear=").append(result.getTdbYear());
    }

    if (result.getAccessType() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("accessType=").append(result.getAccessType());
    }

    if (result.getContentSize() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("contentSize=").append(result.getContentSize());
    }

    if (result.getDiskUsage() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("diskUsage=").append(result.getDiskUsage());
    }

    if (result.getRepositoryPath() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("repositoryPath=").append(result.getRepositoryPath());
    }

    if (result.getRecentPollAgreement() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("recentPollAgreement=")
      .append(result.getRecentPollAgreement());
    }

    if (result.getHighestPollAgreement() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("highestPollAgreement=")
      .append(result.getHighestPollAgreement());
    }

    if (result.getPublishingPlatform() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("publishingPlatform=")
      .append(result.getPublishingPlatform());
    }

    if (result.getTdbPublisher() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("tdbPublisher=").append(result.getTdbPublisher());
    }

    if (result.getAvailableFromPublisher() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("availableFromPublisher=")
      .append(result.getAvailableFromPublisher());
    }

    if (result.getSubstanceState() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("substanceState=").append(result.getSubstanceState());
    }

    if (result.getCreationTime() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("creationTime=").append(result.getCreationTime());
    }

    if (result.getCrawlProxy() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("crawlProxy=").append(result.getCrawlProxy());
    }

    if (result.getCrawlWindow() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("crawlWindow=").append(result.getCrawlWindow());
    }

    if (result.getCrawlPool() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("crawlPool=").append(result.getCrawlPool());
    }

    if (result.getLastCompletedCrawl() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("lastCompletedCrawl=")
      .append(result.getLastCompletedCrawl());
    }

    if (result.getLastCrawl() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("lastCrawl=").append(result.getLastCrawl());
    }

    if (result.getLastCrawlResult() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("lastCrawlResult=").append(result.getLastCrawlResult());
    }

    if (result.getLastCompletedDeepCrawl() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("lastCompletedDeepCrawl=")
      .append(result.getLastCompletedDeepCrawl());
    }

    if (result.getLastDeepCrawl() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("lastDeepCrawl=").append(result.getLastDeepCrawl());
    }

    if (result.getLastDeepCrawlResult() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("lastDeepCrawlResult=").append(result.getLastDeepCrawlResult());
    }

    if (result.getLastCompletedDeepCrawlDepth() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("lastCompletedDeepCrawlDepth=").append(result.getLastCompletedDeepCrawlDepth());
    }

    if (result.getLastMetadataIndex() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("lastMetadataIndex=")
      .append(result.getLastMetadataIndex());
    }

    if (result.getLastCompletedPoll() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("lastCompletedPoll=")
      .append(result.getLastCompletedPoll());
    }

    if (result.getLastPoll() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("lastPoll=").append(result.getLastPoll());
    }

    if (result.getLastPollResult() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("lastPollResult=").append(result.getLastPollResult());
    }

    if (result.getCurrentlyCrawling() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("currentlyCrawling=")
      .append(result.getCurrentlyCrawling());
    }

    if (result.getCurrentlyPolling() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("currentlyPolling=").append(result.getCurrentlyPolling());
    }

    if (result.getSubscriptionStatus() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("subscriptionStatus=")
      .append(result.getSubscriptionStatus());
    }

    if (result.getAuConfiguration() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("auConfiguration=").append(result.getAuConfiguration());
    }

    if (result.getNewContentCrawlUrls() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("newContentCrawlUrls=")
      .append(result.getNewContentCrawlUrls());
    }

    if (result.getUrlStems() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("urlStems=").append(result.getUrlStems());
    }

    if (result.getIsBulkContent() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("isBulkContent=").append(result.getIsBulkContent());
    }

    if (result.getPeerAgreements() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("peerAgreements=").append(result.getPeerAgreements());
    }

    if (result.getUrls() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("urls=").append(result.getUrls());
    }

    if (result.getSubstanceUrls() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("substanceUrls=").append(result.getSubstanceUrls());
    }

    if (result.getArticleUrls() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("articleUrls=").append(result.getArticleUrls());
    }

    if (result.getJournalTitle() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("journalTitle=").append(result.getJournalTitle());
    }

    if (result.getTdbProvider() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("tdbProvider=").append(result.getTdbProvider());
    }

    return builder.append("]").toString();
  }
}
