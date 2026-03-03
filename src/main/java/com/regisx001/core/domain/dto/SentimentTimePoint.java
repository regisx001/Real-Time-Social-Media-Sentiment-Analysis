package com.regisx001.core.domain.dto;

public record SentimentTimePoint(
        String time, // ISO-8601 truncated bucket
        long positive,
        long negative,
        long neutral) {
}
