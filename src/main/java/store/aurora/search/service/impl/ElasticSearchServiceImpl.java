package store.aurora.search.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import store.aurora.book.dto.AuthorDTO;
import store.aurora.book.dto.SearchBookDTO;
import store.aurora.book.dto.aladin.ImageDetail;
import store.aurora.book.entity.Book;
import store.aurora.book.entity.BookImage;
import store.aurora.book.entity.Like;
import store.aurora.book.entity.category.Category;
import store.aurora.book.exception.api.InvalidApiResponseException;
import store.aurora.book.repository.BookAuthorRepository;
import store.aurora.book.repository.BookViewRepository;
import store.aurora.book.repository.LikeRepository;
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
    private static final Logger USER_LOG = LoggerFactory.getLogger("user-logger");

    @Override
    public Page<BookSearchResponseDTO> searchBooks(String type,String keyword, Pageable pageable, String userId) {
        if("fullText".equalsIgnoreCase(type))
        {
            return searchBooksByFullText(keyword, pageable,userId);
        }
        return null;
    }

    private Page<BookSearchResponseDTO> searchBooksByFullText(String keyword, Pageable pageable, String userId) {

        List<SearchBookDTO> bookList = executeSearch(keyword, pageable);
        List<BookSearchResponseDTO> bookSearchResponseDTOList = bookList.stream()
                .map(this::mapBookToDTO)
                .toList();

//       좋아요 상태 조회: 한 번의 쿼리로 좋아요 상태 가져오기
        List<Long> bookIds = bookList.stream()
                .map(SearchBookDTO::getId)
                .toList();

        List<Like> likeList = likeRepository.findByUserIdAndBookIdInAndIsLikeTrue(userId, bookIds);

        //  좋아요 상태 반영: Map을 사용하여 책 ID와 좋아요 여부 매핑 N+1을 해결하기 위하여 백에서 for문으로 처리
        Set<Long> likedBookIds = likeList.stream()
                .map(like -> like.getBook().getId())  // Like 객체에서 Book의 ID를 가져옵니다.
                .collect(Collectors.toSet());

        for (BookSearchResponseDTO bookDTO : bookSearchResponseDTOList) {
            boolean liked = likedBookIds.contains(bookDTO.getId());
            bookDTO.setLiked(liked);  // 좋아요 상태 설정
        }

        return new PageImpl<>(bookSearchResponseDTOList, pageable, bookList.size());
    }

    private List<SearchBookDTO> executeSearch(String keyword, Pageable pageable) {
        int from = pageable.getPageNumber() * pageable.getPageSize();
        int size = pageable.getPageSize();

        SearchRequest searchRequest= new SearchRequest.Builder()
                .index("5rora")  // 사용할 인덱스 이름
                .query(query -> query
                        .bool(boolQuery -> boolQuery
                                .should(shouldQuery -> shouldQuery
                                        .match(match -> match
                                                .field("title")
                                                .query(keyword)
                                                .boost(2.0F)  // title 필드에 높은 가중치 부여
                                        )
                                )
                                .should(shouldQuery -> shouldQuery
                                        .match(match -> match
                                                .field("authors.name")  // AuthorDocument.name 필드
                                                .query(keyword)
                                                .boost(1.5F)  // 저자 이름에 대한 가중치
                                        )
                                )
                                .should(shouldQuery -> shouldQuery
                                        .match(match -> match
                                                .field("categories.name")  // CategoryDocument.name 필드
                                                .query(keyword)
                                                .boost(1.0F)  // 카테고리 이름에 대한 가중치
                                        )
                                )
                        )
                )
                .from(from)
                .size(size)
                .build();

        SearchResponse<SearchBookDTO> searchResponse;
        try {
            searchResponse = elasticsearchClient.search(searchRequest, SearchBookDTO.class);
        } catch (TransportException e) {
                throw new InvalidApiResponseException("Elasticsearch 응답 디코딩 실패: " + e.getMessage());
        } catch (IOException e) {
            throw new InvalidApiResponseException("Elasticsearch 응답중 알 수 없는 오류 발생" );
        }




        return searchResponse.hits().hits().stream()
                .map(Hit::source) // `BookDocument`로 변환
                .toList();
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
    public void saveBooks(Book book) {
        // BookRequestDto -> Book 변환
        BookDocument bookDocument = convertToBookDocument(book);

        // Elasticsearch에 저장
        try {
            elasticSearchRepository.save(bookDocument);
        } catch (Exception e) {
            USER_LOG.warn("엘라스틱 서치 레파지토리에 저장 중 오류 발생");
        }
    }

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

        //커버이미지는 db에서 얻어옴. N+1 발생하지만 save시 한번부르기때문에 1번만 발생.
        BookImage thumbnailImage = bookImageService.getThumbnail(book);
        if (thumbnailImage != null) {
            bookDocument.setCoverImage(thumbnailImage.getFilePath());
        }
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

        // Categories 변환 (재귀적으로 부모-자식 구조 처리)
        if (book.getBookCategories() != null && !book.getBookCategories().isEmpty()) {
            List<CategoryDocument> categories = book.getBookCategories().stream()
                    .map(bookCategory -> convertCategoryToDocument(bookCategory.getCategory()))
                    .toList();
            bookDocument.setCategories(categories);
        }


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

    private CategoryDocument convertCategoryToDocument(Category category) {
        if (category == null) return null;

        // 자식 카테고리 변환
        List<CategoryDocument> childDocuments = category.getChildren().stream()
                .map(this::convertCategoryToDocument)
                .toList();

        // 현재 카테고리와 자식 설정
        return new CategoryDocument(category.getId(), category.getName(), childDocuments);
    }



}
