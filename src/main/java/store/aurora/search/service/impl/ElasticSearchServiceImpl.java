package store.aurora.search.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.UpdateRequest;
import co.elastic.clients.elasticsearch.core.UpdateResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.transport.TransportException;
import com.google.common.collect.Lists;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import store.aurora.book.dto.AuthorDTO;
import store.aurora.book.dto.SearchBookDTO;
import store.aurora.book.entity.Book;
import store.aurora.book.entity.Like;
import store.aurora.book.entity.category.BookCategory;
import store.aurora.book.entity.category.Category;
import store.aurora.book.exception.api.InvalidApiResponseException;
import store.aurora.book.repository.BookAuthorRepository;
import store.aurora.book.repository.BookRepository;
import store.aurora.book.repository.BookViewRepository;
import store.aurora.book.repository.LikeRepository;
import store.aurora.book.repository.category.BookCategoryRepository;
import store.aurora.book.repository.category.CategoryRepository;
import store.aurora.book.service.BookImageService;
import store.aurora.document.*;
import store.aurora.review.repository.ReviewRepository;
import store.aurora.review.service.ReviewService;
import store.aurora.search.dto.BookSearchResponseDTO;
import store.aurora.search.repository.ElasticSearchRepository;
import store.aurora.search.service.ElasticSearchService;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class ElasticSearchServiceImpl implements ElasticSearchService {

    private final ElasticSearchRepository elasticSearchRepository;
    private final BookViewRepository bookViewRepository;
    private final ReviewRepository reviewRepository;
    private final ReviewService reviewService;
    private final LikeRepository likeRepository;
    private final BookImageService bookImageService;
    private final BookAuthorRepository bookAuthorRepository;
    private final ElasticsearchClient elasticsearchClient;
    private final BookCategoryRepository bookCategoryRepository;
    private static final Logger USER_LOG = LoggerFactory.getLogger("user-logger");
    private final CategoryRepository categoryRepository;
    private final BookRepository bookRepository;

    @Override
    public Page<BookSearchResponseDTO> searchBooks(String type,String keyword, Pageable pageable, String userId) {
        if("fullText".equalsIgnoreCase(type))
        {
            return searchBooksByFullText(keyword, pageable,userId);
        }
        return Page.empty();
    }

    private Page<BookSearchResponseDTO> searchBooksByFullText(String keyword, Pageable pageable, String userId) {
        // 책 목록 조회
        List<SearchBookDTO> bookList = executeSearch(keyword, pageable);
        List<BookSearchResponseDTO> bookSearchResponseDTOList = bookList.stream()
                .map(this::mapBookToDTO)
                .toList();

        // 좋아요 상태 조회: 한 번의 쿼리로 좋아요 상태 가져오기
        List<Long> bookIds = bookList.stream()
                .map(SearchBookDTO::getId)
                .toList();

        if (userId != null) {
            List<Like> likeList = likeRepository.findByUserIdAndBookIdInAndIsLikeTrue(userId, bookIds);

            // 좋아요 상태 반영
            Set<Long> likedBookIds = likeList.stream()
                    .map(like -> like.getBook().getId())
                    .collect(Collectors.toSet());

            bookSearchResponseDTOList.forEach(bookDTO -> bookDTO.setLiked(likedBookIds.contains(bookDTO.getId())));
        }

        // Elasticsearch에서 총 결과 수를 가져오는 방법
        long total = getTotalCount(keyword);

        // 결과 반환
        return new PageImpl<>(bookSearchResponseDTOList, pageable, total);
    }

    private List<SearchBookDTO> executeSearch(String keyword, Pageable pageable) {
        int from = pageable.getPageNumber() * pageable.getPageSize();  // 시작 인덱스
        int size = pageable.getPageSize();  // 페이지 크기

        // Elasticsearch 쿼리 생성
        SearchRequest searchRequest = new SearchRequest.Builder()
                .index("5rora")
                .query(query -> query
                        .bool(boolQuery -> boolQuery
                                .filter(filterQuery -> filterQuery
                                        .term(term -> term
                                                .field("active")
                                                .value(true)  // active 필드가 true인 문서만 검색
                                        )
                                )
                                .should(shouldQuery -> shouldQuery
                                        .match(match -> match
                                                .field("title")
                                                .query(keyword)
                                                .analyzer("edge_ngram_analyzer")
                                                .boost(10.0F)
                                        )
                                )
                                .should(shouldQuery -> shouldQuery
                                        .match(match -> match
                                                .field("authors.name")
                                                .query(keyword)
                                                .analyzer("edge_ngram_analyzer")
                                                .boost(1.0F)
                                        )
                                )
                                .should(shouldQuery -> shouldQuery
                                        .match(match -> match
                                                .field("categories.name")
                                                .query(keyword)
                                                .boost(1.0F)
                                        )
                                )
                                .should(shouldQuery -> shouldQuery
                                        .match(match -> match
                                                .field("bookTags.name")
                                                .query(keyword)
                                                .boost(1.0F)
                                        )
                                )
                        )
                )
                .from(from)  // 시작 인덱스
                .size(size)  // 페이지 크기
                .build();

        SearchResponse<SearchBookDTO> searchResponse;
        try {
            searchResponse = elasticsearchClient.search(searchRequest, SearchBookDTO.class);
        } catch (TransportException e) {
            throw new InvalidApiResponseException("Elasticsearch 응답 디코딩 실패: " + e.getMessage());
        } catch (IOException e) {
            throw new InvalidApiResponseException("Elasticsearch 응답중 알 수 없는 오류 발생");
        }

        // 검색 결과에서 데이터를 추출하여 List<SearchBookDTO>로 변환
        return searchResponse.hits().hits().stream()
                .map(Hit::source)
                .toList();
    }

    public long getTotalCount(String search) {
        try {
            // SearchRequest 생성
            SearchRequest searchRequest = new SearchRequest.Builder()
                    .index("5rora") // 인덱스 설정
                    .query(query -> query
                            .bool(boolQuery -> boolQuery
                                    .filter(filterQuery -> filterQuery
                                            .term(term -> term
                                                    .field("active")
                                                    .value(true)  // active 필드가 true인 문서만 검색
                                            )
                                    )
                                    .should(shouldQuery -> shouldQuery
                                            .match(match -> match
                                                    .field("title")
                                                    .query(search)
                                                    .boost(10.0F)  // 제목에 높은 가중치 부여
                                            )
                                    )
                                    .should(shouldQuery -> shouldQuery
                                            .match(match -> match
                                                    .field("authors.name")
                                                    .query(search)
                                                    .boost(1.0F)  // 저자 이름에 가중치 부여
                                            )
                                    )
                                    .should(shouldQuery -> shouldQuery
                                            .match(match -> match
                                                    .field("categories.name")
                                                    .query(search)
                                                    .boost(1.0F)  // 카테고리에 가중치 부여
                                            )
                                    )
                                    .should(shouldQuery -> shouldQuery
                                            .match(match -> match
                                                    .field("bookTags.name")
                                                    .query(search)
                                                    .boost(1.0F)  // 태그 이름에 가중치 부여
                                            )
                                    )
                            )
                    )
                    .size(0)  // 결과 데이터는 필요 없고, 총 히트 수만 계산
                    .trackTotalHits(tt -> tt.enabled(true)) // 총 히트 수 추적 활성화
                    .build();

            // Elasticsearch에서 쿼리 실행
            SearchResponse<SearchBookDTO> searchResponse =
                    elasticsearchClient.search(searchRequest, SearchBookDTO.class);

            // 전체 히트 수 반환 (total 히트 수가 null이 아닌 경우만 반환)
            return searchResponse.hits().total() != null ? searchResponse.hits().total().value() : 0;
        } catch (IOException e) {
            throw new InvalidApiResponseException("Elasticsearch 요청 처리 실패: " + e.getMessage());
        }
    }


    private BookSearchResponseDTO mapBookToDTO(SearchBookDTO book) {
        long viewCount = bookViewRepository.countByBookId(book.getId());
        int reviewCount = reviewRepository.countByBookId(book.getId());
        Double averageRating = reviewService.calculateAverageRating(book.getId());

        List<Long> categoryIds = book.getCategories().stream()
                .map(CategoryDocument::getId)  // CategoryDocument에서 id만 추출
                .toList();

        return new BookSearchResponseDTO(
                book.getId(),
                book.getTitle(),
                book.getRegularPrice(),
                book.getSalePrice(),
                LocalDate.parse(book.getPublishDate()),
                Optional.ofNullable(book.getPublisher())
                        .map(PublisherDocument::getName)
                        .orElse(""),
                book.getCoverImage(),
                convertAuthorsToDTO(book.getAuthors()), // AuthorDocument 리스트를 AuthorDTO 리스트로 변환
                categoryIds,
                viewCount,
                reviewCount,
                averageRating,
                false,
                book.isSale()
        );
    }
    private List<AuthorDTO> convertAuthorsToDTO(List<AuthorDocument> authors) {
        return authors.stream()
                .map(author -> new AuthorDTO(author.getName(), author.getRole())) // AuthorDocument를 AuthorDTO로 변환
                .toList();
    }

    //저장 후의 book 엔티티를 가져옴.
    @Override
    public void saveBook(Book book) {
        // BookRequestDto -> Book 변환
        BookDocument bookDocument = convertToBookDocument(book);

        // Elasticsearch에 저장
        try {
            elasticSearchRepository.save(bookDocument);
        } catch (Exception e) {
            USER_LOG.warn("엘라스틱 서치 레파지토리에 저장 중 오류 발생");
        }
    }

    //entity -> bookDocument
    private BookDocument convertToBookDocument(Book book) {
        // Book의 데이터를 BookDocument로 변환
        BookDocument bookDocument = new BookDocument();

        // 기본 정보 복사
        bookDocument.setId(book.getId());
        bookDocument.setTitle(book.getTitle());
        bookDocument.setRegularPrice(book.getRegularPrice());
        bookDocument.setSalePrice(book.getSalePrice());
        bookDocument.setStock(book.getStock());
        bookDocument.setSale(book.isSale());
        bookDocument.setIsbn(book.getIsbn());
        bookDocument.setContents(book.getContents());
        bookDocument.setExplanation(book.getExplanation());
        bookDocument.setPackaging(book.isPackaging());
        bookDocument.setPublishDate(book.getPublishDate().toString());
        bookDocument.setActive(book.isActive());
        //커버이미지는 db에서 얻어옴. N+1 발생하지만 save시 한번부르기때문에 1번만 발생.
        bookDocument.setCoverImage(bookImageService.getThumbnail(book).getFilePath());

        // Publisher 변환
        if (book.getPublisher() != null) {
            PublisherDocument publisherDocument = new PublisherDocument(
                    book.getPublisher().getId(),
                    book.getPublisher().getName()
            );
            bookDocument.setPublisher(publisherDocument);
        }

        // Series 변환
        if (book.getSeries() != null) {
            SeriesDocument seriesDocument = new SeriesDocument(
                    book.getSeries().getId(),
                    book.getSeries().getName()
            );
            bookDocument.setSeries(seriesDocument);
        }

        // Authors 변환
        List<AuthorDocument> authorDTOs = bookAuthorRepository.findAuthorsByBookId(book.getId());
        bookDocument.setAuthors(authorDTOs);

        // 도서 id에 해당하는 카테고리들의 id 를 반환받음.
        List<BookCategory> bookCategory = bookCategoryRepository.findBookCategoryByBookId(book.getId());
        List<Long> categoryIds = bookCategory.stream()
                .map(bc -> bc.getCategory().getId())  // 'bc'는 bookCategory 리스트의 각 요소
                .toList();
        //위에서 가져온 카테고리 id 에 해당하는 카테고리들을 가져옴.
        List<Category> categoryList = categoryRepository.findByIdIn(categoryIds);
        //카테고리 doc 객체로 변환
        List<CategoryDocument> categoryDocuments =
                categoryList.stream().map(category -> new CategoryDocument(category.getId(),category.getName())).toList();

        bookDocument.setCategories(categoryDocuments);

        // BookTags 변환
        if (book.getBookTags() != null && !book.getBookTags().isEmpty()) {
            List<TagDocument> tags = book.getBookTags().stream()
                    .map(bookTag -> new TagDocument(bookTag.getTag().getId(), bookTag.getTag().getName()))
                    .toList();
            bookDocument.setBookTags(tags);
        }

        // BookImages 변환
        if (book.getBookImages() != null && !book.getBookImages().isEmpty()) {
            List<BookImageDocument> bookImageDocuments = book.getBookImages().stream()
                    .map(bookImage -> new BookImageDocument(bookImage.getId(), bookImage.getFilePath(), bookImage.isThumbnail()))
                    .toList();
            bookDocument.setBookImages(bookImageDocuments);
        }

        return bookDocument;
    }


    @Override
    public Long saveBooksNotInElasticSearch() {
        List<Long> allBooksInDb  = bookRepository.findAll().stream().map(Book::getId).toList();
        List<Long> allBooksInElastic= findAllBooks();
        // allBooksInDb에서 allBooksInElastic에 없는 ID 필터링
        List<Long> idsNotInElastic = allBooksInDb.stream()
                .filter(id -> !allBooksInElastic.contains(id))
                .toList();

        List<Book> books = bookRepository.findALlByIdIn(idsNotInElastic);
        Long count = 0L;  // Long으로 선언하여 반환 타입과 맞춤
        for (Book book : books) {
            try {
                saveBook(book); // 저장 시도
                count++; // 성공한 경우에만 카운트 증가
            } catch (Exception e) {
                USER_LOG.warn("책 저장 실패: {} - {}", book.getId(), e.getMessage());
            }
        }

        return count;
    }

    private List<Long> findAllBooks() {
        try {
            // 쿼리 생성
            SearchRequest request = new SearchRequest.Builder()
                    .index("5rora")
                    .query(q -> q.matchAll(m -> m)) // match_all 쿼리
                    .size(1000)  // 한번에 1000개 가져오기
                    .storedFields(List.of())       // _id만 가져오기 위해 stored_fields 사용
                    .build();

            // Elasticsearch 요청 실행
            SearchResponse<Void> response = elasticsearchClient.search(request, Void.class);

            // _id 추출
            return response.hits().hits().stream()
                    .map(Hit::id) // Hit 객체에서 id만 추출
                    .map(Long::parseLong) // String -> Long 변환
                    .toList();

        } catch (IOException e) {
            throw new IllegalArgumentException("Elasticsearch 요청 실패: " + e.getMessage(), e);
        }
    }

}
