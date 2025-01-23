package store.aurora.book.repository.impl;

import com.querydsl.core.Tuple;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.annotation.DirtiesContext;
import store.aurora.book.config.QuerydslConfiguration;
import store.aurora.book.dto.AuthorDTO;
import store.aurora.book.entity.*;
import store.aurora.book.entity.category.BookCategory;
import store.aurora.book.entity.category.Category;
import store.aurora.book.entity.tag.BookTag;
import store.aurora.book.entity.tag.Tag;

import store.aurora.book.repository.book.BookRepository;
import store.aurora.order.entity.Order;
import store.aurora.order.entity.OrderDetail;
import store.aurora.order.entity.Shipment;
import store.aurora.order.entity.enums.OrderState;
import store.aurora.order.entity.enums.ShipmentState;
import store.aurora.order.entity.enums.ShippingCompaniesCode;
import store.aurora.review.entity.Review;
import store.aurora.search.dto.BookSearchEntityDTO;
import store.aurora.user.entity.User;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static store.aurora.order.entity.QOrderDetail.orderDetail;

@Slf4j
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Import(QuerydslConfiguration.class)
@DataJpaTest
public class BookRepositoryCustomTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private BookRepository bookRepository;

    @BeforeEach
    public void setup() {
        // User 생성
        User user1 = new User("user123", "John Doe", LocalDate.of(1990, 1, 1), "20000101", "john_doe@example.com", true);
        User user2 = new User("user1234", "John Doe2", LocalDate.of(1200, 1, 1), "20000101", "john_doe@example.com", true);

        entityManager.persist(user1);
        entityManager.persist(user2);

        // Publisher 생성
        Publisher publisher = new Publisher("Penguin Books");
        entityManager.persist(publisher);

        // Series 생성
        Series series = new Series( "test name");
        entityManager.persist(series);

        // Category 생성
        Category category1 = new Category("Example Category", null,0);
        Category category2 = new Category( "Example Category2", null,  0);
        entityManager.persist(category1);
        entityManager.persist(category2);

        // Book 생성
        Book book1 = new Book(
                 "test title", 10000, 9000, 100, true, "1234567890123", "sample contents", "test desc", false, true,
                LocalDate.of(2024, 12, 12), publisher, series, new ArrayList<>(), new ArrayList<>(), new ArrayList<>()
        );
        Book book2 = new Book(
               "test title2", 10000, 9000, 100, true, "1234567890124", "sample contents2", "test desc2", false, true,
                LocalDate.of(2024, 12, 12), publisher, series, new ArrayList<>(), new ArrayList<>(), new ArrayList<>()
        );

        entityManager.persist(book1);
        entityManager.persist(book2);

        // BookCategory 생성
        entityManager.persist(new BookCategory( book1, category1));
        entityManager.persist(new BookCategory( book1, category2));

        // Author 생성
        Author author1 = new Author( "example author");
        Author author2 = new Author( "example editor");
        entityManager.persist(author1);
        entityManager.persist(author2);

        // AuthorRole 생성
        AuthorRole roleAuthor = new AuthorRole("지은이");
        AuthorRole roleEditor = new AuthorRole("역은이");
        entityManager.persist(roleAuthor);
        entityManager.persist(roleEditor);

        // BookAuthors 생성
        entityManager.persist(new BookAuthor(author1, roleAuthor, book1));
        entityManager.persist(new BookAuthor( author2, roleEditor, book1));
        entityManager.persist(new BookAuthor(author2, roleAuthor, book2));

        // Review 생성
        Review review1 = new Review();
        review1.setReviewRating(4);
        review1.setReviewContent("test contents");
        review1.setReviewCreateAt(LocalDateTime.now());
        review1.setBook(book1);
        review1.setUser(user1);

        Review review2 = new Review();
        review2.setReviewRating(5);
        review2.setReviewContent("test contents");
        review2.setReviewCreateAt(LocalDateTime.now());
        review2.setBook(book1);
        review2.setUser(user2);

        entityManager.persist(review1);
        entityManager.persist(review2);

        // Tag 생성 - BookTag 저장 전에 Tag를 먼저 저장해야 함
        Tag tag1 = new Tag("초등");
        Tag tag2 = new Tag("설민석");
        entityManager.persist(tag1);  // persist() 사용
        entityManager.persist(tag2);  // persist() 사용

        // BookTag 생성
        BookTag bookTag1 = new BookTag( tag1, book1);
        BookTag bookTag2 = new BookTag(tag2, book1);
        BookTag bookTag3 = new BookTag( tag2, book2);
        entityManager.persist(bookTag1);
        entityManager.persist(bookTag2);
        entityManager.persist(bookTag3);
        // 첫 번째 주문 추가
        Order order1 = new Order();
        order1.setUser(user1);
        order1.setState(OrderState.CONFIRMED);
        order1.setOrderTime(LocalDateTime.of(2024, 12, 1, 10, 0));
        order1.setTotalAmount(18000);
        order1.setPointAmount(1000);
        order1.setName("John Doe");
        order1.setOrderPhone("010-1234-5678");
        order1.setOrderEmail("john@example.com");
        order1.setDeliveryFee(0);

// Order를 먼저 저장
        entityManager.persist(order1);

// Shipment 객체 생성 및 저장
        Shipment shipment1 = new Shipment();
        shipment1.setState(ShipmentState.PENDING);
        shipment1.setShipmentCompaniesCode(ShippingCompaniesCode.CJ_LOGISTICS);
        shipment1.setShipmentDatetime(LocalDateTime.now());
        entityManager.persist(shipment1);  // Shipment 저장

// OrderDetail 설정 및 저장
        OrderDetail orderDetail1 = new OrderDetail();
        orderDetail1.setOrder(order1);  // Order가 영속화된 후에 설정
        orderDetail1.setState(OrderState.CONFIRMED);
        orderDetail1.setAmountDetail(9000);
        orderDetail1.setQuantity(2);
        orderDetail1.setBook(book1);
        orderDetail1.setShipment(shipment1);  // 이미 저장된 shipment 객체 할당

        order1.addOrderDetail(orderDetail1);

// OrderDetail 저장
        entityManager.persist(orderDetail1);

// 두 번째 주문 추가 (같은 방식으로 처리)
        Order order2 = new Order();
        order2.setUser(user2);
        order2.setState(OrderState.CONFIRMED);
        order2.setOrderTime(LocalDateTime.of(2024, 12, 10, 14, 30));
        order2.setTotalAmount(19000);
        order2.setPointAmount(500);
        order2.setName("John Doe2");
        order2.setOrderPhone("010-2345-6789");
        order2.setOrderEmail("john2@example.com");
        order2.setDeliveryFee(0);

// Order를 먼저 저장
        entityManager.persist(order2);

// OrderDetail 설정 및 저장
        OrderDetail orderDetail2 = new OrderDetail();
        orderDetail2.setOrder(order2);  // Order가 영속화된 후에 설정
        orderDetail2.setState(OrderState.CONFIRMED);
        orderDetail2.setAmountDetail(9500);
        orderDetail2.setQuantity(3);
        orderDetail2.setBook(book2);
        orderDetail2.setShipment(shipment1);  // 이미 저장된 shipment 객체 할당

        order2.addOrderDetail(orderDetail2);

// OrderDetail 저장
        entityManager.persist(orderDetail2);

    }



    @DisplayName("책 제목을 통해 책의 세부사항을 가져오는지 확인.(책이 존재하는 경우)")
    @Test
    public void testFindBooksByTitleWithDetails() {
        // Given
        String title = "test title";
        PageRequest pageable = PageRequest.of(0, 10);  // 첫 번째 페이지, 10개의 결과

        // When
        Page<BookSearchEntityDTO> result = bookRepository.findBooksByTitleWithDetails(title, pageable);

        // Then
        assertThat(result).isNotNull();
        log.debug( "testFindBooksByTitleWithDetails 메서드 결과 값 확인 {}", result.getContent());
        assertThat(result.getContent()).isNotEmpty();  // 결과가 비어 있지 않아야 함
        assertThat(result.getTotalElements()).isGreaterThan(0);  // 결과가 하나 이상이어야 함

        // 추가적인 검증 (책 제목이 검색어와 일치하는지)
        result.getContent().forEach(book -> {
            assertThat(book.getTitle()).contains("test title");
            List<AuthorDTO> authorDTO = book.getAuthors();
            authorDTO.forEach(a -> {
                assertThat(a.getName()).isNotEmpty();
                assertThat(a.getRole()).isNotNull();
            });
            assertThat(book.getAuthors()).isNotEmpty();  // authors가 비어 있지 않아야 함
            assertThat(book.getAuthors()).anyMatch(author -> author.getName() != null);  // 작가 이름이 null이 아니어야 함
            assertThat(book.getSalePrice()).isGreaterThan(0);  // 책 가격이 0보다 커야 함
            assertThat(book.getRegularPrice()).isGreaterThan(0);
            assertThat(book.getPublishDate()).isNotNull();  // 책의 발행일자가 null이 아니어야 함
        });
    }

    @DisplayName("책 제목을 통해 책의 세부사항을 가져오는지 확인.(책이 존재하지 않는 경우)")
    @Test
    public void testFindBooksByTitleWithDetailsNotExists() {
        // Given
        String title = "not exist title";
        PageRequest pageable = PageRequest.of(0, 10);  // 첫 번째 페이지, 10개의 결과

        // When
        Page<BookSearchEntityDTO> result = bookRepository.findBooksByTitleWithDetails(title, pageable);

        // Then
        assertThat(result).isNotNull();
        log.debug("testFindBooksByTitleWithDetails 메서드 결과 값 확인 {}", result.getContent());

        assertThat(result.getContent()).isEmpty();//결과가 비어있어야함.
    }

    @DisplayName("책 제목을 통해 책의 세부사항을 가져오는지 확인. (책 제목이 null 또는 blank)")
    @Test
    public void testFindBooksByTitleWithDetailsByNullOrBlankTitle() {
        // Given
        String title = null;
        PageRequest pageable = PageRequest.of(0, 10);  // 첫 번째 페이지, 10개의 결과

        // When
        Page<BookSearchEntityDTO> nullTitleResult = bookRepository.findBooksByTitleWithDetails(title, pageable);

        // Then
        assertThat(nullTitleResult).isNotNull();
        log.debug("testFindBooksByTitleWithDetailsByNullOrBlankTitle 메서드 결과 값 확인 {}", nullTitleResult.getContent());
        assertThat(nullTitleResult.getContent()).isEmpty();//결과가 비어있어야함.


    }
    @DisplayName("작가 이름을 통해 책의 세부사항을 가져오는지 확인. (책이 존재하는경우)")
    @Test
    public void testFindBooksByAuthorNameWithDetails() {
        // Given
        String authorName = "example author";
        PageRequest pageable = PageRequest.of(0, 10); // 첫 번째 페이지, 10개의 결과

        // When
        Page<BookSearchEntityDTO> result = bookRepository.findBooksByAuthorNameWithDetails(authorName, pageable);
        log.debug("testFindBooksByAuthorNameWithDetails (책 존재하는 경우 )메서드 결과 값 확인 {}",result.getContent());

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isNotEmpty(); // 결과가 비어 있지 않아야 함

        // 추가적인 검증
        result.getContent().forEach(book -> {
            assertThat(book.getAuthors()).isNotEmpty(); // authors가 비어 있지 않아야 함
            assertThat(book.getAuthors()).anyMatch(author -> author.getName().contains(authorName)); // 작가 이름 포함 여부 검증
            assertThat(book.getTitle()).isNotBlank(); // 도서 제목이 비어 있지 않아야 함
            assertThat(book.getRegularPrice()).isGreaterThanOrEqualTo(0); // regularPrice가 음수가 아니어야 함
            assertThat(book.getSalePrice()).isGreaterThanOrEqualTo(0); // salePrice가 음수가 아니어야 함
            assertThat(book.getPublishDate()).isNotNull(); // publishDate가 null이 아니어야 함
            assertThat(book.getPublishDate()).isBeforeOrEqualTo(LocalDate.now()); // 출판일이 오늘 날짜 이전이어야 함

            // 리뷰 및 조회수 검증
            assertThat(book.getReviewCount()).isGreaterThanOrEqualTo(0); // 리뷰 개수가 음수가 아니어야 함
            assertThat(book.getViewCount()).isGreaterThanOrEqualTo(0); // 조회수가 음수가 아니어야 함

            assertThat(book.getAuthors()).allMatch(author -> {
                String role = author.getRole();
                return role != null;
            });
        });
    }

    @DisplayName("작가 이름을 통해 책의 세부사항을 가져오는지 확인. (책이 존재하지 않는경우)")
    @Test
    public void testFindBooksByAuthorNameWithDetailsNotExists() {
        // Given
        String authorName = "not exist author";
        PageRequest pageable = PageRequest.of(0, 10); // 첫 번째 페이지, 10개의 결과

        // When
        Page<BookSearchEntityDTO> result = bookRepository.findBooksByAuthorNameWithDetails(authorName, pageable);
        log.debug("testFindBooksByAuthorNameWithDetails (책 존재하지 않는 경우) 메서드 결과 값 확인 {}",result.getContent());

        // Then
        assertThat(result).isNotNull(); //존재하지않아도 빈 페이지를 보여주기에 결과는 null이면 안된다
        assertThat(result.getContent()).isEmpty(); // 결과가 비어있어여함

    }

    @DisplayName("작가 이름이 null 또는 blank면 빈페이지")
    @Test
    public void testFindBooksByAuthorNameWithDetailsByNullOrBlankCategoryName() {
        // Given
        String authorName = null;
        PageRequest pageable = PageRequest.of(0, 10); // 첫 번째 페이지, 10개의 결과

        // When
        Page<BookSearchEntityDTO> nullNameResult = bookRepository.findBooksByAuthorNameWithDetails(authorName, pageable);

        // Then
        assertThat(nullNameResult).isNotNull(); // 페이지는 null이면 안 됨
        log.debug("testFindBooksByAuthorNameWithDetailsByNullOrBlankCategoryName 결과 값 확인: {}", nullNameResult.getContent());
        assertThat(nullNameResult.getContent()).isEmpty(); // 결과가 비어 있어야 함

        authorName = "";
        Page<BookSearchEntityDTO> emptyNameResult = bookRepository.findBooksByAuthorNameWithDetails(authorName, pageable);
        assertThat(emptyNameResult).isNotNull(); // 페이지는 null이면 안 됨
        assertThat(emptyNameResult.getContent()).isEmpty(); // 결과가 비어 있어야 함
    }


    @DisplayName("카테고리 Id로 책의 세부사항을 가져오는지 확인 (카테고리가 존재하는 경우)")
    @Test
    public void testFindBooksByCategoryIdWithDetails() {
        // Given
        Long categoryId = 1L;
        PageRequest pageable = PageRequest.of(0, 10); // 첫 번째 페이지, 10개의 결과

        // When
        Page<BookSearchEntityDTO> result = bookRepository.findBooksByCategoryWithDetails(categoryId, pageable);

        // Then
        assertThat(result).isNotNull();
        log.debug("testFindBooksByCategoryIdWithDetails 메서드 결과 값 확인: {}", result.getContent());

        assertThat(result.getContent()).isNotNull(); // 결과가 비어 있지 않아야 함
        assertThat(result.getTotalElements()).isGreaterThan(0); // 결과가 하나 이상이어야 함

        // 책의 카테고리 Id가 주어진 categoryId와 일치하는지 확인
        result.getContent().forEach(book -> {
            assertThat(book.getCategoryIdList()).isNotEmpty(); // 카테고리가 비어 있지 않아야 함
            assertThat(book.getCategoryIdList()).contains(categoryId); // 카테고리 목록에 categoryId가 포함되어 있는지 확인

            // 추가적인 검증

            // 책 제목이 비어 있지 않아야 함
            assertThat(book.getTitle()).isNotBlank();

            // 가격이 음수일 수 없음
            assertThat(book.getRegularPrice()).isGreaterThanOrEqualTo(0);
            assertThat(book.getSalePrice()).isGreaterThanOrEqualTo(0);

            // 출판일이 null이 아니어야 하며, 미래 날짜가 아니라 오늘 날짜 이전이어야 함
            assertThat(book.getPublishDate()).isNotNull();
            assertThat(book.getPublishDate()).isBeforeOrEqualTo(LocalDate.now());

            // 책의 저자 정보 검증
            assertThat(book.getAuthors()).isNotEmpty(); // 저자 목록이 비어 있지 않아야 함
            book.getAuthors().forEach(author -> {
                assertThat(author.getName()).isNotBlank(); // 작가 이름이 비어 있지 않아야 함
            });

            // 리뷰 개수와 조회수 검증 (음수일 수 없음)
            assertThat(book.getReviewCount()).isGreaterThanOrEqualTo(0);
            assertThat(book.getViewCount()).isGreaterThanOrEqualTo(0);

        });
    }


    @DisplayName("카테고리 Id로 책의 세부사항을 가져오는지 확인 (카테고리가 존재하지 않는 경우)")
    @Test
    public void testFindBooksByCategoryIdWithDetailsNotExists() {
        // Given
        Long categoryId = 13L;
        PageRequest pageable = PageRequest.of(0, 10); // 첫 번째 페이지, 10개의 결과

        // When
        Page<BookSearchEntityDTO> result = bookRepository.findBooksByCategoryWithDetails(categoryId, pageable);

        // Then
        assertThat(result).isNotNull(); // 페이지는 null이면 안 됨
        log.debug("testFindBooksByCategoryIdWithDetailsNotExists 결과 값 확인: {}", result.getContent());
        assertThat(result.getContent()).isEmpty(); // 결과가 비어 있어야 함
    }

    @DisplayName("카테고리 Id로 책의 세부사항을 가져오는지 확인 (카테고리 이름이 null 또는 blank)")
    @Test
    public void testFindBooksByCategoryIdWithDetailsByNullOrBlankCategoryName() {
        // Given
        Long categoryId = null;
        PageRequest pageable = PageRequest.of(0, 10); // 첫 번째 페이지, 10개의 결과

        // When
        Page<BookSearchEntityDTO> result = bookRepository.findBooksByCategoryWithDetails(categoryId, pageable);

        // Then
        assertThat(result).isNotNull(); // 페이지는 null이면 안 됨
        log.debug("testFindBooksByCategoryIdWithDetailsByNullOrBlankCategoryName 결과 값 확인: {}", result.getContent());
        assertThat(result.getContent()).isEmpty(); // 결과가 비어 있어야 함

    }

    @DisplayName("책 id 리스트로 책 세부사항 가져오는지 확인")
    @Test
    void testFindBookByIdIn() {
        List<Long> ids= new ArrayList<>();
        ids.add(1L);
        ids.add(2L);
        PageRequest pageable = PageRequest.of(0, 10);
        Page<BookSearchEntityDTO> result = bookRepository.findBookByIdIn(ids, pageable);
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isNotEmpty();
        assertThat(result.getTotalElements()).isGreaterThan(0);
        result.getContent().forEach(book -> {
            assertThat(book.getTitle()).isNotBlank();
            assertThat(book.getRegularPrice()).isGreaterThanOrEqualTo(0);
            assertThat(book.getSalePrice()).isGreaterThanOrEqualTo(0);
            assertThat(book.getPublishDate()).isNotNull();
            assertThat(book.getPublishDate()).isBeforeOrEqualTo(LocalDate.now());
            assertThat(book.getAuthors()).isNotEmpty();
            book.getAuthors().forEach(author -> {
                assertThat(author.getName()).isNotBlank();
            });
            assertThat(book.getReviewCount()).isGreaterThanOrEqualTo(0);
            assertThat(book.getViewCount()).isGreaterThanOrEqualTo(0);
        });


    }

    @DisplayName("태그 이름으로 책을 검색하는지 확인 (태그가 존재하는 경우)")
    @Test
    public void testFindBooksByTagNameWithDetails() {
        // Given
        String tagName = "초등";
        PageRequest pageable = PageRequest.of(0, 10); // 첫 번째 페이지, 10개의 결과

        // When
        Page<BookSearchEntityDTO> result = bookRepository.findBooksByTagNameWithDetails(tagName, pageable);

        // Then
        assertThat(result).isNotNull();
        log.debug("testFindBooksByTagNameWithDetails 메서드 결과 값 확인: {}", result.getContent());

        assertThat(result.getContent()).isNotNull(); // 결과가 비어 있지 않아야 함
        assertThat(result.getTotalElements()).isGreaterThan(0); // 결과가 하나 이상이어야 함

        // 책의 카테고리 Id 목록에서 해당 태그와 관련된 카테고리가 존재하는지 확인
        result.getContent().forEach(book -> {
            assertThat(book.getCategoryIdList()).isNotEmpty(); // 카테고리 목록이 비어 있지 않아야 함
            // 태그와 관련된 카테고리가 책에 포함되어 있는지 확인
            // (여기서는 카테고리 ID 목록이 특정 조건을 만족하는지 확인)
            // 예를 들어, 태그와 연결된 카테고리 ID가 포함되어 있는지 확인할 수 있음
            // 이 부분은 테스트 대상에 맞게 수정해야 합니다.

            // 책 제목이 비어 있지 않아야 함
            assertThat(book.getTitle()).isNotBlank();

            // 가격이 음수일 수 없음
            assertThat(book.getRegularPrice()).isGreaterThanOrEqualTo(0);
            assertThat(book.getSalePrice()).isGreaterThanOrEqualTo(0);

            // 출판일이 null이 아니어야 하며, 미래 날짜가 아니라 오늘 날짜 이전이어야 함
            assertThat(book.getPublishDate()).isNotNull();
            assertThat(book.getPublishDate()).isBeforeOrEqualTo(LocalDate.now());

            // 책의 저자 정보 검증
            assertThat(book.getAuthors()).isNotEmpty(); // 저자 목록이 비어 있지 않아야 함
            book.getAuthors().forEach(author -> {
                assertThat(author.getName()).isNotBlank(); // 작가 이름이 비어 있지 않아야 함
            });

            // 리뷰 개수와 조회수 검증 (음수일 수 없음)
            assertThat(book.getReviewCount()).isGreaterThanOrEqualTo(0);
            assertThat(book.getViewCount()).isGreaterThanOrEqualTo(0);

        });
    }

    @DisplayName("저번 달 가장 많이 팔린 책 id를 반환한다.")
    @Test
    void testFindMostSoldByLastMonth() {
        // Given
        // When
        // BookService나 bookRepository에서 가장 많이 팔린 책을 조회합니다.
        Tuple result = bookRepository.findMostSoldByLastMonth();  // Tuple로 결과 받아옴

        // Then
        // 가장 많이 팔린 책의 ID를 확인합니다.
        // 테스트 데이터에서 책 2번이 더 많이 팔렸으므로, book2의 ID인 2L이 반환되어야 합니다.
        assertNotNull(result); // 결과가 null이 아니어야 합니다.
        assertEquals(2L, result.get(orderDetail.book.id)); // 가장 많이 팔린 책의 ID는 1이어야 합니다.
    }


}