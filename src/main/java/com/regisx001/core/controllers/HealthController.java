package com.regisx001.core.controllers;

import java.time.Duration;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.regisx001.core.domain.dto.HealthReport;
import com.regisx001.core.domain.dto.ServiceHealth;
import com.regisx001.core.services.HealthCheckService;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
public class HealthController {

    private final HealthCheckService healthCheckService;

    // ---------------------------------------------------------------
    // SSE stream – pushes a full HealthReport every 15 seconds
    // GET /api/health/stream
    // ---------------------------------------------------------------
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<HealthReport>> healthStream() {
        return Flux.interval(Duration.ZERO, Duration.ofSeconds(3))
                .map(seq -> {
                    HealthReport report = buildReport();
                    return ServerSentEvent.<HealthReport>builder()
                            .id(String.valueOf(seq))
                            .event("health")
                            .data(report)
                            .build();
                });
    }

    // ---------------------------------------------------------------
    // REST snapshots (unchanged)
    // ---------------------------------------------------------------

    /**
     * GET /api/health
     * Aggregated health report for all infrastructure components.
     * Returns 200 when all services are UP, 207 (Multi-Status) when degraded.
     */
    @GetMapping
    public ResponseEntity<HealthReport> allHealth() {
        HealthReport report = buildReport();
        int statusCode = "UP".equals(report.overall()) ? 200 : 207;
        return ResponseEntity.status(statusCode).body(report);
    }

    /** GET /api/health/postgres */
    @GetMapping("/postgres")
    public ResponseEntity<ServiceHealth> postgresHealth() {
        return toResponse(healthCheckService.checkPostgres());
    }

    /** GET /api/health/kafka */
    @GetMapping("/kafka")
    public ResponseEntity<ServiceHealth> kafkaHealth() {
        return toResponse(healthCheckService.checkKafka());
    }

    /** GET /api/health/spark */
    @GetMapping("/spark")
    public ResponseEntity<ServiceHealth> sparkHealth() {
        return toResponse(healthCheckService.checkSpark());
    }

    // ---------------------------------------------------------------

    private HealthReport buildReport() {
        return HealthReport.of(List.of(
                healthCheckService.checkPostgres(),
                healthCheckService.checkKafka(),
                healthCheckService.checkSpark()));
    }

    private ResponseEntity<ServiceHealth> toResponse(ServiceHealth health) {
        int code = "UP".equals(health.status()) ? 200 : 503;
        return ResponseEntity.status(code).body(health);
    }
}
