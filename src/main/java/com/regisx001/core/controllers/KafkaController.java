package com.regisx001.core.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.regisx001.core.domain.dto.TweetEvent;
import com.regisx001.core.services.TweetProducer;

/**
 * Controller for publishing tweet events directly to Kafka.
 */
@RestController
@RequestMapping("/api/kafka")
public class KafkaController {

    /**
     * Service for producing tweet events to Kafka.
     */
    private final TweetProducer tweetProducer;

    /**
     * Constructs a new KafkaController with the given TweetProducer.
     *
     * @param tweetProducer the producer service for sending event messages to Kafka
     */
    public KafkaController(TweetProducer tweetProducer) {
        this.tweetProducer = tweetProducer;
    }

    /**
     * Sends a tweet event to the configured Kafka topic.
     *
     * @param tweet the tweet event to send
     * @return a ResponseEntity with a success message
     */
    @PostMapping("/send")
    public ResponseEntity<String> sendTweet(@RequestBody TweetEvent tweet) {
        tweetProducer.sendTweet(tweet);
        return ResponseEntity.ok("Message sent to Kafka");
    }
}
