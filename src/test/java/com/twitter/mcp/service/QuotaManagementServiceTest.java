package com.twitter.mcp.service;

import com.twitter.mcp.exception.QuotaExceededException;
import com.twitter.mcp.model.QuotaUsage;
import com.twitter.mcp.repository.QuotaUsageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for QuotaManagementService.
 */
@ExtendWith(MockitoExtension.class)
class QuotaManagementServiceTest {
    
    @Mock
    private QuotaUsageRepository quotaRepository;
    
    @InjectMocks
    private QuotaManagementService quotaService;
    
    private QuotaUsage testQuota;
    
    @BeforeEach
    void setUp() {
        // Set configuration values
        ReflectionTestUtils.setField(quotaService, "maxReads", 100);
        ReflectionTestUtils.setField(quotaService, "maxWrites", 500);
        ReflectionTestUtils.setField(quotaService, "resetPeriod", "monthly");
        
        // Create test quota
        testQuota = QuotaUsage.builder()
            .id(1L)
            .periodStart(LocalDateTime.now().minusDays(1))
            .periodEnd(LocalDateTime.now().plusDays(29))
            .readsUsed(50)
            .writesUsed(200)
            .readsMax(100)
            .writesMax(500)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
    }
    
    @Test
    void testGetQuotaStatus() {
        // Given
        when(quotaRepository.findActiveQuota(any())).thenReturn(Optional.of(testQuota));
        
        // When
        Map<String, Object> status = quotaService.getQuotaStatus();
        
        // Then
        assertNotNull(status);
        assertEquals(50, status.get("readsUsed"));
        assertEquals(50, status.get("readsRemaining"));
        assertEquals(100, status.get("readsMax"));
        assertEquals(200, status.get("writesUsed"));
        assertEquals(300, status.get("writesRemaining"));
        assertEquals(500, status.get("writesMax"));
        assertEquals("monthly", status.get("resetPeriod"));
        
        verify(quotaRepository, times(1)).findActiveQuota(any());
    }
    
    @Test
    void testCheckAndIncrementRead_Success() throws QuotaExceededException {
        // Given
        when(quotaRepository.findActiveQuota(any())).thenReturn(Optional.of(testQuota));
        when(quotaRepository.save(any())).thenReturn(testQuota);
        
        // When
        quotaService.checkAndIncrementRead();
        
        // Then
        assertEquals(51, testQuota.getReadsUsed());
        verify(quotaRepository, times(1)).save(testQuota);
    }
    
    @Test
    void testCheckAndIncrementRead_QuotaExceeded() {
        // Given
        testQuota.setReadsUsed(100); // At max
        when(quotaRepository.findActiveQuota(any())).thenReturn(Optional.of(testQuota));
        
        // When & Then
        QuotaExceededException exception = assertThrows(
            QuotaExceededException.class,
            () -> quotaService.checkAndIncrementRead()
        );
        
        assertTrue(exception.getMessage().contains("Read quota exceeded"));
        verify(quotaRepository, never()).save(any());
    }
    
    @Test
    void testCheckAndIncrementWrite_Success() throws QuotaExceededException {
        // Given
        when(quotaRepository.findActiveQuota(any())).thenReturn(Optional.of(testQuota));
        when(quotaRepository.save(any())).thenReturn(testQuota);
        
        // When
        quotaService.checkAndIncrementWrite();
        
        // Then
        assertEquals(201, testQuota.getWritesUsed());
        verify(quotaRepository, times(1)).save(testQuota);
    }
    
    @Test
    void testCheckAndIncrementWrite_QuotaExceeded() {
        // Given
        testQuota.setWritesUsed(500); // At max
        when(quotaRepository.findActiveQuota(any())).thenReturn(Optional.of(testQuota));
        
        // When & Then
        QuotaExceededException exception = assertThrows(
            QuotaExceededException.class,
            () -> quotaService.checkAndIncrementWrite()
        );
        
        assertTrue(exception.getMessage().contains("Write quota exceeded"));
        verify(quotaRepository, never()).save(any());
    }
    
    @Test
    void testQuotaUsage_CanRead() {
        // Given
        testQuota.setReadsUsed(50);
        testQuota.setReadsMax(100);
        
        // When & Then
        assertTrue(testQuota.canRead());
    }
    
    @Test
    void testQuotaUsage_CannotRead() {
        // Given
        testQuota.setReadsUsed(100);
        testQuota.setReadsMax(100);
        
        // When & Then
        assertFalse(testQuota.canRead());
    }
    
    @Test
    void testQuotaUsage_CanWrite() {
        // Given
        testQuota.setWritesUsed(200);
        testQuota.setWritesMax(500);
        
        // When & Then
        assertTrue(testQuota.canWrite());
    }
    
    @Test
    void testQuotaUsage_CannotWrite() {
        // Given
        testQuota.setWritesUsed(500);
        testQuota.setWritesMax(500);
        
        // When & Then
        assertFalse(testQuota.canWrite());
    }
}

