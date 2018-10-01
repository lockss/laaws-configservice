#!/bin/bash
#
# Copyright (c) 2018 Board of Trustees of Leland Stanford Jr. University,
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
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL
# STANFORD UNIVERSITY BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
# WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
# IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
#
# Except as contained in this notice, the name of Stanford University shall not
# be used in advertising or otherwise to promote the sale, use or other dealings
# in this Software without prior written authorization from Stanford University.
#

# Generates code using Swagger.
#
# Arguments:
# $1: Path to Swagger code generation JAR file.

# Clean up any previously generated code.
rm -rf src/generated

# Generate the Swagger classes.
java -jar $1 generate -c docs/configGenConfig.json -i docs/swagger.yaml -l spring

# Edit ConfigApiDelegate.java.
CONFIG_API_DELEGATE=src/generated/java/org/lockss/laaws/config/api/ConfigApiDelegate.java
TEMPFILE="$(mktemp)"
sed -e "s/^}$//" $CONFIG_API_DELEGATE > "$TEMPFILE" && cat <<EOF_CONFIG_API_DELEGATE_EDIT >> "$TEMPFILE" && mv "$TEMPFILE" $CONFIG_API_DELEGATE
    /**
     * @see ConfigApi#getStatus
     */
    default ResponseEntity<org.lockss.laaws.status.model.ApiStatus> getStatus() {
      if (getObjectMapper().isPresent() && getAcceptHeader().isPresent()) {
        if (getAcceptHeader().get().contains("application/json")) {
          try {
            return new ResponseEntity<>(getObjectMapper().get()
                .readValue("{  \"ready\" : true,  \"version\" : \"version\"}", org.lockss.laaws.status.model.ApiStatus.class),
                HttpStatus.NOT_IMPLEMENTED);
          } catch (IOException e) {
            log.error("Couldn't serialize response for content type application/json", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
          }
        }
      } else {
        log.warn(
            "ObjectMapper or HttpServletRequest not configured in default StatusApi interface so no example is generated");
      }
      return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }
}
EOF_CONFIG_API_DELEGATE_EDIT

# Edit ConfigApi.java.
CONFIG_API=src/generated/java/org/lockss/laaws/config/api/ConfigApi.java
sed -i "s/public interface ConfigApi/public interface ConfigApi extends org.lockss.spring.status.SpringLockssBaseApi/" $CONFIG_API

TEMPFILE="$(mktemp)"
sed -e "s/^}$//" $CONFIG_API > "$TEMPFILE" && cat <<EOF_CONFIG_API_EDIT >> "$TEMPFILE" && mv "$TEMPFILE" $CONFIG_API
    @Override
    default ResponseEntity<org.lockss.laaws.status.model.ApiStatus> getStatus() {
      return getDelegate().getStatus();
    }
}
EOF_CONFIG_API_EDIT
