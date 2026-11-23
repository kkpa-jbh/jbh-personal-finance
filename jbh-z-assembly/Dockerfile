# Runtime image for jbh-personal-finance.
# CI builds the Quarkus fast-jar first (mvn -Pgithub -pl jbh-z-assembly -am package),
# so this image only copies target/quarkus-app. No Maven and no registry token inside Docker.
FROM eclipse-temurin:21-jre-alpine

# A fixed numeric user and group (10001). Kubernetes "runAsNonRoot" can only check a number:
# with a user name (USER jbh) the pod fails with "image has non-numeric user".
RUN addgroup -S -g 10001 jbh && adduser -S -u 10001 -G jbh jbh
WORKDIR /app

# Keep the fast-jar layout: quarkus-run.jar needs lib/, app/ and quarkus/ next to it.
COPY --chown=10001:10001 target/quarkus-app/ /app/

USER 10001:10001
EXPOSE 7777

# JAVA_OPTS carries the heap cap (-Xmx) from compose. exec keeps java as PID 1 for clean shutdown.
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/quarkus-run.jar"]
