package com.manhnv.capstonebakerai.customer.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@Table(name = "customer_hobbies")
@IdClass(CustomerHobbyId.class)
@Getter
@Setter
@NoArgsConstructor
public class CustomerHobby {

    @Id
    @Column(name = "customer_id")
    private UUID customerId;

    @Id
    @Column(name = "hobby_id")
    private UUID hobbyId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public CustomerHobby(UUID customerId, UUID hobbyId) {
        this.customerId = customerId;
        this.hobbyId = hobbyId;
        this.createdAt = LocalDateTime.now(ZoneOffset.UTC);
    }
}
