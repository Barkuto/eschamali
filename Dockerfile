FROM java:8

WORKDIR /eschamali/

COPY Eschamali-1.0-SNAPSHOT-shaded.jar escha.jar

CMD java -jar escha.jar