package com.manhnv.capstonebakerai.warehouse.repository;
import com.manhnv.capstonebakerai.warehouse.entity.StockInNote;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.manhnv.capstonebakerai.warehouse.enums.NoteStatus;
public interface StockInNoteRepository extends JpaRepository<StockInNote, UUID> {
    boolean existsByCode(String code);
    boolean existsBySupplierId(UUID supplierId);
    Page<StockInNote> findByStatus(NoteStatus status, Pageable pageable);
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select n from StockInNote n where n.noteId=:id")
    Optional<StockInNote> lockById(@Param("id") UUID id);
}
