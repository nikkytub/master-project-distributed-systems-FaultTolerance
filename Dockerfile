################################################################################
#  Licensed to the Apache Software Foundation (ASF) under one
#  or more contributor license agreements.  See the NOTICE file
#  distributed with this work for additional information
#  regarding copyright ownership.  The ASF licenses this file
#  to you under the Apache License, Version 2.0 (the
#  "License"); you may not use this file except in compliance
#  with the License.  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
# limitations under the License.
################################################################################

FROM openjdk:8-jre-alpine

# Install requirements
RUN apk add --no-cache bash snappy

### install iperf to make bandwidth measurements
### to determine connectivity to the neighbor nodes
RUN apk add --no-cache iperf

## add lldp packages
RUN echo "@testing http://dl-cdn.alpinelinux.org/alpine/edge/testing" > /etc/apk/repositories \
&& echo "@main http://dl-cdn.alpinelinux.org/alpine/edge/main" >> /etc/apk/repositories

RUN apk add --no-cache readline@main \
&& apk add --no-cache libevent@main \
&& apk add --no-cache bash@main \
&& apk add --no-cache net-snmp-dev@main \
&& apk add --no-cache libxml2@main \
&& apk add --no-cache lldpd@testing

# Flink environment variables
ENV FLINK_INSTALL_PATH=/opt
ENV FLINK_HOME $FLINK_INSTALL_PATH/flink
ENV PATH $PATH:$FLINK_HOME/bin

### Node Config for Edge Nodes
ENV ID "DOCKER_UNKNOWN"
ENV LOCATION "UNKNOWN_LOCATION"
ENV NEIGHBORS ""
ENV TASKSLOTS 1

## Metrics
ENV GRAPHITE_HOST ""
ENV GRAPHITE_PORT 2003

ENV PROMETHEUS_HOST ""
ENV PROMETHEUS_PORT 9250

ENV STSD_HOST ""
ENV STSD_PORT 8127

# flink-dist can point to a directory or a tarball on the local system
ARG flink_dist=NOT_SET

# Install build dependencies and flink
ADD $flink_dist $FLINK_INSTALL_PATH

RUN set -x && \
  ln -s $FLINK_INSTALL_PATH/flink-* $FLINK_HOME && \
  addgroup -S flink && adduser -D -S -H -G flink -h $FLINK_HOME flink && \
  chown -R flink:flink $FLINK_INSTALL_PATH/flink-* && \
  chown -h flink:flink $FLINK_HOME

COPY docker-entrypoint.sh /

#USER flink
#EXPOSE 8081 6123
ENTRYPOINT ["/docker-entrypoint.sh"]
CMD ["--help"]