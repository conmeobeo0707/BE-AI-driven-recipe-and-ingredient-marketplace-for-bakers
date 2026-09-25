package com.manhnv.capstonebakerai.baker.entity;

import com.manhnv.capstonebakerai.baker.enums.BakerVerificationStatus;
import com.manhnv.capstonebakerai.identity.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "bakers")
@Getter
@Setter
@NoArgsConstructor
public class Baker {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "baker_id", nullable = false, updatable = false)
    private UUID bakerId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "follower", nullable = false)
    private int follower;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 20)
    private BakerVerificationStatus verificationStatus = BakerVerificationStatus.PENDING;

    @Column(name = "bio", columnDefinition = "TEXT")
    private String bio;

    @Column(name = "total_recipe", nullable = false)
    private int totalRecipe;

    @Column(name = "national_id_number", length = 32)
    private String nationalIdNumber;

    @Column(name = "cccd_front_url", length = 2048)
    private String cccdFrontUrl;

    @Column(name = "cccd_back_url", length = 2048)
    private String cccdBackUrl;

    @Column(name = "certificate_url", length = 2048)
    private String certificateUrl;

    @Column(name = "verification_submitted_at")
    private LocalDateTime verificationSubmittedAt;

    @Column(name = "verification_reviewed_at")
    private LocalDateTime verificationReviewedAt;

    @Column(name = "verification_reject_reason", columnDefinition = "TEXT")
    private String verificationRejectReason;
}
