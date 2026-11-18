package com.manhnv.capstonebakerai.order.repository;

import com.manhnv.capstonebakerai.order.entity.Order;
import com.manhnv.capstonebakerai.order.enums.OrderStatus;
import com.manhnv.capstonebakerai.report.dto.response.RevenueMonthRow;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID>, JpaSpecificationExecutor<Order> {

    boolean existsByCode(String code);

    @EntityGraph(attributePaths = {"items", "customer"})
    Optional<Order> findWithItemsById(UUID id);

    List<Order> findByStatusAndDeliveredAtGreaterThanEqualAndDeliveredAtLessThan(OrderStatus status, LocalDateTime from, LocalDateTime to);

    long countByStatusIn(Collection<OrderStatus> statuses);

    @Query("""
    select new com.manhnv.capstonebakerai.report.dto.response.RevenueMonthRow(
        extract(year from o.deliveredAt),
        extract(month from o.deliveredAt),
        count(o),
        sum(o.productsPrice),
        sum(o.discountAmount + o.voucherDiscountAmount),
        sum(o.revenue))
    from Order o
    where o.status = :status
      and o.deliveredAt >= :from and o.deliveredAt < :to
    group by extract(year from o.deliveredAt), extract(month from o.deliveredAt)
    """)
    List<RevenueMonthRow> revenueByMonth(@Param("status") OrderStatus status,
                                         @Param("from") LocalDateTime from,
                                         @Param("to") LocalDateTime to);
}
