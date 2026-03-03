package com.regisx001.core.domain.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Metrics and health information for the PostgreSQL database connection.
 *
 * @param status            the connection status (e.g., UP, DOWN)
 * @param latencyMs         the latency of the connection in milliseconds
 * @param message           an optional message or error reason
 * @param version           the PostgreSQL server version
 * @param activeConnections the current number of active connections
 * @param maxConnections    the maximum allowed connections
 * @param dbSizeBytes       the size of the database in bytes
 * @param dbSizeHuman       the human-readable size of the database
 * @param uptimeSeconds     the uptime of the database server in seconds
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PostgresMetrics(
                String status,
                Long latencyMs,
                String message,
                String version,
                int activeConnections,
                int maxConnections,
                long dbSizeBytes,
                String dbSizeHuman,
                long uptimeSeconds) {
}
