# Java 17 baseline

## Decision

We will use Java 17 as baseline version.

## Rationale

Java 11 active support [will end in September 2023](https://endoflife.date/java), and, following Java "new" release cycle, we should update the baseline
version to the current LTS from time to time.
[EDC](https://github.com/eclipse-edc/Connector/blob/main/docs/developer/decision-records/2023-05-23-java-17-baseline/README.md) upstream is switching to Java 17.

## Approach

Remove the custom `javaVersion` and let the `edc-build` plugin set that when upgrading to the newest version of EDC.

## NOTICE

This work is licensed under the [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0).

- SPDX-License-Identifier: Apache-2.0
- SPDX-FileCopyrightText: 2021,2022,2023 Contributors to the Eclipse Foundation
- Source URL: <https://github.com/eclipse-tractusx/tractusx-edc>
