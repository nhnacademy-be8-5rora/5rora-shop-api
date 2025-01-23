package store.aurora.book.parser;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import store.aurora.book.entity.tag.BookTag;
import store.aurora.book.entity.tag.Tag;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TagParserTest {

    private final TagParser tagParser = new TagParser();

    @Test
    @DisplayName("null 또는 빈 문자열이 주어지면 빈 리스트를 반환한다.")
    void parseTags_shouldReturnEmptyList_whenInputIsNullOrEmpty() {
        assertThat(tagParser.parseTags(null)).isEmpty();
        assertThat(tagParser.parseTags("")).isEmpty();
        assertThat(tagParser.parseTags("   ")).isEmpty();
    }

    @Test
    @DisplayName("콤마로 구분된 태그 문자열을 리스트로 변환한다.")
    void parseTags_shouldSplitTagsByComma() {
        // Given
        String tags = "태그1, 태그2,태그3 ,태그4";

        // When
        List<String> parsedTags = tagParser.parseTags(tags);

        // Then
        assertThat(parsedTags).containsExactly("태그1", "태그2", "태그3", "태그4");
    }

    @Test
    @DisplayName("여러 개의 공백이 포함된 태그 문자열을 정상적으로 변환한다.")
    void parseTags_shouldTrimSpaces() {
        // Given
        String tags = "   태그1   ,    태그2 ,태그3   ";

        // When
        List<String> parsedTags = tagParser.parseTags(tags);

        // Then
        assertThat(parsedTags).containsExactly("태그1", "태그2", "태그3");
    }

    @Test
    @DisplayName("formatTags는 BookTag 리스트를 콤마로 연결된 문자열로 변환한다.")
    void formatTags_shouldReturnCommaSeparatedString() {
        // Given
        BookTag tag1 = new BookTag(new Tag("태그A"));
        BookTag tag2 = new BookTag(new Tag("태그B"));
        BookTag tag3 = new BookTag(new Tag("태그C"));
        List<BookTag> bookTags = List.of(tag1, tag2, tag3);

        // When
        String formattedTags = tagParser.formatTags(bookTags);

        // Then
        assertThat(formattedTags).isEqualTo("태그A, 태그B, 태그C");
    }

    @Test
    @DisplayName("formatTags는 빈 리스트가 주어지면 빈 문자열을 반환한다.")
    void formatTags_shouldReturnEmptyString_whenListIsEmpty() {
        // When
        String formattedTags = tagParser.formatTags(List.of());

        // Then
        assertThat(formattedTags).isEmpty();
    }
}
