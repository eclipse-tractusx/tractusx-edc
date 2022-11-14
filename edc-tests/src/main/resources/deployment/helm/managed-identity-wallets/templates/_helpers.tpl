{{/*
Invoke include on given definition with postgresql dependency context
Usage: include "postgresContext" (list $ "your_include_function_here")
*/}}
{{- define "postgresContext" -}}
{{- $ := index . 0 }}
{{- $function := index . 1 }}
{{- include $function (dict "Values" $.Values.postgresql "Chart" (dict "Name" "postgresql") "Release" $.Release) }}
{{- end }}
