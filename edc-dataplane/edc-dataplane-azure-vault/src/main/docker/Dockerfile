#
#  Copyright (c) 2023 ZF Friedrichshafen AG
#  Copyright (c) 2022,2023 Mercedes-Benz Tech Innovation GmbH
#  Copyright (c) 2021,2023 Contributors to the Eclipse Foundation
#
#  See the NOTICE file(s) distributed with this work for additional
#  information regarding copyright ownership.
#
#  This program and the accompanying materials are made available under the
#  terms of the Apache License, Version 2.0 which is available at
#  https://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
#  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
#  License for the specific language governing permissions and limitations
#  under the License.
#
#  SPDX-License-Identifier: Apache-2.0
#
FROM alpine:3.17.2 as otel

ENV OTEL_AGENT_LOCATION "https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v1.12.1/opentelemetry-javaagent.jar"

HEALTHCHECK NONE

RUN wget ${OTEL_AGENT_LOCATION} -O /tmp/opentelemetry-javaagent.jar

FROM alpine:3.17.2

ARG JAR
ARG LIB

ARG APP_USER=docker
ARG APP_UID=10100

RUN apk update && \
     apk add openjdk11-jre-headless=11.0.18_p10-r0 --no-cache && \
     rm -rf /var/cache/apk/*

RUN addgroup --system "$APP_USER"

RUN adduser \
     --shell /sbin/nologin \
     --disabled-password \
     --gecos "" \
     --ingroup "$APP_USER" \
     --no-create-home \
     --uid "$APP_UID" \
     "$APP_USER"

USER "$APP_USER"
WORKDIR /app

COPY --from=otel /tmp/opentelemetry-javaagent.jar .
COPY ${JAR} edc-dataplane.jar
COPY ${LIB} ./lib/

HEALTHCHECK NONE

CMD ["java", \
     "-javaagent:/app/opentelemetry-javaagent.jar", \
     "-Dedc.fs.config=/app/configuration.properties", \
     "-Djava.util.logging.config.file=/app/logging.properties", \
     "-Dotel.javaagent.configuration-file=/app/opentelemetry.properties", \
     "-Dotel.metrics.exporter=prometheus", \
     "-Dotel.exporter.prometheus.port=9090", \
     "-Djava.security.egd=file:/dev/urandom", \
     "-jar", \
     "edc-dataplane.jar"]
