package com.manhnv.capstonebakerai.warehouse.repository;
import com.manhnv.capstonebakerai.warehouse.entity.StocktakeNote;
import com.manhnv.capstonebakerai.warehouse.enums.StocktakeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
public interface StocktakeNoteRepository extends JpaRepository<StocktakeNote, UUID> {
    boolean existsByCode(String code);
    boolean existsByStatus(StocktakeStatus status);
    @Query("select n from StocktakeNote n where (:status is null or n.status=:status)")
    Page<StocktakeNote> search(@Param("status") StocktakeStatus status, Pageable pageable);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select n from StocktakeNote n where n.noteId=:id")
    java.util.Optional<StocktakeNote> lockById(@Param("id") UUID id);
}
