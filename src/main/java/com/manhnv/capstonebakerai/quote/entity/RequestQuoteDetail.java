package com.manhnv.capstonebakerai.quote.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "request_quote_detail",
        indexes = @Index(name = "idx_rqd_request", columnList = "request_quote_id"))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestQuoteDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_quote_id", nullable = false)
    private RequestQuote requestQuote;

    @Column(name = "product_unit_id", nullable = false)
    private UUID productUnitId;

    @Column(name = "quantity", nullable = false)
    private int quantity;
}
