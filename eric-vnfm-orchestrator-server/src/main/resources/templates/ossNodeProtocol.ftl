<#--

    COPYRIGHT Ericsson 2024



    The copyright to the computer program(s) herein is the property of

    Ericsson Inc. The programs may be used and/or copied only with written

    permission from Ericsson Inc. or in accordance with the terms and

    conditions stipulated in the agreement/contract under which the

    program(s) have been supplied.

-->
<?xml version="1.0" encoding="UTF-8"?>
<hello xmlns="urn:ietf:params:xml:ns:netconf:base:1.0">
    <capabilities>
        <capability>urn:ietf:params:netconf:base:1.0</capability>
        <capability>urn:ietf:params:netconf:capability:writable-running:1.0</capability>
        <capability>urn:ietf:params:netconf:capability:rollback-on-error:1.0</capability>
    </capabilities>
</hello>
]]>]]>

<?xml version="1.0" encoding="UTF-8"?>
<rpc message-id="OAM CMP Server Trust Store" xmlns="urn:ietf:params:xml:ns:netconf:base:1.0">
<edit-config xmlns="urn:ietf:params:xml:ns:netconf:base:1.0">
    <target>
        <running/>
    </target>
    <config xmlns:xc="urn:ietf:params:xml:ns:netconf:base:1.0">
        <truststore xmlns="urn:ietf:params:xml:ns:yang:ietf-truststore">
            <certificates xc:operation="merge">
                <name>oamCmpCaTrustCategory</name>
                <description>The OAM CMP server CA trust category.</description>
            </certificates>
        </truststore>
    </config>
</edit-config>
</rpc>
]]>]]>

<?xml version="1.0" encoding="UTF-8"?>
<rpc message-id="Close Session" xmlns="urn:ietf:params:xml:ns:netconf:base:1.0">
    <close-session />
</rpc>
]]>]]>

<?xml version="1.0" encoding="UTF-8"?>
<hello xmlns="urn:ietf:params:xml:ns:netconf:base:1.0">
    <capabilities>
        <capability>urn:ietf:params:netconf:base:1.0</capability>
        <capability>urn:ietf:params:netconf:capability:writable-running:1.0</capability>
        <capability>urn:ietf:params:netconf:capability:rollback-on-error:1.0</capability>
    </capabilities>
</hello>
]]>]]>

<?xml version="1.0" encoding="UTF-8"?>
<rpc message-id="Install OAM CMP Trusted certificates" xmlns="urn:ietf:params:xml:ns:netconf:base:1.0">
<action xmlns="urn:ietf:params:xml:ns:yang:1">
    <truststore xmlns="urn:ietf:params:xml:ns:yang:ietf-truststore">
        <certificates>
            <name>oamCmpCaTrustCategory</name>
            <install-certificate-pem xmlns="urn:rdns:com:ericsson:oammodel:ericsson-truststore-ext">
                <name>ENM_PKI_Root_CA</name>
                <pem>${enmPKIRootCA}</pem>
            </install-certificate-pem>
        </certificates>
    </truststore>
</action>
</rpc>
]]>]]>

<?xml version="1.0" encoding="UTF-8"?>
<rpc message-id="Close Session" xmlns="urn:ietf:params:xml:ns:netconf:base:1.0">
    <close-session />
</rpc>
]]>]]>

<?xml version="1.0" encoding="UTF-8"?>
<hello xmlns="urn:ietf:params:xml:ns:netconf:base:1.0">
    <capabilities>
        <capability>urn:ietf:params:netconf:base:1.0</capability>
        <capability>urn:ietf:params:netconf:capability:writable-running:1.0</capability>
        <capability>urn:ietf:params:netconf:capability:rollback-on-error:1.0</capability>
    </capabilities>
</hello>
]]>]]>

<?xml version="1.0" encoding="UTF-8"?>
<rpc message-id="OAM Trust Store" xmlns="urn:ietf:params:xml:ns:netconf:base:1.0">
<edit-config xmlns="urn:ietf:params:xml:ns:netconf:base:1.0">
    <target>
        <running/>
    </target>
    <config xmlns:xc="urn:ietf:params:xml:ns:netconf:base:1.0">
        <truststore xmlns="urn:ietf:params:xml:ns:yang:ietf-truststore">
            <certificates xc:operation="merge">
                <name>oamTrustCategory</name>
                <description>The OAM trust category.</description>
            </certificates>
        </truststore>
    </config>
</edit-config>
</rpc>
]]>]]>

<?xml version="1.0" encoding="UTF-8"?>
<rpc message-id="Close Session" xmlns="urn:ietf:params:xml:ns:netconf:base:1.0">
    <close-session />
</rpc>
]]>]]>

<?xml version="1.0" encoding="UTF-8"?>
<hello xmlns="urn:ietf:params:xml:ns:netconf:base:1.0">
    <capabilities>
        <capability>urn:ietf:params:netconf:base:1.0</capability>
        <capability>urn:ietf:params:netconf:capability:writable-running:1.0</capability>
        <capability>urn:ietf:params:netconf:capability:rollback-on-error:1.0</capability>
    </capabilities>
</hello>
]]>]]>

<?xml version="1.0" encoding="UTF-8"?>
<rpc message-id="Intsall OAM Trusted Certificate-1" xmlns="urn:ietf:params:xml:ns:netconf:base:1.0">
<action xmlns="urn:ietf:params:xml:ns:yang:1">
    <truststore xmlns="urn:ietf:params:xml:ns:yang:ietf-truststore">
        <certificates>
            <name>oamTrustCategory</name>
            <install-certificate-pem xmlns="urn:rdns:com:ericsson:oammodel:ericsson-truststore-ext">
                <name>ENM_OAM_CA</name>
                <pem>${enmOAMCA}</pem>
            </install-certificate-pem>
        </certificates>
    </truststore>
</action>
</rpc>
]]>]]>

<?xml version="1.0" encoding="UTF-8"?>
<rpc message-id="Close Session" xmlns="urn:ietf:params:xml:ns:netconf:base:1.0">
    <close-session />
</rpc>
]]>]]>

<?xml version="1.0" encoding="UTF-8"?>
<hello xmlns="urn:ietf:params:xml:ns:netconf:base:1.0">
    <capabilities>
        <capability>urn:ietf:params:netconf:base:1.0</capability>
        <capability>urn:ietf:params:netconf:capability:writable-running:1.0</capability>
        <capability>urn:ietf:params:netconf:capability:rollback-on-error:1.0</capability>
    </capabilities>
</hello>
]]>]]>

<?xml version="1.0" encoding="UTF-8"?>
<rpc message-id="Intsall OAM Trusted Certificate-2" xmlns="urn:ietf:params:xml:ns:netconf:base:1.0">
<action xmlns="urn:ietf:params:xml:ns:yang:1">
    <truststore xmlns="urn:ietf:params:xml:ns:yang:ietf-truststore">
        <certificates>
            <name>oamTrustCategory</name>
            <install-certificate-pem xmlns="urn:rdns:com:ericsson:oammodel:ericsson-truststore-ext">
                <name>NE_OAM_CA</name>
                <pem>${neOAMCA}</pem>
            </install-certificate-pem>
        </certificates>
    </truststore>
</action>
</rpc>
]]>]]>

<?xml version="1.0" encoding="UTF-8"?>
<rpc message-id="Close Session" xmlns="urn:ietf:params:xml:ns:netconf:base:1.0">
    <close-session />
</rpc>
]]>]]>

<?xml version="1.0" encoding="UTF-8"?>
<hello xmlns="urn:ietf:params:xml:ns:netconf:base:1.0">
    <capabilities>
        <capability>urn:ietf:params:netconf:base:1.0</capability>
        <capability>urn:ietf:params:netconf:capability:writable-running:1.0</capability>
        <capability>urn:ietf:params:netconf:capability:rollback-on-error:1.0</capability>
    </capabilities>
</hello>
]]>]]>

<?xml version="1.0" encoding="UTF-8"?>
<rpc message-id="Intsall OAM Trusted Certificate-3" xmlns="urn:ietf:params:xml:ns:netconf:base:1.0">
<action xmlns="urn:ietf:params:xml:ns:yang:1">
    <truststore xmlns="urn:ietf:params:xml:ns:yang:ietf-truststore">
        <certificates>
            <name>oamTrustCategory</name>
            <install-certificate-pem xmlns="urn:rdns:com:ericsson:oammodel:ericsson-truststore-ext">
                <name>ENM_Infrastructure_CA</name>
                <pem>${enmInfrastructureCA}</pem>
            </install-certificate-pem>
        </certificates>
    </truststore>
</action>
</rpc>
]]>]]>

<?xml version="1.0" encoding="UTF-8"?>
<rpc message-id="Close Session" xmlns="urn:ietf:params:xml:ns:netconf:base:1.0">
    <close-session />
</rpc>
]]>]]>

<?xml version="1.0" encoding="UTF-8"?>
<hello xmlns="urn:ietf:params:xml:ns:netconf:base:1.0">
    <capabilities>
        <capability>urn:ietf:params:netconf:base:1.0</capability>
        <capability>urn:ietf:params:netconf:capability:writable-running:1.0</capability>
        <capability>urn:ietf:params:netconf:capability:rollback-on-error:1.0</capability>
    </capabilities>
</hello>
]]>]]>

<?xml version="1.0" encoding="UTF-8"?>
<rpc message-id="Intsall OAM Trusted Certificate-4" xmlns="urn:ietf:params:xml:ns:netconf:base:1.0">
<action xmlns="urn:ietf:params:xml:ns:yang:1">
    <truststore xmlns="urn:ietf:params:xml:ns:yang:ietf-truststore">
        <certificates>
            <name>oamTrustCategory</name>
            <install-certificate-pem xmlns="urn:rdns:com:ericsson:oammodel:ericsson-truststore-ext">
                <name>ENM_PKI_Root_CA</name>
                <pem>${enmPKIRootCA}</pem>
            </install-certificate-pem>
        </certificates>
    </truststore>
</action>
</rpc>
]]>]]>

<?xml version="1.0" encoding="UTF-8"?>
<rpc message-id="Close Session" xmlns="urn:ietf:params:xml:ns:netconf:base:1.0">
    <close-session />
</rpc>
]]>]]>

<?xml version="1.0" encoding="UTF-8"?>
<hello xmlns="urn:ietf:params:xml:ns:netconf:base:1.0">
    <capabilities>
        <capability>urn:ietf:params:netconf:base:1.0</capability>
        <capability>urn:ietf:params:netconf:capability:writable-running:1.0</capability>
        <capability>urn:ietf:params:netconf:capability:rollback-on-error:1.0</capability>
    </capabilities>
</hello>
]]>]]>

<?xml version="1.0" encoding="UTF-8"?>
<rpc message-id="OAM CMP Server Configuration" xmlns="urn:ietf:params:xml:ns:netconf:base:1.0">
<edit-config xmlns="urn:ietf:params:xml:ns:netconf:base:1.0">
    <target>
        <running/>
    </target>
    <config xmlns:xc="urn:ietf:params:xml:ns:netconf:base:1.0">
        <keystore xmlns="urn:ietf:params:xml:ns:yang:ietf-keystore" xmlns:ks="urn:ietf:params:xml:ns:yang:ietf-keystore">
            <cmp xmlns="urn:rdns:com:ericsson:oammodel:ericsson-keystore-ext" xc:operation="merge">
                <certificate-authorities>
                    <certificate-authority>
                        <name>${issuerCA}</name>
                    </certificate-authority>
                </certificate-authorities>
                <cmp-server-groups>
                    <cmp-server-group>
                        <name>oamCmpServerGroup</name>
                        <cmp-server>
                            <name>oamCmpServer</name>
                            <uri>${url}</uri>
                            <ca-certs>oamCmpCaTrustCategory</ca-certs>
                            <certificate-authority>${issuerCA}</certificate-authority>
                            <priority>1</priority>
                        </cmp-server>
                    </cmp-server-group>
                </cmp-server-groups>
            </cmp>
        </keystore>
    </config>
</edit-config>
</rpc>
]]>]]>

<?xml version="1.0" encoding="UTF-8"?>
<rpc message-id="Close Session" xmlns="urn:ietf:params:xml:ns:netconf:base:1.0">
    <close-session />
</rpc>
]]>]]>

<?xml version="1.0" encoding="UTF-8"?>
<hello xmlns="urn:ietf:params:xml:ns:netconf:base:1.0">
    <capabilities>
        <capability>urn:ietf:params:netconf:base:1.0</capability>
        <capability>urn:ietf:params:netconf:capability:writable-running:1.0</capability>
        <capability>urn:ietf:params:netconf:capability:rollback-on-error:1.0</capability>
    </capabilities>
</hello>
]]>]]>

<?xml version="1.0" encoding="UTF-8"?>
<rpc message-id="Init OAM CMP Online Enrollment" xmlns="urn:ietf:params:xml:ns:netconf:base:1.0">
<action xmlns="urn:ietf:params:xml:ns:yang:1" xmlns:nc="urn:ietf:params:xml:ns:netconf:base:1.0">
    <keystore xmlns="urn:ietf:params:xml:ns:yang:ietf-keystore">
        <asymmetric-keys xmlns="urn:ietf:params:xml:ns:yang:ietf-keystore">
            <cmp xmlns="urn:rdns:com:ericsson:oammodel:ericsson-keystore-ext">
                <start-cmp xmlns="urn:rdns:com:ericsson:oammodel:ericsson-keystore-ext">
                    <name>oamNodeCredential</name>
                    <certificate-name>oamNodeCredential</certificate-name>
                    <algorithm>${keyInfo}</algorithm>
                    <subject>${subjectName}</subject>
                    <password>${challengePassword}</password>
                    <cmp-server-group>oamCmpServerGroup</cmp-server-group>
                    <trusted-certs></trusted-certs>
                </start-cmp>
            </cmp>
        </asymmetric-keys>
    </keystore>
</action>
</rpc>
]]>]]>

<?xml version="1.0" encoding="UTF-8"?>
<rpc message-id="Close Session" xmlns="urn:ietf:params:xml:ns:netconf:base:1.0">
    <close-session />
</rpc>
]]>]]>

<?xml version="1.0" encoding="UTF-8"?>
<hello xmlns="urn:ietf:params:xml:ns:netconf:base:1.0">
    <capabilities>
        <capability>urn:ietf:params:netconf:base:1.0</capability>
        <capability>urn:ietf:params:netconf:capability:writable-running:1.0</capability>
        <capability>urn:ietf:params:netconf:capability:rollback-on-error:1.0</capability>
    </capabilities>
</hello>
]]>]]>

<?xml version="1.0" encoding="UTF-8"?>
<rpc message-id="Configure LDAP Server Data" xmlns="urn:ietf:params:xml:ns:netconf:base:1.0">
<edit-config xmlns="urn:ietf:params:xml:ns:netconf:base:1.0">
    <target>
        <running />
    </target>
    <config xmlns:xc="urn:ietf:params:xml:ns:netconf:base:1.0">
        <system xmlns="urn:ietf:params:xml:ns:yang:ietf-system" xc:operation="merge">
            <ldap xmlns="urn:rdns:com:ericsson:oammodel:ericsson-system-ext">
                <server>
                    <name>external</name>
                    <tcp>
                        <address>${ldapIP}</address>
                        <ldaps>
                            <port>${ldapsPort}</port>
                        </ldaps>
                    </tcp>
                </server>
                <security>
                     <tls />
                     <simple-authenticated>
                         <bind-dn>${bindDn}</bind-dn>
                         <bind-password>${bindPassword}</bind-password>
                     </simple-authenticated>
                     <user-base-dn>${baseDn}</user-base-dn>
                </security>
                <options>
                    <timeout>5</timeout>
                    <enable-referrals>false</enable-referrals>
                </options>
            </ldap>
        </system>
    </config>
</edit-config>
</rpc>
]]>]]>

<?xml version="1.0" encoding="UTF-8"?>
<rpc message-id="Close Session" xmlns="urn:ietf:params:xml:ns:netconf:base:1.0">
    <close-session />
</rpc>
]]>]]>