<!--
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
--> 
# laaws-configservice [![Build Status](https://travis-ci.org/lockss/laaws-configservice.svg?branch=master)](https://travis-ci.org/lockss/laaws-configservice)
The LAAWS Configuration REST Web Service.

### Clone the repo
`git clone --recursive ssh://git@gitlab.lockss.org/laaws/laaws-configservice.git`

### Create the Eclipse project (if so desired)
File -> Import... -> Maven -> Existing Maven Projects

### Build and install the required LOCKSS daemon jar files:
run `initBuild`

### Set up the TDB tree:
Edit ./runLaawsConfig and set the TDB_DIR variable properly.

### Build the web service:
`./buildLaawsConfig`

This will run the tests as a pre-requisite for the build.

The result of the build is a so-called "uber JAR" file which includes the
project code plus all its dependencies and which is located at

`./target/laaws-configuration-service-0.0.1-SNAPSHOT.jar`

### Run the web service:
`./runLaawsConfig`

This will use port 54420. To use another port, edit the value of the
`server.port` property in file
`src/main/resources/application.properties`.

The log is at ./logs/laawsconfig.log

### Build and run the web service:
`./buildAndRunLaawsConfig`

This will use port 54420. To use another port, edit the value of the
`server.port` property in file
`src/main/resources/application.properties`.

### API is documented at:
#### http://localhost:54420/swagger-ui.html

### Stop:
`./stopLaawsConfig`
