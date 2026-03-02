package com.regisx001.core.domain.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record KafkaMetrics(
        String status,
        Long latencyMs,
        String message,
        int brokerCount,
        int topicCount,
        List<TopicInfo> topics) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record TopicInfo(
            String name,
            int partitions,
            int replicationFactor) {
    }
}
