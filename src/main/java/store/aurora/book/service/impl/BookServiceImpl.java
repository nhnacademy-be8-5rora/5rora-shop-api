package store.aurora.book.service.impl;

import com.querydsl.core.Tuple;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import store.aurora.book.dto.BookDetailsDto;
import store.aurora.book.dto.BookInfoDTO;
import store.aurora.book.dto.ReviewDto;
import store.aurora.book.dto.aladin.*;
import store.aurora.book.entity.Book;
import store.aurora.book.entity.Publisher;
import store.aurora.book.entity.Series;
import store.aurora.book.entity.category.BookCategory;
import store.aurora.book.entity.category.Category;
import store.aurora.book.entity.tag.BookTag;
import store.aurora.book.entity.tag.Tag;
import store.aurora.book.exception.book.IsbnAlreadyExistsException;
import store.aurora.book.exception.book.NotFoundBookException;
import store.aurora.book.entity.*;
import store.aurora.book.exception.BookNotFoundException;
import store.aurora.book.mapper.BookMapper;
import store.aurora.book.repository.*;
import store.aurora.book.repository.category.CategoryRepository;
import store.aurora.book.service.*;
import store.aurora.book.service.tag.TagService;
import store.aurora.search.dto.BookSearchEntityDTO;
import store.aurora.search.dto.BookSearchResponseDTO;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.data.domain.Page.empty;

@Service
@RequiredArgsConstructor
public class BookServiceImpl implements BookService {
    private final BookRepository bookRepository;
    private final LikeRepository likeRepository;
    private final TagService tagService;
    private final BookImageRepository bookImageRepository;
    private final BookAuthorService bookAuthorService;
    private final BookImageService bookImageService;
    private final PublisherRepository publisherRepository;
    private final SeriesRepository seriesRepository;
    private final CategoryRepository categoryRepository;
    private final BookMapper bookMapper;

    private static final Logger USER_LOG = LoggerFactory.getLogger("user-logger");

    @Transactional
    @Override
    public void saveBook(BookRequestDto bookDto, MultipartFile coverImage, List<MultipartFile> additionalImages) {
        if (bookRepository.existsByIsbn(bookDto.getIsbn13())) {
            throw new IsbnAlreadyExistsException(bookDto.getIsbn13());
        }
        Book book = bookMapper.toEntity(bookDto);
        // 책 저장
        bookRepository.save(book);
        // 작가 정보 저장
        bookAuthorService.parseAndSaveBookAuthors(book, bookDto.getAuthor());
        // 커버 이미지 처리
        bookImageService.handleImageUpload(book,coverImage, true);
        // 추가 이미지 처리
        bookImageService.handleAdditionalImages(book, additionalImages);
    }

    @Transactional
    @Override
    public void updateBook(Long bookId, BookRequestDto bookDto,
                           MultipartFile coverImage,
                           List<MultipartFile> additionalImages,
                           List<Long> deleteImageIds) {
        // 1. 기존 책 정보 조회
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("Book not found with ID: " + bookId));

        // 2. 책 정보 업데이트
        updateBookInfo(book, bookDto);

        // 3. 이미지 삭제 처리
        if (deleteImageIds != null && !deleteImageIds.isEmpty()) {
            bookImageService.deleteImages(deleteImageIds);
        }

        // 4. 커버 이미지 처리
        if (coverImage != null && !coverImage.isEmpty()) {
            bookImageService.handleImageUpload(book, coverImage, true);
        }

        // 5. 추가 이미지 처리
        if (additionalImages != null && !additionalImages.isEmpty()) {
            bookImageService.handleAdditionalImages(book, additionalImages);
        }

        // 6. 책 저장
        bookRepository.save(book);
    }

    @Override
    public Page<BookResponseDto> getAllBooks(Pageable pageable) {
        return bookRepository.findAll(pageable)
                .map(this::convertToDto);
    }

    @Transactional(readOnly = true)
    @Override
    public BookDetailDto getBookDetailsForAdmin(Long bookId) {
        if (!bookRepository.existsById(bookId)) {
            throw new NotFoundBookException(bookId);
        }

        // 관리자용 상세정보 가져오기 (리뷰, 평점 등 제외)
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new NotFoundBookException(bookId));

        return bookMapper.toDetailDto(book);
    }


    @Transactional(readOnly = true)
    public Book getBookById(Long bookId) {
        return bookRepository.findById(bookId)
                .orElseThrow(() -> new BookNotFoundException(bookId));
    }

    @Transactional(readOnly = true)
    public BookDetailsDto getBookDetails(Long bookId) {

        if (!bookRepository.existsById(bookId)) {
            throw new NotFoundBookException(bookId);
        }

        BookDetailsDto bookDetailsDto = bookRepository.findBookDetailsByBookId(bookId);

        double sum = 0;
        double avg = 0.0;
        if(Objects.nonNull(bookDetailsDto.getReviews())) {
            for (ReviewDto reviewDto : bookDetailsDto.getReviews()) {
                int reviewRating = reviewDto.getReviewRating();
                sum += reviewRating;
            }
            avg = Math.round((sum / bookDetailsDto.getReviews().size() * 10) / 10.0);
        }


        bookDetailsDto.setRating(avg);

        return bookDetailsDto;
    }

    public List<BookInfoDTO> getBookInfo(List<Long> bookIds) {
        List<Book> books = bookRepository.findAllById(bookIds);

        Map<Long, Book> bookMap = books.stream()
                .collect(Collectors.toMap(Book::getId, book -> book));

        return bookIds.stream()
                .map(bookId -> {
                    Book book = bookMap.get(bookId);
                    BookInfoDTO bookInfoDTO = new BookInfoDTO();
                    bookInfoDTO.setTitle(book.getTitle());
                    bookInfoDTO.setRegularPrice(book.getRegularPrice());
                    bookInfoDTO.setSalePrice(book.getSalePrice());
                    bookInfoDTO.setStock(book.getStock());
                    bookInfoDTO.setSale(book.isSale());

                    List<BookImage> bookImages = bookImageRepository.findByBook(book);
                    if (!bookImages.isEmpty()) {
                        bookInfoDTO.setFilePath(bookImages.get(0).getFilePath());
                    }

                    return bookInfoDTO;
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public void notExistThrow(Long bookId) {
        if (!bookRepository.existsById(bookId))
            throw new BookNotFoundException(bookId);
    }

    @Override
    public Page<BookSearchResponseDTO> getBooksByLike(String userId, Pageable pageable) {
        // 1. 사용자가 좋아요를 누른 책 리스트 조회
        List<Like> likes = likeRepository.findByUserIdAndIsLikeTrue(userId);
        List<Long> bookIds = new ArrayList<>();
        for (Like like : likes) {
            Long bookId = like.getBook().getId();
            bookIds.add(bookId);
        }

        // 2. bookIds가 비어있으면 빈 Page를 반환
        if (bookIds.isEmpty()) {
            return empty(pageable);  // 빈 페이지 반환
        }

        // 3. 좋아요한 책들 조회 (BookSearchEntityDTO 형태로)
        Page<BookSearchEntityDTO> books = bookRepository.findBookByIdIn(bookIds, pageable);
        // BookSearchEntityDTO -> BookSearchResponseDTO로 변환
        Page<BookSearchResponseDTO> bookSearchResponseDTOPage = books.map(BookSearchResponseDTO::new);

        for (BookSearchResponseDTO book : bookSearchResponseDTOPage.getContent()) {
            book.setLiked(true); // 좋아요 상태를 DTO에 추가
        }

        // 4. BookSearchEntityDTO 리스트를 BookResponseDTO로 변환
        return bookSearchResponseDTOPage;
    }

    @Override
    public Optional<BookSearchResponseDTO>  findMostSeller() {
        Tuple bookIdTuple = bookRepository.findMostSoldBook();
        if (bookIdTuple == null) {
            // bookIdTuple이 null일 경우 로그를 남기고 빈 값 반환
            USER_LOG.info("No most sold book found for last month.");
            return Optional.empty();  // 빈 객체 반환
        }

        Long bookId = bookIdTuple.get(0, Long.class);  // 0번째가 bookId
        List<Long> bookIds = new ArrayList<>();
        bookIds.add(bookId);  // 하나의 bookId를 리스트에 추가

        Pageable pageable = PageRequest.of(0, 1);
        Page<BookSearchEntityDTO> books = bookRepository.findBookByIdIn(bookIds, pageable);

        if (books.isEmpty()) {
            USER_LOG.info("No books found for the given book ID: {}", bookId);
            return null;
        }

        Page<BookSearchResponseDTO> bookSearchResponseDTOPage = books.map(BookSearchResponseDTO::new);

        return bookSearchResponseDTOPage.getContent().isEmpty() ? null : Optional.ofNullable(bookSearchResponseDTOPage.getContent().getFirst());
    }


    // entity -> ResponseDto
    private BookResponseDto convertToDto(Book book) {
        return bookMapper.toResponseDto(book);
    }

    private void updateBookInfo(Book book, BookRequestDto bookDto) {
        book.setTitle(bookDto.getTitle());
        book.setExplanation(bookDto.getDescription());
        book.setContents(bookDto.getContents());
        book.setIsbn(bookDto.getIsbn13());
        book.setSalePrice(bookDto.getPriceSales());
        book.setRegularPrice(bookDto.getPriceStandard());
        book.setPublishDate(bookDto.getPubDate() != null ?
                LocalDate.parse(bookDto.getPubDate(), DateTimeFormatter.ofPattern("yyyy-MM-dd")) : null);
        book.setStock(bookDto.getStock());
        book.setSale(bookDto.getIsForSale());
        book.setPackaging(bookDto.getIsPackaged());

        // Publisher 업데이트
        Publisher publisher = publisherRepository.findByName(bookDto.getPublisher())
                .orElseGet(() -> publisherRepository.save(new Publisher(bookDto.getPublisher())));
        book.setPublisher(publisher);

        // Series 업데이트
        if (!bookDto.getSeriesName().isBlank()) {
            Series series = seriesRepository.findByName(bookDto.getSeriesName())
                    .orElseGet(() -> seriesRepository.save(new Series(bookDto.getSeriesName())));
            book.setSeries(series);
        }

        // Category 업데이트
        List<Category> categories = categoryRepository.findAllById(bookDto.getCategoryIds());
        book.clearBookCategories(); // 기존 카테고리 제거
        for (Category category : categories) {
            BookCategory bookCategory = new BookCategory();
            bookCategory.setCategory(category);
            book.addBookCategory(bookCategory);
        }

        // 7. 태그 업데이트
        if (bookDto.getTags() != null && !bookDto.getTags().isEmpty()) {
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