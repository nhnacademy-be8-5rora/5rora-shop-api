package store.aurora.book.service.impl;

import co.elastic.clients.elasticsearch._types.ElasticsearchException;
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
import store.aurora.book.exception.book.IsbnAlreadyExistsException;
import store.aurora.book.exception.book.NotFoundBookException;
import store.aurora.book.entity.*;
import store.aurora.book.exception.BookNotFoundException;
import store.aurora.book.mapper.BookMapper;
import store.aurora.book.repository.*;
import store.aurora.book.service.*;
import store.aurora.search.dto.BookSearchEntityDTO;
import store.aurora.search.dto.BookSearchResponseDTO;
import store.aurora.search.service.ElasticSearchService;

import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.data.domain.Page.empty;

@Service
@RequiredArgsConstructor
public class BookServiceImpl implements BookService {
    private final BookRepository bookRepository;
    private final LikeRepository likeRepository;
    private final BookImageRepository bookImageRepository;
    private final BookAuthorService bookAuthorService;
    private final BookImageService bookImageService;
    private final BookMapper bookMapper;
    private final ElasticSearchService elasticSearchService;

    private static final Logger USER_LOG = LoggerFactory.getLogger("user-logger");

    @Transactional
    @Override
    public void saveBook(BookRequestDto bookDto, MultipartFile coverImage, List<MultipartFile> additionalImages) {
        if (bookRepository.existsByIsbn(bookDto.getIsbn())) {
            throw new IsbnAlreadyExistsException(bookDto.getIsbn());
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

        //엘라스틱 서치를 위한 레파지토리에도 저장. 실패(서버 문제 등)에도 문제없도록 try-catch 처리.
        try {
            // book 엔티티를 조회하고, 해당 엔티티와 저자 정보를 가져옵니다.
            Optional<Book> optionalBook = bookRepository.findById(book.getId());

            // 엔티티가 존재하지 않으면 예외를 던집니다.
            if (optionalBook.isPresent()) {
                Book bookEntity = optionalBook.get();

                // Elasticsearch에 저장할 수 있도록 처리
                elasticSearchService.saveBooks(bookEntity);
            } else {
                USER_LOG.warn("Book not found with id: " + book.getId());
            }
        } catch (ElasticsearchException e) {
            // Elasticsearch 관련 예외 처리
            USER_LOG.warn("Elasticsearch 서버 오류: " + e.getMessage(), e);
        }catch (Exception e) {
            // 기타 예외 처리
            USER_LOG.warn("Elasticsearch 저장 실패: " + e.getMessage(), e);
        }

    }

    @Transactional
    @Override
    public void updateBook(Long bookId, BookRequestDto bookDto,
                           MultipartFile coverImage,
                           List<MultipartFile> additionalImages,
                           List<Long> deleteImageIds) {
        // 1. 기존 책 정보 조회
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new NotFoundBookException(bookId));
        // 2. 책 정보 업데이트
        bookMapper.updateEntityFromDto(book, bookDto);
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

    @Transactional(readOnly = true)
    @Override
    public Page<BookResponseDto> getBooksByActive(boolean isActive, Pageable pageable) {
        return bookRepository.findByActive(isActive, pageable)
                .map(this::convertToDto);
    }

    @Transactional
    @Override
    public void updateBookActivation(Long bookId, boolean isActive) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new NotFoundBookException(bookId));
        book.setActive(isActive); // 활성/비활성 상태 설정
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
            return Optional.empty();
        }

        Page<BookSearchResponseDTO> bookSearchResponseDTOPage = books.map(BookSearchResponseDTO::new);

        return bookSearchResponseDTOPage.getContent().isEmpty() ? Optional.empty() : Optional.ofNullable(bookSearchResponseDTOPage.getContent().getFirst());
    }

    // entity -> ResponseDto
    private BookResponseDto convertToDto(Book book) {
        return bookMapper.toResponseDto(book);
    }

}