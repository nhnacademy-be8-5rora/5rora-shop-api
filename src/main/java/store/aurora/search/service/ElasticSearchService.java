package store.aurora.search.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import store.aurora.book.entity.Book;
import store.aurora.search.dto.BookSearchResponseDTO;

@Service
public interface ElasticSearchService {
    Page<BookSearchResponseDTO> searchBooks(String type,String keyword, Pageable pageable, String userId);
    void saveBook(Book book);
    Long saveBooksNotInElasticSearch();
}
