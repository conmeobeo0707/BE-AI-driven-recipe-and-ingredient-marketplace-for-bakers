package com.manhnv.capstonebakerai.quote.entity;

import com.manhnv.capstonebakerai.customer.entity.Customer;
import com.manhnv.capstonebakerai.quote.enums.RequestQuoteStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "request_quote",
        indexes = {
                @Index(name = "idx_request_quote_customer", columnList = "customer_id, status"),
                @Index(name = "idx_request_quote_status_created", columnList = "status, created_at")
        })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestQuote {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @OneToMany(mappedBy = "requestQuote", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<RequestQuoteDetail> details = new ArrayList<>();

    @Column(name = "content", length = 1000)
    private String content;

    @Column(name = "contact_phone", nullable = false, length = 20)
    private String contactPhone;

    @Column(name = "contact_name", length = 150)
    private String contactName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RequestQuoteStatus status;

    // userId của staff/manager đang xử lý
    @Column(name = "handled_by")
    private UUID handledBy;

    @Column(name = "handled_at")
    private LocalDateTime handledAt;

    @Column(name = "reject_reason", length = 500)
    private String rejectReason;

    // Ghi chú kết quả, ví dụ "đã tạo đơn OD-XXXXXX" (tùy chọn)
    @Column(name = "result_note", length = 500)
    private String resultNote;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public void addDetail(RequestQuoteDetail detail) {
        detail.setRequestQuote(this);
        this.details.add(detail);
    }
}
