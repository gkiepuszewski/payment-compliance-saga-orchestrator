# Build context is the repository root (not this directory), e.g.:
#   docker build -f payment-service/Dockerfile -t payment-service .
# This is required because payment-service depends on the common-events module within the
# same Maven reactor.

FROM maven:3.9-eclipse-temurin-25-alpine AS build
WORKDIR /workspace

# Copy every module's pom.xml first (Maven needs the full reactor layout to resolve
# -pl/-am), so dependency resolution for this layer can be cached independently of source changes.
COPY pom.xml .
COPY common-events/pom.xml common-events/pom.xml
COPY payment-service/pom.xml payment-service/pom.xml
COPY compliance-service/pom.xml compliance-service/pom.xml
COPY orchestrator-service/pom.xml orchestrator-service/pom.xml
RUN mvn -q -pl common-events,payment-service -am dependency:go-offline

COPY common-events/src common-events/src
COPY payment-service/src payment-service/src
RUN mvn -q -pl common-events,payment-service -am package -DskipTests

FROM eclipse-temurin:25-jre-alpine AS runtime
RUN addgroup -S spring && adduser -S spring -G spring
USER spring
WORKDIR /app
COPY --from=build /workspace/payment-service/target/payment-service-*.jar app.jar

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
