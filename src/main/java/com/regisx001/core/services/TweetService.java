package com.regisx001.core.services;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regisx001.core.domain.dto.TweetEvent;
import com.regisx001.core.domain.entities.Tweet;
import com.regisx001.core.repository.TweetRepository;

import lombok.RequiredArgsConstructor;

/**
 * Service for handling business logic operations related to tweets.
 */
@Service
@RequiredArgsConstructor
public class TweetService {

    /**
     * Repository for persisting initial raw tweets to the database.
     */
    private final TweetRepository rawTweetRepository;

    /**
     * Producer for sending ingested tweet events to Kafka for processing.
     */
    private final TweetProducer tweetProducer;

    /**
     * Creates a new tweet, saves it in the database, and publishes it to Kafka.
     *
     * @param text   the content of the tweet message
     * @param source the source device or application of the tweet
     * @return the saved tweet entity
     */
    @Transactional
    public Tweet createTweet(String text, String source) {
        // 1. Save to DB
        Tweet tweet = new Tweet();
        tweet.setIngestedAt(LocalDateTime.now());

        Map<String, Object> rawData = new HashMap<>();
        rawData.put("text", text);
        rawData.put("source", source);
        tweet.setRawData(rawData);

        tweet = rawTweetRepository.save(tweet);

        // 2. Produce event to Kafka
        TweetEvent event = new TweetEvent(
                tweet.getId().toString(),
                (String) tweet.getRawData().get("text"),
                tweet.getIngestedAt().toEpochSecond(ZoneOffset.UTC));
        tweetProducer.sendTweet(event);

        return tweet;
    }

    /**
     * Retrieves all tweets from the repository.
     *
     * @return a list of all tweets
     */
    public List<Tweet> getAllTweets() {
        return rawTweetRepository.findAll();
    }
}
