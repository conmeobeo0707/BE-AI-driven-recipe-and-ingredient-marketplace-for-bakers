package com.manhnv.capstonebakerai.warehouse.service;

import com.manhnv.capstonebakerai.warehouse.dto.StockTransactionFilter;
import com.manhnv.capstonebakerai.warehouse.dto.StockTransactionResponse;
import com.manhnv.capstonebakerai.user.repository.UserRepository;
import com.manhnv.capstonebakerai.warehouse.entity.StockTransaction;
import com.manhnv.capstonebakerai.warehouse.enums.StockTransactionRefType;
import com.manhnv.capstonebakerai.warehouse.enums.StockTransactionType;
import com.manhnv.capstonebakerai.warehouse.repository.StockInNoteRepository;
import com.manhnv.capstonebakerai.warehouse.repository.StockOutNoteRepository;
import com.manhnv.capstonebakerai.warehouse.repository.StocktakeNoteRepository;
import com.manhnv.capstonebakerai.warehouse.repository.StockTransactionRepository;
import com.manhnv.capstonebakerai.user.dto.UserSummaryResponse;
import com.manhnv.capstonebakerai.user.service.UserSummaryService;
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
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
public class StockTransactionServiceImpl implements StockTransactionService {
    private final StockTransactionRepository transactions;
    private final UserSummaryService userSummaryService;

    private final StockInNoteRepository stockInNotes;
    private final StockOutNoteRepository stockOutNotes;
    private final StocktakeNoteRepository stocktakes;
    private final UserRepository users;

    public StockTransactionServiceImpl(StockTransactionRepository transactions, StockInNoteRepository stockInNotes,
                                       StockOutNoteRepository stockOutNotes, StocktakeNoteRepository stocktakes,
                                       UserRepository users) {
        this.transactions = transactions;
        this.stockInNotes = stockInNotes;
        this.stockOutNotes = stockOutNotes;
        this.stocktakes = stocktakes;
        this.users = users;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StockTransactionResponse> search(StockTransactionFilter filter, Pageable pageable) {
        if (filter.from() != null && filter.to() != null && filter.from().isAfter(filter.to())) {
            throw WarehouseSupport.error("The from date must be before or equal to the to date");
        }

        boolean filterByCode = filter.refCode() != null && !filter.refCode().isBlank();
        Map<StockTransactionRefType, UUID> matchingReferences = filterByCode
                ? findReferences(filter.refType(), filter.refCode().strip()) : Map.of();
        if (filterByCode && matchingReferences.isEmpty()) return Page.empty(pageable);

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
            if (filterByCode) {
                Predicate[] references = matchingReferences.entrySet().stream()
                        .map(entry -> criteriaBuilder.and(
                                criteriaBuilder.equal(root.get("refType"), entry.getKey()),
                                criteriaBuilder.equal(root.get("refId"), entry.getValue())))
                        .toArray(Predicate[]::new);
                predicates.add(criteriaBuilder.or(references));
            }
            if (filter.from() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.<LocalDateTime>get("createdAt"), filter.from()));
            }
            if (filter.to() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.<LocalDateTime>get("createdAt"), filter.to()));
            }
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };

        Page<StockTransaction> results = transactions.findAll(specification, pageable);
        Map<StockTransactionRefType, Map<UUID, String>> codes = referenceCodes(results.getContent());
        Map<UUID, String> names = new HashMap<>();
        List<UUID> userIds = results.stream().map(StockTransaction::getCreatedBy)
                .filter(Objects::nonNull).distinct().toList();
        users.findAllById(userIds).forEach(user -> names.put(user.getUserId(), user.getFullName()));
        return results.map(transaction -> toResponse(transaction,
                codes.getOrDefault(transaction.getRefType(), Map.of()).get(transaction.getRefId()),
                names.get(transaction.getCreatedBy())));
    }

    private Map<StockTransactionRefType, UUID> findReferences(StockTransactionRefType type, String code) {
        Map<StockTransactionRefType, UUID> references = new EnumMap<>(StockTransactionRefType.class);
        if (type == null || type == StockTransactionRefType.STOCK_IN_NOTE) {
            stockInNotes.findByCodeIgnoreCase(code).ifPresent(note ->
                    references.put(StockTransactionRefType.STOCK_IN_NOTE, note.getNoteId()));
        }
        if (type == null || type == StockTransactionRefType.STOCK_OUT_NOTE) {
            stockOutNotes.findByCodeIgnoreCase(code).ifPresent(note ->
                    references.put(StockTransactionRefType.STOCK_OUT_NOTE, note.getNoteId()));
        }
        if (type == null || type == StockTransactionRefType.STOCKTAKE_NOTE) {
            stocktakes.findByCodeIgnoreCase(code).ifPresent(note ->
                    references.put(StockTransactionRefType.STOCKTAKE_NOTE, note.getNoteId()));
        }
        return references;
    }

    private Map<StockTransactionRefType, Map<UUID, String>> referenceCodes(List<StockTransaction> page) {
        Map<StockTransactionRefType, List<UUID>> ids = new EnumMap<>(StockTransactionRefType.class);
        for (StockTransaction transaction : page) {
            ids.computeIfAbsent(transaction.getRefType(), type -> new ArrayList<>()).add(transaction.getRefId());
        }
        Map<StockTransactionRefType, Map<UUID, String>> codes = new EnumMap<>(StockTransactionRefType.class);
        ids.forEach((type, noteIds) -> {
            Map<UUID, String> noteCodes = new HashMap<>();
            switch (type) {
                case STOCK_IN_NOTE -> stockInNotes.findAllById(noteIds)
                        .forEach(note -> noteCodes.put(note.getNoteId(), note.getCode()));
                case STOCK_OUT_NOTE -> stockOutNotes.findAllById(noteIds)
                        .forEach(note -> noteCodes.put(note.getNoteId(), note.getCode()));
                case STOCKTAKE_NOTE -> stocktakes.findAllById(noteIds)
                        .forEach(note -> noteCodes.put(note.getNoteId(), note.getCode()));
            }
            codes.put(type, noteCodes);
        });
        return codes;
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

    private StockTransactionResponse toResponse(StockTransaction stockTransaction, String refCode, String createdByName) {
        return new StockTransactionResponse(stockTransaction.getId(), stockTransaction.getStockLotId(),
                stockTransaction.getProductId(), stockTransaction.getTransactionType(),
                stockTransaction.getQuantityChange(), stockTransaction.getBalanceAfter(),
                stockTransaction.getRefType(), stockTransaction.getRefId(),
                stockTransaction.getCreatedBy(), stockTransaction.getCreatedAt(), refCode, createdByName);
    }
}
