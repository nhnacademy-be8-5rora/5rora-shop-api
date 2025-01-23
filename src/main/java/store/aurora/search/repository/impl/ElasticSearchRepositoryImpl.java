package store.aurora.search.repository.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.FunctionBoostMode;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.transport.TransportException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import store.aurora.book.dto.AuthorDTO;
import store.aurora.book.dto.SearchBookDTO;
import store.aurora.book.entity.Like;
import store.aurora.book.exception.api.InvalidApiResponseException;
import store.aurora.book.repository.book.BookViewRepository;
import store.aurora.book.repository.like.LikeRepository;
import store.aurora.document.AuthorDocument;
import store.aurora.document.CategoryDocument;
import store.aurora.document.PublisherDocument;
import store.aurora.review.repository.ReviewRepository;
import store.aurora.review.service.ReviewService;
import store.aurora.search.dto.BookSearchResponseDTO;
import store.aurora.search.repository.ElasticSearchRepositoryCustom;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Transactional
@Repository
@RequiredArgsConstructor
public class ElasticSearchRepositoryImpl implements ElasticSearchRepositoryCustom {
    private final ElasticsearchClient elasticsearchClient;
    private final BookViewRepository bookViewRepository;
    private final ReviewRepository reviewRepository;
    private final ReviewService reviewService;
    private final LikeRepository likeRepository;
    private static final Logger USER_LOG = LoggerFactory.getLogger("user-logger");
    public static final String BOOK_TAGS_NAME = "bookTags.name";
    public static final String TITLE = "title";
    public static final String AUTHORS_NAME = "authors.name";
    public static final String CATEGORIES_NAME = "categories.name";
    public static final String ACTIVE ="active";
    public static final String INDEX ="5rora";
    public static final String SCORE = "return _score >= 0.5 ? _score : 0;";

    private SearchRequest buildSearchRequest(String field, String keyword, int from, int size) {
        return new SearchRequest.Builder()
                .index(INDEX)
                .query(query -> query
                        .functionScore(functionScoreQuery -> functionScoreQuery
                                .query(innerQuery -> innerQuery
                                        .bool(boolQuery -> boolQuery
                                                .filter(filterQuery -> filterQuery
                                                        .term(term -> term
                                                                .field(ACTIVE)
                                                                .value(true) // active 필드가 true인 문서만 검색
                                                        )
                                                )
                                                .should(shouldQuery -> shouldQuery
                                                        .match(match -> match
                                                                .field(field) // 동적으로 필드 지정
                                                                .query(keyword) // 검색 키워드
                                                                .boost(13.0F)
                                                        )
                                                )
                                        )
                                )
                                .functions(functions -> functions
                                        .scriptScore(scriptScore -> scriptScore
                                                .script(script -> script
                                                        .inline(inline -> inline
                                                                .source(SCORE) // 점수 20 이상 유지
                                                        )
                                                )
                                        )
                                )
                                .boostMode(FunctionBoostMode.Replace) // 기존 점수를 대체
                                .minScore(0.5)  // 점수가 20 이상인 문서만 검색
                        )
                )
                .from(from) // 시작 인덱스
                .size(size) // 페이지 크기
                .sort(sort -> sort
                        .field(f -> f
                                .field("_score")
                                .order(SortOrder.Desc) // 점수를 기준으로 내림차순 정렬
                        )
                )
                .build();

    }


    private List<SearchBookDTO> executeFieldSearch(String field, String keyword,  Pageable pageable) {
        int from = pageable.getPageNumber() * pageable.getPageSize();
        int size = pageable.getPageSize();

        SearchRequest searchRequest = buildSearchRequest( field, keyword,  from, size);

        try {
            SearchResponse<SearchBookDTO> searchResponse = elasticsearchClient.search(searchRequest, SearchBookDTO.class);

            return searchResponse.hits().hits().stream()
                    .map(Hit::source)
                    .toList();
        } catch (TransportException e) {
            throw new InvalidApiResponseException("Elasticsearch 응답 디코딩 실패: " + e.getMessage());
        } catch (IOException e) {
            throw new InvalidApiResponseException("Elasticsearch 요청 처리 실패");
        }
    }

    @Override
    public Page<BookSearchResponseDTO> searchBooksByField(String field, String keyword, Pageable pageable) {
        if(field.equals(TITLE))
        {
            field=TITLE;
        }
        else if(field.equals("authors"))
        {
            field=AUTHORS_NAME;
        }
        else if(field.equals("category"))
        {
            field="category.id";
        }
        else if(field.equals("tag"))
        {
            field=BOOK_TAGS_NAME;
        }
        List<SearchBookDTO> bookList = executeFieldSearch(field, keyword,  pageable);
        List<BookSearchResponseDTO> bookSearchResponseDTOList = bookList.stream()
                .map(this::mapBookToDTO)
                .toList();

        long total = getTotalCountForField(field, keyword); // 필드별 총 개수 계산

        return new PageImpl<>(bookSearchResponseDTOList, pageable, total);
    }


    private long getTotalCountForField(String field, String keyword) {
        try {
            SearchRequest searchRequest = new SearchRequest.Builder()
                    .index(INDEX)
                    .query(query -> query
                            .functionScore(functionScoreQuery -> functionScoreQuery
                                    .query(innerQuery -> innerQuery
                                            .bool(boolQuery -> boolQuery
                                                    .filter(filterQuery -> filterQuery
                                                            .term(term -> term
                                                                    .field(ACTIVE)
                                                                    .value(true) // active 필드가 true인 문서만 검색
                                                            )
                                                    )
                                                    .should(shouldQuery -> shouldQuery
                                                            .match(match -> match
                                                                    .field(field) // 동적으로 필드 지정
                                                                    .query(keyword) // 검색 키워드
                                                            )
                                                    )
                                            )
                                    )
                                    .functions(functions -> functions
                                            .scriptScore(scriptScore -> scriptScore
                                                    .script(script -> script
                                                            .inline(inline -> inline
                                                                    .source(SCORE) // 점수 20 이상 유지
                                                            )
                                                    )
                                            )
                                    )
                                    .boostMode(FunctionBoostMode.Replace) // 기존 점수를 대체
                                    .minScore(0.5)  // 점수가 20 이상인 문서만 검색
                            )
                    )
                    .size(0) // 결과 데이터는 필요 없고, 총 히트 수만 계산
                    .trackTotalHits(tt -> tt.enabled(true)) // 총 히트 수 추적 활성화
                    .build();


            SearchResponse<SearchBookDTO> searchResponse = elasticsearchClient.search(searchRequest, SearchBookDTO.class);

            return searchResponse.hits().total() != null ? searchResponse.hits().total().value() : 0;
        } catch (IOException e) {
            throw new InvalidApiResponseException("Elasticsearch 요청 처리 실패: " + e.getMessage());
        }
    }


    @Override
    public Page<BookSearchResponseDTO> searchBooksWithWeightedFields(String keyword, Pageable pageable, String userId) {
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
                .index(INDEX)
                .query(query -> query
                        .functionScore(functionScoreQuery -> functionScoreQuery
                                .query(innerQuery -> innerQuery
                                        .bool(boolQuery -> boolQuery
                                                .filter(filterQuery -> filterQuery
                                                        .term(term -> term
                                                                .field(ACTIVE)
                                                                .value(true)  // active 필드가 true인 문서만 검색
                                                        )
                                                )
                                                .should(shouldQuery -> shouldQuery
                                                        .match(match -> match
                                                                .field(TITLE)
                                                                .query(keyword)
                                                                .boost(10.0F)
                                                        )
                                                )
                                                .should(shouldQuery -> shouldQuery
                                                        .match(match -> match
                                                                .field(AUTHORS_NAME)
                                                                .query(keyword)
                                                                .boost(3.0F)
                                                        )
                                                )
                                                .should(shouldQuery -> shouldQuery
                                                        .match(match -> match
                                                                .field(CATEGORIES_NAME)
                                                                .query(keyword)
                                                                .boost(3.0F)
                                                        )
                                                )
                                                .should(shouldQuery -> shouldQuery
                                                        .match(match -> match
                                                                .field(BOOK_TAGS_NAME)
                                                                .query(keyword)
                                                                .boost(3.0F)
                                                        )
                                                )
                                        )
                                )
                                .functions(functions -> functions
                                        .scriptScore(scriptScore -> scriptScore
                                                .script(script -> script
                                                        .inline(inline -> inline
                                                                .source(SCORE) // 점수가 20 이상인 경우만 유지
                                                        )
                                                )
                                        )
                                )
                                .boostMode(FunctionBoostMode.Replace) // 기존 점수 대신 새 점수를 사용
                                .minScore(0.5)  // 점수가 20 이상인 문서만 검색
                        )
                )
                .from(from)  // 시작 인덱스
                .size(size)  // 페이지 크기
                .sort(sort -> sort
                        .field(f -> f
                                .field("_score")
                                .order(SortOrder.Desc) // 점수 내림차순 정렬
                        )
                )
                .build();


        SearchResponse<SearchBookDTO> searchResponse;
        try {
            searchResponse = elasticsearchClient.search(searchRequest, SearchBookDTO.class);
        } catch (TransportException e) {
            throw new InvalidApiResponseException("Elasticsearch 응답 디코딩 실패: " + e.getMessage());
        } catch (IOException e) {
            throw new InvalidApiResponseException("Elasticsearch 요청 처리 중 알 수 없는 오류 발생");
        }

        // 검색 결과에서 데이터를 추출하여 List<SearchBookDTO>로 변환
        return searchResponse.hits().hits().stream()
                .map(Hit::source)
                .toList();
    }


    private long getTotalCount(String search) {
        try {
            // SearchRequest 생성
            // SearchRequest 생성
            SearchRequest searchRequest = new SearchRequest.Builder()
                    .index(INDEX) // 인덱스 설정
                    .query(query -> query
                            .functionScore(functionScoreQuery -> functionScoreQuery
                                    .query(innerQuery -> innerQuery
                                            .bool(boolQuery -> boolQuery
                                                    .filter(filterQuery -> filterQuery
                                                            .term(term -> term
                                                                    .field(ACTIVE)
                                                                    .value(true)  // active 필드가 true인 문서만 검색
                                                            )
                                                    )
                                                    .should(shouldQuery -> shouldQuery
                                                            .match(match -> match
                                                                    .field(TITLE)
                                                                    .query(search)
                                                                    .boost(10.0F)  // 제목에 높은 가중치 부여
                                                            )
                                                    )
                                                    .should(shouldQuery -> shouldQuery
                                                            .match(match -> match
                                                                    .field(AUTHORS_NAME)
                                                                    .query(search)
                                                                    .boost(3.0F)  // 저자 이름에 가중치 부여
                                                            )
                                                    )
                                                    .should(shouldQuery -> shouldQuery
                                                            .match(match -> match
                                                                    .field(CATEGORIES_NAME)
                                                                    .query(search)
                                                                    .boost(3.0F)  // 카테고리에 가중치 부여
                                                            )
                                                    )
                                                    .should(shouldQuery -> shouldQuery
                                                            .match(match -> match
                                                                    .field(BOOK_TAGS_NAME)
                                                                    .query(search)
                                                                    .boost(3.0F)  // 태그 이름에 가중치 부여
                                                            )
                                                    )
                                            )
                                    )
                                    .functions(functions -> functions
                                            .scriptScore(scriptScore -> scriptScore
                                                    .script(script -> script
                                                            .inline(inline -> inline
                                                                    .source(SCORE) // 점수 20 이상만 유지
                                                            )
                                                    )
                                            )
                                    )
                                    .boostMode(FunctionBoostMode.Replace) // 기존 점수 대신 스크립트 점수 사용
                                    .minScore(0.5)  // 점수가 20 이상인 문서만 검색
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


}
