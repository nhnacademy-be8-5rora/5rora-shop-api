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
        return tagRepository.findByKeyword(keyword)
                .stream()
                .map(tag -> new TagResponseDto(tag.getId(), tag.getName()))
                .collect(Collectors.toList());
    }

    @Override
    public Page<TagResponseDto> getTags(Pageable pageable) {
        return tagRepository.findAll(pageable)
                .map(this::mapToResponseDto);
    }

    @Override
    public TagResponseDto getTagById(Long id) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("태그를 찾을 수 없습니다."));
        return mapToResponseDto(tag);
    }

    @Transactional
    @Override
    public TagResponseDto createTag(TagRequestDto requestDto) {
        if (tagRepository.findByName(requestDto.getName()).isPresent()) {
            throw new IllegalArgumentException("이미 존재하는 태그입니다.");
        }
        Tag tag = new Tag();
        tag.setName(requestDto.getName());
        tag = tagRepository.save(tag);

        return mapToResponseDto(tag);
    }

    @Transactional
    @Override
    public TagResponseDto updateTag(Long id, TagRequestDto requestDto) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("태그를 찾을 수 없습니다."));
        tag.setName(requestDto.getName());
        tag = tagRepository.save(tag);
        return mapToResponseDto(tag);
    }

    @Transactional
    @Override
    public void deleteTag(Long id) {
        if (!tagRepository.existsById(id)) {
            throw new IllegalArgumentException("태그를 찾을 수 없습니다.");
        }
        tagRepository.deleteById(id);
    }

    @Transactional
    @Override
    public List<Tag> getOrCreateTagsByName(String tags) {
        List<String> tagNames = parseTags(tags);
        if (tagNames == null || tagNames.isEmpty()) {
            return List.of();
        }

        return tagNames.stream()
                .map(tagName -> tagRepository.findByName(tagName)
                        .orElseGet(() -> tagRepository.save(new Tag(tagName))))
                .collect(Collectors.toList());
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
                .collect(Collectors.toList());
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
        if (tags == null || tags.isBlank()) {
            return Collections.emptyList();
        }

        return Arrays.stream(tags.split(","))
                .map(String::trim) // 공백 제거
                .filter(tag -> !tag.isEmpty()) // 빈 태그 제거
                .collect(Collectors.toList());
    }

    private TagResponseDto mapToResponseDto(Tag tag) {
        TagResponseDto responseDto = new TagResponseDto();
        responseDto.setId(tag.getId());
        responseDto.setName(tag.getName());
        return responseDto;
    }
}
