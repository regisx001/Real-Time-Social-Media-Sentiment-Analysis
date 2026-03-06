package com.regisx001.core.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO representing the internal health and GPU metrics of the external Python
 * Intent service.
 */
public record IntentHealthResponse(
        String status,

        @JsonProperty("models_loaded") boolean modelsLoaded,

        @JsonProperty("system_metrics") SystemMetrics systemMetrics) {
    /**
     * DTO for system metrics containing CPU, RAM, and GPU info.
     */
    public record SystemMetrics(
            @JsonProperty("cpu_usage_percent") double cpuUsagePercent,

            @JsonProperty("ram_gb") RamGb ramGb,

            @JsonProperty("gpu") GpuStats gpu) {
    }

    /**
     * DTO for System RAM metrics.
     */
    public record RamGb(
            double total,
            double used,
            double free,
            double percent) {
    }

    /**
     * DTO for GPU metrics.
     */
    public record GpuStats(
            boolean available,

            @JsonProperty("device_name") String deviceName,

            @JsonProperty("vram_gb") VramGb vramGb) {
    }

    /**
     * DTO for GPU VRAM allocation.
     */
    public record VramGb(
            double total,
            double allocated,
            double reserved,
            double free) {
    }
}
