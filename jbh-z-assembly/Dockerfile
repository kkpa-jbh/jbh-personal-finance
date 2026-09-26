# Runtime image for jbh-personal-finance.
# CI builds the Quarkus fast-jar first (mvn -Pgithub -pl jbh-z-assembly -am package),
# so this image only copies target/quarkus-app. No Maven and no registry token inside Docker.
FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S jbh && adduser -S jbh -G jbh
WORKDIR /app

# Keep the fast-jar layout: quarkus-run.jar needs lib/, app/ and quarkus/ next to it.
COPY --chown=jbh:jbh target/quarkus-app/ /app/

USER jbh
EXPOSE 7777

# JAVA_OPTS carries the heap cap (-Xmx) from compose. exec keeps java as PID 1 for clean shutdown.
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/quarkus-run.jar"]
