# Stage 1: Build
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /build

# Copy Maven wrapper / settings first for better layer caching
COPY pom.xml .
COPY common/pom.xml common/
COPY data-processing-module/pom.xml data-processing-module/
COPY inbound-connector-module/pom.xml inbound-connector-module/
COPY outbound-connector-module/pom.xml outbound-connector-module/
COPY app/pom.xml app/

# Download dependencies (cached unless POMs change)
RUN apk add --no-cache maven && \
    mvn dependency:go-offline -q

# Copy sources
COPY common/src common/src
COPY data-processing-module/src data-processing-module/src
COPY inbound-connector-module/src inbound-connector-module/src
COPY outbound-connector-module/src outbound-connector-module/src
COPY app/src app/src

# Build
RUN mvn clean package -DskipTests -q

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN mkdir -p /app/data

COPY --from=builder /build/app/target/app-1.0.0-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
