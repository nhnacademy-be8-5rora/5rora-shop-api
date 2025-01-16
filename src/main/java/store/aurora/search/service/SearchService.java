package store.aurora.search.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import store.aurora.search.dto.BookSearchResponseDTO;

public interface SearchService {
    // title로 책을 검색하며, 사용자 ID를 매개변수로 받아 좋아요 상태를 확인합니다.
    Page<BookSearchResponseDTO> findBooksByKeywordWithDetails(String userId,String type, String keyword, Pageable pageable);

}
