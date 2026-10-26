FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B -ntp dependency:go-offline
COPY src ./src
RUN mvn -B -ntp -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN groupadd --system screi && useradd --system --gid screi screi && mkdir /app/.data && chown screi:screi /app/.data
COPY --from=build /build/target/screi-1.0.0.jar /app/screi.jar
USER screi
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/screi.jar"]
