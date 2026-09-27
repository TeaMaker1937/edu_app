FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Непривилегированный пользователь
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Копируем уже собранный jar
COPY target/*.jar app.jar

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]