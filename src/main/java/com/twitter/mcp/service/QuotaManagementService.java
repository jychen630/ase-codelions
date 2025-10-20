package com.twitter.mcp.service;

import com.twitter.mcp.exception.QuotaExceededException;
import com.twitter.mcp.model.QuotaUsage;
import com.twitter.mcp.repository.QuotaUsageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for managing API quota limits.
 * Tracks read and write operations and enforces limits.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuotaManagementService {
    
    private final QuotaUsageRepository quotaRepository;
    
    @Value("${quota.read.max:100}")
    private Integer maxReads;
    
    @Value("${quota.write.max:500}")
    private Integer maxWrites;
    
    @Value("${quota.reset.period:monthly}")
    private String resetPeriod;
    
    /**
     * Initialize quota tracking on startup.
     */
    @PostConstruct
    public void init() {
        log.info("Initializing quota management: maxReads={}, maxWrites={}, resetPeriod={}",
            maxReads, maxWrites, resetPeriod);
        ensureCurrentQuotaPeriod();
    }
    
    /**
     * Get current quota status.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getQuotaStatus() {
        QuotaUsage quota = getCurrentQuota();
        
        Map<String, Object> status = new HashMap<>();
        status.put("readsUsed", quota.getReadsUsed());
        status.put("readsRemaining", quota.getReadsMax() - quota.getReadsUsed());
        status.put("readsMax", quota.getReadsMax());
        status.put("writesUsed", quota.getWritesUsed());
        status.put("writesRemaining", quota.getWritesMax() - quota.getWritesUsed());
        status.put("writesMax", quota.getWritesMax());
        status.put("periodStart", quota.getPeriodStart().toString());
        status.put("periodEnd", quota.getPeriodEnd().toString());
        status.put("resetPeriod", resetPeriod);
        
        return status;
    }
    
    /**
     * Check if a read operation is allowed.
     */
    @Transactional
    public void checkAndIncrementRead() throws QuotaExceededException {
        QuotaUsage quota = getCurrentQuota();
        
        if (!quota.canRead()) {
            throw new QuotaExceededException(
                String.format("Read quota exceeded: %d/%d used. Resets at %s",
                    quota.getReadsUsed(), quota.getReadsMax(), quota.getPeriodEnd())
            );
        }
        
        quota.incrementReads();
        quotaRepository.save(quota);
        
        log.debug("Read quota used: {}/{}", quota.getReadsUsed(), quota.getReadsMax());
    }
    
    /**
     * Check if a write operation is allowed.
     */
    @Transactional
    public void checkAndIncrementWrite() throws QuotaExceededException {
        QuotaUsage quota = getCurrentQuota();
        
        if (!quota.canWrite()) {
            throw new QuotaExceededException(
                String.format("Write quota exceeded: %d/%d used. Resets at %s",
                    quota.getWritesUsed(), quota.getWritesMax(), quota.getPeriodEnd())
            );
        }
        
        quota.incrementWrites();
        quotaRepository.save(quota);
        
        log.debug("Write quota used: {}/{}", quota.getWritesUsed(), quota.getWritesMax());
    }
    
    /**
     * Get the current active quota period, creating if necessary.
     */
    private QuotaUsage getCurrentQuota() {
        return quotaRepository.findActiveQuota(LocalDateTime.now())
            .orElseGet(this::createNewQuotaPeriod);
    }
    
    /**
     * Ensure a current quota period exists.
     */
    private void ensureCurrentQuotaPeriod() {
        if (quotaRepository.findActiveQuota(LocalDateTime.now()).isEmpty()) {
            createNewQuotaPeriod();
        }
    }
    
    /**
     * Create a new quota period.
     */
    private QuotaUsage createNewQuotaPeriod() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime periodStart;
        LocalDateTime periodEnd;
        
        if ("monthly".equalsIgnoreCase(resetPeriod)) {
            // Start of current month
            periodStart = YearMonth.from(now).atDay(1).atStartOfDay();
            // Start of next month
            periodEnd = YearMonth.from(now).plusMonths(1).atDay(1).atStartOfDay();
        } else {
            // Default to monthly if unknown
            periodStart = YearMonth.from(now).atDay(1).atStartOfDay();
            periodEnd = YearMonth.from(now).plusMonths(1).atDay(1).atStartOfDay();
        }
        
        QuotaUsage quota = QuotaUsage.builder()
            .periodStart(periodStart)
            .periodEnd(periodEnd)
            .readsUsed(0)
            .writesUsed(0)
            .readsMax(maxReads)
            .writesMax(maxWrites)
            .build();
        
        quota = quotaRepository.save(quota);
        
        log.info("Created new quota period: {} to {}", periodStart, periodEnd);
        
        return quota;
    }
    
    /**
     * Reset quota (for testing purposes).
     */
    @Transactional
    public void resetQuota() {
        quotaRepository.deleteAll();
        createNewQuotaPeriod();
        log.info("Quota reset completed");
    }
}

