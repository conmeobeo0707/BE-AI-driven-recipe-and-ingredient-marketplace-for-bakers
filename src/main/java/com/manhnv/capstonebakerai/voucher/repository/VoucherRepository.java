package com.manhnv.capstonebakerai.voucher.repository;

import com.manhnv.capstonebakerai.voucher.entity.Voucher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, UUID>, JpaSpecificationExecutor<Voucher> {
    boolean existsByCode(String code);

    Optional<Voucher> findByCode(String code);

    /** Voucher công khai: đang bật, trong thời gian chạy và còn số lượng. */
    @Query("""
            select v from Voucher v
            where v.status = com.manhnv.capstonebakerai.voucher.enums.VoucherStatus.ACTIVE
              and v.startDate <= :now and v.endDate >= :now
              and v.usedQuantity < v.totalQuantity
            """)
    Page<Voucher> findAvailable(@Param("now") LocalDateTime now, Pageable pageable);

    /** Tăng số lượt dùng, chỉ thành công khi còn số lượng. Trả về 0 nếu hết. */
    @Modifying(flushAutomatically = true)
    @Query("""
            update Voucher v set v.usedQuantity = v.usedQuantity + 1
            where v.id = :id and v.usedQuantity < v.totalQuantity
            """)
    int increaseUsed(@Param("id") UUID id);

    /** Hoàn lượt dùng khi đơn bị hủy. */
    @Modifying(flushAutomatically = true)
    @Query("""
            update Voucher v set v.usedQuantity = v.usedQuantity - 1
            where v.id = :id and v.usedQuantity > 0
            """)
    int decreaseUsed(@Param("id") UUID id);
}
