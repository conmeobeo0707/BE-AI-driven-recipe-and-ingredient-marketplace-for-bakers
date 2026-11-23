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
    Optional<StockOutNote> findByReferenceOrderIdAndType(UUID referenceOrderId, StockOutType type);
    boolean existsByCode(String code);
    Page<StockOutNote> findByStatus(NoteStatus status, Pageable pageable);
    Page<StockOutNote> findByType(StockOutType type, Pageable pageable);
    Page<StockOutNote> findByStatusAndType(NoteStatus status, StockOutType type, Pageable pageable);
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select n from StockOutNote n where n.noteId=:id")
    Optional<StockOutNote> lockById(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select n from StockOutNote n where n.referenceOrderId = :orderId and n.type = :type")
    Optional<StockOutNote> lockByReferenceOrderIdAndType(@Param("orderId") UUID orderId,
            @Param("type") StockOutType type);
}
