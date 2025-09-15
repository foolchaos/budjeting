# === Build stage ===
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
# Pre-download dependencies
RUN mvn -B -q -e -DskipTests dependency:go-offline
COPY src ./src
COPY frontend ./frontend
# Build with Vaadin production frontend compiled
RUN mvn -B -DskipTests -Pproduction clean package

# === Runtime stage ===
FROM eclipse-temurin:21-jre
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0" \
    SPRING_PROFILES_ACTIVE=prod \
    VAADIN_DISABLE_DEV_SERVER=true
WORKDIR /opt/app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]
