package store.aurora.book.controller.book;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;
import store.aurora.book.dto.AuthorDTO;
import store.aurora.book.dto.aladin.*;
import store.aurora.book.dto.category.CategoryResponseDTO;
import store.aurora.book.service.book.BookService;
import store.aurora.search.dto.BookSearchResponseDTO;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.hasSize;

@ExtendWith(SpringExtension.class)
@WebMvcTest(BookController.class)
class BookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BookService bookService;

    @Test
    @DisplayName("활성화된 도서 목록 조회 테스트 - 페이징 검증 (GET /api/books)")
    void getAllBooksTest() throws Exception {
        // given
        List<BookResponseDto> mockBooks = List.of(
                new BookResponseDto(1L, "책 1", "저자 1", "설명 1", "출판사 1", "2024-01-01", "1234567890123"),
                new BookResponseDto(2L, "책 2", "저자 2", "설명 2", "출판사 2", "2024-01-02", "1234567890124"),
                new BookResponseDto(3L, "책 3", "저자 3", "설명 3", "출판사 3", "2024-01-03", "1234567890125"),
                new BookResponseDto(4L, "책 4", "저자 4", "설명 4", "출판사 4", "2024-01-04", "1234567890126"),
                new BookResponseDto(5L, "책 5", "저자 5", "설명 5", "출판사 5", "2024-01-05", "1234567890127"),
                new BookResponseDto(6L, "책 6", "저자 6", "설명 6", "출판사 6", "2024-01-06", "1234567890128") // 2페이지로 넘어감
        );
        Page<BookResponseDto> mockPage = new PageImpl<>(mockBooks.subList(0, 5), PageRequest.of(0, 5), 6);

        when(bookService.getBooksByActive(eq(true), any(PageRequest.class))).thenReturn(mockPage);

        // when & then
        mockMvc.perform(get("/api/books")
                        .param("page", "0")
                        .param("size", "5")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(5)))
                .andExpect(jsonPath("$.totalElements").value(6))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content[0].id").value(1L));

        verify(bookService, times(1)).getBooksByActive(eq(true), any(PageRequest.class));
    }

    @Test
    @DisplayName("비활성화된 도서 목록 조회 테스트 - 페이징 검증 (GET /api/books/deactivate)")
    void getDeactivateBooksTest() throws Exception {
        // given
        List<BookResponseDto> mockBooks = List.of(
                new BookResponseDto(7L, "비활성 책 1", "저자 1", "설명 1", "출판사 1", "2024-01-07", "1234567890129"),
                new BookResponseDto(8L, "비활성 책 2", "저자 2", "설명 2", "출판사 2", "2024-01-08", "1234567890130"),
                new BookResponseDto(9L, "비활성 책 3", "저자 3", "설명 3", "출판사 3", "2024-01-09", "1234567890131"),
                new BookResponseDto(10L, "비활성 책 4", "저자 4", "설명 4", "출판사 4", "2024-01-10", "1234567890132"),
                new BookResponseDto(11L, "비활성 책 5", "저자 5", "설명 5", "출판사 5", "2024-01-11", "1234567890133"),
                new BookResponseDto(12L, "비활성 책 6", "저자 6", "설명 6", "출판사 6", "2024-01-12", "1234567890134") // 2페이지로 넘어감
        );
        Page<BookResponseDto> mockPage = new PageImpl<>(mockBooks.subList(0, 5), PageRequest.of(0, 5), 6);

        when(bookService.getBooksByActive(eq(false), any(PageRequest.class))).thenReturn(mockPage);

        // when & then
        mockMvc.perform(get("/api/books/deactivate")
                        .param("page", "0")
                        .param("size", "5")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(5)))
                .andExpect(jsonPath("$.totalElements").value(6))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content[0].id").value(7L));

        verify(bookService, times(1)).getBooksByActive(eq(false), any(PageRequest.class));
    }

    @Test
    @DisplayName("도서 생성 테스트 (POST /api/books)")
    void createBookTest() throws Exception {
        // given
        BookRequestDto requestDto = new BookRequestDto(
                "새로운 책",
                "테스트 저자",
                "이것은 책 설명입니다.",
                "테스트 출판사",
                LocalDate.of(2024, 1, 1),
                "9781234567890",
                15000,
                18000,
                List.of(1L, 2L)
        );

        doNothing().when(bookService).saveBook(any(BookRequestDto.class), any(MultipartFile.class), anyList());

        // when & then
        mockMvc.perform(multipart("/api/books")
                        .file("coverImage", new byte[]{})
                        .file("additionalImages", new byte[]{})
                        .param("title", requestDto.getTitle())
                        .param("author", requestDto.getAuthor())
                        .param("description", requestDto.getDescription())
                        .param("publisher", requestDto.getPublisher())
                        .param("pubDate", requestDto.getPubDate().toString()) // LocalDate -> String 변환
                        .param("isbn", requestDto.getIsbn())
                        .param("priceSales", String.valueOf(requestDto.getPriceSales()))
                        .param("priceStandard", String.valueOf(requestDto.getPriceStandard()))
                        .param("categoryIds", "1", "2") // 리스트는 개별 값으로 전달
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isCreated());

        verify(bookService, times(1)).saveBook(any(BookRequestDto.class), any(MultipartFile.class), anyList());
    }
    @Test
    @DisplayName("알라딘 API 도서 등록 테스트 (POST /api/books/aladin)")
    void createApiBookTest() throws Exception {
        // given
        AladinBookRequestDto requestDto = new AladinBookRequestDto(
                "알라딘 도서", // 제목
                "테스트 저자", // 저자
                "이것은 알라딘 API를 통해 등록된 도서입니다.", // 설명
                "목차 예시", // 목차
                "테스트 출판사", // 출판사
                "2024-01-01", // 출판 날짜
                "9781234567890", // ISBN
                "9781234567890", // ISBN13
                15000, // 판매 가격
                18000, // 정가 가격
                "https://example.com/cover.jpg", // 표지 이미지 URL
                new AladinBookRequestDto.SeriesInfo("테스트 시리즈"), // 시리즈 정보
                50, // 재고 수량
                true, // 판매 여부
                true, // 포장 가능 여부
                List.of(1L, 2L, 3L), // 카테고리 ID 리스트
                "태그 1, 태그 2, 태그 3" // 태그 목록
        );

        doNothing().when(bookService).saveBookFromApi(any(AladinBookRequestDto.class), anyList());

        // when & then
        mockMvc.perform(multipart("/api/books/aladin")
                        .file("additionalImages", new byte[]{})
                        .param("title", requestDto.getTitle())
                        .param("author", requestDto.getAuthor())
                        .param("description", requestDto.getDescription())
                        .param("contents", requestDto.getContents())
                        .param("publisher", requestDto.getPublisher())
                        .param("pubDate", requestDto.getPubDate()) // String 그대로 전달
                        .param("isbn", requestDto.getIsbn())
                        .param("isbn13", requestDto.getIsbn13())
                        .param("priceSales", String.valueOf(requestDto.getPriceSales()))
                        .param("priceStandard", String.valueOf(requestDto.getPriceStandard()))
                        .param("cover", requestDto.getCover())
                        .param("seriesInfo.seriesName", requestDto.getSeriesInfo().getSeriesName()) // 시리즈 정보
                        .param("stock", String.valueOf(requestDto.getStock()))
                        .param("isSale", String.valueOf(requestDto.isSale()))
                        .param("isPackaging", String.valueOf(requestDto.isPackaging()))
                        .param("categoryIds", "1", "2", "3") // 리스트 값 개별 전달
                        .param("tags", requestDto.getTags()) // 태그 목록
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isCreated());

        verify(bookService, times(1)).saveBookFromApi(any(AladinBookRequestDto.class), anyList());
    }

    @Test
    @DisplayName("도서 수정 데이터 반환 테스트 (GET /api/books/{book-id}/edit)")
    void getBookDetailsForAdminTest() throws Exception {
        // given
        Long bookId = 1L;
        BookDetailDto mockBookDetail = createMockBookDetail(bookId);

        when(bookService.getBookDetailsForAdmin(bookId)).thenReturn(mockBookDetail);

        // when & then
        mockMvc.perform(get("/api/books/{book-id}/edit", bookId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpectAll(
                        jsonPath("$.title").value(mockBookDetail.getTitle()),
                        jsonPath("$.author").value(mockBookDetail.getAuthor()),
                        jsonPath("$.description").value(mockBookDetail.getDescription()),
                        jsonPath("$.contents").value(mockBookDetail.getContents()),
                        jsonPath("$.publisher").value(mockBookDetail.getPublisher()),
                        jsonPath("$.pubDate").value(mockBookDetail.getPubDate().toString()),
                        jsonPath("$.isbn").value(mockBookDetail.getIsbn()),
                        jsonPath("$.priceSales").value(mockBookDetail.getPriceSales()),
                        jsonPath("$.priceStandard").value(mockBookDetail.getPriceStandard()),
                        jsonPath("$.cover.id").value(mockBookDetail.getCover().getId()),
                        jsonPath("$.cover.url").value(mockBookDetail.getCover().getUrl()),
                        jsonPath("$.existingAdditionalImages", hasSize(2)),
                        jsonPath("$.existingAdditionalImages[0].id").value(mockBookDetail.getExistingAdditionalImages().get(0).getId()),
                        jsonPath("$.existingAdditionalImages[0].url").value(mockBookDetail.getExistingAdditionalImages().get(0).getUrl()),
                        jsonPath("$.existingAdditionalImages[1].id").value(mockBookDetail.getExistingAdditionalImages().get(1).getId()),
                        jsonPath("$.existingAdditionalImages[1].url").value(mockBookDetail.getExistingAdditionalImages().get(1).getUrl()),
                        jsonPath("$.stock").value(mockBookDetail.getStock()),
                        jsonPath("$.sale").value(mockBookDetail.isSale()),
                        jsonPath("$.packaging").value(mockBookDetail.isPackaging()),
                        jsonPath("$.seriesName").value(mockBookDetail.getSeriesName()),
                        jsonPath("$.categories", hasSize(2)),
                        jsonPath("$.categories[0].id").value(mockBookDetail.getCategories().get(0).getId()),
                        jsonPath("$.categories[0].name").value(mockBookDetail.getCategories().get(0).getName()),
                        jsonPath("$.categories[1].id").value(mockBookDetail.getCategories().get(1).getId()),
                        jsonPath("$.categories[1].name").value(mockBookDetail.getCategories().get(1).getName()),
                        jsonPath("$.tags").value(mockBookDetail.getTags())
                );

        verify(bookService, times(1)).getBookDetailsForAdmin(bookId);
    }
    @Test
    @DisplayName("도서 수정 테스트 (PUT /api/books/{book-id})")
    void editBookTest() throws Exception {
        // given
        Long bookId = 1L;
        BookRequestDto requestDto = new BookRequestDto(
                "수정된 책 제목",
                "수정된 저자",
                "수정된 설명",
                "수정된 출판사",
                LocalDate.of(2024, 1, 2),
                "9781234567890",
                16000,
                19000,
                List.of(1L, 2L)
        );

        doNothing().when(bookService).updateBook(eq(bookId), any(BookRequestDto.class), any(MultipartFile.class), anyList(), anyList());

        // when & then
        mockMvc.perform(multipart("/api/books/{book-id}", bookId)
                        .file("coverImage", new byte[]{}) // 빈 이미지 파일
                        .file("additionalImages", new byte[]{})
                        .param("title", requestDto.getTitle())
                        .param("author", requestDto.getAuthor())
                        .param("description", requestDto.getDescription())
                        .param("publisher", requestDto.getPublisher())
                        .param("pubDate", requestDto.getPubDate().toString()) // LocalDate -> String 변환
                        .param("isbn", requestDto.getIsbn())
                        .param("priceSales", String.valueOf(requestDto.getPriceSales()))
                        .param("priceStandard", String.valueOf(requestDto.getPriceStandard()))
                        .param("categoryIds", "1", "2") // 리스트 값 전달
                        .param("deleteImages", "3", "4") // 삭제할 이미지 ID
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .with(request -> {
                            request.setMethod("PUT"); // PUT 요청으로 변경
                            return request;
                        }))
                .andExpect(status().isNoContent());

        verify(bookService, times(1)).updateBook(eq(bookId), any(BookRequestDto.class), any(MultipartFile.class), anyList(), anyList());
    }

    @Test
    @DisplayName("도서 활성화 테스트 (POST /api/books/{book-id}/activate)")
    void activateBookTest() throws Exception {
        // given
        Long bookId = 1L;
        doNothing().when(bookService).updateBookActivation(bookId, true);

        // when & then
        mockMvc.perform(post("/api/books/{book-id}/activate", bookId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        verify(bookService, times(1)).updateBookActivation(bookId, true);
    }

    @Test
    @DisplayName("도서 비활성화 테스트 (POST /api/books/{book-id}/deactivate)")
    void deactivateBookTest() throws Exception {
        // given
        Long bookId = 1L;
        doNothing().when(bookService).updateBookActivation(bookId, false);

        // when & then
        mockMvc.perform(post("/api/books/{book-id}/deactivate", bookId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        verify(bookService, times(1)).updateBookActivation(bookId, false);
    }

    @Test
    @DisplayName("유저가 좋아요 누른 책 리스트 반환 테스트")
    void getBooksByLikeTest() throws Exception {
        // given
        String userId = "test-user";
        int pageNum = 1;
        PageRequest pageable = PageRequest.of(0, 8);

        Page<BookSearchResponseDTO> bookPage = getBookSearchResponseDTOPage(pageable);

        when(bookService.getBooksByLike(eq(userId), any(PageRequest.class))).thenReturn(bookPage);

        // when & then
        mockMvc.perform(get("/api/books/my-like-books")
                        .header("X-USER-ID", userId)
                        .param("pageNum", String.valueOf(pageNum)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].title").value("책 제목1"))
                .andExpect(jsonPath("$.content[0].publisherName").value("출판사1"))
                .andExpect(jsonPath("$.content[0].authors[0].name").value("작가1"))
                .andExpect(jsonPath("$.content[0].liked").value(true))
                .andExpect(jsonPath("$.content[1].title").value("책 제목2"))
                .andExpect(jsonPath("$.content[1].publisherName").value("출판사2"))
                .andExpect(jsonPath("$.content[1].authors[0].name").value("작가2"))
                .andExpect(jsonPath("$.content[1].liked").value(false));

        verify(bookService, times(1)).getBooksByLike(eq(userId), any(PageRequest.class));
    }

    private static Page<BookSearchResponseDTO> getBookSearchResponseDTOPage(PageRequest pageable) {
        List<BookSearchResponseDTO> mockBooks = List.of(
                new BookSearchResponseDTO(1L, "책 제목1", 15000, 12000, LocalDate.of(2024, 1, 10),
                        "출판사1", "img1.jpg", List.of(new AuthorDTO("작가1", "저자")),
                        List.of(10L, 20L), 500L, 25, 4.5, true, true),

                new BookSearchResponseDTO(2L, "책 제목2", 18000, 14000, LocalDate.of(2023, 11, 5),
                        "출판사2", "img2.jpg", List.of(new AuthorDTO("작가2", "역자")),
                        List.of(30L, 40L), 800L, 40, 4.8, false, false)
        );

        return new PageImpl<>(mockBooks, pageable, mockBooks.size());
    }

    @Test
    @DisplayName("직전 달 가장 많이 팔린 책 반환 테스트 (책이 있는 경우)")
    void getMostSoldBooksByPreviousMonthTest() throws Exception {
        // given
        BookSearchResponseDTO mockBook = new BookSearchResponseDTO(3L, "베스트셀러", 20000, 18000, LocalDate.of(2024, 1, 1),
                "베스트출판사", "best_img.jpg", List.of(new AuthorDTO("베스트 작가", "저자")),
                List.of(50L), 1000L, 150, 4.9, false, true);

        when(bookService.findMostSoldByLastMonth()).thenReturn(Optional.of(mockBook));

        // when & then
        mockMvc.perform(get("/api/books/most-sold"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("베스트셀러"))
                .andExpect(jsonPath("$.publisherName").value("베스트출판사"))
                .andExpect(jsonPath("$.authors[0].name").value("베스트 작가"))
                .andExpect(jsonPath("$.reviewCount").value(150))
                .andExpect(jsonPath("$.reviewRating").value(4.9));

        verify(bookService, times(1)).findMostSoldByLastMonth();
    }

    @Test
    @DisplayName("직전 달 가장 많이 팔린 책 반환 테스트 (책이 없는 경우)")
    void getMostSoldBooksByPreviousMonthNotFoundTest() throws Exception {
        // given
        when(bookService.findMostSoldByLastMonth()).thenReturn(Optional.empty());

        // when & then
        mockMvc.perform(get("/api/books/most-sold"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("책을 찾을 수 없습니다."));

        verify(bookService, times(1)).findMostSoldByLastMonth();
    }

    private BookDetailDto createMockBookDetail(Long bookId) {
        BookDetailDto bookDetailDto = new BookDetailDto();
        bookDetailDto.setTitle("도서 제목");
        bookDetailDto.setAuthor("테스트 저자");
        bookDetailDto.setDescription("이것은 도서 설명입니다.");
        bookDetailDto.setContents("목차 예시");
        bookDetailDto.setPublisher("테스트 출판사");
        bookDetailDto.setPubDate(LocalDate.of(2024, 1, 1));
        bookDetailDto.setIsbn("9781234567890");
        bookDetailDto.setPriceSales(15000);
        bookDetailDto.setPriceStandard(18000);
        bookDetailDto.setCover(new ImageDetail(1L, "https://example.com/cover.jpg"));
        bookDetailDto.setExistingAdditionalImages(List.of(
                new ImageDetail(2L, "https://example.com/additional1.jpg"),
                new ImageDetail(3L, "https://example.com/additional2.jpg")
        ));
        bookDetailDto.setStock(50);
        bookDetailDto.setSale(true);
        bookDetailDto.setPackaging(false);
        bookDetailDto.setSeriesName("테스트 시리즈");
        bookDetailDto.setCategories(List.of(
                new CategoryResponseDTO(1L, "카테고리 1"),
                new CategoryResponseDTO(2L, "카테고리 2")
        ));
        bookDetailDto.setTags("태그 1, 태그 2, 태그 3");

        return bookDetailDto;
    }

}