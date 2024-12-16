package store.aurora.book.repository.impl;


import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import store.aurora.book.entity.*;
import store.aurora.book.repository.BookRepository;
import store.aurora.search.dto.BookCategorySearchEntityDTO;
import store.aurora.search.dto.BookSearchEntityDTO;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;


//데이터 아직 없어서 확인 못함

@DataJpaTest
@Slf4j
public class BookRepositoryCustomTest {

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private TestEntityManager entityManager;

    @BeforeEach
    public void setup() {
        // Publisher 엔티티 삽입
        Publisher publisher = new Publisher(null, "Example Publisher");
        publisher = entityManager.merge(publisher);

        // Author 엔티티 삽입 (ID가 null인 새로운 엔티티로 취급)
        Author author = new Author(null, "Example Author"); // ID를 null로 설정하여 새로 저장
        author = entityManager.merge(author);

        // Book 엔티티 삽입
        Book book = new Book(1L, "Example Title", 10000, 8000, 100, true, "1234567890123",
                "Example content", "Example explanation", true, LocalDate.of(2020, 1, 1), publisher,
                null, new ArrayList<>(),new ArrayList<>());
        book = entityManager.merge(book);

        // AuthorRole 엔티티 삽입
        AuthorRole authorRole = new AuthorRole(1L, AuthorRole.Role.AUTHOR);
        authorRole = entityManager.merge(authorRole);

        // BookAuthor 엔티티 삽입
        BookAuthor bookAuthor = new BookAuthor(null, author, authorRole, book);  // BookAuthor 생성, ID는 null로 설정
        bookAuthor =entityManager.merge(bookAuthor);

        // category 엔티티 삽입
        Category category = new Category(1L, "Example Category", null, new ArrayList<>(), 1);
        category = entityManager.merge(category);
        Category category2 = new Category(2L, "Example Category2", null, new ArrayList<>(), 1);
        category2 = entityManager.merge(category2);
        // BookCategory 엔티티 삽입
        BookCategory bookCategory = new BookCategory(1L, category, book); // ID는 null로 설정하여 새로 저장
        entityManager.merge(bookCategory); // 저장 후 영속화
        BookCategory bookCategory2 = new BookCategory(2L, category2, book); // ID는 null로 설정하여 새로 저장
        entityManager.merge(bookCategory2); // 저장 후 영속화

        log.debug("setup 카테고리 확인 : {}",category.getName());
        log.debug("setup 카테고리 확인2 : {}",category2.getName());
        log.debug("setup 카테고리 확인 : {}",bookCategory.getCategory().getName());
        log.debug("setup 카테고리 확인2 : {}",bookCategory2.getCategory().getName());

    }





    @DisplayName("책 제목을 통해 책의 세부사항을 가져오는지 확인.(책이 존재하는 경우)")
    @Test
    public void testFindBooksByTitleWithDetails() {
        // Given
        String title = "Example Title";
        PageRequest pageable = PageRequest.of(0, 10);  // 첫 번째 페이지, 10개의 결과

        // When
        Page<BookSearchEntityDTO> result = bookRepository.findBooksByTitleWithDetails(title, pageable);

        // Then
        assertThat(result).isNotNull();
        log.debug("testFindBooksByTitleWithDetails 메서드 결과 값 확인 {}", result.getContent());

        assertThat(result.getContent()).isNotEmpty();  // 결과가 비어 있지 않아야 함
        assertThat(result.getTotalElements()).isGreaterThan(0);  // 결과가 하나 이상이어야 함

        // 추가적인 검증 (책 제목이 검색어와 일치하는지 등)
        result.getContent().forEach(book -> {
            assertThat(book.getTitle()).contains(title);
            assertThat(book.getAuthors()).isNotEmpty();  // authors가 비어 있지 않아야 함
        });
    }

    @DisplayName("책 제목을 통해 책의 세부사항을 가져오는지 확인.(책이 존재하는 경우)")
    @Test
    public void testFindBooksByTitleWithDetailsNotExists() {
        // Given
        String title = "Example Title1";
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
        Page<BookSearchEntityDTO> result = bookRepository.findBooksByTitleWithDetails(title, pageable);

        // Then
        assertThat(result).isNotNull();
        log.debug("testFindBooksByTitleWithDetailsByNullOrBlankTitle 메서드 결과 값 확인 {}", result.getContent());
        assertThat(result.getContent()).isEmpty();//결과가 비어있어야함.

        title="";
        result=bookRepository.findBooksByTitleWithDetails(title, pageable);
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();

    }

    @DisplayName("작가 이름을 통해 책의 세부사항을 가져오는지 확인. (책이 존재하는경우)")
    @Test
    public void testFindBooksByAuthorNameWithDetails() {
        // Given
        String authorName = "Example Author";
        PageRequest pageable = PageRequest.of(0, 10); // 첫 번째 페이지, 10개의 결과

        // When
        Page<BookSearchEntityDTO> result = bookRepository.findBooksByAuthorNameWithDetails(authorName, pageable);
        log.debug("testFindBooksByAuthorNameWithDetails (책 존재하는 경우 )메서드 결과 값 확인 {}",result.getContent());

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isNotEmpty(); // 결과가 비어 있지 않아야 함
        assertThat(result.getTotalElements()).isGreaterThan(0); // 결과가 하나 이상이어야 함

        // 추가적인 검증
        result.getContent().forEach(book -> {
            assertThat(book.getAuthors()).isNotEmpty(); // authors가 비어 있지 않아야 함
            assertThat(book.getAuthors()).anyMatch(author -> author.getName().contains(authorName)); // 작가 이름 포함 여부 검증
            assertThat(book.getTitle()).isNotBlank(); // 도서 제목이 비어 있지 않아야 함
        });
    }


    @DisplayName("작가 이름을 통해 책의 세부사항을 가져오는지 확인. (책이 존재하지 않는경우)")
    @Test
    public void testFindBooksByAuthorNameWithDetailsNotExists() {
        // Given
        String authorName = "Example Author1";
        PageRequest pageable = PageRequest.of(0, 10); // 첫 번째 페이지, 10개의 결과

        // When
        Page<BookSearchEntityDTO> result = bookRepository.findBooksByAuthorNameWithDetails(authorName, pageable);
        log.debug("testFindBooksByAuthorNameWithDetails (책 존재하지 않는 경우) 메서드 결과 값 확인 {}",result.getContent());

        // Then
        assertThat(result).isNotNull(); //존재하지않아도 빈 페이지를 보여주기에 결과는 null이면 안된다
        assertThat(result.getContent()).isEmpty(); // 결과가 비어있어여함

    }

    @DisplayName("작가 이름을 통해 책의 세부사항을 가져오는지 확인. (작가 이름이 null or blank )")
    @Test
    public void testFindBooksByAuthorNameWithDetailsByNullOrBlankName() {
        // Given
        String authorName = null;
        PageRequest pageable = PageRequest.of(0, 10); // 첫 번째 페이지, 10개의 결과

        // When
        Page<BookSearchEntityDTO> result = bookRepository.findBooksByAuthorNameWithDetails(authorName, pageable);
        log.debug("testFindBooksByAuthorNameWithDetails (책 존재하지 않는 경우) 메서드 결과 값 확인 {}",result.getContent());

        // Then
        assertThat(result).isNotNull(); //존재하지않아도 빈 페이지를 보여주기에 결과는 null이면 안된다
        assertThat(result.getContent()).isEmpty(); // 결과가 비어있어여함

        authorName ="";
        result = bookRepository.findBooksByAuthorNameWithDetails(authorName, pageable);
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();

    }

    @DisplayName("카테고리 이름으로 책의 세부사항을 가져오는지 확인 (카테고리가 존재하는 경우)")
    @Test
    public void testFindBooksByCategoryNameWithDetails() {
        // Given
        String categoryName = "Example Category";
        PageRequest pageable = PageRequest.of(0, 10); // 첫 번째 페이지, 10개의 결과

        // When
        Page<BookCategorySearchEntityDTO> result = bookRepository.findBooksByCategoryNameWithDetails(categoryName, pageable);

        // Then
        assertThat(result).isNotNull();
        log.debug("testFindBooksByCategoryNameWithDetails 메서드 결과 값 확인: {}", result.getContent());

        assertThat(result.getContent()).isNotNull(); // 결과가 비어 있지 않아야 함
        assertThat(result.getTotalElements()).isGreaterThan(0); // 결과가 하나 이상이어야 함

        // 책의 카테고리 이름이 주어진 categoryName과 일치하는지 확인
        result.getContent().forEach(book -> {
            assertThat(book.getCategoryNameList()).isNotEmpty(); // 카테고리가 비어 있지 않아야 함
            // categoryNameList가 쉼표로 구분된 문자열일 경우 분리하여 확인
            assertThat(book.getCategoryNameList())
                    .anyMatch(category -> category.split(",") // 쉼표를 기준으로 분리
                            .length > 0 && Arrays.asList(category.split(",")).contains(categoryName)); // categoryName이 목록에 포함되어야 함
        });

    }

    @DisplayName("카테고리 이름으로 책의 세부사항을 가져오는지 확인 (카테고리가 존재하지 않는 경우)")
    @Test
    public void testFindBooksByCategoryNameWithDetailsNotExists() {
        // Given
        String categoryName = "Nonexistent Category";
        PageRequest pageable = PageRequest.of(0, 10); // 첫 번째 페이지, 10개의 결과

        // When
        Page<BookCategorySearchEntityDTO> result = bookRepository.findBooksByCategoryNameWithDetails(categoryName, pageable);

        // Then
        assertThat(result).isNotNull(); // 페이지는 null이면 안 됨
        log.debug("testFindBooksByCategoryNameWithDetailsNotExists 결과 값 확인: {}", result.getContent());
        assertThat(result.getContent()).isEmpty(); // 결과가 비어 있어야 함
    }

    @DisplayName("카테고리 이름으로 책의 세부사항을 가져오는지 확인 (카테고리 이름이 null 또는 blank)")
    @Test
    public void testFindBooksByCategoryNameWithDetailsByNullOrBlankCategoryName() {
        // Given
        String categoryName = null;
        PageRequest pageable = PageRequest.of(0, 10); // 첫 번째 페이지, 10개의 결과

        // When
        Page<BookCategorySearchEntityDTO> result = bookRepository.findBooksByCategoryNameWithDetails(categoryName, pageable);

        // Then
        assertThat(result).isNotNull(); // 페이지는 null이면 안 됨
        log.debug("testFindBooksByCategoryNameWithDetailsByNullOrBlankCategoryName 결과 값 확인: {}", result.getContent());
        assertThat(result.getContent()).isEmpty(); // 결과가 비어 있어야 함

        // Blank 테스트
        categoryName = "";
        result = bookRepository.findBooksByCategoryNameWithDetails(categoryName, pageable);
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty(); // 결과가 비어 있어야 함
    }



}