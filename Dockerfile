FROM eclipse-temurin:25-jdk-jammy AS build

WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw \
    && ./mvnw --batch-mode --no-transfer-progress dependency:go-offline

COPY src/ src/
RUN ./mvnw --batch-mode --no-transfer-progress clean package

FROM eclipse-temurin:25-jre-jammy

WORKDIR /app

COPY --from=build /workspace/target/*.jar application.jar

EXPOSE 8080
USER 10001:10001

ENTRYPOINT ["java", "-jar", "/app/application.jar"]
