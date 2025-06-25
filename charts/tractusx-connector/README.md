# tractusx-connector

![Version: 0.10.0](https://img.shields.io/badge/Version-0.10.0-informational?style=flat-square) ![Type: application](https://img.shields.io/badge/Type-application-informational?style=flat-square) ![AppVersion: 0.10.0](https://img.shields.io/badge/AppVersion-0.10.0-informational?style=flat-square)

A Helm chart for Tractus-X Eclipse Data Space Connector. The connector deployment consists of two runtime consists of a
Control Plane and a Data Plane. Note that _no_ external dependencies such as a PostgreSQL database and HashiCorp Vault are included.

This chart is intended for use with an _existing_ PostgreSQL database and an _existing_ HashiCorp Vault.

**Homepage:** <https://github.com/eclipse-tractusx/tractusx-edc/tree/main/charts/tractusx-connector>

## Setting up IATP

### Preconditions

- You'll need an account with DIM, the wallet for VerifiableCredentials
- the necessary set of VerifiableCredentials for this participant must already be issued to your DIM tenant. This is typically done by the
  Portal during participant onboarding
- the client ID and client secret corresponding to that account must be known

### Preparatory work

- store client secret in the HashiCorp vault using an alias. The exact procedure will depend on your deployment of HashiCorp Vault and
  is out of scope of this document. But by default, Tractus-X EDC expects to find the secret under `secret/client-secret`. The alias must be configured
  using the `iatp.sts.oauth.client.secret_alias` Helm value.

### Configure the chart

Be sure to provide the following configuration entries to your Tractus-X EDC Helm chart:
- `iatp.sts.oauth.token_url`: the token endpoint of DIM
- `iatp.sts.oauth.client.id`: the client ID of your tenant in DIM
- `iatp.sts.oauth.client.secret_alias`: alias under which you saved your DIM client secret in the vault
- `iatp.sts.dim.url`: the base URL for DIM

In addition, in order to map BPNs to DIDs, a new service is required, called the BPN-DID Resolution Service, which
must be configured:
- `controlplane.bdrs.server.url`: base URL of the BPN-DID Resolution Service ("BDRS")

### Launching the application

As an easy starting point, please consider using [this example configuration](https://github.com/eclipse-tractusx/tractusx-edc/blob/main/edc-tests/deployment/src/main/resources/helm/tractusx-connector-test.yaml)
to launch the application. The configuration values mentioned above (`controlplane.ssi.*`) will have to be adapted manually.
Combined, run this shell command to start the in-memory Tractus-X EDC runtime:

```shell
helm repo add tractusx-edc https://eclipse-tractusx.github.io/charts/dev
helm install my-release tractusx-edc/tractusx-connector --version 0.10.0 \
     -f <path-to>/tractusx-connector-test.yaml
```

## Source Code

* <https://github.com/eclipse-tractusx/tractusx-edc/tree/main/charts/tractusx-connector>

## Requirements

| Repository | Name | Version |
|------------|------|---------|
| https://charts.bitnami.com/bitnami | postgresql(postgresql) | 15.2.1 |
| https://helm.releases.hashicorp.com | vault(vault) | 0.27.0 |

## Values

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| controlplane.affinity | object | `{}` | [affinity](https://kubernetes.io/docs/concepts/scheduling-eviction/assign-pod-node/#affinity-and-anti-affinity) to configure which nodes the pods can be scheduled on |
| controlplane.autoscaling.enabled | bool | `false` | Enables [horizontal pod autoscaling](https://kubernetes.io/docs/tasks/run-application/horizontal-pod-autoscale/https://kubernetes.io/docs/tasks/run-application/horizontal-pod-autoscale/) |
| controlplane.autoscaling.maxReplicas | int | `100` | Maximum replicas if resource consumption exceeds resource threshholds |
| controlplane.autoscaling.minReplicas | int | `1` | Minimal replicas if resource consumption falls below resource threshholds |
| controlplane.autoscaling.targetCPUUtilizationPercentage | int | `80` | targetAverageUtilization of cpu provided to a pod |
| controlplane.autoscaling.targetMemoryUtilizationPercentage | int | `80` | targetAverageUtilization of memory provided to a pod |
| controlplane.bdrs.cache_validity_seconds | int | `600` | Time that a cached BPN/DID resolution map is valid in seconds, default is 600 seconds (10 min) |
| controlplane.bdrs.server.url | string | `nil` | URL of the BPN/DID Resolution Service |
| controlplane.catalog | object | `{"crawler":{"initialDelay":null,"num":null,"period":null,"targetsFile":null},"enabled":false}` | configuration for the built-in federated catalog crawler |
| controlplane.catalog.crawler.initialDelay | string | `nil` | Initial delay for the crawling to start. Leave blank for a random delay |
| controlplane.catalog.crawler.num | string | `nil` | Number of desired crawlers. Final number might be different, based on number of crawl targets |
| controlplane.catalog.crawler.period | string | `nil` | Period between two crawl runs in seconds. Default is 60 seconds. |
| controlplane.catalog.crawler.targetsFile | string | `nil` | File path to a JSON file containing TargetNode entries |
| controlplane.catalog.enabled | bool | `false` | Flag to globally enable/disable the FC feature |
| controlplane.debug.enabled | bool | `false` | Enables java debugging mode. |
| controlplane.debug.port | int | `1044` | Port where the debuggee can connect to. |
| controlplane.debug.suspendOnStart | bool | `false` | Defines if the JVM should wait with starting the application until someone connected to the debugging port. |
| controlplane.endpoints | object | `{"catalog":{"authKey":"password","path":"/catalog","port":8085},"control":{"path":"/control","port":8083},"default":{"path":"/api","port":8080},"management":{"authKey":"password","jwksUrl":null,"path":"/management","port":8081},"metrics":{"path":"/metrics","port":9090},"protocol":{"path":"/api/v1/dsp","port":8084}}` | endpoints of the control plane |
| controlplane.endpoints.catalog.authKey | string | `"password"` | authentication key, must be attached to each request as `X-Api-Key` header |
| controlplane.endpoints.catalog.path | string | `"/catalog"` | path for incoming catalog cache query requests |
| controlplane.endpoints.catalog.port | int | `8085` | port for incoming catalog cache query requests |
| controlplane.endpoints.control | object | `{"path":"/control","port":8083}` | control api, used for internal control calls. can be added to the internal ingress, but should probably not |
| controlplane.endpoints.control.path | string | `"/control"` | path for incoming api calls |
| controlplane.endpoints.control.port | int | `8083` | port for incoming api calls |
| controlplane.endpoints.default | object | `{"path":"/api","port":8080}` | default api for health checks, should not be added to any ingress |
| controlplane.endpoints.default.path | string | `"/api"` | path for incoming api calls |
| controlplane.endpoints.default.port | int | `8080` | port for incoming api calls |
| controlplane.endpoints.management | object | `{"authKey":"password","jwksUrl":null,"path":"/management","port":8081}` | data management api, used by internal users, can be added to an ingress and must not be internet facing |
| controlplane.endpoints.management.authKey | string | `"password"` | authentication key, must be attached to each request as `X-Api-Key` header |
| controlplane.endpoints.management.jwksUrl | string | `nil` | if the JWKS url is set, the DelegatedAuth service will be engaged |
| controlplane.endpoints.management.path | string | `"/management"` | path for incoming api calls |
| controlplane.endpoints.management.port | int | `8081` | port for incoming api calls |
| controlplane.endpoints.metrics | object | `{"path":"/metrics","port":9090}` | metrics api, used for application metrics, must not be internet facing |
| controlplane.endpoints.metrics.path | string | `"/metrics"` | path for incoming api calls |
| controlplane.endpoints.metrics.port | int | `9090` | port for incoming api calls |
| controlplane.endpoints.protocol | object | `{"path":"/api/v1/dsp","port":8084}` | dsp api, used for inter connector communication and must be internet facing |
| controlplane.endpoints.protocol.path | string | `"/api/v1/dsp"` | path for incoming api calls |
| controlplane.endpoints.protocol.port | int | `8084` | port for incoming api calls |
| controlplane.env | object | `{}` | Extra environment variables that will be pass onto deployment pods |
| controlplane.envConfigMapNames | list | `[]` | [Kubernetes ConfigMap Resource](https://kubernetes.io/docs/concepts/configuration/configmap/) names to load environment variables from |
| controlplane.envSecretNames | list | `[]` | [Kubernetes Secret Resource](https://kubernetes.io/docs/concepts/configuration/secret/) names to load environment variables from |
| controlplane.envValueFrom | object | `{}` | "valueFrom" environment variable references that will be added to deployment pods. Name is templated. ref: https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.19/#envvarsource-v1-core |
| controlplane.image.pullPolicy | string | `"IfNotPresent"` | [Kubernetes image pull policy](https://kubernetes.io/docs/concepts/containers/images/#image-pull-policy) to use |
| controlplane.image.repository | string | `""` | Which derivate of the control plane to use. When left empty the deployment will select the correct image automatically |
| controlplane.image.tag | string | `""` | Overrides the image tag whose default is the chart appVersion |
| controlplane.ingresses[0].annotations | object | `{}` | Additional ingress annotations to add |
| controlplane.ingresses[0].certManager.clusterIssuer | string | `""` | If preset enables certificate generation via cert-manager cluster-wide issuer |
| controlplane.ingresses[0].certManager.issuer | string | `""` | If preset enables certificate generation via cert-manager namespace scoped issuer |
| controlplane.ingresses[0].className | string | `""` | Defines the [ingress class](https://kubernetes.io/docs/concepts/services-networking/ingress/#ingress-class)  to use |
| controlplane.ingresses[0].enabled | bool | `false` |  |
| controlplane.ingresses[0].endpoints | list | `["protocol"]` | EDC endpoints exposed by this ingress resource |
| controlplane.ingresses[0].hostname | string | `"edc-control.local"` | The hostname to be used to precisely map incoming traffic onto the underlying network service |
| controlplane.ingresses[0].tls | object | `{"enabled":false,"secretName":""}` | TLS [tls class](https://kubernetes.io/docs/concepts/services-networking/ingress/#tls) applied to the ingress resource |
| controlplane.ingresses[0].tls.enabled | bool | `false` | Enables TLS on the ingress resource |
| controlplane.ingresses[0].tls.secretName | string | `""` | If present overwrites the default secret name |
| controlplane.ingresses[1].annotations | object | `{}` | Additional ingress annotations to add |
| controlplane.ingresses[1].certManager.clusterIssuer | string | `""` | If preset enables certificate generation via cert-manager cluster-wide issuer |
| controlplane.ingresses[1].certManager.issuer | string | `""` | If preset enables certificate generation via cert-manager namespace scoped issuer |
| controlplane.ingresses[1].className | string | `""` | Defines the [ingress class](https://kubernetes.io/docs/concepts/services-networking/ingress/#ingress-class)  to use |
| controlplane.ingresses[1].enabled | bool | `false` |  |
| controlplane.ingresses[1].endpoints | list | `["management","control"]` | EDC endpoints exposed by this ingress resource |
| controlplane.ingresses[1].hostname | string | `"edc-control.intranet"` | The hostname to be used to precisely map incoming traffic onto the underlying network service |
| controlplane.ingresses[1].tls | object | `{"enabled":false,"secretName":""}` | TLS [tls class](https://kubernetes.io/docs/concepts/services-networking/ingress/#tls) applied to the ingress resource |
| controlplane.ingresses[1].tls.enabled | bool | `false` | Enables TLS on the ingress resource |
| controlplane.ingresses[1].tls.secretName | string | `""` | If present overwrites the default secret name |
| controlplane.initContainers | list | `[]` |  |
| controlplane.livenessProbe.enabled | bool | `true` | Whether to enable kubernetes [liveness-probe](https://kubernetes.io/docs/tasks/configure-pod-container/configure-liveness-readiness-startup-probes/) |
| controlplane.livenessProbe.failureThreshold | int | `6` | when a probe fails kubernetes will try 6 times before giving up |
| controlplane.livenessProbe.initialDelaySeconds | int | `30` | seconds to wait before performing the first liveness check |
| controlplane.livenessProbe.periodSeconds | int | `10` | this fields specifies that kubernetes should perform a liveness check every 10 seconds |
| controlplane.livenessProbe.successThreshold | int | `1` | number of consecutive successes for the probe to be considered successful after having failed |
| controlplane.livenessProbe.timeoutSeconds | int | `5` | number of seconds after which the probe times out |
| controlplane.logs.level | string | `"DEBUG"` | Defines the log granularity of the default Console Monitor. |
| controlplane.nodeSelector | object | `{}` | [node selector](https://kubernetes.io/docs/concepts/scheduling-eviction/assign-pod-node/#nodeselector) to constrain pods to nodes |
| controlplane.opentelemetry | string | `"otel.javaagent.enabled=false\notel.javaagent.debug=false"` | configuration of the [Open Telemetry Agent](https://opentelemetry.io/docs/instrumentation/java/automatic/agent-config/) to collect and expose metrics |
| controlplane.podAnnotations | object | `{}` | additional annotations for the pod |
| controlplane.podLabels | object | `{}` | additional labels for the pod |
| controlplane.podSecurityContext | object | `{"fsGroup":10001,"runAsGroup":10001,"runAsUser":10001,"seccompProfile":{"type":"RuntimeDefault"}}` | The [pod security context](https://kubernetes.io/docs/tasks/configure-pod-container/security-context/#set-the-security-context-for-a-pod) defines privilege and access control settings for a Pod within the deployment |
| controlplane.podSecurityContext.fsGroup | int | `10001` | The owner for volumes and any files created within volumes will belong to this guid |
| controlplane.podSecurityContext.runAsGroup | int | `10001` | Processes within a pod will belong to this guid |
| controlplane.podSecurityContext.runAsUser | int | `10001` | Runs all processes within a pod with a special uid |
| controlplane.podSecurityContext.seccompProfile.type | string | `"RuntimeDefault"` | Restrict a Container's Syscalls with seccomp |
| controlplane.policy | object | `{"validation":{"enabled":true}}` | configuration for policy engine |
| controlplane.readinessProbe.enabled | bool | `true` | Whether to enable kubernetes [readiness-probes](https://kubernetes.io/docs/tasks/configure-pod-container/configure-liveness-readiness-startup-probes/) |
| controlplane.readinessProbe.failureThreshold | int | `6` | when a probe fails kubernetes will try 6 times before giving up |
| controlplane.readinessProbe.initialDelaySeconds | int | `30` | seconds to wait before performing the first readiness check |
| controlplane.readinessProbe.periodSeconds | int | `10` | this fields specifies that kubernetes should perform a readiness check every 10 seconds |
| controlplane.readinessProbe.successThreshold | int | `1` | number of consecutive successes for the probe to be considered successful after having failed |
| controlplane.readinessProbe.timeoutSeconds | int | `5` | number of seconds after which the probe times out |
| controlplane.replicaCount | int | `1` |  |
| controlplane.resources | object | `{"limits":{"cpu":1.5,"memory":"1024Mi"},"requests":{"cpu":"500m","memory":"1024Mi"}}` | [resource management](https://kubernetes.io/docs/concepts/configuration/manage-resources-containers/) for the container |
| controlplane.resources.limits.cpu | float | `1.5` | Maximum CPU limit |
| controlplane.resources.limits.memory | string | `"1024Mi"` | Maximum memory limit |
| controlplane.resources.requests.cpu | string | `"500m"` | Initial CPU request |
| controlplane.resources.requests.memory | string | `"1024Mi"` | Initial memory request |
| controlplane.securityContext.allowPrivilegeEscalation | bool | `false` | Controls [Privilege Escalation](https://kubernetes.io/docs/concepts/security/pod-security-policy/#privilege-escalation) enabling setuid binaries changing the effective user ID |
| controlplane.securityContext.capabilities.add | list | `[]` | Specifies which capabilities to add to issue specialized syscalls |
| controlplane.securityContext.capabilities.drop | list | `["ALL"]` | Specifies which capabilities to drop to reduce syscall attack surface |
| controlplane.securityContext.readOnlyRootFilesystem | bool | `true` | Whether the root filesystem is mounted in read-only mode |
| controlplane.securityContext.runAsNonRoot | bool | `true` | Requires the container to run without root privileges |
| controlplane.securityContext.runAsUser | int | `10001` | The container's process will run with the specified uid |
| controlplane.service.annotations | object | `{}` | additional annotations for the service |
| controlplane.service.labels | object | `{}` | additional labels for the service |
| controlplane.service.type | string | `"ClusterIP"` | [Service type](https://kubernetes.io/docs/concepts/services-networking/service/#publishing-services-service-types) to expose the running application on a set of Pods as a network service. |
| controlplane.tolerations | list | `[]` | [tolerations](https://kubernetes.io/docs/concepts/scheduling-eviction/taint-and-toleration/) to configure preferred nodes |
| controlplane.url.protocol | string | `""` | Explicitly declared url for reaching the dsp api (e.g. if ingresses not used) |
| controlplane.volumeMounts | string | `nil` | declare where to mount [volumes](https://kubernetes.io/docs/concepts/storage/volumes/) into the container |
| controlplane.volumes | string | `nil` | [volume](https://kubernetes.io/docs/concepts/storage/volumes/) directories |
| customCaCerts | object | `{}` | Add custom ca certificates to the truststore |
| customLabels | object | `{}` | Add some custom labels |
| dataplane.affinity | object | `{}` | [affinity](https://kubernetes.io/docs/concepts/scheduling-eviction/assign-pod-node/#affinity-and-anti-affinity) to configure which nodes the pods can be scheduled on |
| dataplane.autoscaling.enabled | bool | `false` | Enables [horizontal pod autoscaling](https://kubernetes.io/docs/tasks/run-application/horizontal-pod-autoscale/https://kubernetes.io/docs/tasks/run-application/horizontal-pod-autoscale/) |
| dataplane.autoscaling.maxReplicas | int | `100` | Maximum replicas if resource consumption exceeds resource threshholds |
| dataplane.autoscaling.minReplicas | int | `1` | Minimal replicas if resource consumption falls below resource threshholds |
| dataplane.autoscaling.targetCPUUtilizationPercentage | int | `80` | targetAverageUtilization of cpu provided to a pod |
| dataplane.autoscaling.targetMemoryUtilizationPercentage | int | `80` | targetAverageUtilization of memory provided to a pod |
| dataplane.aws.accessKeyId | string | `""` |  |
| dataplane.aws.endpointOverride | string | `""` |  |
| dataplane.aws.secretAccessKey | string | `""` |  |
| dataplane.debug.enabled | bool | `false` | Enables java debugging mode. |
| dataplane.debug.port | int | `1044` | Port where the debuggee can connect to. |
| dataplane.debug.suspendOnStart | bool | `false` | Defines if the JVM should wait with starting the application until someone connected to the debugging port. |
| dataplane.endpoints | object | `{"control":{"path":"/api/control","port":8084},"default":{"path":"/api","port":8080},"metrics":{"path":"/metrics","port":9090},"proxy":{"authKey":"password","path":"/proxy","port":8186},"public":{"path":"/api/public","port":8081}}` | endpoints of the dataplane |
| dataplane.endpoints.control | object | `{"path":"/api/control","port":8084}` | control api, used for internal control calls. can be added to the internal ingress, but should probably not |
| dataplane.endpoints.control.path | string | `"/api/control"` | path for incoming api calls |
| dataplane.endpoints.control.port | int | `8084` | port for incoming api calls |
| dataplane.endpoints.default | object | `{"path":"/api","port":8080}` | default api for health checks, should not be added to any ingress |
| dataplane.endpoints.default.path | string | `"/api"` | path for incoming api calls |
| dataplane.endpoints.default.port | int | `8080` | port for incoming api calls |
| dataplane.endpoints.metrics | object | `{"path":"/metrics","port":9090}` | metrics api, used for application metrics, must not be internet facing |
| dataplane.endpoints.metrics.path | string | `"/metrics"` | path for incoming api calls |
| dataplane.endpoints.metrics.port | int | `9090` | port for incoming api calls |
| dataplane.endpoints.proxy.authKey | string | `"password"` | authentication key, must be attached to each request as `X-Api-Key` header |
| dataplane.endpoints.proxy.path | string | `"/proxy"` | path for incoming api calls |
| dataplane.endpoints.proxy.port | int | `8186` | port for incoming api calls |
| dataplane.endpoints.public | object | `{"path":"/api/public","port":8081}` | public endpoint where the data can be fetched from if HttpPull was used. Must be internet facing. |
| dataplane.endpoints.public.path | string | `"/api/public"` | path for incoming api calls |
| dataplane.endpoints.public.port | int | `8081` | port for incoming api calls |
| dataplane.env | object | `{}` | Extra environment variables that will be pass onto deployment pods |
| dataplane.envConfigMapNames | list | `[]` | [Kubernetes ConfigMap Resource](https://kubernetes.io/docs/concepts/configuration/configmap/) names to load environment variables from |
| dataplane.envSecretNames | list | `[]` | [Kubernetes Secret Resource](https://kubernetes.io/docs/concepts/configuration/secret/) names to load environment variables from |
| dataplane.envValueFrom | object | `{}` | "valueFrom" environment variable references that will be added to deployment pods. Name is templated. ref: https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.19/#envvarsource-v1-core |
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
| dataplane.logs.level | string | `"DEBUG"` | Defines the log granularity of the default Console Monitor. |
| dataplane.nodeSelector | object | `{}` | [node selector](https://kubernetes.io/docs/concepts/scheduling-eviction/assign-pod-node/#nodeselector) to constrain pods to nodes |
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
| dataplane.resources | object | `{"limits":{"cpu":1.5,"memory":"1024Mi"},"requests":{"cpu":"500m","memory":"1024Mi"}}` | [resource management](https://kubernetes.io/docs/concepts/configuration/manage-resources-containers/) for the container |
| dataplane.resources.limits.cpu | float | `1.5` | Maximum CPU limit |
| dataplane.resources.limits.memory | string | `"1024Mi"` | Maximum memory limit |
| dataplane.resources.requests.cpu | string | `"500m"` | Initial CPU request |
| dataplane.resources.requests.memory | string | `"1024Mi"` | Initial memory request |
| dataplane.securityContext | object | `{"allowPrivilegeEscalation":false,"capabilities":{"add":[],"drop":["ALL"]},"readOnlyRootFilesystem":true,"runAsNonRoot":true,"runAsUser":10001}` | The [container security context](https://kubernetes.io/docs/tasks/configure-pod-container/security-context/#set-the-security-context-for-a-container) defines privilege and access control settings for a Container within a pod |
| dataplane.securityContext.allowPrivilegeEscalation | bool | `false` | Controls [Privilege Escalation](https://kubernetes.io/docs/concepts/security/pod-security-policy/#privilege-escalation) enabling setuid binaries changing the effective user ID |
| dataplane.securityContext.capabilities.add | list | `[]` | Specifies which capabilities to add to issue specialized syscalls |
| dataplane.securityContext.capabilities.drop | list | `["ALL"]` | Specifies which capabilities to drop to reduce syscall attack surface |
| dataplane.securityContext.readOnlyRootFilesystem | bool | `true` | Whether the root filesystem is mounted in read-only mode |
| dataplane.securityContext.runAsNonRoot | bool | `true` | Requires the container to run without root privileges |
| dataplane.securityContext.runAsUser | int | `10001` | The container's process will run with the specified uid |
| dataplane.service.annotations | object | `{}` | additional annotations for the service |
| dataplane.service.labels | object | `{}` | additional labels for the service |
| dataplane.service.type | string | `"ClusterIP"` | [Service type](https://kubernetes.io/docs/concepts/services-networking/service/#publishing-services-service-types) to expose the running application on a set of Pods as a network service. |
| dataplane.token.refresh.expiry_seconds | int | `300` | TTL in seconds for access tokens (also known as EDR token) |
| dataplane.token.refresh.expiry_tolerance_seconds | int | `10` | Tolerance for token expiry in seconds |
| dataplane.token.refresh.refresh_endpoint | string | `nil` | Optional endpoint for an OAuth2 token refresh. Default endpoint is `<PUBLIC_API>/token` |
| dataplane.token.signer.privatekey_alias | string | `nil` | Alias under which the private key (JWK or PEM format) is stored in the vault |
| dataplane.token.verifier.publickey_alias | string | `nil` | Alias under which the public key (JWK or PEM format) is stored in the vault, that belongs to the private key which was referred to at `dataplane.token.signer.privatekey_alias` |
| dataplane.tolerations | list | `[]` | [tolerations](https://kubernetes.io/docs/concepts/scheduling-eviction/taint-and-toleration/) to configure preferred nodes |
| dataplane.url.public | string | `""` | Explicitly declared url for reaching the public api (e.g. if ingresses not used) |
| dataplane.volumeMounts | string | `nil` | declare where to mount [volumes](https://kubernetes.io/docs/concepts/storage/volumes/) into the container |
| dataplane.volumes | string | `nil` | [volume](https://kubernetes.io/docs/concepts/storage/volumes/) directories |
| fullnameOverride | string | `""` |  |
| iatp.id | string | `"did:web:changeme"` | Decentralized IDentifier (DID) of the connector |
| iatp.sts.dim.url | string | `nil` | URL where connectors can request SI tokens |
| iatp.sts.oauth.client.id | string | `nil` | Client ID for requesting OAuth2 access token for DIM access |
| iatp.sts.oauth.client.secret_alias | string | `nil` | Alias under which the client secret is stored in the vault for requesting OAuth2 access token for DIM access |
| iatp.sts.oauth.token_url | string | `nil` | URL where connectors can request OAuth2 access tokens for DIM access |
| iatp.trustedIssuers | list | `[]` | Configures the trusted issuers for this runtime |
| imagePullSecrets | list | `[]` | Existing image pull secret to use to [obtain the container image from private registries](https://kubernetes.io/docs/concepts/containers/images/#using-a-private-registry) |
| install.postgresql | bool | `true` | Deploying a PostgreSQL instance |
| install.vault | bool | `true` | Deploying a HashiCorp Vault instance |
| nameOverride | string | `""` |  |
| networkPolicy.controlplane | object | `{"from":[{"namespaceSelector":{}}]}` | Configuration of the controlplane component |
| networkPolicy.controlplane.from | list | `[{"namespaceSelector":{}}]` | Specify from rule network policy for cp (defaults to all namespaces) |
| networkPolicy.dataplane | object | `{"from":[{"namespaceSelector":{}}]}` | Configuration of the dataplane component |
| networkPolicy.dataplane.from | list | `[{"namespaceSelector":{}}]` | Specify from rule network policy for dp (defaults to all namespaces) |
| networkPolicy.enabled | bool | `false` | If `true` network policy will be created to restrict access to control- and dataplane |
| participant.id | string | `"BPNLCHANGEME"` | BPN Number |
| postgresql.auth.database | string | `"edc"` |  |
| postgresql.auth.password | string | `"password"` |  |
| postgresql.auth.username | string | `"user"` |  |
| postgresql.jdbcUrl | string | `"jdbc:postgresql://{{ .Release.Name }}-postgresql:5432/edc"` |  |
| postgresql.primary.persistence.enabled | bool | `false` |  |
| postgresql.readReplicas.persistence.enabled | bool | `false` |  |
| serviceAccount.annotations | object | `{}` | Annotations to add to the service account |
| serviceAccount.create | bool | `true` | Specifies whether a service account should be created |
| serviceAccount.imagePullSecrets | list | `[]` | Existing image pull secret bound to the service account to use to [obtain the container image from private registries](https://kubernetes.io/docs/concepts/containers/images/#using-a-private-registry) |
| serviceAccount.name | string | `""` | The name of the service account to use. If not set and create is true, a name is generated using the fullname template |
| tests | object | `{"hookDeletePolicy":"before-hook-creation,hook-succeeded"}` | Configurations for Helm tests |
| tests.hookDeletePolicy | string | `"before-hook-creation,hook-succeeded"` | Configure the hook-delete-policy for Helm tests |
| vault.hashicorp.healthCheck.enabled | bool | `true` |  |
| vault.hashicorp.healthCheck.standbyOk | bool | `true` |  |
| vault.hashicorp.paths.folder | string | `""` |  |
| vault.hashicorp.paths.health | string | `"/v1/sys/health"` |  |
| vault.hashicorp.paths.secret | string | `"/v1/secret"` |  |
| vault.hashicorp.timeout | int | `30` |  |
| vault.hashicorp.token | string | `"root"` |  |
| vault.hashicorp.url | string | `"http://{{ .Release.Name }}-vault:8200"` |  |
| vault.injector.enabled | bool | `false` |  |
| vault.server.dev.devRootToken | string | `"root"` |  |
| vault.server.dev.enabled | bool | `true` |  |
| vault.server.postStart | string | `nil` |  |

----------------------------------------------
Autogenerated from chart metadata using [helm-docs](https://github.com/norwoodj/helm-docs/)
