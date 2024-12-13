package store.aurora.search.controller;


import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import store.aurora.book.service.BookService;
import store.aurora.search.dto.BookSearchResponseDTO;
import store.aurora.search.service.SearchService;


@RestController
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;


    //디버그위한 요청 url : http://localhost:8083/api/books/search?type=title&keyword=한강&pageNum=1
    @GetMapping("/api/books/search")
    public ResponseEntity<Page<BookSearchResponseDTO>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String type,
            @RequestParam(required = false, defaultValue = "1") String pageNum) { // pageNum 기본값을 1로 설정

        int page = Integer.parseInt(pageNum) - 1; // pageNum은 1부터 시작하므로 1을 빼줘야 0-based 페이지로 맞춰짐
        PageRequest pageRequest = PageRequest.of(page, 8); // 페이지 사이즈는 8로 고정

        Page<BookSearchResponseDTO> bookSearchResponseDTOPage = null;
        if (type != null && keyword != null) {
            switch (type) {
                case "title":
                    // 제목으로 검색하는 로직을 처리
                    bookSearchResponseDTOPage = searchService.findBooksByTitleWithDetails(keyword, pageRequest);
                    break;
                case "category":
                    // 카테고리로 검색하는 로직을 처리
                    break;
                case "author":
                    // 작가로 검색하는 로직을 처리
                    bookSearchResponseDTOPage = searchService.findBooksByAuthorNameWithDetails(keyword, pageRequest);
                    break;
                default:
                    break;
            }
        }

        if (bookSearchResponseDTOPage == null) {
            return ResponseEntity.noContent().build();  // 결과가 없으면 204 No Content 반환
        }

        return ResponseEntity.ok(bookSearchResponseDTOPage);  // 결과가 있으면 200 OK 반환
    }



}