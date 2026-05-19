FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw

# Tentar dependency:go-offline com retry
RUN ./mvnw -B -q dependency:go-offline || ./mvnw -B -q dependency:go-offline || ./mvnw -B -q dependency:go-offline

COPY src ./src
RUN ./mvnw -B -q clean package -DskipTests \
    && cp target/*.jar app.jar

FROM eclipse-temurin:21-jre

WORKDIR /app

RUN useradd -m -u 1001 appuser && chown -R appuser:appuser /app

COPY --from=build --chown=appuser:appuser /app/app.jar /app/app.jar

USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
