#!/bin/bash
#
# Copyright (c) 2018-2020 Board of Trustees of Leland Stanford Jr. University,
# all rights reserved.
#
# Permission is hereby granted, free of charge, to any person obtaining a copy
# of this software and associated documentation files (the "Software"), to deal
# in the Software without restriction, including without limitation the rights
# to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
# copies of the Software, and to permit persons to whom the Software is
# furnished to do so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in
# all copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
# STANFORD UNIVERSITY BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
# WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
# IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
#
# Except as contained in this notice, the name of Stanford University shall not
# be used in advertising or otherwise to promote the sale, use or other dealings
# in this Software without prior written authorization from Stanford University.
#

# Fixes the code generated by Swagger Codegen.

function fixImport() {
JAVA_SRC=$3
sed -i.backup "s/import $1/import $2/" $JAVA_SRC && rm $JAVA_SRC.backup
}

# Edit StatusApiDelegate.java.
STATUS_API_DELEGATE=src/generated/java/org/lockss/laaws/config/api/StatusApiDelegate.java
fixImport org.lockss.laaws.config.model.ApiStatus org.lockss.util.rest.status.ApiStatus $STATUS_API_DELEGATE

# Edit StatusApi.java.
STATUS_API=src/generated/java/org/lockss/laaws/config/api/StatusApi.java
fixImport org.lockss.laaws.config.model.ApiStatus org.lockss.util.rest.status.ApiStatus $STATUS_API

# Edit AusApiDelegate.java.
AUS_API_DELEGATE=src/generated/java/org/lockss/laaws/config/api/AusApiDelegate.java
fixImport org.lockss.laaws.config.model.AuConfiguration org.lockss.config.AuConfiguration $AUS_API_DELEGATE
fixImport org.lockss.laaws.config.model.ContentConfigurationResult org.lockss.ws.entities.ContentConfigurationResult $AUS_API_DELEGATE
fixImport org.lockss.laaws.config.model.RequestAuControlResult org.lockss.ws.entities.RequestAuControlResult $AUS_API_DELEGATE

# Edit AusApi.java.
AUS_API=src/generated/java/org/lockss/laaws/config/api/AusApi.java
fixImport org.lockss.laaws.config.model.AuConfiguration org.lockss.config.AuConfiguration $AUS_API
fixImport org.lockss.laaws.config.model.ContentConfigurationResult org.lockss.ws.entities.ContentConfigurationResult $AUS_API
fixImport org.lockss.laaws.config.model.RequestAuControlResult org.lockss.ws.entities.RequestAuControlResult $AUS_API

# Edit ConfigApiDelegate.java.
CONFIG_API_DELEGATE=src/generated/java/org/lockss/laaws/config/api/ConfigApiDelegate.java
fixImport org.lockss.laaws.config.model.PlatformConfigurationWsResult org.lockss.ws.entities.PlatformConfigurationWsResult $CONFIG_API_DELEGATE

# Edit AusApi.java.
CONFIG_API=src/generated/java/org/lockss/laaws/config/api/ConfigApi.java
fixImport org.lockss.laaws.config.model.PlatformConfigurationWsResult org.lockss.ws.entities.PlatformConfigurationWsResult $CONFIG_API

# Edit AustatusesApiDelegate.java.
AUSTATUSES_API_DELEGATE=src/generated/java/org/lockss/laaws/config/api/AustatusesApiDelegate.java
fixImport org.lockss.laaws.config.model.AuStatus org.lockss.ws.entities.AuStatus $AUSTATUSES_API_DELEGATE

# Edit AustatusesApi.java.
AUSTATUSES_API=src/generated/java/org/lockss/laaws/config/api/AustatusesApi.java
fixImport org.lockss.laaws.config.model.AuStatus org.lockss.ws.entities.AuStatus $AUSTATUSES_API

# Edit AusubstancesApiDelegate.java.
AUSUBSTANCES_API_DELEGATE=src/generated/java/org/lockss/laaws/config/api/AusubstancesApiDelegate.java
fixImport org.lockss.laaws.config.model.CheckSubstanceResult org.lockss.ws.entities.CheckSubstanceResult $AUSUBSTANCES_API_DELEGATE

# Edit AusubstancesApi.java.
AUSUBSTANCES_API=src/generated/java/org/lockss/laaws/config/api/AusubstancesApi.java
fixImport org.lockss.laaws.config.model.CheckSubstanceResult org.lockss.ws.entities.CheckSubstanceResult $AUSUBSTANCES_API

# Edit WsApiDelegate.java.
WS_API_DELEGATE=src/generated/java/org/lockss/laaws/config/api/WsApiDelegate.java
fixImport org.lockss.laaws.config.model.AuWsResult org.lockss.ws.entities.AuWsResult $WS_API_DELEGATE
fixImport org.lockss.laaws.config.model.PluginWsResult org.lockss.ws.entities.PluginWsResult $WS_API_DELEGATE
fixImport org.lockss.laaws.config.model.TdbAuWsResult org.lockss.ws.entities.TdbAuWsResult $WS_API_DELEGATE
fixImport org.lockss.laaws.config.model.TdbPublisherWsResult org.lockss.ws.entities.TdbPublisherWsResult $WS_API_DELEGATE
fixImport org.lockss.laaws.config.model.TdbTitleWsResult org.lockss.ws.entities.TdbTitleWsResult $WS_API_DELEGATE

# Edit WsApi.java.
WS_API=src/generated/java/org/lockss/laaws/config/api/WsApi.java
fixImport org.lockss.laaws.config.model.AuWsResult org.lockss.ws.entities.AuWsResult $WS_API
fixImport org.lockss.laaws.config.model.PluginWsResult org.lockss.ws.entities.PluginWsResult $WS_API
fixImport org.lockss.laaws.config.model.TdbAuWsResult org.lockss.ws.entities.TdbAuWsResult $WS_API
fixImport org.lockss.laaws.config.model.TdbPublisherWsResult org.lockss.ws.entities.TdbPublisherWsResult $WS_API
fixImport org.lockss.laaws.config.model.TdbTitleWsResult org.lockss.ws.entities.TdbTitleWsResult $WS_API

rm src/generated/java/org/lockss/laaws/config/config/SwaggerDocumentationConfig.java