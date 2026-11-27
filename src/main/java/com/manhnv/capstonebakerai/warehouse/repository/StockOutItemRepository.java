package com.manhnv.capstonebakerai.warehouse.repository;
import com.manhnv.capstonebakerai.warehouse.dto.PendingLotDisposal;
import com.manhnv.capstonebakerai.warehouse.entity.StockOutItem;
import com.manhnv.capstonebakerai.warehouse.enums.NoteStatus;
import com.manhnv.capstonebakerai.warehouse.enums.StockOutType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
public interface StockOutItemRepository extends JpaRepository<StockOutItem, UUID> {
    List<StockOutItem> findByNoteIdOrderByItemId(UUID noteId);
    void deleteByNoteId(UUID noteId);

    @Query("""
            select count(i) > 0 from StockOutItem i
            join StockOutNote n on n.noteId = i.noteId
            where i.lotId = :lotId and n.status = :status and n.type = :type
            """)
    boolean hasDisposalForLot(@Param("lotId") UUID lotId, @Param("status") NoteStatus status,
                              @Param("type") StockOutType type);

    @Query("""
            select distinct new com.manhnv.capstonebakerai.warehouse.dto.PendingLotDisposal(i.lotId, n.code)
            from StockOutItem i
            join StockOutNote n on n.noteId = i.noteId
            join StockLot l on l.lotId = i.lotId
            where n.status = :status and n.type = :type and l.expiryDate < :today
            order by n.code
            """)
    List<PendingLotDisposal> findDisposalsForExpiredLots(@Param("status") NoteStatus status,
            @Param("type") StockOutType type, @Param("today") LocalDate today);
}
