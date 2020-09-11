FROM java:8

COPY ./build/Eschamali-1.0-SNAPSHOT-shaded.jar /eschamali/escha.jar

CMD java -jar /eschamali/escha.jar