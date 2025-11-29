package com.team.mcp.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Application-wide {@link Clock} bean.
 *
 * <p>In tests, override with a fixed clock for deterministic time.
 */
@Configuration
public class ClockConfig {

  /**
   * Provides the system UTC clock.
   *
   * @return a UTC {@link Clock} for time operations
   */
  @Bean
  public Clock clock() {
    return Clock.systemUTC();
  }
}
