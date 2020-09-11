FROM java:8

RUN mkdir /eschamali

COPY build/Eschamali-1.0-SNAPSHOT-shaded.jar /eschamali/escha.jar

VOLUME /eschamali

CMD java -jar /eschamali/escha.jar