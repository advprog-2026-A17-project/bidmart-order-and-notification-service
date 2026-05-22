FROM gradle:8.14.4-jdk21-alpine AS builder

WORKDIR /workspace
ENV GRADLE_OPTS="-Dorg.gradle.jvmargs=-Xmx256m -Dorg.gradle.workers.max=1 -Dkotlin.compiler.execution.strategy=in-process"

COPY settings.gradle.kts settings.gradle.kts
COPY build.gradle.kts build.gradle.kts
COPY src src

RUN gradle clean bootJar --no-daemon --max-workers=1

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=builder /workspace/build/libs/bidmart-order-and-notification-service-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8084

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
