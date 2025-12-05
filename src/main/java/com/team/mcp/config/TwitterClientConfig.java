package com.team.mcp.config;

import com.team.mcp.auth.TokenStore;
import com.team.mcp.twitter.FakeTwitterClient;
import com.team.mcp.twitter.MastodonClient;
import com.team.mcp.twitter.TwitterClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration for wiring the {@link TwitterClient} implementation.
 *
 * <p>Depending on the {@code app.fakeTwitter} property, this either exposes a
 * {@link FakeTwitterClient} (for local/test runs) or a Mastodon-backed
 * {@link MastodonClient} for real HTTP calls.</p>
 *
 * <p>This configuration class is not intended to be extended. If you need
 * different wiring, define a separate {@code @Configuration} class rather
 * than subclassing this one.</p>
 */
@Configuration
public class TwitterClientConfig {

  /**
   * Create a {@link FakeTwitterClient} when {@code app.fakeTwitter=true}
   * or the property is missing.
   *
   * <p>This is the default for local development and tests.</p>
   *
   * <p><b>Subclassing note:</b> this method is not intended to be overridden.
   * If a subclass of {@code TwitterClientConfig} is created, it should avoid
   * overriding this method; instead, define a separate {@code @Bean} with a
   * different name if custom behavior is required.</p>
   *
   * @return fake twitter client implementation
   */
  @Bean
  @ConditionalOnProperty(
      name = "app.fakeTwitter",
      havingValue = "true",
      matchIfMissing = true)
  public TwitterClient fakeTwitterClient() {
    return new FakeTwitterClient();
  }

  /**
   * Create a {@link MastodonClient} when {@code app.fakeTwitter=false}.
   *
   * <p>This bean is used for real HTTP calls against a Mastodon instance.</p>
   *
   * <p><b>Subclassing note:</b> subclasses should not override this method.
   * To customize behavior, prefer defining additional beans or a separate
   * configuration class instead of changing this one.</p>
   *
   * @param baseUrl Mastodon instance base URL
   * @param tokenStore token store used to look up access tokens
   * @return mastodon-backed twitter client implementation
   */
  @Bean
  @ConditionalOnProperty(
      name = "app.fakeTwitter",
      havingValue = "false")
  public TwitterClient mastodonTwitterClient(
      @Value("${mastodon.instance-base-url}") final String baseUrl,
      final TokenStore tokenStore) {
    return new MastodonClient(baseUrl, tokenStore);
  }
}

