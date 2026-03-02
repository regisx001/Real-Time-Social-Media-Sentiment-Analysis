package com.regisx001.core.domain.dto;

import java.time.Instant;

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
