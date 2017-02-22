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

### Build the web service:
`./buildLaawsConfig`

This will use port 8888 during the build. To use, for example, port 8889,
instead, either edit the value of $service_port in ./buildLaawsConfig or run:

`./buildLaawsConfig 8889`

The result of the build is a so-called "uber JAR" file which includes the
project code plus all its dependencies and which is located at

`./target/laaws-config-service-swarm.jar`

### Run the web service:
`./runLaawsConfig`

This will listen to port 8888. To use, for example, port 8889, instead, either
edit the value of $service_port in ./runLaawsConfig or run:

`./runLaawsConfig 8889`

The log is at ./logs/laawsconfig.log

### Build and run the web service:
`./buildAndRunLaawsConfig`

This will use port 8888 for both steps. To use, for example, port 8889, instead,
either edit the value of $service_port in ./buildAndRunLaawsConfig or run:

`./buildAndRunLaawsConfig 8889`

### Stop:
`./stopLaawsConfig`

### API is documented at:
#### localhost:8888/docs/
