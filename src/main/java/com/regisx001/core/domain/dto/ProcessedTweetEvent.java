package com.regisx001.core.domain.dto;

public record ProcessedTweetEvent(
                String tweetId,
                String sentiment,
                double score) {
}
