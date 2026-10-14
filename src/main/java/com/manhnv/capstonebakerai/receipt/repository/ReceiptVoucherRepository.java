package com.manhnv.capstonebakerai.receipt.repository;

import com.manhnv.capstonebakerai.receipt.entity.ReceiptVoucher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReceiptVoucherRepository extends JpaRepository<ReceiptVoucher, UUID> {
    boolean existsByCode(String code);
    boolean existsByReceiptCategoryId(UUID categoryId);

    @EntityGraph(attributePaths = "receiptCategory")
    Optional<ReceiptVoucher> findByPaymentId(UUID paymentId);

    @EntityGraph(attributePaths = "receiptCategory")
    Optional<ReceiptVoucher> findWithCategoryById(UUID id);

    @EntityGraph(attributePaths = "receiptCategory")
    Page<ReceiptVoucher> findByReceiptCategoryId(UUID categoryId, Pageable pageable);
}
