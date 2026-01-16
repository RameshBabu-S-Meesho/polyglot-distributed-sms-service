# ---------- Build stage ----------
FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /app

# Copy only what is needed first (better caching)
COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn

RUN ./mvnw -B dependency:go-offline

# Copy source and build
COPY src ./src
RUN ./mvnw clean package -DskipTests

# ---------- Runtime stage ----------
FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=build /app/target/*jar app.jar

EXPOSE 8080

ENTRYPOINT ["java","-jar","app.jar"]
    