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
package com.ericsson.vnfm.orchestrator.utils;

import static org.assertj.core.api.Assertions.assertThat;

import static com.ericsson.vnfm.orchestrator.TestUtils.readDataFromFile;
import static com.ericsson.vnfm.orchestrator.utils.YamlUtility.convertYamlStringIntoMap;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;



@SpringBootTest(classes = { YamlFileMerger.class })
public class FileMergerTest {

    @Autowired
    @Qualifier("yamlFileMerger")
    private FileMerger fileMerger;

    @Test
    public void mergeYamlsAndVerifyPrecedence() {
        String valuesA = readDataFromFile(FileMergerTest.class, "values-merge-a.yaml");
        String valuesB = readDataFromFile(FileMergerTest.class, "values-merge-b.yaml");
        String merged = fileMerger.merge(valuesA, valuesB);
        assertThat(merged).contains("false");
    }

    @Test
    public void mergeMultipleYamlsAndVerifyPrecedence() {
        String valuesA = readDataFromFile(FileMergerTest.class, "values-merge-a.yaml");
        String valuesB = readDataFromFile(FileMergerTest.class, "values-merge-b.yaml");
        String twoYamls = fileMerger.merge(valuesA, valuesB);
        assertThat(twoYamls).contains("valueB").contains("secondValueA");;

        String valuesC = readDataFromFile(FileMergerTest.class, "values-merge-c.yaml");
        String threeYamls = fileMerger.merge(valuesA, valuesB, valuesC);
        assertThat(threeYamls).contains("valueC").contains("secondValueC");

        String valuesD = readDataFromFile(FileMergerTest.class, "values-merge-d.yaml");
        String fourYamls = fileMerger.merge(valuesA, valuesB, valuesC, valuesD);
        assertThat(fourYamls).contains("valueD").contains("secondValueC");
    }

    @Test
    public void mergeLargeYamlsAndVerifyContents() {
        String valuesA = readDataFromFile(FileMergerTest.class, "large-values-a.yaml");
        String valuesB = readDataFromFile(FileMergerTest.class, "large-values-b.yaml");
        String merged = fileMerger.merge(valuesA, valuesB);
        assertThat(merged).contains("agentToBro").contains("brAgent");
    }

    @Test
    public void overrideComplexYamlsWithEmptyMap() {
        String valueA = readDataFromFile(FileMergerTest.class, "additional-params.yaml");
        String valueB = readDataFromFile(FileMergerTest.class, "additional-params-with-empty-map.yaml");
        Map<String, Object> expectedMap = convertYamlStringIntoMap(valueA);
        Map<String, Object> actualMap = convertYamlStringIntoMap(fileMerger.merge(valueA, valueB));

        assertThat(actualMap).isEqualTo(expectedMap);
    }

    @Test
    public void overrideComplexYamlsWithArrayList() {
        String valueA = readDataFromFile(FileMergerTest.class, "additional-params.yaml");
        String valueB = readDataFromFile(FileMergerTest.class, "additional-params-with-empty-array.yaml");
        Map<String, Object> expectedMap = convertYamlStringIntoMap(valueB);
        Map<String, Object> actualMap = convertYamlStringIntoMap(fileMerger.merge(valueA, valueB));

        assertThat(actualMap).isEqualTo(expectedMap);
    }

    @Test
    public void overrideComplexYamlsWithEmptyString() {
        String valueA = readDataFromFile(FileMergerTest.class, "additional-params.yaml");
        String valueB = readDataFromFile(FileMergerTest.class, "additional-params-with-empty-string.yaml");
        Map<String, Object> expectedMap = convertYamlStringIntoMap(valueB);
        Map<String, Object> actualMap = convertYamlStringIntoMap(fileMerger.merge(valueA, valueB));

        assertThat(actualMap).isEqualTo(expectedMap);
    }

    @Test
    public void overrideEmptyMapWithComplexYamls() {
        String valueA = readDataFromFile(FileMergerTest.class, "additional-params-with-empty-map.yaml");
        String valueB = readDataFromFile(FileMergerTest.class, "additional-params.yaml");
        Map<String, Object> expectedMap = convertYamlStringIntoMap(valueB);
        Map<String, Object> actualMap = convertYamlStringIntoMap(fileMerger.merge(valueA, valueB));

        assertThat(actualMap).isEqualTo(expectedMap);
    }

    @Test
    public void overrideEmptyArrayWithComplexYamls() {
        String valueA = readDataFromFile(FileMergerTest.class, "additional-params-with-empty-array.yaml");
        String valueB = readDataFromFile(FileMergerTest.class, "additional-params.yaml");
        Map<String, Object> expectedMap = convertYamlStringIntoMap(valueB);
        Map<String, Object> actualMap = convertYamlStringIntoMap(fileMerger.merge(valueA, valueB));

        assertThat(actualMap).isEqualTo(expectedMap);
    }

    @Test
    public void overrideEmptyStringWithComplexYamls() {
        String valueA = readDataFromFile(FileMergerTest.class, "additional-params-with-empty-string.yaml");
        String valueB = readDataFromFile(FileMergerTest.class, "additional-params.yaml");
        Map<String, Object> expectedMap = convertYamlStringIntoMap(valueB);
        Map<String, Object> actualMap = convertYamlStringIntoMap(fileMerger.merge(valueA, valueB));

        assertThat(actualMap).isEqualTo(expectedMap);
    }
}