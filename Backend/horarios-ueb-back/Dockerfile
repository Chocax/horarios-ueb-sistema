# =====================================================================================================
# Backend Horarios UEB (Spring Boot 4 · Java 17) — imagen para Render
# Se construye desde la RAÍZ del repositorio (contexto «.») porque necesita los scripts de BD/migraciones:
#   docker build -f Backend/horarios-ueb-back/Dockerfile -t horarios-ueb-back .
# =====================================================================================================

# ---------- etapa 1: compilar el jar
FROM eclipse-temurin:17-jdk AS build
WORKDIR /src
COPY BD/migraciones BD/migraciones
COPY Backend/horarios-ueb-back Backend/horarios-ueb-back
WORKDIR /src/Backend/horarios-ueb-back
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew && ./gradlew bootJar --no-daemon -x test

# ---------- etapa 2: imagen liviana solo con el JRE
FROM eclipse-temurin:17-jre
RUN useradd --system --uid 1001 --no-create-home app
WORKDIR /app
COPY --from=build /src/Backend/horarios-ueb-back/build/libs/horarios-ueb-back.jar app.jar
USER app
ENV SPRING_PROFILES_ACTIVE=prod
# Ajustes para el plan gratuito de Render (512 MB de RAM, 0,1 CPU): memoria acotada y arranque más rápido
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70 -XX:+UseSerialGC -Xss512k -XX:TieredStopAtLevel=1 -Djava.security.egd=file:/dev/./urandom"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
