

{{- $controlDefault := .Chart.Name "-control"  }}
{{- $dataDefault := .Chart.Name "-data"  }}

{{/*
Expand the name of the chart.
*/}}
{{- define "control.name" -}}
{{- default $controlDefault .Values.control.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Expand the name of the chart.
*/}}
{{- define "data.name" -}};
{{- default $dataDefault .Values.data.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "control.fullname" -}}
{{- if .Values.control.fullnameOverride }}
{{- .Values.control.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default $controlDefault .Values.control.nameOverride }}
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
{{- define "data.fullname" -}}
{{- if .Values.data.fullnameOverride }}
{{- .Values.data.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default $dataDefault .Values.data.nameOverride }}
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
{{- define "eclipse-dataspace-connector.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "eclipse-dataspace-connector.labels" -}}
helm.sh/chart: {{ include "eclipse-dataspace-connector.chart" . }}
{{ include "eclipse-dataspace-connector.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Control Selector labels
*/}}
{{- define "eclipse-dataspace-connector.selectorLabels" -}}
app.kubernetes.io/name: {{ include "control.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}


{{/*
Data Selector labels
*/}}
{{- define "eclipse-dataspace-connector.selectorLabels" -}}
app.kubernetes.io/name: {{ include "data.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}


{{/*
Create the name of the service account to use
*/}}
{{- define "eclipse-dataspace-connector.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "eclipse-dataspace-connector.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}

{{/*
ids control host
*/}}
{{- define "control.host" -}}
{{- defaultHost:= printf "http://%s:%s" (include "control.fullname") .Values.control.endpoints.ids.port }}
{{- if .Values.control.host }}
{{- default $defaultHost .Values.control.host }}
{{- end }}
{{- end }}

{{/*
contral data host
*/}}
{{- define "data.host.control" -}}
{{- defaultHost:= printf "http://%s:%s" (include "control.fullname") .Values.data.endpoints.control.port }}
{{- if .Values.control.host }}
{{- default $defaultHost .Values.control.host }}
{{- end }}
{{- end }}