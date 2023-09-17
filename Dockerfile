FROM openjdk:11

WORKDIR /

ARG path="target/scala-2.13"

ADD ./${path}/civil-assembly-0.0.1.jar /
ADD ./src/main/resources/application.conf /

# ENV BUILD_ENV "development"
# ENV BUILD_ENV $BUILD_ENV

ENV SCALA_ENV "prod"

EXPOSE 8090

CMD java -jar ./civil-assembly-0.0.1.jar
