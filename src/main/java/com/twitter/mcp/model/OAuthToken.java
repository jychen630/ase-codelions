package com.twitter.mcp.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "oauth_tokens")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OAuthToken {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "client_id", nullable = false)
    private String clientId;
    
    @Column(name = "user_id", nullable = false)
    private String userId;
    
    @Column(name = "access_token", nullable = false, length = 2000)
    private String accessToken; // encrypted
    
    @Column(name = "refresh_token", length = 2000)
    private String refreshToken; // encrypted
    
    @Column(name = "token_type")
    private String tokenType = "Bearer";
    
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
    
    @Column(name = "scope")
    private String scope;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
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
    
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }
    
    public boolean needsRefresh() {
        // refresh if expires within 5 minutes
        return expiresAt != null && LocalDateTime.now().plusMinutes(5).isAfter(expiresAt);
    }
}
