package com.regisx001.core.domain.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Health and performance metrics for an Apache Spark cluster.
 *
 * @param status        the overall status of the cluster (e.g., UP, DOWN)
 * @param latencyMs     the latency for the metrics request in milliseconds
 * @param message       an optional error or status message
 * @param masterUrl     the URL of the Spark Master node
 * @param aliveWorkers  the number of currently active workers
 * @param totalCores    the total number of CPU cores across all workers
 * @param usedCores     the current number of cores being used
 * @param totalMemoryMb the total available memory in megabytes
 * @param usedMemoryMb  the amount of currently used memory in megabytes
 * @param activeApps    the number of active running applications
 * @param completedApps the number of applications that have finished running
 * @param workers       the list of details for each worker node
 */
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

        /**
         * Represents the status and resource utilization of a single Spark worker node.
         *
         * @param id           the unique identifier for the worker
         * @param host         the hostname or IP address of the worker
         * @param port         the port the worker is running on
         * @param cores        the total number of cores available on the worker
         * @param coresUsed    the number of cores currently used by the worker
         * @param memoryMb     the total memory available on the worker in megabytes
         * @param memoryUsedMb the memory currently used on the worker in megabytes
         * @param state        the current operational state of the worker
         */
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
