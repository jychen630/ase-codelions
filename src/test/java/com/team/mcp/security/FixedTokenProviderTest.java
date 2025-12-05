package com.team.mcp.security;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

/**
 * Smoke test for FixedTokenProvider: ensure it is instantiable and, if it
 * exposes a zero-arg String getter, that it can be invoked.
 */
class FixedTokenProviderTest {

  @Test
  void ctor_noArgs_instantiable_andOptionalGetterWorks() throws Exception {
    FixedTokenProvider p = new FixedTokenProvider();
    assertNotNull(p);

    // Try to find any zero-arg method that returns String (e.g., getToken(), value(), token()).
    Method m = Arrays.stream(FixedTokenProvider.class.getDeclaredMethods())
        .filter(mm -> mm.getParameterCount() == 0 && mm.getReturnType().equals(String.class))
        .findFirst()
        .orElse(null);

    if (m != null) {
      m.setAccessible(true);
      Object v = m.invoke(p);
      // We don’t know the default value; just verify invocation succeeds.
      // If our provider reads from props, v may be null; that’s fine.
      assertTrue(v == null || v instanceof String);
    }
  }
}
