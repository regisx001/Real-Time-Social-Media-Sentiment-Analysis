package com.regisx001.core.domain.dto;

/**
 * Summary of sentiment analytics for processed tweets.
 *
 * @param totalProcessed   the total number of tweets processed
 * @param positive         the total number of positive tweets
 * @param negative         the total number of negative tweets
 * @param neutral          the total number of neutral tweets
 * @param throughputPerSec the processing throughput in tweets per second
 */
public record AnalyticsSummary(
                long totalProcessed,
                long positive,
                long negative,
                long neutral,
                double throughputPerSec) {
}
