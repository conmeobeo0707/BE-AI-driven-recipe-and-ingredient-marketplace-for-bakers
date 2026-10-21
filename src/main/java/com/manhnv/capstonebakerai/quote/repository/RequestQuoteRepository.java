package com.manhnv.capstonebakerai.quote.repository;

import com.manhnv.capstonebakerai.quote.entity.RequestQuote;
import com.manhnv.capstonebakerai.quote.enums.RequestQuoteStatus;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RequestQuoteRepository extends JpaRepository<RequestQuote, UUID>, JpaSpecificationExecutor<RequestQuote> {
    @EntityGraph(attributePaths = {"details", "customer"})
    Optional<RequestQuote> findWithDetailsById(UUID id);

    // Nhận xử lý: chỉ thành công nếu yêu cầu còn PENDING, chống 2 staff bấm cùng lúc
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update RequestQuote q
           set q.status = :to, q.handledBy = :staffId, q.handledAt = :now
         where q.id = :id and q.status = :from
        """)
    int claim(@Param("id") UUID id,
              @Param("from") RequestQuoteStatus from,
              @Param("to") RequestQuoteStatus to,
              @Param("staffId") UUID staffId,
              @Param("now") LocalDateTime now);
}
