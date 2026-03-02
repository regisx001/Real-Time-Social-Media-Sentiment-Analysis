package com.regisx001.core.controllers;

import java.time.Duration;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.regisx001.core.domain.dto.DetailedHealthReport;
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
    // SSE – simple health stream (every 3 s)
    // GET /api/health/stream
    // ---------------------------------------------------------------
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<HealthReport>> healthStream() {
        return Flux.interval(Duration.ZERO, Duration.ofSeconds(3))
                .map(seq -> ServerSentEvent.<HealthReport>builder()
                        .id(String.valueOf(seq))
                        .event("health")
                        .data(buildReport())
                        .build());
    }

    // ---------------------------------------------------------------
    // SSE – detailed metrics stream (every 3 s)
    // GET /api/health/details/stream
    // ---------------------------------------------------------------
    @GetMapping(value = "/details/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<DetailedHealthReport>> detailsStream() {
        return Flux.interval(Duration.ZERO, Duration.ofSeconds(3))
                .map(seq -> ServerSentEvent.<DetailedHealthReport>builder()
                        .id(String.valueOf(seq))
                        .event("health-details")
                        .data(buildDetailedReport())
                        .build());
    }

    // ---------------------------------------------------------------
    // REST snapshots
    // ---------------------------------------------------------------

    @GetMapping
    public ResponseEntity<HealthReport> allHealth() {
        HealthReport report = buildReport();
        return ResponseEntity.status("UP".equals(report.overall()) ? 200 : 207).body(report);
    }

    @GetMapping("/details")
    public ResponseEntity<DetailedHealthReport> detailedHealth() {
        DetailedHealthReport report = buildDetailedReport();
        return ResponseEntity.status("UP".equals(report.overall()) ? 200 : 207).body(report);
    }

    @GetMapping("/postgres")
    public ResponseEntity<ServiceHealth> postgresHealth() {
        return toResponse(healthCheckService.checkPostgres());
    }

    @GetMapping("/kafka")
    public ResponseEntity<ServiceHealth> kafkaHealth() {
        return toResponse(healthCheckService.checkKafka());
    }

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

    private DetailedHealthReport buildDetailedReport() {
        return DetailedHealthReport.of(
                healthCheckService.postgresMetrics(),
                healthCheckService.kafkaMetrics(),
                healthCheckService.sparkMetrics());
    }

    private ResponseEntity<ServiceHealth> toResponse(ServiceHealth health) {
        return ResponseEntity.status("UP".equals(health.status()) ? 200 : 503).body(health);
    }
}
