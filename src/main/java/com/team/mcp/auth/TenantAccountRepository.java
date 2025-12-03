package com.team.mcp.auth;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for {@link TenantAccount}.
 */
public interface TenantAccountRepository
    extends JpaRepository<TenantAccount, Long> {

  /**
   * Finds by external uid.
   *
   * @param uid tenant uid
   * @return row if found
   */
  Optional<TenantAccount> findByUid(String uid);
}
