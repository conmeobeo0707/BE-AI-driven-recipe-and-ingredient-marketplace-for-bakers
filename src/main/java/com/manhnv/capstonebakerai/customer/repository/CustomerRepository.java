package com.manhnv.capstonebakerai.customer.repository;

import com.manhnv.capstonebakerai.customer.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {
}
