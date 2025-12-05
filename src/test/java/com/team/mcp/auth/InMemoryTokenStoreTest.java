package com.team.mcp.auth;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for InMemoryTokenStore.
 */
class InMemoryTokenStoreTest {

  private InMemoryTokenStore store;

  @BeforeEach
  void setUp() {
    store = new InMemoryTokenStore();
  }

  @Test
  void put_andGet_works() {
    store.put("acct1", "token123");
    Optional<String> token = store.get("acct1");
    assertTrue(token.isPresent());
    assertEquals("token123", token.get());
  }

  @Test
  void get_nonExistentAccount_returnsEmpty() {
    Optional<String> token = store.get("nonexistent");
    assertFalse(token.isPresent());
  }

  @Test
  void get_nullAccountId_returnsEmpty() {
    Optional<String> token = store.get(null);
    assertFalse(token.isPresent());
  }

  @Test
  void get_blankAccountId_returnsEmpty() {
    Optional<String> token = store.get("   ");
    assertFalse(token.isPresent());
  }

  @Test
  void put_nullAccountId_throwsException() {
    assertThrows(IllegalArgumentException.class, () -> {
      store.put(null, "token");
    });
  }

  @Test
  void put_blankAccountId_throwsException() {
    assertThrows(IllegalArgumentException.class, () -> {
      store.put("   ", "token");
    });
  }

  @Test
  void put_nullToken_throwsException() {
    assertThrows(NullPointerException.class, () -> {
      store.put("acct", null);
    });
  }

  @Test
  void put_updatesExistingToken() {
    store.put("acct1", "token1");
    store.put("acct1", "token2");
    Optional<String> token = store.get("acct1");
    assertEquals("token2", token.get());
  }

  @Test
  void listAccounts_returnsAllAccounts() {
    store.put("acct1", "token1");
    store.put("acct2", "token2");
    store.put("acct3", "token3");

    List<String> accounts = store.listAccounts();
    assertEquals(3, accounts.size());
    assertTrue(accounts.contains("acct1"));
    assertTrue(accounts.contains("acct2"));
    assertTrue(accounts.contains("acct3"));
  }

  @Test
  void listAccounts_emptyStore_returnsEmptyList() {
    List<String> accounts = store.listAccounts();
    assertTrue(accounts.isEmpty());
  }
}

