package com.manhnv.capstonebakerai.warehouse.service;

import com.manhnv.capstonebakerai.common.exception.AppException;
import com.manhnv.capstonebakerai.product.dtos.InventoryUnit;
import com.manhnv.capstonebakerai.product.service.ProductService;
import com.manhnv.capstonebakerai.user.repository.UserRepository;
import com.manhnv.capstonebakerai.warehouse.dto.StockLine;
import com.manhnv.capstonebakerai.warehouse.entity.StockLot;
import com.manhnv.capstonebakerai.warehouse.entity.StockReservation;
import com.manhnv.capstonebakerai.warehouse.entity.StockReservationItem;
import com.manhnv.capstonebakerai.warehouse.enums.LotStatus;
import com.manhnv.capstonebakerai.warehouse.enums.StockReservationStatus;
import com.manhnv.capstonebakerai.warehouse.repository.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StockReservationTest {
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void activeReservationProtectsLastUnitAndReleaseLeavesPhysicalStockUnchanged(boolean useFractionalCombo) {
        var lots = mock(StockLotRepository.class);
        var support = mock(WarehouseSupport.class);
        var products = mock(ProductService.class);
        var reservations = mock(StockReservationRepository.class);
        var reservationItems = mock(StockReservationItemRepository.class);
        var stockOut = new StockOutServiceImpl(mock(StockOutNoteRepository.class), mock(StockOutItemRepository.class),
                lots, support, products, mock(StockTransactionService.class), mock(StocktakeNoteRepository.class),
                mock(StocktakeItemRepository.class), mock(UserRepository.class), reservations, reservationItems);

        UUID firstOrder = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID unitId = UUID.randomUUID();
        var quantity = new BigDecimal("1.00");
        var expiresAt = LocalDateTime.now().plusMinutes(15);
        var lot = new StockLot();
        lot.setLotId(UUID.randomUUID());
        lot.setProductId(productId);
        lot.setCurrentQuantity(quantity);
        UUID orderProductId = useFractionalCombo ? UUID.randomUUID() : productId;
        var orderLines = List.of(new StockLine(orderProductId, unitId, useFractionalCombo ? 3 : 1));
        var unit = new InventoryUnit(unitId, productId, BigDecimal.ONE, true);
        when(products.getSaleRequirements(orderProductId)).thenReturn(Map.of(productId,
                useFractionalCombo ? new BigDecimal("0.331") : BigDecimal.ONE));
        when(products.getActiveInventoryUnit(orderProductId, unitId)).thenReturn(unit);
        when(products.getActiveBaseInventoryUnit(productId)).thenReturn(unit);
        when(lots.findSaleableLots(productId, LotStatus.STORAGE, expiresAt.toLocalDate())).thenReturn(List.of(lot));

        when(reservationItems.saveAll(anyList())).thenAnswer(invocation -> {
            List<StockReservationItem> allocations = invocation.getArgument(0);
            StockReservationItem allocation = allocations.getFirst();
            assertEquals(firstOrder, allocation.getReservation().getOrderId());
            assertEquals(lot.getLotId(), allocation.getLotId());
            assertEquals(quantity, allocation.getQuantity());
            when(reservations.lockByOrderId(firstOrder)).thenReturn(Optional.of(allocation.getReservation()));
            when(reservationItems.sumHeldQuantity(eq(lot.getLotId()), eq(StockReservationStatus.ACTIVE), any()))
                    .thenReturn(quantity);
            return allocations;
        });

        stockOut.reserveForOrder(firstOrder, orderLines, expiresAt);
        assertThrows(AppException.class, () -> stockOut.reserveForOrder(UUID.randomUUID(), orderLines, expiresAt));
        stockOut.releaseOrderReservation(firstOrder);

        StockReservation reservation = reservations.lockByOrderId(firstOrder).orElseThrow();
        assertEquals(StockReservationStatus.RELEASED, reservation.getStatus());
        assertEquals(quantity, lot.getCurrentQuantity());
        verify(reservationItems, times(1)).saveAll(anyList());
    }
}
