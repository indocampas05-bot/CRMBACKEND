FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

RUN mkdir -p /root/.m2 && echo '<settings><mirrors><mirror><id>google-central</id><mirrorOf>central</mirrorOf><url>https://maven-central.storage-download.googleapis.com/maven2/</url></mirror></mirrors></settings>' > /root/.m2/settings.xml

COPY pom.xml .
RUN mvn -B -Dmaven.wagon.http.retryHandler.count=5 dependency:go-offline
COPY src ./src
RUN mvn -B -Dmaven.wagon.http.retryHandler.count=5 clean package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 10000
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=70", "-jar", "app.jar"]