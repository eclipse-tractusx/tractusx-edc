{{/*
Expand the name of the chart.
*/}}
{{- define "txdc.name" -}}
{{- default .Chart.Name .Values.nameOverride | replace "+" "_"  | trunc 63 | trimSuffix "-" -}}
{{- end }}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "txdc.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride }}
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
{{- define "txdc.labels" -}}
helm.sh/chart: {{ include "txdc.chart" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Control Common labels
*/}}
{{- define "txdc.controlplane.labels" -}}
helm.sh/chart: {{ include "txdc.chart" . }}
{{ include "txdc.controlplane.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
app.kubernetes.io/component: edc-controlplane
app.kubernetes.io/part-of: edc
{{- end }}

{{/*
Data Common labels
*/}}
{{- define "txdc.dataplane.labels" -}}
helm.sh/chart: {{ include "txdc.chart" . }}
{{ include "txdc.dataplane.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
app.kubernetes.io/component: edc-dataplane
app.kubernetes.io/part-of: edc
{{- end }}

{{/*
Control Selector labels
*/}}
{{- define "txdc.controlplane.selectorLabels" -}}
app.kubernetes.io/name: {{ include "txdc.name" . }}-controlplane
app.kubernetes.io/instance: {{ .Release.Name }}-controlplane
{{- end }}

{{/*
Data Selector labels
*/}}
{{- define "txdc.dataplane.selectorLabels" -}}
app.kubernetes.io/name: {{ include "txdc.name" . }}-dataplane
app.kubernetes.io/instance: {{ .Release.Name }}-dataplane
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "txdc.controlplane.serviceaccount.name" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "txdc.fullname" . ) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "txdc.dataplane.serviceaccount.name" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "txdc.fullname" . ) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}

{{/*
Control IDS URL
*/}}
{{- define "txdc.controlplane.url.ids" -}}
{{- if .Values.controlplane.url.ids }}{{/* if ids api url has been specified explicitly */}}
{{- .Values.controlplane.url.ids }}
{{- else }}{{/* else when ids api url has not been specified explicitly */}}
{{- with (index .Values.controlplane.ingresses 0) }}
{{- if .enabled }}{{/* if ingress enabled */}}
{{- if .tls.enabled }}{{/* if TLS enabled */}}
{{- printf "https://%s" .hostname -}}
{{- else }}{{/* else when TLS not enabled */}}
{{- printf "http://%s" .hostname -}}
{{- end }}{{/* end if tls */}}
{{- else }}{{/* else when ingress not enabled */}}
{{- printf "http://%s-controlplane:%v" ( include "txdc.fullname" $ ) $.Values.controlplane.endpoints.ids.port -}}
{{- end }}{{/* end if ingress */}}
{{- end }}{{/* end with ingress */}}
{{- end }}{{/* end if .Values.controlplane.url.ids */}}
{{- end }}

{{/*
Validation URL
*/}}
{{- define "txdc.controlplane.url.validation" -}}
{{- printf "http://%s-controlplane:%v%s/token" ( include "txdc.fullname" $ ) $.Values.controlplane.endpoints.validation.port $.Values.controlplane.endpoints.validation.path -}}
{{- end }}

{{/*
Data Control URL
*/}}
{{- define "txdc.dataplane.url.control" -}}
{{- printf "http://%s-dataplane:%v%s" (include "txdc.fullname" . ) .Values.dataplane.endpoints.control.port .Values.dataplane.endpoints.control.path -}}
{{- end }}

{{/*
Data Public URL
*/}}
{{- define "txdc.dataplane.url.public" -}}
{{- if .Values.dataplane.url.public }}{{/* if public api url has been specified explicitly */}}
{{- .Values.dataplane.url.public }}
{{- else }}{{/* else when public api url has not been specified explicitly */}}
{{- with (index  .Values.dataplane.ingresses 0) }}
{{- if .enabled }}{{/* if ingress enabled */}}
{{- if .tls.enabled }}{{/* if TLS enabled */}}
{{- printf "https://%s%s" .hostname $.Values.dataplane.endpoints.public.path -}}
{{- else }}{{/* else when TLS not enabled */}}
{{- printf "http://%s%s" .hostname $.Values.dataplane.endpoints.public.path -}}
{{- end }}{{/* end if tls */}}
{{- else }}{{/* else when ingress not enabled */}}
{{- printf "http://%s-dataplane:%v%s" (include "txdc.fullname" $ ) $.Values.dataplane.endpoints.public.port $.Values.dataplane.endpoints.public.path -}}
{{- end }}{{/* end if ingress */}}
{{- end }}{{/* end with ingress */}}
{{- end }}{{/* end if .Values.dataplane.url.public */}}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "txdc.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "txdc.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}
