FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Run as non-root user for security hardening
RUN addgroup --system logistics && adduser --system --ingroup logistics logistics

ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar

RUN chown logistics:logistics app.jar
USER logistics

EXPOSE 8080

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
