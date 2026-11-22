# syntax=docker/dockerfile:1
# ---------- 1. Build the React single-page app ----------
FROM node:22-alpine AS frontend
WORKDIR /app
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY frontend/ ./
RUN npm run build

# ---------- 2. Build the Spring Boot jar with the SPA bundled as static resources ----------
FROM eclipse-temurin:21-jdk AS backend
WORKDIR /src
COPY backend/.mvn .mvn
COPY backend/mvnw backend/pom.xml ./
RUN chmod +x mvnw && ./mvnw -q -B dependency:go-offline
COPY backend/src src
COPY --from=frontend /app/dist src/main/resources/static
RUN ./mvnw -q -B package -DskipTests && cp target/itsm-*.jar /app.jar

# ---------- 3. Minimal runtime image ----------
FROM eclipse-temurin:21-jre
RUN useradd --system --uid 1001 itsm
WORKDIR /app
COPY --from=backend /app.jar app.jar
USER itsm
EXPOSE 8080
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC" \
    SPRING_PROFILES_ACTIVE=prod
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s CMD wget -qO- http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
