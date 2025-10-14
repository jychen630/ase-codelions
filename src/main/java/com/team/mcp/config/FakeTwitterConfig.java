package com.team.mcp.config;

import com.team.mcp.twitter.FakeTwitterClient;
import com.team.mcp.twitter.TwitterClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Provides a fake Twitter client when {@code app.fakeTwitter=true}.
 * Default is enabled for Iteration-1.
 */
@Configuration
@ConditionalOnProperty(
    name = "app.fakeTwitter",
    havingValue = "true",
    matchIfMissing = true
)
public class FakeTwitterConfig {

  /**
   * Twitter client bean used by services.
   *
   * @return a deterministic, in-memory {@link TwitterClient}
   */
  @Bean
  public TwitterClient twitterClient() {
    return new FakeTwitterClient();
  }
}
