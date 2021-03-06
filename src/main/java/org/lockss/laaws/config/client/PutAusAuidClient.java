/*

 Copyright (c) 2017-2018 Board of Trustees of Leland Stanford Jr. University,
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
package org.lockss.laaws.config.client;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.lockss.config.AuConfiguration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Client for the putAusAuid() operation.
 */
public class PutAusAuidClient extends BaseClient {

  public static void main(String[] args) throws Exception {
    for (int i = 0; i < args.length; i++) {
      System.out.println("args[" + i + "] = " + args[i]);
    }

    if (args.length < 1) {
      System.err.println("ERROR: Missing command line arguments with the "
	  + "identifier of the Archival Unit for which its configuration is "
	  + "to be stored and the configuration to be stored.");
    }

    if (args.length < 2) {
      System.err.println("ERROR: Missing command line arguments with the "
	  + "Archival Unit configuration to be stored.");
    }

    for (int i = 1; i < args.length; i++) {
      System.out.println("arg[" + i + "] = " + args[i]);
    }

    Map<String, String> auConfig = new HashMap<String, String>();

    for (int i = 1; i < args.length; i++) {
      int sepLoc = args[i].trim().indexOf("=");

      if (sepLoc > 0 && sepLoc < args[i].length() - 1) {
	auConfig.put(args[i].substring(0, sepLoc),
	    args[i].substring(sepLoc + 1));
      }
    }

    AuConfiguration auConfiguration = new AuConfiguration(args[0], auConfig);

    String template = baseUri + "/aus/{auid}";

    // Create the URI of the request to the REST service.
    UriComponents uriComponents = UriComponentsBuilder.fromUriString(template)
	.build().expand(Collections.singletonMap("auid", args[0]));

    URI uri = UriComponentsBuilder.newInstance().uriComponents(uriComponents)
	.build().encode().toUri();
    System.out.println("uri = " + uri);

    ResponseEntity<AuConfiguration> response = getRestTemplate().exchange(uri,
	HttpMethod.PUT, new HttpEntity<AuConfiguration>(auConfiguration,
	    getHttpHeaders()), AuConfiguration.class);

    int status = response.getStatusCodeValue();
    System.out.println("status = " + status);
    AuConfiguration result = response.getBody();
    System.out.println("result = " + result);
  }
}
