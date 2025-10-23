package com.team.mcp.auth;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Repository for {@link OAuthToken}. */
@Repository
public interface OAuthTokenRepository extends JpaRepository<OAuthToken, Long> {

  /**
   * Find the latest token row for a given logical account id.
   *
   * @param accountId logical account id
   * @return token if present
   */
  Optional<OAuthToken> findTopByAccountIdOrderByUpdatedAtDesc(String accountId);
}
