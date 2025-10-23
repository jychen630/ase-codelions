package com.team.mcp.search;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** JPA repo for full-text-lite search needs. */
@Repository
public interface TweetRepository extends JpaRepository<TweetEntity, String> {

  /**
   * Find up to 100 most-recent tweets whose {@code text} contains the given
   * query case-insensitively, ordered by {@code createdAt} descending.
   *
   * @param q substring to search for (case-insensitive)
   * @return newest-first list of matching tweets, limited to 100
   */
  List<TweetEntity> findTop100ByTextContainingIgnoreCaseOrderByCreatedAtDesc(
      String q);

  /**
   * Find up to 100 most-recent tweets by exact {@code userHandle} ,
   * ordered by {@code createdAt} descending.
   *
   * @param handle user handle to match (case-insensitive)
   * @return newest-first list of tweets for the handle, limited to 100
   */
  List<TweetEntity> findTop100ByUserHandleIgnoreCaseOrderByCreatedAtDesc(
      String handle);
}

