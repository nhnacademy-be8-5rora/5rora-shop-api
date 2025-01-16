package store.aurora.book.repository.category;

import org.springframework.data.jpa.repository.JpaRepository;
import store.aurora.book.entity.category.BookCategory;

import java.util.List;

public interface BookCategoryRepository extends JpaRepository<BookCategory, Long> {
    boolean existsByCategoryId(Long categoryId);
    List<BookCategory> findBookCategoryByBookId(Long bookId);
}


