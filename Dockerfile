FROM eclipse-temurin:17-jdk AS build

WORKDIR /app

# Install Maven
RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre

RUN groupadd -r spring && useradd -r -g spring spring

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

RUN mkdir -p /app/logs && chown -R spring:spring /app

USER spring

EXPOSE 8080

ENV JAVA_OPTS="-Xmx512m -Xms256m"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
