package com.regisx001.core.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.regisx001.core.domain.entities.Tweet;

@Repository
public interface TweetRepository extends JpaRepository<Tweet, Long> {

    // Count tweets processed in the last N seconds → throughput
    @Query("SELECT COUNT(t) FROM Tweet t WHERE t.processedAt IS NOT NULL AND t.processedAt >= :since")
    long countProcessedSince(@Param("since") LocalDateTime since);

    // Sentiment aggregates: returns rows of [sentiment (String), count (Long)]
    @Query(value = """
            SELECT processed_data->>'sentiment' AS sentiment, COUNT(*) AS cnt
            FROM raw_tweets
            WHERE processed_data IS NOT NULL
              AND processed_data->>'sentiment' IS NOT NULL
            GROUP BY processed_data->>'sentiment'
            """, nativeQuery = true)
    List<Object[]> sentimentCounts();

    // Time-series: rows of [bucket_time (Timestamp), sentiment (String), count
    // (Long)]
    @Query(value = """
            SELECT date_trunc(:bucket, processed_at) AS bucket_time,
                   processed_data->>'sentiment'       AS sentiment,
                   COUNT(*)                           AS cnt
            FROM raw_tweets
            WHERE processed_at >= :since
              AND processed_data IS NOT NULL
              AND processed_data->>'sentiment' IS NOT NULL
            GROUP BY bucket_time, processed_data->>'sentiment'
            ORDER BY bucket_time
            """, nativeQuery = true)
    List<Object[]> sentimentTimeSeries(@Param("bucket") String bucket, @Param("since") LocalDateTime since);

    // Last N processed tweets for live feed
    @Query(value = """
            SELECT * FROM raw_tweets
            WHERE processed_at IS NOT NULL
              AND processed_data IS NOT NULL
            ORDER BY processed_at DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Tweet> findLatestProcessed(@Param("limit") int limit);

    // Paginated list of all tweets, newest first
    Page<Tweet> findAllByOrderByIngestedAtDesc(Pageable pageable);
}
