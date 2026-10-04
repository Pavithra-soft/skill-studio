FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --create-home --shell /usr/sbin/nologin app \
    && chown -R app:app /app
USER app
COPY --from=build /app/target/skill-studio-1.0.0.jar app.jar
ENV JAVA_TOOL_OPTIONS="-Xmx400m -XX:+UseSerialGC"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
