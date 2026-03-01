package com.regisx001.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.regisx001.core.domain.entities.Tweet;

@Repository
public interface TweetRepository extends JpaRepository<Tweet, Long> {
}
