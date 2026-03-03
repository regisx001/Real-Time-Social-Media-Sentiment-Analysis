package com.regisx001.core.controllers;

import java.time.Duration;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.regisx001.core.domain.dto.AnalyticsReport;
import com.regisx001.core.domain.dto.LiveTweetDto;
import com.regisx001.core.domain.entities.Tweet;
import com.regisx001.core.repository.TweetRepository;
import com.regisx001.core.services.AnalyticsService;

import reactor.core.publisher.Flux;

/**
 * Controller for retrieving analytics and live feeds of tweets.
 */
@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    /**
     * Service to compute analytics metrics.
     */
    private final AnalyticsService analyticsService;

    /**
     * Repository to directly fetch tweet data when needed.
     */
    private final TweetRepository tweetRepository;

    /**
     * Constructs a new AnalyticsController with the specified services.
     *
     * @param analyticsService the service for generating analytics reports
     * @param tweetRepository  the repository to access tweet records
     */
    public AnalyticsController(AnalyticsService analyticsService, TweetRepository tweetRepository) {
        this.analyticsService = analyticsService;
        this.tweetRepository = tweetRepository;
    }

    // ---------------------------------------------------------------
    // SSE — full analytics report every 5 s
    // GET /api/analytics/stream
    // ---------------------------------------------------------------
    /**
     * Streams a full analytics report over Server-Sent Events (SSE).
     *
     * @param bucket  the bucket type for aggregation (e.g., "hour")
     * @param hours   the time range in hours
     * @param minutes the time range in minutes (overrides hours if non-negative)
     * @return a Flux of ServerSentEvent containing the current analytics report
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<AnalyticsReport>> analyticsStream(
            @RequestParam(defaultValue = "hour") String bucket,
            @RequestParam(defaultValue = "60") int hours,
            @RequestParam(defaultValue = "-1") int minutes) {
        int effectiveMinutes = minutes >= 0 ? minutes : hours * 60;
        return Flux.interval(Duration.ZERO, Duration.ofSeconds(5))
                .map(seq -> ServerSentEvent.<AnalyticsReport>builder()
                        .id(String.valueOf(seq))
                        .event("analytics")
                        .data(analyticsService.getReport(bucket, effectiveMinutes))
                        .build());
    }

    // ---------------------------------------------------------------
    // SSE — live feed of last 10 processed tweets every 5 s
    // GET /api/analytics/live-feed/stream
    // ---------------------------------------------------------------
    /**
     * Streams a live feed of recently processed tweets via SSE.
     *
     * @return a Flux of ServerSentEvent containing a list of live tweets
     */
    @GetMapping(value = "/live-feed/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<List<LiveTweetDto>>> liveFeedStream() {
        return Flux.interval(Duration.ZERO, Duration.ofSeconds(5))
                .map(seq -> ServerSentEvent.<List<LiveTweetDto>>builder()
                        .id(String.valueOf(seq))
                        .event("live-feed")
                        .data(analyticsService.getLiveFeed(10))
                        .build());
    }

    // ---------------------------------------------------------------
    // REST snapshot — one-shot report
    // GET /api/analytics/report?bucket=hour&hours=24
    // ---------------------------------------------------------------
    /**
     * Retrieves a point-in-time overall analytics report.
     *
     * @param bucket  the bucket type for aggregation (default: "hour")
     * @param hours   how many hours back to look (default: 60)
     * @param minutes how many minutes to look back (overrides hours if
     *                non-negative, default: -1)
     * @return a ResponseEntity containing the analytics report
     */
    @GetMapping("/report")
    public ResponseEntity<AnalyticsReport> report(
            @RequestParam(defaultValue = "hour") String bucket,
            @RequestParam(defaultValue = "60") int hours,
            @RequestParam(defaultValue = "-1") int minutes) {
        int effectiveMinutes = minutes >= 0 ? minutes : hours * 60;
        return ResponseEntity.ok(analyticsService.getReport(bucket, effectiveMinutes));
    }

    // ---------------------------------------------------------------
    // REST — paginated tweet list, newest first
    // GET /api/analytics/tweets?page=0&size=20
    // ---------------------------------------------------------------
    /**
     * Retrieves a paginated list of tweets sorted by ingestion time descending.
     *
     * @param page the page number to fetch (default: 0)
     * @param size the maximum number of tweets per page (default: 20)
     * @return a ResponseEntity containing the paginated tweets
     */
    @GetMapping("/tweets")
    public ResponseEntity<Page<Tweet>> tweets(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(tweetRepository.findAllByOrderByIngestedAtDesc(pageable));
    }
}
