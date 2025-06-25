# tractusx-connector-memory

![Version: 0.10.0](https://img.shields.io/badge/Version-0.10.0-informational?style=flat-square) ![Type: application](https://img.shields.io/badge/Type-application-informational?style=flat-square) ![AppVersion: 0.10.0](https://img.shields.io/badge/AppVersion-0.10.0-informational?style=flat-square)

A Helm chart for Tractus-X Eclipse Data Space Connector based on memory. Please only use this for development or testing purposes, never in production workloads!

**Homepage:** <https://github.com/eclipse-tractusx/tractusx-edc/tree/main/charts/tractusx-connector-memory>

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
helm install my-release tractusx-edc/tractusx-connector-memory --version 0.10.0 \
     -f <path-to>/tractusx-connector-memory-test.yaml \
     --set vault.secrets="client-secret:$YOUR_CLIENT_SECRET"
```

## Source Code

* <https://github.com/eclipse-tractusx/tractusx-edc/tree/main/charts/tractusx-connector-memory>

## Values

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| customCaCerts | object | `{}` | Add custom ca certificates to the truststore |
| customLabels | object | `{}` | Add some custom labels |
| fullnameOverride | string | `""` |  |
| iatp.id | string | `"did:web:changeme"` | Decentralized IDentifier (DID) of the connector |
| iatp.sts.dim.url | string | `nil` | URL where connectors can request SI tokens |
| iatp.sts.oauth.client.id | string | `nil` | Client ID for requesting OAuth2 access token for DIM access |
| iatp.sts.oauth.client.secret_alias | string | `nil` | Alias under which the client secret is stored in the vault for requesting OAuth2 access token for DIM access |
| iatp.sts.oauth.token_url | string | `nil` | URL where connectors can request OAuth2 access tokens for DIM access |
| iatp.trustedIssuers | list | `[]` | Configures the trusted issuers for this runtime |
| imagePullSecrets | list | `[]` | Existing image pull secret to use to [obtain the container image from private registries](https://kubernetes.io/docs/concepts/containers/images/#using-a-private-registry) |
| nameOverride | string | `""` |  |
| participant.id | string | `"BPNLCHANGEME"` | BPN Number |
| runtime.affinity | object | `{}` | [affinity](https://kubernetes.io/docs/concepts/scheduling-eviction/assign-pod-node/#affinity-and-anti-affinity) to configure which nodes the pods can be scheduled on |
| runtime.autoscaling.enabled | bool | `false` | Enables [horizontal pod autoscaling](https://kubernetes.io/docs/tasks/run-application/horizontal-pod-autoscale/https://kubernetes.io/docs/tasks/run-application/horizontal-pod-autoscale/) |
| runtime.autoscaling.maxReplicas | int | `100` | Maximum replicas if resource consumption exceeds resource threshholds |
| runtime.autoscaling.minReplicas | int | `1` | Minimal replicas if resource consumption falls below resource threshholds |
| runtime.autoscaling.targetCPUUtilizationPercentage | int | `80` | targetAverageUtilization of cpu provided to a pod |
| runtime.autoscaling.targetMemoryUtilizationPercentage | int | `80` | targetAverageUtilization of memory provided to a pod |
| runtime.bdrs.cache_validity_seconds | int | `600` | Time that a cached BPN/DID resolution map is valid in seconds, default is 600 seconds (10 min) |
| runtime.bdrs.server.url | string | `nil` | URL of the BPN/DID Resolution Service |
| runtime.catalog | object | `{"crawler":{"initialDelay":null,"num":null,"period":null,"targetsFile":null},"enabled":false}` | configuration for the built-in federated catalog crawler |
| runtime.catalog.crawler.initialDelay | string | `nil` | Initial delay for the crawling to start. Leave blank for a random delay |
| runtime.catalog.crawler.num | string | `nil` | Number of desired crawlers. Final number might be different, based on number of crawl targets |
| runtime.catalog.crawler.period | string | `nil` | Period between two crawl runs in seconds. Default is 60 seconds. |
| runtime.catalog.crawler.targetsFile | string | `nil` | File path to a JSON file containing TargetNode entries |
| runtime.catalog.enabled | bool | `false` | Flag to globally enable/disable the FC feature |
| runtime.debug.enabled | bool | `false` | Enables java debugging mode. |
| runtime.debug.port | int | `1044` | Port where the debuggee can connect to. |
| runtime.debug.suspendOnStart | bool | `false` | Defines if the JVM should wait with starting the application until someone connected to the debugging port. |
| runtime.endpoints | object | `{"catalog":{"authKey":"password","path":"/catalog","port":8085},"control":{"path":"/control","port":8083},"default":{"path":"/api","port":8080},"management":{"authKey":"password","jwksUrl":null,"path":"/management","port":8081},"protocol":{"path":"/api/v1/dsp","port":8084},"proxy":{"authKey":"password","path":"/proxy","port":8186},"public":{"path":"/api/public","port":8086}}` | endpoints of the controlplane |
| runtime.endpoints.catalog.authKey | string | `"password"` | authentication key, must be attached to each request as `X-Api-Key` header |
| runtime.endpoints.catalog.path | string | `"/catalog"` | path for incoming catalog cache query requests |
| runtime.endpoints.catalog.port | int | `8085` | port for incoming catalog cache query requests |
| runtime.endpoints.control | object | `{"path":"/control","port":8083}` | control api, used for internal control calls. can be added to the internal ingress, but should probably not |
| runtime.endpoints.control.path | string | `"/control"` | path for incoming api calls |
| runtime.endpoints.control.port | int | `8083` | port for incoming api calls |
| runtime.endpoints.default | object | `{"path":"/api","port":8080}` | default api for health checks, should not be added to any ingress |
| runtime.endpoints.default.path | string | `"/api"` | path for incoming api calls |
| runtime.endpoints.default.port | int | `8080` | port for incoming api calls |
| runtime.endpoints.management | object | `{"authKey":"password","jwksUrl":null,"path":"/management","port":8081}` | data management api, used by internal users, can be added to an ingress and must not be internet facing |
| runtime.endpoints.management.authKey | string | `"password"` | authentication key, must be attached to each request as `X-Api-Key` header |
| runtime.endpoints.management.jwksUrl | string | `nil` | if the JWKS url is set, the DelegatedAuth service will be engaged |
| runtime.endpoints.management.path | string | `"/management"` | path for incoming api calls |
| runtime.endpoints.management.port | int | `8081` | port for incoming api calls |
| runtime.endpoints.protocol | object | `{"path":"/api/v1/dsp","port":8084}` | dsp api, used for inter connector communication and must be internet facing |
| runtime.endpoints.protocol.path | string | `"/api/v1/dsp"` | path for incoming api calls |
| runtime.endpoints.protocol.port | int | `8084` | port for incoming api calls |
| runtime.endpoints.proxy | object | `{"authKey":"password","path":"/proxy","port":8186}` | proxy API |
| runtime.endpoints.proxy.authKey | string | `"password"` | authentication key, must be attached to each request as `X-Api-Key` header |
| runtime.endpoints.proxy.path | string | `"/proxy"` | path for incoming api calls |
| runtime.endpoints.proxy.port | int | `8186` | port for incoming api calls |
| runtime.endpoints.public | object | `{"path":"/api/public","port":8086}` | public endpoint where the data can be fetched from if HttpPull was used. Must be internet facing. |
| runtime.endpoints.public.path | string | `"/api/public"` | path for incoming api calls |
| runtime.endpoints.public.port | int | `8086` | port for incoming api calls |
| runtime.env | object | `{}` |  |
| runtime.envConfigMapNames | list | `[]` | [Kubernetes ConfigMap Resource](https://kubernetes.io/docs/concepts/configuration/configmap/) names to load environment variables from |
| runtime.envSecretNames | list | `[]` | [Kubernetes Secret Resource](https://kubernetes.io/docs/concepts/configuration/secret/) names to load environment variables from |
| runtime.envValueFrom | object | `{}` | "valueFrom" environment variable references that will be added to deployment pods. Name is templated. ref: https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.19/#envvarsource-v1-core |
| runtime.image.pullPolicy | string | `"IfNotPresent"` | [Kubernetes image pull policy](https://kubernetes.io/docs/concepts/containers/images/#image-pull-policy) to use |
| runtime.image.repository | string | `""` | Which derivate of the control plane to use. When left empty the deployment will select the correct image automatically |
| runtime.image.tag | string | `""` | Overrides the image tag whose default is the chart appVersion |
| runtime.ingresses[0].annotations | object | `{}` | Additional ingress annotations to add |
| runtime.ingresses[0].certManager.clusterIssuer | string | `""` | If preset enables certificate generation via cert-manager cluster-wide issuer |
| runtime.ingresses[0].certManager.issuer | string | `""` | If preset enables certificate generation via cert-manager namespace scoped issuer |
| runtime.ingresses[0].className | string | `""` | Defines the [ingress class](https://kubernetes.io/docs/concepts/services-networking/ingress/#ingress-class)  to use |
| runtime.ingresses[0].enabled | bool | `false` |  |
| runtime.ingresses[0].endpoints | list | `["protocol","public"]` | EDC endpoints exposed by this ingress resource |
| runtime.ingresses[0].hostname | string | `"edc-control.local"` | The hostname to be used to precisely map incoming traffic onto the underlying network service |
| runtime.ingresses[0].tls | object | `{"enabled":false,"secretName":""}` | TLS [tls class](https://kubernetes.io/docs/concepts/services-networking/ingress/#tls) applied to the ingress resource |
| runtime.ingresses[0].tls.enabled | bool | `false` | Enables TLS on the ingress resource |
| runtime.ingresses[0].tls.secretName | string | `""` | If present overwrites the default secret name |
| runtime.ingresses[1].annotations | object | `{}` | Additional ingress annotations to add |
| runtime.ingresses[1].certManager.clusterIssuer | string | `""` | If preset enables certificate generation via cert-manager cluster-wide issuer |
| runtime.ingresses[1].certManager.issuer | string | `""` | If preset enables certificate generation via cert-manager namespace scoped issuer |
| runtime.ingresses[1].className | string | `""` | Defines the [ingress class](https://kubernetes.io/docs/concepts/services-networking/ingress/#ingress-class)  to use |
| runtime.ingresses[1].enabled | bool | `false` |  |
| runtime.ingresses[1].endpoints | list | `["management","control"]` | EDC endpoints exposed by this ingress resource |
| runtime.ingresses[1].hostname | string | `"edc-control.intranet"` | The hostname to be used to precisely map incoming traffic onto the underlying network service |
| runtime.ingresses[1].tls | object | `{"enabled":false,"secretName":""}` | TLS [tls class](https://kubernetes.io/docs/concepts/services-networking/ingress/#tls) applied to the ingress resource |
| runtime.ingresses[1].tls.enabled | bool | `false` | Enables TLS on the ingress resource |
| runtime.ingresses[1].tls.secretName | string | `""` | If present overwrites the default secret name |
| runtime.initContainers | list | `[]` |  |
| runtime.livenessProbe.enabled | bool | `true` | Whether to enable kubernetes [liveness-probe](https://kubernetes.io/docs/tasks/configure-pod-container/configure-liveness-readiness-startup-probes/) |
| runtime.livenessProbe.failureThreshold | int | `6` | when a probe fails kubernetes will try 6 times before giving up |
| runtime.livenessProbe.initialDelaySeconds | int | `30` | seconds to wait before performing the first liveness check |
| runtime.livenessProbe.periodSeconds | int | `10` | this fields specifies that kubernetes should perform a liveness check every 10 seconds |
| runtime.livenessProbe.successThreshold | int | `1` | number of consecutive successes for the probe to be considered successful after having failed |
| runtime.livenessProbe.timeoutSeconds | int | `5` | number of seconds after which the probe times out |
| runtime.logs.level | string | `"DEBUG"` | Defines the log granularity of the default Console Monitor. |
| runtime.nodeSelector | object | `{}` | [node selector](https://kubernetes.io/docs/concepts/scheduling-eviction/assign-pod-node/#nodeselector) to constrain pods to nodes |
| runtime.podAnnotations | object | `{}` | additional annotations for the pod |
| runtime.podLabels | object | `{}` | additional labels for the pod |
| runtime.podSecurityContext | object | `{"fsGroup":10001,"runAsGroup":10001,"runAsUser":10001,"seccompProfile":{"type":"RuntimeDefault"}}` | The [pod security context](https://kubernetes.io/docs/tasks/configure-pod-container/security-context/#set-the-security-context-for-a-pod) defines privilege and access control settings for a Pod within the deployment |
| runtime.podSecurityContext.fsGroup | int | `10001` | The owner for volumes and any files created within volumes will belong to this guid |
| runtime.podSecurityContext.runAsGroup | int | `10001` | Processes within a pod will belong to this guid |
| runtime.podSecurityContext.runAsUser | int | `10001` | Runs all processes within a pod with a special uid |
| runtime.podSecurityContext.seccompProfile.type | string | `"RuntimeDefault"` | Restrict a Container's Syscalls with seccomp |
| runtime.policy | object | `{"validation":{"enabled":true}}` | configuration for policy engine |
| runtime.readinessProbe.enabled | bool | `true` | Whether to enable kubernetes [readiness-probes](https://kubernetes.io/docs/tasks/configure-pod-container/configure-liveness-readiness-startup-probes/) |
| runtime.readinessProbe.failureThreshold | int | `6` | when a probe fails kubernetes will try 6 times before giving up |
| runtime.readinessProbe.initialDelaySeconds | int | `30` | seconds to wait before performing the first readiness check |
| runtime.readinessProbe.periodSeconds | int | `10` | this fields specifies that kubernetes should perform a readiness check every 10 seconds |
| runtime.readinessProbe.successThreshold | int | `1` | number of consecutive successes for the probe to be considered successful after having failed |
| runtime.readinessProbe.timeoutSeconds | int | `5` | number of seconds after which the probe times out |
| runtime.replicaCount | int | `1` |  |
| runtime.resources | object | `{"limits":{"cpu":1.5,"memory":"1024Mi"},"requests":{"cpu":"500m","memory":"1024Mi"}}` | [resource management](https://kubernetes.io/docs/concepts/configuration/manage-resources-containers/) for the container |
| runtime.resources.limits.cpu | float | `1.5` | Maximum CPU limit |
| runtime.resources.limits.memory | string | `"1024Mi"` | Maximum memory limit |
| runtime.resources.requests.cpu | string | `"500m"` | Initial CPU request |
| runtime.resources.requests.memory | string | `"1024Mi"` | Initial memory request |
| runtime.securityContext.allowPrivilegeEscalation | bool | `false` | Controls [Privilege Escalation](https://kubernetes.io/docs/concepts/security/pod-security-policy/#privilege-escalation) enabling setuid binaries changing the effective user ID |
| runtime.securityContext.capabilities.add | list | `[]` | Specifies which capabilities to add to issue specialized syscalls |
| runtime.securityContext.capabilities.drop | list | `["ALL"]` | Specifies which capabilities to drop to reduce syscall attack surface |
| runtime.securityContext.readOnlyRootFilesystem | bool | `true` | Whether the root filesystem is mounted in read-only mode |
| runtime.securityContext.runAsNonRoot | bool | `true` | Requires the container to run without root privileges |
| runtime.securityContext.runAsUser | int | `10001` | The container's process will run with the specified uid |
| runtime.service.annotations | object | `{}` | additional annotations for the service |
| runtime.service.labels | object | `{}` | additional labels for the service |
| runtime.service.type | string | `"ClusterIP"` | [Service type](https://kubernetes.io/docs/concepts/services-networking/service/#publishing-services-service-types) to expose the running application on a set of Pods as a network service. |
| runtime.token.refresh.expiry_seconds | int | `300` | TTL in seconds for access tokens (also known as EDR token) |
| runtime.token.refresh.expiry_tolerance_seconds | int | `10` | Tolerance for token expiry in seconds |
| runtime.token.refresh.refresh_endpoint | string | `nil` | Optional endpoint for an OAuth2 token refresh. Default endpoint is `<PUBLIC_API>/token` |
| runtime.token.signer.privatekey_alias | string | `nil` | Alias under which the private key (JWK or PEM format) is stored in the vault |
| runtime.token.verifier.publickey_alias | string | `nil` | Alias under which the public key (JWK or PEM format) is stored in the vault, that belongs to the private key which was referred to at `dataplane.token.signer.privatekey_alias` |
| runtime.tolerations | list | `[]` | [tolerations](https://kubernetes.io/docs/concepts/scheduling-eviction/taint-and-toleration/) to configure preferred nodes |
| runtime.url.protocol | string | `""` | Explicitly declared url for reaching the dsp api (e.g. if ingresses not used) |
| runtime.url.public | string | `""` | Explicitly declared url for reaching the public api (e.g. if ingresses not used) |
| runtime.volumeMounts | list | `[]` | declare where to mount [volumes](https://kubernetes.io/docs/concepts/storage/volumes/) into the container |
| runtime.volumes | list | `[]` | [volume](https://kubernetes.io/docs/concepts/storage/volumes/) directories |
| serviceAccount.annotations | object | `{}` | Annotations to add to the service account |
| serviceAccount.create | bool | `true` | Specifies whether a service account should be created |
| serviceAccount.imagePullSecrets | list | `[]` | Existing image pull secret bound to the service account to use to [obtain the container image from private registries](https://kubernetes.io/docs/concepts/containers/images/#using-a-private-registry) |
| serviceAccount.name | string | `""` | The name of the service account to use. If not set and create is true, a name is generated using the fullname template |
| tests | object | `{"hookDeletePolicy":"before-hook-creation,hook-succeeded"}` | Configurations for Helm tests |
| tests.hookDeletePolicy | string | `"before-hook-creation,hook-succeeded"` | Configure the hook-delete-policy for Helm tests |
| vault.secrets | string | `""` |  |
| vault.server.postStart | string | `""` |  |

----------------------------------------------
Autogenerated from chart metadata using [helm-docs](https://github.com/norwoodj/helm-docs/)
