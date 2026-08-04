# ============================================================================
# TICKET-ADV146 — Spring Boot Dockerfile (multi-stage Maven -> JRE 25 slim)
#
# Stage 1: build the JAR with Temurin JDK 25 + Maven.
# Stage 2: copy ONLY the JAR onto a slim JRE image.
#
# Build context is the REPO ROOT (see docker-compose.yml's
# `build.context: .` / `build.dockerfile: backend/Dockerfile`), not
# backend/, because the backend depends on the sibling
# com.dbtraining.reconx:recon-audit-starter:1.0.0 module, which isn't
# published to Maven Central — it has to be `mvn install`ed into this
# image's local repo before `backend`'s own package step can resolve it.
# ============================================================================

FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace

COPY recon-audit-starter recon-audit-starter
COPY backend/pom.xml backend/mvnw backend/checkstyle.xml backend/checkstyle-suppressions.xml ./backend/
COPY backend/.mvn backend/.mvn
RUN ./backend/mvnw -f recon-audit-starter/pom.xml -q -B -DskipTests install
RUN ./backend/mvnw -f backend/pom.xml -q -B dependency:go-offline || true

COPY backend/src backend/src
RUN ./backend/mvnw -f backend/pom.xml -q -B -DskipTests package && \
    mv backend/target/reconx-*.jar app.jar

FROM eclipse-temurin:25-jre-alpine
RUN addgroup -S reconx && adduser -S reconx -G reconx
USER reconx
WORKDIR /app
COPY --from=build /workspace/app.jar app.jar
EXPOSE 8080
ENV JAVA_OPTS=""
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]
