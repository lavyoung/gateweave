FROM public.ecr.aws/docker/library/eclipse-temurin:17-jre-jammy AS builder

WORKDIR /builder

ARG JAR_FILE=gateweave-engine/target/gateweave-engine-*-exec.jar

COPY ${JAR_FILE} application.jar

RUN java -Djarmode=layertools -jar application.jar extract

FROM public.ecr.aws/docker/library/eclipse-temurin:17-jre-jammy

LABEL org.opencontainers.image.title="GateWeave Engine"
LABEL org.opencontainers.image.description="GateWeave data plane runtime"

RUN groupadd --system --gid 10001 gateweave \
    && useradd --system --uid 10001 --gid gateweave --home-dir /application \
        --shell /usr/sbin/nologin --no-create-home gateweave

WORKDIR /application

RUN install --directory --owner=10001 --group=10001 /application/.dubbo

COPY --from=builder --chown=10001:10001 /builder/dependencies/ ./
COPY --from=builder --chown=10001:10001 /builder/spring-boot-loader/ ./
COPY --from=builder --chown=10001:10001 /builder/snapshot-dependencies/ ./
COPY --from=builder --chown=10001:10001 /builder/application/ ./

USER 10001:10001

EXPOSE 7397

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
