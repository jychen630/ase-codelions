package com.team.mcp.twitter.dto;

import java.time.Instant;

/**
 * Minimal tweet DTO for iteration 1.
 *
 * @param id        platform (or fake) tweet id
 * @param user      logical user/handle
 * @param text      tweet content
 * @param createdAt creation time in UTC
 */
public record Tweet(
    String id,
    String user,
    String text,
    Instant createdAt
) { }
