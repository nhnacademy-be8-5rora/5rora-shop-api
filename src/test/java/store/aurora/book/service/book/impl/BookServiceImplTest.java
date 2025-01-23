package store.aurora.book.service.book.impl;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.*;

import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import store.aurora.book.dto.BookInfoDTO;
import store.aurora.book.dto.aladin.AladinBookRequestDto;
import store.aurora.book.dto.aladin.BookRequestDto;
import store.aurora.book.dto.aladin.BookResponseDto;
import store.aurora.book.entity.Book;

import store.aurora.book.entity.Publisher;
import store.aurora.book.entity.Series;
import store.aurora.book.exception.book.BookDtoNullException;
import store.aurora.book.exception.book.BookNotFoundException;
import store.aurora.book.exception.book.IsbnAlreadyExistsException;
import store.aurora.book.mapper.BookMapper;
import store.aurora.book.repository.book.BookRepository;
import store.aurora.book.repository.image.BookImageRepository;
import store.aurora.book.service.author.BookAuthorService;
import store.aurora.book.service.image.BookImageService;
import store.aurora.search.service.ElasticSearchService;

@ExtendWith(MockitoExtension.class)
class BookServiceImplTest {

    @Mock
    private BookRepository bookRepository;
    @Mock
    private BookImageService bookImageService;
    @Mock
    private BookAuthorService bookAuthorService;

    @Mock
    private BookImageRepository bookImageRepository;

    @Mock
    private ElasticSearchService elasticSearchService;
    @Mock
    private BookMapper bookMapper;

    @InjectMocks
    private BookServiceImpl bookService;

    private Book book;
    private BookRequestDto bookRequestDto;

    @BeforeEach
    void setUp() {
        Publisher publisher = new Publisher();
        publisher.setId(1L);
        publisher.setName("테스트 출판사");

        Series series = new Series();
        series.setId(1L);
        series.setName("테스트 시리즈");

        book = new Book();
        book.setId(1L);
        book.setTitle("테스트 책");
        book.setRegularPrice(20000);
        book.setSalePrice(15000);
        book.setStock(50);
        book.setSale(true);
        book.setIsbn("1234567890123");
        book.setContents("제1장: 소개");
        book.setExplanation("이 책은 테스트용입니다.");
        book.setPackaging(false);
        book.setActive(true);
        book.setPublishDate(LocalDate.of(2022, 1, 1));
        book.setPublisher(publisher);
        book.setSeries(series);

        bookRequestDto = new BookRequestDto();
        bookRequestDto.setTitle("테스트 책");
        bookRequestDto.setAuthor("테스트 저자");
        bookRequestDto.setDescription("이 책은 테스트용입니다.");
        bookRequestDto.setContents("제1장: 소개");
        bookRequestDto.setPublisher("테스트 출판사");
        bookRequestDto.setPubDate(LocalDate.of(2022, 1, 1));
        bookRequestDto.setIsbn("1234567890123");
        bookRequestDto.setPriceSales(15000);
        bookRequestDto.setPriceStandard(20000);
        bookRequestDto.setStock(50);
        bookRequestDto.setSale(true);
        bookRequestDto.setPackaging(false);
        bookRequestDto.setSeriesName("테스트 시리즈");
        bookRequestDto.setCategoryIds(List.of(1L, 2L));
        bookRequestDto.setTags("소설, 과학");
    }

    @Test
    @DisplayName("책 저장 성공 테스트")
    void saveBook_success() {
        when(bookMapper.toEntity(any())).thenReturn(book);
        when(bookRepository.save(any())).thenReturn(book);

        bookService.saveBook(bookRequestDto, null, Collections.emptyList());

        verify(bookRepository, times(1)).save(any());
        verify(bookImageService, times(1)).processBookImages(any(), any(), any(), any());
    }

    @Test
    @DisplayName("API에서 책 저장 성공 테스트")
    void saveBookFromApi_success() {
        AladinBookRequestDto aladinBookRequestDto = new AladinBookRequestDto();
        aladinBookRequestDto.setTitle("Aladin Test Book");
        aladinBookRequestDto.setIsbn("9876543210987");

        when(bookMapper.aladinToEntity(any())).thenReturn(book);
        when(bookRepository.save(any())).thenReturn(book);
        when(bookRepository.existsByIsbn(anyString())).thenReturn(false); // ISBN 중복 검사 통과하도록 설정

        bookService.saveBookFromApi(aladinBookRequestDto, Collections.emptyList());

        verify(bookRepository, times(1)).save(any());
        verify(bookAuthorService, times(1)).parseAndSaveBookAuthors(any(), any());
    }

    @Test
    @DisplayName("책 수정 성공 테스트")
    void updateBook_success() {
        when(bookRepository.findById(anyLong())).thenReturn(Optional.of(book));

        bookService.updateBook(1L, bookRequestDto, null, Collections.emptyList(), Collections.emptyList());

        verify(bookRepository, times(1)).findById(anyLong());
        verify(elasticSearchService, atMost(3)).saveBook(any()); // 최대 3회 재시도
    }

    @Test
    @DisplayName("책 활성/비활성 성공 테스트")
    void updateBookActivation_success() {
        when(bookRepository.findById(anyLong())).thenReturn(Optional.of(book));

        bookService.updateBookActivation(1L, false);

        assertThat(book.isActive()).isFalse();
        verify(bookRepository, times(1)).save(any());
        verify(elasticSearchService, times(1)).saveBook(any());
    }

    @Test
    @DisplayName("활성/비활성 책 조회 테스트")
    void getBooksByActive_success() {
        Page<Book> bookPage = new PageImpl<>(List.of(book));
        when(bookRepository.findByActive(anyBoolean(), any())).thenReturn(bookPage);
        when(bookMapper.toResponseDto(any())).thenReturn(new BookResponseDto());

        Page<BookResponseDto> result = bookService.getBooksByActive(true, Pageable.unpaged());

        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(bookRepository, times(1)).findByActive(anyBoolean(), any());
    }

    @Test
    @DisplayName("관리자용 책 상세 조회 성공 테스트")
    void getBookDetailsForAdmin_success() {
        when(bookRepository.existsById(anyLong())).thenReturn(true); // 존재하는 책이라고 가정
        when(bookRepository.findById(anyLong())).thenReturn(Optional.of(book));

        bookService.getBookDetailsForAdmin(1L);

        verify(bookRepository, times(1)).existsById(anyLong()); // 존재 여부 검증
        verify(bookRepository, times(1)).findById(anyLong()); // 책 조회
        verify(bookMapper, times(1)).toDetailDto(any()); // DTO 변환 확인
    }

    @Test
    @DisplayName("책 저장 실패 - ISBN 중복")
    void saveBook_fail_isbnDuplicate() {
        when(bookRepository.existsByIsbn(anyString())).thenReturn(true);

        assertThatThrownBy(() -> bookService.saveBook(bookRequestDto, null, Collections.emptyList()))
                .isInstanceOf(IsbnAlreadyExistsException.class);

        verify(bookRepository, never()).save(any());
    }

    @Test
    @DisplayName("책 저장 실패 - 존재하지 않는 책 수정")
    void updateBook_fail_notFound() {
        when(bookRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.updateBook(1L, bookRequestDto, null, Collections.emptyList(), Collections.emptyList()))
                .isInstanceOf(BookNotFoundException.class);
    }

    @Test
    @DisplayName("validateBookExists - 존재하지 않는 책 ID로 호출하면 예외 발생")
    void validateBookExists_fail() {
        when(bookRepository.existsById(anyLong())).thenReturn(false);

        assertThatThrownBy(() -> bookService.getBookDetailsForAdmin(1L))
                .isInstanceOf(BookNotFoundException.class);
    }

    @Test
    @DisplayName("validateBookDto - bookDto가 null이면 예외 발생")
    void validateBookDto_fail() {
        assertThatThrownBy(() -> bookService.saveBook(null, null, Collections.emptyList()))
                .isInstanceOf(BookDtoNullException.class);
    }

    @Test
    @DisplayName("getBookDetailsForAdmin - 존재하지 않는 책 조회 시 예외 발생")
    void getBookDetailsForAdmin_fail() {
        // 존재하지 않는 책 ID이므로 existsById를 true로 설정하고 findById는 empty 반환
        when(bookRepository.existsById(anyLong())).thenReturn(true);
        when(bookRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.getBookDetailsForAdmin(1L))
                .isInstanceOf(BookNotFoundException.class);

        verify(bookRepository, times(1)).existsById(anyLong()); // 존재 여부 확인
        verify(bookRepository, times(1)).findById(anyLong()); // 실제 조회 실행 확인
    }

    @Test
    @DisplayName("updateBook - deleteImageIds가 비어있지 않으면 bookImageService.deleteImages 호출")
    void updateBook_deleteImages_called() {
        when(bookRepository.findById(anyLong())).thenReturn(Optional.of(book));

        List<Long> deleteImageIds = List.of(1L, 2L, 3L);

        bookService.updateBook(1L, bookRequestDto, null, Collections.emptyList(), deleteImageIds);

        verify(bookImageService, times(1)).deleteImages(deleteImageIds);
    }


    @Test
    @DisplayName("책 조회 성공 테스트")
    void getBookById_success() {
        when(bookRepository.findById(anyLong())).thenReturn(Optional.of(book));

        Book result = bookService.getBookById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("테스트 책");
        verify(bookRepository, times(1)).findById(anyLong());
    }

    @Test
    @DisplayName("책 조회 실패 테스트 - 존재하지 않는 책")
    void getBookById_fail() {
        when(bookRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.getBookById(1L))
                .isInstanceOf(BookNotFoundException.class);
    }

    @Test
    @DisplayName("책 정보 조회 성공 테스트")
    void getBookInfo_success() {
        List<Long> bookIds = List.of(1L);
        when(bookRepository.findAllById(bookIds)).thenReturn(List.of(book));
        when(bookImageRepository.findByBook(any())).thenReturn(Collections.emptyList());

        List<BookInfoDTO> result = bookService.getBookInfo(bookIds);

        assertThat(result).isNotEmpty();
        assertThat(result.get(0).getTitle()).isEqualTo("테스트 책");
    }

    @Test
    @DisplayName("책 정보 조회 실패 테스트 - 빈 리스트 입력")
    void getBookInfo_fail() {
        List<Long> bookIds = Collections.emptyList();
        when(bookRepository.findAllById(bookIds)).thenReturn(Collections.emptyList());

        List<BookInfoDTO> result = bookService.getBookInfo(bookIds);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("존재하지 않는 책 ID 입력 시 예외 발생")
    void notExistThrow_fail() {
        when(bookRepository.existsById(anyLong())).thenReturn(false);

        assertThatThrownBy(() -> bookService.notExistThrow(1L))
                .isInstanceOf(BookNotFoundException.class);
    }



}