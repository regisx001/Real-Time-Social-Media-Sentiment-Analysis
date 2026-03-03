package com.regisx001.core.domain.dto;

import java.time.Instant;

/**
 * A detailed health report including specific metrics for all integral system
 * components.
 *
 * @param overall   the aggregated health status of the system (e.g., UP,
 *                  DEGRADED)
 * @param timestamp the timestamp marking when the report was created
 * @param postgres  detailed metrics and health info for the PostgreSQL database
 * @param kafka     detailed metrics and health info for the Kafka cluster
 * @param spark     detailed metrics and health info for the Apache Spark
 *                  cluster
 */
public record DetailedHealthReport(
                String overall,
                Instant timestamp,
                PostgresMetrics postgres,
                KafkaMetrics kafka,
                SparkMetrics spark) {

        public static DetailedHealthReport of(PostgresMetrics pg, KafkaMetrics kafka, SparkMetrics spark) {
                boolean allUp = "UP".equals(pg.status())
                                && "UP".equals(kafka.status())
                                && "UP".equals(spark.status());
                return new DetailedHealthReport(
                                allUp ? "UP" : "DEGRADED",
                                Instant.now(),
                                pg, kafka, spark);
        }
}
