# txdc

![Version: 0.1.0](https://img.shields.io/badge/Version-0.1.0-informational?style=flat-square) ![Type: application](https://img.shields.io/badge/Type-application-informational?style=flat-square) ![AppVersion: 1.16.0](https://img.shields.io/badge/AppVersion-1.16.0-informational?style=flat-square)

A Helm chart for Kubernetes

## Requirements

| Repository | Name | Version |
|------------|------|---------|
| https://charts.bitnami.com/bitnami | postgresql | 11.2.4 |

## Values

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| backendService | object | `{"httpProxyTokenReceiverUrl":""}` | "valueFrom" environment variable references that will be added to deployment pods. Name is templated. |
| controlplane.affinity | object | `{}` |  |
| controlplane.autoscaling.enabled | bool | `false` |  |
| controlplane.autoscaling.maxReplicas | int | `100` |  |
| controlplane.autoscaling.minReplicas | int | `1` |  |
| controlplane.autoscaling.targetCPUUtilizationPercentage | int | `80` |  |
| controlplane.debug.enabled | bool | `false` |  |
| controlplane.debug.port | int | `1044` |  |
| controlplane.debug.suspendOnStart | bool | `false` |  |
| controlplane.endpoints.control.path | string | `"/control"` |  |
| controlplane.endpoints.control.port | int | `8083` |  |
| controlplane.endpoints.data.path | string | `"/data"` |  |
| controlplane.endpoints.data.port | int | `8081` |  |
| controlplane.endpoints.default.path | string | `"/api"` |  |
| controlplane.endpoints.default.port | int | `8080` |  |
| controlplane.endpoints.ids.path | string | `"/api/v1/ids"` |  |
| controlplane.endpoints.ids.port | int | `8084` |  |
| controlplane.endpoints.metrics.path | string | `"/metrics"` |  |
| controlplane.endpoints.metrics.port | int | `8085` |  |
| controlplane.endpoints.validation.path | string | `"/validation"` |  |
| controlplane.endpoints.validation.port | int | `8082` |  |
| controlplane.fullnameOverride | string | `""` |  |
| controlplane.image.pullPolicy | string | `"IfNotPresent"` |  |
| controlplane.image.repository | string | `""` |  |
| controlplane.image.tag | string | `""` |  |
| controlplane.ingresses[0].annotations | object | `{}` | Additional ingress annotations to add |
| controlplane.ingresses[0].certManager.clusterIssuer | string | `""` | If preset enables certificate generation via cert-manager cluster-wide issuer |
| controlplane.ingresses[0].certManager.issuer | string | `""` | If preset enables certificate generation via cert-manager namespace scoped issuer |
| controlplane.ingresses[0].className | string | `""` | Defines the [ingress class](https://kubernetes.io/docs/concepts/services-networking/ingress/#ingress-class)  to use |
| controlplane.ingresses[0].enabled | bool | `false` |  |
| controlplane.ingresses[0].endpoints | list | `["ids"]` | EDC endpoints exposed by this ingress resource |
| controlplane.ingresses[0].hostname | string | `"edc-control.local"` | The hostname to be used to precisely map incoming traffic onto the underlying network service |
| controlplane.ingresses[0].tls | object | `{"enabled":false,"secretName":""}` | TLS [tls class](https://kubernetes.io/docs/concepts/services-networking/ingress/#tls) applied to the ingress resource |
| controlplane.ingresses[0].tls.enabled | bool | `false` | Enables TLS on the ingress resource |
| controlplane.ingresses[0].tls.secretName | string | `""` | If present overwrites the default secret name |
| controlplane.ingresses[1].annotations | object | `{}` | Additional ingress annotations to add |
| controlplane.ingresses[1].certManager.clusterIssuer | string | `""` | If preset enables certificate generation via cert-manager cluster-wide issuer |
| controlplane.ingresses[1].certManager.issuer | string | `""` | If preset enables certificate generation via cert-manager namespace scoped issuer |
| controlplane.ingresses[1].className | string | `""` | Defines the [ingress class](https://kubernetes.io/docs/concepts/services-networking/ingress/#ingress-class)  to use |
| controlplane.ingresses[1].enabled | bool | `false` |  |
| controlplane.ingresses[1].endpoints | list | `["data","control"]` | EDC endpoints exposed by this ingress resource |
| controlplane.ingresses[1].hostname | string | `"edc-control.intranet"` | The hostname to be used to precisely map incoming traffic onto the underlying network service |
| controlplane.ingresses[1].tls | object | `{"enabled":false,"secretName":""}` | TLS [tls class](https://kubernetes.io/docs/concepts/services-networking/ingress/#tls) applied to the ingress resource |
| controlplane.ingresses[1].tls.enabled | bool | `false` | Enables TLS on the ingress resource |
| controlplane.ingresses[1].tls.secretName | string | `""` | If present overwrites the default secret name |
| controlplane.initContainers | list | `[]` |  |
| controlplane.internationalDataSpaces.catalogId | string | `""` |  |
| controlplane.internationalDataSpaces.curator | string | `""` |  |
| controlplane.internationalDataSpaces.description | string | `"Tractus-X Eclipse IDS Data Space Connector"` |  |
| controlplane.internationalDataSpaces.id | string | `"TXDC"` |  |
| controlplane.internationalDataSpaces.maintainer | string | `""` |  |
| controlplane.internationalDataSpaces.title | string | `""` |  |
| controlplane.livenessProbe.enabled | bool | `true` | Whether to enable kubernetes [liveness-probe](https://kubernetes.io/docs/tasks/configure-pod-container/configure-liveness-readiness-startup-probes/) |
| controlplane.livenessProbe.failureThreshold | int | `6` |  |
| controlplane.livenessProbe.initialDelaySeconds | int | `30` |  |
| controlplane.livenessProbe.periodSeconds | int | `10` |  |
| controlplane.livenessProbe.successThreshold | int | `1` |  |
| controlplane.livenessProbe.timeoutSeconds | int | `5` |  |
| controlplane.nameOverride | string | `""` |  |
| controlplane.nodeSelector | object | `{}` |  |
| controlplane.podAnnotations | object | `{}` |  |
| controlplane.podLabels | object | `{}` |  |
| controlplane.podSecurityContext | object | `{}` |  |
| controlplane.readinessProbe.enabled | bool | `true` | Whether to enable kubernetes [readiness-probes](https://kubernetes.io/docs/tasks/configure-pod-container/configure-liveness-readiness-startup-probes/) |
| controlplane.readinessProbe.failureThreshold | int | `6` |  |
| controlplane.readinessProbe.initialDelaySeconds | int | `30` |  |
| controlplane.readinessProbe.periodSeconds | int | `10` |  |
| controlplane.readinessProbe.successThreshold | int | `1` |  |
| controlplane.readinessProbe.timeoutSeconds | int | `5` |  |
| controlplane.replicaCount | int | `1` |  |
| controlplane.resources | object | `{}` |  |
| controlplane.securityContext | object | `{}` |  |
| controlplane.service.annotations | object | `{}` |  |
| controlplane.service.type | string | `"ClusterIP"` |  |
| controlplane.tolerations | list | `[]` |  |
| controlplane.volumeMounts | list | `[]` |  |
| controlplane.volumes | list | `[]` |  |
| customLabels | object | `{}` |  |
| daps.clientId | string | `""` |  |
| daps.host | string | `""` |  |
| daps.paths.jwks | string | `"/jwks.json"` |  |
| daps.paths.token | string | `"/token"` |  |
| dataplane.affinity | object | `{}` |  |
| dataplane.autoscaling.enabled | bool | `false` |  |
| dataplane.autoscaling.maxReplicas | int | `100` |  |
| dataplane.autoscaling.minReplicas | int | `1` |  |
| dataplane.autoscaling.targetCPUUtilizationPercentage | int | `80` |  |
| dataplane.debug.enabled | bool | `false` |  |
| dataplane.debug.port | string | `""` |  |
| dataplane.debug.suspendOnStart | bool | `false` |  |
| dataplane.endpoints.control.path | string | `"/api/dataplane/control"` |  |
| dataplane.endpoints.control.port | int | `9999` |  |
| dataplane.endpoints.default.path | string | `"/api"` |  |
| dataplane.endpoints.default.port | int | `8080` |  |
| dataplane.endpoints.metrics.path | string | `"/metrics"` |  |
| dataplane.endpoints.metrics.port | int | `9090` |  |
| dataplane.endpoints.public.path | string | `"/api/public"` |  |
| dataplane.endpoints.public.port | int | `8185` |  |
| dataplane.fullnameOverride | string | `""` |  |
| dataplane.image.pullPolicy | string | `"IfNotPresent"` |  |
| dataplane.image.repository | string | `""` |  |
| dataplane.image.tag | string | `""` |  |
| dataplane.ingresses[0].annotations | object | `{}` | Additional ingress annotations to add |
| dataplane.ingresses[0].certManager.clusterIssuer | string | `""` | If preset enables certificate generation via cert-manager cluster-wide issuer |
| dataplane.ingresses[0].certManager.issuer | string | `""` | If preset enables certificate generation via cert-manager namespace scoped issuer |
| dataplane.ingresses[0].className | string | `""` | Defines the [ingress class](https://kubernetes.io/docs/concepts/services-networking/ingress/#ingress-class)  to use |
| dataplane.ingresses[0].enabled | bool | `false` |  |
| dataplane.ingresses[0].endpoints | list | `["public"]` | EDC endpoints exposed by this ingress resource |
| dataplane.ingresses[0].hostname | string | `"edc-data.local"` | The hostname to be used to precisely map incoming traffic onto the underlying network service |
| dataplane.ingresses[0].tls | object | `{"enabled":false,"secretName":""}` | TLS [tls class](https://kubernetes.io/docs/concepts/services-networking/ingress/#tls) applied to the ingress resource |
| dataplane.ingresses[0].tls.enabled | bool | `false` | Enables TLS on the ingress resource |
| dataplane.ingresses[0].tls.secretName | string | `""` | If present overwrites the default secret name |
| dataplane.initContainers | list | `[]` |  |
| dataplane.nameOverride | string | `""` |  |
| dataplane.nodeSelector | object | `{}` |  |
| dataplane.podAnnotations | object | `{}` |  |
| dataplane.podLabels | object | `{}` |  |
| dataplane.podSecurityContext | object | `{}` |  |
| dataplane.replicaCount | int | `1` |  |
| dataplane.resources | object | `{}` |  |
| dataplane.securityContext | object | `{}` |  |
| dataplane.service.port | int | `80` |  |
| dataplane.service.type | string | `"ClusterIP"` |  |
| dataplane.tolerations | list | `[]` |  |
| dataplane.volumeMounts | list | `[]` |  |
| dataplane.volumes | list | `[]` |  |
| imagePullSecrets | list | `[]` |  |
| postgresql.auth.password | string | `""` |  |
| postgresql.auth.username | string | `""` |  |
| postgresql.database | string | `"edc"` |  |
| postgresql.enabled | bool | `false` |  |
| postgresql.host | string | `""` |  |
| postgresql.install | bool | `false` |  |
| postgresql.port | int | `5432` |  |
| serviceAccount.annotations | object | `{}` |  |
| serviceAccount.create | bool | `true` |  |
| serviceAccount.name | string | `""` |  |
| vault.azure.certificate | string | `""` |  |
| vault.azure.client | string | `""` |  |
| vault.azure.enabled | bool | `false` |  |
| vault.azure.name | string | `""` |  |
| vault.azure.secret | string | `""` |  |
| vault.azure.tenant | string | `""` |  |
| vault.hashicorp.enabled | bool | `true` |  |
| vault.hashicorp.healthCheck.enabled | bool | `true` |  |
| vault.hashicorp.healthCheck.standbyOk | bool | `true` |  |
| vault.hashicorp.host | string | `""` |  |
| vault.hashicorp.paths.health | string | `"/v1/sys/health"` |  |
| vault.hashicorp.paths.secret | string | `"/v1/secret"` |  |
| vault.hashicorp.timeout | int | `30` |  |
| vault.hashicorp.token | string | `""` |  |
| vault.secretNames.dapsPrivateKey | string | `"daps-private-key"` |  |
| vault.secretNames.dapsPublicKey | string | `"daps-public-key"` |  |
| vault.secretNames.transferProxyTokenEncryptionAesKey | string | `"transfer-proxy-token-encryption-aes-key"` |  |
| vault.secretNames.transferProxyTokenSignerPrivateKey | string | `"transfer-proxy-token-signer-private-key"` |  |
| vault.secretNames.transferProxyTokenSignerPublicKey | string | `"transfer-proxy-token-signer-public-key"` |  |

----------------------------------------------
Autogenerated from chart metadata using [helm-docs v1.10.0](https://github.com/norwoodj/helm-docs/releases/v1.10.0)
