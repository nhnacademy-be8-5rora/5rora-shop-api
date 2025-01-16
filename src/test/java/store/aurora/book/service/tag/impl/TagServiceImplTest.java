package store.aurora.book.service.tag.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import store.aurora.book.dto.tag.TagRequestDto;
import store.aurora.book.dto.tag.TagResponseDto;
import store.aurora.book.entity.Book;
import store.aurora.book.entity.tag.BookTag;
import store.aurora.book.entity.tag.Tag;
import store.aurora.book.exception.tag.TagAlreadyExistException;
import store.aurora.book.exception.tag.TagNotFoundException;
import store.aurora.book.repository.tag.BookTagRepository;
import store.aurora.book.repository.tag.TagRepository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class TagServiceImplTest {

    @Mock
    private TagRepository tagRepository;

    @Mock
    private BookTagRepository bookTagRepository;

    @InjectMocks
    private TagServiceImpl tagService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testSearchTags() {
        String keyword = "av";
        Tag tag = new Tag( "java");
        when(tagRepository.findByNameContaining(keyword)).thenReturn(Collections.singletonList(tag));

        List<TagResponseDto> result = tagService.searchTags(keyword);

        assertThat(result)
                .hasSize(1)
                .extracting(TagResponseDto::getName)
                .containsExactly("java");

        verify(tagRepository, times(1)).findByNameContaining(keyword);
    }

    @Test
    void testGetTags() {
        Pageable pageable = PageRequest.of(0, 10);
        Tag tag = new Tag("java");
        Page<Tag> tagPage = new PageImpl<>(Collections.singletonList(tag));
        when(tagRepository.findAllByOrderById(pageable)).thenReturn(tagPage);

        Page<TagResponseDto> result = tagService.getTags(pageable);

        assertThat(result.getContent())
                .hasSize(1)
                .extracting(TagResponseDto::getName)
                .containsExactly("java");

        verify(tagRepository, times(1)).findAllByOrderById(pageable);
    }

    @Test
    void testGetTagById() {
        Tag tag = new Tag( "java");
        when(tagRepository.findById(1L)).thenReturn(Optional.of(tag));

        TagResponseDto result = tagService.getTagById(1L);

        assertThat(result)
                .isNotNull()
                .extracting(TagResponseDto::getName)
                .isEqualTo("java");

        verify(tagRepository, times(1)).findById(1L);
    }

    @Test
    void testGetTagById_TagNotFound() {
        when(tagRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tagService.getTagById(1L))
                .isInstanceOf(TagNotFoundException.class)
                .hasMessageContaining("태그를 찾을 수 없습니다. ID: 1");

        verify(tagRepository, times(1)).findById(1L);
    }

    @Test
    void testCreateTag() {
        TagRequestDto requestDto = new TagRequestDto("java");
        when(tagRepository.findByName("java")).thenReturn(Optional.empty());
        when(tagRepository.save(any(Tag.class))).thenReturn(new Tag("java"));

        TagResponseDto result = tagService.createTag(requestDto);

        assertThat(result)
                .isNotNull()
                .extracting(TagResponseDto::getName)
                .isEqualTo("java");

        verify(tagRepository, times(1)).findByName("java");
        verify(tagRepository, times(1)).save(any(Tag.class));
    }

    @Test
    void testCreateTag_AlreadyExists() {
        TagRequestDto requestDto = new TagRequestDto("java");
        when(tagRepository.findByName("java")).thenReturn(Optional.of(new Tag("java")));

        assertThatThrownBy(() -> tagService.createTag(requestDto))
                .isInstanceOf(TagAlreadyExistException.class)
                .hasMessageContaining("이미 존재하는 태그입니다");

        verify(tagRepository, times(1)).findByName("java");
    }

    @Test
    void testUpdateTag() {
        Tag tag = new Tag("java");
        TagRequestDto requestDto = new TagRequestDto("spring");
        when(tagRepository.findById(1L)).thenReturn(Optional.of(tag));
        when(tagRepository.save(any(Tag.class))).thenReturn(new Tag("spring"));

        TagResponseDto result = tagService.updateTag(1L, requestDto);

        assertThat(result)
                .isNotNull()
                .extracting(TagResponseDto::getName)
                .isEqualTo("spring");

        verify(tagRepository, times(1)).findById(1L);
        verify(tagRepository, times(1)).save(any(Tag.class));
    }

    @Test
    void testDeleteTag() {
        Tag tag = new Tag("java");
        when(tagRepository.findById(1L)).thenReturn(Optional.of(tag));

        tagService.deleteTag(1L);

        verify(tagRepository, times(1)).findById(1L);
        verify(tagRepository, times(1)).delete(tag);
    }

    @Test
    void testGetOrCreateTagsByName() {
        String tags = "java,spring";
        when(tagRepository.findByName("java")).thenReturn(Optional.of(new Tag("java")));
        when(tagRepository.findByName("spring")).thenReturn(Optional.empty());
        when(tagRepository.save(any(Tag.class))).thenReturn(new Tag("spring"));

        List<Tag> result = tagService.getOrCreateTagsByName(tags);

        assertThat(result)
                .hasSize(2)
                .extracting(Tag::getName)
                .containsExactly("java", "spring");

        verify(tagRepository, times(1)).findByName("java");
        verify(tagRepository, times(1)).findByName("spring");
        verify(tagRepository, times(1)).save(any(Tag.class));
    }

    @Test
    void testGetOrCreateTagsByName_EmptyTags() {
        when(tagRepository.findByName(anyString())).thenReturn(Optional.empty());

        // null 입력 테스트
        List<Tag> resultNull = tagService.getOrCreateTagsByName(null);
        assertThat(resultNull).isEmpty();

        // 빈 문자열 테스트
        List<Tag> resultEmpty = tagService.getOrCreateTagsByName("");
        assertThat(resultEmpty).isEmpty();

        // 공백 문자열 테스트
        List<Tag> resultWhitespace = tagService.getOrCreateTagsByName("   ");
        assertThat(resultWhitespace).isEmpty();

        // 잘못된 입력 테스트
        List<Tag> resultInvalid = tagService.getOrCreateTagsByName(",");
        assertThat(resultInvalid).isEmpty();
    }

    @Test
    void testCreateBookTags() {
        Tag tag = new Tag("java");
        List<Tag> tags = Collections.singletonList(tag);

        List<BookTag> result = tagService.createBookTags(tags);

        assertThat(result)
                .hasSize(1)
                .extracting(BookTag::getTag)
                .containsExactly(tag);
    }

    @Test
    void testGetFormattedTags() {
        Book book = new Book();
        Tag tag = new Tag("java");
        BookTag bookTag = new BookTag();
        bookTag.setTag(tag);
        when(bookTagRepository.findByBook(book)).thenReturn(Collections.singletonList(bookTag));

        String result = tagService.getFormattedTags(book);

        assertThat(result).isEqualTo("java");

        verify(bookTagRepository, times(1)).findByBook(book);
    }


}
