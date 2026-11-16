# Platforms

Tractus-X EDC runs wherever Kubernetes or a container runtime runs: the `tractusx-connector` Helm chart and the
images on Docker Hub make no assumption about a cloud. This page shows what to use from each platform, and points to
the projects that already deploy the connector there.

Every production setup needs the same four things, whatever the platform:

| Need | Chart setting | Bring your own |
|---|---|---|
| PostgreSQL | `install.postgresql: false`, `postgresql.jdbcUrl`, `postgresql.auth` | A managed PostgreSQL |
| A HashiCorp Vault compatible secret store | `install.vault: false`, `vault.hashicorp.url`, `vault.hashicorp.token` | HashiCorp Vault or OpenBao |
| Ingresses with TLS for the DSP and public data plane endpoints | `controlplane.ingresses`, `dataplane.ingresses` | An ingress controller and certificates |
| A DCP wallet | `dcp.*` | [Tractus-X Identity Hub, SAP DIV or another wallet](../architecture/identity-and-trust.md#wallets-you-can-use) |

The bundled PostgreSQL and Vault (in dev mode, root token `root`) are for tests only. Work through the
[production checklist](production-checklist.md) and [tune for production](../operations/tuning.md) on any platform.

## Amazon Web Services

| Building block | AWS service |
|---|---|
| Kubernetes | Amazon EKS |
| PostgreSQL | Amazon RDS for PostgreSQL or Aurora PostgreSQL |
| Secrets | HashiCorp Vault on EKS or HCP Vault (the standard images read secrets from a HashiCorp Vault API) |
| Ingress | AWS Load Balancer Controller, certificates from ACM |
| Data | Amazon S3 with the built-in [`AmazonS3-PUSH`](../integrations/aws-s3.md) transfer |
| Access to S3 without keys | IAM roles for service accounts (IRSA) or EKS Pod Identity on the data plane's service account |

Projects to start from:

<div class="grid cards" markdown>

-   :fontawesome-brands-aws:{ .lg .middle } __Dataspace Connector on AWS__

    ---

    The dataspace connector of AWS Labs, built on the Tractus-X EDC control plane and data plane runtimes, with its
    own AWS integrations.

    [:octicons-arrow-right-24: awslabs/dataspace-connector-on-aws](https://github.com/awslabs/dataspace-connector-on-aws)

-   :material-terraform:{ .lg .middle } __AWS EDC pattern__

    ---

    Terraform, Kubernetes and Helm patterns by Think-iT that install the `tractusx-connector` chart on EKS.

    [:octicons-arrow-right-24: Think-iT-Labs/aws-patterns-edc](https://github.com/Think-iT-Labs/aws-patterns-edc)

</div>

```yaml title="values-aws.yaml (excerpt)"
install:
  postgresql: false
  vault: false
postgresql:
  # Replace ${RDS_ENDPOINT} with the endpoint of your RDS instance.
  jdbcUrl: "jdbc:postgresql://${RDS_ENDPOINT}:5432/edc"
vault:
  hashicorp:
    url: "https://vault.example.com"
serviceAccount:
  create: true
  annotations:
    eks.amazonaws.com/role-arn: "arn:aws:iam::123456789012:role/edc-dataplane-s3"   # IRSA
```

## Microsoft Azure

| Building block | Azure service |
|---|---|
| Kubernetes | Azure Kubernetes Service (AKS) |
| PostgreSQL | Azure Database for PostgreSQL flexible server |
| Secrets | HashiCorp Vault on AKS or HCP Vault |
| Ingress | Application Gateway for Containers, or NGINX ingress with cert-manager |
| Data | Azure Blob Storage with the built-in [`AzureStorage-PUSH`](../integrations/azure-blob.md) transfer |

!!! info "No Azure Key Vault distribution"

    Until 0.8, Tractus-X EDC also shipped images and a chart with Azure Key Vault. They were deprecated in 0.8.0 and
    are no longer published since **0.9.0**, so that the project maintains one distribution built on open-source
    services ([ADR](../development/decision-records/2024-10-07-azure-distribution/README.md)). If you need Key Vault,
    build your own runtime with the upstream vault extension; see [Writing extensions](../developer/extensions.md).

## Other Kubernetes platforms

The chart is plain Kubernetes, so Google Kubernetes Engine, Red Hat OpenShift, SUSE Rancher, STACKIT, IONOS,
Open Telekom Cloud and on-premises clusters work the same way: a managed or self-run PostgreSQL, a HashiCorp Vault
compatible store and an ingress controller. On OpenShift, check that the pod security settings of the chart
(`*.securityContext`) fit your security context constraints.

## Local and test setups

| Setup | For | Guide |
|---|---|---|
| kind | One connector on your machine, from source | [Local quick start](local-quickstart.md) |
| Tractus-X Umbrella | A full dataspace: two connectors, Identity Hubs, BDRS and more | [Deploy in five minutes](index.md#deploy-in-five-minutes) |
| Docker Compose | Two connectors without Kubernetes | [Home: install](../index.md), [Tractus-X Starter Kit](https://github.com/eclipse-tractusx/tractusx-starter-kit) |
| EDC Management Console | Provisioning connectors from a web UI | [ARENA2036/edc-management-console](https://github.com/ARENA2036/edc-management-console) |

!!! todo "Platform guides to come"

    Step-by-step guides per platform are planned. If you run Tractus-X EDC on a platform and want to share how, open
    a pull request or present it in the
    [Connector Open Meeting](https://eclipse-tractusx.github.io/community/open-meetings#edc-weekly), and add your
    project to [Known friends](../home/known-friends.md).


## NOTICE

This work is licensed under the [CC-BY-4.0](https://creativecommons.org/licenses/by/4.0/legalcode).

- AI Disclosure: This file was entirely AI-generated. The AI-generated portions are made available under CC0-1.0 and
  not subject to the project's license. The human contributor has reviewed and verified that the content is correct.
- SPDX-License-Identifier: CC-BY-4.0 AND CC0-1.0
- SPDX-FileCopyrightText: 2026 Catena-X Automotive Network e.V.
- SPDX-FileCopyrightText: 2026 Contributors to the Eclipse Foundation
- Assisted-By: Claude Code (claude-opus-5-5)
- Source URL: <https://github.com/eclipse-tractusx/tractusx-edc>
