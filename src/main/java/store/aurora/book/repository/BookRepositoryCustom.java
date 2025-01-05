package store.aurora.book.repository;


import com.querydsl.core.Tuple;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import store.aurora.book.dto.category.BookCategoryDto;
import store.aurora.book.dto.category.CategoryResponseDTO;
import store.aurora.book.dto.BookDetailsDto;
import store.aurora.book.dto.BookImageDto;
import store.aurora.book.dto.ReviewDto;
import store.aurora.book.entity.Book;
import store.aurora.search.dto.BookSearchEntityDTO;

import java.util.List;

public interface BookRepositoryCustom {
    Page<BookSearchEntityDTO> findBooksByTitleWithDetails(String title, Pageable pageable);
    Page<BookSearchEntityDTO> findBooksByAuthorNameWithDetails(String name, Pageable pageable);
    Page<BookSearchEntityDTO> findBooksByCategoryWithDetails(Long categoryId, Pageable pageable);
    BookDetailsDto findBookDetailsByBookId(Long bookId);
    List<BookImageDto> findBookImagesByBookId(Long bookId);
    List<ReviewDto> findReviewsByBookId(Long bookId);
    List<BookCategoryDto> findCategoryPathByBookId(Long bookId);
    Page<BookSearchEntityDTO> findBookByIdIn(List<Long> bookId,Pageable pageable);
    Tuple findMostSoldBook();
}

