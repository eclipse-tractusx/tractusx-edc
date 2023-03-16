# tractusx-connector

![Version: 0.3.0](https://img.shields.io/badge/Version-0.3.0-informational?style=flat-square) ![Type: application](https://img.shields.io/badge/Type-application-informational?style=flat-square) ![AppVersion: 0.3.0](https://img.shields.io/badge/AppVersion-0.3.0-informational?style=flat-square)

A Helm chart for Tractus-X Eclipse Data Space Connector

## TL;DR
```shell
$ helm repo add catenax-ng-product-edc https://catenax-ng.github.io/product-edc
$ helm install tractusx-connector catenax-ng-product-edc/tractusx-connector --version 0.3.0
```

## Values

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| backendService.httpProxyTokenReceiverUrl | string | `""` |  |
| controlplane.affinity | object | `{}` |  |
| controlplane.autoscaling.enabled | bool | `false` | Enables [horizontal pod autoscaling](https://kubernetes.io/docs/tasks/run-application/horizontal-pod-autoscale/https://kubernetes.io/docs/tasks/run-application/horizontal-pod-autoscale/) |
| controlplane.autoscaling.maxReplicas | int | `100` | Maximum replicas if resource consumption exceeds resource threshholds |
| controlplane.autoscaling.minReplicas | int | `1` | Minimal replicas if resource consumption falls below resource threshholds |
| controlplane.autoscaling.targetCPUUtilizationPercentage | int | `80` | targetAverageUtilization of cpu provided to a pod |
| controlplane.autoscaling.targetMemoryUtilizationPercentage | int | `80` | targetAverageUtilization of memory provided to a pod |
| controlplane.debug.enabled | bool | `false` |  |
| controlplane.debug.port | int | `1044` |  |
| controlplane.debug.suspendOnStart | bool | `false` |  |
| controlplane.endpoints | object | `{"control":{"path":"/control","port":8083},"data":{"authKey":"","path":"/data","port":8081},"default":{"path":"/api","port":8080},"ids":{"path":"/api/v1/ids","port":8084},"metrics":{"path":"/metrics","port":8085},"validation":{"path":"/validation","port":8082}}` | endpoints of the control plane |
| controlplane.endpoints.control | object | `{"path":"/control","port":8083}` | control api, used for internal control calls. can be added to the internal ingress, but should probably not |
| controlplane.endpoints.control.path | string | `"/control"` | path for incoming api calls |
| controlplane.endpoints.control.port | int | `8083` | port for incoming api calls |
| controlplane.endpoints.data | object | `{"authKey":"","path":"/data","port":8081}` | data management api, used by internal users, can be added to an ingress and must not be internet facing |
| controlplane.endpoints.data.authKey | string | `""` | authentication key, must be attached to each 'X-Api-Key' request header |
| controlplane.endpoints.data.path | string | `"/data"` | path for incoming api calls |
| controlplane.endpoints.data.port | int | `8081` | port for incoming api calls |
| controlplane.endpoints.default | object | `{"path":"/api","port":8080}` | default api for health checks, should not be added to any ingress |
| controlplane.endpoints.default.path | string | `"/api"` | path for incoming api calls |
| controlplane.endpoints.default.port | int | `8080` | port for incoming api calls |
| controlplane.endpoints.ids | object | `{"path":"/api/v1/ids","port":8084}` | ids api, used for inter connector communication and must be internet facing |
| controlplane.endpoints.ids.path | string | `"/api/v1/ids"` | path for incoming api calls |
| controlplane.endpoints.ids.port | int | `8084` | port for incoming api calls |
| controlplane.endpoints.metrics | object | `{"path":"/metrics","port":8085}` | metrics api, used for application metrics, must not be internet facing |
| controlplane.endpoints.metrics.path | string | `"/metrics"` | path for incoming api calls |
| controlplane.endpoints.metrics.port | int | `8085` | port for incoming api calls |
| controlplane.endpoints.validation | object | `{"path":"/validation","port":8082}` | validation api, only used by the data plane and should not be added to any ingress |
| controlplane.endpoints.validation.path | string | `"/validation"` | path for incoming api calls |
| controlplane.endpoints.validation.port | int | `8082` | port for incoming api calls |
| controlplane.env | object | `{}` |  |
| controlplane.envConfigMapNames | list | `[]` |  |
| controlplane.envSecretNames | list | `[]` |  |
| controlplane.envValueFrom | object | `{}` |  |
| controlplane.image.pullPolicy | string | `"IfNotPresent"` | [Kubernetes image pull policy](https://kubernetes.io/docs/concepts/containers/images/#image-pull-policy) to use |
| controlplane.image.repository | string | `""` | Which derivate of the control plane to use. when left empty the deployment will select the correct image automatically |
| controlplane.image.tag | string | `""` | Overrides the image tag whose default is the chart appVersion |
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
| controlplane.internationalDataSpaces.catalogId | string | `"TXDC-Catalog"` |  |
| controlplane.internationalDataSpaces.curator | string | `""` |  |
| controlplane.internationalDataSpaces.description | string | `"Tractus-X Eclipse IDS Data Space Connector"` |  |
| controlplane.internationalDataSpaces.id | string | `"TXDC"` |  |
| controlplane.internationalDataSpaces.maintainer | string | `""` |  |
| controlplane.internationalDataSpaces.title | string | `""` |  |
| controlplane.livenessProbe.enabled | bool | `true` | Whether to enable kubernetes [liveness-probe](https://kubernetes.io/docs/tasks/configure-pod-container/configure-liveness-readiness-startup-probes/) |
| controlplane.livenessProbe.failureThreshold | int | `6` | when a probe fails kubernetes will try 6 times before giving up |
| controlplane.livenessProbe.initialDelaySeconds | int | `30` | seconds to wait before performing the first liveness check |
| controlplane.livenessProbe.periodSeconds | int | `10` | this fields specifies that kubernetes should perform a liveness check every 10 seconds |
| controlplane.livenessProbe.successThreshold | int | `1` | number of consecutive successes for the probe to be considered successful after having failed |
| controlplane.livenessProbe.timeoutSeconds | int | `5` | number of seconds after which the probe times out |
| controlplane.logging | string | `".level=INFO\nhandlers=java.util.logging.ConsoleHandler\njava.util.logging.ConsoleHandler.formatter=java.util.logging.SimpleFormatter\njava.util.logging.SimpleFormatter.format=[%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS] [%4$-7s] %5$s%6$s%n"` | configuration of the [Java Util Logging Facade](https://docs.oracle.com/javase/7/docs/technotes/guides/logging/overview.html) |
| controlplane.nodeSelector | object | `{}` |  |
| controlplane.opentelemetry | string | `"otel.javaagent.enabled=false\notel.javaagent.debug=false"` | configuration of the [Open Telemetry Agent](https://opentelemetry.io/docs/instrumentation/java/automatic/agent-config/) to collect and expose metrics |
| controlplane.podAnnotations | object | `{}` | additional annotations for the pod |
| controlplane.podLabels | object | `{}` | additional labels for the pod |
| controlplane.podSecurityContext | object | `{"fsGroup":10001,"runAsGroup":10001,"runAsUser":10001,"seccompProfile":{"type":"RuntimeDefault"}}` | The [pod security context](https://kubernetes.io/docs/tasks/configure-pod-container/security-context/#set-the-security-context-for-a-pod) defines privilege and access control settings for a Pod within the deployment |
| controlplane.podSecurityContext.fsGroup | int | `10001` | The owner for volumes and any files created within volumes will belong to this guid |
| controlplane.podSecurityContext.runAsGroup | int | `10001` | Processes within a pod will belong to this guid |
| controlplane.podSecurityContext.runAsUser | int | `10001` | Runs all processes within a pod with a special uid |
| controlplane.podSecurityContext.seccompProfile.type | string | `"RuntimeDefault"` | Restrict a Container's Syscalls with seccomp |
| controlplane.readinessProbe.enabled | bool | `true` | Whether to enable kubernetes [readiness-probes](https://kubernetes.io/docs/tasks/configure-pod-container/configure-liveness-readiness-startup-probes/) |
| controlplane.readinessProbe.failureThreshold | int | `6` | when a probe fails kubernetes will try 6 times before giving up |
| controlplane.readinessProbe.initialDelaySeconds | int | `30` | seconds to wait before performing the first readiness check |
| controlplane.readinessProbe.periodSeconds | int | `10` | this fields specifies that kubernetes should perform a readiness check every 10 seconds |
| controlplane.readinessProbe.successThreshold | int | `1` | number of consecutive successes for the probe to be considered successful after having failed |
| controlplane.readinessProbe.timeoutSeconds | int | `5` | number of seconds after which the probe times out |
| controlplane.replicaCount | int | `1` |  |
| controlplane.resources | object | `{}` | [resource management](https://kubernetes.io/docs/concepts/configuration/manage-resources-containers/) for the container |
| controlplane.securityContext.allowPrivilegeEscalation | bool | `false` | Controls [Privilege Escalation](https://kubernetes.io/docs/concepts/security/pod-security-policy/#privilege-escalation) enabling setuid binaries changing the effective user ID |
| controlplane.securityContext.capabilities.add | list | `[]` | Specifies which capabilities to add to issue specialized syscalls |
| controlplane.securityContext.capabilities.drop | list | `["ALL"]` | Specifies which capabilities to drop to reduce syscall attack surface |
| controlplane.securityContext.readOnlyRootFilesystem | bool | `true` | Whether the root filesystem is mounted in read-only mode |
| controlplane.securityContext.runAsNonRoot | bool | `true` | Requires the container to run without root privileges |
| controlplane.securityContext.runAsUser | int | `10001` | The container's process will run with the specified uid |
| controlplane.service.annotations | object | `{}` |  |
| controlplane.service.type | string | `"ClusterIP"` | [Service type](https://kubernetes.io/docs/concepts/services-networking/service/#publishing-services-service-types) to expose the running application on a set of Pods as a network service. |
| controlplane.tolerations | list | `[]` |  |
| controlplane.url.ids | string | `""` | Explicitly declared url for reaching the ids api (e.g. if ingresses not used) |
| controlplane.volumeMounts | list | `[]` | declare where to mount [volumes](https://kubernetes.io/docs/concepts/storage/volumes/) into the container |
| controlplane.volumes | list | `[]` | [volume](https://kubernetes.io/docs/concepts/storage/volumes/) directories |
| customLabels | object | `{}` |  |
| daps.clientId | string | `""` |  |
| daps.paths.jwks | string | `"/jwks.json"` |  |
| daps.paths.token | string | `"/token"` |  |
| daps.url | string | `""` |  |
| dataplane.affinity | object | `{}` |  |
| dataplane.autoscaling.enabled | bool | `false` | Enables [horizontal pod autoscaling](https://kubernetes.io/docs/tasks/run-application/horizontal-pod-autoscale/https://kubernetes.io/docs/tasks/run-application/horizontal-pod-autoscale/) |
| dataplane.autoscaling.maxReplicas | int | `100` | Maximum replicas if resource consumption exceeds resource threshholds |
| dataplane.autoscaling.minReplicas | int | `1` | Minimal replicas if resource consumption falls below resource threshholds |
| dataplane.autoscaling.targetCPUUtilizationPercentage | int | `80` | targetAverageUtilization of cpu provided to a pod |
| dataplane.autoscaling.targetMemoryUtilizationPercentage | int | `80` | targetAverageUtilization of memory provided to a pod |
| dataplane.aws.accessKeyId | string | `""` |  |
| dataplane.aws.endpointOverride | string | `""` |  |
| dataplane.aws.secretAccessKey | string | `""` |  |
| dataplane.debug.enabled | bool | `false` |  |
| dataplane.debug.port | int | `1044` |  |
| dataplane.debug.suspendOnStart | bool | `false` |  |
| dataplane.endpoints.control.path | string | `"/api/dataplane/control"` |  |
| dataplane.endpoints.control.port | int | `8083` |  |
| dataplane.endpoints.default.path | string | `"/api"` |  |
| dataplane.endpoints.default.port | int | `8080` |  |
| dataplane.endpoints.metrics.path | string | `"/metrics"` |  |
| dataplane.endpoints.metrics.port | int | `8084` |  |
| dataplane.endpoints.public.path | string | `"/api/public"` |  |
| dataplane.endpoints.public.port | int | `8081` |  |
| dataplane.endpoints.validation.path | string | `"/validation"` |  |
| dataplane.endpoints.validation.port | int | `8082` |  |
| dataplane.env | object | `{}` |  |
| dataplane.envConfigMapNames | list | `[]` |  |
| dataplane.envSecretNames | list | `[]` |  |
| dataplane.envValueFrom | object | `{}` |  |
| dataplane.image.pullPolicy | string | `"IfNotPresent"` | [Kubernetes image pull policy](https://kubernetes.io/docs/concepts/containers/images/#image-pull-policy) to use |
| dataplane.image.repository | string | `""` | Which derivate of the data plane to use. when left empty the deployment will select the correct image automatically |
| dataplane.image.tag | string | `""` | Overrides the image tag whose default is the chart appVersion |
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
| dataplane.livenessProbe.enabled | bool | `true` | Whether to enable kubernetes [liveness-probe](https://kubernetes.io/docs/tasks/configure-pod-container/configure-liveness-readiness-startup-probes/) |
| dataplane.livenessProbe.failureThreshold | int | `6` | when a probe fails kubernetes will try 6 times before giving up |
| dataplane.livenessProbe.initialDelaySeconds | int | `30` | seconds to wait before performing the first liveness check |
| dataplane.livenessProbe.periodSeconds | int | `10` | this fields specifies that kubernetes should perform a liveness check every 10 seconds |
| dataplane.livenessProbe.successThreshold | int | `1` | number of consecutive successes for the probe to be considered successful after having failed |
| dataplane.livenessProbe.timeoutSeconds | int | `5` | number of seconds after which the probe times out |
| dataplane.logging | string | `".level=INFO\nhandlers=java.util.logging.ConsoleHandler\njava.util.logging.ConsoleHandler.formatter=java.util.logging.SimpleFormatter\njava.util.logging.SimpleFormatter.format=[%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS] [%4$-7s] %5$s%6$s%n"` | configuration of the [Java Util Logging Facade](https://docs.oracle.com/javase/7/docs/technotes/guides/logging/overview.html) |
| dataplane.nodeSelector | object | `{}` |  |
| dataplane.opentelemetry | string | `"otel.javaagent.enabled=false\notel.javaagent.debug=false"` | configuration of the [Open Telemetry Agent](https://opentelemetry.io/docs/instrumentation/java/automatic/agent-config/) to collect and expose metrics |
| dataplane.podAnnotations | object | `{}` | additional annotations for the pod |
| dataplane.podLabels | object | `{}` | additional labels for the pod |
| dataplane.podSecurityContext | object | `{"fsGroup":10001,"runAsGroup":10001,"runAsUser":10001,"seccompProfile":{"type":"RuntimeDefault"}}` | The [pod security context](https://kubernetes.io/docs/tasks/configure-pod-container/security-context/#set-the-security-context-for-a-pod) defines privilege and access control settings for a Pod within the deployment |
| dataplane.podSecurityContext.fsGroup | int | `10001` | The owner for volumes and any files created within volumes will belong to this guid |
| dataplane.podSecurityContext.runAsGroup | int | `10001` | Processes within a pod will belong to this guid |
| dataplane.podSecurityContext.runAsUser | int | `10001` | Runs all processes within a pod with a special uid |
| dataplane.podSecurityContext.seccompProfile.type | string | `"RuntimeDefault"` | Restrict a Container's Syscalls with seccomp |
| dataplane.readinessProbe.enabled | bool | `true` | Whether to enable kubernetes [readiness-probes](https://kubernetes.io/docs/tasks/configure-pod-container/configure-liveness-readiness-startup-probes/) |
| dataplane.readinessProbe.failureThreshold | int | `6` | when a probe fails kubernetes will try 6 times before giving up |
| dataplane.readinessProbe.initialDelaySeconds | int | `30` | seconds to wait before performing the first readiness check |
| dataplane.readinessProbe.periodSeconds | int | `10` | this fields specifies that kubernetes should perform a liveness check every 10 seconds |
| dataplane.readinessProbe.successThreshold | int | `1` | number of consecutive successes for the probe to be considered successful after having failed |
| dataplane.readinessProbe.timeoutSeconds | int | `5` | number of seconds after which the probe times out |
| dataplane.replicaCount | int | `1` |  |
| dataplane.resources | object | `{}` | [resource management](https://kubernetes.io/docs/concepts/configuration/manage-resources-containers/) for the container |
| dataplane.securityContext.allowPrivilegeEscalation | bool | `false` | Controls [Privilege Escalation](https://kubernetes.io/docs/concepts/security/pod-security-policy/#privilege-escalation) enabling setuid binaries changing the effective user ID |
| dataplane.securityContext.capabilities.add | list | `[]` | Specifies which capabilities to add to issue specialized syscalls |
| dataplane.securityContext.capabilities.drop | list | `["ALL"]` | Specifies which capabilities to drop to reduce syscall attack surface |
| dataplane.securityContext.readOnlyRootFilesystem | bool | `true` | Whether the root filesystem is mounted in read-only mode |
| dataplane.securityContext.runAsNonRoot | bool | `true` | Requires the container to run without root privileges |
| dataplane.securityContext.runAsUser | int | `10001` | The container's process will run with the specified uid |
| dataplane.service.port | int | `80` |  |
| dataplane.service.type | string | `"ClusterIP"` | [Service type](https://kubernetes.io/docs/concepts/services-networking/service/#publishing-services-service-types) to expose the running application on a set of Pods as a network service. |
| dataplane.tolerations | list | `[]` |  |
| dataplane.url.public | string | `""` | Explicitly declared url for reaching the public api (e.g. if ingresses not used) |
| dataplane.volumeMounts | list | `[]` | declare where to mount [volumes](https://kubernetes.io/docs/concepts/storage/volumes/) into the container |
| dataplane.volumes | list | `[]` | [volume](https://kubernetes.io/docs/concepts/storage/volumes/) directories |
| fullnameOverride | string | `""` |  |
| imagePullSecrets | list | `[]` | Existing image pull secret to use to [obtain the container image from private registries](https://kubernetes.io/docs/concepts/containers/images/#using-a-private-registry) |
| nameOverride | string | `""` |  |
| postgresql.enabled | bool | `false` |  |
| postgresql.jdbcUrl | string | `""` |  |
| postgresql.password | string | `""` |  |
| postgresql.username | string | `""` |  |
| serviceAccount.annotations | object | `{}` |  |
| serviceAccount.create | bool | `true` |  |
| serviceAccount.imagePullSecrets | list | `[]` | Existing image pull secret bound to the service account to use to [obtain the container image from private registries](https://kubernetes.io/docs/concepts/containers/images/#using-a-private-registry) |
| serviceAccount.name | string | `""` |  |
| vault.azure.certificate | string | `""` |  |
| vault.azure.client | string | `""` |  |
| vault.azure.enabled | bool | `false` |  |
| vault.azure.name | string | `""` |  |
| vault.azure.secret | string | `""` |  |
| vault.azure.tenant | string | `""` |  |
| vault.hashicorp.enabled | bool | `false` |  |
| vault.hashicorp.healthCheck.enabled | bool | `true` |  |
| vault.hashicorp.healthCheck.standbyOk | bool | `true` |  |
| vault.hashicorp.paths.health | string | `"/v1/sys/health"` |  |
| vault.hashicorp.paths.secret | string | `"/v1/secret"` |  |
| vault.hashicorp.timeout | int | `30` |  |
| vault.hashicorp.token | string | `""` |  |
| vault.hashicorp.url | string | `""` |  |
| vault.secretNames.dapsPrivateKey | string | `"daps-private-key"` |  |
| vault.secretNames.dapsPublicKey | string | `"daps-public-key"` |  |
| vault.secretNames.transferProxyTokenEncryptionAesKey | string | `"transfer-proxy-token-encryption-aes-key"` |  |
| vault.secretNames.transferProxyTokenSignerPrivateKey | string | `"transfer-proxy-token-signer-private-key"` |  |
| vault.secretNames.transferProxyTokenSignerPublicKey | string | `"transfer-proxy-token-signer-public-key"` |  |

----------------------------------------------
Autogenerated from chart metadata using [helm-docs v1.10.0](https://github.com/norwoodj/helm-docs/releases/v1.10.0)
