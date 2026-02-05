FROM openjdk:17-jdk-slim
WORKDIR /app
COPY . .
RUN ./gradlew jvmJar --no-daemon -x jvmTest
CMD ["java", "-jar", "build/libs/SolaceCore-jvm.jar"]
