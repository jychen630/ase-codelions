package com.twitter.mcp.service;

import com.twitter.mcp.model.OAuthToken;
import com.twitter.mcp.repository.OAuthTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jasypt.encryption.StringEncryptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OAuthTokenService {
    
    private final OAuthTokenRepository tokenRepository;
    
    @Autowired
    @Qualifier("jasyptStringEncryptor")
    private StringEncryptor stringEncryptor;
    
    @Transactional
    public OAuthToken saveToken(OAuthToken token) {
        // encrypt sensitive data before saving
        token.setAccessToken(encrypt(token.getAccessToken()));
        if (token.getRefreshToken() != null) {
            token.setRefreshToken(encrypt(token.getRefreshToken()));
        }
        
        OAuthToken saved = tokenRepository.save(token);
        log.info("Saved OAuth token for client: {} user: {}", token.getClientId(), token.getUserId());
        return saved;
    }
    
    public Optional<OAuthToken> getToken(String clientId, String userId) {
        Optional<OAuthToken> tokenOpt = tokenRepository.findByClientIdAndUserId(clientId, userId);
        
        if (tokenOpt.isPresent()) {
            OAuthToken token = tokenOpt.get();
            // decrypt when retrieving
            token.setAccessToken(decrypt(token.getAccessToken()));
            if (token.getRefreshToken() != null) {
                token.setRefreshToken(decrypt(token.getRefreshToken()));
            }
            return Optional.of(token);
        }
        
        return Optional.empty();
    }
    
    public List<OAuthToken> getTokensForClient(String clientId) {
        List<OAuthToken> tokens = tokenRepository.findByClientId(clientId);
        // decrypt all tokens
        tokens.forEach(token -> {
            token.setAccessToken(decrypt(token.getAccessToken()));
            if (token.getRefreshToken() != null) {
                token.setRefreshToken(decrypt(token.getRefreshToken()));
            }
        });
        return tokens;
    }
    
    @Transactional
    public void deleteToken(String clientId, String userId) {
        tokenRepository.deleteByClientIdAndUserId(clientId, userId);
        log.info("Deleted OAuth token for client: {} user: {}", clientId, userId);
    }
    
    @Transactional
    public void cleanupExpiredTokens() {
        LocalDateTime now = LocalDateTime.now();
        List<OAuthToken> expiredTokens = tokenRepository.findExpiredTokens(now);
        
        if (!expiredTokens.isEmpty()) {
            tokenRepository.deleteByExpiresAtBefore(now);
            log.info("Cleaned up {} expired tokens", expiredTokens.size());
        }
    }
    
    public List<OAuthToken> getTokensNeedingRefresh() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime soon = now.plusMinutes(5);
        return tokenRepository.findTokensNeedingRefresh(now, soon);
    }
    
    @Transactional
    public OAuthToken refreshToken(OAuthToken token, String newAccessToken, String newRefreshToken, LocalDateTime newExpiresAt) {
        token.setAccessToken(encrypt(newAccessToken));
        if (newRefreshToken != null) {
            token.setRefreshToken(encrypt(newRefreshToken));
        }
        token.setExpiresAt(newExpiresAt);
        
        OAuthToken refreshed = tokenRepository.save(token);
        log.info("Refreshed OAuth token for client: {} user: {}", token.getClientId(), token.getUserId());
        return refreshed;
    }
    
    private String encrypt(String plaintext) {
        if (plaintext == null) return null;
        return stringEncryptor.encrypt(plaintext);
    }
    
    private String decrypt(String ciphertext) {
        if (ciphertext == null) return null;
        return stringEncryptor.decrypt(ciphertext);
    }
}
