package store.aurora.review.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;
import store.aurora.book.entity.Book;
import store.aurora.book.repository.book.BookRepository;
import store.aurora.book.service.author.BookAuthorService;
import store.aurora.book.service.image.BookImageService;
import store.aurora.order.repository.OrderDetailRepository;
import store.aurora.review.dto.ReviewRequest;
import store.aurora.review.dto.ReviewResponse;
import store.aurora.review.entity.Review;
import store.aurora.review.entity.ReviewImage;
import store.aurora.review.exception.ReviewAlreadyExistsException;
import store.aurora.review.exception.ReviewNotFoundException;
import store.aurora.review.exception.UnauthorizedReviewException;
import store.aurora.review.repository.ReviewRepository;
import store.aurora.user.entity.User;
import store.aurora.user.exception.UserNotFoundException;
import store.aurora.user.repository.UserRepository;
import store.aurora.file.ObjectStorageService;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReviewServiceTest {

    @InjectMocks
    private ReviewService reviewService;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ObjectStorageService objectStorageService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrderDetailRepository orderDetailRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private BookAuthorService bookAuthorService;

    @Mock
    private BookImageService bookImageService;

    @Test
    @DisplayName("리뷰 저장 실패: UnauthorizedReviewException 발생")
    void saveReview_ShouldThrowUnauthorizedReviewException_WhenBookNotPurchased() {
        // Given
        Long bookId = 1L;
        String userId = "user1";
        ReviewRequest reviewRequest = new ReviewRequest(5, "Great book!");
        List<MultipartFile> files = List.of();

        Book book = new Book();
        User user = new User();

        when(bookRepository.findById(bookId)).thenReturn(java.util.Optional.of(book));
        when(userRepository.findById(userId)).thenReturn(java.util.Optional.of(user));
        when(orderDetailRepository.existsByOrderUserIdAndBookId(userId, bookId)).thenReturn(false);

        // When & Then
        UnauthorizedReviewException exception = assertThrows(UnauthorizedReviewException.class, () -> {
            reviewService.saveReview(reviewRequest, files, bookId, userId);
        });

        assertEquals("이 도서를 주문하지 않아 리뷰를 작성할 수 없습니다.", exception.getMessage());
    }

    @Test
    @DisplayName("리뷰 저장 실패: ReviewAlreadyExistsException 발생")
    void saveReview_ShouldThrowReviewAlreadyExistsException_WhenReviewAlreadyExists() {
        // Given
        Long bookId = 1L;
        String userId = "user1";
        ReviewRequest reviewRequest = new ReviewRequest(5, "Great book!");
        List<MultipartFile> files = List.of();

        Book book = new Book();
        User user = new User();

        when(bookRepository.findById(bookId)).thenReturn(java.util.Optional.of(book));
        when(userRepository.findById(userId)).thenReturn(java.util.Optional.of(user));
        when(orderDetailRepository.existsByOrderUserIdAndBookId(userId, bookId)).thenReturn(true);
        when(reviewRepository.existsByBookIdAndUserId(bookId, userId)).thenReturn(true);

        // When & Then
        ReviewAlreadyExistsException exception = assertThrows(ReviewAlreadyExistsException.class, () -> {
            reviewService.saveReview(reviewRequest, files, bookId, userId);
        });

        assertEquals("리뷰를 이미 작성하셨습니다.", exception.getMessage());
    }

    @Test
    @DisplayName("리뷰 저장 성공")
    void saveReview_ShouldSaveReview_WhenValidInput() throws IOException {
        // Given
        Long bookId = 1L;
        String userId = "user1";
        ReviewRequest reviewRequest = new ReviewRequest(5, "Great book!");
        List<MultipartFile> files = List.of();  // No files in this case

        Book book = new Book();
        book.setId(bookId);

        User user = new User();
        user.setId(userId);

        when(bookRepository.findById(bookId)).thenReturn(java.util.Optional.of(book));
        when(userRepository.findById(userId)).thenReturn(java.util.Optional.of(user));
        when(orderDetailRepository.existsByOrderUserIdAndBookId(userId, bookId)).thenReturn(true);
        when(reviewRepository.existsByBookIdAndUserId(bookId, userId)).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenReturn(new Review());

        // When
        Review savedReview = reviewService.saveReview(reviewRequest, files, bookId, userId);

        // Then
        assertNotNull(savedReview);
        verify(reviewRepository, times(1)).save(any(Review.class));
    }

    @Test
    @DisplayName("도서 ID로 리뷰 목록 조회 성공")
    void getReviewsByBookId_ShouldReturnReviews_WhenValidBookId() {
        // Given
        Long bookId = 1L;
        Book book = new Book();
        book.setId(bookId);

        User user = new User();
        user.setId("userId");

        Review review = new Review();
        review.setBook(book);
        review.setReviewRating(5);
        review.setReviewContent("Great book!");
        review.setReviewCreateAt(LocalDateTime.now());
        review.setUser(user);

        when(bookRepository.findById(bookId)).thenReturn(java.util.Optional.of(book));
        when(reviewRepository.findByBook(book)).thenReturn(List.of(review));

        // When
        List<ReviewResponse> reviews = reviewService.getReviewsByBookId(bookId);

        // Then
        assertEquals(1, reviews.size());
        assertEquals(5, reviews.get(0).getRating());
        assertEquals("Great book!", reviews.get(0).getContent());
    }

    @Test
    @DisplayName("유저 ID로 리뷰 목록 조회 성공")
    void getReviewsByUserId_ShouldReturnReviews_WhenValidUserId() {
        // Given
        String userId = "user1";
        Pageable pageable = PageRequest.of(0, 10);

        User user = new User();
        user.setId(userId);

        Book book = new Book();
        book.setId(1L);
        book.setTitle("Great Book");

        Review review = new Review();
        review.setId(1L);
        review.setReviewRating(5);
        review.setReviewContent("Amazing!");
        review.setReviewCreateAt(LocalDateTime.now());
        review.setUser(user);
        review.setBook(book);

        List<Review> reviews = List.of(review);
        Page<Review> reviewPage = new PageImpl<>(reviews, pageable, reviews.size());

        when(bookAuthorService.getFormattedAuthors(book)).thenReturn("Author 1, Author 2");
        when(bookImageService.getThumbnail(book)).thenReturn(null);
        when(userRepository.findById(userId)).thenReturn(java.util.Optional.of(user));
        when(reviewRepository.findByUserIdWithBook(user.getId(), pageable)).thenReturn(reviews);

        // When
        Page<ReviewResponse> reviewResponses = reviewService.getReviewsByUserId(userId, pageable);

        // Then
        assertNotNull(reviewResponses);
        assertEquals(1, reviewResponses.getContent().size());
        assertEquals(5, reviewResponses.getContent().get(0).getRating());
        assertEquals("Amazing!", reviewResponses.getContent().get(0).getContent());
        assertEquals("Great Book", reviewResponses.getContent().get(0).getTitle());
        verify(userRepository, times(1)).findById(userId);
        verify(reviewRepository, times(1)).findByUserIdWithBook(user.getId(), pageable);
    }

    @Test
    @DisplayName("유저 ID로 리뷰 목록 조회 실패: UserNotFoundException 발생")
    void getReviewsByUserId_ShouldThrowUserNotFoundException_WhenUserNotFound() {
        // Given
        String userId = "user1";
        Pageable pageable = PageRequest.of(0, 10);

        when(userRepository.findById(userId)).thenReturn(java.util.Optional.empty());

        // When & Then
        assertThrows(UserNotFoundException.class, () -> {
            reviewService.getReviewsByUserId(userId, pageable);
        });
    }

    @Test
    @DisplayName("리뷰 ID로 리뷰 조회 성공")
    void getReviewById_ShouldReturnReviewResponse_WhenReviewExists() {
        // Given
        Long reviewId = 1L;
        Review review = new Review();
        review.setId(reviewId);
        review.setReviewRating(5);
        review.setReviewContent("This is a review");
        review.setReviewCreateAt(java.time.LocalDateTime.now());

        User user = new User();
        user.setId("user1");
        review.setUser(user);

        Book book = new Book();
        book.setId(1L);
        review.setBook(book);

        ReviewImage reviewImage1 = new ReviewImage();
        reviewImage1.setImageFilePath("imagePath1");

        ReviewImage reviewImage2 = new ReviewImage();
        reviewImage2.setImageFilePath("imagePath2");

        review.setReviewImages(Arrays.asList(reviewImage1, reviewImage2));

        when(reviewRepository.findById(reviewId)).thenReturn(java.util.Optional.of(review));

        // When
        ReviewResponse response = reviewService.getReviewById(reviewId);

        // Then
        assertNotNull(response);
        assertEquals(reviewId, response.getId());
        assertEquals(5, response.getRating());
        assertEquals("This is a review", response.getContent());
        assertNotNull(response.getReviewCreateAt());
        assertEquals(1L, response.getBookId());
        assertEquals("user1", response.getUserId());
        assertEquals(2, response.getImageFilePath().size());
        assertTrue(response.getImageFilePath().contains("imagePath1"));
        assertTrue(response.getImageFilePath().contains("imagePath2"));

        verify(reviewRepository, times(1)).findById(reviewId);
    }

    @Test
    @DisplayName("리뷰 ID로 리뷰 조회 실패: ReviewNotFoundException 발생")
    void getReviewById_ShouldThrowReviewNotFoundException_WhenReviewNotFound() {
        // Given
        Long reviewId = 1L;

        when(reviewRepository.findById(reviewId)).thenReturn(java.util.Optional.empty());

        // When & Then
        ReviewNotFoundException exception = assertThrows(ReviewNotFoundException.class, () -> {
            reviewService.getReviewById(reviewId);
        });

        assertEquals("Review:1 not found", exception.getMessage());
    }

    @Test
    @DisplayName("리뷰 수정 성공")
    void updateReview_ShouldUpdateReview_WhenValidInput() throws IOException {
        // Given
        Long reviewId = 1L;
        String userId = "user1";
        ReviewRequest request = new ReviewRequest(4, "Updated review content");
        List<MultipartFile> files = new ArrayList<>();  // No files for this case

        Review existingReview = new Review();
        existingReview.setId(reviewId);
        existingReview.setReviewRating(5);
        existingReview.setReviewContent("Original review content");
        existingReview.setReviewCreateAt(LocalDateTime.now().minusDays(1));

        User user = new User();
        user.setId(userId);

        when(reviewRepository.findById(reviewId)).thenReturn(java.util.Optional.of(existingReview));
        when(userRepository.findById(userId)).thenReturn(java.util.Optional.of(user));
        when(reviewRepository.save(existingReview)).thenReturn(existingReview);

        // When
        reviewService.updateReview(reviewId, request, files, userId);

        // Then
        assertEquals(4, existingReview.getReviewRating());
        assertEquals("Updated review content", existingReview.getReviewContent());
        assertNotNull(existingReview.getReviewCreateAt()); // Creation time should be updated
        assertEquals(userId, existingReview.getUser().getId());
        assertEquals(0, existingReview.getReviewImages().size()); // No new images added

        verify(reviewRepository, times(1)).save(existingReview);
    }

    @Test
    @DisplayName("리뷰 수정 실패: ReviewNotFoundException 발생")
    void updateReview_ShouldThrowReviewNotFoundException_WhenReviewNotFound() {
        // Given
        Long reviewId = 1L;
        String userId = "user1";
        ReviewRequest request = new ReviewRequest(4, "Updated review content");
        List<MultipartFile> files = new ArrayList<>();  // No files for this case

        when(reviewRepository.findById(reviewId)).thenReturn(java.util.Optional.empty());

        // When & Then
        ReviewNotFoundException exception = assertThrows(ReviewNotFoundException.class, () -> {
            reviewService.updateReview(reviewId, request, files, userId);
        });

        assertEquals("Review:1 not found", exception.getMessage());
    }

    @Test
    @DisplayName("리뷰 수정 성공: 새로운 이미지 업로드")
    void updateReview_ShouldUploadNewImages_WhenFilesProvided() throws IOException {
        // Given
        Long reviewId = 1L;
        String userId = "user1";
        ReviewRequest request = new ReviewRequest(4, "Updated review content");
        MultipartFile mockFile = mock(MultipartFile.class);
        List<MultipartFile> files = List.of(mockFile);

        Review existingReview = new Review();
        existingReview.setId(reviewId);
        existingReview.setReviewRating(5);
        existingReview.setReviewContent("Original review content");
        existingReview.setReviewCreateAt(LocalDateTime.now().minusDays(1));

        User user = new User();
        user.setId(userId);

        when(reviewRepository.findById(reviewId)).thenReturn(java.util.Optional.of(existingReview));
        when(userRepository.findById(userId)).thenReturn(java.util.Optional.of(user));
        when(objectStorageService.uploadObject(mockFile)).thenReturn("imageUrl");

        // When
        reviewService.updateReview(reviewId, request, files, userId);

        // Then
        assertEquals(1, existingReview.getReviewImages().size());
        assertEquals("imageUrl", existingReview.getReviewImages().get(0).getImageFilePath());

        verify(objectStorageService, times(1)).uploadObject(mockFile);
        verify(reviewRepository, times(1)).save(existingReview);
    }
}
