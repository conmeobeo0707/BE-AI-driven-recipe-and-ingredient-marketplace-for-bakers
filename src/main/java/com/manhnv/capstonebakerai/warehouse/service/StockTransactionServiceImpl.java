package com.manhnv.capstonebakerai.warehouse.service;

import com.manhnv.capstonebakerai.warehouse.dto.StockTransactionFilter;
import com.manhnv.capstonebakerai.warehouse.dto.StockTransactionResponse;
import com.manhnv.capstonebakerai.warehouse.entity.StockTransaction;
import com.manhnv.capstonebakerai.warehouse.enums.StockTransactionRefType;
import com.manhnv.capstonebakerai.warehouse.enums.StockTransactionType;
import com.manhnv.capstonebakerai.warehouse.repository.StockTransactionRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class StockTransactionServiceImpl implements StockTransactionService {
    private final StockTransactionRepository transactions;

    public StockTransactionServiceImpl(StockTransactionRepository transactions) {
        this.transactions = transactions;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StockTransactionResponse> search(StockTransactionFilter filter, Pageable pageable) {
        if (filter.from() != null && filter.to() != null && filter.from().isAfter(filter.to())) {
            throw WarehouseSupport.error("The from date must be before or equal to the to date");
        }

        Specification<StockTransaction> specification = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (filter.productId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("productId"), filter.productId()));
            }
            if (filter.stockLotId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("stockLotId"), filter.stockLotId()));
            }
            if (filter.transactionType() != null) {
                predicates.add(criteriaBuilder.equal(root.get("transactionType"), filter.transactionType()));
            }
            if (filter.refType() != null) {
                predicates.add(criteriaBuilder.equal(root.get("refType"), filter.refType()));
            }
            if (filter.refId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("refId"), filter.refId()));
            }
            if (filter.from() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.<LocalDateTime>get("createdAt"), filter.from()));
            }
            if (filter.to() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.<LocalDateTime>get("createdAt"), filter.to()));
            }
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };

        return transactions.findAll(specification, pageable).map(this::toResponse);
    }

    @Override
    public void record(UUID stockLotId, UUID productId, StockTransactionType transactionType,
            BigDecimal quantityChange, BigDecimal balanceAfter,
            StockTransactionRefType refType, UUID refId) {
        StockTransaction stockTransaction = new StockTransaction();
        stockTransaction.setStockLotId(stockLotId);
        stockTransaction.setProductId(productId);
        stockTransaction.setTransactionType(transactionType);
        stockTransaction.setQuantityChange(quantityChange);
        stockTransaction.setBalanceAfter(balanceAfter);
        stockTransaction.setRefType(refType);
        stockTransaction.setRefId(refId);
        stockTransaction.setCreatedBy(currentUserIdOrNull());
        stockTransaction.setCreatedAt(LocalDateTime.now());
        transactions.save(stockTransaction);
    }

    private UUID currentUserIdOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            return null;
        }
        try {
            return UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private StockTransactionResponse toResponse(StockTransaction stockTransaction) {
        return new StockTransactionResponse(stockTransaction.getId(), stockTransaction.getStockLotId(),
                stockTransaction.getProductId(), stockTransaction.getTransactionType(),
                stockTransaction.getQuantityChange(), stockTransaction.getBalanceAfter(),
                stockTransaction.getRefType(), stockTransaction.getRefId(),
                stockTransaction.getCreatedBy(), stockTransaction.getCreatedAt());
    }
}
