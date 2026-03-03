package com.regisx001.core.domain.entities;

import java.time.LocalDateTime;
import java.util.Map;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity representing a raw and processed tweet within the system, mapped to
 * the database.
 */
@Entity
@Table(name = "raw_tweets")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Tweet {

    /**
     * Unique identifier for the tweet entry in the database.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The original, unconverted JSON data of the incoming tweet.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> rawData;

    /**
     * The processed JSON data representing analytical outputs or derived metrics.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> processedData;

    /**
     * The timestamp denoting exactly when the tweet was ingested into the system.
     */
    @Column(name = "ingested_at", nullable = false)
    private LocalDateTime ingestedAt;

    /**
     * The timestamp denoting when the tweet was thoroughly processed, if
     * applicable.
     */
    @Column(name = "processed_at")
    private LocalDateTime processedAt;

}
