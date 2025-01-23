package store.aurora.book.service.category.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import store.aurora.book.dto.category.CategoryRequestDTO;
import store.aurora.book.dto.category.CategoryResponseDTO;
import store.aurora.book.entity.Book;
import store.aurora.book.entity.category.BookCategory;
import store.aurora.book.entity.category.Category;
import store.aurora.book.exception.category.CategoryAlreadyExistException;
import store.aurora.book.exception.category.CategoryLimitException;
import store.aurora.book.exception.category.CategoryLinkedToBooksException;
import store.aurora.book.exception.category.CategoryNotFoundException;
import store.aurora.book.repository.category.BookCategoryRepository;
import store.aurora.book.repository.category.CategoryRepository;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private BookCategoryRepository bookCategoryRepository;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    private Category rootCategory;
    private Category childCategory;
    private CategoryRequestDTO requestDTO;
    private Book book;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        rootCategory = new Category("Root Category", null, 0);
        rootCategory.setId(1L); // 수동 ID 설정

        childCategory = new Category("Child Category", rootCategory, 1);
        childCategory.setId(2L);

        requestDTO = new CategoryRequestDTO();
        requestDTO.setName("New Category");
        requestDTO.setParentId(1L);

        book = new Book();
        book.setId(10L);
        pageable = PageRequest.of(0, 5);

    }

    @Test
    void createCategory_Success() {
        // Given
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(rootCategory));
        when(categoryRepository.existsByNameAndParent(requestDTO.getName(), rootCategory)).thenReturn(false);

        // save() 호출 시 자동 증가된 ID를 설정하여 반환
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> {
            Category savedCategory = invocation.getArgument(0);
            savedCategory.setId(100L); // 저장 후 ID 설정
            return savedCategory;
        });

        // When
        categoryService.createCategory(requestDTO);

        // Then
        verify(categoryRepository, times(1)).save(any(Category.class));
    }

    @Test
    void createCategory_DuplicateName_ThrowsException() {
        // Given
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(rootCategory));
        when(categoryRepository.existsByNameAndParent("Duplicate Category", rootCategory)).thenReturn(true);

        CategoryRequestDTO requestDTO = new CategoryRequestDTO();
        requestDTO.setName("Duplicate Category");
        requestDTO.setParentId(1L);

        // When & Then
        assertThrows(CategoryAlreadyExistException.class, () -> categoryService.createCategory(requestDTO));
    }

    @Test
    void createCategory_ParentHierarchyDuplicateName_ThrowsException() {
        // Given
        rootCategory.setName("Root Category");
        childCategory.setName("Root Category"); // 동일한 이름 설정

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(rootCategory));
        when(categoryRepository.existsByNameAndParent("Root Category", rootCategory)).thenReturn(false);

        CategoryRequestDTO requestDTO = new CategoryRequestDTO();
        requestDTO.setName("Root Category");
        requestDTO.setParentId(1L);

        // When & Then
        assertThrows(CategoryAlreadyExistException.class, () -> categoryService.createCategory(requestDTO));
    }

    @Test
    void updateCategory_Success() {
        // Given
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(childCategory));
        when(categoryRepository.existsByNameAndParent("Updated Category", rootCategory)).thenReturn(false);

        // When
        categoryService.updateCategory(2L, "Updated Category");

        // Then
        assertThat(childCategory.getName()).isEqualTo("Updated Category");
        verify(categoryRepository, times(1)).save(childCategory);
    }

    @Test
    void updateCategory_DuplicateChildName_ThrowsException() {
        // Given
        rootCategory.getChildren().add(childCategory);
        childCategory.setName("Duplicate Category");

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(rootCategory));

        // When & Then
        assertThrows(CategoryAlreadyExistException.class, () -> categoryService.updateCategory(1L, "Duplicate Category"));
    }

    @Test
    void deleteCategory_Success() {
        // Given
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(childCategory));
        when(bookCategoryRepository.existsByCategoryId(2L)).thenReturn(false);

        // When
        categoryService.deleteCategory(2L);

        // Then
        verify(categoryRepository, times(1)).delete(childCategory);
    }

    @Test
    void deleteCategory_HasLinkedBooks_ThrowsException() {
        // Given
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(childCategory));
        when(bookCategoryRepository.existsByCategoryId(2L)).thenReturn(true);

        // When & Then
        assertThrows(CategoryLinkedToBooksException.class, () -> categoryService.deleteCategory(2L));
    }

    @Test
    void getAllRootCategories_Success() {
        // Given
        when(categoryRepository.findByParentIsNull()).thenReturn(List.of(rootCategory));

        // When
        List<CategoryResponseDTO> result = categoryService.getAllRootCategories();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getId()).isEqualTo(1L);
    }

    @Test
    void getAllChildrenCategories_Success() {
        // Given
        when(categoryRepository.findByParentId(1L)).thenReturn(List.of(childCategory));

        // When
        List<CategoryResponseDTO> result = categoryService.getAllChildrenCategories(1L);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getId()).isEqualTo(2L);
    }

    @Test
    void findById_Success() {
        // Given
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(rootCategory));

        // When
        CategoryResponseDTO result = categoryService.findById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Root Category");
    }

    @Test
    void findById_NotFound_ThrowsException() {
        // Given
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(CategoryNotFoundException.class, () -> categoryService.findById(99L));
    }

    @Test
    void createBookCategories_Success() {
        // Given
        List<Long> categoryIds = List.of(1L, 2L);
        when(categoryRepository.findAllById(categoryIds)).thenReturn(List.of(rootCategory, childCategory));

        // When
        List<BookCategory> result = categoryService.createBookCategories(categoryIds);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getCategory().getId()).isEqualTo(1L);
        assertThat(result.get(1).getCategory().getId()).isEqualTo(2L);
    }

    @Test
    void createBookCategories_TooManyCategories_ThrowsException() {
        // Given
        List<Long> tooManyCategories = List.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 11L);

        // When & Then
        assertThrows(CategoryLimitException.class, () -> categoryService.createBookCategories(tooManyCategories));
    }

    @Test
    void updateBookCategories_Success() {
        // Given
        BookCategory existingBookCategory = new BookCategory();
        existingBookCategory.setCategory(rootCategory);
        book.setBookCategories(new ArrayList<>(List.of(existingBookCategory)));
        List<Long> newCategoryIds = List.of(2L); // 새롭게 추가될 카테고리

        when(categoryRepository.findAllById(Set.of(2L))).thenReturn(List.of(childCategory));

        // When
        categoryService.updateBookCategories(book, newCategoryIds);

        // Then
        Set<Long> updatedCategoryIds = book.getBookCategories().stream()
                .map(bc -> bc.getCategory().getId())
                .collect(Collectors.toSet());

        assertThat(updatedCategoryIds).containsExactly(2L);
    }

    @Test
    void getCategories_Success() {
        // Given
        when(categoryRepository.findByParentIsNull()).thenReturn(List.of(rootCategory));

        // When
        List<CategoryResponseDTO> result = categoryService.getCategories();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getId()).isEqualTo(1L);
    }

    @Test
    void convertChildrenToResponseDTO_Success() {
        // Given
        rootCategory.getChildren().add(childCategory);

        when(categoryRepository.findByParentIsNull()).thenReturn(List.of(rootCategory));

        // When
        List<CategoryResponseDTO> result = categoryService.getCategories();

        // Then
        assertThat(result).isNotEmpty();
        assertThat(result.getFirst().getChildren()).hasSize(1);
        assertThat(result.getFirst().getChildren().getFirst().getId()).isEqualTo(2L);
    }
    @Test
    void getRootCategories_Success() {
        // Given
        Page<Category> categoryPage = new PageImpl<>(List.of(rootCategory), pageable, 1);
        when(categoryRepository.findByParentIsNull(pageable)).thenReturn(categoryPage);

        // When
        Page<CategoryResponseDTO> result = categoryService.getRootCategories(pageable);

        // Then
        assertThat(result).isNotEmpty();
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().getFirst().getId()).isEqualTo(1L);
        assertThat(result.getContent().getFirst().getName()).isEqualTo("Root Category");
    }
    @Test
    void getChildrenCategories_Success() {
        // Given
        Page<Category> categoryPage = new PageImpl<>(List.of(childCategory), pageable, 1);
        when(categoryRepository.findByParentId(1L, pageable)).thenReturn(categoryPage);

        // When
        Page<CategoryResponseDTO> result = categoryService.getChildrenCategories(1L, pageable);

        // Then
        assertThat(result).isNotEmpty();
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().getFirst().getId()).isEqualTo(2L);
        assertThat(result.getContent().getFirst().getName()).isEqualTo("Child Category");
    }


}