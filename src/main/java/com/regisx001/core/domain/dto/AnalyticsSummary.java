package com.regisx001.core.domain.dto;

public record AnalyticsSummary(
        long totalProcessed,
        long positive,
        long negative,
        long neutral,
        double throughputPerSec) {
}
