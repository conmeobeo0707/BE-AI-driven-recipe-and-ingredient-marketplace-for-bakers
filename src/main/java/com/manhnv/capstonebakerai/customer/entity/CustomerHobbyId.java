package com.manhnv.capstonebakerai.customer.entity;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class CustomerHobbyId implements Serializable {
    private UUID customerId;
    private UUID hobbyId;
}
