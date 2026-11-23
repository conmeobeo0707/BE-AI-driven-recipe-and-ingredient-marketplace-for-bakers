package com.manhnv.capstonebakerai.shipment.service;

import com.manhnv.capstonebakerai.common.exception.AppException;
import com.manhnv.capstonebakerai.order.entity.Order;
import com.manhnv.capstonebakerai.order.entity.OrderItem;
import com.manhnv.capstonebakerai.order.enums.PaymentMethod;
import com.manhnv.capstonebakerai.shipment.dto.response.ShipmentResponse;
import com.manhnv.capstonebakerai.shipment.entity.Shipment;
import com.manhnv.capstonebakerai.shipment.enums.ShipmentStatus;
import com.manhnv.capstonebakerai.shipment.exception.ShipmentErrorCode;
import com.manhnv.capstonebakerai.shipment.mapper.ShipmentMapper;
import com.manhnv.capstonebakerai.shipment.provider.ShippingProvider;
import com.manhnv.capstonebakerai.shipment.repository.ShipmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShipmentServiceImplTest {

    @Mock
    ShipmentRepository shipmentRepository;
    @Mock
    ShippingProvider provider;
    @Mock
    ShipmentMapper mapper;
    @InjectMocks
    ShipmentServiceImpl service;

    @Captor
    ArgumentCaptor<Shipment> shipmentCaptor;

    private final UUID orderId = UUID.randomUUID();

    // Phương thức thanh toán khác COD, lấy từ values() để không phụ thuộc tên hằng
    private static final PaymentMethod NON_COD = Arrays.stream(PaymentMethod.values())
            .filter(m -> m != PaymentMethod.COD)
            .findFirst()
            .orElseThrow();

    private Order order;

    @BeforeEach
    void setUp() {
        order = mock(Order.class);
    }

    private OrderItem itemOf(int weightGram, int quantity) {
        OrderItem item = mock(OrderItem.class);
        when(item.getWeightGram()).thenReturn(weightGram);
        when(item.getQuantity()).thenReturn(quantity);
        return item;
    }

    /** Stub các getter của order mà createForOrder luôn dùng khi chưa có shipment. */
    private void stubOrder(PaymentMethod method, BigDecimal total, BigDecimal shippingFee, List<OrderItem> items) {
        when(order.getId()).thenReturn(orderId);
        when(order.getPaymentMethod()).thenReturn(method);
        lenient().when(order.getShippingFee()).thenReturn(shippingFee);
        when(order.getItems()).thenReturn(items);
        lenient().when(order.getTotal()).thenReturn(total);   // chỉ dùng khi COD
    }

    private Shipment shipmentOf(ShipmentStatus status) {
        return Shipment.builder().externalId("EXT-1").status(status).build();
    }

    private ShippingProvider.ShipmentCreateResult providerResult() {
        return new ShippingProvider.ShipmentCreateResult("EXT-1", "TRK-1", "GHN");
    }

    // Crete for order
    // UTCID01
    @Test
    void createForOrder_codOrder_shouldSaveShipmentWithCodAndWeight() {
        BigDecimal total = new BigDecimal("250000");
        BigDecimal shippingFee = new BigDecimal("30000");
        ShipmentResponse response = mock(ShipmentResponse.class);
        stubOrder(PaymentMethod.COD, total, shippingFee, List.of(itemOf(500, 2), itemOf(300, 1)));
        when(shipmentRepository.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(provider.createShipment(order, total, 1300)).thenReturn(providerResult());
        when(provider.name()).thenReturn("GOSHIP");
        when(shipmentRepository.save(any(Shipment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toResponse(any(Shipment.class))).thenReturn(response);

        ShipmentResponse result = service.createForOrder(order);

        assertThat(result).isSameAs(response);
        verify(shipmentRepository).save(shipmentCaptor.capture());
        Shipment saved = shipmentCaptor.getValue();
        assertThat(saved.getOrder()).isSameAs(order);
        assertThat(saved.getProvider()).isEqualTo("GOSHIP");
        assertThat(saved.getCarrier()).isEqualTo("GHN");
        assertThat(saved.getTrackingCode()).isEqualTo("TRK-1");
        assertThat(saved.getExternalId()).isEqualTo("EXT-1");
        assertThat(saved.getShippingFee()).isEqualByComparingTo("30000");
        assertThat(saved.getCodAmount()).isEqualByComparingTo("250000");
        assertThat(saved.getWeightGram()).isEqualTo(1300);
    }

    // UTCID02
    @Test
    void createForOrder_nonCodOrder_shouldUseZeroCod() {
        ShipmentResponse response = mock(ShipmentResponse.class);
        stubOrder(NON_COD, new BigDecimal("250000"), new BigDecimal("30000"), List.of(itemOf(500, 1)));
        when(shipmentRepository.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(provider.createShipment(order, BigDecimal.ZERO, 500)).thenReturn(providerResult());
        when(provider.name()).thenReturn("GOSHIP");
        when(shipmentRepository.save(any(Shipment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toResponse(any(Shipment.class))).thenReturn(response);

        ShipmentResponse result = service.createForOrder(order);

        assertThat(result).isSameAs(response);
        verify(shipmentRepository).save(shipmentCaptor.capture());
        assertThat(shipmentCaptor.getValue().getCodAmount()).isEqualByComparingTo("0");
        assertThat(shipmentCaptor.getValue().getWeightGram()).isEqualTo(500);
        verify(order, never()).getTotal();
    }

    // UTCID03
    @Test
    void createForOrder_noItems_shouldUseMinimumWeightOfOne() {
        ShipmentResponse response = mock(ShipmentResponse.class);
        stubOrder(NON_COD, new BigDecimal("0"), new BigDecimal("30000"), List.of());
        when(shipmentRepository.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(provider.createShipment(order, BigDecimal.ZERO, 1)).thenReturn(providerResult());
        when(provider.name()).thenReturn("GOSHIP");
        when(shipmentRepository.save(any(Shipment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toResponse(any(Shipment.class))).thenReturn(response);

        service.createForOrder(order);

        verify(shipmentRepository).save(shipmentCaptor.capture());
        assertThat(shipmentCaptor.getValue().getWeightGram()).isEqualTo(1);
    }

    // UTCID04
    @Test
    void createForOrder_shipmentExists_shouldReturnExistingWithoutCallingProvider() {
        Shipment existing = shipmentOf(ShipmentStatus.PENDING);
        ShipmentResponse response = mock(ShipmentResponse.class);
        when(order.getId()).thenReturn(orderId);
        when(shipmentRepository.findByOrderId(orderId)).thenReturn(Optional.of(existing));
        when(mapper.toResponse(existing)).thenReturn(response);

        ShipmentResponse result = service.createForOrder(order);

        assertThat(result).isSameAs(response);
        verifyNoInteractions(provider);
        verify(shipmentRepository, never()).save(any());
    }

    // UTCID05
    @Test
    void createForOrder_providerFails_shouldPropagateAndNotSave() {
        stubOrder(PaymentMethod.COD, new BigDecimal("250000"), new BigDecimal("30000"), List.of(itemOf(500, 1)));
        when(shipmentRepository.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(provider.createShipment(any(Order.class), any(BigDecimal.class), anyInt()))
                .thenThrow(new RuntimeException("provider error"));

        assertThatThrownBy(() -> service.createForOrder(order))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("provider error");

        verify(shipmentRepository, never()).save(any());
        verifyNoInteractions(mapper);
    }

    // Cancel for order
    // UTCID01
    @Test
    void cancelForOrder_pendingShipment_shouldCancel() {
        Shipment shipment = shipmentOf(ShipmentStatus.PENDING);
        when(shipmentRepository.findByOrderId(orderId)).thenReturn(Optional.of(shipment));

        service.cancelForOrder(orderId);

        verify(provider).cancelShipment("EXT-1");
        assertThat(shipment.getStatus()).isEqualTo(ShipmentStatus.CANCELLED);
        verify(shipmentRepository).save(shipment);
    }

    // UTCID02
    @Test
    void cancelForOrder_pickingShipment_shouldCancel() {
        Shipment shipment = shipmentOf(ShipmentStatus.PICKING);
        when(shipmentRepository.findByOrderId(orderId)).thenReturn(Optional.of(shipment));

        service.cancelForOrder(orderId);

        verify(provider).cancelShipment("EXT-1");
        assertThat(shipment.getStatus()).isEqualTo(ShipmentStatus.CANCELLED);
        verify(shipmentRepository).save(shipment);
    }

    // UTCID03
    @Test
    void cancelForOrder_inTransitShipment_shouldDoNothing() {
        Shipment shipment = shipmentOf(ShipmentStatus.IN_TRANSIT);
        when(shipmentRepository.findByOrderId(orderId)).thenReturn(Optional.of(shipment));

        service.cancelForOrder(orderId);

        verifyNoInteractions(provider);
        verify(shipmentRepository, never()).save(any());
        assertThat(shipment.getStatus()).isEqualTo(ShipmentStatus.IN_TRANSIT);
    }

    // UTCID04
    @Test
    void cancelForOrder_noShipment_shouldDoNothing() {
        when(shipmentRepository.findByOrderId(orderId)).thenReturn(Optional.empty());

        service.cancelForOrder(orderId);

        verifyNoInteractions(provider);
        verify(shipmentRepository, never()).save(any());
    }

    // Get by order id
    // UTCID01
    @Test
    void getByOrderId_shipmentExists_shouldReturnResponse() {
        Shipment shipment = shipmentOf(ShipmentStatus.PENDING);
        ShipmentResponse response = mock(ShipmentResponse.class);
        when(shipmentRepository.findByOrderId(orderId)).thenReturn(Optional.of(shipment));
        when(mapper.toResponse(shipment)).thenReturn(response);

        ShipmentResponse result = service.getByOrderId(orderId);

        assertThat(result).isSameAs(response);
    }

    // UTCID02
    @Test
    void getByOrderId_shipmentNotFound_shouldThrowNotFound() {
        when(shipmentRepository.findByOrderId(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getByOrderId(orderId))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ShipmentErrorCode.SHIPMENT_NOT_FOUND);

        verifyNoInteractions(mapper);
    }
}