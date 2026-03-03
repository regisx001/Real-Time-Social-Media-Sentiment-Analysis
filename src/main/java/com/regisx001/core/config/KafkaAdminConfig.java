package com.regisx001.core.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaAdmin;

/**
 * Configuration class for Kafka Admin.
 */
@Configuration
public class KafkaAdminConfig {

    /**
     * Creates a KafkaAdmin bean.
     * 
     * @return the KafkaAdmin instance
     */
    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put("bootstrap.servers", "localhost:9092");
        return new KafkaAdmin(configs);
    }
}
