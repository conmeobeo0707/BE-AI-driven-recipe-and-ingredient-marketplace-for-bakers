package com.manhnv.capstonebakerai.customer.repository;

import com.manhnv.capstonebakerai.customer.entity.CustomerAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerAddressRepository extends JpaRepository<CustomerAddress, UUID> {
    List<CustomerAddress> findByCustomerIdOrderByDefaultAddressDescCreatedAtDesc(UUID customerId);

    List<CustomerAddress> findByCustomerId(UUID customerId);

    Optional<CustomerAddress> findByIdAndCustomerId(UUID id, UUID customerId);

    long countByCustomerId(UUID customerId);
}
