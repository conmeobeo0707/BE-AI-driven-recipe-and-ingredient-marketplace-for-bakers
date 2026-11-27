package com.manhnv.capstonebakerai.warehouse.service;

import com.manhnv.capstonebakerai.common.exception.AppException;
import com.manhnv.capstonebakerai.user.entity.User;
import com.manhnv.capstonebakerai.user.enums.Role;
import com.manhnv.capstonebakerai.user.enums.UserStatus;
import com.manhnv.capstonebakerai.user.repository.UserRepository;
import com.manhnv.capstonebakerai.warehouse.dto.StocktakeCreateRequest;
import com.manhnv.capstonebakerai.warehouse.entity.StockLot;
import com.manhnv.capstonebakerai.warehouse.entity.StocktakeItem;
import com.manhnv.capstonebakerai.warehouse.entity.StocktakeNote;
import com.manhnv.capstonebakerai.warehouse.enums.LotStatus;
import com.manhnv.capstonebakerai.warehouse.enums.StockReservationStatus;
import com.manhnv.capstonebakerai.warehouse.enums.StocktakeStatus;
import com.manhnv.capstonebakerai.warehouse.exception.WarehouseErrorCode;
import com.manhnv.capstonebakerai.warehouse.repository.StockLotRepository;
import com.manhnv.capstonebakerai.warehouse.repository.StockOutNoteRepository;
import com.manhnv.capstonebakerai.warehouse.repository.StocktakeItemRepository;
import com.manhnv.capstonebakerai.warehouse.repository.StocktakeNoteRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class StocktakeCreateTest {
    private final UUID managerId = UUID.randomUUID();
    private final UUID productId = UUID.randomUUID();
    private final UUID noteId = UUID.randomUUID();
    private final StocktakeNoteRepository notes = mock(StocktakeNoteRepository.class);
    private final StocktakeItemRepository items = mock(StocktakeItemRepository.class);
    private final StockLotRepository lots = mock(StockLotRepository.class);
    private final WarehouseSupport support = mock(WarehouseSupport.class);
    private final UserRepository users = mock(UserRepository.class);
    private final StocktakeServiceImpl service = new StocktakeServiceImpl(
            notes, items, lots, support, mock(StockOutNoteRepository.class),
            users, mock(StockTransactionService.class));

    @BeforeEach
    void authenticateManager() {
        Jwt jwt = Jwt.withTokenValue("test").header("alg", "none").subject(managerId.toString()).build();
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_MANAGER"))));
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createsDraftWithSnapshotOfEachLot() {
        StockLot first = lot("10.00");
        StockLot second = lot("5.00");
        stubCreation(Stream.of(first, second).sorted(Comparator.comparing(StockLot::getLotId)).toList());

        var result = service.create(new StocktakeCreateRequest(List.of(productId), null));

        assertEquals(StocktakeStatus.DRAFT, result.note().getStatus());
        assertEquals(managerId, result.note().getCreatedBy());
        assertNull(result.note().getHandleBy());
        assertEquals(1, result.products().size());
        assertEquals(productId, result.products().getFirst().productId());
        List<StocktakeItem> counts = result.products().getFirst().lots();
        assertEquals(2, counts.size());
        assertSnapshot(first, counts);
        assertSnapshot(second, counts);
        verify(items).saveAll(counts);
    }

    @Test
    void createsAssignedNoteWhenActiveStaffIsProvided() {
        UUID staffId = UUID.randomUUID();
        User staff = new User();
        staff.setRole(Role.STAFF);
        staff.setStatus(UserStatus.ACTIVE);
        when(users.findById(staffId)).thenReturn(Optional.of(staff));
        stubCreation(List.of(lot("10.00")));

        var result = service.create(new StocktakeCreateRequest(List.of(productId), staffId));

        assertEquals(StocktakeStatus.ASSIGNED, result.note().getStatus());
        assertEquals(staffId, result.note().getHandleBy());
    }

    @Test
    void rejectsDuplicateProductIdsBeforeAccessingInventory() {
        var request = new StocktakeCreateRequest(List.of(productId, productId), null);

        var error = assertThrows(AppException.class, () -> service.create(request));

        assertEquals(WarehouseErrorCode.INVALID_REQUEST, error.getErrorCode());
        verifyNoInteractions(support, lots, notes, items);
    }

    @Test
    void rejectsCreationWhenLotHasAnActiveReservation() {
        StockLot reservedLot = lot("10.00");
        stubCountableLots(List.of(reservedLot));
        when(lots.hasActiveReservations(eq(List.of(reservedLot.getLotId())),
                eq(StockReservationStatus.ACTIVE), any())).thenReturn(true);

        var error = assertThrows(AppException.class,
                () -> service.create(new StocktakeCreateRequest(List.of(productId), null)));

        assertEquals(WarehouseErrorCode.CONFLICT, error.getErrorCode());
        verifyNoInteractions(notes, items);
    }

    private void stubCountableLots(List<StockLot> countableLots) {
        when(lots.findCountableLots(List.of(productId), List.of(LotStatus.STORAGE, LotStatus.EXPIRED)))
                .thenReturn(countableLots);
    }

    private void stubCreation(List<StockLot> countableLots) {
        stubCountableLots(countableLots);
        when(lots.lockByLotIds(countableLots.stream().map(StockLot::getLotId).toList()))
                .thenReturn(countableLots);
        when(notes.save(any(StocktakeNote.class))).thenAnswer(invocation -> {
            StocktakeNote note = invocation.getArgument(0);
            note.setNoteId(noteId);
            return note;
        });
    }

    private StockLot lot(String quantity) {
        StockLot lot = new StockLot();
        lot.setLotId(UUID.randomUUID());
        lot.setProductId(productId);
        lot.setStatus(LotStatus.STORAGE);
        lot.setCurrentQuantity(new BigDecimal(quantity));
        lot.setUnitCost(BigDecimal.ONE);
        return lot;
    }

    private void assertSnapshot(StockLot lot, List<StocktakeItem> counts) {
        StocktakeItem count = counts.stream()
                .filter(item -> lot.getLotId().equals(item.getLotId()))
                .findFirst()
                .orElseThrow();
        assertEquals(noteId, count.getNoteId());
        assertEquals(productId, count.getProductId());
        assertEquals(lot.getCurrentQuantity(), count.getSystemQuantity());
        assertEquals(lot.getUnitCost(), count.getUnitCost());
    }
}
