package com.manhnv.capstonebakerai.product.entities;

import com.manhnv.capstonebakerai.product.enums.ProductStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "product")
@Getter@Setter@NoArgsConstructor@AllArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID productId;

    @Column(name = "name", nullable = false)
    String name;

    @Column(name = "description")
    String description;

    @Column(name = "img_url")
    String imgUrl;

    @Column(name = "price", nullable = false)
    String price;

    @Column(name = "quantity", nullable = false)
    int quantity;

    @Column(name = "status", nullable = false)
    ProductStatus status;

    @Column(name = "created_at")
    LocalDate createdAt;

    @Column(name = "store_id", nullable = false)
    UUID storeId;
}
