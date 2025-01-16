package store.aurora.search.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import store.aurora.book.dto.AuthorDTO;
import store.aurora.search.dto.BookSearchResponseDTO;
import store.aurora.search.service.SearchService;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SearchController.class)
class SearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SearchService searchService;

    private static final String BASE_URL = "/api/books/search";

    private PageRequest createPageRequest(int pageNum) {
        return PageRequest.of(pageNum, 8, Sort.by(Sort.Order.asc("id")));
    }

    @DisplayName("제목을 기준으로 검색할 때 내용과 상태코드가 잘 넘어오는지 확인")
    @Test
    void searchByTitle_ReturnsResults() throws Exception {
        // Arrange
        String keyword = "Example Title";
        String type = "title";
        int pageNum = 1;
        List<AuthorDTO> authors = Arrays.asList(new AuthorDTO("John Doe", "Writer"));
        PageRequest pageRequest = createPageRequest(pageNum);
        BookSearchResponseDTO responseDTO = new BookSearchResponseDTO(
                1L, "Example Title", 1000, 900, LocalDate.now(),
                "Example Publisher", "test.jpeg", authors, null, 5L, 3, 3.5, true, true
        );
        Page<BookSearchResponseDTO> page = new PageImpl<>(Collections.singletonList(responseDTO));

        when(searchService.findBooksByKeywordWithDetails(any(), eq(type), eq(keyword), any()))
                .thenReturn(page);

        // Act & Assert
        mockMvc.perform(get(BASE_URL)
                        .param("type", type)
                        .param("keyword", keyword)
                        .param("pageNum", String.valueOf(pageNum))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id", is(1)))
                .andExpect(jsonPath("$.content[0].title", is("Example Title")));

        verify(searchService, times(1)).findBooksByKeywordWithDetails(any(), eq(type), eq(keyword), eq(pageRequest));
    }

    @DisplayName("검색 결과가 없을 때 No Content 상태 코드 반환 확인")
    @Test
    void searchByTitle_NoResults() throws Exception {
        // Arrange
        String keyword = "Nonexistent Title";
        String type = "title";
        int pageNum = 1;
        Page<BookSearchResponseDTO> emptyPage = Page.empty();

        when(searchService.findBooksByKeywordWithDetails(any(), eq(type), eq(keyword), any()))
                .thenReturn(emptyPage);

        // Act & Assert
        mockMvc.perform(get(BASE_URL)
                        .param("type", type)
                        .param("keyword", keyword)
                        .param("pageNum", String.valueOf(pageNum))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        verify(searchService, times(1)).findBooksByKeywordWithDetails(any(), eq(type), eq(keyword), any());
    }

    @DisplayName("잘못된 페이지 번호로 검색 시 No Content 상태 코드 반환 확인")
    @Test
    void searchByTitle_InvalidPageNumber() throws Exception {
        // Act & Assert
        mockMvc.perform(get(BASE_URL)
                        .param("pageNum", "invalid")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        verify(searchService, times(0)).findBooksByKeywordWithDetails(any(), any(), any(), any());
    }

    @DisplayName("검색 중 서비스 예외 발생 시 Internal Server Error 반환 확인")
    @Test
    void searchByTitle_ServiceThrowsException() throws Exception {
        // Arrange
        String keyword = "Example Title";
        String type = "title";
        int pageNum = 1;

        when(searchService.findBooksByKeywordWithDetails(any(), eq(type), eq(keyword), any()))
                .thenThrow(new IllegalArgumentException("Service Exception"));

        // Act & Assert
        mockMvc.perform(get(BASE_URL)
                        .param("type", type)
                        .param("keyword", keyword)
                        .param("pageNum", String.valueOf(pageNum))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());

        verify(searchService, times(1)).findBooksByKeywordWithDetails(any(), eq(type), eq(keyword), any());
    }

    @DisplayName("정렬 방향이 desc일 때 올바르게 정렬 기준을 적용하는지 확인")
    @Test
    void searchWithDescendingOrder() throws Exception {
        // Arrange
        String keyword = "Example Title";
        String type = "title";
        int pageNum = 1;
        String orderBy = "price";
        String orderDirection = "desc";
        PageRequest pageRequest = PageRequest.of(pageNum, 8, Sort.by(Sort.Order.desc(orderBy)));

        BookSearchResponseDTO responseDTO = new BookSearchResponseDTO(
                1L, "Example Title", 1000, 900, LocalDate.now(),
                "Example Publisher", "test.jpeg", null, null, 5L, 3, 3.5, true, true
        );
        Page<BookSearchResponseDTO> page = new PageImpl<>(Collections.singletonList(responseDTO));

        when(searchService.findBooksByKeywordWithDetails(any(), eq(type), eq(keyword), eq(pageRequest)))
                .thenReturn(page);

        // Act & Assert
        mockMvc.perform(get(BASE_URL)
                        .param("type", type)
                        .param("keyword", keyword)
                        .param("pageNum", String.valueOf(pageNum))
                        .param("orderBy", orderBy)
                        .param("orderDirection", orderDirection)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id", is(1)))
                .andExpect(jsonPath("$.content[0].title", is("Example Title")));

        verify(searchService, times(1)).findBooksByKeywordWithDetails(any(), eq(type), eq(keyword), eq(pageRequest));
    }

}
