UPDATE app_vnf_instance
SET oss_topology='{"snmpSecurityLevel":{"type":"string","required":"false","defaultValue":"snmpSecurityLvl0"},"axeNodeInterfaceAIp":{"type":"string",
"required":"false","defaultValue":"axeNode123"}}', instantiate_oss_topology='{"snmpSecurityLevel":{"type":"string","required":"false",
"defaultValue":"snmpSecurityLvl1"},"axeNodeInterfaceAIp":{"type":"string","required":"false","defaultValue":"axeNodeInstantiate"}}'
WHERE vnf_id='10def1ce-4cf4-477c-aab3-21c454e6a389';

UPDATE app_vnf_instance
SET oss_topology='{"snmpSecurityLevel":{"type":"string","required":"false","defaultValue":"snmpSecurityLvl0"},"axeNodeInterfaceAIp":{"type":"string",
"required":"false","defaultValue":"axeNode123"}}', instantiate_oss_topology='{"snmpSecurityLevel":{"type":"string","required":"false",
"defaultValue":"snmpSecurityLvl1"},"axeNodeInterfaceAIp":{"type":"string","required":"false","defaultValue":"axeNodeInstantiate"}}'
WHERE vnf_id='11def1ce-4cf4-477c-aab3-21c454e6a389';