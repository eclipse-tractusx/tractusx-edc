# backend-application

![Version: 0.0.1](https://img.shields.io/badge/Version-0.0.1-informational?style=flat-square) ![Type: application](https://img.shields.io/badge/Type-application-informational?style=flat-square) ![AppVersion: 0.0.1](https://img.shields.io/badge/AppVersion-0.0.1-informational?style=flat-square)

The Eclipse Dataspace Connector requires the Backend Application to transfer data using the HTTP-TransferMethod.

## Values

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| affinity | object | `{}` | [Affinity](https://kubernetes.io/docs/concepts/scheduling-eviction/assign-pod-node/#affinity-and-anti-affinity) constrains which nodes the Pod can be scheduled on based on node labels. |
| automountServiceAccountToken | bool | `false` | Whether to [automount kubernetes API credentials](https://kubernetes.io/docs/tasks/configure-pod-container/configure-service-account/#use-the-default-service-account-to-access-the-api-server) into the pod |
| autoscaling.enabled | bool | `false` | Enables [horizontal pod autoscaling](https://kubernetes.io/docs/tasks/run-application/horizontal-pod-autoscale/https://kubernetes.io/docs/tasks/run-application/horizontal-pod-autoscale/) |
| autoscaling.maxReplicas | int | `100` | Maximum replicas if resource consumption exceeds resource threshholds |
| autoscaling.minReplicas | int | `1` | Minimal replicas if resource consumption falls below resource threshholds |
| autoscaling.targetCPUUtilizationPercentage | int | `80` | targetAverageUtilization of cpu provided to a pod |
| autoscaling.targetMemoryUtilizationPercentage | int | `80` | targetAverageUtilization of memory provided to a pod |
| container.port | int | `8080` |  |
| fullnameOverride | string | `""` | Overrides the releases full name |
| image.command[0] | string | `"/bin/bash"` |  |
| image.command[1] | string | `"-c"` |  |
| image.command[2] | string | `"apt-get update && apt-get install -y ucspi-tcp curl jq && rm -rf /var/lib/apt/lists/* && tcpserver -v 0.0.0.0 \"${TCP_SERVER_PORT}\" \"${TCP_SERVER_SCRIPT_PATH}\""` |  |
| image.pullPolicy | string | `"IfNotPresent"` | [Kubernetes image pull policy](https://kubernetes.io/docs/concepts/containers/images/#image-pull-policy) to use |
| image.repository | string | `"ubuntu"` | Which container image to use |
| image.tag | string | `"22.04"` | Overrides the image tag whose default is the chart appVersion |
| imagePullSecrets | list | `[]` | Image pull secret to create to [obtain the container image from private registries](https://kubernetes.io/docs/concepts/containers/images/#using-a-private-registry) |
| livenessProbe | object | `{"exec":{"command":["/bin/bash","-c","/usr/bin/ps -ef | grep tcpserver | grep -v grep"]},"initialDelaySeconds":10,"periodSeconds":10}` | [Liveness-Probe](https://kubernetes.io/docs/tasks/configure-pod-container/configure-liveness-readiness-startup-probes/#define-a-liveness-command) to detect and remedy broken applications |
| livenessProbe.exec | object | `{"command":["/bin/bash","-c","/usr/bin/ps -ef | grep tcpserver | grep -v grep"]}` | exec command for liveness check |
| livenessProbe.initialDelaySeconds | int | `10` | initialDelaySeconds before performing the first probe |
| livenessProbe.periodSeconds | int | `10` | periodSeconds between each probe |
| nameOverride | string | `""` | Overrides the charts name |
| nodeSelector | object | `{}` | [Node-Selector](https://kubernetes.io/docs/concepts/scheduling-eviction/assign-pod-node/#nodeselector) to constrain the Pod to nodes with specific labels. |
| persistence.accessMode | string | `nil` | [PersistentVolume Access Modes](https://kubernetes.io/docs/concepts/storage/persistent-volumes/#access-modes) Access mode to use. One of (ReadOnlyMany, ReadWriteOnce, ReadWriteMany, ReadWriteOncePod) |
| persistence.capacity | string | `"100M"` | Capacity given to the claimed [PersistentVolume](https://kubernetes.io/docs/concepts/storage/persistent-volumes/) |
| persistence.enabled | bool | `false` | Whether to enable persistence via [PersistentVolumeClaim](https://kubernetes.io/docs/concepts/storage/persistent-volumes/#reserving-a-persistentvolume) |
| persistence.storageClassName | string | `nil` | Storage class to use together with the claimed [PersistentVolume](https://kubernetes.io/docs/concepts/storage/persistent-volumes/) |
| podAnnotations | object | `{}` | [Annotations](https://kubernetes.io/docs/concepts/overview/working-with-objects/annotations/) added to deployed [pods](https://kubernetes.io/docs/concepts/workloads/pods/) |
| podSecurityContext | object | `{}` |  |
| readinessProbe | object | `{"exec":{"command":["/bin/bash","-c","/usr/bin/ps -ef | grep tcpserver | grep -v grep"]},"initialDelaySeconds":10,"periodSeconds":10}` | [Readiness-Probe](https://kubernetes.io/docs/tasks/configure-pod-container/configure-liveness-readiness-startup-probes/#define-readiness-probes) to detect ready applications to receive traffic |
| readinessProbe.exec | object | `{"command":["/bin/bash","-c","/usr/bin/ps -ef | grep tcpserver | grep -v grep"]}` | exec command for readiness check |
| readinessProbe.initialDelaySeconds | int | `10` | initialDelaySeconds before performing the first probe |
| readinessProbe.periodSeconds | int | `10` | periodSeconds between each probe |
| replicaCount | int | `1` |  |
| resources | object | `{}` | [Resource management](https://kubernetes.io/docs/concepts/configuration/manage-resources-containers/) applied to the deployed pod |
| script | object | `{"content":"#!/bin/bash\n\nPAYLOAD=\"\"\nPAYLOAD_INCOMING=0\nexport TMOUT=3.5\nwhile IFS= read -r LINE || [ \"$LINE\" ]; do\n  if [ $PAYLOAD_INCOMING -eq 1 ]; then\n    PAYLOAD=\"${PAYLOAD}${LINE}\"\n    break\n  fi\n\n  if [[ \"${#LINE}\" = \"1\" && \"$(printf \"%d\" \"'${LINE}\")\" = \"13\" ]]; then\n    PAYLOAD_INCOMING=1\n  fi\ndone\n\nif [ -z \"$PAYLOAD\" ]; then\n  echo -ne \"HTTP/1.1 400 Bad Request\\r\\nContent-Length: 2\\r\\nContent-Type: application/json\\r\\nConnection: close\\r\\n\\r\\n{}\"\n  exit 1\nfi\n\nENDPOINT=$(echo $PAYLOAD | jq -r '.endpoint')\nif [ -z \"$ENDPOINT\" ]; then\n  echo -ne \"HTTP/1.1 400 Bad Request\\r\\nContent-Length: 2\\r\\nContent-Type: application/json\\r\\nConnection: close\\r\\n\\r\\n{}\"\n  exit 1\nfi\n\nID=$(echo $PAYLOAD | jq -r '.id')\nAUTH_KEY=$(echo $PAYLOAD | jq -r '.authKey')\nAUTH_CODE=$(echo $PAYLOAD | jq -r '.authCode')\n\nmkdir -p /tmp/data/\necho \"${AUTH_KEY}: ${AUTH_CODE}\" >| header.txt\n\ncurl -L -H @header.txt -o \"/tmp/data/${ID}\" ${ENDPOINT}\nif [ ! $? -eq 0 ]; then\n  echo \"calling endpoint ($ENDPOINT) failed ($?)\" 1>&2\n  echo -ne \"HTTP/1.1 400 Bad Request\\r\\nContent-Length: 2\\r\\nContent-Type: application/json\\r\\nConnection: close\\r\\n\\r\\n{}\"\n  exit 1\nfi\n\necho -ne \"HTTP/1.1 200 OK\\r\\nContent-Length: 2\\r\\nContent-Type: application/json\\r\\nConnection: close\\r\\n\\r\\n{}\"","path":"/opt/tcpserver/handler.sh"}` | script invoked on http calls |
| securityContext | object | `{}` |  |
| service.port | int | `80` | [Service type](https://kubernetes.io/docs/concepts/services-networking/service/#defining-a-service) to expose the running application on a set of Pods as a network service. |
| service.type | string | `"ClusterIP"` | [Service type](https://kubernetes.io/docs/concepts/services-networking/service/#publishing-services-service-types) to expose the running application on a set of Pods as a network service. |
| serviceAccount.annotations | object | `{}` | [Annotations](https://kubernetes.io/docs/concepts/overview/working-with-objects/annotations/) to add to the service account |
| serviceAccount.create | bool | `true` | Specifies whether a [service account](https://kubernetes.io/docs/tasks/configure-pod-container/configure-service-account/) should be created per release |
| serviceAccount.name | string | `""` | The name of the service account to use. If not set and create is true, a name is generated using the release's fullname template |
| tolerations | list | `[]` | [Tolerations](https://kubernetes.io/docs/concepts/scheduling-eviction/taint-and-toleration/) are applied to Pods to schedule onto nodes with matching taints. |

----------------------------------------------
Autogenerated from chart metadata using [helm-docs v1.10.0](https://github.com/norwoodj/helm-docs/releases/v1.10.0)
