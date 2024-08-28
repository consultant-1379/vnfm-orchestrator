UPDATE APP_VNF_INSTANCE SET temp_instance = '{"vnfInstanceId":"13143285-ced7-4e67-a55b-8236c88de4ea","vnfInstanceName":"msg-multi-chart-chg","vnfInstanceDescription":"","vnfDescriptorId":"68aea56b-14e8-4850-b2a6-48f785042127","vnfProviderName":"Ericsson","vnfProductName":"SGSN-MME","vnfSoftwareVersion":"1.20(CXS101289_R81E08)","vnfdVersion":"cxp9025898_4r81e08","vnfPackageId":"68aea56b-14e8-4850-b2a6-48f785042127","instantiationState":"INSTANTIATED","clusterName":"default","namespace":"test","helmCharts":[{"id":null,"helmChartUrl":"sample-helm1.tgz","priority":1,"releaseName":"end-to-end-1","state":null}],"operationOccurrenceId":"521ecc62-420d-49bd-aa7d-705dd926e6e1","allOperation":[{"operationOccurrenceId":"924f7503-b8f9-45ee-8915-45312771f389","operationState":"COMPLETED","grantId":null,"lifecycleOperationType":"INSTANTIATE","automaticInvocation":false,"operationParams":"{\"flavourId\":null,\"instantiationLevelId\":null,\"clusterName\":null,\"extVirtualLinks\":null,\"extManagedVirtualLinks\":null,\"localizationLanguage\":null,\"additionalParams\":{\"namespace\":\"my-namespace\"}}","cancelPending":false,"error":null,"valuesFileParams":null,"vnfSoftwareVersion":"1.20(CXS101289_R81E08)","vnfProductName":"SGSN-MME","cancelModeType":null},{"operationOccurrenceId":"334a3ce0-ec57-44c5-a397-8ab34e3f367a","operationState":"STARTING","grantId":null,"lifecycleOperationType":"CHANGE_PACKAGE_INFO","automaticInvocation":false,"operationParams":"{\"vnfdId\":\"3d02c5c9-7a9b-48da-8ceb-46fcc83f584c\",\"additionalParams\":{}}","cancelPending":false,"error":null,"valuesFileParams":null,"vnfSoftwareVersion":"1.20(CXS101289_R81E08)","vnfProductName":"SGSN-MME","cancelModeType":null}],"ossTopology":"{\"disableLdapUser\":{\"type\":\"boolean\",\"required\":\"false\"}}","instantiateOssTopology":"{\"disableLdapUser\":{\"type\":\"boolean\",\"required\":\"false\"}}","addNodeOssTopology":null,"addedToOss":false,"addNodePythonFile":null,"deleteNodePythonFile":null,"combinedValuesFile":null,"combinedAdditionalParams":"{}","policies":null,"resourceDetails":"","scaleInfoEntity":null,"manoControlledScaling":null,"upgradePackageDetails":null,"allStringValuesAsArray":null}'
WHERE vnf_id = '13143285-ced7-4e67-a55b-8236c88de4ea';

UPDATE APP_VNF_INSTANCE SET temp_instance = '{"vnfInstanceId":"865e3873-6a0e-443c-9b0c-4da9d9c2ab71","vnfInstanceName":"msg-chg-complete","vnfInstanceDescription":"","vnfDescriptorId":"68aea56b-14e8-4850-b2a6-48f785042127","vnfProviderName":"Ericsson","vnfProductName":"SGSN-MME","vnfSoftwareVersion":"1.20(CXS101289_R81E08)","vnfdVersion":"cxp9025898_4r81e08","vnfPackageId":"68aea56b-14e8-4850-b2a6-48f785042127","instantiationState":"INSTANTIATED","clusterName":"default","namespace":"test","helmCharts":[{"id":null,"helmChartUrl":"sample-helm1.tgz","priority":1,"releaseName":"end-to-end-1","state":null}],"operationOccurrenceId":"865e3873-6a0e-443c-9b0c-4da9d9c2ab71","allOperation":[{"operationOccurrenceId":"924f7503-b8f9-45ee-8915-45312771f389","operationState":"COMPLETED","grantId":null,"lifecycleOperationType":"INSTANTIATE","automaticInvocation":false,"operationParams":"{\"flavourId\":null,\"instantiationLevelId\":null,\"clusterName\":null,\"extVirtualLinks\":null,\"extManagedVirtualLinks\":null,\"localizationLanguage\":null,\"additionalParams\":{\"namespace\":\"my-namespace\"}}","cancelPending":false,"error":null,"valuesFileParams":null,"vnfSoftwareVersion":"1.20(CXS101289_R81E08)","vnfProductName":"SGSN-MME","cancelModeType":null},{"operationOccurrenceId":"334a3ce0-ec57-44c5-a397-8ab34e3f367a","operationState":"STARTING","grantId":null,"lifecycleOperationType":"CHANGE_PACKAGE_INFO","automaticInvocation":false,"operationParams":"{\"vnfdId\":\"3d02c5c9-7a9b-48da-8ceb-46fcc83f584c\",\"additionalParams\":{}}","cancelPending":false,"error":null,"valuesFileParams":null,"vnfSoftwareVersion":"1.20(CXS101289_R81E08)","vnfProductName":"SGSN-MME","cancelModeType":null}],"ossTopology":"{\"disableLdapUser\":{\"type\":\"boolean\",\"required\":\"false\"}}","instantiateOssTopology":"{\"disableLdapUser\":{\"type\":\"boolean\",\"required\":\"false\"}}","addNodeOssTopology":null,"addedToOss":false,"addNodePythonFile":null,"deleteNodePythonFile":null,"combinedValuesFile":null,"combinedAdditionalParams":"{}","policies":null,"resourceDetails":"","scaleInfoEntity":null,"manoControlledScaling":null,"upgradePackageDetails":null,"allStringValuesAsArray":null}'
WHERE vnf_id = '865e3873-6a0e-443c-9b0c-4da9d9c2ab71';

UPDATE APP_VNF_INSTANCE SET temp_instance = '{"vnfInstanceId":"837dbdb3-2240-4d3b-b840-c9deafa98c83","vnfInstanceName":"msg-chg-complete-a","vnfInstanceDescription":"","vnfDescriptorId":"68aea56b-14e8-4850-b2a6-48f785042127","vnfProviderName":"Ericsson","vnfProductName":"SGSN-MME","vnfSoftwareVersion":"1.20(CXS101289_R81E08)","vnfdVersion":"cxp9025898_4r81e08","vnfPackageId":"68aea56b-14e8-4850-b2a6-48f785042127","instantiationState":"INSTANTIATED","clusterName":"default","namespace":"test","helmCharts":[{"id":"e264eeca-3450-478e-9ed6-0a70448b710f","helmChartUrl":"https://arm.epk.ericsson.se/artifactory/proj-am-helm-local/acceptance-test/spider-app-2.74.6.tgz","priority":1,"releaseName":"msg-chg-complete-a","state":null}],"operationOccurrenceId":"837dbdb3-2240-4d3b-b840-c9deafa98c83","allOperation":[{"operationOccurrenceId":"924f7503-b8f9-45ee-8915-45312771f389","operationState":"COMPLETED","grantId":null,"lifecycleOperationType":"INSTANTIATE","automaticInvocation":false,"operationParams":"{\"flavourId\":null,\"instantiationLevelId\":null,\"clusterName\":null,\"extVirtualLinks\":null,\"extManagedVirtualLinks\":null,\"localizationLanguage\":null,\"additionalParams\":{\"namespace\":\"my-namespace\"}}","cancelPending":false,"error":null,"valuesFileParams":null,"vnfSoftwareVersion":"1.20(CXS101289_R81E08)","vnfProductName":"SGSN-MME","cancelModeType":null},{"operationOccurrenceId":"334a3ce0-ec57-44c5-a397-8ab34e3f367a","operationState":"STARTING","grantId":null,"lifecycleOperationType":"CHANGE_PACKAGE_INFO","automaticInvocation":false,"operationParams":"{\"vnfdId\":\"3d02c5c9-7a9b-48da-8ceb-46fcc83f584c\",\"additionalParams\":{}}","cancelPending":false,"error":null,"valuesFileParams":null,"vnfSoftwareVersion":"1.20(CXS101289_R81E08)","vnfProductName":"SGSN-MME","cancelModeType":null}],"ossTopology":"{\"disableLdapUser\":{\"type\":\"boolean\",\"required\":\"false\"}}","instantiateOssTopology":"{\"disableLdapUser\":{\"type\":\"boolean\",\"required\":\"false\"}}","addNodeOssTopology":null,"addedToOss":false,"addNodePythonFile":null,"deleteNodePythonFile":null,"combinedValuesFile":null,"combinedAdditionalParams":"{}","policies":null,"resourceDetails":"","scaleInfoEntity":null,"manoControlledScaling":null,"upgradePackageDetails":null,"allStringValuesAsArray":null}'
WHERE vnf_id = '837dbdb3-2240-4d3b-b840-c9deafa98c83';