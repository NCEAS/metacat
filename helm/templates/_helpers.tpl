{{/*
Expand the name of the chart.
*/}}
{{- define "metacat.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "metacat.fullname" -}}
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
{{- define "metacat.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "metacat.labels" -}}
helm.sh/chart: {{ include "metacat.chart" . }}
{{ include "metacat.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "metacat.selectorLabels" -}}
app.kubernetes.io/name: {{ include "metacat.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "metacat.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "metacat.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}

{{/*
Metacat setup quirk - will not use https unless server.httpPort AND httpSSLPort are BOTH set
to 443. Automating this here, so operator doesn't need to mess with this confusing config:
* If .Values.metacat.server.httpPort is set explicitly, use it.
Otherwise:
* If using the ingress, set server.httpPort correctly to 80 or 443, depending if TLS is set up
*/}}
{{- define "metacat.httpPort" -}}
{{- $metacatHttpPort := (index .Values.metacat "server.httpPort") -}}
{{- if not $metacatHttpPort -}}
{{- if .Values.ingress.enabled -}}
    {{- $metacatHttpPort = ternary "80" "443" ( eq (len .Values.ingress.tls) 0 ) -}}
{{- else -}}
    {{- $metacatHttpPort = "80" -}}
{{- end -}}
{{- end -}}
{{- $metacatHttpPort | quote }}
{{- end }}
