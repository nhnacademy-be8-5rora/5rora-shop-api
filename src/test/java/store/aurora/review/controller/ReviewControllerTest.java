package store.aurora.review.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import store.aurora.point.service.PointHistoryService;
import store.aurora.review.dto.ReviewRequest;
import store.aurora.review.dto.ReviewResponse;
import store.aurora.review.entity.Review;
import store.aurora.review.exception.ReviewAlreadyExistsException;
import store.aurora.review.exception.ReviewNotFoundException;
import store.aurora.review.service.ReviewService;
import store.aurora.user.entity.User;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReviewController.class)
public class ReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReviewService reviewService;

    @MockBean
    private PointHistoryService pointHistoryService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createReview_ShouldReturn201_WhenReviewIsCreatedSuccessfully() throws Exception {
        // Given
        ReviewRequest request = new ReviewRequest();
        request.setRating(5);
        request.setContent("Great book!");

        User user = new User();
        user.setId("user1");

        Review review = new Review();
        review.setId(1L);
        review.setUser(user);

        MockMultipartFile file = new MockMultipartFile(
                "files", "image.jpg", MediaType.IMAGE_JPEG_VALUE, "Dummy Image Content".getBytes()
        );

        when(reviewService.saveReview(any(ReviewRequest.class), anyList(), eq(1L), eq("user1"))).thenReturn(review);

        // When & Then
        mockMvc.perform(multipart("/api/reviews")
                        .file(file)
                        .param("bookId", "1")
                        .param("userId", "user1")
                        .param("rating", "5")
                        .param("content", "Great book!")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isCreated())
                .andExpect(content().string("Upload OK"));

        verify(reviewService, times(1)).saveReview(any(ReviewRequest.class), anyList(), eq(1L), eq("user1"));
        verify(pointHistoryService, times(1)).earnReviewPoint(any(User.class), anyBoolean());
    }

    @Test
    void createReview_ShouldReturn500_WhenIOExceptionOccurs() throws Exception {
        // Given
        ReviewRequest request = new ReviewRequest();
        request.setRating(5);
        request.setContent("Great book!");

        MockMultipartFile file = new MockMultipartFile(
                "files", "image.jpg", MediaType.IMAGE_JPEG_VALUE, "Dummy Image Content".getBytes()
        );

        when(reviewService.saveReview(any(ReviewRequest.class), anyList(), eq(1L), eq("user1")))
                .thenThrow(new IOException("Upload failed"));

        // When & Then
        mockMvc.perform(multipart("/api/reviews")
                        .file(file)
                        .param("bookId", "1")
                        .param("userId", "user1")
                        .param("rating", "5")
                        .param("content", "Great book!")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Upload failed: Upload failed"));

        verify(reviewService, times(1)).saveReview(any(ReviewRequest.class), anyList(), eq(1L), eq("user1"));
    }

    @Test
    void createReview_ShouldReturn409_WhenReviewAlreadyExists() throws Exception {
        // Given
        ReviewRequest request = new ReviewRequest();
        request.setRating(5);
        request.setContent("Great book!");

        MockMultipartFile file = new MockMultipartFile(
                "files", "image.jpg", MediaType.IMAGE_JPEG_VALUE, "Dummy Image Content".getBytes()
        );

        when(reviewService.saveReview(any(ReviewRequest.class), anyList(), eq(1L), eq("user1")))
                .thenThrow(new ReviewAlreadyExistsException("리뷰를 이미 작성하셨습니다."));

        // When & Then
        mockMvc.perform(multipart("/api/reviews")
                        .file(file)
                        .param("bookId", "1")
                        .param("userId", "user1")
                        .param("rating", "5")
                        .param("content", "Great book!")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorMsg").value("리뷰를 이미 작성하셨습니다."));
    }

    @Test
    void getReviewsByBookId_ShouldReturnReviewsList() throws Exception {
        // Given
        Long bookId = 1L;

        ReviewResponse review1 = new ReviewResponse(
                1L, 5, "Amazing book!", List.of("image1.jpg", "image2.jpg"),
                LocalDateTime.of(2025, 1, 1, 10, 0), bookId,
                "Book Title 1", "Author 1", "cover1.jpg", "user1"
        );

        ReviewResponse review2 = new ReviewResponse(
                2L, 4, "Good read.", List.of(),
                LocalDateTime.of(2025, 1, 2, 12, 30), bookId,
                "Book Title 1", "Author 1", "cover1.jpg", "user2"
        );

        List<ReviewResponse> reviews = List.of(review1, review2);

        when(reviewService.getReviewsByBookId(bookId)).thenReturn(reviews);

        // When & Then
        mockMvc.perform(get("/api/reviews/book/{bookId}", bookId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                // 검증 - 첫 번째 리뷰
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].rating").value(5))
                .andExpect(jsonPath("$[0].content").value("Amazing book!"))
                .andExpect(jsonPath("$[0].imageFilePath[0]").value("image1.jpg"))
                .andExpect(jsonPath("$[0].imageFilePath[1]").value("image2.jpg"))
                .andExpect(jsonPath("$[0].reviewCreateAt").value("2025-01-01T10:00:00"))
                .andExpect(jsonPath("$[0].bookId").value(1))
                .andExpect(jsonPath("$[0].title").value("Book Title 1"))
                .andExpect(jsonPath("$[0].author").value("Author 1"))
                .andExpect(jsonPath("$[0].cover").value("cover1.jpg"))
                .andExpect(jsonPath("$[0].userId").value("user1"))
                // 검증 - 두 번째 리뷰
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].rating").value(4))
                .andExpect(jsonPath("$[1].content").value("Good read."))
                .andExpect(jsonPath("$[1].imageFilePath").isEmpty())
                .andExpect(jsonPath("$[1].reviewCreateAt").value("2025-01-02T12:30:00"))
                .andExpect(jsonPath("$[1].bookId").value(1))
                .andExpect(jsonPath("$[1].title").value("Book Title 1"))
                .andExpect(jsonPath("$[1].author").value("Author 1"))
                .andExpect(jsonPath("$[1].cover").value("cover1.jpg"))
                .andExpect(jsonPath("$[1].userId").value("user2"));

        verify(reviewService, times(1)).getReviewsByBookId(bookId);
    }

    @Test
    void getReviewsByUserId_ShouldReturnPagedReviews() throws Exception {
        // Given
        String userId = "user1";
        int page = 0;
        int size = 5;

        List<ReviewResponse> reviewList = List.of(
                new ReviewResponse(
                        1L, 5, "Amazing book!", List.of("image1.jpg"),
                        LocalDateTime.of(2025, 1, 1, 10, 0), 101L,
                        "Book Title 1", "Author 1", "cover1.jpg", userId
                ),
                new ReviewResponse(
                        2L, 4, "Good read.", List.of(),
                        LocalDateTime.of(2025, 1, 2, 12, 30), 102L,
                        "Book Title 2", "Author 2", "cover2.jpg", userId
                )
        );

        Page<ReviewResponse> pagedReviews = new PageImpl<>(reviewList, PageRequest.of(page, size), reviewList.size());

        when(reviewService.getReviewsByUserId(eq(userId), any(Pageable.class))).thenReturn(pagedReviews);

        // When & Then
        mockMvc.perform(get("/api/reviews/user/{userId}", userId)
                        .param("page", String.valueOf(page))
                        .param("size", String.valueOf(size))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                // 검증 - 첫 번째 리뷰
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].rating").value(5))
                .andExpect(jsonPath("$.content[0].content").value("Amazing book!"))
                .andExpect(jsonPath("$.content[0].imageFilePath[0]").value("image1.jpg"))
                .andExpect(jsonPath("$.content[0].reviewCreateAt").value("2025-01-01T10:00:00"))
                .andExpect(jsonPath("$.content[0].bookId").value(101))
                .andExpect(jsonPath("$.content[0].title").value("Book Title 1"))
                .andExpect(jsonPath("$.content[0].author").value("Author 1"))
                .andExpect(jsonPath("$.content[0].cover").value("cover1.jpg"))
                .andExpect(jsonPath("$.content[0].userId").value("user1"))
                // 검증 - 두 번째 리뷰
                .andExpect(jsonPath("$.content[1].id").value(2))
                .andExpect(jsonPath("$.content[1].rating").value(4))
                .andExpect(jsonPath("$.content[1].content").value("Good read."))
                .andExpect(jsonPath("$.content[1].imageFilePath").isEmpty())
                .andExpect(jsonPath("$.content[1].reviewCreateAt").value("2025-01-02T12:30:00"))
                .andExpect(jsonPath("$.content[1].bookId").value(102))
                .andExpect(jsonPath("$.content[1].title").value("Book Title 2"))
                .andExpect(jsonPath("$.content[1].author").value("Author 2"))
                .andExpect(jsonPath("$.content[1].cover").value("cover2.jpg"))
                .andExpect(jsonPath("$.content[1].userId").value("user1"))
                // 페이징 정보 검증
                .andExpect(jsonPath("$.pageable.pageNumber").value(0))
                .andExpect(jsonPath("$.pageable.pageSize").value(5))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1));

        verify(reviewService, times(1)).getReviewsByUserId(eq(userId), any(Pageable.class));
    }

    @Test
    void getReviewById_ShouldReturnReviewResponse_WhenReviewExists() throws Exception {
        // Given
        Long reviewId = 1L;

        ReviewResponse reviewResponse = new ReviewResponse(
                1L, 5, "Amazing book!", List.of("image1.jpg", "image2.jpg"),
                LocalDateTime.of(2025, 1, 1, 10, 0), 101L,
                "Book Title", "Author Name", "cover.jpg", "user1"
        );

        when(reviewService.getReviewById(reviewId)).thenReturn(reviewResponse);

        // When & Then
        mockMvc.perform(get("/api/reviews/{reviewId}", reviewId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.rating").value(5))
                .andExpect(jsonPath("$.content").value("Amazing book!"))
                .andExpect(jsonPath("$.imageFilePath[0]").value("image1.jpg"))
                .andExpect(jsonPath("$.imageFilePath[1]").value("image2.jpg"))
                .andExpect(jsonPath("$.reviewCreateAt").value("2025-01-01T10:00:00"))
                .andExpect(jsonPath("$.bookId").value(101))
                .andExpect(jsonPath("$.title").value("Book Title"))
                .andExpect(jsonPath("$.author").value("Author Name"))
                .andExpect(jsonPath("$.cover").value("cover.jpg"))
                .andExpect(jsonPath("$.userId").value("user1"));

        verify(reviewService, times(1)).getReviewById(reviewId);
    }

    @Test
    void getReviewById_ShouldReturn404_WhenReviewDoesNotExist() throws Exception {
        // Given
        Long reviewId = 999L;

        when(reviewService.getReviewById(reviewId)).thenThrow(new ReviewNotFoundException(reviewId));

        // When & Then
        mockMvc.perform(get("/api/reviews/{reviewId}", reviewId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorMsg").value("Review:999 not found"))
                .andExpect(jsonPath("$.httpStatus").value("404"));

        verify(reviewService, times(1)).getReviewById(reviewId);
    }



}
