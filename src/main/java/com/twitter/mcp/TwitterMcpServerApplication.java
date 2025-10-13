package com.twitter.mcp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main application class for Twitter MCP Server.
 * This server implements the Model Context Protocol (MCP) to expose Twitter/X
 * functionality to AI agents and clients.
 */
@SpringBootApplication
public class TwitterMcpServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(TwitterMcpServerApplication.class, args);
    }
}

