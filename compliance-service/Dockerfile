# Build context is the repository root (not this directory), e.g.:
#   docker build -f compliance-service/Dockerfile -t compliance-service .

FROM maven:3.9-eclipse-temurin-25-alpine AS build
WORKDIR /workspace

COPY pom.xml .
COPY common-events/pom.xml common-events/pom.xml
COPY payment-service/pom.xml payment-service/pom.xml
COPY compliance-service/pom.xml compliance-service/pom.xml
COPY orchestrator-service/pom.xml orchestrator-service/pom.xml
RUN mvn -q -pl common-events,compliance-service -am dependency:go-offline

COPY common-events/src common-events/src
COPY compliance-service/src compliance-service/src
RUN mvn -q -pl common-events,compliance-service -am package -DskipTests

FROM eclipse-temurin:25-jre-alpine AS runtime
RUN addgroup -S spring && adduser -S spring -G spring
USER spring
WORKDIR /app
COPY --from=build /workspace/compliance-service/target/compliance-service-*.jar app.jar

EXPOSE 8082
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
