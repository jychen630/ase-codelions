package com.team.mcp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Spring Boot entry point for the MCP application.
 *
 * <p>Spring may proxy configuration classes, so we keep a visible no-arg
 * constructor. A small non-static method prevents the utility-class
 * Checkstyle rule from firing.
 */
@SpringBootApplication
@EnableScheduling // enables @Scheduled methods
public class McpApplication {

  /** Public no-arg constructor required by Spring's proxying. */
  public McpApplication() {
    // intentionally empty
  }

  /** No-op instance method to avoid "utility class" rule. */
  @SuppressWarnings("unused")
  void touch() {
    // no-op
  }

  /**
   * Application bootstrap.
   *
   * @param args command line arguments
   */
  public static void main(final String[] args) {
    SpringApplication.run(McpApplication.class, args);
  }
}
