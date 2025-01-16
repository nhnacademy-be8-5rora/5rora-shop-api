package store.aurora.search.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import store.aurora.book.repository.BookRepository;
import store.aurora.book.service.LikeService;
import store.aurora.search.dto.BookSearchEntityDTO;
import store.aurora.search.dto.BookSearchResponseDTO;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class SearchServiceImplTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private LikeService likeService;

    @InjectMocks
    private SearchServiceImpl searchService;

    SearchServiceImplTest() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("검색 키워드가 null인 경우 빈 페이지 반환")
    void testFindBooksByKeywordWithNullKeyword() {
        Pageable pageable = mock(Pageable.class);
        Page<BookSearchResponseDTO> result = searchService.findBooksByKeywordWithDetails(null, "title", null, pageable);

        assertTrue(result.isEmpty(), "키워드가 null인 경우 결과는 빈 페이지여야 합니다.");
    }

    @Test
    @DisplayName("카테고리 ID로 검색: 유효한 ID인 경우 결과 반환")
    void testFindBooksByValidCategoryId() {
        Pageable pageable = mock(Pageable.class);
        Long categoryId = 1L;

        BookSearchEntityDTO book = new BookSearchEntityDTO();
        book.setId(1L);
        book.setTitle("Test Book");
        book.setRegularPrice(20000);
        book.setSalePrice(18000);
        book.setSale(true);
        book.setPublishDate(LocalDate.of(2023, 1, 15));
        book.setPublisherName("Test Publisher");
        book.setAuthors("example author(작가)");
        book.setCategoryIdList("1,2");
        book.setViewCount(100L);
        book.setReviewCount(5);
        book.setReviewRating(4.5);

        List<BookSearchEntityDTO> books = List.of(book);
        Page<BookSearchEntityDTO> bookPage = new PageImpl<>(books);

        when(bookRepository.findBooksByCategoryWithDetails(eq(categoryId), any(Pageable.class))).thenReturn(bookPage);

        Page<BookSearchResponseDTO> result = searchService.findBooksByKeywordWithDetails(null, "category", "1", pageable);

        assertFalse(result.isEmpty(), "카테고리 ID가 유효하면 결과가 반환되어야 합니다.");
        assertEquals(1, result.getContent().size(), "결과 크기는 1이어야 합니다.");
        assertEquals("Test Book", result.getContent().get(0).getTitle(), "결과 제목이 일치해야 합니다.");
        verify(bookRepository, times(1)).findBooksByCategoryWithDetails(eq(categoryId), any(Pageable.class));
    }

    @Test
    @DisplayName("카테고리 ID로 검색: 숫자가 아닌 경우 빈 페이지 반환")
    void testFindBooksByCategoryIdWithInvalidFormat() {
        Pageable pageable = mock(Pageable.class);

        Page<BookSearchResponseDTO> result = searchService.findBooksByKeywordWithDetails(null, "category", "invalidCategory", pageable);

        assertTrue(result.isEmpty(), "유효하지 않은 카테고리 ID는 빈 페이지를 반환해야 합니다.");
    }

    @Test
    @DisplayName("저자 이름으로 검색: 결과 반환 및 좋아요 상태 확인")
    void testFindBooksByAuthorName() {
        Pageable pageable = mock(Pageable.class);
        String authorName = "J.K. Rowling";
        String userId = "user123";  // 유저 ID 설정

        // 책 정보 설정
        BookSearchEntityDTO book = new BookSearchEntityDTO.Builder()
                .id(1L)
                .title("Harry Potter and the Sorcerer's Stone")
                .regularPrice(20000)
                .salePrice(15000)
                .isSale(true)
                .publishDate(LocalDate.of(1997, 6, 26))
                .publisherName("Scholastic")
                .authors("J.K. Rowling(작가)")  // 저자 문자열 설정
                .bookImagePath("imagePath.jpg")
                .categories("1,2,3")  // 카테고리 ID 설정
                .viewCount(100L)
                .reviewCount(200)
                .averageReviewRating(4.5)
                .build();

        // 책 목록을 페이지로 래핑
        List<BookSearchEntityDTO> books = List.of(book);
        Page<BookSearchEntityDTO> bookPage = new PageImpl<>(books);

        // bookRepository의 findBooksByAuthorNameWithDetails 메서드 동작 설정
        when(bookRepository.findBooksByAuthorNameWithDetails(eq(authorName), any(Pageable.class)))
                .thenReturn(bookPage);

        // likeService의 getLikedBookIds 메서드 동작 설정
        Set<Long> likedBookIds = Set.of(1L);  // 유저가 좋아요한 책 ID 설정
        when(likeService.getLikedBookIds(eq(userId), anyList())).thenReturn(likedBookIds);

        // 검색 서비스 호출
        Page<BookSearchResponseDTO> result = searchService.findBooksByKeywordWithDetails(
                userId, "author", authorName, pageable);

        // 결과 검증
        assertFalse(result.isEmpty(), "저자 이름이 유효하면 결과가 반환되어야 합니다.");
        assertEquals(1, result.getContent().size(), "결과 크기는 1이어야 합니다.");
        assertEquals("Harry Potter and the Sorcerer's Stone", result.getContent().get(0).getTitle(), "책 제목이 일치해야 합니다.");
        assertTrue(result.getContent().get(0).isLiked(), "책에 대해 좋아요가 눌려져 있어야 합니다.");
        verify(bookRepository, times(1)).findBooksByAuthorNameWithDetails(eq(authorName), any(Pageable.class));
        verify(likeService, times(1)).getLikedBookIds(eq(userId), anyList());
    }



}
