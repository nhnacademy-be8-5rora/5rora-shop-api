package store.aurora.search.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import store.aurora.search.dto.BookSearchResponseDTO;
import store.aurora.search.service.ElasticSearchService;
import store.aurora.book.dto.AuthorDTO;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ElasticSearchControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ElasticSearchService elasticSearchService;

    @InjectMocks
    private ElasticSearchController elasticSearchController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(elasticSearchController).build();
    }

    @Test
    @DisplayName("성공적인 책 검색 결과 반환")
    void testGetBooks_Success() throws Exception {
        // Given
        String type = "fullText";
        String keyword = "포켓몬스터";
        int pageNum = 0;
        String userId = "user123";

        // Set up authors
        AuthorDTO author1 = new AuthorDTO("john doe", "Author");
        AuthorDTO author2 = new AuthorDTO("doe john", "Author");

        // Create BookSearchResponseDTO
        BookSearchResponseDTO book1 = new BookSearchResponseDTO(1L, "포켓몬스터 1", 10000, 8000,
                LocalDate.of(2022, 5, 10), "Publisher 1", "img1.jpg",
                List.of(author1), Arrays.asList(1L, 2L), 100L, 10, 4.5, false, true);

        BookSearchResponseDTO book2 = new BookSearchResponseDTO(2L, "포켓몬스터 2", 12000, 10000,
                LocalDate.of(2023, 1, 15), "Publisher 2", "img2.jpg",
                List.of(author2), Arrays.asList(2L, 3L), 150L, 20, 4.0, true, false);

        // Create a Page of BookSearchResponseDTO
        Page<BookSearchResponseDTO> page = new PageImpl<>(Arrays.asList(book1, book2), PageRequest.of(pageNum , 8), 2);

        // Mock service to return the Page
        when(elasticSearchService.searchBooks(type, keyword, PageRequest.of(pageNum , 8), userId)).thenReturn(page);

        // Create a custom ObjectMapper to handle the LocalDate format
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();  // This is important for LocalDate handling

        // When & Then
        mockMvc.perform(get("/api/books/search/elasticSearch")
                        .header("X-USER-ID", userId)
                        .param("type", type)
                        .param("keyword", keyword)
                        .param("pageNum", String.valueOf(pageNum))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(page)))  // Use ObjectMapper to serialize the response
                .andExpect(status().isOk())  // Expect 200 OK
                .andExpect(jsonPath("$.content[0].title").value("포켓몬스터 1"))
                .andExpect(jsonPath("$.content[1].title").value("포켓몬스터 2"))
                .andExpect(jsonPath("$.content[0].authors[0].name").value("john doe"))
                .andExpect(jsonPath("$.content[1].authors[0].name").value("doe john"));
    }

    @Test
    @DisplayName("잘못된 페이지 번호로 인한 400 오류 반환")
    void testGetBooks_BadRequest_InvalidPageNum() throws Exception {
        // Given
        int invalidPageNum = -1;

        // When & Then
        mockMvc.perform(get("/api/books/search/elasticSearch")
                        .param("pageNum", String.valueOf(invalidPageNum))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("검색 결과가 없을 때 204 No Content 반환 (null 반환)")
    void testGetBooks_NoContent_NullPage() throws Exception {
        // Given
        String type = "fullText";
        String keyword = "포켓몬스터";
        int pageNum = 0;
        String userId = "user123";

        // Mock service to return null
        when(elasticSearchService.searchBooks(type, keyword, PageRequest.of(pageNum , 8), userId))
                .thenReturn(null);

        // When & Then
        mockMvc.perform(get("/api/books/search/elasticSearch")
                        .header("X-USER-ID", userId)
                        .param("type", type)
                        .param("keyword", keyword)
                        .param("pageNum", String.valueOf(pageNum))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());  // Expect 204 No Content
    }
    @Test
    @DisplayName("검색 결과가 없을 때 204 No Content 반환 (empty 반환)")
    void testGetBooks_NoContent_EmptyPage() throws Exception {
        // Given
        String type = "fullText";
        String keyword = "포켓몬스터";
        int pageNum = 0;
        String userId = "user123";

        // Mock service to return null
        when(elasticSearchService.searchBooks(type, keyword, PageRequest.of(pageNum , 8), userId))
                .thenReturn(Page.empty());

        // When & Then
        mockMvc.perform(get("/api/books/search/elasticSearch")
                        .header("X-USER-ID", userId)
                        .param("type", type)
                        .param("keyword", keyword)
                        .param("pageNum", String.valueOf(pageNum))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());  // Expect 204 No Content
    }
}
