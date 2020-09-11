FROM ubuntu:18.04

RUN \
apt update -y && \
apt install -y \
	git openjdk-8-jdk

COPY docker/*.sh /root/

WORKDIR /eschamali

VOLUME /data

CMD /root/install.sh && /root/start.sh