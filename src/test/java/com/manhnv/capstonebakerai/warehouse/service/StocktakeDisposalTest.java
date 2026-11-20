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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.math.BigDecimal;
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

    @Test
    void assignedStaffRecordsShortageAndOnlyManagerPostsDisposalOnce() {
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

        var count = new StocktakeItem();
        count.setItemId(countId);
        count.setLotId(lotId);
        count.setProductId(productId);
        count.setSystemQuantity(initial);
        count.setUnitCost(BigDecimal.ONE);

        when(stocktakes.lockById(stocktakeId)).thenReturn(Optional.of(stocktake));
        when(stocktakes.save(stocktake)).thenReturn(stocktake);
        when(counts.findByNoteIdOrderByItemId(stocktakeId)).thenReturn(List.of(count));
        when(lots.lockByLotIds(List.of(lotId))).thenReturn(List.of(lot));
        when(lots.findById(lotId)).thenReturn(Optional.of(lot));
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

        var stocktakeService = new StocktakeServiceImpl(stocktakes, counts, lots, warehouse, stockOutNotes, users);
        var stockOutService = new StockOutServiceImpl(stockOutNotes, stockOutItems, lots, warehouse,
                mock(ProductService.class), transactions, stocktakes, counts, users);
        var request = new StocktakeCountRequest(List.of(new StocktakeCountRequest.CountedProduct(productId,
                List.of(new StocktakeCountRequest.CountedLot(countId, actual, "Missing", null)))));

        authenticate(UUID.randomUUID(), "STAFF");
        assertThrows(AppException.class, () -> stocktakeService.count(stocktakeId, request));
        authenticate(staffId, "STAFF");
        var withoutReason = new StocktakeCountRequest(List.of(new StocktakeCountRequest.CountedProduct(productId,
                List.of(new StocktakeCountRequest.CountedLot(countId, actual, null, null)))));
        assertThrows(AppException.class, () -> stocktakeService.complete(stocktakeId, withoutReason));
        assertEquals(StocktakeStatus.ASSIGNED, stocktake.getStatus());

        stocktakeService.count(stocktakeId, request);
        assertEquals(StocktakeStatus.ASSIGNED, stocktake.getStatus());
        stocktakeService.complete(stocktakeId, request);
        assertEquals(StocktakeStatus.PENDING_DISPOSAL, stocktake.getStatus());
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
        verify(warehouse).adjustStock(Map.of(productId, new BigDecimal("-2.00")));
        verify(transactions).record(lotId, productId, StockTransactionType.DISPOSAL,
                new BigDecimal("-2.00"), actual, StockTransactionRefType.STOCK_OUT_NOTE, noteId);
    }

    private void authenticate(UUID userId, String role) {
        Jwt jwt = Jwt.withTokenValue("test").header("alg", "none").subject(userId.toString()).build();
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }
}
