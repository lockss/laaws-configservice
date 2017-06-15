/*

 Copyright (c) 2017 Board of Trustees of Leland Stanford Jr. University,
 all rights reserved.

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in
 all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL
 STANFORD UNIVERSITY BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

 Except as contained in this notice, the name of Stanford University shall not
 be used in advertising or otherwise to promote the sale, use or other dealings
 in this Software without prior written authorization from Stanford University.

 */
package org.lockss.laaws.config.api;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import org.lockss.laaws.config.model.ConfigModSpec;

/**
 * Base provider of access to the configuration.
 */
public abstract class ConfigApiService {

  public static final String SECTION_NAME_CLUSTER = "cluster";
  public static final String SECTION_NAME_UI_IP_ACCESS = "ui_ip_access";
  public static final String SECTION_NAME_PROXY_IP_ACCESS = "proxy_ip_access";
  public static final String SECTION_NAME_PLUGIN = "plugin";
  public static final String SECTION_NAME_AU = "au";
  public static final String SECTION_NAME_TITLE_DB = "titledb";
  public static final String SECTION_NAME_ICP_SERVER = "icp_server";
  public static final String SECTION_NAME_AUDIT_PROXY = "audit_proxy";
  public static final String SECTION_NAME_CONTENT_SERVERS = "content_servers";
  public static final String SECTION_NAME_ACCESS_GROUPS = "access_groups";
  public static final String SECTION_NAME_CRAWL_PROXY = "crawl_proxy";
  public static final String SECTION_NAME_EXPERT = "expert";
  public static final String SECTION_NAME_ALERT = "alert";
  public static final String SECTION_NAME_CRONSTATE = "cronstate";

  /**
   * Deletes the configuration for a section given the section name.
   * 
   * @param sectionName
   *          A String with the section name.
   * @param securityContext
   *          A SecurityContext providing access to security related
   *          information.
   * @return a Response with any data that needs to be returned to the runtime.
   * @throws ApiException
   *           if there are problems.
   */
  public abstract Response deleteConfig(String sectionName,
      SecurityContext securityContext) throws ApiException;

  /**
   * Provides the configuration for a section given the section name.
   * 
   * @param sectionName
   *          A String with the section name.
   * @param securityContext
   *          A SecurityContext providing access to security related
   *          information.
   * @return a Response with any data that needs to be returned to the runtime.
   * @throws ApiException
   *           if there are problems.
   */
  public abstract Response getConfig(String sectionName,
      SecurityContext securityContext) throws ApiException;

  /**
   * Provides the timestamp of the last time the configuration was updated.
   * 
   * @param securityContext
   *          A SecurityContext providing access to security related
   *          information.
   * @return a Response with any data that needs to be returned to the runtime.
   * @throws ApiException
   *           if there are problems.
   */
  public abstract Response getLastUpdateTime(SecurityContext securityContext)
      throws ApiException;

  /**
   * Provides the URLs from which the configuration was loaded.
   * 
   * @param securityContext
   *          A SecurityContext providing access to security related
   *          information.
   * @return a Response with any data that needs to be returned to the runtime.
   * @throws ApiException
   *           if there are problems.
   */
  public abstract Response getLoadedUrlList(SecurityContext securityContext)
      throws ApiException;

  /**
   * Modifies the configuration for a section given the section name.
   * 
   * @param sectionName
   *          A String with the section name.
   * @param configModSpec
   *          A ConfigModSpec with the configuration modifications.
   * @param securityContext
   *          A SecurityContext providing access to security related
   *          information.
   * @return a Response with any data that needs to be returned to the runtime.
   * @throws ApiException
   *           if there are problems.
   */
  public abstract Response putConfig(String sectionName, ConfigModSpec
      configModSpec, SecurityContext securityContext) throws ApiException;

  /**
   * Requests a reload of the configuration.
   * 
   * @param securityContext
   *          A SecurityContext providing access to security related
   *          information.
   * @return a Response with any data that needs to be returned to the runtime.
   * @throws ApiException
   *           if there are problems.
   */
  public abstract Response requestReload(SecurityContext securityContext)
      throws ApiException;
}
