package store.aurora.book.mapper;

import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import store.aurora.book.dto.aladin.*;
import store.aurora.book.entity.Book;
import store.aurora.book.entity.BookImage;
import store.aurora.book.entity.Publisher;
import store.aurora.book.entity.Series;
import store.aurora.book.entity.category.BookCategory;
import store.aurora.book.entity.category.Category;
import store.aurora.book.entity.tag.BookTag;
import store.aurora.book.entity.tag.Tag;
import store.aurora.book.service.BookAuthorService;
import store.aurora.book.service.BookImageService;
import store.aurora.book.service.PublisherService;
import store.aurora.book.service.SeriesService;
import store.aurora.book.service.category.CategoryService;
import store.aurora.book.service.tag.TagService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class BookMapper {
    private final PublisherService publisherService;
    private final SeriesService seriesService;
    private final CategoryService categoryService;
    private final TagService tagService;
    private final BookAuthorService bookAuthorService;
    private final BookImageService bookImageService;

    // BookRequestDto -> Book 변환
    public Book aladinToEntity(AladinBookRequestDto bookDto) {
        Book book = new Book();
        book.setTitle(bookDto.getTitle());
        book.setExplanation(bookDto.getDescription());
        book.setContents(!StringUtils.isBlank(bookDto.getContents()) ? bookDto.getContents() : null);
        book.setIsbn(bookDto.getIsbn13());
        book.setSalePrice(bookDto.getPriceSales());
        book.setRegularPrice(bookDto.getPriceStandard());
        book.setPublishDate(!StringUtils.isBlank(bookDto.getPubDate())
                ? LocalDate.parse(bookDto.getPubDate(), DateTimeFormatter.ofPattern("yyyy-MM-dd")) : null);
        book.setStock(bookDto.getStock());
        book.setSale(bookDto.isSale());
        book.setPackaging(bookDto.isPackaging());

        // Publisher
        book.setPublisher(publisherService.getOrCreatePublisher(bookDto.getPublisher()));

        // Series
        if (bookDto.getSeriesInfo() != null && !StringUtils.isBlank(bookDto.getSeriesInfo().getSeriesName())) {
            book.setSeries(seriesService.getOrCreateSeries(bookDto.getSeriesInfo().getSeriesName()));
        }

        // Categories
        List<BookCategory> bookCategories = categoryService.createBookCategories(bookDto.getCategoryIds());
        bookCategories.forEach(book::addBookCategory);

        // Tags
        if (!StringUtils.isBlank(bookDto.getTags())) {
            // 태그 파싱 및 생성/조회
            List<Tag> tags = tagService.getOrCreateTagsByName(bookDto.getTags());

            // BookTag 생성 및 연결
            List<BookTag> bookTags = tagService.createBookTags(tags);
            bookTags.forEach(book::addBookTag);
        }
        return book;
    }

    public Book toEntity(BookRequestDto bookDto) {
        Book book = new Book();
        book.setTitle(bookDto.getTitle());
        book.setExplanation(bookDto.getDescription());
        book.setContents(!StringUtils.isBlank(bookDto.getContents()) ? bookDto.getContents() : null);
        book.setIsbn(bookDto.getIsbn());
        book.setSalePrice(bookDto.getPriceSales());
        book.setRegularPrice(bookDto.getPriceStandard());
        book.setPublishDate(bookDto.getPubDate());
        book.setStock(bookDto.getStock());
        book.setSale(bookDto.isSale());
        book.setPackaging(bookDto.isPackaging());

        // Publisher
        book.setPublisher(publisherService.getOrCreatePublisher(bookDto.getPublisher()));

        // Series
        if (!StringUtils.isBlank(bookDto.getSeriesName())) {
            book.setSeries(seriesService.getOrCreateSeries(bookDto.getSeriesName()));
        }

        // Categories
        List<BookCategory> bookCategories = categoryService.createBookCategories(bookDto.getCategoryIds());
        bookCategories.forEach(book::addBookCategory);

        // Tags
        if (!StringUtils.isBlank(bookDto.getTags())) {
            // 태그 파싱 및 생성/조회
            List<Tag> tags = tagService.getOrCreateTagsByName(bookDto.getTags());

            // BookTag 생성 및 연결
            List<BookTag> bookTags = tagService.createBookTags(tags);
            bookTags.forEach(book::addBookTag);
        }
        return book;
    }


    // Book -> BookResponseDto 변환
    public BookResponseDto toResponseDto(Book book) {
        BookResponseDto bookDto = new BookResponseDto();
        bookDto.setId(book.getId());
        bookDto.setTitle(book.getTitle());
        bookDto.setAuthor(bookAuthorService.getFormattedAuthors(book));
        BookImage thumbnailImage = bookImageService.getThumbnail(book);
        if (thumbnailImage != null) {
            bookDto.setCover(thumbnailImage.getFilePath());
        } else {
            bookDto.setCover(null);
        }
        bookDto.setDescription(book.getExplanation());
        bookDto.setIsbn13(book.getIsbn());
        bookDto.setPriceSales(book.getSalePrice());
        bookDto.setPriceStandard(book.getRegularPrice());
        bookDto.setPubDate(book.getPublishDate() != null ? book.getPublishDate().toString() : null);
        bookDto.setStock(book.getStock());
        bookDto.setSale(book.isSale());
        bookDto.setPackaging(book.isPackaging());
        bookDto.setPublisher(book.getPublisher().getName());
        return bookDto;
    }

    // Book -> BookDetailDto 변환
    public BookDetailDto toDetailDto(Book book) {
        BookDetailDto bookDetailDto = new BookDetailDto();
        bookDetailDto.setTitle(book.getTitle());
        bookDetailDto.setAuthor(bookAuthorService.getFormattedAuthors(book));
        bookDetailDto.setDescription(book.getExplanation());
        bookDetailDto.setContents(book.getContents());
        bookDetailDto.setIsbn(book.getIsbn());
        bookDetailDto.setPublisher(book.getPublisher().getName());
        bookDetailDto.setPriceStandard(book.getRegularPrice());
        bookDetailDto.setPriceSales(book.getSalePrice());
        bookDetailDto.setPubDate(book.getPublishDate());
        bookDetailDto.setStock(book.getStock());
        bookDetailDto.setSale(book.isSale());
        bookDetailDto.setPackaging(book.isPackaging());
        bookDetailDto.setSeriesName(book.getSeries() != null ? book.getSeries().getName() : null);
        bookDetailDto.setCategoryIds(book.getBookCategories().stream()
                .map(category -> category.getCategory().getId())
                .toList());
        bookDetailDto.setTags(tagService.getFormattedTags(book));
        // Cover 이미지 처리
        BookImage thumbnailImage = bookImageService.getThumbnail(book);
        if (thumbnailImage != null) {
            bookDetailDto.setCover(new ImageDetail(thumbnailImage.getId(), thumbnailImage.getFilePath()));
        }

        // Additional Images 처리
        List<ImageDetail> additionalImages = bookImageService.getAdditionalImages(book).stream()
                .map(image -> new ImageDetail(image.getId(), image.getFilePath()))
                .toList();
        bookDetailDto.setExistingAdditionalImages(additionalImages);
        return bookDetailDto;
    }

    public void updateEntityFromDto(Book book, BookRequestDto bookDto) {
        book.setTitle(bookDto.getTitle());
        book.setExplanation(bookDto.getDescription());
        book.setContents(bookDto.getContents());
        book.setIsbn(bookDto.getIsbn());
        book.setSalePrice(bookDto.getPriceSales());
        book.setRegularPrice(bookDto.getPriceStandard());
        book.setPublishDate(bookDto.getPubDate());
        book.setStock(bookDto.getStock());
        book.setSale(bookDto.isSale());
        book.setPackaging(bookDto.isPackaging());

        // Publisher 업데이트
        Publisher publisher = publisherService.getOrCreatePublisher(bookDto.getPublisher());
        book.setPublisher(publisher);

        // Series 업데이트
        if (!StringUtils.isBlank(bookDto.getSeriesName())) {
            Series series = seriesService.getOrCreateSeries(bookDto.getSeriesName());
            book.setSeries(series);
        }
        // Category 업데이트
        categoryService.updateBookCategories(book, bookDto.getCategoryIds());

        // 태그 업데이트
        if (!StringUtils.isBlank(bookDto.getTags())) {
            List<Tag> tags = tagService.getOrCreateTagsByName(bookDto.getTags());
            book.clearBookTags();
            for (Tag tag : tags) {
                BookTag bookTag = new BookTag();
                bookTag.setTag(tag);
                book.addBookTag(bookTag);
            }
        }
    }
}
