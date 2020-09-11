FROM ubuntu:18.04

RUN \
apt update -y && \
apt install -y \
	git openjdk-8-jdk

COPY docker/*.sh /root/
RUN chmod 0755 /root/*.sh

VOLUME /data

CMD /bin/bash /root/install.sh && /bin/bash /root/start.sh