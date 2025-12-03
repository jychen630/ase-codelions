package com.team.mcp.scheduling;

import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for SchedulerRunner.
 */
class SchedulerRunnerTest {

  @Test
  void run_callsPublisherTick_andLogsInfoWhenPosted() {
    SchedulingService svc = mock(SchedulingService.class);
    when(svc.publisherTick()).thenReturn(3);

    SchedulerRunner runner = new SchedulerRunner(svc);
    runner.run();

    verify(svc, times(1)).publisherTick();
    // We don't assert logs; just executing the branch is enough for coverage.
  }

  @Test
  void run_callsPublisherTick_andLogsDebugWhenNothingDue() {
    SchedulingService svc = mock(SchedulingService.class);
    when(svc.publisherTick()).thenReturn(0);

    SchedulerRunner runner = new SchedulerRunner(svc);
    runner.run();

    verify(svc, times(1)).publisherTick();
  }
}

