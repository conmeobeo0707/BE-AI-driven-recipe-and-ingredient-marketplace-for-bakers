package com.manhnv.capstonebakerai.media.entity;

import com.manhnv.capstonebakerai.media.enums.MediaStatus;
import com.manhnv.capstonebakerai.media.enums.MediaType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "media")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Media {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private MediaStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private MediaType type;

    @Column(name = "url")
    private String url;

    @Version
    private Long version;

    @Column(name = "temp_since")
    private Instant tempSince;
}
