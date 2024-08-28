<#--

    COPYRIGHT Ericsson 2024



    The copyright to the computer program(s) herein is the property of

    Ericsson Inc. The programs may be used and/or copied only with written

    permission from Ericsson Inc. or in accordance with the terms and

    conditions stipulated in the agreement/contract under which the

    program(s) have been supplied.

-->
<#import "commons.ftl" as commons>
import os
import re

# Function definition

<@commons.checkCmdOutput/>

<@commons.executeCommand/>

<@commons.executeCommandWithFileUploadDownload/>

<@commons.downloadFirstFile/>

<@commons.printData item = '${operationResponse}'/>

<@commons.logging logFileName = '${operation}'/>

def initializeOutput():
    output = {}
    generateEnrollmentInfoStatus = {"exitStatus": None, "commandOutput": None}
    output['generateEnrollmentInfoStatus'] = generateEnrollmentInfoStatus
    getLdapInfoStatus = {"exitStatus": None, "commandOutput": None}
    output['getLdapInfoStatus'] = getLdapInfoStatus
    return output

# Check Generate Enrollment Information
def generate_enrollment_info(file_to_upload, file_to_download):
    command = 'secadm generateenrollmentinfo --verbose --xmlfile file:%s' % (os.path.basename(file_to_upload))
    return executeCommandWithFileUploadDownload(command, file_to_upload, file_to_download)

# Get ldap details of enm server
def get_ldap_details():
    command = 'secadm ldap configure --manual'
    return executeCommand(command)

<@commons.startScriptingSession/>

${operationResponse} = initializeOutput()

check_status, check_output = generate_enrollment_info('${file_to_upload}', '${generated_xml_file}')
${operationResponse}["generateEnrollmentInfoStatus"]["exitStatus"] = check_status
${operationResponse}["generateEnrollmentInfoStatus"]["commandOutput"] = check_output

check_status, check_output = get_ldap_details()
if check_output is not None and check_status == 0:
   result = re.findall('\'(.*?)\'', check_output)
   if result is not None and result[-1] == 'Command Executed Successfully':
     ldap_info = {}
     for element in result:
       if element != 'PROPERTY\\tVALUE' and '\\t' in element:
          split_element = element.split('\\t')
          ldap_info[split_element[0]] = split_element[1]
     ${operationResponse}["getLdapInfoStatus"]["exitStatus"] = check_status
     ${operationResponse}["getLdapInfoStatus"]["commandOutput"] = ldap_info
   else:
     ${operationResponse}["getLdapInfoStatus"]["exitStatus"] = check_status
     ${operationResponse}["getLdapInfoStatus"]["commandOutput"] = check_output
else:
   ${operationResponse}["getLdapInfoStatus"]["exitStatus"] = check_status
   ${operationResponse}["getLdapInfoStatus"]["commandOutput"] = check_output

printData()

# Closing ENM scripting session
enmscripting.close(session)
