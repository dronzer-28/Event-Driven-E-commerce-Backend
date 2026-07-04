# Build stage
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml .
COPY common-events/pom.xml common-events/pom.xml
COPY common-security/pom.xml common-security/pom.xml
COPY order-service/pom.xml order-service/pom.xml
COPY inventory-service/pom.xml inventory-service/pom.xml
COPY payment-service/pom.xml payment-service/pom.xml
COPY notification-service/pom.xml notification-service/pom.xml
COPY user-service/pom.xml user-service/pom.xml
RUN mvn -q -pl common-security,user-service -am dependency:go-offline

COPY common-security/src common-security/src
COPY user-service/src user-service/src
RUN mvn -q -pl common-security,user-service -am package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /workspace/user-service/target/user-service.jar app.jar
EXPOSE 8085
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
