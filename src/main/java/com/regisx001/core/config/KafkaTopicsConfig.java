package com.regisx001.core.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Configuration class for Kafka topics.
 */
@Configuration
public class KafkaTopicsConfig {

    /**
     * Creates a new Kafka topic for raw tweets.
     * 
     * @return the NewTopic instance for "tweets.raw"
     */
    @Bean
    public NewTopic rawTweetsTopic() {
        return TopicBuilder.name("tweets.raw")
                .partitions(3)
                .replicas(1)
                .build();
    }

    /**
     * Creates a new Kafka topic for processed tweets.
     * 
     * @return the NewTopic instance for "tweets.processed"
     */
    @Bean
    public NewTopic processedTweetsTopic() {
        return TopicBuilder.name("tweets.processed")
                .partitions(3)
                .replicas(1)
                .build();
    }
}
