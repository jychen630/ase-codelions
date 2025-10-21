package com.team.mcp.auth;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * JPA repository for {@link TokenCredential}.
 */
@Repository
public interface TokenCredentialRepository
    extends JpaRepository<TokenCredential, String> {

  /**
   * Find by account id.
   *
   * @param accountId id
   * @return optional credential
   */
  Optional<TokenCredential> findByAccountId(String accountId);

  /**
   * List all account ids that currently have a token.
   *
   * @return list of account ids
   */
  default List<String> listAccountIds() {
    // small table in Iteration-1; stream all ids
    return findAll().stream().map(TokenCredential::getAccountId).toList();
  }
}
