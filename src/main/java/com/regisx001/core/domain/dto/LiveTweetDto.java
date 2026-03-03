package com.regisx001.core.domain.dto;

public record LiveTweetDto(
        Long id,
        String text,
        String sentiment,
        double score,
        String processedAt) { // ISO-8601
}
