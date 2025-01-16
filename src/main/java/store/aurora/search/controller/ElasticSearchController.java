package store.aurora.search.controller;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import store.aurora.search.dto.BookSearchResponseDTO;
import store.aurora.search.service.ElasticSearchService;



@RestController
@RequestMapping("/api/books/search/elasticSearch")
@RequiredArgsConstructor
public class ElasticSearchController {

    private final ElasticSearchService elasticSearchService;

    @GetMapping
    public ResponseEntity<Page<BookSearchResponseDTO>> getBooks(@RequestHeader(name ="X-USER-ID",required = false) String userId,
                                           @RequestParam(required = false) String type,
                                           @RequestParam(required = false) String keyword,
                                           @RequestParam(required = false,defaultValue = "1") int pageNum) {

        //front에서 이미 -1을 한 상태로 pageNum을 보내줌.
        if (pageNum < 0) {
            return ResponseEntity.badRequest().build();
        }
        PageRequest pageRequest = PageRequest.of(pageNum,8);
        Page<BookSearchResponseDTO> books = elasticSearchService.searchBooks(type,keyword,pageRequest,userId);
        if(books==null || books.isEmpty())
        {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(books);
    }
    //db에는 저장됐지만 , 엘라스틱서치에는 저장안된 책들 모두 저장하는것
    // todo 일단 size를 1000개 고정으로 만들어둠. 입력받아서 하도록 바꾸어야함,
    @PostMapping("/sync")
    public ResponseEntity<Long> syncBooksToElasticSearch() {
        try {
            // 엘라스틱서치에 데이터 저장
            Long count = elasticSearchService.saveBooksNotInElasticSearch();

            return ResponseEntity.ok(count);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
