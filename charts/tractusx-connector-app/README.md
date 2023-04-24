# tractusx-connector-app

![Version: 0.3.2](https://img.shields.io/badge/Version-0.3.2-informational?style=flat-square) ![Type: application](https://img.shields.io/badge/Type-application-informational?style=flat-square) ![AppVersion: 0.3.2](https://img.shields.io/badge/AppVersion-0.3.2-informational?style=flat-square)

A Helm chart for Tractus-X Eclipse Data Space Connector Application. This includes the runtime, which consists of a control plane
and a data plane, and all third-party services such as PostgreSQL and HashiCorp Vault.

This chart is intended to be used as self-contained deployment, which only requires an external DAPS instance.

**Homepage:** <https://github.com/eclipse-tractusx/tractusx-edc/tree/main/charts/tractusx-connector-app>

## Source Code

* <https://github.com/eclipse-tractusx/tractusx-edc/tree/main/charts/tractusx-connector-app>

## Requirements

| Repository | Name | Version |
|------------|------|---------|
| file://../tractusx-connector | runtime(tractusx-connector) | 0.3.2 |
| https://charts.bitnami.com/bitnami | postgresql(postgresql) | 12.1.6 |
| https://helm.releases.hashicorp.com | vault(vault) | 0.20.0 |

## Values

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| customLabels | object | `{}` |  |
| fullnameOverride | string | `""` |  |
| imagePullSecrets | list | `[]` | Existing image pull secret to use to [obtain the container image from private registries](https://kubernetes.io/docs/concepts/containers/images/#using-a-private-registry) |
| nameOverride | string | `""` |  |
| runtime.backendService.httpProxyTokenReceiverUrl | string | `""` |  |
| runtime.controlplane.affinity | object | `{}` |  |
| runtime.controlplane.autoscaling.enabled | bool | `false` | Enables [horizontal pod autoscaling](https://kubernetes.io/docs/tasks/run-application/horizontal-pod-autoscale/https://kubernetes.io/docs/tasks/run-application/horizontal-pod-autoscale/) |
| runtime.controlplane.autoscaling.maxReplicas | int | `100` | Maximum replicas if resource consumption exceeds resource threshholds |
| runtime.controlplane.autoscaling.minReplicas | int | `1` | Minimal replicas if resource consumption falls below resource threshholds |
| runtime.controlplane.autoscaling.targetCPUUtilizationPercentage | int | `80` | targetAverageUtilization of cpu provided to a pod |
| runtime.controlplane.autoscaling.targetMemoryUtilizationPercentage | int | `80` | targetAverageUtilization of memory provided to a pod |
| runtime.controlplane.debug.enabled | bool | `false` |  |
| runtime.controlplane.debug.port | int | `1044` |  |
| runtime.controlplane.debug.suspendOnStart | bool | `false` |  |
| runtime.controlplane.endpoints | object | `{"control":{"path":"/control","port":8083},"default":{"path":"/api","port":8080},"management":{"authKey":"","path":"/management","port":8081},"metrics":{"path":"/metrics","port":9090},"observability":{"insecure":true,"path":"/observability","port":8085},"protocol":{"path":"/api/v1/ids","port":8084}}` | endpoints of the control plane |
| runtime.controlplane.endpoints.control | object | `{"path":"/control","port":8083}` | control api, used for internal control calls. can be added to the internal ingress, but should probably not |
| runtime.controlplane.endpoints.control.path | string | `"/control"` | path for incoming api calls |
| runtime.controlplane.endpoints.control.port | int | `8083` | port for incoming api calls |
| runtime.controlplane.endpoints.default | object | `{"path":"/api","port":8080}` | default api for health checks, should not be added to any ingress |
| runtime.controlplane.endpoints.default.path | string | `"/api"` | path for incoming api calls |
| runtime.controlplane.endpoints.default.port | int | `8080` | port for incoming api calls |
| runtime.controlplane.endpoints.management | object | `{"authKey":"","path":"/management","port":8081}` | data management api, used by internal users, can be added to an ingress and must not be internet facing |
| runtime.controlplane.endpoints.management.authKey | string | `""` | authentication key, must be attached to each 'X-Api-Key' request header |
| runtime.controlplane.endpoints.management.path | string | `"/management"` | path for incoming api calls |
| runtime.controlplane.endpoints.management.port | int | `8081` | port for incoming api calls |
| runtime.controlplane.endpoints.metrics | object | `{"path":"/metrics","port":9090}` | metrics api, used for application metrics, must not be internet facing |
| runtime.controlplane.endpoints.metrics.path | string | `"/metrics"` | path for incoming api calls |
| runtime.controlplane.endpoints.metrics.port | int | `9090` | port for incoming api calls |
| runtime.controlplane.endpoints.observability | object | `{"insecure":true,"path":"/observability","port":8085}` | observability api with unsecured access, must not be internet facing |
| runtime.controlplane.endpoints.observability.insecure | bool | `true` | allow or disallow insecure access, i.e. access without authentication |
| runtime.controlplane.endpoints.observability.path | string | `"/observability"` | observability api, provides /health /readiness and /liveness endpoints |
| runtime.controlplane.endpoints.observability.port | int | `8085` | port for incoming API calls |
| runtime.controlplane.endpoints.protocol | object | `{"path":"/api/v1/ids","port":8084}` | ids api, used for inter connector communication and must be internet facing |
| runtime.controlplane.endpoints.protocol.path | string | `"/api/v1/ids"` | path for incoming api calls |
| runtime.controlplane.endpoints.protocol.port | int | `8084` | port for incoming api calls |
| runtime.controlplane.env | object | `{}` |  |
| runtime.controlplane.envConfigMapNames | list | `[]` |  |
| runtime.controlplane.envSecretNames | list | `[]` |  |
| runtime.controlplane.envValueFrom | object | `{}` |  |
| runtime.controlplane.image.pullPolicy | string | `"IfNotPresent"` | [Kubernetes image pull policy](https://kubernetes.io/docs/concepts/containers/images/#image-pull-policy) to use |
| runtime.controlplane.image.repository | string | `""` | Which derivate of the control plane to use. when left empty the deployment will select the correct image automatically |
| runtime.controlplane.image.tag | string | `""` | Overrides the image tag whose default is the chart appVersion |
| runtime.controlplane.ingresses[0].annotations | object | `{}` | Additional ingress annotations to add |
| runtime.controlplane.ingresses[0].certManager.clusterIssuer | string | `""` | If preset enables certificate generation via cert-manager cluster-wide issuer |
| runtime.controlplane.ingresses[0].certManager.issuer | string | `""` | If preset enables certificate generation via cert-manager namespace scoped issuer |
| runtime.controlplane.ingresses[0].className | string | `""` | Defines the [ingress class](https://kubernetes.io/docs/concepts/services-networking/ingress/#ingress-class)  to use |
| runtime.controlplane.ingresses[0].enabled | bool | `false` |  |
| runtime.controlplane.ingresses[0].endpoints | list | `["ids"]` | EDC endpoints exposed by this ingress resource |
| runtime.controlplane.ingresses[0].hostname | string | `"edc-control.local"` | The hostname to be used to precisely map incoming traffic onto the underlying network service |
| runtime.controlplane.ingresses[0].tls | object | `{"enabled":false,"secretName":""}` | TLS [tls class](https://kubernetes.io/docs/concepts/services-networking/ingress/#tls) applied to the ingress resource |
| runtime.controlplane.ingresses[0].tls.enabled | bool | `false` | Enables TLS on the ingress resource |
| runtime.controlplane.ingresses[0].tls.secretName | string | `""` | If present overwrites the default secret name |
| runtime.controlplane.ingresses[1].annotations | object | `{}` | Additional ingress annotations to add |
| runtime.controlplane.ingresses[1].certManager.clusterIssuer | string | `""` | If preset enables certificate generation via cert-manager cluster-wide issuer |
| runtime.controlplane.ingresses[1].certManager.issuer | string | `""` | If preset enables certificate generation via cert-manager namespace scoped issuer |
| runtime.controlplane.ingresses[1].className | string | `""` | Defines the [ingress class](https://kubernetes.io/docs/concepts/services-networking/ingress/#ingress-class)  to use |
| runtime.controlplane.ingresses[1].enabled | bool | `false` |  |
| runtime.controlplane.ingresses[1].endpoints | list | `["management","control"]` | EDC endpoints exposed by this ingress resource |
| runtime.controlplane.ingresses[1].hostname | string | `"edc-control.intranet"` | The hostname to be used to precisely map incoming traffic onto the underlying network service |
| runtime.controlplane.ingresses[1].tls | object | `{"enabled":false,"secretName":""}` | TLS [tls class](https://kubernetes.io/docs/concepts/services-networking/ingress/#tls) applied to the ingress resource |
| runtime.controlplane.ingresses[1].tls.enabled | bool | `false` | Enables TLS on the ingress resource |
| runtime.controlplane.ingresses[1].tls.secretName | string | `""` | If present overwrites the default secret name |
| runtime.controlplane.initContainers | list | `[]` |  |
| runtime.controlplane.internationalDataSpaces.catalogId | string | `"TXDC-Catalog"` |  |
| runtime.controlplane.internationalDataSpaces.curator | string | `""` |  |
| runtime.controlplane.internationalDataSpaces.description | string | `"Tractus-X Eclipse IDS Data Space Connector"` |  |
| runtime.controlplane.internationalDataSpaces.id | string | `"TXDC"` |  |
| runtime.controlplane.internationalDataSpaces.maintainer | string | `""` |  |
| runtime.controlplane.internationalDataSpaces.title | string | `""` |  |
| runtime.controlplane.livenessProbe.enabled | bool | `true` | Whether to enable kubernetes [liveness-probe](https://kubernetes.io/docs/tasks/configure-pod-container/configure-liveness-readiness-startup-probes/) |
| runtime.controlplane.livenessProbe.failureThreshold | int | `6` | when a probe fails kubernetes will try 6 times before giving up |
| runtime.controlplane.livenessProbe.initialDelaySeconds | int | `30` | seconds to wait before performing the first liveness check |
| runtime.controlplane.livenessProbe.periodSeconds | int | `10` | this fields specifies that kubernetes should perform a liveness check every 10 seconds |
| runtime.controlplane.livenessProbe.successThreshold | int | `1` | number of consecutive successes for the probe to be considered successful after having failed |
| runtime.controlplane.livenessProbe.timeoutSeconds | int | `5` | number of seconds after which the probe times out |
| runtime.controlplane.logging | string | `".level=INFO\norg.eclipse.edc.level=ALL\nhandlers=java.util.logging.ConsoleHandler\njava.util.logging.ConsoleHandler.formatter=java.util.logging.SimpleFormatter\njava.util.logging.ConsoleHandler.level=ALL\njava.util.logging.SimpleFormatter.format=[%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS] [%4$-7s] %5$s%6$s%n"` | configuration of the [Java Util Logging Facade](https://docs.oracle.com/javase/7/docs/technotes/guides/logging/overview.html) |
| runtime.controlplane.nodeSelector | object | `{}` |  |
| runtime.controlplane.opentelemetry | string | `"otel.javaagent.enabled=false\notel.javaagent.debug=false"` | configuration of the [Open Telemetry Agent](https://opentelemetry.io/docs/instrumentation/java/automatic/agent-config/) to collect and expose metrics |
| runtime.controlplane.podAnnotations | object | `{}` | additional annotations for the pod |
| runtime.controlplane.podLabels | object | `{}` | additional labels for the pod |
| runtime.controlplane.podSecurityContext | object | `{"fsGroup":10001,"runAsGroup":10001,"runAsUser":10001,"seccompProfile":{"type":"RuntimeDefault"}}` | The [pod security context](https://kubernetes.io/docs/tasks/configure-pod-container/security-context/#set-the-security-context-for-a-pod) defines privilege and access control settings for a Pod within the deployment |
| runtime.controlplane.podSecurityContext.fsGroup | int | `10001` | The owner for volumes and any files created within volumes will belong to this guid |
| runtime.controlplane.podSecurityContext.runAsGroup | int | `10001` | Processes within a pod will belong to this guid |
| runtime.controlplane.podSecurityContext.runAsUser | int | `10001` | Runs all processes within a pod with a special uid |
| runtime.controlplane.podSecurityContext.seccompProfile.type | string | `"RuntimeDefault"` | Restrict a Container's Syscalls with seccomp |
| runtime.controlplane.readinessProbe.enabled | bool | `true` | Whether to enable kubernetes [readiness-probes](https://kubernetes.io/docs/tasks/configure-pod-container/configure-liveness-readiness-startup-probes/) |
| runtime.controlplane.readinessProbe.failureThreshold | int | `6` | when a probe fails kubernetes will try 6 times before giving up |
| runtime.controlplane.readinessProbe.initialDelaySeconds | int | `30` | seconds to wait before performing the first readiness check |
| runtime.controlplane.readinessProbe.periodSeconds | int | `10` | this fields specifies that kubernetes should perform a readiness check every 10 seconds |
| runtime.controlplane.readinessProbe.successThreshold | int | `1` | number of consecutive successes for the probe to be considered successful after having failed |
| runtime.controlplane.readinessProbe.timeoutSeconds | int | `5` | number of seconds after which the probe times out |
| runtime.controlplane.replicaCount | int | `1` |  |
| runtime.controlplane.resources | object | `{}` | [resource management](https://kubernetes.io/docs/concepts/configuration/manage-resources-containers/) for the container |
| runtime.controlplane.securityContext.allowPrivilegeEscalation | bool | `false` | Controls [Privilege Escalation](https://kubernetes.io/docs/concepts/security/pod-security-policy/#privilege-escalation) enabling setuid binaries changing the effective user ID |
| runtime.controlplane.securityContext.capabilities.add | list | `[]` | Specifies which capabilities to add to issue specialized syscalls |
| runtime.controlplane.securityContext.capabilities.drop | list | `["ALL"]` | Specifies which capabilities to drop to reduce syscall attack surface |
| runtime.controlplane.securityContext.readOnlyRootFilesystem | bool | `true` | Whether the root filesystem is mounted in read-only mode |
| runtime.controlplane.securityContext.runAsNonRoot | bool | `true` | Requires the container to run without root privileges |
| runtime.controlplane.securityContext.runAsUser | int | `10001` | The container's process will run with the specified uid |
| runtime.controlplane.service.annotations | object | `{}` |  |
| runtime.controlplane.service.type | string | `"ClusterIP"` | [Service type](https://kubernetes.io/docs/concepts/services-networking/service/#publishing-services-service-types) to expose the running application on a set of Pods as a network service. |
| runtime.controlplane.tolerations | list | `[]` |  |
| runtime.controlplane.url.ids | string | `""` | Explicitly declared url for reaching the ids api (e.g. if ingresses not used) |
| runtime.controlplane.volumeMounts | list | `[]` | declare where to mount [volumes](https://kubernetes.io/docs/concepts/storage/volumes/) into the container |
| runtime.controlplane.volumes | list | `[]` | [volume](https://kubernetes.io/docs/concepts/storage/volumes/) directories |
| runtime.daps.clientId | string | `""` |  |
| runtime.daps.paths.jwks | string | `"/jwks.json"` |  |
| runtime.daps.paths.token | string | `"/token"` |  |
| runtime.daps.url | string | `""` |  |
| runtime.dataplane.affinity | object | `{}` |  |
| runtime.dataplane.autoscaling.enabled | bool | `false` | Enables [horizontal pod autoscaling](https://kubernetes.io/docs/tasks/run-application/horizontal-pod-autoscale/https://kubernetes.io/docs/tasks/run-application/horizontal-pod-autoscale/) |
| runtime.dataplane.autoscaling.maxReplicas | int | `100` | Maximum replicas if resource consumption exceeds resource threshholds |
| runtime.dataplane.autoscaling.minReplicas | int | `1` | Minimal replicas if resource consumption falls below resource threshholds |
| runtime.dataplane.autoscaling.targetCPUUtilizationPercentage | int | `80` | targetAverageUtilization of cpu provided to a pod |
| runtime.dataplane.autoscaling.targetMemoryUtilizationPercentage | int | `80` | targetAverageUtilization of memory provided to a pod |
| runtime.dataplane.aws.accessKeyId | string | `""` |  |
| runtime.dataplane.aws.endpointOverride | string | `""` |  |
| runtime.dataplane.aws.secretAccessKey | string | `""` |  |
| runtime.dataplane.debug.enabled | bool | `false` |  |
| runtime.dataplane.debug.port | int | `1044` |  |
| runtime.dataplane.debug.suspendOnStart | bool | `false` |  |
| runtime.dataplane.endpoints.control.path | string | `"/api/dataplane/control"` |  |
| runtime.dataplane.endpoints.control.port | int | `8083` |  |
| runtime.dataplane.endpoints.default.path | string | `"/api"` |  |
| runtime.dataplane.endpoints.default.port | int | `8080` |  |
| runtime.dataplane.endpoints.metrics.path | string | `"/metrics"` |  |
| runtime.dataplane.endpoints.metrics.port | int | `9090` |  |
| runtime.dataplane.endpoints.observability.insecure | bool | `true` | allow or disallow insecure access, i.e. access without authentication |
| runtime.dataplane.endpoints.observability.path | string | `"/observability"` | observability api, provides /health /readiness and /liveness endpoints |
| runtime.dataplane.endpoints.observability.port | int | `8085` | port for incoming API calls |
| runtime.dataplane.endpoints.public.path | string | `"/api/public"` |  |
| runtime.dataplane.endpoints.public.port | int | `8081` |  |
| runtime.dataplane.env | object | `{}` |  |
| runtime.dataplane.envConfigMapNames | list | `[]` |  |
| runtime.dataplane.envSecretNames | list | `[]` |  |
| runtime.dataplane.envValueFrom | object | `{}` |  |
| runtime.dataplane.image.pullPolicy | string | `"IfNotPresent"` | [Kubernetes image pull policy](https://kubernetes.io/docs/concepts/containers/images/#image-pull-policy) to use |
| runtime.dataplane.image.repository | string | `""` | Which derivate of the data plane to use. when left empty the deployment will select the correct image automatically |
| runtime.dataplane.image.tag | string | `""` | Overrides the image tag whose default is the chart appVersion |
| runtime.dataplane.ingresses[0].annotations | object | `{}` | Additional ingress annotations to add |
| runtime.dataplane.ingresses[0].certManager.clusterIssuer | string | `""` | If preset enables certificate generation via cert-manager cluster-wide issuer |
| runtime.dataplane.ingresses[0].certManager.issuer | string | `""` | If preset enables certificate generation via cert-manager namespace scoped issuer |
| runtime.dataplane.ingresses[0].className | string | `""` | Defines the [ingress class](https://kubernetes.io/docs/concepts/services-networking/ingress/#ingress-class)  to use |
| runtime.dataplane.ingresses[0].enabled | bool | `false` |  |
| runtime.dataplane.ingresses[0].endpoints | list | `["public"]` | EDC endpoints exposed by this ingress resource |
| runtime.dataplane.ingresses[0].hostname | string | `"edc-data.local"` | The hostname to be used to precisely map incoming traffic onto the underlying network service |
| runtime.dataplane.ingresses[0].tls | object | `{"enabled":false,"secretName":""}` | TLS [tls class](https://kubernetes.io/docs/concepts/services-networking/ingress/#tls) applied to the ingress resource |
| runtime.dataplane.ingresses[0].tls.enabled | bool | `false` | Enables TLS on the ingress resource |
| runtime.dataplane.ingresses[0].tls.secretName | string | `""` | If present overwrites the default secret name |
| runtime.dataplane.initContainers | list | `[]` |  |
| runtime.dataplane.livenessProbe.enabled | bool | `true` | Whether to enable kubernetes [liveness-probe](https://kubernetes.io/docs/tasks/configure-pod-container/configure-liveness-readiness-startup-probes/) |
| runtime.dataplane.livenessProbe.failureThreshold | int | `6` | when a probe fails kubernetes will try 6 times before giving up |
| runtime.dataplane.livenessProbe.initialDelaySeconds | int | `30` | seconds to wait before performing the first liveness check |
| runtime.dataplane.livenessProbe.periodSeconds | int | `10` | this fields specifies that kubernetes should perform a liveness check every 10 seconds |
| runtime.dataplane.livenessProbe.successThreshold | int | `1` | number of consecutive successes for the probe to be considered successful after having failed |
| runtime.dataplane.livenessProbe.timeoutSeconds | int | `5` | number of seconds after which the probe times out |
| runtime.dataplane.logging | string | `".level=INFO\norg.eclipse.edc.level=ALL\nhandlers=java.util.logging.ConsoleHandler\njava.util.logging.ConsoleHandler.formatter=java.util.logging.SimpleFormatter\njava.util.logging.ConsoleHandler.level=ALL\njava.util.logging.SimpleFormatter.format=[%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS] [%4$-7s] %5$s%6$s%n"` | configuration of the [Java Util Logging Facade](https://docs.oracle.com/javase/7/docs/technotes/guides/logging/overview.html) |
| runtime.dataplane.nodeSelector | object | `{}` |  |
| runtime.dataplane.opentelemetry | string | `"otel.javaagent.enabled=false\notel.javaagent.debug=false"` | configuration of the [Open Telemetry Agent](https://opentelemetry.io/docs/instrumentation/java/automatic/agent-config/) to collect and expose metrics |
| runtime.dataplane.podAnnotations | object | `{}` | additional annotations for the pod |
| runtime.dataplane.podLabels | object | `{}` | additional labels for the pod |
| runtime.dataplane.podSecurityContext | object | `{"fsGroup":10001,"runAsGroup":10001,"runAsUser":10001,"seccompProfile":{"type":"RuntimeDefault"}}` | The [pod security context](https://kubernetes.io/docs/tasks/configure-pod-container/security-context/#set-the-security-context-for-a-pod) defines privilege and access control settings for a Pod within the deployment |
| runtime.dataplane.podSecurityContext.fsGroup | int | `10001` | The owner for volumes and any files created within volumes will belong to this guid |
| runtime.dataplane.podSecurityContext.runAsGroup | int | `10001` | Processes within a pod will belong to this guid |
| runtime.dataplane.podSecurityContext.runAsUser | int | `10001` | Runs all processes within a pod with a special uid |
| runtime.dataplane.podSecurityContext.seccompProfile.type | string | `"RuntimeDefault"` | Restrict a Container's Syscalls with seccomp |
| runtime.dataplane.readinessProbe.enabled | bool | `true` | Whether to enable kubernetes [readiness-probes](https://kubernetes.io/docs/tasks/configure-pod-container/configure-liveness-readiness-startup-probes/) |
| runtime.dataplane.readinessProbe.failureThreshold | int | `6` | when a probe fails kubernetes will try 6 times before giving up |
| runtime.dataplane.readinessProbe.initialDelaySeconds | int | `30` | seconds to wait before performing the first readiness check |
| runtime.dataplane.readinessProbe.periodSeconds | int | `10` | this fields specifies that kubernetes should perform a liveness check every 10 seconds |
| runtime.dataplane.readinessProbe.successThreshold | int | `1` | number of consecutive successes for the probe to be considered successful after having failed |
| runtime.dataplane.readinessProbe.timeoutSeconds | int | `5` | number of seconds after which the probe times out |
| runtime.dataplane.replicaCount | int | `1` |  |
| runtime.dataplane.resources | object | `{}` | [resource management](https://kubernetes.io/docs/concepts/configuration/manage-resources-containers/) for the container |
| runtime.dataplane.securityContext.allowPrivilegeEscalation | bool | `false` | Controls [Privilege Escalation](https://kubernetes.io/docs/concepts/security/pod-security-policy/#privilege-escalation) enabling setuid binaries changing the effective user ID |
| runtime.dataplane.securityContext.capabilities.add | list | `[]` | Specifies which capabilities to add to issue specialized syscalls |
| runtime.dataplane.securityContext.capabilities.drop | list | `["ALL"]` | Specifies which capabilities to drop to reduce syscall attack surface |
| runtime.dataplane.securityContext.readOnlyRootFilesystem | bool | `true` | Whether the root filesystem is mounted in read-only mode |
| runtime.dataplane.securityContext.runAsNonRoot | bool | `true` | Requires the container to run without root privileges |
| runtime.dataplane.securityContext.runAsUser | int | `10001` | The container's process will run with the specified uid |
| runtime.dataplane.service.port | int | `80` |  |
| runtime.dataplane.service.type | string | `"ClusterIP"` | [Service type](https://kubernetes.io/docs/concepts/services-networking/service/#publishing-services-service-types) to expose the running application on a set of Pods as a network service. |
| runtime.dataplane.tolerations | list | `[]` |  |
| runtime.dataplane.url.public | string | `""` | Explicitly declared url for reaching the public api (e.g. if ingresses not used) |
| runtime.dataplane.volumeMounts | list | `[]` | declare where to mount [volumes](https://kubernetes.io/docs/concepts/storage/volumes/) into the container |
| runtime.dataplane.volumes | list | `[]` | [volume](https://kubernetes.io/docs/concepts/storage/volumes/) directories |
| runtime.postgresql.enabled | bool | `false` |  |
| runtime.postgresql.jdbcUrl | string | `""` |  |
| runtime.postgresql.password | string | `""` |  |
| runtime.postgresql.username | string | `""` |  |
| runtime.serviceAccount.annotations | object | `{}` |  |
| runtime.serviceAccount.create | bool | `true` |  |
| runtime.serviceAccount.imagePullSecrets | list | `[]` | Existing image pull secret bound to the service account to use to [obtain the container image from private registries](https://kubernetes.io/docs/concepts/containers/images/#using-a-private-registry) |
| runtime.serviceAccount.name | string | `""` |  |
| runtime.vault.hashicorp.enabled | bool | `true` |  |
| runtime.vault.hashicorp.healthCheck.enabled | bool | `true` |  |
| runtime.vault.hashicorp.healthCheck.standbyOk | bool | `true` |  |
| runtime.vault.hashicorp.paths.health | string | `"/v1/sys/health"` |  |
| runtime.vault.hashicorp.paths.secret | string | `"/v1/secret"` |  |
| runtime.vault.hashicorp.timeout | int | `30` |  |
| runtime.vault.hashicorp.token | string | `""` |  |
| runtime.vault.hashicorp.url | string | `""` |  |
| runtime.vault.secretNames.dapsPrivateKey | string | `"daps-private-key"` |  |
| runtime.vault.secretNames.dapsPublicKey | string | `"daps-public-key"` |  |
| runtime.vault.secretNames.transferProxyTokenEncryptionAesKey | string | `"transfer-proxy-token-encryption-aes-key"` |  |
| runtime.vault.secretNames.transferProxyTokenSignerPrivateKey | string | `"transfer-proxy-token-signer-private-key"` |  |
| runtime.vault.secretNames.transferProxyTokenSignerPublicKey | string | `"transfer-proxy-token-signer-public-key"` |  |

----------------------------------------------
Autogenerated from chart metadata using [helm-docs v1.10.0](https://github.com/norwoodj/helm-docs/releases/v1.10.0)
