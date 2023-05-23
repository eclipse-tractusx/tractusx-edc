/*
 * Copyright (c) 2022 Mercedes-Benz Tech Innovation GmbH
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 * Contributors:
 *       Mercedes-Benz Tech Innovation GmbH - Add vault health check
 *
 */

package org.eclipse.tractusx.edc.hashicorpvault;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class HashicorpVaultHealthResponsePayload {
    @JsonProperty("initialized")
    private boolean isInitialized;

    @JsonProperty("sealed")
    private boolean isSealed;

    @JsonProperty("standby")
    private boolean isStandby;

    @JsonProperty("performance_standby")
    private boolean isPerformanceStandby;

    @JsonProperty("replication_performance_mode")
    private String replicationPerformanceMode;

    @JsonProperty("replication_dr_mode")
    private String replicationDrMode;

    @JsonProperty("server_time_utc")
    private long serverTimeUtc;

    @JsonProperty("version")
    private String version;

    @JsonProperty("cluster_name")
    private String clusterName;

    @JsonProperty("cluster_id")
    private String clusterId;

    public boolean isInitialized() {
        return isInitialized;
    }

    public boolean isSealed() {
        return isSealed;
    }

    public boolean isStandby() {
        return isStandby;
    }

    public boolean isPerformanceStandby() {
        return isPerformanceStandby;
    }

    public String getReplicationPerformanceMode() {
        return replicationPerformanceMode;
    }

    public String getReplicationDrMode() {
        return replicationDrMode;
    }

    public long getServerTimeUtc() {
        return serverTimeUtc;
    }

    public String getVersion() {
        return version;
    }

    public String getClusterName() {
        return clusterName;
    }

    public String getClusterId() {
        return clusterId;
    }
}
