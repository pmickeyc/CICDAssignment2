FROM eclipse-temurin:21-jre

ARG JAR_FILE=target/payment-api-0.0.1-SNAPSHOT.jar

WORKDIR /opt/payment-api

COPY ${JAR_FILE} app.jar

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "/opt/payment-api/app.jar"]
