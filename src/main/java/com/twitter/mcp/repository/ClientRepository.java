package com.twitter.mcp.repository;

import com.twitter.mcp.model.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {
    
    Optional<Client> findByClientId(String clientId);
    
    List<Client> findByIsActiveTrue();
    
    boolean existsByClientId(String clientId);
}
