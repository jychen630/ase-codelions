package com.twitter.mcp.repository;

import com.twitter.mcp.model.OAuthToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OAuthTokenRepository extends JpaRepository<OAuthToken, Long> {
    
    Optional<OAuthToken> findByClientIdAndUserId(String clientId, String userId);
    
    List<OAuthToken> findByClientId(String clientId);
    
    List<OAuthToken> findByUserId(String userId);
    
    @Query("SELECT t FROM OAuthToken t WHERE t.expiresAt < :now")
    List<OAuthToken> findExpiredTokens(@Param("now") LocalDateTime now);
    
    @Query("SELECT t FROM OAuthToken t WHERE t.expiresAt BETWEEN :now AND :soon")
    List<OAuthToken> findTokensNeedingRefresh(@Param("now") LocalDateTime now, @Param("soon") LocalDateTime soon);
    
    void deleteByClientIdAndUserId(String clientId, String userId);
    
    void deleteByExpiresAtBefore(LocalDateTime expiredBefore);
}
