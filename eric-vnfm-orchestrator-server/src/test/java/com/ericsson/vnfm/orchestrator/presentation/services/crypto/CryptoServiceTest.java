/*
 * COPYRIGHT Ericsson 2024
 *
 *
 *
 * The copyright to the computer program(s) herein is the property of
 *
 * Ericsson Inc. The programs may be used and/or copied only with written
 *
 * permission from Ericsson Inc. or in accordance with the terms and
 *
 * conditions stipulated in the agreement/contract under which the
 *
 * program(s) have been supplied.
 */
package com.ericsson.vnfm.orchestrator.presentation.services.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;

import static com.ericsson.vnfm.orchestrator.utils.Utility.convertObjToJsonString;

import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import com.ericsson.am.shared.vnfd.service.CryptoService;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.transaction.TransactionSystemException;

import com.ericsson.vnfm.orchestrator.TestUtils;
import com.ericsson.vnfm.orchestrator.model.entity.LifecycleOperation;
import com.ericsson.vnfm.orchestrator.model.entity.VnfInstance;
import com.ericsson.am.shared.vnfd.service.exception.CryptoException;
import com.ericsson.vnfm.orchestrator.repositories.LifecycleOperationRepository;
import com.ericsson.vnfm.orchestrator.repositories.VnfInstanceRepository;
import com.ericsson.vnfm.orchestrator.utils.AbstractDbSetupTest;
import com.google.common.base.Strings;


public class CryptoServiceTest extends AbstractDbSetupTest {

    @Autowired
    private LifecycleOperationRepository lifecycleOperationRepository;

    @Autowired
    private VnfInstanceRepository vnfInstanceRepository;

    @SpyBean
    private CryptoService cryptoService;

    private static final String AES = "AES";
    private static final String SECRET = "secret-key-12345";

    private final Key key;
    private final Cipher cipher;

    public CryptoServiceTest() throws NoSuchPaddingException, NoSuchAlgorithmException {
        key = new SecretKeySpec(SECRET.getBytes(), AES);
        cipher = Cipher.getInstance(AES);
    }

    @BeforeEach
    public void setup() throws Exception {
        doAnswer(invocation -> encryptString(invocation.getArgument(0)))
                .when(cryptoService).encryptString(anyString());
        // Enable mock decrypt for reading
        doAnswer(invocation -> decryptString(invocation.getArgument(0)))
                .when(cryptoService).decryptString(anyString());
    }

    @Test
    public void encryptOnSaveVnfInstanceSuccess() {
        String vnfInstanceId = "crypto1-rd45-477c-vnf0-judc7";
        VnfInstance instanceBeforeSave = addDataToInstance(vnfInstanceId);

        // Only enable encryption onSave, do not decrypt after load
        doAnswer(invocation -> invocation.getArgument(0))
                .when(cryptoService).decryptString(anyString());

        String combinedValuesFileAsJsonBefore = instanceBeforeSave.getCombinedValuesFile();
        String ossTopologyAsJsonBefore = instanceBeforeSave.getOssTopology();
        String instantiateOssTopologyAsJsonBefore = instanceBeforeSave.getInstantiateOssTopology();
        String addNodeOssTopologyAsJsonBefore = instanceBeforeSave.getAddNodeOssTopology();
        String tempInstanceAsJsonBefore = instanceBeforeSave.getTempInstance();
        String sitebasicFileAsJsonBefore = instanceBeforeSave.getSitebasicFile();
        String sensitiveInfoAsJsonBefore = instanceBeforeSave.getSensitiveInfo();
        String ossNodeProtocolFileAsJsonBefore = instanceBeforeSave.getOssNodeProtocolFile();

        vnfInstanceRepository.save(instanceBeforeSave);

        VnfInstance vnfInstanceAfterSave = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceId);

        assertThat(vnfInstanceAfterSave.getCombinedValuesFile()).isNotNull().isNotEqualTo(combinedValuesFileAsJsonBefore);
        assertThat(vnfInstanceAfterSave.getOssTopology()).isNotNull().isNotEqualTo(ossTopologyAsJsonBefore);
        assertThat(vnfInstanceAfterSave.getInstantiateOssTopology()).isNotNull().isNotEqualTo(instantiateOssTopologyAsJsonBefore);
        assertThat(vnfInstanceAfterSave.getAddNodeOssTopology()).isNotNull().isNotEqualTo(addNodeOssTopologyAsJsonBefore);
        assertThat(vnfInstanceAfterSave.getTempInstance()).isNotNull().isNotEqualTo(tempInstanceAsJsonBefore);
        assertThat(vnfInstanceAfterSave.getSitebasicFile()).isNotNull().isNotEqualTo(sitebasicFileAsJsonBefore);
        assertThat(vnfInstanceAfterSave.getSensitiveInfo()).isNotNull().isNotEqualTo(sensitiveInfoAsJsonBefore);
        assertThat(vnfInstanceAfterSave.getOssNodeProtocolFile()).isNotNull().isNotEqualTo(ossNodeProtocolFileAsJsonBefore);

        // Enable decrypt for reading
        doAnswer(invocation -> decryptString(invocation.getArgument(0)))
                .when(cryptoService).decryptString(anyString());

        VnfInstance vnfInstanceEnableDecrypt = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceId);

        assertThat(vnfInstanceEnableDecrypt.getCombinedValuesFile()).isNotNull().isEqualTo(combinedValuesFileAsJsonBefore);
        assertThat(vnfInstanceEnableDecrypt.getOssTopology()).isNotNull().isEqualTo(ossTopologyAsJsonBefore);
        assertThat(vnfInstanceEnableDecrypt.getInstantiateOssTopology()).isNotNull().isEqualTo(instantiateOssTopologyAsJsonBefore);
        assertThat(vnfInstanceEnableDecrypt.getAddNodeOssTopology()).isNotNull().isEqualTo(addNodeOssTopologyAsJsonBefore);
        assertThat(vnfInstanceEnableDecrypt.getTempInstance()).isNotNull().isEqualTo(tempInstanceAsJsonBefore);
        assertThat(vnfInstanceEnableDecrypt.getSitebasicFile()).isNotNull().isEqualTo(sitebasicFileAsJsonBefore);
        assertThat(vnfInstanceEnableDecrypt.getSensitiveInfo()).isNotNull().isEqualTo(sensitiveInfoAsJsonBefore);
        assertThat(vnfInstanceEnableDecrypt.getOssNodeProtocolFile()).isNotNull().isEqualTo(ossNodeProtocolFileAsJsonBefore);

        // reset: disable encryption before saving as test data
        doAnswer(invocation -> invocation.getArgument(0))
                .when(cryptoService).encryptString(anyString());
        doAnswer(invocation -> invocation.getArgument(0))
                .when(cryptoService).decryptString(anyString());

        VnfInstance instanceReset = addDataToInstance(vnfInstanceId);
        instanceReset.setCombinedAdditionalParams("{\"test-key-1\":\"test-value-1\"}"); // bypass the cache
        vnfInstanceRepository.save(instanceReset);

        // Final check without encryption
        VnfInstance vnfInstanceUnencrypted = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceId);
        assertThat(vnfInstanceUnencrypted.getCombinedAdditionalParams()).isNotNull().isEqualTo("{\"test-key-1\":\"test-value-1\"}");
    }

    @Test
    public void encryptOnSaveLifeCycleOperationSuccess() {
        // Disable encryption and decryption
        doAnswer(i -> i.getArgument(0))
                .when(cryptoService).decryptString(anyString());
        doAnswer(i -> i.getArgument(0))
                .when(cryptoService).encryptString(anyString());

        String lifecycleOperationId = "crypto2lc-rd45-4673-oper-fgrte";
        LifecycleOperation lifecycleOperationBeforeSave = addDataToLifecycleOperation(lifecycleOperationId);

        String operationParamsAsJsonBefore = lifecycleOperationBeforeSave.getOperationParams();
        String valuesFileParamsAsJsonBefore = lifecycleOperationBeforeSave.getValuesFileParams();
        String combinedValuesFileAsJsonBefore = lifecycleOperationBeforeSave.getCombinedValuesFile();

        // Enable encryption before save
        doAnswer(i -> encryptString(i.getArgument(0)))
                .when(cryptoService).encryptString(anyString());
        lifecycleOperationRepository.save(lifecycleOperationBeforeSave);

        LifecycleOperation lifecycleOperationEncrypted = lifecycleOperationRepository.findByOperationOccurrenceId(lifecycleOperationId);

        assertThat(lifecycleOperationEncrypted.getOperationParams()).isNotNull().isNotEqualTo(operationParamsAsJsonBefore);
        assertThat(lifecycleOperationEncrypted.getValuesFileParams()).isNotNull().isNotEqualTo(valuesFileParamsAsJsonBefore);
        assertThat(lifecycleOperationEncrypted.getCombinedValuesFile()).isNotNull().isNotEqualTo(combinedValuesFileAsJsonBefore);

        // enable decryption back
        doAnswer(i -> decryptString(i.getArgument(0)))
                .when(cryptoService).decryptString(anyString());

        LifecycleOperation lifecycleOperationDecrypted = lifecycleOperationRepository.findByOperationOccurrenceId(lifecycleOperationId);

        assertThat(lifecycleOperationDecrypted.getOperationParams()).isNotNull().isEqualTo(operationParamsAsJsonBefore);
        assertThat(lifecycleOperationDecrypted.getValuesFileParams()).isNotNull().isEqualTo(valuesFileParamsAsJsonBefore);
        assertThat(lifecycleOperationDecrypted.getCombinedValuesFile()).isNotNull().isEqualTo(combinedValuesFileAsJsonBefore);

        // Disable encryption and decrypt back
        doAnswer(i -> decryptString(i.getArgument(0)))
                .when(cryptoService).encryptString(anyString());
        doAnswer(i -> i.getArgument(0))
                .when(cryptoService).decryptString(anyString());

        LifecycleOperation lifecycleOperation = lifecycleOperationRepository.findByOperationOccurrenceId(lifecycleOperationId);
        lifecycleOperation.setCombinedAdditionalParams("{\"test-key-1\":\"test-value-1\"}"); // bypass the cache
        lifecycleOperation = lifecycleOperationRepository.save(lifecycleOperation);

        // Final check without encryption
        assertThat(lifecycleOperation.getCombinedAdditionalParams()).isNotNull().isEqualTo("{\"test-key-1\":\"test-value-1\"}");
    }

    @Test
    public void cryptoServiceFailsForVnfInstanceRead() {
        String vnfInstanceId = "crypto3-rd45-477c-vnf0-judc7";
        doThrow(new CryptoException("Error from Crypto Service when decrypting data.")).when(cryptoService).decryptString(anyString());
        assertThatThrownBy(() -> vnfInstanceRepository.findById(vnfInstanceId))
                .isInstanceOf(JpaSystemException.class);
    }

    @Test
    public void cryptoServiceFailsForVnfInstanceWrite() {
        doAnswer(invocation -> invocation.getArgument(0))
                .when(cryptoService).decryptString(anyString());
        String vnfInstanceId = "crypto3-rd45-477c-vnf0-judc7";
        VnfInstance instance = addDataToInstance(vnfInstanceId);
        instance.setCombinedValuesFile("try to change");
        doThrow(new CryptoException("Error from Crypto Service when encrypting data.")).when(cryptoService).encryptString(anyString());
        assertThatThrownBy(() -> vnfInstanceRepository.save(instance))
                .isInstanceOf(TransactionSystemException.class);
    }

    @Test
    public void cryptoServiceFailsForLifecycleOperationRead() {
        String lifecycleOperationId = "crypto3lc-rd45-4673-oper-fgrte";
        doThrow(new CryptoException("Error from Crypto Service when decrypting data.")).when(cryptoService).decryptString(anyString());
        assertThatThrownBy(() -> lifecycleOperationRepository.findById(lifecycleOperationId))
                .isInstanceOf(JpaSystemException.class);
    }

    @Test
    public void cryptoServiceFailsForLifecycleOperationWrite() {
        doAnswer(invocation -> invocation.getArgument(0))
                .when(cryptoService).decryptString(anyString());
        String lifecycleOperationId = "crypto3lc-rd45-4673-oper-fgrte";
        LifecycleOperation lifecycleOperation = addDataToLifecycleOperation(lifecycleOperationId);
        lifecycleOperation.setCombinedValuesFile("try to change");
        doThrow(new CryptoException("Error from Crypto Service when encrypting data.")).when(cryptoService).encryptString(anyString());
        assertThatThrownBy(() -> lifecycleOperationRepository.save(lifecycleOperation))
                .isInstanceOf(TransactionSystemException.class);
    }

    private VnfInstance addDataToInstance(String vnfInstanceId) {
        VnfInstance instance = vnfInstanceRepository.findByVnfInstanceId(vnfInstanceId);
        Map<String, String> combinedValuesFile = new HashMap<>();
        combinedValuesFile.put("test-1", "value-1");
        combinedValuesFile.put("test-2", "value-2");
        combinedValuesFile.put("test-3", "value-3");
        String combinedValuesFileAsJson = convertObjToJsonString(combinedValuesFile);

        String ossTopology1 = TestUtils.parseJsonFile("oss_params1.json");
        String ossTopology2 = TestUtils.parseJsonFile("oss_params2.json");
        String tempInstance = TestUtils.parseJsonFile("temp_instance.json");
        instance.setCombinedValuesFile(combinedValuesFileAsJson);
        instance.setOssTopology(ossTopology1);
        instance.setInstantiateOssTopology(ossTopology1);
        instance.setAddNodeOssTopology(ossTopology2);
        instance.setTempInstance(tempInstance);
        instance.setSitebasicFile("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n<Nodes>\r\n   <Node>\r\n      "
                + "<nodeFdn>VPP00001</nodeFdn>");
        instance.setSensitiveInfo("{\"test-key-1\":\"test-value-1\",\"test-key-2\":\"test-value-2\"}");
        instance.setOssNodeProtocolFile("<?xml version=\"1.0\" encoding=\"UTF-8\"?> <hello xmlns=\"urn:ietf:params:xml:ns:netconf:base:1"
                + ".0\">");
        return instance;
    }

    private LifecycleOperation addDataToLifecycleOperation(String lifecycleOperationId) {
        LifecycleOperation lifecycleOperation = lifecycleOperationRepository.findByOperationOccurrenceId(lifecycleOperationId);
        String combinedValuesFile = "{\"eric-adp-gs-testapp\":{\"ingress\":{\"enabled\":false}},"
                + "\"influxdb\":{\"ext\":{\"apiAccessHostname\":\"influxdb-service2\"}},"
                + "\"eric-pm-server\":{\"server\":{\"persistentVolume\":{\"storageClass\":\"network-block\"},\"ingress\":{\"enabled\":false}}},"
                + "\"pm-testapp\":{\"ingress\":{\"domain\":\"server\"}},\"tags\":{\"all\":false,\"pm\":true}}";
        String valuesFileParams = "{\"pm-testapp\":{\"ingress\":{\"domain\":\"server\"}},"
                + "\"eric-pm-server\":{\"server\":{\"persistentVolume\":{\"storageClass\":\"network-block\"},\"ingress\":{\"enabled\":false}}}}";
        String operationParams = "{\"additionalParams\":{\"tags.all\":false,\"skipVerification\":false,\"applicationTimeOut\":\"3600\","
                + "\"cleanUpResources\":true,\"namespace\":\"sample-test-vnf\",\"tags.pm\":true,\"eric-pm-server.server.persistentVolume"
                + ".storageClass\":\"network-block\"},\"extVirtualLinks\":null,\"instantiationLevelId\":null,\"flavourId\":null,"
                + "\"clusterName\":\"default\",\"extManagedVirtualLinks\":null,\"localizationLanguage\":null}";

        lifecycleOperation.setCombinedValuesFile(combinedValuesFile);
        lifecycleOperation.setValuesFileParams(valuesFileParams);
        lifecycleOperation.setOperationParams(operationParams);
        return lifecycleOperation;
    }

    private String decryptString(final String data) {
        if (isEmptyJsonOrNull(data)) {
            return data;
        }
        try {
            cipher.init(Cipher.DECRYPT_MODE, key);
            return new String(cipher.doFinal(Base64.getDecoder().decode(data)));
        } catch (InvalidKeyException | BadPaddingException | IllegalBlockSizeException e) {
            throw new IllegalStateException(e);
        }
    }

    private static boolean isEmptyJsonOrNull(String data) {
        if (Strings.isNullOrEmpty(data)) {
            return true;
        }
        try {
            JSONObject dbDataAsJson = new JSONObject(data);
            if (dbDataAsJson.keySet().isEmpty()) {
                return true;
            }
        } catch (JSONException e) {
        }
        return false;
    }

    private String encryptString(final String data) {
        if (isEmptyJsonOrNull(data)) {
            return data;
        }
        try {
            cipher.init(Cipher.ENCRYPT_MODE, key);
            return Base64.getEncoder().encodeToString(cipher.doFinal(data.getBytes()));
        } catch (IllegalBlockSizeException | BadPaddingException | InvalidKeyException e) {
            throw new IllegalStateException(e);
        }
    }
}
