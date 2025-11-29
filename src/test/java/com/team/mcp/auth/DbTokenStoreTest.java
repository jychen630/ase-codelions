package com.team.mcp.auth;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Unit tests for DbTokenStore (no Spring context needed).
 */
class DbTokenStoreTest {

  @Test
  void get_decryptsTokenWhenPresent() {
    TokenCredentialRepository repo = mock(TokenCredentialRepository.class);
    SecretCryptoService crypto = mock(SecretCryptoService.class);
    DbTokenStore store = new DbTokenStore(repo, crypto);

    TokenCredential cred = new TokenCredential("acctA", "ENC");
    when(repo.findByAccountId("acctA")).thenReturn(Optional.of(cred));
    when(crypto.decrypt("ENC")).thenReturn("plain");

    Optional<String> token = store.get("acctA");

    assertTrue(token.isPresent());
    assertEquals("plain", token.get());
    verify(repo).findByAccountId("acctA");
    verify(crypto).decrypt("ENC");
  }

  @Test
  void get_returnsEmptyWhenMissing() {
    TokenCredentialRepository repo = mock(TokenCredentialRepository.class);
    SecretCryptoService crypto = mock(SecretCryptoService.class);
    DbTokenStore store = new DbTokenStore(repo, crypto);

    when(repo.findByAccountId("acctB")).thenReturn(Optional.empty());

    Optional<String> token = store.get("acctB");

    assertTrue(token.isEmpty());
    verify(repo).findByAccountId("acctB");
    verifyNoInteractions(crypto);
  }

  @Test
  void put_insertsNewRowWhenAbsent() {
    TokenCredentialRepository repo = mock(TokenCredentialRepository.class);
    SecretCryptoService crypto = mock(SecretCryptoService.class);
    DbTokenStore store = new DbTokenStore(repo, crypto);

    when(crypto.encrypt("plain")).thenReturn("ENC");
    when(repo.findByAccountId("acctA")).thenReturn(Optional.empty());

    store.put("acctA", "plain");

    verify(crypto).encrypt("plain");
    verify(repo).findByAccountId("acctA");

    ArgumentCaptor<TokenCredential> captor =
        ArgumentCaptor.forClass(TokenCredential.class);
    verify(repo).save(captor.capture());
    TokenCredential saved = captor.getValue();
    assertEquals("acctA", saved.getAccountId());
    assertEquals("ENC", saved.getToken());
  }

  @Test
  void put_updatesExistingRow() {
    TokenCredentialRepository repo = mock(TokenCredentialRepository.class);
    SecretCryptoService crypto = mock(SecretCryptoService.class);
    DbTokenStore store = new DbTokenStore(repo, crypto);

    TokenCredential existing = new TokenCredential("acctA", "OLD");
    when(crypto.encrypt("newPlain")).thenReturn("NEWENC");
    when(repo.findByAccountId("acctA")).thenReturn(Optional.of(existing));

    store.put("acctA", "newPlain");

    assertEquals("NEWENC", existing.getToken());
    verify(repo).save(existing);
  }

  @Test
  void listAccounts_delegatesToRepository() {
    TokenCredentialRepository repo = mock(TokenCredentialRepository.class);
    SecretCryptoService crypto = mock(SecretCryptoService.class);
    DbTokenStore store = new DbTokenStore(repo, crypto);

    when(repo.listAccountIds()).thenReturn(List.of("a", "b"));

    assertEquals(List.of("a", "b"), store.listAccounts());
    verify(repo).listAccountIds();
  }
}

