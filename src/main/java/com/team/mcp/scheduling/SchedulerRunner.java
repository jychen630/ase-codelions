package com.team.mcp.scheduling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodic job that attempts to publish any due scheduled posts.
 */
@Component
public final class SchedulerRunner {

  /** Class logger. */
  private static final Logger LOGGER =
      LoggerFactory.getLogger(SchedulerRunner.class);

  /** Coordinates persistence and publication. */
  private final SchedulingService scheduling;

  /**
   * Creates a new runner.
   *
   * @param schedulingService scheduling/publishing service
   */
  public SchedulerRunner(final SchedulingService schedulingService) {
    this.scheduling = schedulingService;
    LOGGER.info("SchedulerRunner initialized.");
  }

  /**
   * Executes on a fixed delay, publishing any due posts.
   */
  @Scheduled(fixedDelayString = "${app.publisher.fixedDelay:15000}")
  public void run() {
    final int posted = scheduling.publisherTick();
    if (posted > 0) {
      LOGGER.info("Scheduler tick: posted {} tweet(s).", posted);
    } else {
      LOGGER.debug("Scheduler tick: nothing due.");
    }
  }
}
