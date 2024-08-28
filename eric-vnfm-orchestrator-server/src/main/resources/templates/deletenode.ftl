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

def initializeOutput():
    pmSupervisionStatus = {"exitStatus": None, "commandOutput": None}
    ${operationResponse}['pmSupervisionStatus'] = pmSupervisionStatus

    cmSupervisionStatus = {"exitStatus": None, "commandOutput": None}
    ${operationResponse}['cmSupervisionStatus'] = cmSupervisionStatus

    cmNodeSupervisionStatus = {"exitStatus": None, "commandOutput": None}
    ${operationResponse}['cmNodeSupervisionStatus'] = cmNodeSupervisionStatus

    inventorySupervisionStatus = {"exitStatus": None, "commandOutput": None}
    ${operationResponse}['inventorySupervisionStatus'] = inventorySupervisionStatus

    fmAlarmSupervisionStatus = {"exitStatus": None, "commandOutput": None}
    ${operationResponse}['fmAlarmSupervisionStatus'] = fmAlarmSupervisionStatus

    deleteNrmData = {"exitStatus": None, "commandOutput": None}
    ${operationResponse}['deleteNrmData'] = deleteNrmData

    deleteNodeData = {"exitStatus": None, "commandOutput": None}
    ${operationResponse}['deleteNodeData'] = deleteNodeData

<@commons.startScriptingSession/>

${operationResponse} = {}
initializeOutput()


# Disable PM Node Supervision
command1 = 'cmedit set ${nodeMO},PmFunction=1 pmEnabled=false'
${operationResponse}["pmSupervisionStatus"]["exitStatus"],${operationResponse}["pmSupervisionStatus"]["commandOutput"] = executeCommand(command1)

# Disable CM Node Supervision
command2 = 'cmedit set ${nodeMO},CmNodeHeartbeatSupervision=1 active=false'
${operationResponse}["cmNodeSupervisionStatus"]["exitStatus"],${operationResponse}["cmNodeSupervisionStatus"]["commandOutput"] = executeCommand(command2)

# Disable Inventory Supervision
command3 = 'cmedit set ${nodeMO},InventorySupervision=1 active=false'
${operationResponse}["inventorySupervisionStatus"]["exitStatus"] ,${operationResponse}["inventorySupervisionStatus"]["commandOutput"]  = executeCommand(command3)

# Disable CM Supervision
command4 = 'cmedit set ${nodeMO},CmNodeHeartbeatSupervision=1 active=false'
${operationResponse}["cmSupervisionStatus"]["exitStatus"],${operationResponse}["cmSupervisionStatus"]["commandOutput"] = executeCommand(command4)

# Disable FM Alarm Supervision
command5 = 'alarm disable ${nodeMO}'
${operationResponse}["fmAlarmSupervisionStatus"]["exitStatus"],${operationResponse}["fmAlarmSupervisionStatus"]["commandOutput"] = executeCommand(command5)

# Delete NRM data
command6 = 'cmedit action ${nodeMO},CmFunction=1 deleteNrmDataFromEnm'
${operationResponse}["deleteNrmData"]["exitStatus"],${operationResponse}["deleteNrmData"]["commandOutput"] = executeCommand(command6)

# Delete Node data
command7 = 'cmedit delete ${nodeMO} -ALL'
${operationResponse}["deleteNodeData"]["exitStatus"],${operationResponse}["deleteNodeData"]["commandOutput"] = executeCommand(command7)

printData()

# Closing ENM scripting session
enmscripting.close(session)
