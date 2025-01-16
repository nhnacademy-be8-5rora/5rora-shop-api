package store.aurora.search.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import store.aurora.book.repository.BookRepository;

import store.aurora.book.service.LikeService;
import store.aurora.search.dto.BookSearchEntityDTO;
import store.aurora.search.dto.BookSearchResponseDTO;
import store.aurora.search.service.SearchService;

import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@Transactional(isolation = Isolation.REPEATABLE_READ)
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final BookRepository bookRepository;

    private final LikeService likeService;

    @Override
    public Page<BookSearchResponseDTO> findBooksByKeywordWithDetails(String userId, String type, String keyword, Pageable pageable) {
        if (Objects.isNull(keyword)) {
            return Page.empty(pageable);
        }

        Page<BookSearchEntityDTO> bookSearchEntityDTOPage;
        if ("category".equals(type)) {
            try {
                // keyword가 숫자가 아닌 경우 예외가 발생하도록 처리
                Long categoryId = Long.parseLong(keyword);
                bookSearchEntityDTOPage = bookRepository.findBooksByCategoryWithDetails(categoryId, pageable);
            } catch (NumberFormatException e) {
                // NumberFormatException 발생 시 빈 페이지 반환
                return Page.empty(pageable);
            }
        } else {
            bookSearchEntityDTOPage = switch (type) {
                case "author" -> bookRepository.findBooksByAuthorNameWithDetails(keyword, pageable);
                default -> bookRepository.findBooksByTitleWithDetails(keyword, pageable);
            };
        }

        if (bookSearchEntityDTOPage.isEmpty()) {
            return Page.empty(pageable); // 결과가 없으면 빈 페이지 반환
        }

        // BookSearchEntityDTO -> BookSearchResponseDTO로 변환
        Page<BookSearchResponseDTO> bookSearchResponseDTOPage = bookSearchEntityDTOPage.map(BookSearchResponseDTO::new);

        // userId가 null이 아니면 좋아요 상태를 확인
        if (userId != null) {
            // 모든 책들의 id를 모아서 한 번에 좋아요 여부를 조회
            List<Long> bookIds = bookSearchResponseDTOPage.getContent().stream()
                    .map(BookSearchResponseDTO::getId)
                    .toList();

            // 유저가 좋아요를 눌렀던 책 목록을 한 번에 조회
            Set<Long> likedBookIds = likeService.getLikedBookIds(userId, bookIds);

            // 각 책에 대해 좋아요 여부를 설정
            for (BookSearchResponseDTO book : bookSearchResponseDTOPage.getContent()) {
                boolean isLiked = likedBookIds.contains(book.getId());
                book.setLiked(isLiked); // 좋아요 상태를 DTO에 추가
            }
        }

        return bookSearchResponseDTOPage;
    }

}
