package com.team.mcp.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables scheduled tasks for the app (e.g., SchedulerRunner).
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
