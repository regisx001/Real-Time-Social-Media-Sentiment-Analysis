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

/**
 * Repository interface for managing Tweet entities.
 */
@Repository
public interface TweetRepository extends JpaRepository<Tweet, Long> {

    /**
     * Counts the number of tweets processed after a given time.
     * 
     * @param since the time to count from
     * @return the number of tweets processed
     */
    @Query("SELECT COUNT(t) FROM Tweet t WHERE t.processedAt IS NOT NULL AND t.processedAt >= :since")
    long countProcessedSince(@Param("since") LocalDateTime since);

    /**
     * Retrieves aggregated sentiment counts for processed tweets.
     * 
     * @return a list of rows with sentiment and count
     */
    @Query(value = """
            SELECT processed_data->>'sentiment' AS sentiment, COUNT(*) AS cnt
            FROM raw_tweets
            WHERE processed_data IS NOT NULL
              AND processed_data->>'sentiment' IS NOT NULL
            GROUP BY processed_data->>'sentiment'
            """, nativeQuery = true)
    List<Object[]> sentimentCounts();

    /**
     * Retrieves time-series sentiment data based on a specific time bucket.
     * 
     * @param bucket the time truncation bucket
     * @param since  the start time
     * @return a list of rows with bucket time, sentiment, and count
     */
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

    /**
     * Retrieves the latest N processed tweets.
     * 
     * @param limit the number of tweets to retrieve
     * @return a list of latest processed tweets
     */
    @Query(value = """
            SELECT * FROM raw_tweets
            WHERE processed_at IS NOT NULL
              AND processed_data IS NOT NULL
            ORDER BY processed_at DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Tweet> findLatestProcessed(@Param("limit") int limit);

    /**
     * Retrieves all tweets ordered by their ingestion time descending.
     * 
     * @param pageable pagination details
     * @return a page of tweets
     */
    Page<Tweet> findAllByOrderByIngestedAtDesc(Pageable pageable);
}
