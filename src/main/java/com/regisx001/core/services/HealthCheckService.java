package com.regisx001.core.services;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.TopicDescription;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.regisx001.core.domain.dto.KafkaMetrics;
import com.regisx001.core.domain.dto.PostgresMetrics;
import com.regisx001.core.domain.dto.ServiceHealth;
import com.regisx001.core.domain.dto.SparkMetrics;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class HealthCheckService {

    private final DataSource dataSource;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${spring.kafka.bootstrap-servers}")
    private String kafkaBootstrapServers;

    @Value("${health.spark.master-url:http://localhost:8080}")
    private String sparkMasterUrl;

    // ---------------------------------------------------------------
    // PostgreSQL – simple ping
    // ---------------------------------------------------------------
    public ServiceHealth checkPostgres() {
        long start = System.currentTimeMillis();
        try (Connection conn = dataSource.getConnection()) {
            boolean valid = conn.isValid(5);
            long latency = System.currentTimeMillis() - start;
            if (valid)
                return ServiceHealth.up("PostgreSQL", latency);
            return ServiceHealth.down("PostgreSQL", "Connection validation returned false");
        } catch (Exception ex) {
            log.warn("PostgreSQL health check failed: {}", ex.getMessage());
            return ServiceHealth.down("PostgreSQL", ex.getMessage());
        }
    }

    // ---------------------------------------------------------------
    // PostgreSQL – detailed metrics
    // ---------------------------------------------------------------
    public PostgresMetrics postgresMetrics() {
        long start = System.currentTimeMillis();
        try (Connection conn = dataSource.getConnection();
                Statement st = conn.createStatement()) {

            String version = null;
            try (ResultSet rs = st.executeQuery("SELECT version()")) {
                if (rs.next())
                    version = rs.getString(1);
            }

            int activeConnections = 0;
            try (ResultSet rs = st.executeQuery(
                    "SELECT count(*) FROM pg_stat_activity WHERE state = 'active'")) {
                if (rs.next())
                    activeConnections = rs.getInt(1);
            }

            int maxConnections = 100;
            try (ResultSet rs = st.executeQuery("SHOW max_connections")) {
                if (rs.next())
                    maxConnections = Integer.parseInt(rs.getString(1).trim());
            }

            long dbSizeBytes = 0;
            try (ResultSet rs = st.executeQuery("SELECT pg_database_size(current_database())")) {
                if (rs.next())
                    dbSizeBytes = rs.getLong(1);
            }

            String dbSizeHuman = null;
            try (ResultSet rs = st.executeQuery("SELECT pg_size_pretty(pg_database_size(current_database()))")) {
                if (rs.next())
                    dbSizeHuman = rs.getString(1);
            }

            long uptimeSeconds = 0;
            try (ResultSet rs = st.executeQuery(
                    "SELECT EXTRACT(EPOCH FROM (now() - pg_postmaster_start_time()))::bigint")) {
                if (rs.next())
                    uptimeSeconds = rs.getLong(1);
            }

            long latency = System.currentTimeMillis() - start;
            return new PostgresMetrics("UP", latency, null, version,
                    activeConnections, maxConnections, dbSizeBytes, dbSizeHuman, uptimeSeconds);

        } catch (Exception ex) {
            log.warn("PostgreSQL metrics failed: {}", ex.getMessage());
            return new PostgresMetrics("DOWN", null, ex.getMessage(),
                    null, 0, 0, 0, null, 0);
        }
    }

    // ---------------------------------------------------------------
    // Kafka – simple ping
    // ---------------------------------------------------------------
    public ServiceHealth checkKafka() {
        long start = System.currentTimeMillis();
        Map<String, Object> config = Map.of(
                AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapServers,
                AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, "5000",
                AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, "5000");
        try (AdminClient admin = AdminClient.create(config)) {
            admin.listTopics().names().get(5, TimeUnit.SECONDS);
            long latency = System.currentTimeMillis() - start;
            return ServiceHealth.up("Kafka", latency);
        } catch (Exception ex) {
            log.warn("Kafka health check failed: {}", ex.getMessage());
            return ServiceHealth.down("Kafka", ex.getMessage());
        }
    }

    // ---------------------------------------------------------------
    // Kafka – detailed metrics
    // ---------------------------------------------------------------
    public KafkaMetrics kafkaMetrics() {
        long start = System.currentTimeMillis();
        Map<String, Object> config = Map.of(
                AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapServers,
                AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, "5000",
                AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, "5000");
        try (AdminClient admin = AdminClient.create(config)) {
            Set<String> topicNames = admin.listTopics().names().get(5, TimeUnit.SECONDS);

            Map<String, TopicDescription> descriptions = admin
                    .describeTopics(topicNames).allTopicNames().get(5, TimeUnit.SECONDS);

            List<KafkaMetrics.TopicInfo> topics = descriptions.values().stream()
                    .map(td -> new KafkaMetrics.TopicInfo(
                            td.name(),
                            td.partitions().size(),
                            td.partitions().isEmpty() ? 0
                                    : td.partitions().get(0).replicas().size()))
                    .sorted((a, b) -> a.name().compareTo(b.name()))
                    .collect(Collectors.toList());

            int brokerCount = admin.describeCluster().nodes()
                    .get(5, TimeUnit.SECONDS).size();

            long latency = System.currentTimeMillis() - start;
            return new KafkaMetrics("UP", latency, null, brokerCount,
                    topicNames.size(), topics);

        } catch (Exception ex) {
            log.warn("Kafka metrics failed: {}", ex.getMessage());
            return new KafkaMetrics("DOWN", null, ex.getMessage(),
                    0, 0, List.of());
        }
    }

    // ---------------------------------------------------------------
    // Spark – simple ping
    // ---------------------------------------------------------------
    public ServiceHealth checkSpark() {
        long start = System.currentTimeMillis();
        String endpoint = sparkMasterUrl + "/json/";
        try {
            HttpURLConnection conn = (HttpURLConnection) URI.create(endpoint).toURL().openConnection();
            conn.setConnectTimeout(5_000);
            conn.setReadTimeout(5_000);
            conn.setRequestMethod("GET");
            int code = conn.getResponseCode();
            long latency = System.currentTimeMillis() - start;
            if (code >= 200 && code < 400) {
                try (var in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    String body = in.lines().collect(Collectors.joining());
                    conn.disconnect();
                    if (body.contains("\"status\"") && body.contains("\"ALIVE\"")) {
                        return ServiceHealth.up("Spark", latency);
                    }
                    return ServiceHealth.down("Spark", "Status not ALIVE");
                }
            }
            conn.disconnect();
            return ServiceHealth.down("Spark", "HTTP " + code);
        } catch (Exception ex) {
            log.warn("Spark health check failed: {}", ex.getMessage());
            return ServiceHealth.down("Spark", ex.getMessage());
        }
    }

    // ---------------------------------------------------------------
    // Spark – detailed metrics (parses /json/ response)
    // ---------------------------------------------------------------
    public SparkMetrics sparkMetrics() {
        long start = System.currentTimeMillis();
        String endpoint = sparkMasterUrl + "/json/";
        try {
            HttpURLConnection conn = (HttpURLConnection) URI.create(endpoint).toURL().openConnection();
            conn.setConnectTimeout(5_000);
            conn.setReadTimeout(5_000);
            conn.setRequestMethod("GET");
            int code = conn.getResponseCode();
            if (code < 200 || code >= 400) {
                conn.disconnect();
                return new SparkMetrics("DOWN", null, "HTTP " + code,
                        null, 0, 0, 0, 0, 0, 0, 0, List.of());
            }
            String body;
            try (var in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                body = in.lines().collect(Collectors.joining());
            }
            conn.disconnect();
            long latency = System.currentTimeMillis() - start;

            JsonNode root = objectMapper.readTree(body);

            String masterUrl = root.path("url").asText(null);
            int aliveWorkers = root.path("aliveworkers").asInt(0);
            int totalCores = root.path("cores").asInt(0);
            int usedCores = root.path("coresused").asInt(0);
            long totalMem = root.path("memory").asLong(0);
            long usedMem = root.path("memoryused").asLong(0);
            int activeApps = root.path("activeapps").size();
            int completedApps = root.path("completedapps").size();

            List<SparkMetrics.WorkerInfo> workers = new ArrayList<>();
            for (JsonNode w : root.path("workers")) {
                workers.add(new SparkMetrics.WorkerInfo(
                        w.path("id").asText(),
                        w.path("host").asText(),
                        w.path("port").asInt(),
                        w.path("cores").asInt(),
                        w.path("coresused").asInt(),
                        w.path("memory").asLong(),
                        w.path("memoryused").asLong(),
                        w.path("state").asText()));
            }

            boolean alive = body.contains("\"status\"") && body.contains("\"ALIVE\"");
            return new SparkMetrics(
                    alive ? "UP" : "DOWN", latency,
                    alive ? null : "Master status not ALIVE",
                    masterUrl, aliveWorkers, totalCores, usedCores,
                    totalMem, usedMem, activeApps, completedApps, workers);

        } catch (Exception ex) {
            log.warn("Spark metrics failed: {}", ex.getMessage());
            return new SparkMetrics("DOWN", null, ex.getMessage(),
                    null, 0, 0, 0, 0, 0, 0, 0, List.of());
        }
    }
}
