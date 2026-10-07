package com.manhnv.capstonebakerai.expense.repository;

import com.manhnv.capstonebakerai.expense.dto.response.ExpenseCategorySummary;
import com.manhnv.capstonebakerai.expense.dto.response.ExpenseVoucherResponse;
import com.manhnv.capstonebakerai.expense.entity.ExpenseVoucher;
import com.manhnv.capstonebakerai.expense.enums.ExpenseVoucherStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ExpenseVoucherRepository extends JpaRepository<ExpenseVoucher, UUID> {
    boolean existsByExpenseCategoryId(UUID categoryId);
    boolean existsByCode(String code);
    @Query("""
        select new com.manhnv.capstonebakerai.expense.dto.response.ExpenseCategorySummary(
            c.id, c.name, c.type, sum(v.amount), count(v))
        from ExpenseVoucher v
        join v.expenseCategory c
        where v.status = :status
          and v.createdAt >= :from
          and v.createdAt < :to
          and (:categoryId is null or c.id = :categoryId)
        group by c.id, c.name, c.type
        order by sum(v.amount) desc
        """)
    List<ExpenseCategorySummary> summarizeByCategory(
            @Param("status") ExpenseVoucherStatus status,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("categoryId") UUID categoryId);
    @Query("""
        select new com.manhnv.capstonebakerai.expense.dto.response.ExpenseVoucherResponse(
            v.id, v.code, c.id, c.name,
            v.amount, v.reason, v.status, v.createdBy, v.createdAt)
        from ExpenseVoucher v
        join v.expenseCategory c
        where v.status = :status
          and v.createdAt >= :from
          and v.createdAt < :to
          and (:categoryId is null or c.id = :categoryId)
        order by v.createdAt desc
        """)
    Page<ExpenseVoucherResponse> findVouchersByMonth(
            @Param("status") ExpenseVoucherStatus status,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("categoryId") UUID categoryId,
            Pageable pageable);
}
