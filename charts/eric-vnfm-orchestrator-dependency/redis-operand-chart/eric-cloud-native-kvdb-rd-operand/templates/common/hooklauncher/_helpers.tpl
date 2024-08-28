{{/*
Name to be used for hooklauncher resources.
*/}}
{{- define "eric-cloud-native-kvdb-rd-operand.hkln.name" -}}
{{ printf "%s-hkln" (include "eric-cloud-native-kvdb-rd-operand.name" .) }}
{{- end }}

{{/*
Name to be used for the secret storing jobs managed by hooklauncher.
*/}}
{{- define "eric-cloud-native-kvdb-rd-operand.hkln.job-inventory-secret-name" -}}
{{ printf "%s-inventory" (include "eric-cloud-native-kvdb-rd-operand.hkln.name" .) }}
{{- end }}

{{/*
Name to be used for hooklauncher job container.
*/}}
{{- define "eric-cloud-native-kvdb-rd-operand.hkln.containerName" -}}
{{ printf "hooklauncher" }}
{{- end -}}

{{/*
The hooklauncher image path
*/}}
{{- define "eric-cloud-native-kvdb-rd-operand.hkln.image-path" }}
  {{- $productInfo := fromYaml (.Files.Get "eric-product-info.yaml") -}}
  {{- $registryUrl := $productInfo.images.hooklauncher.registry -}}
  {{- $repoPath := $productInfo.images.hooklauncher.repoPath -}}
  {{- $name := $productInfo.images.hooklauncher.name -}}
  {{- $tag := $productInfo.images.hooklauncher.tag -}}
  {{- if .Values.global -}}
      {{- if .Values.global.registry -}}
          {{- if .Values.global.registry.url -}}
              {{- $registryUrl = .Values.global.registry.url -}}
          {{- end -}}
      {{- end -}}
  {{- end -}}
  {{- if .Values.imageCredentials -}}
      {{- if .Values.imageCredentials.registry -}}
          {{- if .Values.imageCredentials.registry.url -}}
              {{- $registryUrl = .Values.imageCredentials.registry.url -}}
          {{- end -}}
      {{- end -}}
      {{- if not (kindIs "invalid" .Values.imageCredentials.repoPath) -}}
          {{- $repoPath = .Values.imageCredentials.repoPath -}}
      {{- end -}}
      {{- if .Values.imageCredentials.hooklauncher -}}
          {{- if .Values.imageCredentials.hooklauncher.registry -}}
              {{- if .Values.imageCredentials.hooklauncher.registry.url -}}
                  {{- $registryUrl = .Values.imageCredentials.hooklauncher.registry.url -}}
              {{- end -}}
          {{- end -}}
          {{- if not (kindIs "invalid" .Values.imageCredentials.hooklauncher.repoPath) -}}
              {{- $repoPath = .Values.imageCredentials.hooklauncher.repoPath -}}
          {{- end -}}
      {{- end -}}
  {{- end -}}
  {{- if $repoPath -}}
      {{- $repoPath = printf "%s/" $repoPath -}}
  {{- end -}}
  {{- printf "%s/%s%s:%s" $registryUrl $repoPath $name $tag -}}
{{- end -}}


{{/*
Create version
*/}}
{{- define "eric-cloud-native-kvdb-rd-operand.hkln.version" -}}
{{- printf "%s" .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
Default labels attached to all the k8s objects
*/}}
{{- define "eric-cloud-native-kvdb-rd-operand.hkln.helm-labels" }}
chart: {{ .Chart.Name }}
release: "{{ .Release.Name }}"
app: {{ template "eric-cloud-native-kvdb-rd-operand.hkln.name" . }}
app.kubernetes.io/name: {{ template "eric-cloud-native-kvdb-rd-operand.hkln.name" . }}
app.kubernetes.io/version: {{ template "eric-cloud-native-kvdb-rd-operand.hkln.version" . }}
app.kubernetes.io/instance: "{{ .Release.Name }}"
app.kubernetes.io/managed-by: "{{ .Release.Service }}"
{{- end -}}

{{/*
Common labels
*/}}
{{- define "eric-cloud-native-kvdb-rd-operand.hkln.labels" -}}
  {{- $helmLabels := include "eric-cloud-native-kvdb-rd-operand.hkln.helm-labels" . | fromYaml -}}
  {{- $globalLabels := (.Values.global).labels -}}
  {{- $serviceLabels := .Values.labels -}}
  {{- $staticLabels := dict -}}
  {{- $_ := set $staticLabels "sidecar.istio.io/inject" "false" -}}
  {{- include "eric-cloud-native-kvdb-rd-operand.mergeLabels" (dict "location" .Template.Name "sources" (list $helmLabels $globalLabels $serviceLabels $staticLabels)) | trim }}
{{- end -}}

{{/*
Create annotation for the product information
*/}}
{{- define "eric-cloud-native-kvdb-rd-operand.hkln.product-info" }}
ericsson.com/product-name: {{ (fromYaml (.Files.Get "eric-product-info.yaml")).productName | quote }}
ericsson.com/product-number: {{ (fromYaml (.Files.Get "eric-product-info.yaml")).productNumber | quote }}
ericsson.com/product-revision: {{ regexReplaceAll "(.*)[+|-].*" .Chart.Version "${1}" | quote }}
{{- end }}

{{/*
Common annotations
*/}}
{{- define "eric-cloud-native-kvdb-rd-operand.hkln.annotations" -}}
  {{- $productInfoAnn := include "eric-cloud-native-kvdb-rd-operand.hkln.product-info" . | fromYaml -}}
  {{- $globalAnn := (.Values.global).annotations -}}
  {{- $serviceAnn := .Values.annotations -}}
  {{- include "eric-cloud-native-kvdb-rd-operand.mergeAnnotations" (dict "location" .Template.Name "sources" (list $productInfoAnn $globalAnn $serviceAnn)) | trim }}
{{- end -}}

{{/*
Tell hooklauncher to delete jobs and their pods after execution.
Default value: true
Possible values: true, false, onSuccess (Only deletes the hooklauncher jobs when it is successful)
*/}}
{{- define "eric-cloud-native-kvdb-rd-operand.hkln.cleanup" -}}
{{- $cleanup := "true" -}}
{{- if .Values.hooklauncher -}}
  {{- if has (upper (toString .Values.hooklauncher.cleanup)) (list "TRUE" "FALSE" "ONSUCCESS") -}}
    {{- $cleanup = .Values.hooklauncher.cleanup -}}
  {{- end -}}
{{- end -}}
{{- $cleanup | toString -}}
{{- end -}}

{{/*
Tell hooklauncher to terminate early on failure (e.g. fast-fail).
Default value: true
*/}}
{{- define "eric-cloud-native-kvdb-rd-operand.hkln.terminateEarlyOnFailure" -}}
{{- $terminateEarlyOnFailure := "true" -}}
{{- if .Values.hooklauncher -}}
  {{- if has (toString .Values.hooklauncher.terminateEarlyOnFailure) (list "true" "false") -}}
    {{- $terminateEarlyOnFailure = .Values.hooklauncher.terminateEarlyOnFailure -}}
  {{- end -}}
{{- end -}}
{{- $terminateEarlyOnFailure -}}
{{- end -}}

{{/*
Set backoffLimit for the hooklauncher jobs.
Default value: 6
*/}}
{{- define "eric-cloud-native-kvdb-rd-operand.hkln.backoffLimit" -}}
{{- $backoffLimit := 6 -}}
{{- if (((.Values).hooklauncher).backoffLimit) -}}
  {{- $backoffLimit = .Values.hooklauncher.backoffLimit -}}
{{- end -}}
{{- if eq ((((.Values).hooklauncher).backoffLimit) | toString ) "0" -}}
  {{- $backoffLimit = .Values.hooklauncher.backoffLimit -}}
{{- end -}}
{{- $backoffLimit -}}
{{- end -}}

{{/*
Create hooklauncher resource limits, requests attributes (DR-D1126-005)
*/}}
{{- define "eric-cloud-native-kvdb-rd-operand.hkln.resources" -}}
  {{- $limitsDict := dict -}}
  {{- $_ := set $limitsDict "memory" "100Mi" -}}
  {{- $_ := set $limitsDict "cpu" "50m" -}}
  {{- $_ := set $limitsDict "ephemeral-storage" "100Mi" -}}
  {{- $requestsDict := dict -}}
  {{- $_ := set $requestsDict "memory" "50Mi" -}}
  {{- $_ := set $requestsDict "cpu" "20m" -}}
  {{- $_ := set $requestsDict "ephemeral-storage" "100Mi" -}}

  {{- if .Values.resources -}}
    {{- if .Values.resources.hooklauncher -}}
      {{- if .Values.resources.hooklauncher.limits -}}
        {{- if eq "" (.Values.resources.hooklauncher.limits.memory | toString) -}}
          {{- $_ := unset $limitsDict "memory" -}}
        {{- else if not (kindIs "invalid" .Values.resources.hooklauncher.limits.memory) -}}
          {{- $_ := set $limitsDict "memory" (.Values.resources.hooklauncher.limits.memory) -}}
        {{- end -}}

        {{- if eq "" (.Values.resources.hooklauncher.limits.cpu | toString) -}}
          {{- $_ := unset $limitsDict "cpu" -}}
        {{- else if not (kindIs "invalid" .Values.resources.hooklauncher.limits.cpu) -}}
          {{- $_ := set $limitsDict "cpu" (.Values.resources.hooklauncher.limits.cpu) -}}
        {{- end -}}

        {{- if eq "" (index .Values.resources.hooklauncher.limits "ephemeral-storage" | toString) -}}
          {{- $_ := unset $limitsDict "ephemeral-storage" -}}
        {{- else if not (kindIs "invalid" (index .Values.resources.hooklauncher.limits "ephemeral-storage")) -}}
          {{- $_ := set $limitsDict "ephemeral-storage" (index .Values.resources.hooklauncher.limits "ephemeral-storage") -}}
        {{- end -}}
      {{- end -}}

      {{- if .Values.resources.hooklauncher.requests -}}
        {{- if eq "" (.Values.resources.hooklauncher.requests.memory | toString) -}}
          {{- $_ := unset $requestsDict "memory" -}}
        {{- else if not (kindIs "invalid" .Values.resources.hooklauncher.requests.memory) -}}
          {{- $_ := set $requestsDict "memory" (.Values.resources.hooklauncher.requests.memory) -}}
        {{- end -}}

        {{- if eq "" (.Values.resources.hooklauncher.requests.cpu | toString) -}}
          {{- $_ := unset $requestsDict "cpu" -}}
        {{- else if not (kindIs "invalid" .Values.resources.hooklauncher.requests.cpu) -}}
          {{- $_ := set $requestsDict "cpu" (.Values.resources.hooklauncher.requests.cpu) -}}
        {{- end -}}

        {{- if eq "" (index .Values.resources.hooklauncher.requests "ephemeral-storage" | toString) -}}
          {{- $_ := unset $requestsDict "ephemeral-storage" -}}
        {{- else if not (kindIs "invalid" (index .Values.resources.hooklauncher.requests "ephemeral-storage")) -}}
          {{- $_ := set $requestsDict "ephemeral-storage" (index .Values.resources.hooklauncher.requests "ephemeral-storage") -}}
        {{- end -}}
      {{- end -}}
    {{- end -}}
  {{- end -}}

{{- if $limitsDict }}
limits:
  {{- range $key, $value := $limitsDict }}
  {{ $key }}: {{ $value | quote }}
  {{- end -}}
{{- end }}
{{- if $requestsDict }}
requests:
  {{- range $key, $value := $requestsDict }}
  {{ $key }}: {{ $value | quote }}
  {{- end -}}
{{- end }}
{{- end -}}

{{/*
Create image pull policy, service level parameter takes precedence
*/}}
{{- define "eric-cloud-native-kvdb-rd-operand.hkln.imagePullPolicy" -}}
{{- $pullPolicy := "IfNotPresent" -}}
{{- if .Values.global -}}
    {{- if .Values.global.registry -}}
        {{- if .Values.global.registry.imagePullPolicy -}}
            {{- $pullPolicy = .Values.global.registry.imagePullPolicy -}}
        {{- end -}}
    {{- end -}}
{{- end -}}
{{- if .Values.imageCredentials -}}
  {{- if index .Values.imageCredentials "registry" -}}
    {{- if index .Values.imageCredentials "registry" "imagePullPolicy" -}}
      {{- $pullPolicy = index .Values.imageCredentials "registry" "imagePullPolicy" -}}
    {{- end -}}
  {{- end -}}
  {{- if index .Values.imageCredentials "hooklauncher" -}}
    {{- if index .Values.imageCredentials "hooklauncher" "registry" -}}
      {{- if index .Values.imageCredentials "hooklauncher" "registry" "imagePullPolicy" -}}
        {{- $pullPolicy = index .Values.imageCredentials "hooklauncher" "registry" "imagePullPolicy" -}}
      {{- end -}}
    {{- end -}}
  {{- end -}}
{{- end -}}
{{- print $pullPolicy -}}
{{- end -}}

{{/*
Create image pull secret, service level parameter takes precedence
*/}}
{{- define "eric-cloud-native-kvdb-rd-operand.hkln.pullSecrets" -}}
{{- $pullSecret := "" -}}
{{- if .Values.global -}}
    {{- if .Values.global.pullSecret -}}
        {{- $pullSecret = .Values.global.pullSecret -}}
    {{- end -}}
{{- end -}}
{{- if .Values.imageCredentials -}}
  {{- if .Values.imageCredentials.pullSecret -}}
    {{- $pullSecret = .Values.imageCredentials.pullSecret -}}
  {{- end -}}
{{- end -}}
{{- print $pullSecret -}}
{{- end -}}

{{/*
Create tolerations
*/}}
{{- define "eric-cloud-native-kvdb-rd-operand.hkln.tolerations" -}}
  {{- if .Values.tolerations -}}
    {{- if eq (typeOf .Values.tolerations) ("[]interface {}") -}}
      {{- .Values.tolerations | toYaml -}}
    {{- else if eq (typeOf .Values.tolerations) ("map[string]interface {}") -}}
      {{- if .Values.tolerations.hooklauncher -}}
        {{- .Values.tolerations.hooklauncher | toYaml -}}
      {{- end -}}
    {{- end -}}
  {{- end -}}
{{- end -}}

{{/*
Create priorityClassName (DR-D1126-030)
*/}}
{{- define "eric-cloud-native-kvdb-rd-operand.hkln.priorityClassName" -}}
{{- $podPriorityClassName := "" -}}
{{- if .Values.podPriority -}}
  {{- if (index .Values.podPriority "priorityClassName") -}}
    {{- $podPriorityClassName = (index .Values.podPriority "priorityClassName") -}}
  {{- end -}}
  {{- if (index .Values.podPriority "hooklauncher") -}}
    {{- if (index .Values.podPriority "hooklauncher" "priorityClassName") -}}
      {{- $podPriorityClassName = (index .Values.podPriority "hooklauncher" "priorityClassName") -}}
    {{- end -}}
  {{- end -}}
{{- end -}}
{{- print $podPriorityClassName -}}
{{- end -}}

{{/*
Profile helper function (e.g. appArmor, seccomp)
*/}}
{{- define "eric-cloud-native-kvdb-rd-operand.hkln.profile-helper" -}}
  {{- $profileType := "" -}}
  {{- $localhostProfile := "" -}}
  {{- if index .root.Values .profileKey -}}
    {{- if index .root.Values .profileKey "type" -}}
      {{- $profileType = index .root.Values .profileKey "type" -}}
      {{- if eq $profileType .localhostkey }}
        {{- $localhostProfile = index .root.Values .profileKey "localhostProfile" -}}
      {{- end }}
    {{- end }}

    {{- if index .root.Values .profileKey .containerName -}}
      {{- if index .root.Values .profileKey .containerName "type" -}}
        {{- $profileType = index .root.Values .profileKey .containerName "type" -}}
        {{- $localhostProfile = "" -}}
        {{- if eq $profileType .localhostkey }}
          {{- $localhostProfile = index .root.Values .profileKey .containerName "localhostProfile" -}}
        {{- end }}
      {{- end -}}
    {{- end -}}
  {{- end -}}

  {{- $result := dict -}}
  {{- if $profileType -}}
    {{- $_ := set $result "type" $profileType -}}
  {{- end -}}
  {{- if $localhostProfile -}}
    {{- $_ := set $result "localhostProfile" $localhostProfile -}}
  {{- end -}}

  {{- range $key, $value := $result }}
{{ $key }}: {{ $value | quote }}
  {{- end -}}
{{- end -}}

{{/*
Create appArmorProfile (DR-D1123-127) (as annotations, currently in beta)
NOTE: We use this as annotations until K8s supports it as first-class fields.
*/}}
{{- define "eric-cloud-native-kvdb-rd-operand.hkln.appArmorProfileAnn" -}}
  {{- $result := include "eric-cloud-native-kvdb-rd-operand.hkln.profile-helper" (dict "root" .root "profileKey" "appArmorProfile" "localhostkey" "localhost" "containerName" .containerName) | fromYaml -}}

  {{- if $result -}}
    {{- $profileRef := index $result "type" -}}
    {{- $localhostProfile := index $result "localhostProfile" -}}

    {{- if eq $profileRef "RuntimeDefault" -}}
      {{- $profileRef = "runtime/default" -}}
    {{- else if eq $profileRef "Unconfined" -}}
      {{- $profileRef = "unconfined" -}}
    {{- else if eq $profileRef "localhost" -}}
      {{- if $localhostProfile -}}
        {{- $profileRef = printf "localhost/%s" $localhostProfile -}}
      {{- end -}}
    {{- end -}}

    {{- printf "container.apparmor.security.beta.kubernetes.io/%s: %s" .containerName $profileRef -}}
  {{- end -}}
{{- end -}}

{{/*
Create seccompProfile (DR-D1123-128)
*/}}
{{- define "eric-cloud-native-kvdb-rd-operand.hkln.seccompProfile" -}}
{{- if eq .Scope "Pod" -}}
{{- include "eric-cloud-native-kvdb-rd-operand.hkln.profile-helper" (dict "root" .root "profileKey" "seccompProfile" "localhostkey" "Localhost" "containerName" "") -}}
{{- else -}}
{{- include "eric-cloud-native-kvdb-rd-operand.hkln.profile-helper" (dict "root" .root "profileKey" "seccompProfile" "localhostkey" "Localhost" "containerName" .Scope) -}}
{{- end -}}
{{- end -}}

{{- define "eric-cloud-native-kvdb-rd-operand.hkln.securityPolicy.reference" -}}
  {{- if .Values.global -}}
    {{- if .Values.global.security -}}
      {{- if .Values.global.security.policyReferenceMap -}}
        {{ $mapped := index .Values "global" "security" "policyReferenceMap" "default-restricted-security-policy" }}
        {{- if $mapped -}}
          {{ $mapped }}
        {{- else -}}
          default-restricted-security-policy
        {{- end -}}
      {{- else -}}
        default-restricted-security-policy
      {{- end -}}
    {{- else -}}
      default-restricted-security-policy
    {{- end -}}
  {{- else -}}
    default-restricted-security-policy
  {{- end -}}
{{- end -}}

{{/*
Create timezone
*/}}
{{- define "eric-cloud-native-kvdb-rd-operand.hkln.timezone" -}}
{{- $timezone := "UTC" -}}
{{- if .Values.global -}}
    {{- if .Values.global.timezone -}}
        {{- $timezone = .Values.global.timezone -}}
    {{- end -}}
{{- end -}}
{{- print $timezone -}}
{{- end -}}

{{/*
Create a merged set of nodeSelectors from global and service level.
*/}}
{{- define "eric-cloud-native-kvdb-rd-operand.hkln.nodeSelector" -}}
  {{- $hkln := (.Values.nodeSelector).hooklauncher -}}
  {{- $global := (.Values.global).nodeSelector -}}
  {{- include "eric-cloud-native-kvdb-rd-operand.aggregatedMerge" (dict "context" "nodeSelector" "location" .Template.Name "sources" (list $global $hkln)) -}}
{{- end -}}

{{/*
Create maxEgressRate (DR-D1125-040-AD)
*/}}
{{- define "eric-cloud-native-kvdb-rd-operand.hkln.maxEgressRate" -}}
{{- $maxEgressRate := "" -}}
{{- if .Values.bandwidth -}}
  {{- if (index .Values.bandwidth "hooklauncher") -}}
    {{- if (index .Values.bandwidth "hooklauncher" "maxEgressRate") -}}
      {{- $maxEgressRate = (index .Values.bandwidth "hooklauncher" "maxEgressRate") -}}
    {{- end -}}
  {{- end -}}
{{- end -}}
{{- print $maxEgressRate -}}
{{- end -}}
