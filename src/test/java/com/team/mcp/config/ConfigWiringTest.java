package com.team.mcp.config;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Clock;

import com.team.mcp.scheduling.SchedulingService;
import com.team.mcp.twitter.TwitterClient;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.beans.factory.annotation.Autowired;

@SpringBootTest
class ConfigWiringTest {

  @Autowired private ApplicationContext ctx;

  @Test
  void contextLoads_andCoreBeansPresent() {
    assertNotNull(ctx.getBean(Clock.class));
    assertNotNull(ctx.getBean(TwitterClient.class));
    assertNotNull(ctx.getBean(SchedulingService.class));
  }
}
