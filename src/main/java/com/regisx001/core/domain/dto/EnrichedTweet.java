package com.regisx001.core.domain.dto;

import java.util.List;

public record EnrichedTweet(
        String tweet_id,
        String event_timestamp,
        String intent,
        List<String> entities) {
}
