package com.regisx001.core.controllers;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.regisx001.core.domain.entities.Tweet;
import com.regisx001.core.services.TweetService;

/**
 * Controller for managing tweets through REST API.
 */
@RestController
@RequestMapping("/api/tweets")
public class TweetController {

    /**
     * Service for handling tweet-related operations.
     */
    private final TweetService tweetService;

    /**
     * Constructs a new TweetController with the specified TweetService.
     *
     * @param tweetService the service to be used for tweet operations
     */
    public TweetController(TweetService tweetService) {
        this.tweetService = tweetService;
    }

    /**
     * Creates a new tweet based on the provided payload.
     *
     * @param payload a map containing the tweet text and optionally its source
     * @return a ResponseEntity containing the created tweet, or a bad request
     *         response if the text is invalid
     */
    @PostMapping
    public ResponseEntity<Tweet> createTweet(@RequestBody Map<String, String> payload) {
        String text = payload.get("text");
        String source = payload.getOrDefault("source", "api");
        if (text == null || text.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        Tweet created = tweetService.createTweet(text, source);
        return ResponseEntity.ok(created);
    }

    /**
     * Retrieves a list of all tweets.
     *
     * @return a ResponseEntity containing the list of all tweets
     */
    @GetMapping
    public ResponseEntity<List<Tweet>> listTweets() {
        return ResponseEntity.ok(tweetService.getAllTweets());
    }
}
