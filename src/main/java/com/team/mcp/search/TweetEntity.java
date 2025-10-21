package com.team.mcp.search;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Persisted tweet row (agnostic to Fake/Real source).
 *
 * <p>Simple JPA entity mapped to the {@code TWEETS} table.
 * This class is {@code final} (not intended for extension).
 */
@Entity
@Table(
    name = "tweets",
    indexes = {
        @Index(name = "ix_tweets_created_at", columnList = "created_at"),
        @Index(name = "ix_tweets_user_handle", columnList = "user_handle")
    }
)
public final class TweetEntity {

  /** Maximum ID length (column length). */
  private static final int MAX_ID_LEN = 64;

  /** Maximum user handle length (column length). */
  private static final int MAX_USER_LEN = 64;

  /** Maximum text length (column length). */
  private static final int MAX_TEXT_LEN = 1024;

  /** Platform (or fake) tweet id. */
  @Id
  @Column(length = MAX_ID_LEN, nullable = false)
  private String id;

  /** Logical user/handle. */
  @Column(name = "user_handle", length = MAX_USER_LEN, nullable = false)
  private String userHandle;

  /** Tweet content text. */
  @Column(length = MAX_TEXT_LEN, nullable = false)
  private String text;

  /** Creation time in UTC. */
  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  /**
   * JPA only.
   * Required by the provider to instantiate the entity via reflection.
   */
  protected TweetEntity() {
    // no-op
  }

  /**
   * Create a new immutable tweet row.
   *
   * @param idParam tweet id
   * @param userHandleParam user handle
   * @param textParam tweet text
   * @param createdAtParam creation instant (UTC)
   */
  public TweetEntity(
      final String idParam,
      final String userHandleParam,
      final String textParam,
      final Instant createdAtParam) {
    this.id = idParam;
    this.userHandle = userHandleParam;
    this.text = textParam;
    this.createdAt = createdAtParam;
  }

  /**
   * @return tweet id
   */
  public String getId() {
    return id;
  }

  /**
   * @return user handle
   */
  public String getUserHandle() {
    return userHandle;
  }

  /**
   * @return tweet text
   */
  public String getText() {
    return text;
  }

  /**
   * @return creation time (UTC)
   */
  public Instant getCreatedAt() {
    return createdAt;
  }
}
