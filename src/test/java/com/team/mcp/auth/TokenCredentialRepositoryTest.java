package com.team.mcp.auth;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Verifies the default listAccountIds() method.
 */
class TokenCredentialRepositoryTest {

  @Test
  void listAccountIds_usesFindAllAndMapsIds() {
    // CALLS_REAL_METHODS -> default methods run, abstract ones can be stubbed
    TokenCredentialRepository repo =
        mock(TokenCredentialRepository.class, Mockito.CALLS_REAL_METHODS);

    List<TokenCredential> entities = List.of(
        new TokenCredential("acct1", "t1"),
        new TokenCredential("acct2", "t2")
    );
    when(repo.findAll()).thenReturn(entities);

    List<String> ids = repo.listAccountIds();

    assertEquals(List.of("acct1", "acct2"), ids);
    verify(repo).findAll();
  }
}

