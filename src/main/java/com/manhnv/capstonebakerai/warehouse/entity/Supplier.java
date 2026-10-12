package com.manhnv.capstonebakerai.warehouse.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.UUID;

@Entity @Table(name = "suppliers")
@Getter @Setter @NoArgsConstructor
public class Supplier {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "supplier_id")
    private UUID supplierId;
    @Column(name = "supplier_name", nullable = false, length = 255)
    private String supplierName;
    @Column(name = "phone_number", length = 20)
    private String phoneNumber;
    @Column(length = 100)
    private String email;
    @Column(columnDefinition = "text")
    private String address;
}
