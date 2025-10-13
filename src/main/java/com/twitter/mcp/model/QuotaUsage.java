package com.twitter.mcp.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity representing API quota usage tracking.
 * Stores read and write operation counts with reset periods.
 */
@Entity
@Table(name = "quota_usage")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuotaUsage {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "period_start", nullable = false)
    private LocalDateTime periodStart;
    
    @Column(name = "period_end", nullable = false)
    private LocalDateTime periodEnd;
    
    @Column(name = "reads_used", nullable = false)
    private Integer readsUsed;
    
    @Column(name = "writes_used", nullable = false)
    private Integer writesUsed;
    
    @Column(name = "reads_max", nullable = false)
    private Integer readsMax;
    
    @Column(name = "writes_max", nullable = false)
    private Integer writesMax;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    /**
     * Check if this quota period is currently active.
     */
    public boolean isActive() {
        LocalDateTime now = LocalDateTime.now();
        return now.isAfter(periodStart) && now.isBefore(periodEnd);
    }
    
    /**
     * Check if read quota is available.
     */
    public boolean canRead() {
        return readsUsed < readsMax;
    }
    
    /**
     * Check if write quota is available.
     */
    public boolean canWrite() {
        return writesUsed < writesMax;
    }
    
    /**
     * Increment read usage.
     */
    public void incrementReads() {
        this.readsUsed++;
    }
    
    /**
     * Increment write usage.
     */
    public void incrementWrites() {
        this.writesUsed++;
    }
}

