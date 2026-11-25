package com.manhnv.capstonebakerai.shipment.service;

import com.manhnv.capstonebakerai.common.exception.AppException;
import com.manhnv.capstonebakerai.order.entity.Order;
import com.manhnv.capstonebakerai.order.enums.OrderStatus;
import com.manhnv.capstonebakerai.order.service.OrderService;
import com.manhnv.capstonebakerai.shipment.dto.request.ShipmentWebhookRequest;
import com.manhnv.capstonebakerai.shipment.entity.Shipment;
import com.manhnv.capstonebakerai.shipment.enums.ShipmentStatus;
import com.manhnv.capstonebakerai.shipment.exception.ShipmentErrorCode;
import com.manhnv.capstonebakerai.shipment.repository.ShipmentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShipmentWebhookServiceImplTest {

    @Mock
    ShipmentRepository shipmentRepository;
    @Mock
    OrderService orderService;
    @InjectMocks
    ShipmentWebhookServiceImpl service;

    private static final UUID ORDER_ID = UUID.randomUUID();
    private static final String EXTERNAL_ID = "EXT-1";

    private Shipment shipmentWith(ShipmentStatus status) {
        Order order = Order.builder().id(ORDER_ID).build();
        Shipment shipment = Shipment.builder()
                .order(order)
                .externalId(EXTERNAL_ID)
                .status(status)
                .build();
        when(shipmentRepository.findByExternalId(EXTERNAL_ID)).thenReturn(Optional.of(shipment));
        return shipment;
    }

    // UTCID01
    @Test
    void handle_shipmentNotFound_shouldThrowShipmentNotFound() {
        when(shipmentRepository.findByExternalId(EXTERNAL_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.handle(new ShipmentWebhookRequest(EXTERNAL_ID, 903)))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ShipmentErrorCode.SHIPMENT_NOT_FOUND);

        verify(shipmentRepository, never()).save(any(Shipment.class));
        verifyNoInteractions(orderService);
    }

    // UTCID02
    @Test
    void handle_unknownStatusCode_shouldIgnore() {
        Shipment shipment = shipmentWith(ShipmentStatus.PICKING);

        service.handle(new ShipmentWebhookRequest(EXTERNAL_ID, 999));

        assertThat(shipment.getStatus()).isEqualTo(ShipmentStatus.PICKING);
        verify(shipmentRepository, never()).save(any(Shipment.class));
        verifyNoInteractions(orderService);
    }

    // UTCID03
    @Test
    void handle_sameStatus_shouldIgnore() {
        Shipment shipment = shipmentWith(ShipmentStatus.IN_TRANSIT);

        service.handle(new ShipmentWebhookRequest(EXTERNAL_ID, 903));

        assertThat(shipment.getStatus()).isEqualTo(ShipmentStatus.IN_TRANSIT);
        assertThat(shipment.getShippedAt()).isNull();
        verify(shipmentRepository, never()).save(any(Shipment.class));
        verifyNoInteractions(orderService);
    }

    // UTCID04
    @Test
    void handle_picking_shouldSaveAndSyncReadyToShip() {
        Shipment shipment = shipmentWith(ShipmentStatus.PENDING);

        service.handle(new ShipmentWebhookRequest(EXTERNAL_ID, 902));

        assertThat(shipment.getStatus()).isEqualTo(ShipmentStatus.PICKING);
        assertThat(shipment.getShippedAt()).isNull();
        assertThat(shipment.getDeliveredAt()).isNull();
        verify(shipmentRepository).save(shipment);
        verify(orderService).syncStatusFromShipment(ORDER_ID, OrderStatus.READY_TO_SHIP);
    }

    // UTCID05
    @Test
    void handle_inTransit_shouldSetShippedAtAndSyncShipping() {
        Shipment shipment = shipmentWith(ShipmentStatus.PICKING);

        service.handle(new ShipmentWebhookRequest(EXTERNAL_ID, 903));

        assertThat(shipment.getStatus()).isEqualTo(ShipmentStatus.IN_TRANSIT);
        assertThat(shipment.getShippedAt()).isNotNull();
        assertThat(shipment.getDeliveredAt()).isNull();
        verify(shipmentRepository).save(shipment);
        verify(orderService).syncStatusFromShipment(ORDER_ID, OrderStatus.SHIPPING);
    }

    // UTCID06
    @Test
    void handle_delivered_shouldSetDeliveredAtAndSyncDelivered() {
        Shipment shipment = shipmentWith(ShipmentStatus.IN_TRANSIT);

        service.handle(new ShipmentWebhookRequest(EXTERNAL_ID, 905));

        assertThat(shipment.getStatus()).isEqualTo(ShipmentStatus.DELIVERED);
        assertThat(shipment.getDeliveredAt()).isNotNull();
        verify(shipmentRepository).save(shipment);
        verify(orderService).syncStatusFromShipment(ORDER_ID, OrderStatus.DELIVERED);
    }

    // UTCID07
    @Test
    void handle_failed_shouldSaveWithoutSyncingOrder() {
        Shipment shipment = shipmentWith(ShipmentStatus.IN_TRANSIT);

        service.handle(new ShipmentWebhookRequest(EXTERNAL_ID, 906));

        assertThat(shipment.getStatus()).isEqualTo(ShipmentStatus.FAILED);
        verify(shipmentRepository).save(shipment);
        verifyNoInteractions(orderService);
    }

    // UTCID08
    @Test
    void handle_returned_shouldSaveWithoutSyncingOrder() {
        Shipment shipment = shipmentWith(ShipmentStatus.FAILED);

        service.handle(new ShipmentWebhookRequest(EXTERNAL_ID, 907));

        assertThat(shipment.getStatus()).isEqualTo(ShipmentStatus.RETURNED);
        verify(shipmentRepository).save(shipment);
        verifyNoInteractions(orderService);
    }

    // UTCID09
    @Test
    void handle_cancelled_shouldSaveWithoutSyncingOrder() {
        Shipment shipment = shipmentWith(ShipmentStatus.PICKING);

        service.handle(new ShipmentWebhookRequest(EXTERNAL_ID, 914));

        assertThat(shipment.getStatus()).isEqualTo(ShipmentStatus.CANCELLED);
        verify(shipmentRepository).save(shipment);
        verifyNoInteractions(orderService);
    }

    // UTCID10
    @Test
    void handle_pendingCodeWhileInTransit_shouldRegressToPendingWithoutSyncingOrder() {
        Shipment shipment = shipmentWith(ShipmentStatus.IN_TRANSIT);

        service.handle(new ShipmentWebhookRequest(EXTERNAL_ID, 900));

        assertThat(shipment.getStatus()).isEqualTo(ShipmentStatus.PENDING);
        verify(shipmentRepository).save(shipment);
        verifyNoInteractions(orderService);
    }
}
