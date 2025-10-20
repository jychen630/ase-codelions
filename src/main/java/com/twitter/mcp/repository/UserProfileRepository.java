package com.twitter.mcp.repository;

import com.twitter.mcp.model.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {
    
    Optional<UserProfile> findByUserId(String userId);
    
    Optional<UserProfile> findByUsername(String username);
    
    @Query("SELECT u FROM UserProfile u WHERE u.lastSyncAt < :cutoff OR u.lastSyncAt IS NULL")
    List<UserProfile> findProfilesNeedingSync(@Param("cutoff") LocalDateTime cutoff);
    
    @Query("SELECT u FROM UserProfile u WHERE u.username LIKE %:query% OR u.displayName LIKE %:query%")
    List<UserProfile> searchByUsernameOrDisplayName(@Param("query") String query);
}
