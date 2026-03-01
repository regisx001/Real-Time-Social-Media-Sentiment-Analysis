package com.regisx001.core.domain.dto;

public record TweetEvent(
                String tweetId,
                String text,
                long timestamp) {
}
