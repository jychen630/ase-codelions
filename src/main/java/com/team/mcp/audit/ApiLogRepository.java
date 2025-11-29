package com.team.mcp.audit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for {@link ApiLog} rows in {@code api_logs}.
 */
@Repository
public interface ApiLogRepository extends JpaRepository<ApiLog, Long> {
  // No custom methods needed for Iteration-2.
}

