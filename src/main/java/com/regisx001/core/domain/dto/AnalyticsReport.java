package com.regisx001.core.domain.dto;

import java.util.List;

/**
 * Represents a comprehensive analytics report containing global summary metrics
 * and a time-series of sentiment data points.
 *
 * @param summary    the overall analytics summary
 * @param timeSeries the sequence of sentiment points over time
 */
public record AnalyticsReport(
                AnalyticsSummary summary,
                List<SentimentTimePoint> timeSeries) {
}
