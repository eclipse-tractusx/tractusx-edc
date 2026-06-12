---
status: "proposed"
date: 2025-07-17
decision-makers: TBD
consulted: TBD
informed: TBD
---

# Kafka on Kubernetes: Strimzi vs Bitnami Helm Chart

## Context and Problem Statement

For the Tractus-X EDC Kafka Extension, we need to deploy Apache Kafka on Kubernetes as part of our streaming data plane architecture.
There are multiple options available for deploying Kafka on Kubernetes, with Strimzi Operator and Bitnami Helm Chart being the most prominent solutions.
We need to decide which approach to recommend, considering that we want to keep the setup simple and do not need a full production environment.

## Decision Drivers

* Operational complexity and maintenance overhead
* Security features and compliance with Tractus-X requirements
* Flexibility for different deployment scenarios (development, testing, production)
* Integration with Kubernetes-native features

## Considered Options

* Strimzi Kafka Operator via Helm Chart
* Bitnami Kafka Helm Chart

## Decision Outcome

Chosen option: "Bitnami Kafka Helm Chart", because it provides a simpler deployment approach that meets the project's requirements while minimizing operational complexity and learning curve.

### Consequences

* ✅ Good, because it simplifies the overall setup with a single solution for all environments
* ✅ Good, because it reduces the learning curve and operational overhead
* ✅ Good, because it provides sufficient security features (TLS, SASL, ACLs) to meet the project's requirements
* ✅ Good, because it integrates with Prometheus/Grafana for observability needs
* ⚪ Neutral, because manual scaling and updates are acceptable for the current project scope
* ⚠️ Bad, because it lacks some advanced features that might be needed for large-scale production deployments in the future

## Pros and Cons of the Options

### Strimzi Kafka Operator via Helm Chart

* ✅ Good, because it provides a Kubernetes operator for automated lifecycle management
* ✅ Good, because it offers comprehensive security features (TLS, mTLS, OAuth2, SCRAM, ACLs)
* ✅ Good, because it supports dynamic broker scaling via CRDs
* ✅ Good, because it has built-in observability with Prometheus/Grafana, Cruise Control, and Kafka Exporter
* ✅ Good, because it includes built-in CRDs for Kafka Connect and MirrorMaker
* ✅ Good, because it supports automated rolling updates
* ✅ Good, because it has deep Kubernetes-native integration through CRDs and RBAC
* ❌ Bad, because it has higher complexity with a steeper learning curve for CRDs and Operator concepts

### Bitnami Kafka Helm Chart

* ✅ Good, because it provides a simpler deployment approach with standard Helm practices
* ✅ Good, because it has lower complexity, making it easier to understand and deploy
* ✅ Good, because it supports basic security features (TLS, basic authentication, SASL, ACLs)
* ✅ Good, because it integrates with Prometheus/Grafana for observability
* ⚠️ Bad, because it requires manual configuration updates for Kafka lifecycle management
* ⚠️ Bad, because scaling requires manual Helm upgrades or replica changes
* ⚠️ Bad, because it has less automation for operations like rolling updates

## More Information

The Strimzi Operator is available at: https://github.com/strimzi/strimzi-kafka-operator/tree/main/helm-charts/helm3/strimzi-kafka-operator

The Bitnami Kafka Helm Chart is available at: https://github.com/bitnami/charts/tree/main/bitnami/kafka

## NOTICE

This work is licensed under the [CC-BY-4.0](https://creativecommons.org/licenses/by/4.0/legalcode).

- SPDX-License-Identifier: CC-BY-4.0
- SPDX-FileCopyrightText: 2025 Contributors to the Eclipse Foundation
- Source URL: <https://github.com/eclipse-tractusx/tractusx-edc-kafka-extension>
