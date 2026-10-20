package com.manhnv.capstonebakerai.quote.service;

import com.manhnv.capstonebakerai.common.exception.AppException;
import com.manhnv.capstonebakerai.customer.entity.Customer;
import com.manhnv.capstonebakerai.customer.repository.CustomerRepository;
import com.manhnv.capstonebakerai.product.entities.Product;
import com.manhnv.capstonebakerai.product.entities.ProductUnit;
import com.manhnv.capstonebakerai.product.entities.UnitStatus;
import com.manhnv.capstonebakerai.product.enums.ProductStatus;
import com.manhnv.capstonebakerai.product.repository.ProductRepository;
import com.manhnv.capstonebakerai.product.repository.ProductUnitRepository;
import com.manhnv.capstonebakerai.quote.dto.request.MarkOrderedRequest;
import com.manhnv.capstonebakerai.quote.dto.request.RejectRequestQuoteRequest;
import com.manhnv.capstonebakerai.quote.dto.request.RequestQuoteItemRequest;
import com.manhnv.capstonebakerai.quote.dto.request.RequestQuoteRequest;
import com.manhnv.capstonebakerai.quote.dto.response.RequestQuoteResponse;
import com.manhnv.capstonebakerai.quote.dto.response.RequestQuoteSummaryResponse;
import com.manhnv.capstonebakerai.quote.entity.RequestQuote;
import com.manhnv.capstonebakerai.quote.entity.RequestQuoteDetail;
import com.manhnv.capstonebakerai.quote.enums.RequestQuoteStatus;
import com.manhnv.capstonebakerai.quote.event.RequestQuoteCreatedEvent;
import com.manhnv.capstonebakerai.quote.event.RequestQuoteStatusChangedEvent;
import com.manhnv.capstonebakerai.quote.exception.RequestQuoteErrorCode;
import com.manhnv.capstonebakerai.quote.mapper.QuoteLookup;
import com.manhnv.capstonebakerai.quote.mapper.RequestQuoteMapper;
import com.manhnv.capstonebakerai.quote.repository.RequestQuoteRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RequestQuoteServiceImpl implements RequestQuoteService {

    RequestQuoteRepository repository;
    CustomerRepository customerRepository;
    ProductRepository productRepository;
    ProductUnitRepository productUnitRepository;
    RequestQuoteMapper mapper;
    ApplicationEventPublisher eventPublisher;


    @Override
    @Transactional
    public RequestQuoteResponse create(UUID userId, RequestQuoteRequest request) {
        Customer customer = findCustomerByUserId(userId);
        RequestQuote q = RequestQuote.builder()
                .customer(customer)
                .contactPhone(request.contactPhone())
                .contactName(request.contactName())
                .content(request.content())
                .status(RequestQuoteStatus.PENDING)
                .build();
        applyItems(q, request.items());
        RequestQuote saved = repository.save(q);

        // Notification Configure
        eventPublisher.publishEvent(new RequestQuoteCreatedEvent(
                saved.getId(),
                saved.getContactName(),
                saved.getContactPhone()
        ));
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RequestQuoteSummaryResponse> getMine(UUID userId, RequestQuoteStatus status, Pageable pageable) {
        Customer customer = findCustomerByUserId(userId);
        return repository.findAll(spec(customer.getId(), status), pageable).map(mapper::toSummary);
    }

    @Override
    @Transactional(readOnly = true)
    public RequestQuoteResponse getMine(UUID userId, UUID id) {
        return toResponse(findOwned(userId, id));
    }

    @Override
    @Transactional
    public RequestQuoteResponse updateMine(UUID userId, UUID id, RequestQuoteRequest request) {
        RequestQuote q = findOwned(userId, id);
        if (!q.getStatus().isEditableByCustomer()) {
            throw new AppException(RequestQuoteErrorCode.REQUEST_QUOTE_CANNOT_EDIT);
        }
        q.setContactPhone(request.contactPhone());
        q.setContactName(request.contactName());
        q.setContent(request.content());
        q.getDetails().clear();                 // orphanRemoval sẽ xóa các dòng cũ
        applyItems(q, request.items());
        return toResponse(repository.save(q));
    }

    @Override
    @Transactional
    public RequestQuoteResponse cancelMine(UUID userId, UUID id) {
        RequestQuote q = findOwned(userId, id);
        if (!q.getStatus().isCancellableByCustomer()) {
            throw new AppException(RequestQuoteErrorCode.REQUEST_QUOTE_CANNOT_CANCEL);
        }
        q.setStatus(RequestQuoteStatus.CANCELLED);
        RequestQuote saved = repository.save(q);
        publishStatusChanged(saved, true);
        return toResponse(saved);
    }

    // ===== Staff / Manager =====

    @Override
    @Transactional(readOnly = true)
    public Page<RequestQuoteSummaryResponse> getAll(RequestQuoteStatus status, Pageable pageable) {
        return repository.findAll(spec(null, status), pageable).map(mapper::toSummary);
    }

    @Override
    @Transactional(readOnly = true)
    public RequestQuoteResponse getForStaff(UUID id) {
        return toResponse(findOrThrow(id));
    }

    @Override
    @Transactional
    public RequestQuoteResponse claim(UUID id, UUID staffUserId) {
        int updated = repository.claim(id, RequestQuoteStatus.PENDING,
                RequestQuoteStatus.IN_PROGRESS, staffUserId, LocalDateTime.now());
        if (updated == 0) {
            if (!repository.existsById(id)) {
                throw new AppException(RequestQuoteErrorCode.REQUEST_QUOTE_NOT_FOUND);
            }
            throw new AppException(RequestQuoteErrorCode.REQUEST_QUOTE_ALREADY_HANDLED);
        }
        RequestQuote q = findOrThrow(id);
        publishStatusChanged(q, false);
        return toResponse(q);
    }

    @Override
    @Transactional
    public RequestQuoteResponse markOrdered(UUID id, UUID staffUserId, boolean manager, MarkOrderedRequest request) {
        RequestQuote q = findOrThrow(id);
        assertHandler(q, staffUserId, manager);
        transition(q, RequestQuoteStatus.ORDERED);
        if (request != null) q.setResultNote(request.resultNote());
        RequestQuote saved = repository.save(q);
        publishStatusChanged(saved, false);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public RequestQuoteResponse reject(UUID id, UUID staffUserId, boolean manager, RejectRequestQuoteRequest request) {
        RequestQuote q = findOrThrow(id);
        assertHandler(q, staffUserId, manager);
        transition(q, RequestQuoteStatus.REJECTED);
        if (q.getHandledBy() == null) {         // từ chối thẳng khi còn PENDING
            q.setHandledBy(staffUserId);
            q.setHandledAt(LocalDateTime.now());
        }
        q.setRejectReason(request.reason());
        RequestQuote saved = repository.save(q);
        publishStatusChanged(saved, false);
        return toResponse(saved);
    }

    // ===== Helpers =====

    private void publishStatusChanged(RequestQuote q, boolean byCustomer) {
        eventPublisher.publishEvent(new RequestQuoteStatusChangedEvent(
                q.getId(),
                q.getCustomer().getUser().getUserId(),
                q.getStatus(),
                q.getHandledBy(),
                q.getRejectReason(),
                byCustomer
        ));
    }

    private void transition(RequestQuote q, RequestQuoteStatus next) {
        if (!q.getStatus().canTransitionTo(next)) {
            throw new AppException(RequestQuoteErrorCode.REQUEST_QUOTE_INVALID_STATUS_TRANSITION);
        }
        q.setStatus(next);
    }

    // Đang IN_PROGRESS thì chỉ người đã nhận (hoặc manager) được thao tác
    private void assertHandler(RequestQuote q, UUID staffUserId, boolean manager) {
        if (q.getStatus() == RequestQuoteStatus.IN_PROGRESS
                && !manager && !staffUserId.equals(q.getHandledBy())) {
            throw new AppException(RequestQuoteErrorCode.REQUEST_QUOTE_NOT_HANDLER);
        }
    }

    private void applyItems(RequestQuote q, List<RequestQuoteItemRequest> items) {
        List<UUID> unitIds = items.stream().map(RequestQuoteItemRequest::productUnitId).toList();
        if (unitIds.size() != new HashSet<>(unitIds).size()) {
            throw new AppException(RequestQuoteErrorCode.DUPLICATE_ITEM);
        }
        Map<UUID, ProductUnit> units = loadUnits(unitIds);
        Map<UUID, Product> products = loadProducts(units);

        for (RequestQuoteItemRequest item : items) {
            ProductUnit unit = units.get(item.productUnitId());
            if (unit == null) throw new AppException(RequestQuoteErrorCode.PRODUCT_UNIT_NOT_FOUND);
            Product product = products.get(unit.getProductId());
            if (product == null || product.getStatus() != ProductStatus.ACTIVE
                    || unit.getStatus() != UnitStatus.ACTIVE || !unit.isSale()) {
                throw new AppException(RequestQuoteErrorCode.PRODUCT_NOT_AVAILABLE);
            }
            q.addDetail(RequestQuoteDetail.builder()
                    .productUnitId(item.productUnitId())
                    .quantity(item.quantity())
                    .build());
        }
    }

    private RequestQuoteResponse toResponse(RequestQuote q) {
        Map<UUID, ProductUnit> units = loadUnits(
                q.getDetails().stream().map(RequestQuoteDetail::getProductUnitId).toList());
        return mapper.toResponse(q, new QuoteLookup(units, loadProducts(units)));
    }

    private Map<UUID, ProductUnit> loadUnits(List<UUID> unitIds) {
        return productUnitRepository.findAllById(unitIds).stream()
                .collect(Collectors.toMap(ProductUnit::getUnitId, Function.identity()));
    }

    private Map<UUID, Product> loadProducts(Map<UUID, ProductUnit> units) {
        List<UUID> productIds = units.values().stream().map(ProductUnit::getProductId).distinct().toList();
        return productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(Product::getProductId, Function.identity()));
    }

    private Specification<RequestQuote> spec(UUID customerId, RequestQuoteStatus status) {
        return (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (customerId != null) ps.add(cb.equal(root.get("customer").get("id"), customerId));
            if (status != null) ps.add(cb.equal(root.get("status"), status));
            return cb.and(ps.toArray(new Predicate[0]));
        };
    }

    private Customer findCustomerByUserId(UUID userId) {
        return customerRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(RequestQuoteErrorCode.CUSTOMER_NOT_FOUND));
    }

    private RequestQuote findOrThrow(UUID id) {
        return repository.findWithDetailsById(id)
                .orElseThrow(() -> new AppException(RequestQuoteErrorCode.REQUEST_QUOTE_NOT_FOUND));
    }

    private RequestQuote findOwned(UUID userId, UUID id) {
        Customer customer = findCustomerByUserId(userId);
        RequestQuote q = findOrThrow(id);
        if (!q.getCustomer().getId().equals(customer.getId())) {
            throw new AppException(RequestQuoteErrorCode.REQUEST_QUOTE_NOT_FOUND);   // không lộ sự tồn tại
        }
        return q;
    }
}
