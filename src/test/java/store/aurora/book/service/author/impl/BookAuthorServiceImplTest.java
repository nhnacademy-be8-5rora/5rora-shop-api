package store.aurora.book.service.author.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import store.aurora.book.dto.author.ParsedAuthorDto;
import store.aurora.book.entity.Author;
import store.aurora.book.entity.AuthorRole;
import store.aurora.book.entity.Book;
import store.aurora.book.entity.BookAuthor;
import store.aurora.book.parser.AuthorParser;
import store.aurora.book.repository.author.BookAuthorRepository;
import store.aurora.book.service.author.AuthorRoleService;
import store.aurora.book.service.author.AuthorService;

import java.util.List;

@ExtendWith(MockitoExtension.class)
class BookAuthorServiceImplTest {

    @Mock private AuthorService authorService;
    @Mock private AuthorRoleService authorRoleService;
    @Mock private BookAuthorRepository bookAuthorRepository;
    @Mock private AuthorParser authorParser;

    @InjectMocks
    private BookAuthorServiceImpl bookAuthorService;

    private Book sampleBook;

    @BeforeEach
    void setUp() {
        sampleBook = new Book();
        sampleBook.setId(1L);
        sampleBook.setTitle("테스트 책");
    }

    @Test
    @DisplayName("책의 저자 정보를 파싱하여 BookAuthor로 저장한다.")
    void parseAndSaveBookAuthors_ShouldParseAndSave() {
        // Given
        String authorsString = "홍길동(저자), 이몽룡(역자)";
        List<ParsedAuthorDto> parsedAuthors = List.of(
                new ParsedAuthorDto("홍길동", "저자"),
                new ParsedAuthorDto("이몽룡", "역자")
        );

        when(authorParser.parseAuthors(authorsString)).thenReturn(parsedAuthors);
        when(authorService.getOrCreateAuthor(anyString())).thenAnswer(invocation -> new Author(null, invocation.getArgument(0)));
        when(authorRoleService.getOrCreateRole(anyString())).thenAnswer(invocation -> new AuthorRole(null, invocation.getArgument(0)));

        // When
        bookAuthorService.parseAndSaveBookAuthors(sampleBook, authorsString);

        // Then
        verify(bookAuthorRepository, times(1)).saveAll(anyList());
    }

    @Test
    @DisplayName("책에 저장된 저자 목록을 문자열로 변환한다.")
    void getFormattedAuthors_ShouldReturnFormattedString() {
        // Given
        Author author1 = new Author(1L, "홍길동");
        Author author2 = new Author(2L, "이몽룡");
        AuthorRole roleWriter = new AuthorRole(1L, "저자");
        AuthorRole roleTranslator = new AuthorRole(2L, "역자");

        List<BookAuthor> bookAuthors = List.of(
                new BookAuthor(1L, author1, roleWriter, sampleBook),
                new BookAuthor(2L, author2, roleTranslator, sampleBook)
        );

        when(bookAuthorRepository.findByBook(sampleBook)).thenReturn(bookAuthors);
        when(authorParser.formatAuthors(bookAuthors)).thenReturn("홍길동 (저자), 이몽룡 (역자)");

        // When
        String formattedAuthors = bookAuthorService.getFormattedAuthors(sampleBook);

        // Then
        assertThat(formattedAuthors).isEqualTo("홍길동 (저자), 이몽룡 (역자)");
    }
}