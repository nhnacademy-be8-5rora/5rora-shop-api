package store.aurora.book.service.category.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import store.aurora.book.dto.category.CategoryRequestDTO;
import store.aurora.book.dto.category.CategoryResponseDTO;
import store.aurora.book.entity.Book;
import store.aurora.book.entity.category.BookCategory;
import store.aurora.book.entity.category.Category;
import store.aurora.book.exception.category.*;
import store.aurora.book.repository.category.BookCategoryRepository;
import store.aurora.book.repository.category.CategoryRepository;
import store.aurora.book.service.category.CategoryService;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;
    private final BookCategoryRepository bookCategoryRepository;

    @Override
    @Transactional
    public void createCategory(CategoryRequestDTO requestDTO) {
        Category parent = getParentCategory(requestDTO.getParentId());
        validateUniqueCategoryName(requestDTO.getName(), parent);

        Category category = buildCategory(requestDTO, parent);
        categoryRepository.save(category);
    }

    private Category buildCategory(CategoryRequestDTO requestDTO, Category parent) {
        Category category = new Category();
        category.setName(requestDTO.getName());
        category.setParent(parent);

        if (parent != null) {
            parent.addChild(category); // 부모의 depth를 기반으로 계산
        } else {
            category.setDepth(0); // 루트 카테고리
        }

        return category;
    }

    @Override
    @Transactional
    public void updateCategoryName(Long categoryId, String newName) {
        Category category = findCategoryByIdOrThrow(categoryId);

        checkNameInHierarchy(category, newName);

        validateUniqueCategoryName(newName, category.getParent());
        category.setName(newName);

        categoryRepository.save(category);
    }

    @Override
    @Transactional
    public void deleteCategory(Long categoryId) {
        Category category = findCategoryByIdOrThrow(categoryId);

        if (bookCategoryRepository.existsByCategoryId(categoryId)) {
            throw new CategoryLinkedToBooksException(categoryId);
        }

        if (!category.getChildren().isEmpty()) {
            throw new SubCategoryExistsException("하위 카테고리가 존재하므로 삭제할 수 없습니다.");
        }
        // 부모-자식 관계 끊기
        if (category.getParent() != null) {
            category.getParent().getChildren().remove(category); // 부모의 자식 목록에서 제거
        }

        categoryRepository.delete(category);
    }

    @Transactional(readOnly = true)
    @Override
    public Page<CategoryResponseDTO> getRootCategories(Pageable pageable) {
        return categoryRepository.findByParentIsNull(pageable)
                .map(this::mapToResponseDTO);
    }

    @Transactional(readOnly = true)
    @Override
    public Page<CategoryResponseDTO> getChildrenCategories(Long parentId, Pageable pageable) {
        return categoryRepository.findByParentId(parentId, pageable)
                .map(this::mapToResponseDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Book> getBooksByCategoryId(Long categoryId) {
        Category category = categoryRepository.findCategoryWithBooksById(categoryId)
                .orElseThrow(() -> new CategoryNotFoundException(categoryId));

        return category.getBookCategories().stream()
                .map(BookCategory::getBook)
                .toList();
    }

    @Override
    @Transactional
    public CategoryResponseDTO findById(Long categoryId) {
        Optional<Category> category = categoryRepository.findById(categoryId);
        CategoryResponseDTO categoryDTO = null;
        if(category.isPresent()) {
            Category categoryEntity = category.get();
            categoryDTO = new CategoryResponseDTO(
                    categoryEntity.getId(),
                    categoryEntity.getName(),
                    categoryEntity.getParent() != null ? categoryEntity.getParent().getId() : null,
                    categoryEntity.getParent() != null ? categoryEntity.getParent().getName() : null,
                    categoryEntity.getDepth(),
                    convertChildrenToResponseDTO(categoryEntity.getChildren())
            );
        }
        return categoryDTO;
    }

    @Transactional
    @Override
    public List<BookCategory> createBookCategories(List<Long> categoryIds) {
        if (categoryIds.isEmpty() || categoryIds.size() > 10) {
            throw new CategoryLimitException("카테고리는 최소 1개 이상, 최대 10개 이하만 선택할 수 있습니다.");
        }
        List<Category> categories = categoryRepository.findAllById(categoryIds);
        return categories.stream()
                .map(category -> {
                    BookCategory bookCategory = new BookCategory();
                    bookCategory.setCategory(category);
                    return bookCategory;
                })
                .toList();
    }

    @Transactional
    @Override
    public void updateBookCategories(Book book, List<Long> categoryIds) {
        // 현재 연결된 카테고리 ID 가져오기
        Set<Long> currentCategoryIds = book.getBookCategories().stream()
                .map(bookCategory -> bookCategory.getCategory().getId())
                .collect(Collectors.toSet());

        // 추가해야 할 카테고리와 제거해야 할 카테고리 계산
        Set<Long> newCategoryIds = new HashSet<>(categoryIds);
        Set<Long> categoriesToAdd = new HashSet<>(newCategoryIds);
        categoriesToAdd.removeAll(currentCategoryIds);

        Set<Long> categoriesToRemove = new HashSet<>(currentCategoryIds);
        categoriesToRemove.removeAll(newCategoryIds);

        // 데이터베이스에서 추가할 카테고리 조회
        List<Category> categoriesToAddEntities = categoryRepository.findAllById(categoriesToAdd);

        // 새로운 카테고리 추가
        categoriesToAddEntities.forEach(category -> {
            BookCategory bookCategory = new BookCategory();
            bookCategory.setCategory(category);
            book.addBookCategory(bookCategory);
        });

        // 기존 카테고리 중 제거
        book.getBookCategories().removeIf(bookCategory ->
                categoriesToRemove.contains(bookCategory.getCategory().getId()));
    }

    @Override
    public List<CategoryResponseDTO> getCategories() {
        // parent가 null인 카테고리들을 가져옴
        List<Category> categories = categoryRepository.findByParentIsNull();

        // Category를 CategoryResponseDTO로 변환
        return categories.stream()
                .map(category -> new CategoryResponseDTO(
                        category.getId(),
                        category.getName(),
                        category.getParent() != null ? category.getParent().getId() : null, // parent가 없을 경우 null 처리
                        category.getParent() != null ? category.getParent().getName() : null,
                        category.getDepth(),
                        convertChildrenToResponseDTO(category.getChildren()) // 자식 카테고리들도 변환
                ))
                .toList();
    }

    // 재귀적으로 자식 카테고리들을 CategoryResponseDTO로 변환하는 메서드
    private List<CategoryResponseDTO> convertChildrenToResponseDTO(List<Category> children) {
        return children.stream()
                .map(child -> new CategoryResponseDTO(
                        child.getId(),
                        child.getName(),
                        child.getParent() != null ? child.getParent().getId() : null, // parent가 없을 경우 null 처리
                        child.getParent() != null ? child.getParent().getName() : null,
                        child.getDepth(),
                        convertChildrenToResponseDTO(child.getChildren()) // 자식 카테고리가 있을 경우 재귀적으로 변환
                ))
                .toList();
    }

    private Category findCategoryByIdOrThrow(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CategoryNotFoundException(categoryId));
    }


    private Category getParentCategory(Long parentId) {
        if (parentId == null) {
            return null;
        }
        return categoryRepository.findById(parentId)
                .orElseThrow(() -> new CategoryNotFoundException(parentId));
    }

    private void validateUniqueCategoryName(String name, Category parent) {
        boolean existsInSameParent = (parent != null)
                ? categoryRepository.existsByNameAndParent(name, parent)
                : categoryRepository.existsByNameAndParentIsNull(name);
        if (existsInSameParent) {
            throw new CategoryAlreadyExistException("같은 상위 카테고리 아래에 동일한 이름의 카테고리가 이미 존재합니다.");
        }


        if (parent != null) {
            checkParentHierarchy(name, parent);
        }
    }
    private void checkParentHierarchy(String name, Category parent) {
        Category current = parent;
        while (current != null) {
            if (current.getName().equals(name)) {
                throw new CategoryAlreadyExistException("하위 카테고리의 이름은 상위 카테고리 이름과 중복될 수 없습니다.");
            }
            current = current.getParent();
        }
    }

    private void checkNameInHierarchy(Category parent, String newName) {
        Queue<Category> queue = new LinkedList<>();
        queue.addAll(parent.getChildren());

        while (!queue.isEmpty()) {
            Category current = queue.poll();

            // 하위 계층의 이름 중복 여부 확인
            if (current.getName().equals(newName)) {
                throw new CategoryAlreadyExistException("하위 계층에 동일한 이름이 존재합니다: " + newName);
            }

            // 현재 노드의 자식들을 큐에 추가
            queue.addAll(current.getChildren());
        }
    }
    private CategoryResponseDTO mapToResponseDTO(Category category) {
        CategoryResponseDTO dto = new CategoryResponseDTO();
        dto.setId(category.getId());
        dto.setName(category.getName());
        dto.setDepth(category.getDepth());
        dto.setParentId(category.getParent() != null ? category.getParent().getId() : null);
        dto.setParentName(category.getParent() != null ? category.getParent().getName() : null);
        return dto;
    }

}
