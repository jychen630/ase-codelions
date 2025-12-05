package com.team.mcp.twitter.dto;

import java.time.Instant;

/**
 * Minimal tweet DTO for iteration 1.
 *
 * @param id        platform (or fake) tweet id
 * @param user      logical user/handle
 * @param text      tweet content
 * @param createdAt creation time in UTC
 * @param hasMedia  whether the tweet contains media attachments (optional)
 */
public record Tweet(
    String id,
    String user,
    String text,
    Instant createdAt,
    Boolean hasMedia
) {
  /**
   * Constructor with hasMedia defaulting to false for backward compatibility.
   *
   * @param idVal tweet id
   * @param userVal user handle
   * @param textVal tweet text
   * @param createdAtVal creation timestamp
   */
  public Tweet(
      final String idVal,
      final String userVal,
      final String textVal,
      final Instant createdAtVal) {
    this(idVal, userVal, textVal, createdAtVal, false);
  }
}
