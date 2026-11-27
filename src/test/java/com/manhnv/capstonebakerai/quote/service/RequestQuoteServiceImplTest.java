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
import com.manhnv.capstonebakerai.user.entity.User;
import com.manhnv.capstonebakerai.user.service.UserSummaryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RequestQuoteServiceImplTest {

    @Mock
    RequestQuoteRepository repository;
    @Mock
    CustomerRepository customerRepository;
    @Mock
    ProductRepository productRepository;
    @Mock
    ProductUnitRepository productUnitRepository;
    @Mock
    RequestQuoteMapper mapper;
    @Mock
    ApplicationEventPublisher eventPublisher;
    @Mock
    UserSummaryService userSummaryService;
    @InjectMocks
    RequestQuoteServiceImpl service;

    @Captor
    ArgumentCaptor<RequestQuote> quoteCaptor;
    @Captor ArgumentCaptor<RequestQuoteCreatedEvent> createdEventCaptor;
    @Captor ArgumentCaptor<RequestQuoteStatusChangedEvent> statusEventCaptor;

    private final UUID userId = UUID.randomUUID();
    private final UUID quoteId = UUID.randomUUID();
    private final UUID productId = UUID.randomUUID();
    private final UUID unitId = UUID.randomUUID();
    private final UUID customerId = UUID.randomUUID();
    private final UUID staffId = UUID.randomUUID();

    private Customer customer;
    private Product product;
    private ProductUnit unit;

    @BeforeEach
    void setUp() {
        customer = mock(Customer.class);
        product = mock(Product.class);
        unit = mock(ProductUnit.class);
    }

    // ===== Helpers dùng chung (stub theo nhu cầu để không dính UnnecessaryStubbingException) =====

    private void stubCustomerFound() {
        when(customerRepository.findByUserId(userId)).thenReturn(Optional.of(customer));
    }

    /** Unit + product tồn tại; trạng thái ACTIVE/isSale do từng test tự stub. */
    private void stubUnitAndProductExist() {
        when(unit.getUnitId()).thenReturn(unitId);
        when(unit.getProductId()).thenReturn(productId);
        when(product.getProductId()).thenReturn(productId);
        when(productUnitRepository.findAllById(List.of(unitId))).thenReturn(List.of(unit));
        when(productRepository.findAllById(List.of(productId))).thenReturn(List.of(product));
    }

    private RequestQuoteRequest validRequest() {
        return new RequestQuoteRequest("0901234567", "Nguyen Van A", "Can bao gia banh sinh nhat",
                List.of(new RequestQuoteItemRequest(unitId, 2)));
    }

    private RequestQuote quoteOf(Customer owner, RequestQuoteStatus status) {
        return RequestQuote.builder()
                .id(quoteId)
                .customer(owner)
                .status(status)
                .contactPhone("0900000000")
                .contactName("Old Name")
                .content("old content")
                .build();
    }

    /** Customer tồn tại + quote thuộc về customer đó (dùng cho findOwned). */
    private void stubOwnedQuote(RequestQuote q) {
        stubCustomerFound();
        when(customer.getId()).thenReturn(customerId);
        when(repository.findWithDetailsById(quoteId)).thenReturn(Optional.of(q));
    }
    /** publishStatusChanged gọi q.getCustomer().getUser().getUserId() nên phải dựng chuỗi này. */
    private void stubCustomerUser() {
        User user = mock(User.class);
        when(customer.getUser()).thenReturn(user);
        when(user.getUserId()).thenReturn(userId);
    }

    // Create
    // UTCID01
    @org.testng.annotations.Test
    void create_validRequest_shouldSavePendingQuoteAndPublishEvent() {
        RequestQuoteResponse response = mock(RequestQuoteResponse.class);
        stubCustomerFound();
        stubUnitAndProductExist();
        when(product.getStatus()).thenReturn(ProductStatus.ACTIVE);
        when(unit.getStatus()).thenReturn(UnitStatus.ACTIVE);
        when(unit.isSale()).thenReturn(true);
        when(repository.save(any(RequestQuote.class))).thenAnswer(inv -> {
            RequestQuote q = inv.getArgument(0);
            q.setId(quoteId);
            return q;
        });
        when(mapper.toResponse(any(RequestQuote.class), any(QuoteLookup.class))).thenReturn(response);

        RequestQuoteResponse result = service.create(userId, validRequest());

        assertThat(result).isNotNull();
        verify(repository).save(quoteCaptor.capture());
        RequestQuote saved = quoteCaptor.getValue();
        assertThat(saved.getStatus()).isEqualTo(RequestQuoteStatus.PENDING);
        assertThat(saved.getCustomer()).isSameAs(customer);
        assertThat(saved.getContactPhone()).isEqualTo("0901234567");
        assertThat(saved.getDetails()).hasSize(1);
        assertThat(saved.getDetails().get(0).getProductUnitId()).isEqualTo(unitId);
        assertThat(saved.getDetails().get(0).getQuantity()).isEqualTo(2);

        verify(eventPublisher).publishEvent(createdEventCaptor.capture());
        assertThat(createdEventCaptor.getValue()).usingRecursiveComparison()
                .isEqualTo(new RequestQuoteCreatedEvent(quoteId, "Nguyen Van A", "0901234567"));
    }

    // UTCID02
    @Test
    void create_customerNotFound_shouldThrowCustomerNotFound() {
        when(customerRepository.findByUserId(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(userId, validRequest()))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(RequestQuoteErrorCode.CUSTOMER_NOT_FOUND);

        verify(repository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    // UTCID03
    @Test
    void create_duplicateItems_shouldThrowDuplicateItem() {
        RequestQuoteRequest request = new RequestQuoteRequest("0901234567", "Nguyen Van A", "note",
                List.of(new RequestQuoteItemRequest(unitId, 1), new RequestQuoteItemRequest(unitId, 3)));
        stubCustomerFound();

        assertThatThrownBy(() -> service.create(userId, request))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(RequestQuoteErrorCode.DUPLICATE_ITEM);

        verifyNoInteractions(productUnitRepository);
        verify(repository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    // UTCID04
    @Test
    void create_unitNotFound_shouldThrowProductUnitNotFound() {
        stubCustomerFound();
        when(productUnitRepository.findAllById(List.of(unitId))).thenReturn(List.of());

        assertThatThrownBy(() -> service.create(userId, validRequest()))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(RequestQuoteErrorCode.PRODUCT_UNIT_NOT_FOUND);

        verify(repository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    // UTCID05
    @Test
    void create_productNotActive_shouldThrowProductNotAvailable() {
        stubCustomerFound();
        stubUnitAndProductExist();
        when(product.getStatus()).thenReturn(ProductStatus.INACTIVE);

        assertThatThrownBy(() -> service.create(userId, validRequest()))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(RequestQuoteErrorCode.PRODUCT_NOT_AVAILABLE);

        verify(repository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    // UTCID06
    @Test
    void create_unitNotActive_shouldThrowProductNotAvailable() {
        stubCustomerFound();
        stubUnitAndProductExist();
        when(product.getStatus()).thenReturn(ProductStatus.ACTIVE);
        when(unit.getStatus()).thenReturn(UnitStatus.INACTIVE);

        assertThatThrownBy(() -> service.create(userId, validRequest()))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(RequestQuoteErrorCode.PRODUCT_NOT_AVAILABLE);

        verify(repository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    // UTCID07
    @Test
    void create_unitNotForSale_shouldThrowProductNotAvailable() {
        stubCustomerFound();
        stubUnitAndProductExist();
        when(product.getStatus()).thenReturn(ProductStatus.ACTIVE);
        when(unit.getStatus()).thenReturn(UnitStatus.ACTIVE);
        when(unit.isSale()).thenReturn(false);

        assertThatThrownBy(() -> service.create(userId, validRequest()))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(RequestQuoteErrorCode.PRODUCT_NOT_AVAILABLE);

        verify(repository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    // UTCID01
    @Test
    void getMineList_statusNull_shouldReturnAllMyQuotes() {
        Pageable pageable = PageRequest.of(0, 10);
        RequestQuote q = quoteOf(customer, RequestQuoteStatus.PENDING);
        RequestQuoteSummaryResponse summary = mock(RequestQuoteSummaryResponse.class);
        stubCustomerFound();
        when(repository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(q), pageable, 1));
        when(mapper.toSummary(q)).thenReturn(summary);

        Page<RequestQuoteSummaryResponse> result = service.getMine(userId, null, pageable);

        assertThat(result.getContent()).containsExactly(summary);
        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(repository).findAll(any(Specification.class), eq(pageable));
    }

    // UTCID02
    @Test
    void getMineList_withStatus_shouldReturnFilteredQuotes() {
        Pageable pageable = PageRequest.of(0, 10);
        RequestQuote q = quoteOf(customer, RequestQuoteStatus.PENDING);
        RequestQuoteSummaryResponse summary = mock(RequestQuoteSummaryResponse.class);
        stubCustomerFound();
        when(repository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(q), pageable, 1));
        when(mapper.toSummary(q)).thenReturn(summary);

        Page<RequestQuoteSummaryResponse> result = service.getMine(userId, RequestQuoteStatus.PENDING, pageable);

        assertThat(result.getContent()).containsExactly(summary);
        verify(repository).findAll(any(Specification.class), eq(pageable));
    }

    // UTCID03
    @Test
    void getMineList_customerNotFound_shouldThrowCustomerNotFound() {
        Pageable pageable = PageRequest.of(0, 10);
        when(customerRepository.findByUserId(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getMine(userId, null, pageable))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(RequestQuoteErrorCode.CUSTOMER_NOT_FOUND);

        verifyNoInteractions(repository);
    }

    // Get mine detail
    // UTCID01
    @Test
    void getMineDetail_ownedQuote_shouldReturnResponse() {
        RequestQuote q = quoteOf(customer, RequestQuoteStatus.PENDING);
        RequestQuoteResponse response = mock(RequestQuoteResponse.class);
        stubOwnedQuote(q);
        when(mapper.toResponse(eq(q), any(QuoteLookup.class))).thenReturn(response);

        RequestQuoteResponse result = service.getMine(userId, quoteId);

        assertThat(result).isNotNull();
    }

    // UTCID02
    @Test
    void getMineDetail_customerNotFound_shouldThrowCustomerNotFound() {
        when(customerRepository.findByUserId(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getMine(userId, quoteId))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(RequestQuoteErrorCode.CUSTOMER_NOT_FOUND);

        verifyNoInteractions(repository);
    }

    // UTCID03
    @Test
    void getMineDetail_quoteNotFound_shouldThrowQuoteNotFound() {
        stubCustomerFound();
        when(repository.findWithDetailsById(quoteId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getMine(userId, quoteId))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(RequestQuoteErrorCode.REQUEST_QUOTE_NOT_FOUND);
    }

    // UTCID04
    @Test
    void getMineDetail_quoteOfAnotherCustomer_shouldThrowQuoteNotFound() {
        Customer other = mock(Customer.class);
        when(other.getId()).thenReturn(UUID.randomUUID());
        RequestQuote q = quoteOf(other, RequestQuoteStatus.PENDING);
        stubOwnedQuote(q);

        assertThatThrownBy(() -> service.getMine(userId, quoteId))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(RequestQuoteErrorCode.REQUEST_QUOTE_NOT_FOUND);

        verifyNoInteractions(mapper);
    }

    // Get for staff
    // UTCID01
    @Test
    void getForStaff_quoteExists_shouldReturnResponse() {
        RequestQuote q = quoteOf(customer, RequestQuoteStatus.PENDING);
        RequestQuoteResponse response = mock(RequestQuoteResponse.class);
        when(repository.findWithDetailsById(quoteId)).thenReturn(Optional.of(q));
        when(mapper.toResponse(eq(q), any(QuoteLookup.class))).thenReturn(response);

        RequestQuoteResponse result = service.getForStaff(quoteId);

        assertThat(result).isNotNull();
        verifyNoInteractions(customerRepository);
    }

    // UTCID02
    @Test
    void getForStaff_quoteNotFound_shouldThrowQuoteNotFound() {
        when(repository.findWithDetailsById(quoteId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getForStaff(quoteId))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(RequestQuoteErrorCode.REQUEST_QUOTE_NOT_FOUND);

        verifyNoInteractions(mapper);
    }

    // update mine
    // UTCID01
    @Test
    void updateMine_pendingQuote_shouldUpdateFieldsAndReplaceDetails() {
        UUID oldUnitId = UUID.randomUUID();
        RequestQuote q = quoteOf(customer, RequestQuoteStatus.PENDING);
        q.addDetail(RequestQuoteDetail.builder().productUnitId(oldUnitId).quantity(5).build());
        RequestQuoteResponse response = mock(RequestQuoteResponse.class);
        stubOwnedQuote(q);
        stubUnitAndProductExist();
        when(product.getStatus()).thenReturn(ProductStatus.ACTIVE);
        when(unit.getStatus()).thenReturn(UnitStatus.ACTIVE);
        when(unit.isSale()).thenReturn(true);
        when(repository.save(any(RequestQuote.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toResponse(any(RequestQuote.class), any(QuoteLookup.class))).thenReturn(response);

        RequestQuoteResponse result = service.updateMine(userId, quoteId, validRequest());

        assertThat(result).isNotNull();
        verify(repository).save(quoteCaptor.capture());
        RequestQuote saved = quoteCaptor.getValue();
        assertThat(saved.getContactPhone()).isEqualTo("0901234567");
        assertThat(saved.getContactName()).isEqualTo("Nguyen Van A");
        assertThat(saved.getContent()).isEqualTo("Can bao gia banh sinh nhat");
        assertThat(saved.getStatus()).isEqualTo(RequestQuoteStatus.PENDING);
        assertThat(saved.getDetails()).hasSize(1);
        assertThat(saved.getDetails().get(0).getProductUnitId()).isEqualTo(unitId);
        assertThat(saved.getDetails().get(0).getQuantity()).isEqualTo(2);
    }

    // UTCID02
    @Test
    void updateMine_inProgressQuote_shouldThrowCannotEdit() {
        RequestQuote q = quoteOf(customer, RequestQuoteStatus.IN_PROGRESS);
        stubOwnedQuote(q);

        assertThatThrownBy(() -> service.updateMine(userId, quoteId, validRequest()))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(RequestQuoteErrorCode.REQUEST_QUOTE_CANNOT_EDIT);

        verify(repository, never()).save(any());
        verifyNoInteractions(productUnitRepository);
        assertThat(q.getContactName()).isEqualTo("Old Name");   // dữ liệu không bị đổi
    }

    // UTCID03
    @Test
    void updateMine_quoteOfAnotherCustomer_shouldThrowQuoteNotFound() {
        Customer other = mock(Customer.class);
        when(other.getId()).thenReturn(UUID.randomUUID());
        RequestQuote q = quoteOf(other, RequestQuoteStatus.PENDING);
        stubOwnedQuote(q);

        assertThatThrownBy(() -> service.updateMine(userId, quoteId, validRequest()))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(RequestQuoteErrorCode.REQUEST_QUOTE_NOT_FOUND);

        verify(repository, never()).save(any());
    }

    // UTCID04
    @Test
    void updateMine_duplicateItems_shouldThrowDuplicateItem() {
        RequestQuote q = quoteOf(customer, RequestQuoteStatus.PENDING);
        RequestQuoteRequest request = new RequestQuoteRequest("0901234567", "Nguyen Van A", "note",
                List.of(new RequestQuoteItemRequest(unitId, 1), new RequestQuoteItemRequest(unitId, 3)));
        stubOwnedQuote(q);

        assertThatThrownBy(() -> service.updateMine(userId, quoteId, request))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(RequestQuoteErrorCode.DUPLICATE_ITEM);

        verify(repository, never()).save(any());
    }

    // Cancel mine
    // UTCID01
    @Test
    void cancelMine_pendingQuote_shouldCancelAndPublishEventByCustomer() {
        RequestQuote q = quoteOf(customer, RequestQuoteStatus.PENDING);
        RequestQuoteResponse response = mock(RequestQuoteResponse.class);
        stubOwnedQuote(q);
        stubCustomerUser();
        when(repository.save(any(RequestQuote.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toResponse(any(RequestQuote.class), any(QuoteLookup.class))).thenReturn(response);

        RequestQuoteResponse result = service.cancelMine(userId, quoteId);

        assertThat(result).isNotNull();
        verify(repository).save(quoteCaptor.capture());
        assertThat(quoteCaptor.getValue().getStatus()).isEqualTo(RequestQuoteStatus.CANCELLED);

        verify(eventPublisher).publishEvent(statusEventCaptor.capture());
        assertThat(statusEventCaptor.getValue()).usingRecursiveComparison()
                .isEqualTo(new RequestQuoteStatusChangedEvent(
                        quoteId, userId, RequestQuoteStatus.CANCELLED, null, null, true));
    }

    // UTCID02
    @Test
    void cancelMine_inProgressQuote_shouldCancelAndKeepHandledBy() {
        RequestQuote q = quoteOf(customer, RequestQuoteStatus.IN_PROGRESS);
        q.setHandledBy(staffId);
        RequestQuoteResponse response = mock(RequestQuoteResponse.class);
        stubOwnedQuote(q);
        stubCustomerUser();
        when(repository.save(any(RequestQuote.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toResponse(any(RequestQuote.class), any(QuoteLookup.class))).thenReturn(response);

        RequestQuoteResponse result = service.cancelMine(userId, quoteId);

        assertThat(result).isNotNull();
        verify(repository).save(quoteCaptor.capture());
        assertThat(quoteCaptor.getValue().getStatus()).isEqualTo(RequestQuoteStatus.CANCELLED);
        assertThat(quoteCaptor.getValue().getHandledBy()).isEqualTo(staffId);

        verify(eventPublisher).publishEvent(statusEventCaptor.capture());
        assertThat(statusEventCaptor.getValue()).usingRecursiveComparison()
                .isEqualTo(new RequestQuoteStatusChangedEvent(
                        quoteId, userId, RequestQuoteStatus.CANCELLED, staffId, null, true));
    }

    // UTCID03
    @Test
    void cancelMine_orderedQuote_shouldThrowCannotCancel() {
        RequestQuote q = quoteOf(customer, RequestQuoteStatus.ORDERED);
        stubOwnedQuote(q);

        assertThatThrownBy(() -> service.cancelMine(userId, quoteId))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(RequestQuoteErrorCode.REQUEST_QUOTE_CANNOT_CANCEL);

        assertThat(q.getStatus()).isEqualTo(RequestQuoteStatus.ORDERED);   // trạng thái không đổi
        verify(repository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    // UTCID04
    @Test
    void cancelMine_quoteOfAnotherCustomer_shouldThrowQuoteNotFound() {
        Customer other = mock(Customer.class);
        when(other.getId()).thenReturn(UUID.randomUUID());
        RequestQuote q = quoteOf(other, RequestQuoteStatus.PENDING);
        stubOwnedQuote(q);

        assertThatThrownBy(() -> service.cancelMine(userId, quoteId))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(RequestQuoteErrorCode.REQUEST_QUOTE_NOT_FOUND);

        verify(repository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    // Get all
    // UTCID04
    @Test
    void getAll_emptyPage_shouldReturnEmptyPage() {
        Pageable pageable = PageRequest.of(0, 10);
        when(repository.findAll(any(Specification.class), eq(pageable))).thenReturn(Page.empty(pageable));

        Page<RequestQuoteSummaryResponse> result = service.getAll(null, pageable);

        assertThat(result.getContent()).isEmpty();
        verify(userSummaryService).getSummaries(List.of());
        verifyNoInteractions(mapper);
    }

    // Claim
    // UTCID01
    @Test
    void claim_pendingQuote_shouldReloadAndPublishEvent() {
        RequestQuote q = quoteOf(customer, RequestQuoteStatus.IN_PROGRESS);
        q.setHandledBy(staffId);
        RequestQuoteResponse response = mock(RequestQuoteResponse.class);
        stubCustomerUser();
        when(repository.claim(eq(quoteId), eq(RequestQuoteStatus.PENDING),
                eq(RequestQuoteStatus.IN_PROGRESS), eq(staffId), any(LocalDateTime.class))).thenReturn(1);
        when(repository.findWithDetailsById(quoteId)).thenReturn(Optional.of(q));
        when(mapper.toResponse(eq(q), any(QuoteLookup.class))).thenReturn(response);

        RequestQuoteResponse result = service.claim(quoteId, staffId);

        assertThat(result).isNotNull();
        verify(repository, never()).existsById(any());
        verify(eventPublisher).publishEvent(statusEventCaptor.capture());
        assertThat(statusEventCaptor.getValue()).usingRecursiveComparison()
                .isEqualTo(new RequestQuoteStatusChangedEvent(
                        quoteId, userId, RequestQuoteStatus.IN_PROGRESS, staffId, null, false));
    }

    // UTCID02
    @Test
    void claim_quoteNotFound_shouldThrowQuoteNotFound() {
        when(repository.claim(eq(quoteId), eq(RequestQuoteStatus.PENDING),
                eq(RequestQuoteStatus.IN_PROGRESS), eq(staffId), any(LocalDateTime.class))).thenReturn(0);
        when(repository.existsById(quoteId)).thenReturn(false);

        assertThatThrownBy(() -> service.claim(quoteId, staffId))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(RequestQuoteErrorCode.REQUEST_QUOTE_NOT_FOUND);

        verify(repository, never()).findWithDetailsById(any());
        verifyNoInteractions(eventPublisher);
    }

    // UTCID03
    @Test
    void claim_alreadyHandled_shouldThrowAlreadyHandled() {
        when(repository.claim(eq(quoteId), eq(RequestQuoteStatus.PENDING),
                eq(RequestQuoteStatus.IN_PROGRESS), eq(staffId), any(LocalDateTime.class))).thenReturn(0);
        when(repository.existsById(quoteId)).thenReturn(true);

        assertThatThrownBy(() -> service.claim(quoteId, staffId))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(RequestQuoteErrorCode.REQUEST_QUOTE_ALREADY_HANDLED);

        verify(repository, never()).findWithDetailsById(any());
        verifyNoInteractions(eventPublisher);
    }

    // Mark ordered
    // UTCID01
    @Test
    void markOrdered_handlerWithNote_shouldOrderAndSaveNote() {
        RequestQuote q = quoteOf(customer, RequestQuoteStatus.IN_PROGRESS);
        q.setHandledBy(staffId);
        RequestQuoteResponse response = mock(RequestQuoteResponse.class);
        when(repository.findWithDetailsById(quoteId)).thenReturn(Optional.of(q));
        stubCustomerUser();
        when(repository.save(any(RequestQuote.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toResponse(any(RequestQuote.class), any(QuoteLookup.class))).thenReturn(response);

        RequestQuoteResponse result = service.markOrdered(quoteId, staffId, false,
                new MarkOrderedRequest("Da tao don OD-000001"));

        assertThat(result).isNotNull();
        verify(repository).save(quoteCaptor.capture());
        assertThat(quoteCaptor.getValue().getStatus()).isEqualTo(RequestQuoteStatus.ORDERED);
        assertThat(quoteCaptor.getValue().getResultNote()).isEqualTo("Da tao don OD-000001");

        verify(eventPublisher).publishEvent(statusEventCaptor.capture());
        assertThat(statusEventCaptor.getValue()).usingRecursiveComparison()
                .isEqualTo(new RequestQuoteStatusChangedEvent(
                        quoteId, userId, RequestQuoteStatus.ORDERED, staffId, null, false));
    }

    // UTCID02
    @Test
    void markOrdered_nullRequest_shouldOrderWithoutChangingNote() {
        RequestQuote q = quoteOf(customer, RequestQuoteStatus.IN_PROGRESS);
        q.setHandledBy(staffId);
        RequestQuoteResponse response = mock(RequestQuoteResponse.class);
        when(repository.findWithDetailsById(quoteId)).thenReturn(Optional.of(q));
        stubCustomerUser();
        when(repository.save(any(RequestQuote.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toResponse(any(RequestQuote.class), any(QuoteLookup.class))).thenReturn(response);

        RequestQuoteResponse result = service.markOrdered(quoteId, staffId, false, null);

        assertThat(result).isNotNull();
        verify(repository).save(quoteCaptor.capture());
        assertThat(quoteCaptor.getValue().getStatus()).isEqualTo(RequestQuoteStatus.ORDERED);
        assertThat(quoteCaptor.getValue().getResultNote()).isNull();
        verify(eventPublisher).publishEvent(any(RequestQuoteStatusChangedEvent.class));
    }

    // UTCID03
    @Test
    void markOrdered_managerOnOtherStaffQuote_shouldOrderAndKeepHandledBy() {
        UUID managerId = UUID.randomUUID();
        RequestQuote q = quoteOf(customer, RequestQuoteStatus.IN_PROGRESS);
        q.setHandledBy(staffId);
        RequestQuoteResponse response = mock(RequestQuoteResponse.class);
        when(repository.findWithDetailsById(quoteId)).thenReturn(Optional.of(q));
        stubCustomerUser();
        when(repository.save(any(RequestQuote.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toResponse(any(RequestQuote.class), any(QuoteLookup.class))).thenReturn(response);

        RequestQuoteResponse result = service.markOrdered(quoteId, managerId, true,
                new MarkOrderedRequest("Manager xu ly"));

        assertThat(result).isNotNull();
        verify(repository).save(quoteCaptor.capture());
        assertThat(quoteCaptor.getValue().getStatus()).isEqualTo(RequestQuoteStatus.ORDERED);
        assertThat(quoteCaptor.getValue().getHandledBy()).isEqualTo(staffId);   // không bị ghi đè bằng managerId

        verify(eventPublisher).publishEvent(statusEventCaptor.capture());
        assertThat(statusEventCaptor.getValue().handledBy()).isEqualTo(staffId);
    }

    // UTCID04
    @Test
    void markOrdered_notHandlerAndNotManager_shouldThrowNotHandler() {
        UUID otherStaffId = UUID.randomUUID();
        RequestQuote q = quoteOf(customer, RequestQuoteStatus.IN_PROGRESS);
        q.setHandledBy(staffId);
        when(repository.findWithDetailsById(quoteId)).thenReturn(Optional.of(q));

        assertThatThrownBy(() -> service.markOrdered(quoteId, otherStaffId, false,
                new MarkOrderedRequest("note")))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(RequestQuoteErrorCode.REQUEST_QUOTE_NOT_HANDLER);

        assertThat(q.getStatus()).isEqualTo(RequestQuoteStatus.IN_PROGRESS);   // trạng thái không đổi
        verify(repository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    // UTCID05
    @Test
    void markOrdered_pendingQuote_shouldThrowInvalidTransition() {
        RequestQuote q = quoteOf(customer, RequestQuoteStatus.PENDING);
        when(repository.findWithDetailsById(quoteId)).thenReturn(Optional.of(q));

        assertThatThrownBy(() -> service.markOrdered(quoteId, staffId, false,
                new MarkOrderedRequest("note")))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(RequestQuoteErrorCode.REQUEST_QUOTE_INVALID_STATUS_TRANSITION);

        assertThat(q.getStatus()).isEqualTo(RequestQuoteStatus.PENDING);
        verify(repository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    // UTCID06
    @Test
    void markOrdered_quoteNotFound_shouldThrowQuoteNotFound() {
        when(repository.findWithDetailsById(quoteId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.markOrdered(quoteId, staffId, false,
                new MarkOrderedRequest("note")))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(RequestQuoteErrorCode.REQUEST_QUOTE_NOT_FOUND);

        verify(repository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    // Reject
    // UTCID01
    @Test
    void reject_inProgressByHandler_shouldRejectAndKeepHandler() {
        LocalDateTime claimedAt = LocalDateTime.of(2026, 11, 20, 9, 0);
        RequestQuote q = quoteOf(customer, RequestQuoteStatus.IN_PROGRESS);
        q.setHandledBy(staffId);
        q.setHandledAt(claimedAt);
        RequestQuoteResponse response = mock(RequestQuoteResponse.class);
        when(repository.findWithDetailsById(quoteId)).thenReturn(Optional.of(q));
        stubCustomerUser();
        when(repository.save(any(RequestQuote.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toResponse(any(RequestQuote.class), any(QuoteLookup.class))).thenReturn(response);

        RequestQuoteResponse result = service.reject(quoteId, staffId, false,
                new RejectRequestQuoteRequest("Het nguyen lieu"));

        assertThat(result).isNotNull();
        verify(repository).save(quoteCaptor.capture());
        RequestQuote saved = quoteCaptor.getValue();
        assertThat(saved.getStatus()).isEqualTo(RequestQuoteStatus.REJECTED);
        assertThat(saved.getRejectReason()).isEqualTo("Het nguyen lieu");
        assertThat(saved.getHandledBy()).isEqualTo(staffId);
        assertThat(saved.getHandledAt()).isEqualTo(claimedAt);   // không bị ghi đè

        verify(eventPublisher).publishEvent(statusEventCaptor.capture());
        assertThat(statusEventCaptor.getValue()).usingRecursiveComparison()
                .isEqualTo(new RequestQuoteStatusChangedEvent(
                        quoteId, userId, RequestQuoteStatus.REJECTED, staffId, "Het nguyen lieu", false));
    }

    // UTCID02
    @Test
    void reject_pendingQuote_shouldRejectAndSetHandler() {
        RequestQuote q = quoteOf(customer, RequestQuoteStatus.PENDING);
        RequestQuoteResponse response = mock(RequestQuoteResponse.class);
        when(repository.findWithDetailsById(quoteId)).thenReturn(Optional.of(q));
        stubCustomerUser();
        when(repository.save(any(RequestQuote.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toResponse(any(RequestQuote.class), any(QuoteLookup.class))).thenReturn(response);

        RequestQuoteResponse result = service.reject(quoteId, staffId, false,
                new RejectRequestQuoteRequest("Ngoai pham vi phuc vu"));

        assertThat(result).isNotNull();
        verify(repository).save(quoteCaptor.capture());
        RequestQuote saved = quoteCaptor.getValue();
        assertThat(saved.getStatus()).isEqualTo(RequestQuoteStatus.REJECTED);
        assertThat(saved.getHandledBy()).isEqualTo(staffId);
        assertThat(saved.getHandledAt()).isNotNull();
        assertThat(saved.getRejectReason()).isEqualTo("Ngoai pham vi phuc vu");

        verify(eventPublisher).publishEvent(statusEventCaptor.capture());
        assertThat(statusEventCaptor.getValue()).usingRecursiveComparison()
                .isEqualTo(new RequestQuoteStatusChangedEvent(
                        quoteId, userId, RequestQuoteStatus.REJECTED, staffId, "Ngoai pham vi phuc vu", false));
    }

    // UTCID03
    @Test
    void reject_managerOnOtherStaffQuote_shouldRejectAndKeepHandledBy() {
        UUID managerId = UUID.randomUUID();
        RequestQuote q = quoteOf(customer, RequestQuoteStatus.IN_PROGRESS);
        q.setHandledBy(staffId);
        q.setHandledAt(LocalDateTime.of(2026, 11, 20, 9, 0));
        RequestQuoteResponse response = mock(RequestQuoteResponse.class);
        when(repository.findWithDetailsById(quoteId)).thenReturn(Optional.of(q));
        stubCustomerUser();
        when(repository.save(any(RequestQuote.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toResponse(any(RequestQuote.class), any(QuoteLookup.class))).thenReturn(response);

        RequestQuoteResponse result = service.reject(quoteId, managerId, true,
                new RejectRequestQuoteRequest("Manager tu choi"));

        assertThat(result).isNotNull();
        verify(repository).save(quoteCaptor.capture());
        assertThat(quoteCaptor.getValue().getStatus()).isEqualTo(RequestQuoteStatus.REJECTED);
        assertThat(quoteCaptor.getValue().getHandledBy()).isEqualTo(staffId);   // không bị ghi đè bằng managerId
        assertThat(quoteCaptor.getValue().getRejectReason()).isEqualTo("Manager tu choi");
    }

    // UTCID04
    @Test
    void reject_notHandlerAndNotManager_shouldThrowNotHandler() {
        UUID otherStaffId = UUID.randomUUID();
        RequestQuote q = quoteOf(customer, RequestQuoteStatus.IN_PROGRESS);
        q.setHandledBy(staffId);
        when(repository.findWithDetailsById(quoteId)).thenReturn(Optional.of(q));

        assertThatThrownBy(() -> service.reject(quoteId, otherStaffId, false,
                new RejectRequestQuoteRequest("reason")))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(RequestQuoteErrorCode.REQUEST_QUOTE_NOT_HANDLER);

        assertThat(q.getStatus()).isEqualTo(RequestQuoteStatus.IN_PROGRESS);
        verify(repository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    // UTCID05
    @Test
    void reject_orderedQuote_shouldThrowInvalidTransition() {
        RequestQuote q = quoteOf(customer, RequestQuoteStatus.ORDERED);
        when(repository.findWithDetailsById(quoteId)).thenReturn(Optional.of(q));

        assertThatThrownBy(() -> service.reject(quoteId, staffId, false,
                new RejectRequestQuoteRequest("reason")))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(RequestQuoteErrorCode.REQUEST_QUOTE_INVALID_STATUS_TRANSITION);

        assertThat(q.getStatus()).isEqualTo(RequestQuoteStatus.ORDERED);
        verify(repository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    // UTCID06
    @Test
    void reject_quoteNotFound_shouldThrowQuoteNotFound() {
        when(repository.findWithDetailsById(quoteId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.reject(quoteId, staffId, false,
                new RejectRequestQuoteRequest("reason")))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(RequestQuoteErrorCode.REQUEST_QUOTE_NOT_FOUND);

        verify(repository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    // UTCID07
    @Test
    void reject_nullRequest_shouldThrowNullPointerException() {
        RequestQuote q = quoteOf(customer, RequestQuoteStatus.IN_PROGRESS);
        q.setHandledBy(staffId);
        when(repository.findWithDetailsById(quoteId)).thenReturn(Optional.of(q));

        assertThatThrownBy(() -> service.reject(quoteId, staffId, false, null))
                .isInstanceOf(NullPointerException.class);

        verify(repository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }
}
