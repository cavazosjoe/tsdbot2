FROM openjdk:8
RUN mkdir /app
ADD tsdbot-1.0-SNAPSHOT.jar /app/tsdbot.jar
ADD prod.yml /app/prod.yml
CMD java -jar /app/tsdbot.jar server /app/prod.yml
EXPOSE 8080