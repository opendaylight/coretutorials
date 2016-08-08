# This is a comment
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

COPY configure-cluster-ipdetect.sh /opt/scripts/configure-cluster-ipdetect.sh
COPY install_odl.sh /opt/scripts/install_odl.sh

CMD ["/usr/sbin/sshd", "-D"]

