package store.aurora.book.repository.category;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import store.aurora.book.entity.category.Category;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    boolean existsByNameAndParentIsNull(String name); // 최상위 부모 간 이름 중복 검증

    boolean existsByNameAndParent(String name, Category parent);


    @EntityGraph(attributePaths = {"bookCategories.book"})
    Optional<Category> findCategoryWithBooksById(Long id);

    Page<Category> findByParentId(Long parentId,Pageable pageable);
    List<Category> findByParentIsNull();
    Page<Category> findByParentIsNull(Pageable pageable);

    List<Category> findByIdIn(List<Long> ids);

}

