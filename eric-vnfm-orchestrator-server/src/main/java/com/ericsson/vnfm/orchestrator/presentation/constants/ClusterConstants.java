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
package com.ericsson.vnfm.orchestrator.presentation.constants;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.data.domain.Sort;

public final class ClusterConstants {
    public static final String CONFIG_EXTENSION = ".config";
    public static final Integer CLUSTER_CONFIG_DESCRIPTION_MAX_SIZE = 250;
    public static final String DEFAULT_NAMESPACE = "default";
    public static final String VERIFICATION_NAMESPACE = "kube-system";
    public static final String DEFAULT_CRD_NAMESPACE = "eric-crd-ns";
    public static final List<String> KUBE_NAMESPACES = Collections.unmodifiableList(Arrays.asList(
            DEFAULT_NAMESPACE, VERIFICATION_NAMESPACE, "kube-public", "kube-node-lease"));
    public static final String CLUSTERS_COUNT_USED = "clusters.count.used";
    public static final String CLUSTERS_COUNT = "clusters.count";
    public static final String CLUSTERS_COUNT_USED_DESCRIPTION = "Number of registered clusters with status IN_USE";
    public static final String CLUSTERS_COUNT_DESCRIPTION = "Number of all registered clusters";

    private ClusterConstants() {
    }

    public static final class ClusterConfigs {
        // Defaults sort values (if Direction is not provided, equals to ASC)
        public static final Sort NAME = Sort.by(CommonConstants.Request.NAME);
        public static final Sort STATUS = Sort.by(CommonConstants.STATUS);
        public static final Sort CRD_NAMESPACE = Sort.by(Request.CRD_NAMESPACE);
        public static final List<Sort> SORT_COLUMNS = List.of(NAME, STATUS, CRD_NAMESPACE);

        private ClusterConfigs() {
        }
    }

    public static final class DBFields {
        public static final String CONFIG_FILE = "config_file";
        public static final String CLUSTER_SERVER = "cluster_server";
        public static final String APP_CLUSTER_CONFIG_FILE = "app_cluster_config_file";
        public static final String APP_VNF_INSTANCE_NAMESPACE_DETAILS = "vnfinstance_namespace_details";

        private DBFields() {
        }
    }

    public static final class Request {
        public static final String CLUSTER_NAME = "clusterName";
        public static final String NAMESPACE = "namespace";
        public static final String CRD_NAMESPACE = "crdNamespace";
        public static final String CLUSTER = "cluster";
        public static final String NAMESPACE_VALUE_REGEX = "^[a-z0-9]([-a-z0-9]*[a-z0-9])?$";
        public static final String AT_LEAST_ONE_ALPHABET_CHARACTER_REGEX = ".*[a-zA-Z]+.*";
        public static final int NAMESPACE_NAME_MAX_LENGTH = 63;
        public static final int NAMESPACE_NAME_MIN_LENGTH = 2;

        private Request() {
        }
    }

    public static final class Errors {
        public static final String INVALID_CLUSTER_CONFIG = "Cluster config file not valid.";
        public static final String CLUSTER_NOT_FOUND = "Cluster config file does not exist.";
        public static final String CLUSTER_NOT_FOUND_MESSAGE = "Cluster config file %s does not exist.";
        public static final String NOT_SAME_CLUSTER = "Cluster config belongs to another cluster than original, "
                + VERIFICATION_NAMESPACE + " uid differs";
        public static final String NO_VERIFICATION_NAMESPACE = VERIFICATION_NAMESPACE + " namespace not found on cluster";
        public static final String CLUSTER_NAMESPASES_MISSING_MSG = "Following VNF namespaces are missing on cluster: %s";
        public static final String CLUSTER_NAMESPASES_MISSING = "Required namespaces missing";
        public static final String CLUSTER_CONFIG_UPDATE_VALIDATION_TITLE = "Cluster config update validation error";
        public static final String CLUSTER_CONFIG_UPDATE_VALIDATION_ERROR = "One of the clusters must be marked as default";
        public static final String CLUSTER_CONFIG_FILE_NOT_DEFAULT = "Cluster config file with name {} sets as not default";
        public static final String NAMESPACE_VALIDATION_ERROR_MESSAGE =
                "Namespace value must consist of lower case alphanumeric characters or '-'. It must start and end with an alphanumeric character";
        public static final String DEFAULT_CLUSTER_FILE_NOT_FOUND =
                "At least one cluster config has to be registered in order to proceed with LCM operations.";

        private Errors() {
        }
    }

    public static final class Lock {

        public static final int CLUSTER_CONFIG_MAX_LOCK_ATTEMPTS = 3;
        public static final long CLUSTER_CONFIG_BACK_OFF_PERIOD = TimeUnit.SECONDS.toMillis(5);
        public static final long CLUSTER_CONFIG_LOCK_DURATION = 2;
        public static final String CLUSTER_CONFIG_KEY_PREFIX = "cluster-config:";

        private Lock() {

        }
    }
}
