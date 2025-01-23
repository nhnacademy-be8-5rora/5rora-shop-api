package store.aurora.book.service.book.impl;

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
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;
import store.aurora.book.dto.BookDetailsDto;
import store.aurora.book.dto.BookInfoDTO;
import store.aurora.book.dto.ReviewDto;
import store.aurora.book.dto.aladin.*;
import store.aurora.book.entity.Book;
import store.aurora.book.exception.book.BookDtoNullException;
import store.aurora.book.exception.book.IsbnAlreadyExistsException;
import store.aurora.book.exception.book.BookNotFoundException;
import store.aurora.book.entity.*;
import store.aurora.book.mapper.BookMapper;
import store.aurora.book.repository.book.BookRepository;
import store.aurora.book.repository.image.BookImageRepository;
import store.aurora.book.repository.like.LikeRepository;
import store.aurora.book.service.author.BookAuthorService;
import store.aurora.book.service.book.BookService;
import store.aurora.book.service.image.BookImageService;
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

    // 도서 등록
    @Transactional
    @Override
    public void saveBook(BookRequestDto bookDto, MultipartFile coverImage, List<MultipartFile> additionalImages) {
        Book book = saveOrUpdateBook(bookDto, null); // 책 등록
        bookImageService.processBookImages(book, null, coverImage, additionalImages); // 이미지 처리
        saveToElasticsearch(book);
    }

    @Transactional
    @Override
    public void saveBookFromApi(AladinBookRequestDto bookDto, List<MultipartFile> additionalImages) {
        validateBookDto(bookDto);
        validateIsbnDuplicate(bookDto.getIsbn());

        Book book = bookMapper.aladinToEntity(bookDto);
        // 책 저장
        bookRepository.save(book);
        // 작가 정보 저장
        bookAuthorService.parseAndSaveBookAuthors(book, bookDto.getAuthor());
        // 커버 이미지 url 과 추가 이미지 저장
        bookImageService.processBookImages(book, bookDto.getCover(), null, additionalImages);

        saveToElasticsearch(book);
    }

    //도서 수정
    @Transactional
    @Override
    public void updateBook(Long bookId, BookRequestDto bookDto,
                           MultipartFile coverImage,
                           List<MultipartFile> additionalImages,
                           List<Long> deleteImageIds) {
        Book book = saveOrUpdateBook(bookDto, bookId); // 기존 책 수정

        if (!CollectionUtils.isEmpty(deleteImageIds)) {
            bookImageService.deleteImages(deleteImageIds);
        }
        bookImageService.processBookImages(book, null, coverImage, additionalImages);

        int retryCount = 0;
        int maxRetries = 3;

        while (retryCount < maxRetries) {
            try {
                elasticSearchService.saveBook(book);
                break; // 성공 시 루프 종료
            } catch (ElasticsearchException e) {
                retryCount++;
                if (retryCount == maxRetries) {
                    USER_LOG.warn("엘라스틱 서치에 데이터를 반영하는 중 실패. 최대 재시도 횟수 도달.");
                } else {
                    USER_LOG.info("엘라스틱 서치 저장 재시도 중: {}",retryCount + "회");
                }
            }
        }
    }

    @Transactional
    @Override
    public void updateBookStockOnOrder(Long bookId, int quantity) {
        /*
        *  1. 재고 감소
        *  2. 재고 감소 후 책 정보 저장
        *  3. todo: 재고 부족 시 어떻게 할지?
         */

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new BookNotFoundException(bookId));

        int newStock = book.getStock() - quantity;

        if (newStock < 0) {
            throw new IllegalArgumentException("재고가 부족합니다.");
        }

        book.setStock(quantity);
        bookRepository.save(book);
    }

    // 책 활성/비활성(soft 삭제 기능)
    @Transactional
    @Override
    public void updateBookActivation(Long bookId, boolean isActive) {
        Book book = findBookById(bookId);
        book.setActive(isActive);
        bookRepository.save(book);

        elasticSearchService.saveBook(book);
    }

    // 활성/비활성 도서 페이징 처리해서 목록 불러오기
    @Transactional(readOnly = true)
    @Override
    public Page<BookResponseDto> getBooksByActive(boolean isActive, Pageable pageable) {
        return bookRepository.findByActive(isActive, pageable)
                .map(this::convertToDto);
    }

    // 책 수정 폼 불러오기
    @Transactional(readOnly = true)
    @Override
    public BookDetailDto getBookDetailsForAdmin(Long bookId) {
        validateBookExists(bookId);
        Book book = findBookById(bookId);
        // 관리자용 상세정보 가져오기
        return bookMapper.toDetailDto(book);
    }

    @Override
    @Transactional(readOnly = true)
    public Book getBookById(Long bookId) {
        return findBookById(bookId);
    }

    @Override
    @Transactional(readOnly = true)
    public BookDetailsDto getBookDetails(Long bookId) {
        validateBookExists(bookId);

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
    @Override
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

    @Override
    @Transactional(readOnly = true)
    public void notExistThrow(Long bookId) {
        validateBookExists(bookId);
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
    public Optional<BookSearchResponseDTO>  findMostSoldByLastMonth() {
        Tuple bookIdTuple = bookRepository.findMostSoldByLastMonth();
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

    //private

    private void validateBookDto(Object bookDto) {
        if (bookDto == null) {
            throw new BookDtoNullException("bookDto가 null 입니다.");
        }
    }

    private void validateBookExists(Long bookId) {
        if (!bookRepository.existsById(bookId)) {
            throw new BookNotFoundException(bookId);
        }
    }

    private void validateIsbnDuplicate(String isbn) {
        if (bookRepository.existsByIsbn(isbn)) {
            throw new IsbnAlreadyExistsException(isbn);
        }
    }

    private Book findBookById(Long bookId) {
        return bookRepository.findById(bookId)
                .orElseThrow(() -> new BookNotFoundException(bookId));
    }

    // book -> Dto
    private BookResponseDto convertToDto(Book book) {
        return bookMapper.toResponseDto(book);
    }

    // 저장와 업데이트 통합
    private Book saveOrUpdateBook(BookRequestDto bookDto, Long bookId) {
        validateBookDto(bookDto);

        Book book;
        if (bookId == null) { // 새 책 등록
            validateIsbnDuplicate(bookDto.getIsbn());
            book = bookMapper.toEntity(bookDto);
        } else { // 기존 책 수정
            book = findBookById(bookId);
            bookMapper.updateEntityFromDto(book, bookDto);
        }

        bookRepository.save(book);
        bookAuthorService.parseAndSaveBookAuthors(book, bookDto.getAuthor());

        return book;
    }
    private void saveToElasticsearch(Book book) {
        //엘라스틱 서치를 위한 레파지토리에도 저장. 실패(서버 문제 등)에도 문제없도록 try-catch 처리.
        try {
            // book 엔티티를 조회하고, 해당 엔티티와 저자 정보를 가져옵니다.
            Optional<Book> optionalBook = bookRepository.findById(book.getId());

            // 엔티티가 존재하지 않으면 예외를 던집니다.
            if (optionalBook.isPresent()) {
                Book bookEntity = optionalBook.get();

                // Elasticsearch에 저장할 수 있도록 처리
                elasticSearchService.saveBook(bookEntity);
            } else {
                USER_LOG.warn("Book not found with id: {}", book.getId());
            }
        } catch (ElasticsearchException e) {
            // Elasticsearch 관련 예외 처리
            USER_LOG.warn("Elasticsearch 서버 오류: " + e.getMessage(), e);
        } catch (Exception e) {
            // 기타 예외 처리
            USER_LOG.warn("Elasticsearch 저장 실패: " + e.getMessage(), e);
        }
    }
}