package com.regisx001.core.domain.dto;

/**
 * Data Transfer Object representing a live stream tweet including its sentiment
 * analysis.
 *
 * @param id          the primary identifier of the tweet
 * @param text        the actual content textual data of the tweet
 * @param sentiment   the sentiment class evaluated (e.g., Positive, Negative)
 * @param score       the confidence score backing the evaluated sentiment
 * @param processedAt the ISO-8601 formatted timestamp of when analysis occurred
 */
public record LiveTweetDto(
                Long id,
                String text,
                String sentiment,
                double score,
                String processedAt) { // ISO-8601
}
