package com.regisx001.core.services;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.regisx001.core.domain.dto.TweetEvent;

import lombok.extern.slf4j.Slf4j;

/**
 * Service responsible for publishing raw tweet events to Kafka for processing.
 */
@Slf4j
@Service
public class TweetProducer {

    /**
     * Kafka endpoint template used to send message streams.
     */
    private final KafkaTemplate<String, TweetEvent> kafkaTemplate;

    /**
     * The target Kafka topic for raw tweets.
     */
    private static final String TOPIC = "tweets.raw";

    /**
     * Constructs a new TweetProducer.
     *
     * @param kafkaTemplate the KafkaTemplate to be used for sending messages
     */
    public TweetProducer(KafkaTemplate<String, TweetEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Sends a raw tweet event to the configured Kafka topic.
     *
     * @param tweet the tweet event to be published
     */
    public void sendTweet(TweetEvent tweet) {
        log.info("Sending tweet to Kafka: " + tweet);
        kafkaTemplate.send(TOPIC, tweet.tweetId(), tweet);
    }
}
