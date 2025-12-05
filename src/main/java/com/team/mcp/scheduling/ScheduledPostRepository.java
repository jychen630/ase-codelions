package com.team.mcp.scheduling;

import com.team.mcp.scheduling.ScheduledPost.Status;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * JPA repository for {@link ScheduledPost} with queries used by the publisher.
 */
public interface ScheduledPostRepository
    extends JpaRepository<ScheduledPost, Long> {

  /**
   * Returns PENDING posts whose {@code runAt <= :now}, ordered by run time.
   *
   * @param now current time (inclusive upper bound)
   * @param status lifecycle status to match (usually {@code PENDING})
   * @param pageable page request (use to limit batch size)
   * @return due scheduled posts
   */
  @Query("""
      select s
        from ScheduledPost s
       where s.status = :status
         and s.runAt <= :now
    order by s.runAt asc
      """)
  List<ScheduledPost> findDue(@Param("now") Instant now,
                              @Param("status") Status status,
                              Pageable pageable);

  /**
   * Finds a scheduled post by id only if it matches the given status.
   *
   * @param id database id
   * @param status required lifecycle status
   * @return optional matching entity
   */
  Optional<ScheduledPost> findByIdAndStatus(Long id, Status status);
}
