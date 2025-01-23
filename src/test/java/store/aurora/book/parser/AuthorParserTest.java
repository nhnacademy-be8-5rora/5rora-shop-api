package store.aurora.book.parser;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import store.aurora.book.dto.author.ParsedAuthorDto;
import store.aurora.book.entity.AuthorRole;
import store.aurora.book.entity.BookAuthor;
import store.aurora.book.entity.Author;
import store.aurora.book.repository.author.AuthorRoleRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AuthorParserTest {

    private final AuthorRoleRepository authorRoleRepository = mock(AuthorRoleRepository.class);
    private final AuthorParser authorParser = new AuthorParser(authorRoleRepository);

    @Test
    @DisplayName("null 또는 빈 문자열이 주어지면 빈 리스트를 반환한다.")
    void parseAuthors_shouldReturnEmptyList_whenInputIsNullOrEmpty() {
        assertThat(authorParser.parseAuthors(null)).isEmpty();
        assertThat(authorParser.parseAuthors("")).isEmpty();
        assertThat(authorParser.parseAuthors("   ")).isEmpty();
    }

    @Test
    @DisplayName("저자 역할이 포함된 문자열을 올바르게 파싱한다.")
    void parseAuthors_shouldExtractRolesCorrectly() {
        // Given
        String authorsString = "홍길동(저자), 이몽룡(역자), 성춘향(기획)";

        // When
        List<ParsedAuthorDto> parsedAuthors = authorParser.parseAuthors(authorsString);

        // Then
        assertThat(parsedAuthors).hasSize(3);
        assertThat(parsedAuthors.get(0)).extracting("name", "role").containsExactly("홍길동", "저자");
        assertThat(parsedAuthors.get(1)).extracting("name", "role").containsExactly("이몽룡", "역자");
        assertThat(parsedAuthors.get(2)).extracting("name", "role").containsExactly("성춘향", "기획");
    }

    @Test
    @DisplayName("역할이 없는 경우 기본 역할 '지은이'로 설정한다.")
    void parseAuthors_shouldAssignDefaultRole() {
        // Given
        String authorsString = "홍길동, 이몽룡, 성춘향";

        // When
        List<ParsedAuthorDto> parsedAuthors = authorParser.parseAuthors(authorsString);

        // Then
        assertThat(parsedAuthors).hasSize(3);
        parsedAuthors.forEach(author ->
                assertThat(author.getRole()).isEqualTo("지은이")
        );
    }

    @Test
    @DisplayName("콤마와 공백이 포함된 경우 정상적으로 파싱된다.")
    void parseAuthors_shouldHandleCommaAndSpacesCorrectly() {
        // Given
        String authorsString = "  홍길동(저자) ,  이몽룡(역자) ,  성춘향   ";

        // When
        List<ParsedAuthorDto> parsedAuthors = authorParser.parseAuthors(authorsString);

        // Then
        assertThat(parsedAuthors).hasSize(3);
        assertThat(parsedAuthors.get(0)).extracting("name", "role").containsExactly("홍길동", "저자");
        assertThat(parsedAuthors.get(1)).extracting("name", "role").containsExactly("이몽룡", "역자");
        assertThat(parsedAuthors.get(2)).extracting("name", "role").containsExactly("성춘향", "지은이");
    }

    @Test
    @DisplayName("formatAuthors는 BookAuthor 리스트를 역할별 그룹화된 문자열로 변환한다.")
    void formatAuthors_shouldGroupByRole() {
        // Given
        Author author1 = new Author(1L, "홍길동");
        Author author2 = new Author(2L, "이몽룡");
        Author author3 = new Author(3L, "성춘향");

        AuthorRole roleWriter = new AuthorRole(1L, "저자");
        AuthorRole roleTranslator = new AuthorRole(2L, "역자");

        BookAuthor bookAuthor1 = new BookAuthor(1L, author1, roleWriter, null);
        BookAuthor bookAuthor2 = new BookAuthor(2L, author2, roleTranslator, null);
        BookAuthor bookAuthor3 = new BookAuthor(3L, author3, null, null); // 역할 없음 → 기본값 "지은이"

        List<BookAuthor> bookAuthors = List.of(bookAuthor1, bookAuthor2, bookAuthor3);

        // When
        String formattedAuthors = authorParser.formatAuthors(bookAuthors);

        // Then
        assertThat(formattedAuthors).isEqualTo("홍길동 (저자), 이몽룡 (역자), 성춘향 (지은이)");
    }

    @Test
    @DisplayName("formatAuthors는 빈 리스트가 주어지면 빈 문자열을 반환한다.")
    void formatAuthors_shouldReturnEmptyString_whenListIsEmpty() {
        // When
        String formattedAuthors = authorParser.formatAuthors(List.of());

        // Then
        assertThat(formattedAuthors).isEmpty();
    }
}
