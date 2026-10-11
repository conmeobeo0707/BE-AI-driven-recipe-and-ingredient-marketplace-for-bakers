package com.manhnv.capstonebakerai.customer.repository;

import com.manhnv.capstonebakerai.customer.entity.CustomerHobby;
import com.manhnv.capstonebakerai.customer.entity.CustomerHobbyId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CustomerHobbyRepository extends JpaRepository<CustomerHobby, CustomerHobbyId> {
    List<CustomerHobby> findAllByCustomerId(UUID customerId);

    void deleteAllByCustomerId(UUID customerId);
}
