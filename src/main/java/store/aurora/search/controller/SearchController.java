package store.aurora.search.controller;


import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import store.aurora.search.dto.BookSearchResponseDTO;
import store.aurora.search.service.SearchService;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;
    private static final Logger USER_LOG = LoggerFactory.getLogger("user-logger");

    @GetMapping("/api/books/search")
    public ResponseEntity<Page<BookSearchResponseDTO>> search(
            @RequestHeader(value = "X-USER-ID", required = false) String userId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String orderBy,
            @RequestParam(required = false) String orderDirection,
            @RequestParam(required = false, defaultValue = "1") String pageNum) {

        int page = validateAndParsePageNum(pageNum);
        if (page < 0) {
            return ResponseEntity.noContent().build();
        }

        PageRequest pageRequest = createPageRequest(page, orderBy, orderDirection);

        try {
            Page<BookSearchResponseDTO> bookSearchResponseDTOPage = searchService.findBooksByKeywordWithDetails(userId, type, keyword, pageRequest);
            if (bookSearchResponseDTOPage == null || bookSearchResponseDTOPage.isEmpty()) {
                USER_LOG.info("조회 결과가 존재하지않습니다.");
                return ResponseEntity.noContent().build();
            }
            // PageImpl을 사용하여 반환
            List<BookSearchResponseDTO> content = bookSearchResponseDTOPage.getContent();
            long totalElements = bookSearchResponseDTOPage.getTotalElements();
            Page<BookSearchResponseDTO> pageResult = new PageImpl<>(content, pageRequest, totalElements);

            return ResponseEntity.ok(pageResult);
        } catch (IllegalArgumentException e) {
            USER_LOG.error(e.getMessage(), e);
            return ResponseEntity.status(500).build(); // 500 Internal Server Error for unexpected errors
        }
    }

    private int validateAndParsePageNum(String pageNum) {
        try {
            int page = Integer.parseInt(pageNum);
            return Math.max(page, 0);
        } catch (NumberFormatException e) {
            return -1; // 잘못된 pageNum 값 처리
        }
    }

    private PageRequest createPageRequest(int page, String orderBy, String orderDirection) {
        if (orderBy == null || orderBy.isEmpty()) {
            orderBy = "id";
        }
        Sort.Order order = "desc".equalsIgnoreCase(orderDirection)
                ? Sort.Order.desc(orderBy)
                : Sort.Order.asc(orderBy);
        return PageRequest.of(page, 8, Sort.by(order));
    }
}
