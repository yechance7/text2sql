# Stage 1: Cache Maven dependencies
FROM maven:3.9-amazoncorretto-21 AS cache
WORKDIR /app
# you should add pom.yaml if you add new maven subproject
COPY pom.yaml ./
COPY ingest-core/pom.yaml ingest-core/pom.yaml
COPY ingest-cli/pom.yaml ingest-cli/pom.yaml
COPY infer-core/pom.yaml infer-core/pom.yaml
COPY infer-server/pom.yaml infer-server/pom.yaml
COPY exposed-pgvector/pom.yaml exposed-pgvector/pom.yaml
COPY .mvn/extensions.xml  .mvn/extensions.xml
RUN mvn dependency:go-offline

# Stage 2: Build Application
FROM maven:3.9-amazoncorretto-21 AS build
WORKDIR /app
COPY --from=cache /root/.m2 /root/.m2
COPY . .
RUN mvn clean package -pl infer-server -am

# Stage 3: Create the Runtime Image
FROM amazoncorretto:21 AS runtime
ARG config_path=./infer_config.yaml
# never override INFER_CONFIG in cli
ENV INFER_CONFIG=/app/infer_config.yaml
EXPOSE 8080
WORKDIR /app
COPY --from=build /app/infer-server*.jar app.jar
COPY $config_path /app/infer_config.yaml
ENTRYPOINT ["java", "-jar", "app.jar"]
