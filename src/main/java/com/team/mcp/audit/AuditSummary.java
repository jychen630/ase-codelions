package com.team.mcp.audit;

import java.time.Instant;

/**
 * Lightweight per-tool summary used by the audit HTTP API.
 *
 * @param toolName   MCP tool name (e.g., "search_tweets")
 * @param totalCalls total invocations for this tool
 * @param okCalls    how many finished successfully
 * @param errorCalls how many returned an error
 * @param avgDurationMs average duration in milliseconds
 * @param lastCallAt timestamp of the most recent call, or null
 */
public record AuditSummary(
    String toolName,
    long totalCalls,
    long okCalls,
    long errorCalls,
    long avgDurationMs,
    Instant lastCallAt
) { }
