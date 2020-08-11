FROM openjdk:11-jdk

RUN apt-get update && apt-get install tar

ENV UID 1000
ENV GID 1000

# TODO : Change for port 80
EXPOSE 8080
EXPOSE 34197-34297

RUN addgroup --system --gid $GID spring && adduser --system --uid $UID spring --ingroup spring
USER spring:spring

ARG JAR_FILE=target/*.jar
ENTRYPOINT ["java","-jar","/app.jar"]
COPY ${JAR_FILE} app.jar
