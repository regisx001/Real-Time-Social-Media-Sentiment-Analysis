package com.regisx001.core.domain.dto;

/**
 * Represents a raw tweet event to be processed.
 *
 * @param tweetId   the unique identifier of the tweet
 * @param text      the content of the tweet
 * @param timestamp the time the tweet was created or received
 */
public record TweetEvent(
        String tweetId,
        String text,
        long timestamp) {
}
