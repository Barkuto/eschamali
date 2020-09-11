FROM ubuntu:18.04

RUN \
apt update -y && \
apt install -y \
	git openjdk-8-jdk

WORKDIR /eschamali

COPY build/Eschamali-1.0-SNAPSHOT-shaded.jar escha.jar

CMD ["java", "-jar", "escha.jar"]