{{- define "eric-cloud-native-kvdb-rd-operand.hkln.job" -}}
{{- $containerName := include "eric-cloud-native-kvdb-rd-operand.hkln.containerName" . -}}
apiVersion: batch/v1
kind: Job
metadata:
  name: {{ template "eric-cloud-native-kvdb-rd-operand.hkln.name" .root }}-{{ .suffix }}
  labels:
    {{- include "eric-cloud-native-kvdb-rd-operand.hkln.labels" .root | nindent 4 }}
  annotations:
    {{- $helmHook := dict -}}
    {{- $_ := set $helmHook "helm.sh/hook" .helmHook -}}
    {{- $_ := set $helmHook "helm.sh/hook-weight" .weight -}}
    {{- $_ := set $helmHook "helm.sh/hook-delete-policy" "before-hook-creation,hook-succeeded" -}}
    {{- $commonAnn := fromYaml (include "eric-cloud-native-kvdb-rd-operand.hkln.annotations" .root) -}}
    {{- include "eric-cloud-native-kvdb-rd-operand.mergeAnnotations" (dict "location" .root.Template.Name "sources" (list $helmHook $commonAnn)) | trim | nindent 4 }}
spec:
  template:
    metadata:
      labels:
        {{- include "eric-cloud-native-kvdb-rd-operand.hkln.labels" .root | nindent 8 }}
      annotations:
        {{- $appArmorAnn := include "eric-cloud-native-kvdb-rd-operand.hkln.appArmorProfileAnn" (dict "root" .root "containerName" $containerName) | fromYaml -}}
        {{- $egressAnn := dict -}}
        {{- if include "eric-cloud-native-kvdb-rd-operand.hkln.maxEgressRate" .root }}
        {{- $_ := set $egressAnn "kubernetes.io/egress-bandwidth" (include "eric-cloud-native-kvdb-rd-operand.hkln.maxEgressRate" .root) -}}
        {{ end }}
        {{- $commonAnn := fromYaml (include "eric-cloud-native-kvdb-rd-operand.hkln.annotations" .root) -}}
        {{- include "eric-cloud-native-kvdb-rd-operand.mergeAnnotations" (dict "location" .root.Template.Name "sources" (list $appArmorAnn $egressAnn $commonAnn)) | trim | nindent 8 }}
    spec:
      {{- if include "eric-cloud-native-kvdb-rd-operand.hkln.pullSecrets" .root }}
      imagePullSecrets:
        - name: {{ template "eric-cloud-native-kvdb-rd-operand.hkln.pullSecrets" .root }}
      {{- end }}
      containers:
        - name: {{ $containerName }}
          image: {{ include "eric-cloud-native-kvdb-rd-operand.hkln.image-path" .root }}
          env:
            - name: TZ
              value: {{ include "eric-cloud-native-kvdb-rd-operand.hkln.timezone" .root }}
          args: [
            "/hooklauncher/hooklauncher",
            "--namespace", {{ .root.Release.Namespace | quote }},
            "--job-inventory-secret", {{ include "eric-cloud-native-kvdb-rd-operand.hkln.job-inventory-secret-name" .root | quote }},
            "--instance", {{ include "eric-cloud-native-kvdb-rd-operand.hkln.name" .root | quote }},
            "--this-version", {{ .root.Chart.Version | quote }},
            "--trigger", {{ .trigger | quote }},
            "--cleanup", {{ include "eric-cloud-native-kvdb-rd-operand.hkln.cleanup" .root | quote }},
            "--terminate-early={{ template "eric-cloud-native-kvdb-rd-operand.hkln.terminateEarlyOnFailure" .root }}",
            "--incluster"
          ]
          imagePullPolicy: {{ template "eric-cloud-native-kvdb-rd-operand.hkln.imagePullPolicy" .root }}
          {{- if include "eric-cloud-native-kvdb-rd-operand.hkln.resources" .root }}
          resources:
            {{- include "eric-cloud-native-kvdb-rd-operand.hkln.resources" .root | trim | nindent 12 }}
          {{- end }}
          securityContext:
            allowPrivilegeEscalation: false
            privileged: false
            readOnlyRootFilesystem: true
            runAsNonRoot: true
            capabilities:
              drop:
                - all
            {{- if include "eric-cloud-native-kvdb-rd-operand.hkln.seccompProfile" (dict "root" .root "Scope" $containerName) }}
            seccompProfile:
              {{- include "eric-cloud-native-kvdb-rd-operand.hkln.seccompProfile" (dict "root" .root "Scope" $containerName) | trim | nindent 14 }}
            {{- end }}
      restartPolicy: OnFailure
      serviceAccountName: {{ template "eric-cloud-native-kvdb-rd-operand.hkln.name" .root }}
      {{- if include "eric-cloud-native-kvdb-rd-operand.hkln.priorityClassName" .root }}
      priorityClassName: {{ include "eric-cloud-native-kvdb-rd-operand.hkln.priorityClassName" .root }}
      {{- end }}
      {{- if include "eric-cloud-native-kvdb-rd-operand.hkln.tolerations" .root }}
      tolerations: {{- include "eric-cloud-native-kvdb-rd-operand.hkln.tolerations" .root | nindent 6 }}
      {{- end }}
      {{- if include "eric-cloud-native-kvdb-rd-operand.hkln.nodeSelector" .root }}
      nodeSelector: {{- include "eric-cloud-native-kvdb-rd-operand.hkln.nodeSelector" .root | trim | nindent 8 }}
      {{- end }}
      {{- if include "eric-cloud-native-kvdb-rd-operand.hkln.seccompProfile" (dict "root" .root "Scope" "Pod") }}
      securityContext:
        seccompProfile:
          {{- include "eric-cloud-native-kvdb-rd-operand.hkln.seccompProfile" (dict "root" .root "Scope" "Pod") | trim | nindent 10 }}
      {{- end }}
  backoffLimit: {{ template "eric-cloud-native-kvdb-rd-operand.hkln.backoffLimit" .root | default 6 }}
{{- end -}}
