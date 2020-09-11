FROM java:8

WORKDIR /eschamali
COPY Eschamali-1.0-SNAPSHOT-shaded.jar escha.jar

VOLUME /eschamali

CMD java -jar escha.jar