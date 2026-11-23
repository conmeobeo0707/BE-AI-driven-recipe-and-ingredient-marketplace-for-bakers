package com.manhnv.capstonebakerai.receipt.service;

import com.manhnv.capstonebakerai.common.exception.AppException;
import com.manhnv.capstonebakerai.receipt.dto.request.ReceiptCategoryRequest;
import com.manhnv.capstonebakerai.receipt.dto.response.ReceiptCategoryResponse;
import com.manhnv.capstonebakerai.receipt.entity.ReceiptCategory;
import com.manhnv.capstonebakerai.receipt.enums.ReceiptCategoryType;
import com.manhnv.capstonebakerai.receipt.exception.ReceiptCategoryErrorCode;
import com.manhnv.capstonebakerai.receipt.mapper.ReceiptCategoryMapper;
import com.manhnv.capstonebakerai.receipt.repository.ReceiptCategoryRepository;
import com.manhnv.capstonebakerai.receipt.repository.ReceiptVoucherRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReceiptCategoryServiceImplTest {

    @Mock
    ReceiptCategoryRepository categoryRepository;
    @Mock
    ReceiptVoucherRepository voucherRepository;
    @Mock
    ReceiptCategoryMapper mapper;
    @InjectMocks
    ReceiptCategoryServiceImpl service;

    @Captor
    ArgumentCaptor<ReceiptCategory> categoryCaptor;

    private final UUID categoryId = UUID.randomUUID();

    // Lấy giá trị từ values() để không phụ thuộc tên hằng của enum
    private static final ReceiptCategoryType TYPE_A = ReceiptCategoryType.values()[0];
    private static final ReceiptCategoryType TYPE_B =
            ReceiptCategoryType.values()[ReceiptCategoryType.values().length - 1];

    private ReceiptCategory existingCategory() {
        return ReceiptCategory.builder()
                .id(categoryId)
                .name("Old Name")
                .type(TYPE_A)
                .build();
    }

    // Create
    // UTCID01
    @Test
    void create_newName_shouldTrimNameAndSave() {
        ReceiptCategoryResponse response = mock(ReceiptCategoryResponse.class);
        when(categoryRepository.existsByNameIgnoreCase("Ingredients")).thenReturn(false);
        when(categoryRepository.save(any(ReceiptCategory.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toResponse(any(ReceiptCategory.class))).thenReturn(response);

        ReceiptCategoryResponse result = service.create(new ReceiptCategoryRequest("  Ingredients  ", TYPE_A));

        assertThat(result).isSameAs(response);
        verify(categoryRepository).save(categoryCaptor.capture());
        assertThat(categoryCaptor.getValue().getName()).isEqualTo("Ingredients");
        assertThat(categoryCaptor.getValue().getType()).isEqualTo(TYPE_A);
    }

    // UTCID02
    @Test
    void create_nameExists_shouldThrowNameExisted() {
        when(categoryRepository.existsByNameIgnoreCase("Ingredients")).thenReturn(true);

        assertThatThrownBy(() -> service.create(new ReceiptCategoryRequest("  Ingredients  ", TYPE_A)))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ReceiptCategoryErrorCode.RECEIPT_CATEGORY_NAME_EXISTED);

        verify(categoryRepository, never()).save(any());
        verifyNoInteractions(mapper);
    }

    // Update
    // UTCID01
    @Test
    void update_validRequest_shouldUpdateNameAndType() {
        ReceiptCategory category = existingCategory();
        ReceiptCategoryResponse response = mock(ReceiptCategoryResponse.class);
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(categoryRepository.existsByNameIgnoreCaseAndIdNot("New Name", categoryId)).thenReturn(false);
        when(categoryRepository.save(any(ReceiptCategory.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toResponse(any(ReceiptCategory.class))).thenReturn(response);

        ReceiptCategoryResponse result = service.update(categoryId, new ReceiptCategoryRequest("  New Name  ", TYPE_B));

        assertThat(result).isSameAs(response);
        verify(categoryRepository).save(categoryCaptor.capture());
        assertThat(categoryCaptor.getValue().getName()).isEqualTo("New Name");
        assertThat(categoryCaptor.getValue().getType()).isEqualTo(TYPE_B);
    }

    // UTCID02
    @Test
    void update_categoryNotFound_shouldThrowNotFound() {
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(categoryId, new ReceiptCategoryRequest("New Name", TYPE_A)))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ReceiptCategoryErrorCode.RECEIPT_CATEGORY_NOT_FOUND);

        verify(categoryRepository, never()).existsByNameIgnoreCaseAndIdNot(any(), any());
        verify(categoryRepository, never()).save(any());
    }

    // UTCID03
    @Test
    void update_nameUsedByAnotherCategory_shouldThrowNameExisted() {
        ReceiptCategory category = existingCategory();
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(categoryRepository.existsByNameIgnoreCaseAndIdNot("Taken", categoryId)).thenReturn(true);

        assertThatThrownBy(() -> service.update(categoryId, new ReceiptCategoryRequest("  Taken ", TYPE_B)))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ReceiptCategoryErrorCode.RECEIPT_CATEGORY_NAME_EXISTED);

        assertThat(category.getName()).isEqualTo("Old Name");   // dữ liệu không bị đổi
        assertThat(category.getType()).isEqualTo(TYPE_A);
        verify(categoryRepository, never()).save(any());
    }

    // Delete
    // UTCID01
    @Test
    void delete_notInUse_shouldDeleteCategory() {
        ReceiptCategory category = existingCategory();
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(voucherRepository.existsByReceiptCategoryId(categoryId)).thenReturn(false);

        service.delete(categoryId);

        verify(categoryRepository).delete(category);
    }

    // UTCID02
    @Test
    void delete_categoryNotFound_shouldThrowNotFound() {
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(categoryId))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ReceiptCategoryErrorCode.RECEIPT_CATEGORY_NOT_FOUND);

        verifyNoInteractions(voucherRepository);
        verify(categoryRepository, never()).delete(any());
    }

    // UTCID03
    @Test
    void delete_categoryInUse_shouldThrowInUse() {
        ReceiptCategory category = existingCategory();
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(voucherRepository.existsByReceiptCategoryId(categoryId)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(categoryId))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ReceiptCategoryErrorCode.RECEIPT_CATEGORY_IN_USE);

        verify(categoryRepository, never()).delete(any());
    }

    // Get by id
    // UTCID01
    @Test
    void getById_categoryExists_shouldReturnResponse() {
        ReceiptCategory category = existingCategory();
        ReceiptCategoryResponse response = mock(ReceiptCategoryResponse.class);
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(mapper.toResponse(category)).thenReturn(response);

        ReceiptCategoryResponse result = service.getById(categoryId);

        assertThat(result).isSameAs(response);
    }

    // UTCID02
    @Test
    void getById_categoryNotFound_shouldThrowNotFound() {
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(categoryId))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ReceiptCategoryErrorCode.RECEIPT_CATEGORY_NOT_FOUND);

        verifyNoInteractions(mapper);
    }

    // Get all
    // UTCID01
    @Test
    void getAll_categoriesExist_shouldReturnMappedList() {
        ReceiptCategory c1 = existingCategory();
        ReceiptCategory c2 = ReceiptCategory.builder().id(UUID.randomUUID()).name("Other").type(TYPE_B).build();
        ReceiptCategoryResponse r1 = mock(ReceiptCategoryResponse.class);
        ReceiptCategoryResponse r2 = mock(ReceiptCategoryResponse.class);
        when(categoryRepository.findAll()).thenReturn(List.of(c1, c2));
        when(mapper.toResponse(c1)).thenReturn(r1);
        when(mapper.toResponse(c2)).thenReturn(r2);

        List<ReceiptCategoryResponse> result = service.getAll();

        assertThat(result).containsExactly(r1, r2);
    }

    // UTCID02
    @Test
    void getAll_noCategory_shouldReturnEmptyList() {
        when(categoryRepository.findAll()).thenReturn(List.of());

        List<ReceiptCategoryResponse> result = service.getAll();

        assertThat(result).isEmpty();
        verifyNoInteractions(mapper);
    }
}
