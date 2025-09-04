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
* If .Values.metacat.server.port is set explicitly, use it.
Otherwise:
* If using the ingress, set server.port correctly to 80 or 443, depending if TLS is set up
*/}}
{{- define "metacat.serverPort" -}}
{{- $metacatServerPort := (index .Values.metacat "server.port") -}}
{{- if not $metacatServerPort -}}
{{- if .Values.ingress.enabled -}}
    {{- $metacatServerPort = ternary "80" "443" ( eq (len .Values.ingress.tls) 0 ) -}}
{{- else -}}
    {{- $metacatServerPort = "80" -}}
{{- end -}}
{{- end -}}
{{- $metacatServerPort }}
{{- end }}

{{/*
* If .Values.metacat.server.https is set explicitly, use it.
Otherwise:
* If using the ingress, set server.https correctly to true or false, depending if TLS is set up
*/}}
{{- define "metacat.serverHttps" -}}
{{- $metacatServerHttps := (index .Values.metacat "server.https") -}}
{{- if not $metacatServerHttps -}}
{{- if .Values.ingress.enabled -}}
    {{- $metacatServerHttps = ternary "false" "true" ( eq (len .Values.ingress.tls) 0 ) -}}
{{- else -}}
    {{- $metacatServerHttps = "false" -}}
{{- end -}}
{{- end -}}
{{- $metacatServerHttps }}
{{- end }}

{{/*
For DataONE mutual authentication with x509 certificates, add the following annotations to the
nginx ingress. Note these certificates are NOT the same as the one used for TLS ("SSL") access
via https
*/}}
{{- define "dataone.mutual.auth.annotations" -}}
{{- $caSecretName := .Values.ingress.d1CaCertSecretName -}}
# Enable client certificate authentication
nginx.ingress.kubernetes.io/auth-tls-verify-client: "optional_no_ca"
# The secret containing the trusted ca certificate and private key
nginx.ingress.kubernetes.io/auth-tls-secret: "{{ .Release.Namespace }}/{{ $caSecretName }}"
# Specify the verification depth in the client certificates chain
nginx.ingress.kubernetes.io/auth-tls-verify-depth: "10"
# Specify if certificates are passed to upstream server
nginx.ingress.kubernetes.io/auth-tls-pass-certificate-to-upstream: "true"
{{- end }}

{{/*
set RabbitMQ HostName
*/}}
{{- define "metacat.rabbitmq.hostname" -}}
{{- $rmqHost := (index .Values.metacat "index.rabbitmq.hostname") }}
{{- if and (index .Values.global "dataone-indexer.enabled") (not $rmqHost) -}}
{{- if (index .Values "dataone-indexer" "rabbitmq" "fullnameOverride") }}
{{- $rmqFullName := (index .Values "dataone-indexer" "rabbitmq" "fullnameOverride") }}
{{- $rmqHost = printf "%s-headless" ($rmqFullName | trunc 63 | trimSuffix "-") }}
{{- else }}
{{- $rmqName := (index .Values "dataone-indexer" "rabbitmq" "nameOverride") }}
{{- $rmqHost = printf "%s-%s-headless" .Release.Name ($rmqName | trunc 63 | trimSuffix "-") }}
{{- end }}
{{- end }}
{{- $rmqHost }}
{{- end }}

{{/*
set solr HostName
*/}}
{{- define "metacat.solr.hostname" -}}
{{- $solrHost := (index .Values "dataone-indexer" "solr" "hostname") }}
{{- if and (index .Values.global "dataone-indexer.enabled") (not $solrHost) -}}
    {{- $solrHost = printf "%s-solr-headless" .Release.Name -}}
{{- end }}
{{- $solrHost }}
{{- end }}

{{/*
set postgresql HostName
*/}}
{{- define "metacat.postgresql.hostname" -}}
{{- $dbUri := (index .Values.metacat "database.connectionURI") }}
{{- if not $dbUri }}
  {{- .Release.Name }}-cnpg-rw
{{- else }}
  {{- regexFind "://[^/]+" $dbUri | trimPrefix "://" }}
{{- end }}
{{- end }}

{{/*
Detect whether we're doing a helm install/upgrade, or just a local 'helm template'
*/}}
{{- define "helm.sees.cluster" -}}
{{- $ns := lookup "v1" "Namespace" .Release.Namespace .Release.Namespace -}}
{{- if $ns -}}
true
{{- else -}}
{{- /* no output means false */ -}}
{{- end -}}
{{- end }}

{{/*
set postgresql UserName
*/}}
{{- define "metacat.postgresql.username" -}}
{{- $secretName := ( include "metacat.cnpg.secret.name" . ) }}
{{- $secret := lookup "v1" "Secret" .Release.Namespace $secretName -}}
{{- if $secret -}}
  {{- $raw := index $secret.data "username" | required (printf "Key 'username' not found in Secret %s" $secretName) | toString -}}
  {{- $raw | b64dec -}}
{{- else -}}
  {{- if ( include "helm.sees.cluster" .) -}}
    {{- fail (printf "Secret %s not found in namespace %s - %s" $secretName .Release.Namespace ) -}}
  {{- else -}}
    {{- printf "templating-only--Secret-%s-not-found-locally" $secretName }}
  {{- end -}}
{{- end -}}
{{- end -}}

{{/*
set postgresql Basic Auth Secret Name
*/}}
{{- define "metacat.cnpg.secret.name" -}}
{{- $secretName := .Values.database.existingSecret }}
{{- if $secretName }}
{{- $secretName }}
{{- else }}
{{- printf "%s-metacat-cnpg" .Release.Name }}
{{- end }}
{{- end }}

{{/*
Renders a value that contains template.
Usage:
{{ include "helpers.tplvalues.render" ( dict "value" .Values.path.to.the.Value "context" $) }}
*/}}
{{- define "helpers.tplvalues.render" -}}
    {{- if typeIs "string" .value }}
        {{- tpl .value .context }}
    {{- else }}
        {{- tpl (.value | toYaml) .context }}
    {{- end }}
{{- end -}}
