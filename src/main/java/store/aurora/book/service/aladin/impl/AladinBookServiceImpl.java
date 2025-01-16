package store.aurora.book.service.aladin.impl;

import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;
import store.aurora.book.dto.aladin.AladinApiResponse;
import store.aurora.book.dto.aladin.AladinBookRequestDto;
import store.aurora.book.entity.Book;
import store.aurora.book.exception.book.IsbnAlreadyExistsException;
import store.aurora.book.mapper.BookMapper;
import store.aurora.book.repository.BookRepository;
import store.aurora.book.service.aladin.AladinBookRedisService;
import store.aurora.book.service.aladin.AladinBookService;
import store.aurora.book.service.BookAuthorService;
import store.aurora.book.service.BookImageService;
import store.aurora.book.util.AladinBookClient;
import store.aurora.search.service.ElasticSearchService;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AladinBookServiceImpl implements AladinBookService {

    private final AladinBookClient aladinBookClient;
    private final BookRepository bookRepository;
    private final BookMapper bookMapper;
    private final BookAuthorService bookAuthorService;
    private final BookImageService bookImageService;
    private final ObjectMapper objectMapper;
    private final AladinBookRedisService aladinBookRedisService;
    private final ElasticSearchService elasticSearchService;
    private static final Logger USER_LOG = LoggerFactory.getLogger("user-logger");

    @Value("${aladin.api.ttb-key}")
    private String ttbKey;

    @Transactional
    @Override
    public void saveBookFromApi(AladinBookRequestDto bookDto, List<MultipartFile> additionalImages) {
        if (bookRepository.existsByIsbn(bookDto.getIsbn13())) {
            throw new IsbnAlreadyExistsException(bookDto.getIsbn13());
        }
        Book book = bookMapper.aladinToEntity(bookDto);
        // 책 저장
        bookRepository.save(book);
        // 작가 정보 저장
        bookAuthorService.parseAndSaveBookAuthors(book, bookDto.getAuthor());
        // 커버 이미지 url 과 추가 이미지 저장
        bookImageService.processApiImages(book, bookDto.getCover(), additionalImages);

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

    @Override
    public List<AladinBookRequestDto> searchBooks(String query, String queryType, String searchTarget, int start) {
        String cacheKey = createCacheKey(query, queryType, searchTarget, start);

        // Redis에서 데이터 조회
        List<AladinBookRequestDto> cachedBooks = aladinBookRedisService.getBooks(cacheKey);
        if (!CollectionUtils.isEmpty(cachedBooks)) {
            return cachedBooks;
        }
        // 알라딘 API 호출
        List<AladinBookRequestDto> books = getBooksFromApi(query, queryType, searchTarget, start);

        // Redis에 데이터 저장
        aladinBookRedisService.storeBooks(cacheKey, books);

        return books;
    }

    @Override
    public AladinBookRequestDto getBookDetailsByIsbn(String isbn13) {
        String bookCacheKey = "book:" + isbn13;

        // Redis에서 데이터 조회
        AladinBookRequestDto book = aladinBookRedisService.getBook(bookCacheKey);
        if (book != null) {
            return book;
        }

        // Redis에 데이터가 없을 경우, 외부 API 호출
        book = getBookDetailsFromApi(isbn13);

        // Redis에 저장
        if (book != null) {
            aladinBookRedisService.storeBooks(bookCacheKey, List.of(book));
        }

        return book;
    }

    private String createCacheKey(String query, String queryType, String searchTarget, int start) {
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        return String.format("search:%s:type:%s:target:%s:page:%d", encodedQuery,queryType,searchTarget,start);
    }

    // 알라딘 검색 api
    private List<AladinBookRequestDto> getBooksFromApi(String query, String queryType, String searchTarget, int start) {
        try {
            String response = aladinBookClient.searchBooks(
                    ttbKey, query, queryType, searchTarget, start, 50, "js", "20131101"
            );

            if (StringUtils.isBlank(response)) {
                USER_LOG.info("알라딘 API 응답이 비어 있습니다. Query: {}", query);
                return Collections.emptyList();
            }

            AladinApiResponse apiResponse = objectMapper.readValue(response, AladinApiResponse.class);
            if (apiResponse == null || CollectionUtils.isEmpty(apiResponse.getItems())) {
                USER_LOG.info("알라딘 API 응답에 유효한 데이터가 없습니다. Query: {}", query);
                return Collections.emptyList();
            }

            return apiResponse.getItems();
        } catch (JsonProcessingException e) {
            USER_LOG.warn("API 응답 파싱 실패: 유효하지 않은 JSON 형식", e);
            return Collections.emptyList();
        } catch (Exception e) {
            USER_LOG.warn("알라딘 API 호출 중 알 수 없는 예외 발생.", e);
            return Collections.emptyList();
        }
    }

    // 외부 API를 호출하여 책 데이터 조회
    private AladinBookRequestDto getBookDetailsFromApi(String isbn13) {
        try {
            String response = aladinBookClient.getBookDetails(ttbKey, "ISBN13", isbn13, "js", "20131101");

            if (StringUtils.isBlank(response)) {
                USER_LOG.info("알라딘 API 응답이 비어 있습니다. ISBN: {}", isbn13);
                return null;
            }

            AladinApiResponse apiResponse = objectMapper.readValue(response, AladinApiResponse.class);
            if (CollectionUtils.isEmpty(apiResponse.getItems())) {
                USER_LOG.info("알라딘 API에서 유효한 책 정보를 찾지 못했습니다. ISBN: {}", isbn13);
                return null;
            }
            return apiResponse.getItems().getFirst();

        } catch (JsonProcessingException e) {
            USER_LOG.warn("알라딘 API 응답 파싱 실패: 유효하지 않은 JSON 형식. ISBN: {}", isbn13, e);
        } catch (Exception e) {
            USER_LOG.warn("알라딘 API 호출 중 알 수 없는 예외가 발생했습니다. ISBN: {}", isbn13, e);
        }
        return null;
    }
}
