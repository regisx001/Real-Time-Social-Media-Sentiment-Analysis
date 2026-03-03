package com.regisx001.core.domain.dto;

/**
 * Represents a tweet event that has been processed with sentiment analysis.
 *
 * @param tweetId   the unique identifier of the tweet
 * @param sentiment the predicted sentiment (e.g., Positive, Negative, Neutral)
 * @param score     the confidence score or sentiment magnitude
 */
public record ProcessedTweetEvent(
        String tweetId,
        String sentiment,
        double score) {
}
