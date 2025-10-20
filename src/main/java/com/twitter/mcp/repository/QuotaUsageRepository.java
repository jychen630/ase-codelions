package com.twitter.mcp.repository;

import com.twitter.mcp.model.QuotaUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Repository for QuotaUsage entity.
 */
@Repository
public interface QuotaUsageRepository extends JpaRepository<QuotaUsage, Long> {
    
    /**
     * Find the active quota period.
     */
    @Query("SELECT q FROM QuotaUsage q WHERE q.periodStart <= :now AND q.periodEnd > :now")
    Optional<QuotaUsage> findActiveQuota(LocalDateTime now);
    
    /**
     * Find the most recent quota period.
     */
    Optional<QuotaUsage> findTopByOrderByPeriodStartDesc();
}

