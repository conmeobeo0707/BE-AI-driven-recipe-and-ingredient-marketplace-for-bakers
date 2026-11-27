package com.manhnv.capstonebakerai.warehouse.service;

import com.manhnv.capstonebakerai.common.exception.AppException;
import com.manhnv.capstonebakerai.product.dtos.InventoryProduct;
import com.manhnv.capstonebakerai.product.dtos.InventoryUnit;
import com.manhnv.capstonebakerai.product.enums.ProductStatus;
import com.manhnv.capstonebakerai.product.service.ProductService;
import com.manhnv.capstonebakerai.user.repository.UserRepository;
import com.manhnv.capstonebakerai.warehouse.entity.StockLot;
import com.manhnv.capstonebakerai.warehouse.entity.StockOutNote;
import com.manhnv.capstonebakerai.warehouse.enums.NoteStatus;
import com.manhnv.capstonebakerai.warehouse.enums.StockOutType;
import com.manhnv.capstonebakerai.warehouse.exception.WarehouseErrorCode;
import com.manhnv.capstonebakerai.warehouse.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ExpiredLotDisposalTest {
    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void rejectsSecondPendingDisposalAndAllowsAnotherAfterRejection() {
        var notes = mock(StockOutNoteRepository.class);
        var items = mock(StockOutItemRepository.class);
        var lots = mock(StockLotRepository.class);
        var support = mock(WarehouseSupport.class);
        var products = mock(ProductService.class);
        var service = new StockOutServiceImpl(notes, items, lots, support, products, mock(StockTransactionService.class),
                mock(StocktakeNoteRepository.class), mock(StocktakeItemRepository.class), mock(UserRepository.class),
                mock(StockReservationRepository.class), mock(StockReservationItemRepository.class));

        UUID productId = UUID.randomUUID();
        UUID lotId = UUID.randomUUID();
        UUID unitId = UUID.randomUUID();
        var lot = new StockLot();
        lot.setLotId(lotId);
        lot.setProductId(productId);
        lot.setExpiryDate(LocalDate.now().minusDays(1));
        lot.setCurrentQuantity(BigDecimal.TEN);
        lot.setUnitCost(new BigDecimal("2500.00"));
        var unit = new InventoryUnit(unitId, productId, BigDecimal.ONE, true);
        when(lots.findById(lotId)).thenReturn(Optional.of(lot));
        when(lots.lockByLotIds(List.of(lotId))).thenReturn(List.of(lot));
        when(products.getActiveBaseInventoryUnit(productId)).thenReturn(unit);
        when(support.unit(productId, unitId)).thenReturn(unit);
        when(support.baseUnit(productId)).thenReturn(unit);
        when(support.lockProducts(List.of(productId))).thenReturn(
                List.of(new InventoryProduct(productId, BigDecimal.TEN, false, ProductStatus.INACTIVE)));
        when(items.hasDisposalForLot(lotId, NoteStatus.PENDING, StockOutType.DISPOSAL))
                .thenReturn(false, true, false);
        when(notes.save(any(StockOutNote.class))).thenAnswer(invocation -> {
            StockOutNote note = invocation.getArgument(0);
            note.setNoteId(UUID.randomUUID());
            return note;
        });

        Jwt jwt = Jwt.withTokenValue("test").header("alg", "none").subject(UUID.randomUUID().toString()).build();
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_STAFF"))));

        StockOutNote first = service.disposeExpiredLot(lotId);
        assertEquals(new BigDecimal("25000.00"), first.getTotalCost());
        var inOrder = inOrder(products, lots, items);
        inOrder.verify(products).lockInventoryProducts(List.of(productId));
        inOrder.verify(lots).lockByLotIds(List.of(lotId));
        inOrder.verify(items).hasDisposalForLot(lotId, NoteStatus.PENDING, StockOutType.DISPOSAL);

        var duplicate = assertThrows(AppException.class, () -> service.disposeExpiredLot(lotId));
        assertEquals(WarehouseErrorCode.CONFLICT, duplicate.getErrorCode());
        verify(notes, times(1)).save(any(StockOutNote.class));

        StockOutNote replacement = service.disposeExpiredLot(lotId);
        assertEquals(NoteStatus.PENDING, replacement.getStatus());
        assertNotEquals(first.getNoteId(), replacement.getNoteId());
        verify(items, times(2)).saveAll(anyList());
        assertEquals(BigDecimal.TEN, lot.getCurrentQuantity());
    }
}
