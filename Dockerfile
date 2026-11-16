# ---------- Etapa 1: compilación ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
# Primero solo el pom, para que Docker cachee las dependencias entre compilaciones
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B -DskipTests package && cp target/*.jar app.jar

# ---------- Etapa 2: ejecución (solo el JRE y el .jar) ----------
FROM eclipse-temurin:17-jre
WORKDIR /app
RUN useradd --system --uid 1001 biblioteca
COPY --from=build /app/app.jar app.jar
USER biblioteca
EXPOSE 8080
# Estado de la aplicación en /actuator/health (lo comprueba el CI tras arrancar)
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
