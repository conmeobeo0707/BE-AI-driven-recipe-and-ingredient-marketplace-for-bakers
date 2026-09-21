//package com.manhnv.capstonebakerai.entity;
//
//import jakarta.persistence.*;
//import lombok.*;
//
//import java.time.LocalDateTime;
//
//@Entity
//@Table(name = "users")
//@Getter
//@Setter
//@Builder
//@NoArgsConstructor
//@AllArgsConstructor
//public class User {
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    @Column(nullable = false, unique = true, length = 100)
//    private String username;
//
//    @Column(name = "password_hash", nullable = false, length = 255)
//    private String passwordHash;
//
//    @Column(nullable = false)
//    private Boolean enabled;
//
//    @Column(name = "created_at", nullable = false)
//    private LocalDateTime createdAt;
//
//}
