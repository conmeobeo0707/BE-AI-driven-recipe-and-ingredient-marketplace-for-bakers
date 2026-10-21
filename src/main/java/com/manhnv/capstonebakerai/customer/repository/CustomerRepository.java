package com.manhnv.capstonebakerai.customer.repository;

import com.manhnv.capstonebakerai.customer.entity.Customer;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {
    Optional<Customer> findByUser_UserId(UUID userId);

    @Query("""
            select c
            from Customer c
            join fetch c.user
            order by c.user.createdAt desc, c.id asc
            """)
    List<Customer> findAllWithUserOrderByCreatedAtDesc();

    @Query("""
            select c
            from Customer c
            join fetch c.user
            where c.user.userId = :userId
            """)
    Optional<Customer> findByUserId(@Param("userId") UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select c
            from Customer c
            where c.user.userId = :userId
            """)
    Optional<Customer> findByUserIdForUpdate(@Param("userId") UUID userId);
}
