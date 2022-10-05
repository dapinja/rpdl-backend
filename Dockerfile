# syntax=docker/dockerfile:1
FROM gradle:7.5.1-jdk17 AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src

RUN mkdir torrust
WORKDIR ./torrust

RUN git clone https://github.com/XwyDevelopment/TorrustApiWrapper .

RUN gradle publishMainPublicationToMavenLocal --no-daemon

WORKDIR ..

RUN gradle shadowjar --no-daemon

FROM openjdk:17-alpine

RUN mkdir /app

COPY --from=build /home/gradle/src/build/libs/*.jar /app/app.jar

EXPOSE 5671

CMD ["java", "-jar", "/app.jar"]