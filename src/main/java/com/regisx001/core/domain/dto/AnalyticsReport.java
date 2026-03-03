package com.regisx001.core.domain.dto;

import java.util.List;

public record AnalyticsReport(
        AnalyticsSummary summary,
        List<SentimentTimePoint> timeSeries) {
}
