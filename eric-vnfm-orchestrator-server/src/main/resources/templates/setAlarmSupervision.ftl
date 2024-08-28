<#--

    COPYRIGHT Ericsson 2024



    The copyright to the computer program(s) herein is the property of

    Ericsson Inc. The programs may be used and/or copied only with written

    permission from Ericsson Inc. or in accordance with the terms and

    conditions stipulated in the agreement/contract under which the

    program(s) have been supplied.

-->
<#import "commons.ftl" as commons>

<#assign nodeMO = "${managedElementId}">

# Function definition

<@commons.logging logFileName = '${operation}'/>

<@commons.checkCmdOutput/>

<@commons.executeCommand/>

<@commons.printData item = '${operationResponse}'/>

def initializeOutput():
    checkFmAlarmSupervisionStatus = {"exitStatus": None, "commandOutput": None}
    ${operationResponse}['checkFmAlarmSupervisionStatus'] = checkFmAlarmSupervisionStatus

    setFmAlarmSupervisionStatus = {"exitStatus": None, "commandOutput": None}
    ${operationResponse}['setFmAlarmSupervisionStatus'] = setFmAlarmSupervisionStatus

<@commons.startScriptingSession/>


# Check FM Alarm Supervision Status
def checkAlarmSupervisionStatus():
    command = 'alarm status ${nodeMO} -l'
    response = terminal.execute(command)
    commandOutput = response.get_output()
    return checkCmdOutput( str(commandOutput) ),commandOutput

# Enable FM Alarm Supervision
def enableAlarmSupervision():
    command = 'alarm enable ${nodeMO}'
    return executeCommand(command)

# Disable FM Alarm Supervision
def disableAlarmSupervision():
    command = 'alarm disable ${nodeMO}'
    return executeCommand(command)

# Enable/Disable FM Alarm Supervision
def setAlarmSupervisionStatus(set_value):
    if set_value == 'on':
      return enableAlarmSupervision()
    else:
      return disableAlarmSupervision()

def get_alarm_status(response):
  for element in response:
    if 'Supervision Status' in element:
      return str(element).split()[-1]


${operationResponse} = {}
initializeOutput()

check_status, check_output = checkAlarmSupervisionStatus()
${operationResponse}["checkFmAlarmSupervisionStatus"]["exitStatus"] = check_status
${operationResponse}["checkFmAlarmSupervisionStatus"]["commandOutput"] = str(check_output)

if check_status is not None and check_status == 0:
    status = get_alarm_status(check_output)
    if status != '${set_value}':
      ${operationResponse}["setFmAlarmSupervisionStatus"]["exitStatus"], ${operationResponse}["setFmAlarmSupervisionStatus"]["commandOutput"] = setAlarmSupervisionStatus('${set_value}')

printData()

# Closing ENM scripting session
enmscripting.close(session)
