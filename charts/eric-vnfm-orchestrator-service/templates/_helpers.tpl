{{/* vim: set filetype=mustache: */}}
{{/*
Expand the name of the chart.
*/}}
{{- define "eric-vnfm-orchestrator-service.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
*/}}
{{- define "eric-vnfm-orchestrator-service.fullname" -}}
{{- if .Values.fullnameOverride -}}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- template "eric-vnfm-orchestrator-service.name" . -}}
{{- end -}}
{{- end -}}


{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "eric-vnfm-orchestrator-service.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
Create main image registry url
*/}}
{{- define "eric-vnfm-orchestrator-service.mainImagePath" -}}
  {{- include "eric-eo-evnfm-library-chart.mainImagePath" (dict "ctx" . "svcRegistryName" "orchestratorService") -}}
{{- end -}}

{{/*
The pgInitContainer image registry url
*/}}
{{- define "eric-vnfm-orchestrator-service.pgInitContainerPath" -}}
  {{- include "eric-eo-evnfm-library-chart.mainImagePath" (dict "ctx" . "svcRegistryName" "pgInitContainer") -}}
{{- end -}}

{{/*
Create image pull secrets
*/}}
{{- define "eric-vnfm-orchestrator-service.pullSecrets" -}}
  {{- include "eric-eo-evnfm-library-chart.pullSecrets" . -}}
{{- end -}}

{{/*
Create Ericsson Product Info
*/}}
{{- define "eric-vnfm-orchestrator-service.helm-annotations" -}}
{{- include "eric-eo-evnfm-library-chart.helm-annotations" . -}}
{{- end -}}

{{/*
Create prometheus info
*/}}
{{- define "eric-vnfm-orchestrator-service.prometheus" -}}
  {{- include "eric-eo-evnfm-library-chart.prometheus" . -}}
{{- end -}}

{{/*
Create Ericsson product app.kubernetes.io info
*/}}
{{- define "eric-vnfm-orchestrator-service.kubernetes-io-info" -}}
  {{- include "eric-eo-evnfm-library-chart.kubernetes-io-info" . -}}
{{- end -}}

{{/*
Create pull policy for orchestrator service
*/}}
{{- define "eric-vnfm-orchestrator-service.imagePullPolicy" -}}
  {{- include "eric-eo-evnfm-library-chart.imagePullPolicy" (dict "ctx" . "svcRegistryName" "orchestratorService") -}}
{{- end -}}

{{/*
Create pull policy for pgInitContainer
*/}}
{{- define "eric-vnfm-orchestrator-service.pgInitContainer.imagePullPolicy" -}}
  {{- include "eric-eo-evnfm-library-chart.pgInitContainer.imagePullPolicy" . -}}
{{- end -}}

{{- define "eric-vnfm-orchestrator-service.nodeSelector" -}}
  {{- include "eric-eo-evnfm-library-chart.nodeSelector" . -}}
{{- end -}}

{{/*
Create chart version as used by the chart label.
*/}}
{{- define "eric-vnfm-orchestrator-service.version" -}}
  {{- printf "%s" .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
Kubernetes labels
*/}}
{{- define "eric-vnfm-orchestrator-service.kubernetes-labels" -}}
app.kubernetes.io/name: {{ include "eric-vnfm-orchestrator-service.name" . }}
app.kubernetes.io/instance: {{ .Release.Name | quote }}
app.kubernetes.io/version: {{ include "eric-vnfm-orchestrator-service.version" . }}
{{- end -}}

{{/*
Common labels
*/}}
{{- define "eric-vnfm-orchestrator-service.labels" -}}
  {{- $kubernetesLabels := include "eric-vnfm-orchestrator-service.kubernetes-labels" . | fromYaml -}}
  {{- $globalLabels := (.Values.global).labels -}}
  {{- $serviceLabels := .Values.labels -}}
  {{- include "eric-eo-evnfm-library-chart.mergeLabels" (dict "location" .Template.Name "sources" (list $kubernetesLabels $globalLabels $serviceLabels)) }}
{{- end -}}

{{/*
Merged labels for extended defaults
*/}}
{{- define "eric-vnfm-orchestrator-service.labels.extended-defaults" -}}
  {{- $extendedLabels := dict -}}
  {{- $_ := set $extendedLabels "logger-communication-type" "direct" -}}
  {{- $_ := set $extendedLabels "app" (include "eric-vnfm-orchestrator-service.name" .) -}}
  {{- $_ := set $extendedLabels "chart" (include "eric-vnfm-orchestrator-service.chart" .) -}}
  {{- $_ := set $extendedLabels "release" (.Release.Name) -}}
  {{- $_ := set $extendedLabels "heritage" (.Release.Service) -}}
  {{- $_ := set $extendedLabels "eric-eo-lm-consumer-access" "true" -}}
  {{- $commonLabels := include "eric-vnfm-orchestrator-service.labels" . | fromYaml -}}
  {{- $serviceMesh := include "eric-vnfm-orchestrator-service.service-mesh-inject" . | fromYaml -}}
  {{- include "eric-eo-evnfm-library-chart.mergeLabels" (dict "location" .Template.Name "sources" (list $commonLabels $serviceMesh $extendedLabels)) | trim }}
{{- end -}}


{{/*
Create Ericsson product specific annotations
*/}}
{{- define "eric-vnfm-orchestrator-service.helm-annotations_product_name" -}}
{{- include "eric-eo-evnfm-library-chart.helm-annotations_product_name" . -}}
{{- end -}}
{{- define "eric-vnfm-orchestrator-service.helm-annotations_product_number" -}}
{{- include "eric-eo-evnfm-library-chart.helm-annotations_product_number" . -}}
{{- end -}}
{{- define "eric-vnfm-orchestrator-service.helm-annotations_product_revision" -}}
{{- include "eric-eo-evnfm-library-chart.helm-annotations_product_revision" . -}}
{{- end -}}

{{/*
Create a dict of annotations for the product information (DR-D1121-064, DR-D1121-067).
*/}}
{{- define "eric-vnfm-orchestrator-service.product-info" }}
ericsson.com/product-name: {{ template "eric-vnfm-orchestrator-service.helm-annotations_product_name" . }}
ericsson.com/product-number: {{ template "eric-vnfm-orchestrator-service.helm-annotations_product_number" . }}
ericsson.com/product-revision: {{ template "eric-vnfm-orchestrator-service.helm-annotations_product_revision" . }}
{{- end }}

{{/*
Common annotations
*/}}
{{- define "eric-vnfm-orchestrator-service.annotations" -}}
  {{- $productInfo := include "eric-vnfm-orchestrator-service.helm-annotations" . | fromYaml -}}
  {{- $globalAnn := (.Values.global).annotations -}}
  {{- $serviceAnn := .Values.annotations -}}
  {{- include "eric-eo-evnfm-library-chart.mergeAnnotations" (dict "location" .Template.Name "sources" (list $productInfo $globalAnn $serviceAnn)) | trim }}
{{- end -}}


{{/*
Define probes
*/}}
{{- define "eric-vnfm-orchestrator-service.probes" -}}
{{- $default := .Values.probes -}}
{{- if .Values.probing }}
  {{- if .Values.probing.liveness }}
    {{- if .Values.probing.liveness.orchestrator }}
      {{- $default := mergeOverwrite $default.orchestrator.livenessProbe .Values.probing.liveness.orchestrator  -}}
    {{- end }}
  {{- end }}
  {{- if .Values.probing.readiness }}
    {{- if .Values.probing.readiness.orchestrator }}
      {{- $default := mergeOverwrite $default.orchestrator.readinessProbe .Values.probing.readiness.orchestrator  -}}
    {{- end }}
  {{- end }}
{{- end }}
{{- $default | toJson -}}
{{- end -}}

{{/*
To support Dual stack.
*/}}
{{- define "eric-vnfm-orchestrator-service.internalIPFamily" -}}
{{- include "eric-eo-evnfm-library-chart.internalIPFamily" . -}}
{{- end -}}

{{- define "eric-vnfm-orchestrator-service.podPriority" -}}
{{- include "eric-eo-evnfm-library-chart.podPriority" ( dict "ctx" . "svcName" "orchestrator" ) -}}
{{- end -}}

{{/*
Define tolerations property
*/}}
{{- define "eric-vnfm-orchestrator-service.tolerations.orchestrator" -}}
  {{- include "eric-eo-evnfm-library-chart.merge-tolerations" (dict "root" . "podbasename" "orchestrator" ) -}}
{{- end -}}

{{/*
Define DB connection pool max life time property
If not set by user, defaults to 14 minutes.
*/}}
{{ define "eric-vnfm-orchestrator-service.db.connection.pool.max.lifetime" -}}
- name: "spring.datasource.hikari.max-lifetime"
  value: {{ index .Values "global" "db" "connection" "max-lifetime" | default "840000" | quote -}}
{{- end -}}

{{/*
Define RedisCluster port
*/}}
{{ define "eric-vnfm-orchestrator-service.redis.port" -}}
{{ $redisPort := .Values.redis.port -}}
{{- if .Values.global -}}
    {{- if .Values.global.security -}}
        {{- if .Values.global.security.tls -}}
            {{- if .Values.global.security.tls.enabled -}}
            {{- $redisPort =  .Values.redis.tlsPort -}}
            {{- end -}}
        {{- end -}}
    {{- end -}}
{{- end -}}
{{- $redisPort -}}
{{- end -}}

{{/* Name of the secret holding Redis ACL username and password */}}
{{- define "eric-vnfm-orchestrator-service.redis.acl.secretname" }}
    {{- printf "%s-secret-%s" .Values.redis.host .Values.redis.acl.user -}}
{{- end }}

{{/*
Check global.security.tls.enabled
*/}}
{{- define "eric-vnfm-orchestrator-service.global-security-tls-enabled" -}}
  {{- include "eric-eo-evnfm-library-chart.global-security-tls-enabled" . -}}
{{- end -}}

{{/*
DR-D470217-007-AD This helper defines whether this service enter the Service Mesh or not.
*/}}
{{- define "eric-vnfm-orchestrator-service.service-mesh-enabled" }}
{{- include "eric-eo-evnfm-library-chart.service-mesh-enabled" . -}}
{{- end -}}

{{/*
DR-D470217-011 This helper defines the annotation which bring the service into the mesh.
*/}}
{{- define "eric-vnfm-orchestrator-service.service-mesh-inject" }}
{{- include "eric-eo-evnfm-library-chart.service-mesh-inject" . -}}
{{- end -}}

{{/*
This helper defines log level for Service Mesh.
*/}}
{{- define "eric-vnfm-orchestrator-service.service-mesh-logs" }}
  {{- include "eric-eo-evnfm-library-chart.service-mesh-logs" . -}}
{{- end -}}

{{/*
GL-D470217-080-AD
This helper captures the service mesh version from the integration chart to
annotate the workloads so they are redeployed in case of service mesh upgrade.
*/}}
{{- define "eric-vnfm-orchestrator-service.service-mesh-version" }}
{{- include "eric-eo-evnfm-library-chart.service-mesh-version" . -}}
{{- end -}}

{{/*
DR-D1123-124
Evaluating the Security Policy Cluster Role Name
*/}}
{{- define "eric-vnfm-orchestrator-service.securityPolicy.reference" -}}
{{- include "eric-eo-evnfm-library-chart.securityPolicy.reference" . -}}
{{- end -}}

{{/*
DR-D1123-136
Define fsGroup property
*/}}
{{- define "eric-vnfm-orchestrator-service.fsGroup" -}}
  {{- include "eric-eo-evnfm-library-chart.fsGroup" . -}}
{{- end -}}

{{/*
DR-D470222-010
Configuration of Log Collection Streaming Method
*/}}
{{- define "eric-vnfm-orchestrator-service.log.streamingMethod" -}}
  {{- include "eric-eo-evnfm-library-chart.log.streamingMethod" . -}}
{{- end }}

{{/*
Istio excludeOutboundPorts. Outbound ports to be excluded from redirection to Envoy.
*/}}
{{- define "eric-vnfm-orchestrator-service.excludeOutboundPorts" -}}
  {{- include "eric-eo-evnfm-library-chart.excludeOutboundPorts" . -}}
{{- end -}}

{{/*
DR-D1123-134
Generation of role bindings for admission control in OpenShift environment
*/}}
{{- define "eric-vnfm-orchestrator-service.serviceAccount.name" -}}
  {{- printf "%s-sa" (include "eric-vnfm-orchestrator-service.name" .) | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
DR-D1123-134
Rolekind parameter for generation of role bindings for admission control in OpenShift environment
*/}}
{{- define "eric-vnfm-orchestrator-service.securityPolicy.rolekind" }}
  {{- include "eric-eo-evnfm-library-chart.securityPolicy.rolekind" . }}
{{- end }}

{{/*
DR-D1123-134
Rolename parameter for generation of role bindings for admission control in OpenShift environment
*/}}
{{- define "eric-vnfm-orchestrator-service.securityPolicy.rolename" }}
  {{- include "eric-eo-evnfm-library-chart.securityPolicy.rolename" . }}
{{- end }}

{{/*
DR-D1123-134
RoleBinding name for generation of role bindings for admission control in OpenShift environment
*/}}
{{- define "eric-vnfm-orchestrator-service.securityPolicy.rolebinding.name" }}
  {{- include "eric-eo-evnfm-library-chart.securityPolicy.rolebinding.name" . }}
{{- end }}

{{/*
build vhost for envoy filter
*/}}
{{- define "eric-vnfm-orchestrator-service.envoy.retryAfter.vhost" }}
   {{- $serviceName := .Values.highAvailability.serviceMesh.envoyFilter.wfs.serviceName  -}}
   {{- $namespace := .Release.Namespace -}}
   {{- $port := .Values.highAvailability.serviceMesh.envoyFilter.wfs.port -}}
   {{- printf "%s.%s.svc.cluster.local:%v" $serviceName $namespace $port -}}
{{- end }}
