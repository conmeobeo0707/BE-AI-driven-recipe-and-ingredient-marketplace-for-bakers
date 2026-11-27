package com.manhnv.capstonebakerai.warehouse.service;

import com.manhnv.capstonebakerai.common.exception.AppException;
import com.manhnv.capstonebakerai.product.dtos.InventoryUnit;
import com.manhnv.capstonebakerai.product.dtos.InventoryProduct;
import com.manhnv.capstonebakerai.product.service.ProductService;
import com.manhnv.capstonebakerai.user.repository.UserRepository;
import com.manhnv.capstonebakerai.warehouse.dto.StocktakeCountRequest;
import com.manhnv.capstonebakerai.warehouse.entity.*;
import com.manhnv.capstonebakerai.warehouse.enums.*;
import com.manhnv.capstonebakerai.warehouse.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class StocktakeDisposalTest {

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void assignedStaffRecordsShortageAndOnlyManagerPostsDisposalOnce(boolean hasSurplus) {
        var stocktakes = mock(StocktakeNoteRepository.class);
        var counts = mock(StocktakeItemRepository.class);
        var lots = mock(StockLotRepository.class);
        var stockOutNotes = mock(StockOutNoteRepository.class);
        var stockOutItems = mock(StockOutItemRepository.class);
        var warehouse = mock(WarehouseSupport.class);
        var transactions = mock(StockTransactionService.class);
        var users = mock(UserRepository.class);

        UUID staffId = UUID.randomUUID();
        UUID managerId = UUID.randomUUID();
        UUID stocktakeId = UUID.randomUUID();
        UUID lotId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID countId = UUID.randomUUID();
        UUID noteId = UUID.randomUUID();
        UUID unitId = UUID.randomUUID();
        var initial = new BigDecimal("10.00");
        var actual = new BigDecimal("8.00");

        var stocktake = new StocktakeNote();
        stocktake.setNoteId(stocktakeId);
        stocktake.setHandleBy(staffId);

        var lot = new StockLot();
        lot.setLotId(lotId);
        lot.setProductId(productId);
        lot.setCurrentQuantity(initial);
        lot.setUnitCost(BigDecimal.ONE);

        var surplusLot = new StockLot();
        surplusLot.setLotId(UUID.randomUUID());
        surplusLot.setProductId(UUID.randomUUID());
        surplusLot.setCurrentQuantity(initial);
        surplusLot.setUnitCost(BigDecimal.ONE);
        var surplusCount = new StocktakeItem();
        surplusCount.setItemId(UUID.randomUUID());
        surplusCount.setLotId(surplusLot.getLotId());
        surplusCount.setProductId(surplusLot.getProductId());
        surplusCount.setSystemQuantity(initial);
        surplusCount.setUnitCost(BigDecimal.ONE);
        var surplusActual = new BigDecimal("13.00");

        var count = new StocktakeItem();
        count.setItemId(countId);
        count.setLotId(lotId);
        count.setProductId(productId);
        count.setSystemQuantity(initial);
        count.setUnitCost(BigDecimal.ONE);

        when(stocktakes.lockById(stocktakeId)).thenReturn(Optional.of(stocktake));
        when(stocktakes.save(stocktake)).thenReturn(stocktake);
        when(counts.findByNoteIdOrderByItemId(stocktakeId)).thenReturn(hasSurplus ? List.of(count, surplusCount) : List.of(count));
        when(lots.lockByLotIds(List.of(lotId))).thenReturn(List.of(lot));
        when(lots.findById(lotId)).thenReturn(Optional.of(lot));
        if (hasSurplus) {
            when(lots.lockByLotIds(List.of(lotId, surplusLot.getLotId()))).thenReturn(List.of(lot, surplusLot));
            when(lots.lockByLotIds(List.of(surplusLot.getLotId()))).thenReturn(List.of(surplusLot));
        }
        var unit = mock(InventoryUnit.class);
        when(unit.unitId()).thenReturn(unitId);
        when(warehouse.baseUnit(productId)).thenReturn(unit);
        when(warehouse.unit(productId, unitId)).thenReturn(unit);
        var product = mock(InventoryProduct.class);
        when(product.productId()).thenReturn(productId);
        when(warehouse.lockProducts(List.of(productId))).thenReturn(List.of(product));
        when(stockOutNotes.save(any())).thenAnswer(invocation -> {
            StockOutNote note = invocation.getArgument(0);
            note.setNoteId(noteId);
            return note;
        });

        var stocktakeService = new StocktakeServiceImpl(stocktakes, counts, lots, warehouse, stockOutNotes, users, transactions);
        var stockOutService = new StockOutServiceImpl(stockOutNotes, stockOutItems, lots, warehouse,
                mock(ProductService.class), transactions, stocktakes, counts, users,
                mock(StockReservationRepository.class), mock(StockReservationItemRepository.class));
        var shortage = new StocktakeCountRequest.CountedProduct(productId,
                List.of(new StocktakeCountRequest.CountedLot(countId, actual, "Missing", null)));
        var surplus = new StocktakeCountRequest.CountedProduct(surplusLot.getProductId(),
                List.of(new StocktakeCountRequest.CountedLot(surplusCount.getItemId(), surplusActual, "Surplus", null)));
        var request = new StocktakeCountRequest(hasSurplus ? List.of(shortage, surplus) : List.of(shortage));

        authenticate(UUID.randomUUID(), "STAFF");
        assertThrows(AppException.class, () -> stocktakeService.count(stocktakeId, request));
        authenticate(staffId, "STAFF");
        var withoutReason = new StocktakeCountRequest(List.of(new StocktakeCountRequest.CountedProduct(productId,
                List.of(new StocktakeCountRequest.CountedLot(countId, actual, null, null)))));
        assertThrows(AppException.class, () -> stocktakeService.complete(stocktakeId, withoutReason));
        assertEquals(StocktakeStatus.ASSIGNED, stocktake.getStatus());

        stocktakeService.count(stocktakeId, request);
        assertEquals(StocktakeStatus.ASSIGNED, stocktake.getStatus());
        var submitted = stocktakeService.complete(stocktakeId, request);
        assertEquals(StocktakeStatus.PENDING_DISPOSAL, submitted.note().getStatus());
        assertNull(submitted.pendingDisposalCode());
        assertEquals(initial, surplusLot.getCurrentQuantity());
        verify(warehouse, never()).adjustStock(any());
        verifyNoInteractions(transactions);
        assertEquals(initial, lot.getCurrentQuantity());

        when(stockOutItems.saveAll(any())).thenAnswer(invocation -> {
            List<StockOutItem> pending = invocation.getArgument(0);
            when(stockOutItems.findByNoteIdOrderByItemId(noteId)).thenReturn(pending);
            return pending;
        });
        StockOutNote disposal = stockOutService.createStocktakeDisposal(stocktakeId);
        when(stockOutNotes.findStocktakeNoteId(noteId)).thenReturn(Optional.of(stocktakeId));
        when(stockOutNotes.lockById(noteId)).thenReturn(Optional.of(disposal));
        when(stockOutNotes.findByStocktakeNoteIdAndStatus(stocktakeId, NoteStatus.PENDING)).thenReturn(Optional.of(disposal));

        assertEquals(NoteStatus.PENDING, disposal.getStatus());
        assertEquals(staffId, disposal.getCreatedBy());
        assertThrows(AppException.class, () -> stockOutService.createStocktakeDisposal(stocktakeId));
        assertThrows(AppException.class, () -> stockOutService.complete(noteId));
        assertEquals(initial, lot.getCurrentQuantity());

        authenticate(managerId, "MANAGER");
        stockOutService.complete(noteId);
        assertEquals(NoteStatus.COMPLETED, disposal.getStatus());
        assertEquals(StocktakeStatus.COMPLETED, stocktake.getStatus());
        assertEquals(managerId, disposal.getApprovedBy());
        assertEquals(actual, lot.getCurrentQuantity());
        assertThrows(AppException.class, () -> stockOutService.complete(noteId));
        if (hasSurplus) {
            assertEquals(surplusActual, surplusLot.getCurrentQuantity());
            verify(warehouse).adjustStock(Map.of(productId, new BigDecimal("-2.00"),
                    surplusLot.getProductId(), new BigDecimal("3.00")));
            verify(transactions).record(surplusLot.getLotId(), surplusLot.getProductId(),
                    StockTransactionType.STOCKTAKE_INCREASE, new BigDecimal("3.00"), surplusActual,
                    StockTransactionRefType.STOCKTAKE_NOTE, stocktakeId);
        } else {
            verify(warehouse).adjustStock(Map.of(productId, new BigDecimal("-2.00")));
        }
        verify(transactions).record(lotId, productId, StockTransactionType.DISPOSAL,
                new BigDecimal("-2.00"), actual, StockTransactionRefType.STOCK_OUT_NOTE, noteId);
    }

    @Test
    void surplusOnlyUpdatesExpiredLotAndRecordsIncreaseOnce() {
        var stocktakes = mock(StocktakeNoteRepository.class);
        var counts = mock(StocktakeItemRepository.class);
        var lots = mock(StockLotRepository.class);
        var warehouse = mock(WarehouseSupport.class);
        var transactions = mock(StockTransactionService.class);
        var service = new StocktakeServiceImpl(stocktakes, counts, lots, warehouse,
                mock(StockOutNoteRepository.class), mock(UserRepository.class), transactions);

        UUID staffId = UUID.randomUUID();
        var note = new StocktakeNote();
        note.setNoteId(UUID.randomUUID());
        note.setHandleBy(staffId);
        var lot = new StockLot();
        lot.setLotId(UUID.randomUUID());
        lot.setProductId(UUID.randomUUID());
        lot.setExpiryDate(LocalDate.now().minusDays(1));
        lot.setCurrentQuantity(new BigDecimal("10.00"));
        var count = new StocktakeItem();
        count.setItemId(UUID.randomUUID());
        count.setProductId(lot.getProductId());
        count.setLotId(lot.getLotId());
        count.setSystemQuantity(lot.getCurrentQuantity());
        count.setUnitCost(BigDecimal.ONE);
        var actual = new BigDecimal("12.00");

        when(stocktakes.lockById(note.getNoteId())).thenReturn(Optional.of(note));
        when(counts.findByNoteIdOrderByItemId(note.getNoteId())).thenReturn(List.of(count));
        when(lots.lockByLotIds(List.of(lot.getLotId()))).thenReturn(List.of(lot));

        authenticate(staffId, "STAFF");
        var request = new StocktakeCountRequest(List.of(new StocktakeCountRequest.CountedProduct(lot.getProductId(),
                List.of(new StocktakeCountRequest.CountedLot(count.getItemId(), actual, "Surplus", null)))));
        assertEquals(StocktakeStatus.COMPLETED, service.complete(note.getNoteId(), request).note().getStatus());
        assertEquals(actual, lot.getCurrentQuantity());
        assertEquals(LotStatus.EXPIRED, lot.getStatus());
        assertThrows(AppException.class, () -> service.complete(note.getNoteId(), request));

        var increase = new BigDecimal("2.00");
        verify(warehouse).adjustStock(Map.of(lot.getProductId(), increase));
        verify(transactions).record(lot.getLotId(), lot.getProductId(), StockTransactionType.STOCKTAKE_INCREASE,
                increase, actual, StockTransactionRefType.STOCKTAKE_NOTE, note.getNoteId());
        verifyNoMoreInteractions(transactions);
    }

    private void authenticate(UUID userId, String role) {
        Jwt jwt = Jwt.withTokenValue("test").header("alg", "none").subject(userId.toString()).build();
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }
}
