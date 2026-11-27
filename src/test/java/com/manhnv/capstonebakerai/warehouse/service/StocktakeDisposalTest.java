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
    void postsNewDamageOrLegacyShortageOnlyOnce(boolean hasSurplus) {
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
        var damage = hasSurplus ? new BigDecimal("1.00") : BigDecimal.ZERO;
        var shortage = new StocktakeCountRequest.CountedProduct(productId,
                List.of(new StocktakeCountRequest.CountedLot(countId, actual, "Missing", null,
                        damage, hasSurplus ? "Damaged packaging" : null)));
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
        assertEquals(StocktakeStatus.PENDING_REVIEW, submitted.note().getStatus());
        assertEquals(actual.subtract(damage), count.getGoodQuantity());
        assertNull(submitted.pendingDisposalCode());
        assertEquals(initial, surplusLot.getCurrentQuantity());
        verify(warehouse, never()).adjustStock(any());
        verifyNoInteractions(transactions);
        assertEquals(initial, lot.getCurrentQuantity());

        assertThrows(AppException.class, () -> stockOutService.createStocktakeDisposal(stocktakeId));
        BigDecimal beforeDisposal;
        if (hasSurplus) {
            authenticate(managerId, "MANAGER");
            stocktakeService.returnForCorrection(stocktakeId, "Check quantities");
            assertEquals(StocktakeStatus.ASSIGNED, stocktake.getStatus());
            assertEquals("Check quantities", stocktake.getReviewReturnReason());
            verifyNoInteractions(transactions);
            authenticate(staffId, "STAFF");
            stocktakeService.complete(stocktakeId, request);
            authenticate(managerId, "MANAGER");
            assertEquals(StocktakeStatus.PENDING_DISPOSAL, stocktakeService.approve(stocktakeId).note().getStatus());
            assertThrows(AppException.class, () -> stocktakeService.approve(stocktakeId));
            beforeDisposal = actual;
            assertEquals(actual, lot.getCurrentQuantity());
            assertEquals(surplusActual, surplusLot.getCurrentQuantity());
            verify(warehouse).adjustStock(Map.of(productId, new BigDecimal("-2.00"),
                    surplusLot.getProductId(), new BigDecimal("3.00")));
            verify(transactions).record(lotId, productId, StockTransactionType.STOCKTAKE_DECREASE,
                    new BigDecimal("-2.00"), actual, StockTransactionRefType.STOCKTAKE_NOTE, stocktakeId);
            verify(transactions).record(surplusLot.getLotId(), surplusLot.getProductId(),
                    StockTransactionType.STOCKTAKE_INCREASE, new BigDecimal("3.00"), surplusActual,
                    StockTransactionRefType.STOCKTAKE_NOTE, stocktakeId);
        } else {
            // A previously submitted note has no review flag in the database.
            stocktake.setReviewRequired(null);
            beforeDisposal = initial;
        }
        authenticate(staffId, "STAFF");
        var disposalQuantity = hasSurplus ? damage : new BigDecimal("2.00");

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
        assertEquals(disposalQuantity, stockOutItems.findByNoteIdOrderByItemId(noteId).getFirst().getQuantity());
        assertThrows(AppException.class, () -> stockOutService.createStocktakeDisposal(stocktakeId));
        assertThrows(AppException.class, () -> stockOutService.complete(noteId));
        assertEquals(beforeDisposal, lot.getCurrentQuantity());

        assertThrows(AppException.class, () -> stockOutService.startDisposal(noteId));
        assertThrows(AppException.class, () -> stockOutService.approveDisposal(noteId));

        authenticate(managerId, "MANAGER");
        stockOutService.approveDisposal(noteId);
        assertEquals(NoteStatus.PENDING, disposal.getStatus());
        assertEquals(StocktakeStatus.PENDING_DISPOSAL, stocktake.getStatus());
        assertNotNull(disposal.getApprovedAt());
        assertThrows(AppException.class, () -> stockOutService.complete(noteId));
        assertThrows(AppException.class, () -> stockOutService.cancel(noteId, "Rejected"));
        assertEquals(beforeDisposal, lot.getCurrentQuantity());
        verify(transactions, never()).record(eq(lotId), eq(productId), eq(StockTransactionType.DISPOSAL), any(), any(), any(), any());

        authenticate(staffId, "STAFF");
        assertThrows(AppException.class, () -> stockOutService.complete(noteId));
        stockOutService.startDisposal(noteId);
        assertEquals(staffId, disposal.getStartedBy());
        assertNotNull(disposal.getStartedAt());
        assertEquals(beforeDisposal, lot.getCurrentQuantity());
        verify(transactions, never()).record(eq(lotId), eq(productId), eq(StockTransactionType.DISPOSAL), any(), any(), any(), any());

        authenticate(UUID.randomUUID(), "STAFF");
        assertThrows(AppException.class, () -> stockOutService.complete(noteId));
        authenticate(staffId, "STAFF");
        stockOutService.complete(noteId);
        assertEquals(NoteStatus.COMPLETED, disposal.getStatus());
        assertNotNull(disposal.getCompletedAt());
        assertEquals(StocktakeStatus.COMPLETED, stocktake.getStatus());
        assertEquals(managerId, disposal.getApprovedBy());
        BigDecimal afterDisposal = beforeDisposal.subtract(disposalQuantity);
        assertEquals(afterDisposal, lot.getCurrentQuantity());
        assertThrows(AppException.class, () -> stockOutService.complete(noteId));
        verify(warehouse).adjustStock(Map.of(productId, disposalQuantity.negate()));
        verify(transactions).record(lotId, productId, StockTransactionType.DISPOSAL,
                disposalQuantity.negate(), afterDisposal, StockTransactionRefType.STOCK_OUT_NOTE, noteId);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void pureDiscrepancyPostsOnlyOnManagerApproval(boolean isSurplus) {
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
        var actual = new BigDecimal(isSurplus ? "12.00" : "8.00");

        when(stocktakes.lockById(note.getNoteId())).thenReturn(Optional.of(note));
        when(stocktakes.save(note)).thenReturn(note);
        when(counts.findByNoteIdOrderByItemId(note.getNoteId())).thenReturn(List.of(count));
        when(lots.lockByLotIds(List.of(lot.getLotId()))).thenReturn(List.of(lot));

        authenticate(staffId, "STAFF");
        var request = new StocktakeCountRequest(List.of(new StocktakeCountRequest.CountedProduct(lot.getProductId(),
                List.of(new StocktakeCountRequest.CountedLot(count.getItemId(), actual, "Surplus", null)))));
        assertEquals(StocktakeStatus.PENDING_REVIEW, service.complete(note.getNoteId(), request).note().getStatus());
        assertEquals(new BigDecimal("10.00"), lot.getCurrentQuantity());
        verifyNoInteractions(transactions);
        authenticate(UUID.randomUUID(), "MANAGER");
        assertEquals(StocktakeStatus.COMPLETED, service.approve(note.getNoteId()).note().getStatus());
        assertEquals(actual, lot.getCurrentQuantity());
        assertEquals(LotStatus.EXPIRED, lot.getStatus());
        assertThrows(AppException.class, () -> service.approve(note.getNoteId()));

        var increase = new BigDecimal(isSurplus ? "2.00" : "-2.00");
        verify(warehouse).adjustStock(Map.of(lot.getProductId(), increase));
        verify(transactions).record(lot.getLotId(), lot.getProductId(), isSurplus ? StockTransactionType.STOCKTAKE_INCREASE : StockTransactionType.STOCKTAKE_DECREASE,
                increase, actual, StockTransactionRefType.STOCKTAKE_NOTE, note.getNoteId());
        verifyNoMoreInteractions(transactions);
    }

    @Test
    void savingCountsKeepsDamageUntilAnExplicitZeroIsSaved() throws Exception {
        var notes = mock(StocktakeNoteRepository.class);
        var items = mock(StocktakeItemRepository.class);
        var service = new StocktakeServiceImpl(notes, items, mock(StockLotRepository.class),
                mock(WarehouseSupport.class), mock(StockOutNoteRepository.class),
                mock(UserRepository.class), mock(StockTransactionService.class));
        UUID staffId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        var note = new StocktakeNote();
        note.setNoteId(UUID.randomUUID());
        note.setHandleBy(staffId);
        var item = new StocktakeItem();
        item.setItemId(UUID.randomUUID());
        item.setProductId(productId);
        item.setLotId(UUID.randomUUID());
        when(notes.lockById(note.getNoteId())).thenReturn(Optional.of(note));
        when(notes.findById(note.getNoteId())).thenReturn(Optional.of(note));
        when(items.findByNoteIdOrderByItemId(note.getNoteId())).thenReturn(List.of(item));
        authenticate(staffId, "STAFF");

        String json = """
                {"products":[{"productId":"%s","lots":[{"itemId":"%s","actualQuantity":8,
                "damagedQuantity":1,"damageReason":"Torn packaging"}]}]}
                """.formatted(productId, item.getItemId());
        var request = new tools.jackson.databind.json.JsonMapper().readValue(json, StocktakeCountRequest.class);

        service.count(note.getNoteId(), request);
        StocktakeItem saved = service.get(note.getNoteId()).products().getFirst().lots().getFirst();
        assertEquals(BigDecimal.ONE, saved.getDamagedQuantity());
        assertEquals(new BigDecimal("7"), saved.getGoodQuantity());
        assertEquals("Torn packaging", saved.getDamageReason());

        service.count(note.getNoteId(), new StocktakeCountRequest(List.of(
                new StocktakeCountRequest.CountedProduct(productId, List.of(
                        new StocktakeCountRequest.CountedLot(item.getItemId(), new BigDecimal("8"), null, "Recounted"))))));
        assertEquals(BigDecimal.ONE, item.getDamagedQuantity());
        assertEquals("Torn packaging", item.getDamageReason());

        service.count(note.getNoteId(), new StocktakeCountRequest(List.of(
                new StocktakeCountRequest.CountedProduct(productId, List.of(
                        new StocktakeCountRequest.CountedLot(item.getItemId(), new BigDecimal("8"), null, null,
                                BigDecimal.ZERO, null))))));
        assertEquals(BigDecimal.ZERO, item.getDamagedQuantity());
        assertNull(item.getDamageReason());
    }

    private void authenticate(UUID userId, String role) {
        Jwt jwt = Jwt.withTokenValue("test").header("alg", "none").subject(userId.toString()).build();
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }
}
