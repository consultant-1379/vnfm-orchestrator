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

<@commons.startScriptingSession/>

${operationResponse} = {}

command = 'cmedit get ${nodeMO}'
exitCode, output = executeCommand(command)

checkNodeStatus={}
checkNodeStatus['exitStatus'] = exitCode
checkNodeStatus['commandOutput'] = output

${operationResponse}['${operationResponse}'] = checkNodeStatus

printData()
# Closing ENM scripting session
enmscripting.close(session)