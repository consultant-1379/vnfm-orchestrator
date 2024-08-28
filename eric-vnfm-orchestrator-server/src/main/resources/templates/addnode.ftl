<#--

    COPYRIGHT Ericsson 2024



    The copyright to the computer program(s) herein is the property of

    Ericsson Inc. The programs may be used and/or copied only with written

    permission from Ericsson Inc. or in accordance with the terms and

    conditions stipulated in the agreement/contract under which the

    program(s) have been supplied.

-->
import sys
<#import "commons.ftl" as commons>
<#assign nodeMO = "NetworkElement=${managedElementId}">
# Function definition
<@commons.logging logFileName = '${operation}'/>
<@commons.checkCmdOutput/>
<@commons.executeCommand/>
<@commons.printData item = '${operationResponse}'/>
def deleteNodeDataOnFailure():
    "Delete node on failure"
    # Delete Node data
    deleteCommand1 = 'cmedit delete ${nodeMO} -ALL --force'
    deleteCmdExitStatus1,delCmdOutput1 = executeCommand(deleteCommand1)
    ${operationResponse}["deleteNodeData"]["exitStatus"] = deleteCmdExitStatus1
    ${operationResponse}["deleteNodeData"]["commandOutput"] = delCmdOutput1
    printData()
    sys.exit(1)
def initializeOutput():
    createNetworkMOStatus = {}
    createNetworkMOStatus["exitStatus"] = None
    createNetworkMOStatus["commandOutput"] = None
    ${operationResponse}['createNetworkMOStatus'] = createNetworkMOStatus
    createConnectionInfoMOStatus = {}
    createConnectionInfoMOStatus["exitStatus"] = None
    createConnectionInfoMOStatus["commandOutput"] = None
    ${operationResponse}['createConnectionInfoMOStatus'] = createConnectionInfoMOStatus
    createCredentialsStatus = {}
    createCredentialsStatus["exitStatus"] = None
    createCredentialsStatus["commandOutput"] = None
    ${operationResponse}['createCredentialsStatus'] = createCredentialsStatus
    snmpV3CredentialsStatus = {}
    snmpV3CredentialsStatus["exitStatus"] = None
    snmpV3CredentialsStatus["commandOutput"] = None
    ${operationResponse}['snmpV3CredentialsStatus'] = snmpV3CredentialsStatus
    pmSupervisionStatus = {}
    pmSupervisionStatus["exitStatus"] = None
    pmSupervisionStatus["commandOutput"] = None
    ${operationResponse}['pmSupervisionStatus'] = pmSupervisionStatus
    cmSupervisionStatus = {}
    cmSupervisionStatus["exitStatus"] = None
    cmSupervisionStatus["commandOutput"] = None
    ${operationResponse}['cmSupervisionStatus'] = cmSupervisionStatus
    alarmSupervisionStatus = {}
    alarmSupervisionStatus["exitStatus"] = None
    alarmSupervisionStatus["commandOutput"] = None
    ${operationResponse}['alarmSupervisionStatus'] = alarmSupervisionStatus
    fdn = {}
    fdn["exitStatus"] = None
    fdn["commandOutput"] = None
    ${operationResponse}['fdn'] = fdn
    deleteNodeData = {}
    deleteNodeData["exitStatus"] = None
    deleteNodeData["commandOutput"] = None
    ${operationResponse}['deleteNodeData'] = deleteNodeData
    virtualNetworkFunctionData = {}
    virtualNetworkFunctionData["exitStatus"] = None
    virtualNetworkFunctionData["commandOutput"] = None
    ${operationResponse}['virtualNetworkFunctionData'] = virtualNetworkFunctionData
    associateVnfmVnf = {}
    associateVnfmVnf["exitStatus"] = None
    associateVnfmVnf["commandOutput"] = None
    ${operationResponse}['associateVnfmVnf'] = associateVnfmVnf
<@commons.startScriptingSession/>
${operationResponse} = {}
initializeOutput()
vnfDataCmdExitStatus = None
associationExitStatus = None

# Creating the NetworkElement MO
command1 = 'cmedit create ${nodeMO} networkElementId=${managedElementId},neType=${networkElementType}<#if cnfType?? && cnfType?trim != "">,cnfType=${cnfType}</#if>,ossPrefix="<#if subNetworks?? && subNetworks?trim != "">${subNetworks},</#if>MeContext=${managedElementId}"<#if networkElementVersion?? && networkElementVersion?trim != "">,ossModelIdentity=${networkElementVersion}</#if><#if timeZone?? && timeZone?trim != "">,timeZone=${timeZone}</#if>'
cmdExitStatus1,cmdOutput1 = executeCommand(command1)
${operationResponse}["createNetworkMOStatus"]["exitStatus"] = cmdExitStatus1
${operationResponse}["createNetworkMOStatus"]["commandOutput"] = cmdOutput1
# creating the ConnectivityInformation MO
if(cmdExitStatus1 is not None and cmdExitStatus1 == 0):
<#if !cbpOiConnectivityInformationModelVersion?? >
    <#assign cbpOiConnectivityInformationModelVersion = '1.0.0'>
</#if>
<#if snmpVersion?? && (snmpVersion == "SNMP_V3")>
    command2 = 'cmedit create ${nodeMO},CbpOiConnectivityInformation="1" CbpOiConnectivityInformationId="1", ipAddress="${nodeIpAddress}", transportProtocol="<#if transportProtocol?? && transportProtocol?trim!= "">${transportProtocol}<#else>SSH</#if>", port=${netConfPort},snmpVersion="SNMP_V3", snmpAgentPort=<#if snmpPort?? && snmpPort?trim != "">${snmpPort}<#else>161</#if>, snmpSecurityName="${snmpSecurityName}", snmpSecurityLevel=${snmpSecurityLevel} -ns=CBPOI_MED -version=${cbpOiConnectivityInformationModelVersion}'
<#else>
    command2 = 'cmedit create ${nodeMO},CbpOiConnectivityInformation="1" CbpOiConnectivityInformationId="1", ipAddress="${nodeIpAddress}", transportProtocol="<#if transportProtocol?? && transportProtocol?trim!= "">${transportProtocol}<#else>SSH</#if>", port=${netConfPort},snmpAgentPort=<#if snmpPort?? && snmpPort?trim!= "">${snmpPort}<#else>161</#if>, snmpReadCommunity="${communityString}", snmpWriteCommunity="${communityString}" -ns=CBPOI_MED -version=${cbpOiConnectivityInformationModelVersion}'
</#if>
    cmdExitStatus2,cmdOutput2 = executeCommand(command2)
    ${operationResponse}["createConnectionInfoMOStatus"]["exitStatus"] = cmdExitStatus2
    ${operationResponse}["createConnectionInfoMOStatus"]["commandOutput"] = cmdOutput2
    if(cmdExitStatus2 is not None and cmdExitStatus2 == 0):
    <#if tenant?? && tenant?trim!= "">
        # creating VirtualNetworkFunctionData
        vnfcommand = 'cmedit create ${nodeMO},VirtualNetworkFunctionData=1 virtualNetworkFunctionDataId=1,tenant="${tenant}",vnfInstanceId="${vnfInstanceId}" -ns OSS_NE_DEF -v 1.0.0'
        vnfDataCmdExitStatus,vnfDataOutput = executeCommand(vnfcommand)
        ${operationResponse}["virtualNetworkFunctionData"]["exitStatus"] = vnfDataCmdExitStatus
        ${operationResponse}["virtualNetworkFunctionData"]["commandOutput"] = vnfDataOutput
     </#if>
    <#if vnfmName?? && vnfmName?trim != "" && !smallStackApplication>
        # associate vnfm with the vnf only for fullStack application
        vnfmassociate = 'cmedit action ${nodeMO},VirtualNetworkFunctionData=1 attach.(virtualManager="VirtualNetworkFunctionManager=${vnfmName}")'
        associationExitStatus,associationOutput = executeCommand(vnfmassociate)
        ${operationResponse}["associateVnfmVnf"]["exitStatus"] = associationExitStatus
        ${operationResponse}"associateVnfmVnf"]["commandOutput"] = associationOutput
    </#if>
        if((vnfDataCmdExitStatus is None or vnfDataCmdExitStatus == 0) and (associationExitStatus is None or associationExitStatus == 0)):
            # Create Credentials on the Nodes.
            command3 = 'secadm credentials create --secureusername ${networkElementUsername} --secureuserpassword "${networkElementPassword}" -n ${managedElementId}'
            cmdExitStatus3,cmdOutput3 = executeCommand(command3)
            ${operationResponse}["createCredentialsStatus"]["exitStatus"] = cmdExitStatus3
            ${operationResponse}["createCredentialsStatus"]["commandOutput"] = cmdOutput3
            <#if snmpVersion?? && (snmpVersion == "SNMP_V3") && snmpSecurityLevel?? && (snmpSecurityLevel == "AUTH_NO_PRIV")>
            if(cmdExitStatus3 is not None and cmdExitStatus3 == 0):
                # Configure permitted level of security within a security model
                command4 = 'secadm snmp authnopriv --auth_algo ${snmpAuthProtocol} --auth_password ${snmpAuthPassword} -n ${managedElementId}'
                cmdExitStatus4,cmdOutput4 = executeCommand(command4)
                ${operationResponse}["snmpV3CredentialsStatus"]["exitStatus"] = cmdExitStatus4
                ${operationResponse}["snmpV3CredentialsStatus"]["commandOutput"] = cmdOutput4
                if(cmdExitStatus4 is None and cmdExitStatus4 == 1):
                    deleteNodeDataOnFailure()
            else:
                deleteNodeDataOnFailure()
            <#elseif snmpVersion?? && (snmpVersion == "SNMP_V3") &&  snmpSecurityLevel?? && (snmpSecurityLevel == "AUTH_PRIV")>
            if(cmdExitStatus3 is not None and cmdExitStatus3 == 0):
                # Configure authpriv on the Node
                command4 = 'secadm snmp authpriv --auth_algo ${snmpAuthProtocol} --auth_password ${snmpAuthPassword} --priv_algo ${snmpPrivProtocol} --priv_password ${snmpPrivacyPassword} -n ${managedElementId}'
                cmdExitStatus4,cmdOutput4 = executeCommand(command4)
                ${operationResponse}["snmpV3CredentialsStatus"]["exitStatus"] = cmdExitStatus4
                ${operationResponse}["snmpV3CredentialsStatus"]["commandOutput"] = cmdOutput4
                if(cmdExitStatus4 is None and cmdExitStatus4 == 1):
                    deleteNodeDataOnFailure()
            else:
                deleteNodeDataOnFailure()
            </#if>
        else:
            deleteNodeDataOnFailure()
    else:
        deleteNodeDataOnFailure()
else:
    printData()
    sys.exit(1)
printData()
# Closing ENM scripting session
enmscripting.close(session)