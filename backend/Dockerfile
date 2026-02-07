FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy pom first for better layer caching
COPY pom.xml .

# Download dependencies only (faster if pom hasn't changed)
RUN mvn dependency:go-offline -B -q

# Copy source code
COPY src ./src

# Build with parallel threads for faster build
RUN mvn clean package -DskipTests -B -T 1C

FROM eclipse-temurin:17-jre
WORKDIR /app

# install curl for healthchecks
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]

