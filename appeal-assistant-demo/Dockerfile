# syntax=docker/dockerfile:1
# Denied-claim appeal assistant demo: built and run entirely in Docker (no local Java or Maven needed).

# ---- build stage: Maven + JDK 21 compile the WAR ----
FROM maven:3.10.0-eclipse-temurin-21 AS build
WORKDIR /src
ENV MAVEN_OPTS="-Xmx512m"
# pom first, so dependency downloads are cached between builds
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -B -q dependency:go-offline
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -q package

# ---- runtime stage: Tomcat 11 on JDK 21 with only the WAR ----
FROM tomcat:11.0.27-jdk21-temurin
ENV TZ=Asia/Kolkata \
    CATALINA_OPTS="-Xms128m -Xmx384m -Duser.timezone=Asia/Kolkata"
RUN rm -rf /usr/local/tomcat/webapps/*
COPY --from=build /src/target/ROOT.war /usr/local/tomcat/webapps/ROOT.war
EXPOSE 8080
