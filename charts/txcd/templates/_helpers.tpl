{{- define "control-default" -}}
{{ printf "%s-%s" .Chart.Name "controlplane" -}}
{{ end }}

{{- define "data-default" -}}
{{ printf "%s-%s" .Chart.Name "dataplane" -}}
{{ end }}

{{/*
Expand the name of the chart.
*/}}
{{- define "txdc.name" -}}
{{- printf "%s-%s" .Chart.Name "txdc" | replace "+" "_"  | trunc 63 | trimSuffix "-" -}}
{{- end }}

{{/*
Expand the name of the chart.
*/}}
{{- define "txdc.serviceaccount.name" -}}
{{- printf "%s-%s" (include "txdc.name" . ) "serviceaccount" | replace "+" "_"  | trunc 63 | trimSuffix "-" -}}
{{- end }}

{{/*
Expand the name of the chart.
*/}}
{{- define "txdc.controlplane.name" -}}
{{- default (include "data-default" . ) .Values.controlplane.nameOverride | replace "+" "_"  | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Expand the name of the chart.
*/}}
{{- define "txdc.dataplane.name" -}}
{{- default (include "control-default" . ) .Values.dataplane.nameOverride | replace "+" "_"  | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "txdc.controlplane.fullname" -}}
{{- if .Values.controlplane.fullnameOverride }}
{{- .Values.controlplane.fullnameOverride | replace "+" "_"  | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default (include "control-default" . ) .Values.controlplane.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | replace "+" "_"  | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | replace "+" "_"  | trunc 63 | trimSuffix "-" -}}
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
{{- .Values.dataplane.fullnameOverride | replace "+" "_"  | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $dataDefault := ( printf "%s-%s" .Chart.Name "data") }}
{{- $name := default $dataDefault .Values.dataplane.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | replace "+" "_"  | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | replace "+" "_"  | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "txdc.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | replace "+" "_"  | trunc 63 | trimSuffix "-" }}
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
{{- end }}

{{/*
Control Selector labels
*/}}
{{- define "txdc.controlplane.selectorLabels" -}}
app.kubernetes.io/name: {{ include "txdc.controlplane.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- if .Values.controlplane.podLabels }}
{{ .Values.controlplane.podLabels | toYaml }}
{{- end }}
{{- end }}

{{/*
Data Selector labels
*/}}
{{- define "txdc.dataplane.selectorLabels" -}}
app.kubernetes.io/name: {{ include "txdc.dataplane.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- if .Values.dataplane.podLabels }}
{{ .Values.dataplane.podLabels | toYaml }}
{{- end }}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "txdc.controlplane.serviceaccount.name" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "txdc.controlplane.fullname" . ) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "txdc.dataplane.serviceaccount.name" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "txdc.dataplane.fullname" . ) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}

{{/*
Control IDS URL
*/}}
{{- define "txdc.controlplane.url.ids" -}}
{{- with (index .Values.controlplane.ingresses 0) }}
{{- if .enabled }}{{/* if ingress enabled */}}
{{- if .tls.enabled }}{{/* if TLS enabled */}}
{{- printf "https://%s%s" .hostname $.Values.controlplane.endpoints.ids.path -}}
{{- else }}{{/* else when TLS not enabled */}}
{{- printf "http://%s%s" .hostname $.Values.controlplane.endpoints.ids.path -}}
{{- end }}{{/* end if tls */}}
{{- else }}{{/* else when ingress not enabled */}}
{{- printf "http://%s:%v%s" ( include "txdc.controlplane.fullname" $ ) $.Values.controlplane.endpoints.ids.port $.Values.controlplane.endpoints.ids.path -}}
{{- end }}{{/* end if ingress */}}
{{- end }}{{/* end with ingress */}}
{{- end }}

{{/*
Data Control URL
*/}}
{{- define "txdc.dataplane.url.control" -}}
{{- printf "http://%s:%v%s" (include "txdc.dataplane.fullname" . ) .Values.dataplane.endpoints.control.port .Values.dataplane.endpoints.control.path -}}
{{- end }}

{{/*
Data Public URL
*/}}
{{- define "txdc.dataplane.url.public" -}}
{{- with (index  .Values.dataplane.ingresses 0) }}
{{- if .enabled }}{{/* if ingress enabled */}}
{{- if .tls.enabled }}{{/* if TLS enabled */}}
{{- printf "https://%s%s" .hostname $.Values.dataplane.endpoints.public.path -}}
{{- else }}{{/* else when TLS not enabled */}}
{{- printf "http://%s%s" .hostname $.Values.dataplane.endpoints.public.path -}}
{{- end }}{{/* end if tls */}}
{{- else }}{{/* else when ingress not enabled */}}
{{- printf "http://%s:%v%s" (include "txdc.dataplane.fullname" $ ) $.Values.dataplane.endpoints.public.port $.Values.dataplane.endpoints.public.path -}}
{{- end }}{{/* end if ingress */}}
{{- end }}{{/* end with ingress */}}
{{- end }}

{{/*
PostgreSQL Password
*/}}
{{- define "txdc.postgresql.password" -}}
{{- if .Values.postgresql.auth.password }}
{{- print .Values.postgresql.auth.password | quote }}
{{- else }}
{{ print ""}}
{{- end }}
{{- end }}

{{/*
PostgreSQL User
*/}}
{{- define "txdc.postgresql.username" -}}
{{- if .Values.postgresql.auth.username }}
{{- print .Values.postgresql.auth.username | quote }}
{{- else }}
{{ print ""}}
{{- end }}
{{- end }}

{{/*
PostgreSQL Connection Host
*/}}
{{- define "txdc.postgresql.host" -}}
{{- if .Values.postgresql.host }}
{{ print .Values.postgresql.host }}
{{- else }}
{{- printf "%s-%s" .Release.Name "postgresql" }}
{{- end }}
{{- end }}

{{/*
PostgreSQL Connection Host
*/}}
{{- define "txdc.postgresql.port" -}}
{{- if .Values.postgresql.containerPorts.postgresql }}
{{- print .Values.postgresql.containerPorts.postgresql }}
{{- else }}
{{ print "5432" }}
{{- end }}
{{- end }}

{{/*
PostgreSQL Connection Host
*/}}
{{- define "txdc.postgresql.database" -}}
{{- if .Values.postgresql.auth.database }}
{{- print .Values.postgresql.auth.database }}
{{- else }}
{{ print "postgres" }}
{{- end }}
{{- end }}

{{/*
PostgreSQL Connection String
{{ printf "jdbc:postgresql://%s:%s/%s" .Values.postgresql.host .Values.postgresql.port .Values.postgresql.database }}
*/}}
{{- define "txdc.postgresql.jdbcUrl" -}}
{{ printf "jdbc:postgresql://%s:%v/%s" (include "txdc.postgresql.host" . ) (include "txdc.postgresql.port" . ) (include "txdc.postgresql.database" . ) }}
{{- end }}


