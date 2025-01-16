package store.aurora.search.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import store.aurora.book.entity.*;
import store.aurora.book.entity.category.BookCategory;
import store.aurora.book.entity.category.Category;
import store.aurora.book.entity.tag.BookTag;
import store.aurora.book.entity.tag.Tag;
import store.aurora.book.repository.BookAuthorRepository;
import store.aurora.book.repository.BookViewRepository;
import store.aurora.book.repository.LikeRepository;
import store.aurora.book.repository.category.BookCategoryRepository;
import store.aurora.book.repository.category.CategoryRepository;
import store.aurora.book.service.BookImageService;
import store.aurora.review.repository.ReviewRepository;
import store.aurora.review.service.ReviewService;
import store.aurora.search.dto.BookSearchResponseDTO;
import store.aurora.search.repository.ElasticSearchRepository;

import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

public class ElasticSearchServiceImplTest {

    @InjectMocks
    private ElasticSearchServiceImpl elasticSearchService;

    @Mock
    private BookViewRepository bookViewRepository;

    @Mock
    private LikeRepository likeRepository;

    @Mock
    private BookAuthorRepository bookAuthorRepository;

    @Mock
    private BookCategoryRepository bookCategoryRepository;

    @Mock
    private ElasticSearchRepository elasticSearchRepository;

    @Mock
    private BookImageService bookImageService;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ElasticsearchClient elasticsearchClient;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ReviewService reviewService;
    private static final String USER_ID = "testUserId";
    private static final String KEYWORD = "testKeyword";
    private static final Pageable PAGEABLE = PageRequest.of(0, 8);
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testSaveBooks() {
        // given
        Book book = createBookEntity();

        // Mocking BookImageService's getThumbnail() method to return a valid BookImage
        BookImage bookImage = new BookImage();
        bookImage.setFilePath("/images/book_cover.jpg");
        when(bookImageService.getThumbnail(any(Book.class))).thenReturn(bookImage);

        // when
        elasticSearchService.saveBook(book);

        // then
        verify(elasticSearchRepository, times(1)).save(any());
    }

    private Book createBookEntity() {
        // Publisher 객체 생성
        Publisher publisher = new Publisher();
        publisher.setId(1L);
        publisher.setName("Publisher Name");

        // Series 객체 생성 (선택적)
        Series series = new Series();
        series.setId(1L);
        series.setName("Series Name");

        // Book 객체 생성
        Book book = new Book();
        book.setId(1L);
        book.setTitle("New Book");
        book.setRegularPrice(10000);
        book.setSalePrice(8000);
        book.setStock(100);
        book.setSale(true);
        book.setIsbn("978-1234567890");
        book.setContents("Book contents");
        book.setExplanation("Book explanation");
        book.setPackaging(true);
        book.setActive(true);
        book.setPublishDate(LocalDate.now());
        book.setPublisher(publisher);
        book.setSeries(series);

        // BookImage 추가
        BookImage bookImage = new BookImage();
        bookImage.setFilePath("/images/book_cover.jpg");
        bookImage.setThumbnail(true);
        book.addBookImage(bookImage);

        // BookCategory 추가
        BookCategory bookCategory = new BookCategory();
        Category category = new Category();
        category.setId(1L);
        category.setName("Category Name");
        bookCategory.setCategory(category);
        book.addBookCategory(bookCategory);

        // BookTag 추가
        BookTag bookTag = new BookTag();
        Tag tag = new Tag();
        tag.setId(1L);
        tag.setName("Tag Name");
        bookTag.setTag(tag);
        book.addBookTag(bookTag);

        return book;
    }
    @Test
    void testSearchBooks_withNonFullTextSearchType() {
        // 1. type이 "fullText"가 아닌 경우
        Page<BookSearchResponseDTO> result = elasticSearchService.searchBooks("otherType", KEYWORD, PAGEABLE, USER_ID);

        // 2. 반환값이 null인지 검증
        assertTrue(result.isEmpty());  // "fullText"가 아니므로 null 반환되어야 함
    }
}
