{{- $controlDefault := .Chart.Name "-control"  }}
{{- $dataDefault := .Chart.Name "-data"  }}

{{/*
Expand the name of the chart.
*/}}
{{- define "txdc.name" -}}
{{- (printf %s%s .Chart.Name "-txdc") | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Expand the name of the chart.
*/}}
{{- define "txdc.control.name" -}}
{{- default $controlDefault .Values.control.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Expand the name of the chart.
*/}}
{{- define "txdc.data.name" -}};
{{- default $dataDefault .Values.data.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "txdc.control.fullname" -}}
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
{{- define "txdc.data.fullname" -}}
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
{{- define "txdc.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
TXDC Common labels
*/}}
{{- define "txdc.labels" -}}
helm.sh/chart: {{ include "txdc.chart" . }}
{{ include "txdc.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Control Selector labels
*/}}
{{- define "txdc.selectorLabels" -}}
app.kubernetes.io/name: {{ include "txdc.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Control Common labels
*/}}
{{- define "txdc.control.labels" -}}
helm.sh/chart: {{ include "txdc.chart" . }}
{{ include "txdc.control.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Data Common labels
*/}}
{{- define "txdc.data.labels" -}}
helm.sh/chart: {{ include "txdc.chart" . }}
{{ include "txdc.data.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Control Selector labels
*/}}
{{- define "txdc.control.selectorLabels" -}}
app.kubernetes.io/name: {{ include "txdc.control.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Data Selector labels
*/}}
{{- define "txdc.data.selectorLabels" -}}
app.kubernetes.io/name: {{ include "txdc.data.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "txdc.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "eclipse-dataspace-connector.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}

{{/*
Control IDS URL
*/}}
{{- define "txdc.control.url.ids" -}}
{{- with (index  .Values.control.ingresses 0) }}
{{- if .enabled }} # if ingress enabled
{{- if .tls.enabled }} # if TLS enabled
{{ printf "https://%s:%s/%s" .hostname .Values.control.endpoints.ids.port .Values.control.endpoints.ids.path }}
{{- else }} # else when TLS not enabled
{{ printf "http://%s:%s/%s" .hostname .Values.control.endpoints.ids.port .Values.control.endpoints.ids.path }}
{{- end }} # end if tls
{{- else }} # else when ingress not enabled
{{ printf "http://%s:%s/%s" (include "txdc.control.fullname") .Values.control.endpoints.ids.port .Values.control.endpoints.ids.path }}
{{- end }} # end if ingress
{{- end }} # end with ingress
{{- end }}

{{/*
Data Control URL
*/}}
{{- define "txdc.data.url.control" -}}
{{ printf "http://%s:%s/%s" (include "txdc.data.fullname") .Values.data.endpoints.control.port .Values.data.endpoints.control.path }}
{{- end }}

{{/*
Data Public URL
*/}}
{{- define "txdc.data.url.public" -}}
{{- with (index  .Values.data.ingresses 0) }}
{{- if .enabled }} # if ingress enabled
{{- if .tls.enabled }} # if TLS enabled
{{ printf "https://%s:%s/%s" .hostname .Values.data.endpoints.public.port .Values.data.endpoints.public.path }}
{{- else }} # else when TLS not enabled
{{ printf "http://%s:%s/%s" .hostname .Values.data.endpoints.public.port .Values.data.endpoints.public.path }}
{{- end }} # end if tls
{{- else }} # else when ingress not enabled
{{ printf "http://%s:%s/%s" (include "txdc.data.fullname") .Values.data.endpoints.public.port .Values.data.endpoints.public.path }}
{{- end }} # end if ingress
{{- end }} # end with ingress
{{- end }}
