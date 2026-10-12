package com.manhnv.capstonebakerai.warehouse.repository;
import com.manhnv.capstonebakerai.warehouse.entity.StockOutNote;
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
import com.manhnv.capstonebakerai.warehouse.enums.StockOutType;
public interface StockOutNoteRepository extends JpaRepository<StockOutNote, UUID> {
    boolean existsByCode(String code);
    @Query("select n from StockOutNote n where (:status is null or n.status=:status) and (:type is null or n.type=:type)")
    Page<StockOutNote> search(@Param("status") NoteStatus status, @Param("type") StockOutType type, Pageable pageable);
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select n from StockOutNote n where n.noteId=:id")
    Optional<StockOutNote> lockById(@Param("id") UUID id);
}
