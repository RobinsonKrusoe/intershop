FROM openjdk:21-jdk-slim
COPY target/*.jar intershop.jar
ENTRYPOINT ["java","-jar","/intershop.jar"]