package store.aurora.book.service.tag.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import store.aurora.book.dto.tag.TagRequestDto;
import store.aurora.book.dto.tag.TagResponseDto;
import store.aurora.book.entity.Book;
import store.aurora.book.entity.tag.BookTag;
import store.aurora.book.entity.tag.Tag;
import store.aurora.book.exception.tag.TagAlreadyExistException;
import store.aurora.book.exception.tag.TagNotFoundException;
import store.aurora.book.repository.tag.BookTagRepository;
import store.aurora.book.repository.tag.TagRepository;
import store.aurora.book.service.tag.TagService;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TagServiceImpl implements TagService {
    private final TagRepository tagRepository;
    private final BookTagRepository bookTagRepository;

    @Transactional(readOnly = true)
    @Override
    public List<TagResponseDto> searchTags(String keyword) {
        return tagRepository.findByNameContaining(keyword)
                .stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    @Override
    public Page<TagResponseDto> getTags(Pageable pageable) {
        return tagRepository.findAllByOrderById(pageable)
                .map(this::mapToResponseDto);
    }

    @Override
    public TagResponseDto getTagById(Long id) {
        return mapToResponseDto(findTagById(id));
    }

    @Transactional
    @Override
    public TagResponseDto createTag(TagRequestDto requestDto) {
        validateDuplicateTagName(requestDto.getName());

        Tag tag = new Tag();
        tag.setName(requestDto.getName());
        tag = tagRepository.save(tag);

        return mapToResponseDto(tag);
    }

    @Transactional
    @Override
    public TagResponseDto updateTag(Long id, TagRequestDto requestDto) {
        validateDuplicateTagName(requestDto.getName());
        Tag tag = findTagById(id);
        tag.setName(requestDto.getName());
        tag = tagRepository.save(tag);
        return mapToResponseDto(tag);
    }

    @Transactional
    @Override
    public void deleteTag(Long id) {
        Tag tag = findTagById(id);
        tagRepository.delete(tag);
    }

    @Transactional
    @Override
    public List<Tag> getOrCreateTagsByName(String tags) {
        if (tags == null || tags.trim().isEmpty()) { // null 체크 추가
            return Collections.emptyList();
        }
        List<String> tagNames = parseTags(tags);

        return tagNames.stream()
                .map(tagName -> tagRepository.findByName(tagName)
                        .orElseGet(() -> tagRepository.save(new Tag(tagName))))
                .toList();
    }

    @Transactional
    @Override
    public List<BookTag> createBookTags(List<Tag> tags) {
        return tags.stream()
                .map(tag -> {
                    BookTag bookTag = new BookTag();
                    bookTag.setTag(tag);
                    return bookTag;
                })
                .toList();
    }


    @Transactional(readOnly = true)
    @Override
    public String getFormattedTags(Book book) {
        List<BookTag> bookTags = bookTagRepository.findByBook(book);

        return bookTags.stream()
                .map(bookTag -> bookTag.getTag().getName())
                .collect(Collectors.joining(", "));
    }

    private List<String> parseTags(String tags) {
        if (tags == null || tags.trim().isEmpty()) {
            return Collections.emptyList(); // null 또는 빈 문자열이면 빈 리스트 반환
        }
        return Arrays.stream(tags.split(","))
                .map(String::trim) // 공백 제거
                .filter(tag -> !tag.isEmpty()) // 빈 태그 제거
                .toList();
    }

    private void validateDuplicateTagName(String name) {
        if (tagRepository.findByName(name).isPresent()) {
            throw new TagAlreadyExistException("이미 존재하는 태그입니다: " + name);
        }
    }

    private TagResponseDto mapToResponseDto(Tag tag) {
        return TagResponseDto.builder()
                .id(tag.getId())
                .name(tag.getName())
                .build();
    }

    private Tag findTagById(Long id) {
        return tagRepository.findById(id)
                .orElseThrow(() -> new TagNotFoundException("태그를 찾을 수 없습니다. ID: " + id));
    }
}
