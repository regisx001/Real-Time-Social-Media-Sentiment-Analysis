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

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final TweetRepository tweetRepository;

    public AnalyticsController(AnalyticsService analyticsService, TweetRepository tweetRepository) {
        this.analyticsService = analyticsService;
        this.tweetRepository = tweetRepository;
    }

    // ---------------------------------------------------------------
    // SSE — full analytics report every 5 s
    // GET /api/analytics/stream
    // ---------------------------------------------------------------
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
    @GetMapping("/tweets")
    public ResponseEntity<Page<Tweet>> tweets(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(tweetRepository.findAllByOrderByIngestedAtDesc(pageable));
    }
}
