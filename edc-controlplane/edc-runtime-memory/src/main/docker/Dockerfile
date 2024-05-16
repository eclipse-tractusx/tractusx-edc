#################################################################################
#  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
#  Copyright (c) 2023 ZF Friedrichshafen AG
#  Copyright (c) 2022,2023 Mercedes-Benz Tech Innovation GmbH
#  Copyright (c) 2021,2023 Contributors to the Eclipse Foundation
#
#  See the NOTICE file(s) distributed with this work for additional
#  information regarding copyright ownership.
#
#  This program and the accompanying materials are made available under the
#  terms of the Apache License, Version 2.0 which is available at
#  https://www.apache.org/licenses/LICENSE-2.0.
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
#  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
#  License for the specific language governing permissions and limitations
#  under the License.
#
#  SPDX-License-Identifier: Apache-2.0
#################################################################################


FROM eclipse-temurin:22.0.1_8-jre-alpine
ARG JAR

ARG APP_USER=docker
ARG APP_UID=10100
ARG ADDITIONAL_FILES

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

COPY ${JAR} edc-controlplane.jar
COPY ${ADDITIONAL_FILES} ./

HEALTHCHECK NONE

# use the "exec" syntax so that SIGINT reaches the JVM -> graceful termination
CMD ["sh", "-c", "exec java -Dedc.fs.config=/app/configuration.properties -Djava.util.logging.config.file=/app/logging.properties -Djava.security.egd=file:/dev/urandom -jar edc-controlplane.jar"]
