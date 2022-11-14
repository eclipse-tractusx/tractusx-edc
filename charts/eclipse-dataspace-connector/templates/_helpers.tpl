{{- $controlDefault := .Chart.Name "-control"  }}
{{- $dataDefault := .Chart.Name "-data"  }}

{{/*
Expand the name of the chart.
*/}}
{{- define "txdc.name" -}}
{{- (printf "%s-%s" .Chart.Name "txdc") | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Expand the name of the chart.
*/}}
{{- define "txdc.controlplane.name" -}}
{{- $controlDefault := (printf "%s-%s" .Chart.Name "control") }}
{{- default $controlDefault .Values.controlplaneplane.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Expand the name of the chart.
*/}}
{{- define "txdc.dataplane.name" -}}
{{- $dataDefault := (printf "%s-%s" .Chart.Name "data") }}
{{- default $dataDefault .Values.dataplaneplane.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "txdc.controlplane.fullname" -}}
{{- if .Values.controlplane.fullnameOverride }}
{{- .Values.controlplane.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $controlDefault := (printf "%s-%s" .Chart.Name "control") }}
{{- $name := default $controlDefault .Values.controlplaneplane.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "txdc.dataplane.fullname" -}}
{{- if .Values.dataplane.fullnameOverride }}
{{- .Values.dataplane.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $dataDefault := ( printf "%s-%s" .Chart.Name "data") }}
{{- $name := default $dataDefault .Values.dataplaneplane.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "txdc.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Control Common labels
*/}}
{{- define "txdc.controlplane.labels" -}}
helm.sh/chart: {{ include "txdc.chart" . }}
{{ include "txdc.controlplane.selectorLabels" . }}
{{- with .Values.customLabels }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Data Common labels
*/}}
{{- define "txdc.dataplane.labels" -}}
helm.sh/chart: {{ include "txdc.chart" . }}
{{ include "txdc.dataplane.selectorLabels" . }}
{{- with .Values.customLabels }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Control Selector labels
*/}}
{{- define "txdc.controlplane.selectorLabels" -}}
app.kubernetes.io/name: {{ include "txdc.controlplane.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Data Selector labels
*/}}
{{- define "txdc.dataplane.selectorLabels" -}}
app.kubernetes.io/name: {{ include "txdc.dataplane.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "txdc.controlplane.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "txdc.controlplane.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "txdc.dataplane.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "txdc.dataplane.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}

{{/*
Control IDS URL
*/}}
{{- define "txdc.controlplane.url.ids" -}}
{{- with (index  .Values.controlplaneplane.ingresses 0) }}
{{- if .enabled }} # if ingress enabled
{{- if .tls.enabled }} # if TLS enabled
{{ printf "https://%s/%s" .hostname .Values.controlplaneplane.endpoints.ids.path }}
{{- else }} # else when TLS not enabled
{{ printf "http://%s/%s" .hostname .Values.controlplane.endpoints.ids.path }}
{{- end }} # end if tls
{{- else }} # else when ingress not enabled
{{ printf "http://%s:%s/%s" (include "txdc.controlplane.fullname") .Values.controlplane.endpoints.ids.port .Values.controlplane.endpoints.ids.path }}
{{- end }} # end if ingress
{{- end }} # end with ingress
{{- end }}

{{/*
Data Control URL
*/}}
{{- define "txdc.dataplane.url.control" -}}
{{ printf "http://%s:%s/%s" (include "txdc.dataplane.fullname") .Values.dataplane.endpoints.control.port .Values.dataplane.endpoints.control.path }}
{{- end }}

{{/*
Data Public URL
*/}}
{{- define "txdc.dataplane.url.public" -}}
{{- with (index  .Values.dataplane.ingresses 0) }}
{{- if .enabled }} # if ingress enabled
{{- if .tls.enabled }} # if TLS enabled
{{ printf "https://%s/%s" .hostname .Values.dataplane.endpoints.public.path }}
{{- else }} # else when TLS not enabled
{{ printf "http://%s/%s" .hostname .Values.dataplane.endpoints.public.path }}
{{- end }} # end if tls
{{- else }} # else when ingress not enabled
{{ printf "http://%s:%s/%s" (include "txdc.dataplane.fullname") .Values.dataplane.endpoints.public.port .Values.dataplane.endpoints.public.path }}
{{- end }} # end if ingress
{{- end }} # end with ingress
{{- end }}
