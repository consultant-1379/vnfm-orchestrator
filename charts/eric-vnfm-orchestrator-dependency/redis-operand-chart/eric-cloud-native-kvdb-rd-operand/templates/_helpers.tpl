{{/* vim: set filetype=mustache: */}}

{{/*
Create a map from values with defaults if missing in values file.
This hides defaults from values file.
*/}}
{{- define "eric-cloud-native-kvdb-rd-operand.global" -}}
  {{- $globalDefaults := dict "timezone" "UTC" -}}
  {{- $globalDefaults := merge $globalDefaults (dict "registry" (dict "imagePullPolicy" "IfNotPresent" )) -}}
  {{- $globalDefaults := merge $globalDefaults (dict "registry" (dict "url" "selndocker.mo.sw.ericsson.se")) -}}
  {{- $globalDefaults := merge $globalDefaults (dict "pullSecret" "") -}}
  {{- $globalDefaults := merge $globalDefaults (dict "nodeSelector" (dict)) -}}
  {{- $globalDefaults := merge $globalDefaults (dict "annotations" (dict)) -}}
  {{- $globalDefaults := merge $globalDefaults (dict "labels" (dict)) -}}
  {{- $globalDefaults := merge $globalDefaults (dict "security" (dict "policyBinding" (dict "create" false))) -}}
  {{- $globalDefaults := merge $globalDefaults (dict "security" (dict "policyReferenceMap" (dict))) -}}
  {{- $globalDefaults := merge $globalDefaults (dict "security" (dict "tls" (dict "enabled" true))) -}}
  {{- $globalDefaults := merge $globalDefaults (dict "internalIPFamily" "") -}}
  {{- $globalDefaults := merge $globalDefaults (dict "networkPolicy" (dict "enabled" false)) -}}
  {{- if .Values.global -}}
    {{- mergeOverwrite $globalDefaults .Values.global | toJson -}}
  {{- else -}}
    {{- $globalDefaults | toJson -}}
  {{- end -}}
{{- end -}}

{{/*
Expand the name of the chart.
*/}}
{{- define "eric-cloud-native-kvdb-rd-operand.name" -}}
  {{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
Create chart version as used by the chart label.
*/}}
{{- define "eric-cloud-native-kvdb-rd-operand.version" -}}
  {{- printf "%s" .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "eric-cloud-native-kvdb-rd-operand.chart" -}}
  {{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
Create a merged set of labels from global and service level.
*/}}
{{- define "eric-cloud-native-kvdb-rd-operand.labels" -}}
  {{- $standard := include "eric-cloud-native-kvdb-rd-operand.standard-labels" . | fromYaml -}}
  {{- $global := (.Values.global).labels -}}
  {{- $service := .Values.labels -}}
  {{- include "eric-cloud-native-kvdb-rd-operand.mergeLabels" (dict "location" .Template.Name "sources" (list $standard $global $service)) | trim }}
{{- end -}}

{{/*
Common labels
*/}}
{{- define "eric-cloud-native-kvdb-rd-operand.standard-labels" -}}
app.kubernetes.io/name: {{ template "eric-cloud-native-kvdb-rd-operand.name" . }}
app.kubernetes.io/version: {{ template "eric-cloud-native-kvdb-rd-operand.version" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
chart: {{ template "eric-cloud-native-kvdb-rd-operand.chart" . }}
{{- end -}}

{{/*
Create annotations for prometheus
*/}}
{{- define "eric-cloud-native-kvdb-rd-operand.prometheus" -}}
{{- $global := fromJson (include "eric-cloud-native-kvdb-rd-operand.global" .) -}}
{{- if .Values.metrics.enabled -}}
prometheus.io/path: "/metrics"
prometheus.io/port: "9121"
prometheus.io/scrape-role: "endpoints"
prometheus.io/scrape-interval: "15s"
{{- if $global.security.tls.enabled }}
prometheus.io/scheme: "https"
{{- end -}}
{{- end -}}
{{- end -}}

{{- define "eric-cloud-native-kvdb-rd-operand.logTransformerHost" -}}
  {{- if ((.Values.logshipper).logtransformer).host -}}
    {{- .Values.logshipper.logtransformer.host -}}
  {{- else -}}
    {{- .Values.logTransformerService.host -}}
  {{- end -}}
{{- end -}}

{{/*
Port for Log Transformer streaming. Default port is different for TLS and non-TLS.
*/}}
{{- define "eric-cloud-native-kvdb-rd-operand.logTransformerPort" -}}
  {{- $g := fromJson (include "eric-cloud-native-kvdb-rd-operand.global" .) -}}
  {{- if $g.security.tls.enabled }}
    {{- 5024 -}}
  {{- else -}}
    {{- 5025 -}}
  {{- end -}}
{{- end -}}

{{/*
Name of the metrics exporter port
*/}}
{{- define "eric-cloud-native-kvdb-rd-operand.metrics-port-name" -}}
{{- $global := fromJson (include "eric-cloud-native-kvdb-rd-operand.global" .) -}}
{{- if $global.security.tls.enabled -}}
metrics-tls
{{- else -}}
metrics
{{- end -}}
{{- end -}}

{{/*
Create image pull policy for node image
*/}}
{{- define "eric-cloud-native-kvdb-rd-operand.node-imagePullPolicy" -}}
  {{/* Get global imagePullPolicy */}}
  {{- $g := fromJson (include "eric-cloud-native-kvdb-rd-operand.global" .) -}}
  {{- $imagePullPolicy := $g.registry.imagePullPolicy -}}

  {{/* Get local override*/}}
  {{- if .Values.imageCredentials -}}
    {{- if .Values.imageCredentials.node -}}
      {{- if .Values.imageCredentials.node.registry -}}
        {{- if .Values.imageCredentials.node.registry.imagePullPolicy -}}
          {{- $imagePullPolicy = .Values.imageCredentials.node.registry.imagePullPolicy -}}
        {{- end -}}
      {{- end -}}
    {{- end -}}
  {{- end -}}

  {{/* Print policy */}}
  {{- print $imagePullPolicy -}}
{{- end -}}

{{/*
Create image pull policy for metricsExporter
*/}}
{{- define "eric-cloud-native-kvdb-rd-operand.metricsExporter-imagePullPolicy" -}}
  {{/* Get global imagePullPolicy */}}
  {{- $g := fromJson (include "eric-cloud-native-kvdb-rd-operand.global" .) -}}
  {{- $imagePullPolicy := $g.registry.imagePullPolicy -}}

  {{/* Get local override*/}}
  {{- if .Values.imageCredentials -}}
    {{- if .Values.imageCredentials.metricsExporter -}}
      {{- if .Values.imageCredentials.metricsExporter.registry -}}
        {{- if .Values.imageCredentials.metricsExporter.registry.imagePullPolicy -}}
          {{- $imagePullPolicy = .Values.imageCredentials.metricsExporter.registry.imagePullPolicy -}}
        {{- end -}}
      {{- end -}}
    {{- end -}}
  {{- end -}}

  {{/* Print policy */}}
  {{- print $imagePullPolicy -}}
{{- end -}}

{{/*
Create image pull secrets
*/}}
{{- define "eric-cloud-native-kvdb-rd-operand.pullSecrets" -}}
  {{/* Get global pullSecret */}}
  {{- $g := fromJson (include "eric-cloud-native-kvdb-rd-operand.global" .) -}}
  {{- $pullSecret := $g.pullSecret -}}

  {{/* Get local override */}}
  {{- if .Values.imageCredentials -}}
    {{- if .Values.imageCredentials.pullSecret -}}
      {{- $pullSecret = .Values.imageCredentials.pullSecret -}}
    {{- end -}}
  {{- end -}}

  {{/* Print the secret name */}}
  {{- print $pullSecret -}}
{{- end -}}

{{/*
Timezone variable
*/}}
{{- define "eric-cloud-native-kvdb-rd-operand.timezone" -}}
  {{- $g := fromJson (include "eric-cloud-native-kvdb-rd-operand.global" .) -}}
  {{- print $g.timezone | quote -}}
{{- end -}}

{{/*
 Limit Pod Egress bandwidth(DR-D1125-040)
*/}}
{{- define "eric-cloud-native-kvdb-rd-operand.egressAnnotations" -}}
  {{- if .Values.bandwidth.maxEgressRate -}}
    kubernetes.io/egress-bandwidth: {{ .Values.bandwidth.maxEgressRate }}
  {{- end -}}
{{- end -}}

{{/*
Create a merged set of annotations from global and service level.
(DR-D1121-065, DR-D1121-060)
*/}}
{{- define "eric-cloud-native-kvdb-rd-operand.annotations" -}}
  {{- $productInfo := include "eric-cloud-native-kvdb-rd-operand.helm-annotations" . | fromYaml -}}
  {{- $global := (.Values.global).annotations -}}
  {{- $service := .Values.annotations -}}
  {{- include "eric-cloud-native-kvdb-rd-operand.mergeAnnotations" (dict "location" (.Template.Name) "sources" (list $productInfo $global $service)) | trim }}
{{- end -}}

{{/*
Create Ericsson product specific annotations
*/}}
{{- define "eric-cloud-native-kvdb-rd-operand.helm-annotations" -}}
ericsson.com/product-name: {{ (fromYaml (.Files.Get "eric-product-info.yaml")).productName | quote }}
ericsson.com/product-number: {{ (fromYaml (.Files.Get "eric-product-info.yaml")).productNumber | quote }}
ericsson.com/product-revision: {{ regexReplaceAll "(.*)[+-].*" .Chart.Version "${1}" | quote }}
{{- end -}}

{{/*
Expand the name of the chart.
*/}}
{{- define "eric-cloud-native-kvdb-rd-operand.nodename" -}}
  {{- $name := default .Release.Name .Values.nameOverride -}}
  {{- printf "%s" $name | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
The KVDB Operand image path
*/}}
{{- define "eric-cloud-native-kvdb-rd-operand.operandImagePath" -}}
  {{/* Read defaults from eric-product-info.yaml */}}
  {{- $productInfo := fromYaml (.Files.Get "eric-product-info.yaml") -}}
  {{- $registryUrl := $productInfo.images.kvdbOperand.registry -}}
  {{- $repoPath := $productInfo.images.kvdbOperand.repoPath -}}
  {{- $name := $productInfo.images.kvdbOperand.name -}}
  {{- $tag := $productInfo.images.kvdbOperand.tag -}}

  {{/* Get global override */}}
  {{- if ((.Values.global).registry).url -}}
    {{- $registryUrl = .Values.global.registry.url -}}
  {{- end -}}

  {{/* Get local override */}}
  {{- if (.Values.imageCredentials).node -}}
    {{- if ((.Values.imageCredentials.node).registry).url -}}
      {{- $registryUrl = .Values.imageCredentials.node.registry.url -}}
    {{- end -}}

    {{- if .Values.imageCredentials.node.repoPath -}}
      {{- $repoPath = .Values.imageCredentials.node.repoPath -}}
    {{- end -}}
  {{- end -}}

  {{- if $repoPath -}}
    {{- $repoPath = printf "%s/" $repoPath -}}
  {{- end -}}

  {{/* Write full image path */}}
  {{- printf "%s/%s%s:%s" $registryUrl $repoPath $name $tag -}}
{{- end -}}

{{/*
The Metrics Exporter image path
*/}}
{{- define "eric-cloud-native-kvdb-rd-operand.metricsExporterImagePath" -}}
  {{/* Read defaults from eric-product-info.yaml */}}
  {{- $productInfo := fromYaml (.Files.Get "eric-product-info.yaml") -}}
  {{- $registryUrl := $productInfo.images.kvdbMetricsExporter.registry -}}
  {{- $repoPath := $productInfo.images.kvdbMetricsExporter.repoPath -}}
  {{- $name := $productInfo.images.kvdbMetricsExporter.name -}}
  {{- $tag := $productInfo.images.kvdbMetricsExporter.tag -}}

  {{/* Get global override */}}
  {{- if ((.Values.global).registry).url -}}
    {{- $registryUrl = .Values.global.registry.url -}}
  {{- end -}}

  {{/* Get local override */}}
  {{- if (.Values.imageCredentials).metricsExporter -}}
    {{- if ((.Values.imageCredentials.metricsExporter).registry).url -}}
      {{- $registryUrl = .Values.imageCredentials.metricsExporter.registry.url -}}
    {{- end -}}

    {{- if .Values.imageCredentials.metricsExporter.repoPath -}}
      {{- $repoPath = .Values.imageCredentials.metricsExporter.repoPath -}}
    {{- end -}}
  {{- end -}}

  {{- if $repoPath -}}
    {{- $repoPath = printf "%s/" $repoPath -}}
  {{- end -}}

  {{/* Write full image path */}}
  {{- printf "%s/%s%s:%s" $registryUrl $repoPath $name $tag -}}
{{- end -}}

{{/*
Create merged nodeSelector (global + service local)
*/}}
{{- define "eric-cloud-native-kvdb-rd-operand.nodeSelector" -}}
  {{- $global := (.Values.global).nodeSelector -}}
  {{- $service := .Values.nodeSelector -}}
  {{- $context := "eric-cloud-native-kvdb-rd-operand.nodeSelector" -}}
  {{- include "eric-cloud-native-kvdb-rd-operand.aggregatedMerge" (dict "context" $context "location" .Template.Name "sources" (list $global $service)) | trim -}}
{{- end }}

{{/*
Create timing definitions

redisStartDelay:
cluster-node-timeout * 5, allows for FAIL state detection, election, and some
extra time to propagate the information throughout the Redis cluster.
*/}}
{{- define "eric-cloud-native-kvdb-rd-operand.redisStartDelay" -}}
  {{- div .Values.server.clusterNodeTimeoutMs 1000 | mul 5 -}}
{{- end -}}

{{/*
Methods for defining security policies
*/}}
{{- define "eric-cloud-native-kvdb-rd-operand.securityPolicy.reference" -}}
  {{- $g := fromJson (include "eric-cloud-native-kvdb-rd-operand.global" .) -}}
  {{- $policyName := "" -}}

  {{/* Get global default security policy */}}
  {{- if $g.security -}}
    {{- if $g.security.policyReferenceMap -}}
      {{- if index $g "security" "policyReferenceMap" "default-restricted-security-policy" -}}
        {{- $policyName = index $g "security" "policyReferenceMap" "default-restricted-security-policy" -}}
      {{- end -}}
    {{- end -}}
  {{- end -}}

  {{/* Use global or default security policy */}}
  {{- $policyName := default "default-restricted-security-policy" $policyName -}}
  {{- $policyName -}}
{{- end -}}

{{/*
Annotations for security-policy
*/}}
{{- define "eric-cloud-native-kvdb-rd-operand.securityPolicy.annotations" -}}
ericsson.com/security-policy.type: "restricted/default"
ericsson.com/security-policy.capabilities: ""
{{- end -}}

{{- define "eric-cloud-native-kvdb-rd-operand.combinedCAMountPath" -}}
  /etc/tls/combined-ca
{{- end -}}

{{- define "eric-cloud-native-kvdb-rd-operand.rootCAMountPath" -}}
  /etc/tls/root-ca
{{- end -}}

{{- define "eric-cloud-native-kvdb-rd-operand.clientCAMountPath" -}}
  /etc/tls/client-ca
{{- end -}}

{{- define "eric-cloud-native-kvdb-rd-operand.clientCertificateMountPath" -}}
  /etc/tls/client
{{- end -}}

{{- define "eric-cloud-native-kvdb-rd-operand.probesCertificateMountPath" -}}
  /etc/tls/probes
{{- end -}}

{{- define "eric-cloud-native-kvdb-rd-operand.serverCertificateMountPath" -}}
  /etc/tls/server
{{- end -}}

{{- define "eric-cloud-native-kvdb-rd-operand.metricsExporterCertificateMountPath" -}}
  /etc/tls/metrics-exporter
{{- end -}}

{{- define "eric-cloud-native-kvdb-rd-operand.metricsClientCertificateMountPath" -}}
  /etc/tls/metrics-client-cert
{{- end -}}

{{- define "eric-cloud-native-kvdb-rd-operand.logTransformerClientCertificateMountPath" -}}
  /etc/tls/log-transformer-client-cert
{{- end -}}

{{- define "eric-cloud-native-kvdb-rd-operand.pmServerCAMountPath" -}}
  /etc/tls/pm-server-ca
{{- end -}}

{{- define "eric-cloud-native-kvdb-rd-operand.serverSecretName" -}}
  {{- template "eric-cloud-native-kvdb-rd-operand.name" . }}-tls-server-secret
{{- end -}}

{{- define "eric-cloud-native-kvdb-rd-operand.clientSecretName" -}}
  {{- template "eric-cloud-native-kvdb-rd-operand.name" . }}-tls-client-secret
{{- end -}}

{{- define "eric-cloud-native-kvdb-rd-operand.metricsExporterSecretName" -}}
  {{- printf "%s-metrics-exporter-secret" (include "eric-cloud-native-kvdb-rd-operand.name" .) | trunc 63 | trimSuffix "-" }}
{{- end -}}

{{- define "eric-cloud-native-kvdb-rd-operand.metricsClientSecret" -}}
  {{- template "eric-cloud-native-kvdb-rd-operand.name" . }}-tls-metrics-client
{{- end -}}

{{/* Name of the secret holding the probes server certificate */}}
{{- define "eric-cloud-native-kvdb-rd-operand.probesServerSecretName" }}
    {{- printf "%s-%s" (include "eric-cloud-native-kvdb-rd-operand.name" .) "probes-server-secret" -}}
{{- end }}

{{- define "eric-cloud-native-kvdb-rd-operand.logTransformerClientSecret" -}}
  {{- template "eric-cloud-native-kvdb-rd-operand.name" . }}-lt-client-cert
{{- end -}}

{{- define "eric-cloud-native-kvdb-rd-operand.passwordGenerator" -}}
  {{- randAlphaNum 128 -}}
{{- end -}}

{{- define "eric-cloud-native-kvdb-rd-operand.validateUser" -}}
  {{- $user := . -}}
  {{- if not $user.name }}
    {{- fail (printf "Missing mandatory parameter \"name\" for user \"%s\" in \"security.acl.users\"." $user) }}
  {{- end }}
  {{- if not $user.permissions }}
    {{- fail (printf "Missing mandatory parameter \"permissions\" for user \"%s\" in \"security.acl.users\"." $user) }}
  {{- end }}
{{- end -}}

{{/* Probes server listening port */}}
{{- define "eric-cloud-native-kvdb-rd-operand.probes.port" }}
    {{- printf "%d" 8080 -}}
{{- end }}

{{/* K8s client probes scheme HTTP | HTTPS */}}
{{- define "eric-cloud-native-kvdb-rd-operand.probes.scheme" }}
{{- $global := fromJson (include "eric-cloud-native-kvdb-rd-operand.global" .) -}}
{{- if $global.security.tls.enabled }}
    {{- printf "%s" "HTTPS" -}}
{{- else }}
    {{- printf "%s" "HTTP" -}}
{{- end }}
{{- end }}

{{- define "eric-cloud-native-kvdb-rd-operand.metricsProbeCommand" -}}
{{- $global := fromJson (include "eric-cloud-native-kvdb-rd-operand.global" .) -}}
exec:
  command: [
    "curl",
    "--fail",
    "--retry", "0",
    "--output", "/dev/null",
    "--silent",
    "--show-error",
    {{- if $global.security.tls.enabled }}
      "--cacert", "{{ template "eric-cloud-native-kvdb-rd-operand.rootCAMountPath" . }}/cacertbundle.pem",
      {{- if eq .Values.service.endpoints.metrics.tls.verifyClientCertificate "required" }}
        "--cert", "{{ template "eric-cloud-native-kvdb-rd-operand.metricsClientCertificateMountPath" . }}/cert.pem",
        "--key", "{{ template "eric-cloud-native-kvdb-rd-operand.metricsClientCertificateMountPath" . }}/privkey.pem",
      {{- end }}
      "https://localhost:9121/health"
    {{- else }}
    "http://localhost:9121/health"
    {{- end }}
  ]
{{- end -}}

# Mandatory hardcoded values
{{- define "eric-cloud-native-kvdb-rd-operand.internalUserSecretName" -}}
  {{- .Values.kvdbOperatorName }}-internal-user-secret
{{- end -}}

{{- define "eric-cloud-native-kvdb-rd-operand.internalUserName" -}}
  internal-kvdb-user
{{- end -}}

{{- define "eric-cloud-native-kvdb-rd-operand.isDefaultUserInUsersList" -}}
  {{- $users := . }}
  {{- range $user := $users }}
    {{- if eq $user.name "default" -}}
      true
    {{- end }}
  {{- end }}
{{- end }}

{{/* Deprecation notices */}}
{{- define "eric-cloud-native-kvdb-rd-operand.deprecation.notices" }}
  {{- if (.Values.images).logshipper }}
    {{- range $k, $_ := (index .Values.images.logshipper) }}
      {{- printf "'images.logshipper.%s' is deprecated as of release 3.3.0, the input value will be discarded.\n" $k }}
    {{- end }}
  {{- end }}
    {{- if (.Values.logshipper).storagePath }}
    {{- printf "'log.logshipper.storagePath ' is deprecated as of release 3.3.0, the input value will be discarded.\n" }}
  {{- end }}
  {{- if (.Values.logshipper).storageAllocation }}
    {{- printf "'logshipper.storageAllocation' is deprecated as of release 3.3.0, the input value will be discarded.\n" }}
  {{- end }}
  {{- if (.Values.logshipper).logplane }}
    {{- printf "'logshipper.logplane' is deprecated as of release 3.3.0, the input value will be discarded.\n" }}
  {{- end }}
  {{- if (.Values.logshipper).harvester }}
    {{- printf "'logshipper.harvester' is deprecated as of release 3.3.0, the input value will be discarded.\n" }}
  {{- end }}
  {{- if ((.Values.logshipper).logtransformer).host }}
    {{- printf "'logshipper.logtransformer.host' is deprecated as of release 3.3.0, the input value will be discarded.\n" }}
  {{- end }}
  {{- if .Values.log.node.logsize }}
    {{- printf "'log.node.logsize' is deprecated as of release 3.3.0, the input value will be discarded.\n" }}
  {{- end }}
  {{- if .Values.log.node.rotations }}
    {{- printf "'log.node.rotations' is deprecated as of release 3.3.0, the input value will be discarded.\n" }}
  {{- end }}
  {{- if .Values.log.metricsExporter.logsize }}
    {{- printf "'log.metricsExporter.logsize' is deprecated as of release 3.3.0, the input value will be discarded.\n" }}
  {{- end }}
  {{- if .Values.log.metricsExporter.rotations }}
    {{- printf "'log.metricsExporter.rotations' is deprecated as of release 3.3.0, the input value will be discarded.\n" }}
  {{- end }}
  {{- if ((.Values.log).logshipper).level }}
    {{- printf "'log.logshipper.level' is deprecated as of release 3.3.0, the input value will be discarded.\n" }}
  {{- end }}
  {{- if (.Values.resources).logshipper }}
    {{- range $k, $_ := (index .Values.resources.logshipper) }}
      {{- printf "'resources.logshipper.%s' is deprecated as of release 3.3.0, the input value will be discarded.\n" $k }}
    {{- end }}
  {{- end }}
  {{- if ((.Values.probes).logshipper).livenessProbe }}
    {{- range $k, $_ := (index .Values.probes.logshipper.livenessProbe) }}
      {{- printf "'probes.logshipper.livenessProbe.%s' is deprecated as of release 3.3.0, the input value will be discarded.\n" $k }}
    {{- end }}
  {{- end }}
  {{- if ((.Values.readinessProbe).logshipper).initialDelaySeconds }}
    {{- printf "'readinessProbe.logshipper.initialDelaySeconds' is deprecated as of release 3.3.0, the input value will be discarded.\n" }}
  {{- end }}
  {{- if (.Values.podPriority).priorityClassName }}
    {{- printf "'podPriority.priorityClassName' is deprecated as of release 3.5.0. It is replaced by podPriority.node.priorityClassName and will soon be removed.\n" }}
  {{- end }}
  {{- if eq (typeOf .Values.tolerations) ("[]interface {}") }}
    {{- printf "'tolerations' is deprecated as of release 3.5.0. It is replaced by tolerations.node and will soon be removed.\n" }}
  {{- end }}
{{- end }}

{{/*
Create tolerations for operand Pod
*/}}
{{- define "eric-cloud-native-kvdb-rd-operand.tolerations" -}}
  {{- $oldSetByUser := eq (typeOf .Values.tolerations) ("[]interface {}") -}}
  {{- if $oldSetByUser -}}
    {{- .Values.tolerations | toYaml -}}
  {{- else -}}
    {{- .Values.tolerations.node | toYaml -}}
  {{- end -}}
{{- end -}}

{{/*
Create priorityClassName (DR-D1126-030)
*/}}
{{- define "eric-cloud-native-kvdb-rd-operand.priorityClassName" -}}
  {{- if .Values.podPriority.node.priorityClassName -}}
    {{- .Values.podPriority.node.priorityClassName -}}
  {{- else if .Values.podPriority.priorityClassName -}}
    {{- .Values.podPriority.priorityClassName -}}
  {{- end -}}
{{- end -}}
