FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# copy pom.xml first so Docker can cache the dependencies
# if pom.xml doesn't change, Docker skips re-downloading everything
COPY pom.xml .
RUN mvn dependency:go-offline

COPY src ./src
RUN mvn clean package -DskipTests

# create a lightweight container to run the app
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# grab .jar from prev step
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]

