# Copyright © 2016 Cisco Systems, Inc. and others.  All rights reserved.
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html
#
# Authors: Peter Gubka, Jan Medved
#
# This Dockerfile builds a Docker container that can be used for testing the
# OpenDaylight controller in a clustered configuration.
#

FROM ubuntu:16.04
MAINTAINER Jan Medved <jmedved@cisco.com>

RUN apt-get update && \
    apt-get install -y \
      lsb-release \
      openjdk-8-jre \
      openssh-client \
      openssh-server \
      nano \
      net-tools \
      python \
      sudo \
      zip && \
    echo 'export JAVA_HOME="/usr/lib/jvm/java-1.8.0-openjdk-amd64"' >>~/.bashrc && \
    echo 'root:docker123' | chpasswd && \
    sed -i 's/prohibit-password/yes/' /etc/ssh/sshd_config && \
    mkdir /var/run/sshd

# Setup the default user
RUN useradd odl -G sudo -s /bin/bash -m && \
    echo "odl:docker123" | chpasswd && \
    echo "%sudo ALL=(ALL)    NOPASSWD: ALL" >> /etc/sudoers

USER odl
RUN mkdir -p /home/odl/.ssh

COPY configure-cluster-ipdetect.sh /opt/scripts/configure-cluster-ipdetect.sh
COPY install_odl.sh /opt/scripts/install_odl.sh

USER root

CMD ["/usr/sbin/sshd", "-D"]

