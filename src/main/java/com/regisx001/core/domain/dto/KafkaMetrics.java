package com.regisx001.core.domain.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Metrics and cluster status information for an Apache Kafka deployment.
 *
 * @param status      the overarching status of the Kafka cluster
 * @param latencyMs   the response latency in milliseconds to retrieve metrics
 * @param message     an optional message regarding the cluster status
 * @param brokerCount the number of available active broker nodes
 * @param topicCount  the total number of topics within the cluster
 * @param topics      detailed information about specific metrics-relevant
 *                    topics
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record KafkaMetrics(
                String status,
                Long latencyMs,
                String message,
                int brokerCount,
                int topicCount,
                List<TopicInfo> topics) {

        /**
         * Information about a specific Kafka topic.
         *
         * @param name              the name of the topic
         * @param partitions        the number of partitions allocated to the topic
         * @param replicationFactor the replication factor configured for the topic
         */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public record TopicInfo(
                        String name,
                        int partitions,
                        int replicationFactor) {
        }
}
