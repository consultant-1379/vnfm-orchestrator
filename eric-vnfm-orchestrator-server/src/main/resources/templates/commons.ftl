<#--

    COPYRIGHT Ericsson 2024



    The copyright to the computer program(s) herein is the property of

    Ericsson Inc. The programs may be used and/or copied only with written

    permission from Ericsson Inc. or in accordance with the terms and

    conditions stipulated in the agreement/contract under which the

    program(s) have been supplied.

-->
<#macro imports>
import logging
import json
</#macro>

<#macro checkCmdOutput>
def checkCmdOutput( cmdoutput ):
    """This checks command execution successful or not"""
    if cmdoutput.find("Error") != -1:
        return 1
    return 0
</#macro>

<#macro executeCommand>
""" TODO Need to remove unicode chars from the response output with mapping elements. ex: {u'Invalid Command..}"""
"""commandOutput = str(list(map(str, response.get_output())))"""
def executeCommand( command ):
    """This function is to execute commands"""
    response = terminal.execute(command)
    commandOutput = str(response.get_output())
    return checkCmdOutput( commandOutput ),commandOutput
</#macro>

<#macro executeCommandWithFileUpload>
def executeCommandWithFile( command, file_name ):
    """This function is to execute commands with file upload"""
    with open(file_name, 'rb') as file_to_upload:
        response = terminal.execute(command, file_to_upload)
    if response.is_command_result_available():
        commandOutput = str(response.get_output())
        for line in response.get_output():
            logging.debug(line)
        return checkCmdOutput( commandOutput ),commandOutput
</#macro>

<#macro executeCommandWithFileUploadDownload>
def executeCommandWithFileUploadDownload( command, file_upload, download_path ):
    """This function is to execute commands with file upload"""
    with open(file_upload, 'rb') as file_to_upload:
        response = terminal.execute(command, file_to_upload)
    if response.is_command_result_available():
        download_first_file(response, download_path)
        commandOutput = str(response.get_output())
        for line in response.get_output():
            logging.debug(line)
        return checkCmdOutput( commandOutput ),commandOutput
</#macro>

<#macro printData item>
import json
import re

def printData():
    response_json_data = json.dumps(${item})
    print(re.sub('password\s.*\s','password ******** ', response_json_data))
</#macro>

<#macro logging logFileName>
import logging

logging.basicConfig(filename='${logFileName}.log', level=logging.INFO)
</#macro>

<#macro startScriptingSession>
# Open ENM scripting session
import enmscripting

session = enmscripting.open()
terminal = session.terminal()
</#macro>

<#macro createDir>
def create_dir(path=''):
    if path:
        if not os.path.exists(path):
            logging.debug('Creating directory for saving files downloaded from ENM.')
            os.makedirs(path)
        return path
    else:
        return os.getcwd()
</#macro>

<#macro downloadFirstFile>
def download_first_file( response, download_path=None):
    if response.has_files():
        for file in response.files():
            filename = file.get_name()
            logging.debug('File Name: ' + filename)
            file.download(download_path)
            break
</#macro>