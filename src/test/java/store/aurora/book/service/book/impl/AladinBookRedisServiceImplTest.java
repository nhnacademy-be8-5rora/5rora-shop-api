package store.aurora.book.service.book.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import store.aurora.book.dto.aladin.AladinBookRequestDto;
import store.aurora.book.util.RedisCacheManager;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AladinBookRedisServiceImplTest {

    @Mock
    private RedisCacheManager redisCacheManager;

    @InjectMocks
    private AladinBookRedisServiceImpl aladinBookRedisService;

    private static final String CACHE_KEY = "test_cache_key";
    private static final String BOOK_CACHE_KEY = "book:1234567890123";

    private AladinBookRequestDto sampleBook;

    @BeforeEach
    void setUp() {
        sampleBook = new AladinBookRequestDto();
        sampleBook.setTitle("테스트 책");
        sampleBook.setIsbn13("1234567890123");
    }

    @Test
    @DisplayName("storeBooks - 빈 리스트가 전달되면 Redis에 저장되지 않는다.")
    void storeBooks_ShouldNotStore_WhenBookListIsEmpty() {
        // When
        aladinBookRedisService.storeBooks(CACHE_KEY, List.of());

        // Then
        verify(redisCacheManager, never()).store(anyString(), any(), any());
    }

    @Test
    @DisplayName("storeBooks - 유효한 책 리스트를 Redis에 저장한다.")
    void storeBooks_ShouldStoreValidBooks() {
        // When
        aladinBookRedisService.storeBooks(CACHE_KEY, List.of(sampleBook));

        // Then
        verify(redisCacheManager, times(1)).store(eq(CACHE_KEY), anyList(), eq(Duration.ofMinutes(30)));
        verify(redisCacheManager, times(1)).store(eq(BOOK_CACHE_KEY), eq(sampleBook), eq(Duration.ofMinutes(30)));
    }

    @Test
    @DisplayName("storeIndividualBooks - 유효한 ISBN이 있는 책만 개별적으로 저장한다.")
    void storeIndividualBooks_ShouldStoreValidIsbnBooks() {
        // Given
        AladinBookRequestDto bookWithoutIsbn = new AladinBookRequestDto();
        bookWithoutIsbn.setTitle("책 제목");
        bookWithoutIsbn.setIsbn13(null); // ISBN 없음

        // When
        aladinBookRedisService.storeIndividualBooks(List.of(sampleBook, bookWithoutIsbn));

        // Then
        verify(redisCacheManager, times(1)).store(eq(BOOK_CACHE_KEY), eq(sampleBook), eq(Duration.ofMinutes(30)));
        verify(redisCacheManager, never()).store(eq("book:null"), any(), any());
    }

    @Test
    @DisplayName("getBooks - 캐시에서 책 리스트를 가져온다.")
    void getBooks_ShouldReturnCachedBooks() {
        // Given
        when(redisCacheManager.get(eq(CACHE_KEY), any(TypeReference.class)))
                .thenReturn(List.of(sampleBook));

        // When
        List<AladinBookRequestDto> books = aladinBookRedisService.getBooks(CACHE_KEY);

        // Then
        assertThat(books).isNotEmpty();
        assertThat(books).hasSize(1);
        assertThat(books.get(0).getTitle()).isEqualTo("테스트 책");
    }

    @Test
    @DisplayName("getBook - 캐시에서 개별 책 데이터를 가져온다.")
    void getBook_ShouldReturnCachedBook() {
        // Given
        when(redisCacheManager.get(eq(BOOK_CACHE_KEY), any(TypeReference.class)))
                .thenReturn(sampleBook);

        // When
        AladinBookRequestDto book = aladinBookRedisService.getBook(BOOK_CACHE_KEY);

        // Then
        assertThat(book).isNotNull();
        assertThat(book.getTitle()).isEqualTo("테스트 책");
        assertThat(book.getIsbn13()).isEqualTo("1234567890123");
    }
}