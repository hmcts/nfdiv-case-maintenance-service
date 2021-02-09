ARG APP_INSIGHTS_AGENT_VERSION=3.0.2
FROM hmctspublic.azurecr.io/base/java:openjdk-11-distroless-1.4

ENV APP nfdiv-case-maintenance-service.jar

COPY lib/AI-Agent.xml /opt/app/
COPY build/libs/$APP /opt/app/

CMD ["nfdiv-case-maintenance-service.jar"]
