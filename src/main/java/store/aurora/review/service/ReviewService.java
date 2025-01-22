package store.aurora.review.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import store.aurora.book.entity.Book;
import store.aurora.book.exception.book.BookNotFoundException;
import store.aurora.book.repository.book.BookRepository;
import store.aurora.book.entity.BookImage;
import store.aurora.book.service.author.BookAuthorService;
import store.aurora.book.service.image.BookImageService;
import store.aurora.file.ObjectStorageService;
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

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewService {
    private final ReviewRepository reviewRepository;
    private final ObjectStorageService objectStorageService;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final OrderDetailRepository orderDetailRepository;

    private final BookAuthorService bookAuthorService;
    private final BookImageService bookImageService;

    @Transactional
    // 리뷰 등록
    public Review saveReview(ReviewRequest request, List<MultipartFile> files, Long bookId, String userId) throws IOException {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new BookNotFoundException(bookId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        // 해당 사용자가 이 책을 주문했는지 확인
        boolean hasPurchasedBook = orderDetailRepository.existsByOrderUserIdAndBookId(userId, bookId);
        if (!hasPurchasedBook) {
            throw new UnauthorizedReviewException("이 도서를 주문하지 않아 리뷰를 작성할 수 없습니다.");
        }

        // 이미 해당 책의 리뷰를 작성한 경우
        if (reviewRepository.existsByBookIdAndUserId(bookId, userId)) {
            throw new ReviewAlreadyExistsException("리뷰를 이미 작성하셨습니다.");
        }

        Review review = new Review();
        review.setReviewRating(request.getRating());
        review.setReviewContent(request.getContent());
        review.setReviewCreateAt(LocalDateTime.now());
        review.setBook(book);
        review.setUser(user);

        List<ReviewImage> images = new ArrayList<>();
        for (MultipartFile file : files) {
            String imageUrl = objectStorageService.uploadObject(file);
            ReviewImage image = new ReviewImage();
            image.setImageFilePath(imageUrl);
            image.setReview(review);
            images.add(image);
        }

        review.setReviewImages(images);
        return reviewRepository.save(review);
    }

    // 리뷰 목록 조회 (도서 ID로 조회)
    @Transactional(readOnly = true)
    public List<ReviewResponse> getReviewsByBookId(Long bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new BookNotFoundException(bookId));

        return reviewRepository.findByBook(book).stream()
                .map(review -> {
                    ReviewResponse response = new ReviewResponse();
                    response.setId(review.getId());
                    response.setRating(review.getReviewRating());
                    response.setContent(review.getReviewContent());
                    response.setReviewCreateAt(review.getReviewCreateAt());
                    response.setBookId(review.getBook().getId());
                    response.setUserId(review.getUser().getId());

                    List<ReviewImage> reviewImages = review.getReviewImages();
                    List<String> reviewImgPaths = new ArrayList<>();
                    for(ReviewImage reviewImage : reviewImages) {
                        reviewImgPaths.add(reviewImage.getImageFilePath());
                    }
                    response.setImageFilePath(reviewImgPaths);

                    return response;
                })
                .toList();
    }

    // 리뷰 목록 조회 (사용자 ID로 조회)
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getReviewsByUserId(String userId, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        List<Review> reviews = reviewRepository.findByUserIdWithBook(user.getId(), pageable);

        List<ReviewResponse> reviewResponses = reviews.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());

        return new PageImpl<>(reviewResponses, pageable, reviewResponses.size());
    }

    // 리뷰 조회
    @Transactional(readOnly = true)
    public ReviewResponse getReviewById(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException(reviewId));

        ReviewResponse response = new ReviewResponse();
        response.setId(review.getId());
        response.setRating(review.getReviewRating());
        response.setContent(review.getReviewContent());
        response.setReviewCreateAt(review.getReviewCreateAt());
        response.setBookId(review.getBook().getId());
        response.setUserId(review.getUser().getId());

        // 리뷰 이미지 처리
        List<ReviewImage> reviewImages = review.getReviewImages();
        List<String> reviewImgPaths = new ArrayList<>();
        for (ReviewImage reviewImage : reviewImages) {
            reviewImgPaths.add(reviewImage.getImageFilePath());
        }
        response.setImageFilePath(reviewImgPaths);

        return response;
    }


    // 리뷰 수정
    @Transactional
    public void updateReview(Long reviewId, ReviewRequest request, List<MultipartFile> files, String userId) throws IOException {
        Review existingReview = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException(reviewId));

        existingReview.setReviewRating(request.getRating());
        existingReview.setReviewContent(request.getContent());
        existingReview.setReviewCreateAt(LocalDateTime.now());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        existingReview.setUser(user);

        // 기존 이미지 삭제
        List<ReviewImage> existingImages = existingReview.getReviewImages();
//        existingImages.clear();

        if (files != null && !files.isEmpty()) {
            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    String imageUrl = objectStorageService.uploadObject(file);
                    ReviewImage image = new ReviewImage();
                    image.setImageFilePath(imageUrl);
                    image.setReview(existingReview);
                    existingImages.add(image);
                }
            }
        }

        existingReview.setReviewImages(existingImages);
        reviewRepository.save(existingReview);
    }


    public Double calculateAverageRating(Long bookId) {
        List<Review> reviews = reviewRepository.findByBookId(bookId);

        if (reviews.isEmpty()) {
            return 0.0;  // 리뷰가 없으면 평균 평점은 0.0
        }

        // 평점의 합을 구하고 리뷰의 개수로 나누어 평균을 계산
        double totalRating = reviews.stream()
                .mapToInt(Review::getReviewRating)
                .sum();

        return totalRating / reviews.size();  // 리뷰 개수로 나눠서 평균 계산
    }

    // ReviewResponseDto 로 변환
    public ReviewResponse convertToDto(Review review) {
        ReviewResponse response = new ReviewResponse();

        // 리뷰 기본 정보 설정
        response.setId(review.getId());
        response.setRating(review.getReviewRating());
        response.setContent(review.getReviewContent());
        response.setReviewCreateAt(review.getReviewCreateAt());

        // 책 정보 설정
        Book book = review.getBook();
        response.setBookId(book.getId());
        response.setTitle(book.getTitle());
        response.setAuthor(bookAuthorService.getFormattedAuthors(book));
        // 책 이미지 설정
        BookImage thumbnailImage = bookImageService.getThumbnail(book);
        response.setCover(thumbnailImage != null ? thumbnailImage.getFilePath() : null);

        // 사용자 정보 설정
        response.setUserId(review.getUser().getId());

        // 리뷰 이미지 처리
        List<String> reviewImgPaths = review.getReviewImages().stream()
                .map(ReviewImage::getImageFilePath)
                .collect(Collectors.toList());
        response.setImageFilePath(reviewImgPaths);

        return response;
    }

}
