<#--

    COPYRIGHT Ericsson 2024



    The copyright to the computer program(s) herein is the property of

    Ericsson Inc. The programs may be used and/or copied only with written

    permission from Ericsson Inc. or in accordance with the terms and

    conditions stipulated in the agreement/contract under which the

    program(s) have been supplied.

-->
<#import "commons.ftl" as commons>
<#assign nodeMO = "NetworkElement=${managedElementId}">
# Function definition
<@commons.logging logFileName = '${operation}'/>
<@commons.checkCmdOutput/>
<@commons.executeCommand/>
<@commons.printData item = '${operationResponse}'/>

def initializeOutput():
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

def enableSupervisions():
    # Enabling PmFunction
    <#if pmFunction?string == "true">
    command5 = 'cmedit set ${nodeMO},PmFunction=1 pmEnabled=true'
    cmdExitStatus5,cmdOutput5 = executeCommand(command5)
    ${operationResponse}["pmSupervisionStatus"]["exitStatus"] = cmdExitStatus5
    ${operationResponse}["pmSupervisionStatus"]["commandOutput"] = cmdOutput5
    </#if>
    # Enabling CM Supervision
    <#if cmNodeHeartbeatSupervision?string == "true">
    command6 = 'cmedit set ${nodeMO},CmNodeHeartbeatSupervision=1 active=true'
    cmdExitStatus6,cmdOutput6 = executeCommand(command6)
    ${operationResponse}["cmSupervisionStatus"]["exitStatus"] = cmdExitStatus6
    ${operationResponse}["cmSupervisionStatus"]["commandOutput"] = cmdOutput6
    </#if>
    # Enabling Alarm Supervision
    <#if fmAlarmSupervision?string == "true">
    command7 = 'alarm enable ${nodeMO}'
    cmdExitStatus7,cmdOutput7 = executeCommand(command7)
    ${operationResponse}["alarmSupervisionStatus"]["exitStatus"] = cmdExitStatus7
    ${operationResponse}["alarmSupervisionStatus"]["commandOutput"] = cmdOutput7
    </#if>
    # Fetching Fdn of the node
    command8 = 'cmedit get ${managedElementId}'
    cmdExitStatus8,cmdOutput8 = executeCommand(command8)
    fdn = {}
    ${operationResponse}['fdn'] = fdn
    ${operationResponse}["fdn"]["exitStatus"] = cmdExitStatus8
    ${operationResponse}["fdn"]["commandOutput"] = cmdOutput8

<@commons.startScriptingSession/>

${operationResponse} = {}
initializeOutput()
enableSupervisions()

printData()

# Closing ENM scripting session
enmscripting.close(session)
