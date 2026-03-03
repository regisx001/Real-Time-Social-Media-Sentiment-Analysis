package com.regisx001.core.domain.dto;

/**
 * Aggregated sentiment metrics for a specific time period.
 *
 * @param time     an ISO-8601 string representing a truncated time bucket
 * @param positive the number of positive sentiments recorded in the bucket
 * @param negative the number of negative sentiments recorded in the bucket
 * @param neutral  the number of neutral sentiments recorded in the bucket
 */
public record SentimentTimePoint(
                String time, // ISO-8601 truncated bucket
                long positive,
                long negative,
                long neutral) {
}
