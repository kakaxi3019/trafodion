# @@@ START COPYRIGHT @@@
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#
# @@@ END COPYRIGHT @@@

# This Makefile is just a thin shell to Maven, which is used to do the real build

include ../macros.gmk #top level

.NOTPARALLEL: all

all: build_all

build_all:	 
	$(MAKE) mavenbuild
	echo "$(MAVEN) site package -DskipTests"
	echo "### For full Maven output, see file build_utility.log"

mavenbuild:
        # create a jar manifest file with the correct version information
	mkdir -p src/main/resources
	$(TRAF_HOME)/export/include/SCMBuildJava.sh 1.0.1 > src/main/resources/trafodion-utility.jar.mf
        # run maven
	set -o pipefail && $(MAVEN) package -DskipTests | tee maven_build.log | grep -e '\[INFO\] Building' -e '\[INFO\] BUILD SUCCESS' -e 'ERROR'
	cp -pf target/trafodion-utility-*.jar $(TRAF_HOME)/export/lib

clean:
	$(MAVEN) clean
	$(RM) build_utility.log
	$(RM) -rf $(TRAF_HOME)/export/lib/trafodion-utility-*.jar
