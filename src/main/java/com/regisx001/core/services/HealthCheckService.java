package com.regisx001.core.services;

import java.net.HttpURLConnection;
import java.net.URI;
import java.sql.Connection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.regisx001.core.domain.dto.ServiceHealth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class HealthCheckService {

    private final DataSource dataSource;

    @Value("${spring.kafka.bootstrap-servers}")
    private String kafkaBootstrapServers;

    @Value("${health.spark.master-url:http://localhost:8080}")
    private String sparkMasterUrl;

    // ---------------------------------------------------------------
    // PostgreSQL
    // ---------------------------------------------------------------
    public ServiceHealth checkPostgres() {
        long start = System.currentTimeMillis();
        try (Connection conn = dataSource.getConnection()) {
            boolean valid = conn.isValid(5);
            long latency = System.currentTimeMillis() - start;
            if (valid) {
                return ServiceHealth.up("PostgreSQL", latency);
            }
            return ServiceHealth.down("PostgreSQL", "Connection validation returned false");
        } catch (Exception ex) {
            log.warn("PostgreSQL health check failed: {}", ex.getMessage());
            return ServiceHealth.down("PostgreSQL", ex.getMessage());
        }
    }

    // ---------------------------------------------------------------
    // Kafka
    // ---------------------------------------------------------------
    public ServiceHealth checkKafka() {
        long start = System.currentTimeMillis();
        Map<String, Object> config = Map.of(
                AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapServers,
                AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, "5000",
                AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, "5000");
        try (AdminClient admin = AdminClient.create(config)) {
            Set<String> topics = admin.listTopics().names().get(5, TimeUnit.SECONDS);
            long latency = System.currentTimeMillis() - start;
            log.debug("Kafka health check OK – topics visible: {}", topics.size());
            return ServiceHealth.up("Kafka", latency);
        } catch (Exception ex) {
            log.warn("Kafka health check failed: {}", ex.getMessage());
            return ServiceHealth.down("Kafka", ex.getMessage());
        }
    }

    // ---------------------------------------------------------------
    // Spark Master (probes the standalone master REST endpoint /json/)
    // Port 8080 is the persistent Spark Master Web UI — always up while
    // the master container is running, unlike port 4040 which is
    // ephemeral (only alive during an active job).
    // ---------------------------------------------------------------
    public ServiceHealth checkSpark() {
        long start = System.currentTimeMillis();
        // /json/ returns cluster state: {"status":"ALIVE", "workers":[...], ...}
        String endpoint = sparkMasterUrl + "/json/";
        try {
            HttpURLConnection conn = (HttpURLConnection) URI.create(endpoint).toURL().openConnection();
            conn.setConnectTimeout(5_000);
            conn.setReadTimeout(5_000);
            conn.setRequestMethod("GET");
            int code = conn.getResponseCode();
            long latency = System.currentTimeMillis() - start;
            if (code >= 200 && code < 400) {
                // Read and inspect the status field from the JSON body
                try (var in = new java.io.BufferedReader(
                        new java.io.InputStreamReader(conn.getInputStream()))) {
                    String body = in.lines().collect(java.util.stream.Collectors.joining());
                    conn.disconnect();
                    // The Spark Master JSON uses spaces around the colon: "status" : "ALIVE"
                    if (body.contains("\"status\"") && body.contains("\"ALIVE\"")) {
                        return ServiceHealth.up("Spark", latency);
                    }
                    return ServiceHealth.down("Spark", "Master responded but status is not ALIVE: " + body);
                }
            }
            conn.disconnect();
            return ServiceHealth.down("Spark", "HTTP " + code + " from " + endpoint);
        } catch (Exception ex) {
            log.warn("Spark health check failed: {}", ex.getMessage());
            return ServiceHealth.down("Spark", ex.getMessage());
        }
    }
}
