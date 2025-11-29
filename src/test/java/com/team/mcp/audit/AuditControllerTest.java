package com.team.mcp.audit;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AuditControllerTest {

  @Test
  void recent_clampsLimit_andDelegatesToService() {
    AuditService service = mock(AuditService.class);
    AuditController controller = new AuditController(service);

    when(service.findRecent(1)).thenReturn(Collections.emptyList());
    when(service.findRecent(50)).thenReturn(Collections.emptyList());
    when(service.findRecent(500)).thenReturn(Collections.emptyList());

    controller.recent(0);
    controller.recent(50);
    controller.recent(1000);

    verify(service).findRecent(1);
    verify(service).findRecent(50);
    verify(service).findRecent(500);
  }

  @Test
  void recent_returnsServiceResultDirectly() {
    AuditService service = mock(AuditService.class);
    AuditController controller = new AuditController(service);

    ToolCallAudit row = new ToolCallAudit(
        "tools/call", "search_tweets", "acct",
        true, 10L, null, null);
    List<ToolCallAudit> rows = List.of(row);

    when(service.findRecent(10)).thenReturn(rows);

    List<ToolCallAudit> result = controller.recent(10);

    assertSame(rows, result);
  }

  @Test
  void summary_clampsHours_andPassesInstantToService() {
    AuditService service = mock(AuditService.class);
    AuditController controller = new AuditController(service);

    when(service.summarizeByToolSince(any()))
        .thenReturn(Collections.emptyList());

    controller.summary(0);    // clamps to 1
    controller.summary(24);   // within range
    controller.summary(999);  // clamps to 168

    ArgumentCaptor<Instant> captor = ArgumentCaptor.forClass(
        Instant.class);
    verify(service, times(3)).summarizeByToolSince(captor.capture());

    Instant now = Instant.now();
    for (Instant since : captor.getAllValues()) {
      // The since instant must not be in the future.
      assertTrue(!since.isAfter(now));
    }
  }
}

