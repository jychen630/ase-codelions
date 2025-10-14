package com.team.mcp.scheduling;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;

/**
 * JPA entity representing a tweet scheduled for future publication.
 */
@Entity
@Table(
    name = "scheduled_posts",
    indexes = {
        @Index(
            name = "idx_scheduled_status_runat",
            columnList = "status, run_at"
        ),
        @Index(
            name = "idx_scheduled_account",
            columnList = "account_id"
        )
    }
)
public final class ScheduledPost {

  /** Maximum length for account id/handle column. */
  private static final int LEN_ACCOUNT_ID = 128;

  /** Maximum length for tweet text column. */
  private static final int LEN_TEXT = 1000;

  /** Maximum length for persisted {@link Status} string. */
  private static final int LEN_STATUS = 16;

  /** Maximum length for the posted tweet id column. */
  private static final int LEN_TWEET_ID = 128;

  /** Lifecycle status for a scheduled post. */
  public enum Status {
    /** Created and awaiting publishing time. */
    PENDING,
    /** Successfully posted. */
    POSTED,
    /** Publishing attempted and failed (may retry later). */
    FAILED
  }

  /** Surrogate primary key (auto-generated). */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** Logical account id/handle. */
  @Column(name = "account_id", nullable = false, length = LEN_ACCOUNT_ID)
  private String accountId;

  /** Tweet content to publish. */
  @Column(name = "text", nullable = false, length = LEN_TEXT)
  private String text;

  /** Time to publish, in UTC. */
  @Column(name = "run_at", nullable = false)
  private Instant runAt;

  /** Current lifecycle status. */
  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = LEN_STATUS)
  private Status status = Status.PENDING;

  /** Platform tweet id after successful posting (nullable). */
  @Column(name = "posted_tweet_id", length = LEN_TWEET_ID)
  private String postedTweetId;

  /** Creation timestamp (set on insert). */
  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  /** Update timestamp (set on insert/update). */
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  /** For JPA only. */
  protected ScheduledPost() {
    // JPA
  }

  /**
   * Creates a new scheduled post in {@link Status#PENDING}.
   *
   * @param accountIdParam logical account id/handle
   * @param textParam tweet content to publish
   * @param runAtParam time to publish, in UTC
   */
  public ScheduledPost(final String accountIdParam,
                       final String textParam,
                       final Instant runAtParam) {
    this.accountId = Objects.requireNonNull(accountIdParam, "accountId");
    this.text = Objects.requireNonNull(textParam, "text");
    this.runAt = Objects.requireNonNull(runAtParam, "runAt");
    this.status = Status.PENDING;
  }

  /**
   * Set timestamps on insert.
   * Suppressed PMD: called by JPA via reflection.
   */
  @SuppressWarnings("PMD.UnusedPrivateMethod")
  @PrePersist
  private void onCreate() {
    final Instant now = Instant.now();
    this.createdAt = now;
    this.updatedAt = now;
  }

  /**
   * Update timestamp on update.
   * Suppressed PMD: called by JPA via reflection.
   */
  @SuppressWarnings("PMD.UnusedPrivateMethod")
  @PreUpdate
  private void onUpdate() {
    this.updatedAt = Instant.now();
  }

  /**
   * Marks this post as successfully posted with the given platform tweet id.
   *
   * @param tweetId id assigned by the platform for the posted tweet
   */
  public void markPosted(final String tweetId) {
    this.status = Status.POSTED;
    this.postedTweetId = tweetId;
  }

  /** Marks this post as failed to publish. */
  public void markFailed() {
    this.status = Status.FAILED;
  }

  /* ---------- Getters / Setters ---------- */

  /** @return primary key */
  public Long getId() {
    return id;
  }

  /** @return logical account id/handle */
  public String getAccountId() {
    return accountId;
  }

  /** @return tweet content */
  public String getText() {
    return text;
  }

  /** @return scheduled publish time (UTC) */
  public Instant getRunAt() {
    return runAt;
  }

  /** @return lifecycle status */
  public Status getStatus() {
    return status;
  }

  /** @return platform tweet id when posted, otherwise {@code null} */
  public String getPostedTweetId() {
    return postedTweetId;
  }

  /** @return creation timestamp */
  public Instant getCreatedAt() {
    return createdAt;
  }

  /** @return last update timestamp */
  public Instant getUpdatedAt() {
    return updatedAt;
  }

  /**
   * Sets the logical account id/handle.
   *
   * @param accountIdParam account id to set
   */
  public void setAccountId(final String accountIdParam) {
    this.accountId = accountIdParam;
  }

  /**
   * Sets the tweet content.
   *
   * @param textParam text to set
   */
  public void setText(final String textParam) {
    this.text = textParam;
  }

  /**
   * Sets the scheduled publish time (UTC).
   *
   * @param runAtParam time to set
   */
  public void setRunAt(final Instant runAtParam) {
    this.runAt = runAtParam;
  }

  /**
   * Sets the lifecycle status.
   *
   * @param statusParam status to set
   */
  public void setStatus(final Status statusParam) {
    this.status = statusParam;
  }

  /**
   * Sets the platform tweet id (after posting).
   *
   * @param postedTweetIdParam tweet id to set
   */
  public void setPostedTweetId(final String postedTweetIdParam) {
    this.postedTweetId = postedTweetIdParam;
  }
}
