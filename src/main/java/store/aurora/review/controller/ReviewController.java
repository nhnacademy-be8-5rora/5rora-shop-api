package store.aurora.review.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import store.aurora.point.service.PointHistoryService;
import store.aurora.review.dto.ReviewRequest;
import store.aurora.review.dto.ReviewResponse;
import store.aurora.review.entity.Review;
import store.aurora.review.service.ReviewService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/reviews")
@Validated
@RequiredArgsConstructor
public class ReviewController {
    private final ReviewService reviewService;
    private final PointHistoryService pointHistoryService;

    private static final Logger LOG = LoggerFactory.getLogger("user-logger");

    // 리뷰 등록
    @PostMapping
    @Operation(summary = "리뷰 등록", description = "리뷰를 등록합니다.")
    @ApiResponse(responseCode = "201", description = "성공",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Review.class)))
    @ApiResponse(responseCode = "403", description = "도서를 주문하지 않음")
    @ApiResponse(responseCode = "409", description = "리뷰를 이미 작성함")
    @ApiResponse(responseCode = "500", description = "서버 오류")
    public ResponseEntity<String> createReview(@ModelAttribute @Valid ReviewRequest request,
                                               @RequestPart(value = "files", required = false) List<MultipartFile> files,
                                               @RequestParam Long bookId,
                                               @RequestParam String userId) {

        if (files == null) { files = new ArrayList<>(); }

        List<MultipartFile> validFiles = files.stream()
                .filter(file -> file != null && !file.isEmpty())
                .toList();

        try {
            Review savedReview = reviewService.saveReview(request, validFiles, bookId, userId);

            try{
                pointHistoryService.earnReviewPoint(savedReview.getUser(), !savedReview.getReviewImages().isEmpty());
            } catch (Exception e) { // case: review가 null, empty인데 getFirst,
                // todo: 예상 가능한 에러 별 브라우저 응답 다르게 (잠깐 db 에러는 적립 재시도)
                LOG.warn("Failed to earn points: category=review, userId={}", userId, e);
            }

            return ResponseEntity.status(201).body("Upload OK"); //body(review);
        } catch (IOException e) {
            LOG.error(e.getMessage());
            return ResponseEntity.status(500).body("Upload failed: " + e.getMessage()); //.body(null);
        }
    }

    // 도서 ID로 리뷰 조회
    @GetMapping("/book/{bookId}")
    @Operation(summary = "도서 ID로 리뷰 리스트 조회", description = "해당 bookId의 도서의 리뷰 리스트를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "성공",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Review.class)))
    @ApiResponse(responseCode = "404", description = "해당 도서를 찾을 수 없음")
    @ApiResponse(responseCode = "500", description = "서버 오류")
    public ResponseEntity<List<ReviewResponse>> getReviewsByBookId(@PathVariable Long bookId) {
        List<ReviewResponse> reviews = reviewService.getReviewsByBookId(bookId);
        return ResponseEntity.ok(reviews);
    }

    // 사용자 ID로 리뷰 조회
    @GetMapping("/user/{userId}")
    @Operation(summary = "사용자 ID로 리뷰 리스트 조회", description = "해당 userId의 사용자가 작성한 리뷰 리스트를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "성공",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Review.class)))
    @ApiResponse(responseCode = "404", description = "해당 유저를 찾을 수 없음")
    @ApiResponse(responseCode = "500", description = "서버 오류")
    public Page<ReviewResponse> getReviewsByUserId(@PathVariable String userId,
                                                   @RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "5") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return reviewService.getReviewsByUserId(userId, pageable);
//        return ResponseEntity.ok(reviewResponsePage);
    }

    // 리뷰 상세 조회
    @GetMapping("/{reviewId}")
    @Operation(summary = "리뷰 상세 조회", description = "해당 reviewId의 리뷰 상세내용을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "성공",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Review.class)))
    @ApiResponse(responseCode = "404", description = "해당 리뷰를 찾을 수 없음")
    @ApiResponse(responseCode = "500", description = "서버 오류")
    public ReviewResponse getReviewById(@PathVariable Long reviewId) {
        return reviewService.getReviewById(reviewId);
    }


    // 리뷰 수정
    @PutMapping("/{reviewId}/edit")
    @Operation(summary = "리뷰 수정", description = "리뷰를 수정합니다.")
    @ApiResponse(responseCode = "200", description = "성공",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Review.class)))
    @ApiResponse(responseCode = "404", description = "해당 (리뷰/도서/유저)가 존재하지 않음")
    @ApiResponse(responseCode = "500", description = "서버 오류")
    public ResponseEntity<String> updateReview(@PathVariable Long reviewId,
                                               @ModelAttribute @Valid ReviewRequest request,
                                               @RequestPart(required = false) List<MultipartFile> files,
//                                               @RequestParam Long bookId,
                                               @RequestParam String userId) {
        if (files == null) { files = new ArrayList<>(); }
        try {
            reviewService.updateReview(reviewId, request, files, userId);
            return ResponseEntity.status(200).body("Review updated successfully");
        } catch (IOException e) {
            LOG.error(e.getMessage());
            return ResponseEntity.status(500).body("Update failed: " + e.getMessage());
        }
    }
}
