package com.manhnv.capstonebakerai.order.repository;

import com.manhnv.capstonebakerai.order.entity.Order;
import com.manhnv.capstonebakerai.order.enums.OrderStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID>, JpaSpecificationExecutor<Order> {

    boolean existsByCode(String code);

    @EntityGraph(attributePaths = {"items", "customer"})
    Optional<Order> findWithItemsById(UUID id);

    List<Order> findByStatusAndDeliveredAtGreaterThanEqualAndDeliveredAtLessThan(OrderStatus status, LocalDateTime from, LocalDateTime to);
}
