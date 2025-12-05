package com.team.mcp.scheduling;

import com.team.mcp.twitter.TwitterClient;
import com.team.mcp.twitter.TwitterClient.TwitterException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.stubbing.Answer;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static com.team.mcp.scheduling.ScheduledPost.Status.PENDING;
import static com.team.mcp.scheduling.ScheduledPost.Status.POSTED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class SchedulingServiceTest {

  private ScheduledPostRepository repo;
  private TwitterClient twitterClient;
  private Clock fixedClock;
  private SchedulingService service;

  @BeforeEach
  void setUp() {
    repo = mock(ScheduledPostRepository.class);
    twitterClient = mock(TwitterClient.class);
    fixedClock =
        Clock.fixed(Instant.parse("2025-10-01T12:00:00Z"), ZoneOffset.UTC);
    service = new SchedulingService(repo, twitterClient, fixedClock, 50);
  }

  @Test
  void schedule_rejectsPastTime() {
    Instant past = Instant.parse("2025-09-30T12:00:00Z");
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class,
            () -> service.schedule("Hi", past, "acct"));
    assertTrue(ex.getMessage().toLowerCase().contains("future"));
  }

  @Test
  void schedule_persistsPending_andReturnsId() {
    Instant future = Instant.parse("2025-10-01T12:05:00Z");

    when(repo.save(any(ScheduledPost.class)))
        .thenAnswer((Answer<ScheduledPost>) inv -> {
          ScheduledPost s = inv.getArgument(0);
          ScheduledPost spy = spy(s);
          doReturn(42L).when(spy).getId();
          return spy;
        });

    String id = service.schedule("Hello world", future, "acct-1");
    assertEquals("42", id);

    ArgumentCaptor<ScheduledPost> captor =
        ArgumentCaptor.forClass(ScheduledPost.class);
    verify(repo).save(captor.capture());
    ScheduledPost saved = captor.getValue();
    assertEquals("acct-1", saved.getAccountId());
    assertEquals(PENDING, saved.getStatus());
    assertEquals("Hello world", saved.getText());
    assertEquals(future, saved.getRunAt());
  }

  @Test
  void publisherTick_postsDueItems_marksPosted_incrementsCount()
      throws TwitterException {
    List<ScheduledPost> store = new ArrayList<>();
    ScheduledPost due1 = new ScheduledPost(
        "acct", "T1", Instant.parse("2025-10-01T11:59:00Z"));
    ScheduledPost due2 = new ScheduledPost(
        "acct", "T2", Instant.parse("2025-10-01T12:00:00Z"));
    ScheduledPost future = new ScheduledPost(
        "acct", "T3", Instant.parse("2025-10-01T12:01:00Z"));
    store.add(due1);
    store.add(due2);
    store.add(future);

    when(twitterClient.postTweet(anyString(), anyString()))
        .thenReturn("tw-100")
        .thenReturn("tw-101");

    when(repo.findDue(any(Instant.class), eq(PENDING), any(Pageable.class)))
        .thenAnswer((Answer<List<ScheduledPost>>) inv -> {
          Instant now = inv.getArgument(0);
          return store.stream()
              .filter(s -> s.getStatus() == PENDING
                  && !s.getRunAt().isAfter(now))
              .toList();
        });

    int processed = service.publisherTick();
    assertEquals(2, processed);
    assertEquals(POSTED, due1.getStatus());
    assertEquals(POSTED, due2.getStatus());
    assertEquals("tw-100", due1.getPostedTweetId());
    assertEquals("tw-101", due2.getPostedTweetId());

    assertEquals(PENDING, future.getStatus());

    int processedAgain = service.publisherTick();
    assertEquals(0, processedAgain);
  }

  @Test
  void publisherTick_handlesClientFailure_marksFailed()
      throws TwitterException {
    List<ScheduledPost> store = new ArrayList<>();
    ScheduledPost due = new ScheduledPost(
        "acct", "Boom", Instant.parse("2025-10-01T11:59:00Z"));
    store.add(due);

    when(repo.findDue(any(Instant.class), eq(PENDING), any(Pageable.class)))
        .thenReturn(store);

    when(twitterClient.postTweet(anyString(), anyString()))
        .thenThrow(new TwitterException("rate limited"));

    int processed = service.publisherTick();
    assertEquals(0, processed);
    assertEquals(ScheduledPost.Status.FAILED, due.getStatus());
  }

  @Test
  void publisherTick_noDueItems_returnsZero() throws TwitterException {
    when(repo.findDue(any(Instant.class), eq(PENDING), any(Pageable.class)))
        .thenReturn(List.of());

    int processed = service.publisherTick();
    assertEquals(0, processed);
    verifyNoInteractions(twitterClient);
  }

  @Test
  void publisherTick_partialFailure_continuesProcessing()
      throws TwitterException {
    var due1 = new ScheduledPost(
        "acct", "T1", Instant.parse("2025-10-01T11:59:00Z"));
    var due2 = new ScheduledPost(
        "acct", "T2", Instant.parse("2025-10-01T11:59:30Z"));

    when(repo.findDue(any(Instant.class), eq(PENDING), any(Pageable.class)))
        .thenReturn(List.of(due1, due2));

    when(twitterClient.postTweet(anyString(), anyString()))
        .thenThrow(new TwitterException("boom"))
        .thenReturn("tw-200");

    int processed = service.publisherTick();
    assertEquals(1, processed);
    assertEquals(ScheduledPost.Status.FAILED, due1.getStatus());
    assertEquals(ScheduledPost.Status.POSTED, due2.getStatus());
    assertEquals("tw-200", due2.getPostedTweetId());
  }

  @Test
  void schedule_nullAccountId_throwsException() {
    Instant future = Instant.parse("2025-10-01T12:05:00Z");
    IllegalArgumentException ex = assertThrows(
        IllegalArgumentException.class,
        () -> service.schedule("text", future, null)
    );
    assertTrue(ex.getMessage().contains("accountId"));
  }

  @Test
  void schedule_blankAccountId_throwsException() {
    Instant future = Instant.parse("2025-10-01T12:05:00Z");
    IllegalArgumentException ex = assertThrows(
        IllegalArgumentException.class,
        () -> service.schedule("text", future, "   ")
    );
    assertTrue(ex.getMessage().contains("accountId"));
  }

  @Test
  void schedule_nullText_throwsException() {
    Instant future = Instant.parse("2025-10-01T12:05:00Z");
    IllegalArgumentException ex = assertThrows(
        IllegalArgumentException.class,
        () -> service.schedule(null, future, "acct")
    );
    assertTrue(ex.getMessage().contains("text"));
  }

  @Test
  void schedule_blankText_throwsException() {
    Instant future = Instant.parse("2025-10-01T12:05:00Z");
    IllegalArgumentException ex = assertThrows(
        IllegalArgumentException.class,
        () -> service.schedule("   ", future, "acct")
    );
    assertTrue(ex.getMessage().contains("text"));
  }

  @Test
  void schedule_nullTime_throwsException() {
    IllegalArgumentException ex = assertThrows(
        IllegalArgumentException.class,
        () -> service.schedule("text", null, "acct")
    );
    assertTrue(ex.getMessage().contains("time"));
  }

  @Test
  void schedule_currentTime_throwsException() {
    Instant now = Instant.parse("2025-10-01T12:00:00Z");
    IllegalArgumentException ex = assertThrows(
        IllegalArgumentException.class,
        () -> service.schedule("text", now, "acct")
    );
    assertTrue(ex.getMessage().toLowerCase().contains("future"));
  }

  @Test
  void constructor_nullClock_usesSystemClock() {
    SchedulingService svc = new SchedulingService(
        repo, twitterClient, null, 50
    );
    // Use a time far in the future to ensure it's after system clock
    Instant future = Instant.now().plusSeconds(3600);
    when(repo.save(any(ScheduledPost.class)))
        .thenAnswer((Answer<ScheduledPost>) inv -> {
          ScheduledPost s = inv.getArgument(0);
          ScheduledPost spy = spy(s);
          doReturn(1L).when(spy).getId();
          return spy;
        });
    // Should work with system clock
    String id = svc.schedule("test", future, "acct");
    assertEquals("1", id);
  }

  @Test
  void constructor_zeroBatchSize_usesDefault() {
    SchedulingService svc = new SchedulingService(
        repo, twitterClient, fixedClock, 0
    );
    when(repo.findDue(any(Instant.class), eq(PENDING), any(Pageable.class)))
        .thenReturn(List.of());
    // Should use default batch size (50)
    int processed = svc.publisherTick();
    assertEquals(0, processed);
    verify(repo).findDue(any(Instant.class), eq(PENDING), 
        eq(PageRequest.of(0, 50)));
  }

  @Test
  void constructor_negativeBatchSize_usesDefault() {
    SchedulingService svc = new SchedulingService(
        repo, twitterClient, fixedClock, -10
    );
    when(repo.findDue(any(Instant.class), eq(PENDING), any(Pageable.class)))
        .thenReturn(List.of());
    // Should use default batch size (50)
    int processed = svc.publisherTick();
    assertEquals(0, processed);
    verify(repo).findDue(any(Instant.class), eq(PENDING), 
        eq(PageRequest.of(0, 50)));
  }
}
