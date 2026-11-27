package com.manhnv.capstonebakerai.warehouse.repository;
import com.manhnv.capstonebakerai.warehouse.entity.StocktakeNote;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
public interface StocktakeNoteRepository extends JpaRepository<StocktakeNote, UUID> {
    java.util.Optional<StocktakeNote> findByCodeIgnoreCase(String code);
    boolean existsByCode(String code);
    @Query("""
            select n from StocktakeNote n
            where (:manager = true or n.handleBy = :userId)
              and (:status is null or :status = case
                when cast(n.status as String) = 'COMPLETED' then 'COMPLETED'
                when cast(n.status as String) = 'CANCELLED' then 'CANCELLED'
                when n.countsSubmittedAt is not null then 'PENDING_DISPOSAL'
                when n.handleBy is not null then 'ASSIGNED'
                else 'DRAFT' end)
            """)
    Page<StocktakeNote> findVisible(boolean manager, UUID userId, String status, Pageable pageable);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select n from StocktakeNote n where n.noteId=:id")
    java.util.Optional<StocktakeNote> lockById(@org.springframework.data.repository.query.Param("id") UUID id);
}
