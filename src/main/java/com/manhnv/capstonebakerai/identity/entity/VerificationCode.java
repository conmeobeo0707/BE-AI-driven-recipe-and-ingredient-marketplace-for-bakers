package com.manhnv.capstonebakerai.identity.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "verification_code")
@Getter
@Setter
@NoArgsConstructor
public class VerificationCode {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "verification_code_id", nullable = false, updatable = false)
    private UUID verificationCodeId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "email_verification_token", nullable = false, length = 255)
    private String emailVerificationToken;

    @Column(name = "email_verification_token_expiry_time", nullable = false)
    private LocalDateTime emailVerificationTokenExpiryTime;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
