FROM gradle:8.10.2-jdk17 AS build

ARG SERVICE
WORKDIR /workspace

COPY gradle gradle
COPY gradlew build.gradle settings.gradle ./
COPY admin-service admin-service
COPY facility-service facility-service
COPY inspection-service inspection-service
COPY login-service login-service

RUN chmod +x gradlew \
    && ./gradlew :${SERVICE}:bootJar --no-daemon \
    && cp "$(find "${SERVICE}/build/libs" -maxdepth 1 -name '*.jar' ! -name '*-plain.jar' | head -n 1)" /workspace/app.jar

FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /workspace/app.jar app.jar

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "exec java $JAVA_TOOL_OPTIONS -jar /app/app.jar"]
