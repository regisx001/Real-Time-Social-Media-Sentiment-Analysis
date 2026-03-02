package com.regisx001.core.domain.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SparkMetrics(
        String status,
        Long latencyMs,
        String message,
        String masterUrl,
        int aliveWorkers,
        int totalCores,
        int usedCores,
        long totalMemoryMb,
        long usedMemoryMb,
        int activeApps,
        int completedApps,
        List<WorkerInfo> workers) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record WorkerInfo(
            String id,
            String host,
            int port,
            int cores,
            int coresUsed,
            long memoryMb,
            long memoryUsedMb,
            String state) {
    }
}
