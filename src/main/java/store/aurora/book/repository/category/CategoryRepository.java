package store.aurora.book.repository.category;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import store.aurora.book.entity.category.Category;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    boolean existsByNameAndParentIsNull(String name); // 최상위 부모 간 이름 중복 검증

    boolean existsByNameAndParent(String name, Category parent);

    Page<Category> findByParentId(Long parentId,Pageable pageable);
    List<Category> findByParentId(Long parentId);
    List<Category> findByParentIsNull();
    Page<Category> findByParentIsNull(Pageable pageable);

    List<Category> findByIdIn(List<Long> ids);

}

