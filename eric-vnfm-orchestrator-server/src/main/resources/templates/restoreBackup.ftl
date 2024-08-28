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

    importBackupStatus = {"exitStatus": None, "commandOutput": None}
    ${operationResponse}['importBackupStatus'] = importBackupStatus
    importBackupProgress = {"exitStatus": None, "commandOutput": None}
    ${operationResponse}['importBackupProgress'] = importBackupProgress
    restoreBackupStatus = {"exitStatus": None, "commandOutput": None}
    ${operationResponse}['restoreBackupStatus'] = restoreBackupStatus

<@commons.startScriptingSession/>

${operationResponse} = {}
initializeOutput()

if "${operation}" == "importBackup":
    command1 = 'cmedit action MeContext=${nodeMO},ManagedElement=${nodeMO},brm=1,backup-manager=configuration-system import-backup.(uri="${backupFileRef}",password=${password})'
    ${operationResponse}["importBackupStatus"]["exitStatus"],${operationResponse}["importBackupStatus"]["commandOutput"] = executeCommand(command1)

else if "${operation}" == "importBackupProgress":
     command1 = 'cmedit show MeContext=${nodeMO},ManagedElement=${nodeMO},brm=1,backup-manager=configuration-system,progress-report=${actionId}'
     ${operationResponse}["importBackupProgress"]["exitStatus"],${operationResponse}["importBackupProgress"]["commandOutput"] = executeCommand(command1)

else if "${operation}" == "restoreLatestBackup":
    command1 = 'cmedit action MeContext=${nodeMO}, ManagedElement=${nodeMO},brm=1,backup-manager=configuration-system,backup=Latest restore'
    ${operationResponse}["restoreBackupStatus"]["exitStatus"],${operationResponse}["restoreBackupStatus"]["commandOutput"] = executeCommand(command1)

else if "${operation}" == "restoreBackup":
    command1 = 'cmedit action MeContext=${nodeMO},ManagedElement=${nodeMO},brm=1,backup-manager=configuration-system,backup=${backupFile} restore'
    ${operationResponse}["restoreBackupStatus"]["exitStatus"],${operationResponse}["restoreBackupStatus"]["commandOutput"] = executeCommand(command1)


printData()

# Closing ENM scripting session
enmscripting.close(session)


