package store.aurora.book.service.book.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import store.aurora.book.dto.aladin.AladinApiResponse;
import store.aurora.book.dto.aladin.AladinBookRequestDto;

import store.aurora.book.service.book.AladinBookRedisService;
import store.aurora.book.service.book.AladinBookService;

import store.aurora.book.util.AladinBookClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AladinBookServiceImpl implements AladinBookService {

    private final AladinBookClient aladinBookClient;
    private final ObjectMapper objectMapper;
    private final AladinBookRedisService aladinBookRedisService;
    private static final Logger USER_LOG = LoggerFactory.getLogger("user-logger");

    @Value("${aladin.api.ttb-key}")
    private String ttbKey;

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
    public AladinBookRequestDto getBookDetailsByIsbn(String isbn) {
        String bookCacheKey = "book:" + isbn;

        // Redis에서 데이터 조회
        AladinBookRequestDto book = aladinBookRedisService.getBook(bookCacheKey);
        if (book != null) {
            return book;
        }

        // Redis에 데이터가 없을 경우, 외부 API 호출
        book = getBookDetailsFromApi(isbn);

        // Redis에 저장
        if (book != null) {
            aladinBookRedisService.storeBooks(bookCacheKey, List.of(book));
        }

        return book;
    }

    private String createCacheKey(String query, String queryType, String searchTarget, int start) {
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8); //공백, 특수 문자, 한글이 안전하게 저장됨
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
    private AladinBookRequestDto getBookDetailsFromApi(String isbn) {
        try {
            String isbnType = (isbn.length() == 13) ? "ISBN13" : "ISBN";

            String response = aladinBookClient.getBookDetails(ttbKey, isbnType, isbn, "js", "20131101");

            if (StringUtils.isBlank(response)) {
                USER_LOG.info("알라딘 API 응답이 비어 있습니다. ISBN: {}", isbn);
                return null;
            }

            AladinApiResponse apiResponse = objectMapper.readValue(response, AladinApiResponse.class);
            if (CollectionUtils.isEmpty(apiResponse.getItems())) {
                USER_LOG.info("알라딘 API에서 유효한 책 정보를 찾지 못했습니다. ISBN: {}", isbn);
                return null;
            }
            return apiResponse.getItems().getFirst();

        } catch (JsonProcessingException e) {
            USER_LOG.warn("알라딘 API 응답 파싱 실패: 유효하지 않은 JSON 형식. ISBN: {}", isbn, e);
        } catch (Exception e) {
            USER_LOG.warn("알라딘 API 호출 중 알 수 없는 예외가 발생했습니다. ISBN: {}", isbn, e);
        }
        return null;
    }
}
