package com.regisx001.core.services;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.regisx001.core.domain.dto.AnalyticsReport;
import com.regisx001.core.domain.dto.AnalyticsSummary;
import com.regisx001.core.domain.dto.LiveTweetDto;
import com.regisx001.core.domain.dto.SentimentTimePoint;
import com.regisx001.core.domain.entities.Tweet;
import com.regisx001.core.repository.TweetRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for calculating overall sentiment analytics and managing live feeds.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    /**
     * Repository used directly to run aggregations over stored tweets.
     */
    private final TweetRepository tweetRepository;

    // ---------------------------------------------------------------
    // Full analytics report (summary + time-series)
    // bucket = "hour" or "day"
    // lookbackHours = how far back to query for the time-series
    // ---------------------------------------------------------------
    /**
     * Generates a full analytics report containing summaries and time-series data.
     *
     * @param bucket          the aggregation bucket ("hour" or "minute" depending
     *                        on requirements)
     * @param lookbackMinutes how far back (in minutes) to gather time-series points
     *                        for the report
     * @return a comprehensive AnalyticsReport containing the data
     */
    public AnalyticsReport getReport(String bucket, int lookbackMinutes) {
        AnalyticsSummary summary = buildSummary();
        List<SentimentTimePoint> timeSeries = buildTimeSeries(bucket, lookbackMinutes);
        return new AnalyticsReport(summary, timeSeries);
    }

    // ---------------------------------------------------------------
    // Summary: total counts + 60-second rolling throughput
    // ---------------------------------------------------------------
    private AnalyticsSummary buildSummary() {
        List<Object[]> rows = tweetRepository.sentimentCounts();

        long positive = 0, negative = 0, neutral = 0;
        for (Object[] row : rows) {
            String label = (String) row[0];
            long cnt = ((Number) row[1]).longValue();
            switch (label) {
                case "POSITIVE" -> positive = cnt;
                case "NEGATIVE" -> negative = cnt;
                default -> neutral += cnt; // NEUTRAL + UNKNOWN
            }
        }
        long total = positive + negative + neutral;

        long processedLast60s = tweetRepository.countProcessedSince(
                LocalDateTime.now().minusSeconds(60));
        double throughput = processedLast60s / 60.0;

        return new AnalyticsSummary(total, positive, negative, neutral, throughput);
    }

    // ---------------------------------------------------------------
    // Time-series: pivot rows into SentimentTimePoint list
    // ---------------------------------------------------------------
    private List<SentimentTimePoint> buildTimeSeries(String bucket, int lookbackMinutes) {
        LocalDateTime since = LocalDateTime.now().minusMinutes(lookbackMinutes);
        List<Object[]> rows = tweetRepository.sentimentTimeSeries(bucket, since);

        // Use LinkedHashMap to preserve time order
        Map<String, long[]> buckets = new LinkedHashMap<>();
        for (Object[] row : rows) {
            // Hibernate 6 returns LocalDateTime for timestamp columns in native queries
            LocalDateTime ldt;
            if (row[0] instanceof Timestamp ts) {
                ldt = ts.toLocalDateTime();
            } else {
                ldt = (LocalDateTime) row[0];
            }
            String sentiment = (String) row[1];
            long cnt = ((Number) row[2]).longValue();
            String key = ldt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

            buckets.computeIfAbsent(key, k -> new long[3]);
            long[] vals = buckets.get(key);
            switch (sentiment) {
                case "POSITIVE" -> vals[0] += cnt;
                case "NEGATIVE" -> vals[1] += cnt;
                default -> vals[2] += cnt;
            }
        }

        List<SentimentTimePoint> result = new ArrayList<>(buckets.size());
        buckets.forEach((time, vals) -> result.add(new SentimentTimePoint(time, vals[0], vals[1], vals[2])));
        return result;
    }

    // ---------------------------------------------------------------
    // Live feed: last N processed tweets
    // ---------------------------------------------------------------
    /**
     * Fetches a direct stream of the latest completely processed tweets.
     *
     * @param limit the max number of recent tweets to retrieve
     * @return a list of live tweets mapped for the UI
     */
    public List<LiveTweetDto> getLiveFeed(int limit) {
        List<Tweet> tweets = tweetRepository.findLatestProcessed(limit);
        List<LiveTweetDto> result = new ArrayList<>(tweets.size());
        for (Tweet t : tweets) {
            Map<String, Object> pd = t.getProcessedData();
            if (pd == null)
                continue;

            String text = t.getRawData() != null
                    ? (String) t.getRawData().get("text")
                    : "";
            String sentiment = (String) pd.getOrDefault("sentiment", "UNKNOWN");
            double score = pd.get("score") instanceof Number n ? n.doubleValue() : 0.0;
            String processedAt = t.getProcessedAt() != null
                    ? t.getProcessedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                    : "";

            result.add(new LiveTweetDto(t.getId(), text, sentiment, score, processedAt));
        }
        return result;
    }
}
