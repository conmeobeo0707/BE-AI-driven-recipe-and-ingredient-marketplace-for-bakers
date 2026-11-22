package com.manhnv.capstonebakerai.expense.service;

import com.manhnv.capstonebakerai.common.exception.AppException;
import com.manhnv.capstonebakerai.expense.dto.request.ExpenseCategoryRequest;
import com.manhnv.capstonebakerai.expense.dto.response.ExpenseCategoryResponse;
import com.manhnv.capstonebakerai.expense.entity.ExpenseCategory;
import com.manhnv.capstonebakerai.expense.enums.ExpenseCategoryType;
import com.manhnv.capstonebakerai.expense.mapper.ExpenseCategoryMapper;
import com.manhnv.capstonebakerai.expense.repository.ExpenseCategoryRepository;
import com.manhnv.capstonebakerai.expense.repository.ExpenseVoucherRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ExpenseCategoryServiceImplTest {

    @Mock
    ExpenseCategoryRepository categoryRepository;
    @Mock
    ExpenseVoucherRepository voucherRepository;
    @Mock
    ExpenseCategoryMapper mapper;
    @InjectMocks ExpenseCategoryServiceImpl categoryService;

    // Create
    // UTCID01
    @Test
    void create_newName_shouldSaveAndReturnResponse() {
        // Arrange
        ExpenseCategoryRequest request = new ExpenseCategoryRequest("Electricity", ExpenseCategoryType.SALARY);
        ExpenseCategory saved = ExpenseCategory.builder().name("Electricity").type(ExpenseCategoryType.SALARY).build();
        ExpenseCategoryResponse expected = mock(ExpenseCategoryResponse.class);

        when(categoryRepository.existsByNameIgnoreCase("Electricity")).thenReturn(false);
        when(categoryRepository.save(any(ExpenseCategory.class))).thenReturn(saved);
        when(mapper.toResponse(saved)).thenReturn(expected);

        // Action
        ExpenseCategoryResponse response = categoryService.create(request);

        // Assert
        assertThat(response).isSameAs(expected);
        verify(categoryRepository).save(any(ExpenseCategory.class));
    }

    // UTCID02
    @Test
    void create_nameWithSpaces_shouldSaveTrimmedName() {
        // Arrange
        ExpenseCategoryRequest request = new ExpenseCategoryRequest("  Rent  ", ExpenseCategoryType.SALARY);

        when(categoryRepository.existsByNameIgnoreCase("  Rent  ")).thenReturn(false);
        when(categoryRepository.save(any(ExpenseCategory.class))).thenAnswer(inv -> inv.getArgument(0));

        // Action
        categoryService.create(request);

        // Assert
        ArgumentCaptor<ExpenseCategory> captor = ArgumentCaptor.forClass(ExpenseCategory.class);
        verify(categoryRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Rent");
    }

    // UTCID03
    @Test
    void create_nameExisted_shouldThrow() {
        // Arrange
        ExpenseCategoryRequest request = new ExpenseCategoryRequest("Rent", ExpenseCategoryType.SALARY);

        when(categoryRepository.existsByNameIgnoreCase("Rent")).thenReturn(true);

        // Action - Assert
        assertThatThrownBy(() -> categoryService.create(request))
                .isInstanceOf(AppException.class);
        verify(categoryRepository, never()).save(any());
    }

    // Update
    // UTCID01
    @Test
    void update_validCategoryNewName_shouldUpdate() {
        // Arrange
        UUID id = UUID.randomUUID();
        ExpenseCategory entity = ExpenseCategory.builder().name("Old").type(ExpenseCategoryType.SALARY).build();
        ExpenseCategoryRequest request = new ExpenseCategoryRequest("  New  ", ExpenseCategoryType.SALARY);

        when(categoryRepository.findById(id)).thenReturn(Optional.of(entity));
        when(categoryRepository.existsByNameIgnoreCaseAndIdNot("New", id)).thenReturn(false);
        when(categoryRepository.save(entity)).thenReturn(entity);

        // Action
        categoryService.update(id, request);

         // Assert
        assertThat(entity.getName()).isEqualTo("New");
        assertThat(entity.getType()).isEqualTo(ExpenseCategoryType.SALARY);
        verify(categoryRepository).save(entity);
    }

    // UTCID02
    @Test
    void update_categoryNotFound_shouldThrow() {
        // Arrange
        UUID id = UUID.randomUUID();
        ExpenseCategoryRequest request = new ExpenseCategoryRequest("New", ExpenseCategoryType.SALARY);

        when(categoryRepository.findById(id)).thenReturn(Optional.empty());

        // Action - Assert
        assertThatThrownBy(() -> categoryService.update(id, request))
                .isInstanceOf(AppException.class);
        verify(categoryRepository, never()).save(any());
    }

    // UTCID03
    @Test
    void update_nameExisted_shouldThrow() {
        // Arrange
        UUID id = UUID.randomUUID();
        ExpenseCategory entity = ExpenseCategory.builder().name("Old").type(ExpenseCategoryType.SALARY).build();
        ExpenseCategoryRequest request = new ExpenseCategoryRequest("Rent", ExpenseCategoryType.SALARY);

        when(categoryRepository.findById(id)).thenReturn(Optional.of(entity));
        when(categoryRepository.existsByNameIgnoreCaseAndIdNot("Rent", id)).thenReturn(true);

        // Action - Assert
        assertThatThrownBy(() -> categoryService.update(id, request))
                .isInstanceOf(AppException.class);
        verify(categoryRepository, never()).save(any());
    }

    // UTCID04: kỳ vọng nghiệp vụ đúng, hiện tại sẽ FAIL
    @Test
    void update_sameNameChangeTypeOnly_shouldUpdate() {
        UUID id = UUID.randomUUID();
        ExpenseCategory entity = ExpenseCategory.builder().name("Rent").type(ExpenseCategoryType.SALARY).build();
        // đổi sang type khác SALARY của enum bạn đang có
        ExpenseCategoryRequest request = new ExpenseCategoryRequest("Rent", ExpenseCategoryType.OTHER);

        when(categoryRepository.findById(id)).thenReturn(Optional.of(entity));
        // repository thật sẽ trả true vì chính nó đang mang tên "Rent"
        when(categoryRepository.existsByNameIgnoreCaseAndIdNot("Rent", id)).thenReturn(false);
        when(categoryRepository.save(entity)).thenReturn(entity);

        assertThatCode(() -> categoryService.update(id, request)).doesNotThrowAnyException();
        assertThat(entity.getType()).isEqualTo(ExpenseCategoryType.OTHER);
    }

    // Delete
    // UTCID01
    @Test
    void delete_categoryNotInUse_shouldDelete() {
        UUID id = UUID.randomUUID();
        ExpenseCategory entity = ExpenseCategory.builder().name("Rent").build();

        when(categoryRepository.findById(id)).thenReturn(Optional.of(entity));
        when(voucherRepository.existsByExpenseCategoryId(id)).thenReturn(false);

        categoryService.delete(id);

        verify(categoryRepository).delete(entity);
    }

    // UTCID02
    @Test
    void delete_categoryNotFound_shouldThrow() {
        UUID id = UUID.randomUUID();

        when(categoryRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.delete(id))
                .isInstanceOf(AppException.class);
        verify(voucherRepository, never()).existsByExpenseCategoryId(any());
        verify(categoryRepository, never()).delete(any());
    }

    // UTCID03
    @Test
    void delete_categoryInUse_shouldThrow() {
        UUID id = UUID.randomUUID();
        ExpenseCategory entity = ExpenseCategory.builder().name("Rent").build();

        when(categoryRepository.findById(id)).thenReturn(Optional.of(entity));
        when(voucherRepository.existsByExpenseCategoryId(id)).thenReturn(true);

        assertThatThrownBy(() -> categoryService.delete(id))
                .isInstanceOf(AppException.class);
        verify(categoryRepository, never()).delete(any());
    }

    // Get all
    // UTCID01
    @Test
    void getAll_hasCategories_shouldReturnMappedList() {
        ExpenseCategory c1 = ExpenseCategory.builder().name("Rent").build();
        ExpenseCategory c2 = ExpenseCategory.builder().name("Electricity").build();
        ExpenseCategoryResponse r1 = mock(ExpenseCategoryResponse.class);
        ExpenseCategoryResponse r2 = mock(ExpenseCategoryResponse.class);

        when(categoryRepository.findAll()).thenReturn(List.of(c1, c2));
        when(mapper.toResponse(c1)).thenReturn(r1);
        when(mapper.toResponse(c2)).thenReturn(r2);

        List<ExpenseCategoryResponse> result = categoryService.getAll();

        assertThat(result).containsExactly(r1, r2);
    }

    // UTCID02
    @Test
    void getAll_noCategories_shouldReturnEmptyList() {
        when(categoryRepository.findAll()).thenReturn(List.of());

        List<ExpenseCategoryResponse> result = categoryService.getAll();

        assertThat(result).isEmpty();
        verifyNoInteractions(mapper);
    }

}
