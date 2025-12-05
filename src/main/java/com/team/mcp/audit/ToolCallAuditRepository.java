package com.team.mcp.audit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Repository for {@link ToolCallAudit}. */
@Repository
public interface ToolCallAuditRepository
    extends JpaRepository<ToolCallAudit, Long> { }

