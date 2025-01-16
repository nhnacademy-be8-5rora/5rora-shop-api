package store.aurora.book.repository.impl;


import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.*;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;
import store.aurora.book.dto.*;
import store.aurora.book.dto.category.BookCategoryDto;
import store.aurora.book.entity.Book;
import store.aurora.book.entity.category.Category;
import store.aurora.book.entity.QBook;
import store.aurora.book.repository.BookRepositoryCustom;

import store.aurora.order.entity.enums.OrderState;
import store.aurora.search.SortConstants;
import store.aurora.search.dto.BookSearchEntityDTO;
import store.aurora.search.service.StringConstants;

import static store.aurora.book.entity.QBook.book;
import static store.aurora.book.entity.QBookAuthor.bookAuthor;
import static store.aurora.book.entity.QBookView.bookView;
import static store.aurora.book.entity.category.QBookCategory.bookCategory;
import static store.aurora.book.entity.category.QCategory.category;
import static store.aurora.book.entity.QLike.like;
import static store.aurora.book.entity.QPublisher.publisher;
import static store.aurora.book.entity.QAuthor.author;
import static store.aurora.book.entity.QAuthorRole.authorRole;
import static store.aurora.book.entity.QBookImage.bookImage;
import static store.aurora.book.entity.tag.QBookTag.bookTag;
import static store.aurora.book.entity.tag.QTag.tag;
import static store.aurora.order.entity.QOrderDetail.orderDetail;
import static store.aurora.review.entity.QReview.review;
import static store.aurora.review.entity.QReviewImage.reviewImage;
import static store.aurora.user.entity.QUser.user;
import static store.aurora.order.entity.QOrder.order;




import java.time.LocalDate;
import java.util.*;
import java.util.Objects;


@Slf4j
public class BookRepositoryCustomImpl extends QuerydslRepositorySupport implements BookRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Autowired
    public BookRepositoryCustomImpl(JPAQueryFactory queryFactory) {
        super(Book.class);
        this.queryFactory = queryFactory;
    }

    //특정 String을 포함하는 제목을 가진 책들을 조인해서 값들을 반환.
    @Override
    public Page<BookSearchEntityDTO> findBooksByTitleWithDetails(String title, Pageable pageable) {
        // title이 null이거나 공백인 경우 빈 페이지 반환
        if (Objects.isNull(title)) {
            return Page.empty(pageable);  // Pageable 정보와 함께 빈 페이지 반환
        }

        // 필터링 조건 정의
        BooleanExpression titleCondition = title.isBlank()
                ? null // 조건 없음
                : book.title.like("%" + title + "%"); // 제목 필터링 조건


        // 정렬 기준 추출
        Sort.Order sortOrder = pageable.getSort().stream().findFirst().orElse(Sort.Order.asc("id")); // 기본 정렬 기준
        Expression<?> orderByExpression = switch (sortOrder.getProperty().toLowerCase()) {
            case SortConstants.SALE_PRICE -> book.salePrice;
            case SortConstants.PUBLISH_DATE -> book.publishDate;
            case SortConstants.TITLE -> book.title;
            case SortConstants.REVIEW_RATING -> getAverageReviewRatingSubquery(); // reviewRating에 대한 정렬 처리
            case SortConstants.LIKE -> getLikeCountSubquery();
            case SortConstants.VIEW -> getViewCountSubquery();
            default -> book.id; // 기본값
        };

        // `reviewRating` 기준으로 정렬 시, 리뷰가 100개 이상인 책만 필터링하는 조건 추가
        BooleanExpression reviewCountCondition = (sortOrder.getProperty().equalsIgnoreCase(SortConstants.REVIEW_RATING))
                ? getReviewCountSubquery().goe(100) // 리뷰 개수가 100개 이상인 경우에만
                : null;

        // 제네릭 타입 명시적 설정
        @SuppressWarnings("unchecked")
        OrderSpecifier<Long> orderSpecifier = sortOrder.getDirection().isDescending()
                ? new OrderSpecifier<>(Order.DESC, (Expression<Long>) orderByExpression)
                : new OrderSpecifier<>(Order.ASC, (Expression<Long>) orderByExpression);

        // 데이터 조회
        List<Tuple> results = from(book)
                .leftJoin(book.publisher, publisher)
                .where(titleCondition)
                .where(reviewCountCondition)
                .where(book.active.isTrue())
                .orderBy(orderSpecifier)
                .groupBy(book.id, book.title, book.regularPrice, book.salePrice, book.publishDate, publisher.name)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .select(
                        book.id,
                        book.title,
                        book.regularPrice,
                        book.salePrice,
                        book.isSale,
                        book.publishDate,
                        publisher.name,
                        JPAExpressions.select(Expressions.stringTemplate(
                                        StringConstants.GROUP_CONCAT_AUTHOR_ROLE,
                                        author.name, authorRole.role
                                ))
                                .from(bookAuthor)
                                .leftJoin(bookAuthor.author, author)
                                .leftJoin(bookAuthor.authorRole, authorRole)
                                .where(bookAuthor.book.id.eq(book.id)),
                        getBookImagePathSubquery(),
                        JPAExpressions.select(Expressions.stringTemplate(
                                        StringConstants.GROUP_CONCAT_CATEGORY_ID,
                                        category.id
                                ))
                                .from(bookCategory)
                                .leftJoin(bookCategory.category, category)
                                .where(bookCategory.book.id.eq(book.id)),
                        getViewCountSubquery(),
                        getReviewCountSubquery(),
                        getAverageReviewRatingSubquery()
                )
                .fetch();

        // 결과 변환
        List<BookSearchEntityDTO> content = (results == null || results.isEmpty())
                ? Collections.emptyList() // 결과가 없을 경우 빈 리스트 반환
                : results.stream()
                .map(this::convertToDTO) // 메서드 호출
                .toList();

        // Count query for pagination
        long total = from(book)
                .where(titleCondition) // 제목 필터링 조건 추가
                .fetchCount();

        return new PageImpl<>(content, pageable, total);
    }




    //특정 String을 포함하는 작가이름을 가진 책들을 조인해서 값들을 반환.
    @Override
    public Page<BookSearchEntityDTO> findBooksByAuthorNameWithDetails(String name, Pageable pageable) {
        if (Objects.isNull(name) || name.isBlank()) {
            return Page.empty(pageable);
        }

        // 리뷰 개수
        var reviewCountSubquery = JPAExpressions.select(review.count().intValue()) // int로 변환
                .from(review)
                .where(review.book.id.eq(book.id));

        // 정렬 조건 설정
        Sort.Order sortOrder = pageable.getSort().stream().findFirst().orElse(Sort.Order.asc("title")); // 기본 정렬 기준
        Expression<?> orderByExpression = switch (sortOrder.getProperty().toLowerCase()) {
            case SortConstants.SALE_PRICE -> book.salePrice;
            case SortConstants.PUBLISH_DATE -> book.publishDate;
            case SortConstants.TITLE -> book.title;
            case SortConstants.REVIEW_RATING -> getAverageReviewRatingSubquery(); // reviewRating에 대한 정렬 처리
            case SortConstants.LIKE -> getLikeCountSubquery();
            case SortConstants.VIEW -> getViewCountSubquery();
            default -> book.title; // 기본값
        };

        // 제네릭 타입 명시적 설정
        @SuppressWarnings("unchecked")
        OrderSpecifier<Long> orderSpecifier = sortOrder.getDirection().isDescending()
                ? new OrderSpecifier<>(Order.DESC, (Expression<Long>) orderByExpression)
                : new OrderSpecifier<>(Order.ASC, (Expression<Long>) orderByExpression);


        // 메인 쿼리: 저자 이름 검색 및 도서 정보 조회
        BooleanBuilder whereBuilder = new BooleanBuilder();
        whereBuilder.and(Expressions.stringTemplate("LOWER({0})", author.name).like("%" + name.toLowerCase() + "%")); // 대소문자 구분 없이 저자 이름 조건

        // reviewrating으로 정렬할 때, 리뷰 개수가 100개 이상인 책만 가져오기
        if (SortConstants.REVIEW_RATING.equalsIgnoreCase(sortOrder.getProperty())) {
            whereBuilder.and(reviewCountSubquery.goe(100)); // 리뷰 개수가 100개 이상인 책만 가져오기
        }

        List<Tuple> results = from(book)
                .leftJoin(book.publisher, publisher)
                .leftJoin(bookAuthor).on(bookAuthor.book.id.eq(book.id))
                .leftJoin(bookAuthor.author, author)
                .leftJoin(bookAuthor.authorRole, authorRole)
                .where(whereBuilder) // 동적으로 where 조건 추가
                .where(book.active.isTrue())
                .groupBy(book.id, book.title, book.regularPrice, book.salePrice, book.publishDate, publisher.name)
                .orderBy(orderSpecifier) // 동적 정렬 조건 추가
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .select(
                        book.id,
                        book.title,
                        book.regularPrice,
                        book.salePrice,
                        book.isSale,
                        book.publishDate,
                        publisher.name,
                        // 여러 저자와 역할을 쉼표로 묶어서 반환
                        JPAExpressions.select(Expressions.stringTemplate(
                                        StringConstants.GROUP_CONCAT_AUTHOR_ROLE,
                                        author.name, authorRole.role
                                ))
                                .from(bookAuthor)
                                .leftJoin(bookAuthor.author, author)
                                .leftJoin(bookAuthor.authorRole, authorRole)
                                .where(bookAuthor.book.id.eq(book.id)),
                        getBookImagePathSubquery(),
                        // 카테고리 아이디를 List로 변환
                        JPAExpressions.select(Expressions.stringTemplate(
                                        StringConstants.GROUP_CONCAT_CATEGORY_ID,
                                        category.id
                                ))
                                .from(bookCategory)
                                .leftJoin(bookCategory.category, category)
                                .where(bookCategory.book.id.eq(book.id)),
                        getViewCountSubquery(), // 조회수 추가
                        reviewCountSubquery, // 리뷰 갯수 추가
                        getAverageReviewRatingSubquery() // 평균 리뷰 점수 추가
                )
                .fetch();

        // 결과 변환
        List<BookSearchEntityDTO> content = (results == null || results.isEmpty())
                ? Collections.emptyList() // 결과가 없을 경우 빈 리스트 반환
                : results.stream()
                .map(this::convertToDTO) // 메서드 호출
                .toList();

        // 총 데이터 수 계산
        long total = from(book)
                .leftJoin(bookAuthor).on(bookAuthor.book.id.eq(book.id))
                .leftJoin(bookAuthor.author, author)
                .where(whereBuilder) // 동적으로 where 조건 추가
                .fetchCount();

        // 페이지 처리된 결과 반환
        return new PageImpl<>(content, pageable, total);
    }




    // 특정 카테고리Id를 가진 책들을 반환.
    @Override
    public Page<BookSearchEntityDTO> findBooksByCategoryWithDetails(Long categoryId, Pageable pageable) {
        if (categoryId == null) {
            return Page.empty(pageable);
        }

        // 최하위 카테고리일 경우 해당 카테고리만 가져오고, 그렇지 않으면 해당 카테고리와 하위 카테고리들을 모두 가져오기
        var categoryHierarchySubquery = JPAExpressions.select(category.id)
                .from(category)
                .where(
                        category.id.eq(categoryId) // 현재 카테고리
                                .or(category.parent.id.eq(categoryId)) // 첫 번째 단계 (상위 카테고리의 하위 카테고리)
                                .or(category.id.in(
                                        JPAExpressions.select(category.id)
                                                .from(category)
                                                .where(category.parent.id.in(
                                                        JPAExpressions.select(category.id)
                                                                .from(category)
                                                                .where(category.parent.id.eq(categoryId)) // 두 번째 단계
                                                ))
                                ))
                                .or(category.id.in(
                                        JPAExpressions.select(category.id)
                                                .from(category)
                                                .where(category.parent.id.in(
                                                        JPAExpressions.select(category.id)
                                                                .from(category)
                                                                .where(category.parent.id.in(
                                                                        JPAExpressions.select(category.id)
                                                                                .from(category)
                                                                                .where(category.parent.id.eq(categoryId)) // 세 번째 단계
                                                                ))
                                                ))
                                ))
                )
                .distinct();


        // 리뷰 개수
        var reviewCountSubquery = JPAExpressions.select(review.count().intValue())
                .from(review)
                .where(review.book.id.eq(book.id));

        // 정렬 조건 설정
        Sort.Order sortOrder = pageable.getSort().stream().findFirst().orElse(Sort.Order.asc(SortConstants.TITLE)); // 기본 정렬 기준
        Expression<?> orderByExpression = switch (sortOrder.getProperty().toLowerCase()) {
            case SortConstants.SALE_PRICE -> book.salePrice;
            case SortConstants.PUBLISH_DATE -> book.publishDate;
            case SortConstants.TITLE -> book.title;
            case SortConstants.REVIEW_RATING -> getAverageReviewRatingSubquery(); // reviewRating에 대한 정렬 처리
            case SortConstants.LIKE -> getLikeCountSubquery();
            case SortConstants.VIEW -> getViewCountSubquery();
            default -> book.title; // 기본값
        };

        // 제네릭 타입 명시적 설정
        @SuppressWarnings("unchecked")
        OrderSpecifier<Long> orderSpecifier = sortOrder.getDirection().isDescending()
                ? new OrderSpecifier<>(Order.DESC, (Expression<Long>) orderByExpression)
                : new OrderSpecifier<>(Order.ASC, (Expression<Long>) orderByExpression);


        // 메인 쿼리: 카테고리와 자식 카테고리들을 포함하여 책들을 검색
        BooleanBuilder whereBuilder = new BooleanBuilder();
        whereBuilder.and(bookCategory.category.id.in(categoryHierarchySubquery)); // 카테고리 조건

        // 리뷰가 100개 이상인 책만 가져오기
        if (SortConstants.REVIEW_RATING.equalsIgnoreCase(sortOrder.getProperty())) {
            whereBuilder.and(reviewCountSubquery.goe(100));
        }

        // 메인 쿼리: 책 검색
        List<Tuple> results = from(book)
                .leftJoin(book.publisher, publisher)
                .leftJoin(bookCategory).on(bookCategory.book.id.eq(book.id))
                .leftJoin(bookCategory.category, category)
                .where(whereBuilder) // 동적으로 where 조건 추가
                .where(book.active.isTrue())
                .groupBy(book.id, book.title, book.regularPrice, book.salePrice, book.publishDate, publisher.name)
                .orderBy(orderSpecifier) // 정렬 조건 추가
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .select(
                        book.id,
                        book.title,
                        book.regularPrice,
                        book.salePrice,
                        book.isSale,
                        book.publishDate,
                        publisher.name,
                        // 여러 저자와 역할을 쉼표로 묶어서 반환
                        JPAExpressions.select(Expressions.stringTemplate(
                                        StringConstants.GROUP_CONCAT_AUTHOR_ROLE,
                                        author.name, authorRole.role
                                ))
                                .from(bookAuthor)
                                .leftJoin(bookAuthor.author, author)
                                .leftJoin(bookAuthor.authorRole, authorRole)
                                .where(bookAuthor.book.id.eq(book.id)),
                        getBookImagePathSubquery(),
                        // 카테고리 아이디를 List로 변환
                        JPAExpressions.select(Expressions.stringTemplate(
                                        StringConstants.GROUP_CONCAT_CATEGORY_ID,
                                        category.id
                                ))
                                .from(bookCategory)
                                .leftJoin(bookCategory.category, category)
                                .where(bookCategory.book.id.eq(book.id)),
                        getViewCountSubquery(), // 조회수 추가
                        reviewCountSubquery, // 리뷰 갯수 추가
                        getAverageReviewRatingSubquery() // 평균 리뷰 점수 추가
                )
                .fetch();

        // 결과 변환
        List<BookSearchEntityDTO> content = (results == null || results.isEmpty())
                ? Collections.emptyList() // 결과가 없을 경우 빈 리스트 반환
                : results.stream()
                .map(this::convertToDTO) // 메서드 호출
                .toList();

        // 총 데이터 수 계산
        long total = from(book)
                .leftJoin(bookCategory).on(bookCategory.book.id.eq(book.id))
                .leftJoin(bookCategory.category, category)
                .where(whereBuilder) // 동적으로 where 조건 추가
                .fetchCount();

        // 페이지 처리된 결과 반환
        return new PageImpl<>(content, pageable, total);
    }


    @Override
    public BookDetailsDto findBookDetailsByBookId(Long bookId) {
        // 책 기본 정보 조회
        Book book = queryFactory
                .from(QBook.book)
                .leftJoin(QBook.book.publisher, publisher)
                .where(QBook.book.id.eq(bookId))
                .select(QBook.book)
                .fetchOne();


        // BookImage와 Review 데이터를 메서드를 통해 각각 가져오기
        List<BookImageDto> bookImages = findBookImagesByBookId(bookId);
        List<ReviewDto> reviews = findReviewsByBookId(bookId);

        List<String> tagNames = queryFactory
                .select(tag.name)
                .from(QBook.book)
                .join(QBook.book.bookTags, bookTag)
                .join(bookTag.tag, tag)
                .where(QBook.book.id.eq(bookId))
                .fetch();


        // 좋아요 개수 조회
        int likeCount = Optional.ofNullable(queryFactory
                        .select(like.count())
                        .from(like)
                        .where(like.book.id.eq(bookId))
                        .fetchOne())
                .map(Long::intValue)
                .orElse(0);


        List<BookCategoryDto> categoryPathByBookId = findCategoryPathByBookId(bookId);

        return new BookDetailsDto(
                book.getId(),
                book.getTitle(),
                book.getIsbn(),
                book.getRegularPrice(),
                book.getSalePrice(),
                book.getExplanation(),
                book.getContents(),
                book.getPublishDate(),
                new PublisherDto(book.getPublisher().getId(), book.getPublisher().getName()),
                bookImages,
                reviews,
                tagNames,  // 태그 이름들
                likeCount,  // 좋아요 개수
                categoryPathByBookId,
                0L
        );
    }


    @Override
    public List<BookImageDto> findBookImagesByBookId(Long bookId) {
        return queryFactory
                .select(bookImage.filePath)
                .from(bookImage)
                .where(bookImage.book.id.eq(bookId))
                .fetch()
                .stream()
                .map(BookImageDto::new)  // String을 BookImageDto로 변환
                .toList();
    }


    @Override
    public List<ReviewDto> findReviewsByBookId(Long bookId) {


        List<String> reviewImageFilePaths = queryFactory
                .select(reviewImage.imageFilePath)
                .from(reviewImage)
                .join(reviewImage.review, review)
                .where(review.book.id.eq(bookId))
                .fetch();

        return queryFactory
                .from(review)
                .leftJoin(review.user, user)
                .where(review.book.id.eq(bookId))
                .select(Projections.constructor(
                        ReviewDto.class,
                        review.id,
                        review.reviewContent,
                        review.reviewRating,
                        review.reviewCreateAt,
                        user.name,
                        Expressions.constant(reviewImageFilePaths)
                ))
                .fetch();
    }

    @Override
    public List<BookCategoryDto> findCategoryPathByBookId(Long bookId) {
        // 1. Book ID로 연관된 Category 리스트 조회
        List<Category> categoryList = queryFactory
                .select(bookCategory.category)
                .from(bookCategory)
                .join(bookCategory.category, category)
                .where(bookCategory.book.id.eq(bookId))
                .fetch();

        // 2. 각 카테고리와 상위 관계를 CategoryDto로 변환
        Map<Long, BookCategoryDto> categoryMap = new HashMap<>();

        for (Category category : categoryList) {
            Category current = category;

            while (current != null) {
                categoryMap.putIfAbsent(
                        current.getId(),
                        new BookCategoryDto(
                                current.getId(),
                                current.getName(),
                                current.getDepth(),
//                                current.getDisplayOrder(),
                                new ArrayList<>()
                        )
                );
                current = current.getParent();
            }
        }

        // 3. 부모-자식 관계 설정
        List<BookCategoryDto> roots = new ArrayList<>();
        Set<Long> processedIds = new HashSet<>();

        for (Category category : categoryList) {
            Category current = category;

            while (current != null) {
                BookCategoryDto currentDto = categoryMap.get(current.getId());

                if (current.getParent() != null) {
                    BookCategoryDto parentDto = categoryMap.get(current.getParent().getId());
                    if (!parentDto.getChildren().contains(currentDto)) {
                        parentDto.getChildren().add(currentDto);
                    }
                } else if (!processedIds.contains(current.getId())) {
                    roots.add(currentDto); // 최상위 카테고리 추가
                    processedIds.add(current.getId());
                }

                current = current.getParent();
            }
        }

        // 4. 최상위 카테고리 리스트 반환
//        roots.sort(Comparator.comparingInt(BookCategoryDto::getDisplayOrder)); // 정렬 기준 설정
        return roots;
    }

    @Override
    public Page<BookSearchEntityDTO> findBookByIdIn(List<Long> bookId, Pageable pageable) {

        // 정렬 기준 추출
        Sort.Order sortOrder = pageable.getSort().stream().findFirst().orElse(Sort.Order.asc("id")); // 기본 정렬 기준
        Expression<?> orderByExpression = switch (sortOrder.getProperty().toLowerCase()) {
            case SortConstants.SALE_PRICE -> book.salePrice;
            case SortConstants.PUBLISH_DATE -> book.publishDate;
            case SortConstants.TITLE -> book.title;
            case SortConstants.REVIEW_RATING -> getAverageReviewRatingSubquery(); // reviewRating에 대한 정렬 처리
            case SortConstants.LIKE -> getLikeCountSubquery();
            case SortConstants.VIEW -> getViewCountSubquery();
            default -> book.id; // 기본값
        };

        // `reviewRating` 기준으로 정렬 시, 리뷰가 100개 이상인 책만 필터링하는 조건 추가
        BooleanExpression reviewCountCondition = (sortOrder.getProperty().equalsIgnoreCase(SortConstants.REVIEW_RATING))
                ?  getReviewCountSubquery().goe(100) // 리뷰 개수가 100개 이상인 경우에만
                : null;

        // 제네릭 타입 명시적 설정
        @SuppressWarnings("unchecked")
        OrderSpecifier<Long> orderSpecifier = sortOrder.getDirection().isDescending()
                ? new OrderSpecifier<>(Order.DESC, (Expression<Long>) orderByExpression)
                : new OrderSpecifier<>(Order.ASC, (Expression<Long>) orderByExpression);


        // bookDetail 가져오기
        List<Tuple> results = from(book)
                .leftJoin(book.publisher, publisher)
                .where(book.id.in(bookId)) // bookId에 포함된 책만 조회
                .where(reviewCountCondition) // reviewCountCondition을 where 절에 추가
                .where(book.active.isTrue())
                .groupBy(book.id, book.title, book.regularPrice, book.salePrice, book.publishDate, publisher.name)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .select(
                        book.id,
                        book.title,
                        book.regularPrice,
                        book.salePrice,
                        book.isSale,
                        book.publishDate,
                        publisher.name,
                        // 여러 저자와 역할을 쉼표로 묶어서 반환
                        JPAExpressions.select(Expressions.stringTemplate(
                                        StringConstants.GROUP_CONCAT_AUTHOR_ROLE,
                                        author.name, authorRole.role
                                ))
                                .from(bookAuthor)
                                .leftJoin(bookAuthor.author, author)
                                .leftJoin(bookAuthor.authorRole, authorRole)
                                .where(bookAuthor.book.id.eq(book.id)),
                        getBookImagePathSubquery(),
                        // 카테고리 아이디를 List로 변환
                        JPAExpressions.select(Expressions.stringTemplate(
                                        StringConstants.GROUP_CONCAT_CATEGORY_ID,
                                        category.id
                                ))
                                .from(bookCategory)
                                .leftJoin(bookCategory.category, category)
                                .where(bookCategory.book.id.eq(book.id)),
                        getViewCountSubquery(),
                        getReviewCountSubquery(), // 리뷰 갯수 추가
                        getAverageReviewRatingSubquery() // 평균 리뷰 점수 추가
                )
                .orderBy(orderSpecifier) // 정렬 기준 추가
                .fetch();
        // 결과 변환
        List<BookSearchEntityDTO> content = (results == null || results.isEmpty())
                ? Collections.emptyList() // 결과가 없을 경우 빈 리스트 반환
                : results.stream()
                .map(this::convertToDTO) // 메서드 호출
                .toList();

        // Count query for pagination
        long total = from(book)
                .where(book.id.in(bookId)) // bookId에 포함된 책만 조회
                .fetchCount();

        return new PageImpl<>(content, pageable, total);
    }

    //리뷰 개수 가져오는 서브쿼리
    private JPQLQuery<Integer> getReviewCountSubquery() {
        return JPAExpressions.select(review.count().intValue())
                .from(review)
                .where(review.book.id.eq(book.id));
    }

    // 이미지를 하나만 가져오는 서브쿼리
    private Expression<String> getBookImagePathSubquery() {
        return JPAExpressions.select(bookImage.filePath)
                .from(bookImage)
                .where(bookImage.book.id.eq(book.id)
                        .and(bookImage.isThumbnail.isTrue()));
    }

    // 조회수를 가져오는 서브쿼리
    private Expression<Long> getViewCountSubquery() {
        return JPAExpressions.select(bookView.count())
                .from(bookView)
                .where(bookView.book.id.eq(book.id));
    }
    // 평균 리뷰 점수 가져오는 서브쿼리
    private Expression<Double> getAverageReviewRatingSubquery() {
        return JPAExpressions.select(review.reviewRating.avg())
                .from(review)
                .where(review.book.id.eq(book.id));
    }

    // 좋아요 수를 가져오는 서브쿼리
    public Expression<Long> getLikeCountSubquery() {
        return JPAExpressions.select(like.count())
                .from(like)
                .where(like.book.id.eq(book.id))
                .where(like.isLike.eq(Boolean.TRUE));  // 수정된 부분
    }

    @Override
// 저번 달 1일부터 마지막 날까지 가장 많이 팔린 책 id 뽑기
    public Tuple findMostSoldBook() {

        // 현재 날짜 기준으로 저번 달의 시작일과 종료일 계산
        LocalDate now = LocalDate.now();
        LocalDate lastMonthStart = now.minusMonths(1).withDayOfMonth(1);
        LocalDate lastMonthEnd = now.minusMonths(1).withDayOfMonth(now.minusMonths(1).lengthOfMonth());

        return queryFactory
                .select(orderDetail.book.id, orderDetail.quantity.sum())
                .from(orderDetail)
                .join(orderDetail.order, order) // Order와 조인
                .where(orderDetail.state.eq(OrderState.CONFIRMED)
                        .and(order.orderTime.between(lastMonthStart.atStartOfDay(), lastMonthEnd.atTime(23, 59, 59)))) // 날짜 조건 추가
                .groupBy(orderDetail.book.id)
                .orderBy(orderDetail.quantity.sum().desc())
                .fetchFirst(); // 첫 번째 결과만 가져옴
    }

    private BookSearchEntityDTO convertToDTO(Tuple tuple) {
        return new BookSearchEntityDTO.Builder()
                .id(tuple.get(book.id))
                .title(tuple.get(book.title))
                .regularPrice(Optional.ofNullable(tuple.get(book.regularPrice)).orElse(0)) // 기본값 0
                .salePrice(Optional.ofNullable(tuple.get(book.salePrice)).orElse(0)) // 기본값 0
                .isSale(Optional.ofNullable(tuple.get(book.isSale)).orElse(false)) // 기본값 false
                .publishDate(tuple.get(book.publishDate))
                .publisherName(Optional.ofNullable(tuple.get(publisher.name)).orElse("Unknown"))
                .authors(Optional.ofNullable(tuple.get(7, String.class)).orElse("")) // 저자 정보 기본값
                .bookImagePath(Optional.ofNullable(tuple.get(8, String.class)).orElse(""))
                .categories(Optional.ofNullable(tuple.get(9, String.class)).orElse(""))
                .viewCount(Optional.ofNullable(tuple.get(10, Long.class)).orElse(0L)) // Long으로 명시적으로 지정
                .reviewCount(Optional.ofNullable(tuple.get(11, Integer.class)).orElse(0)) // Integer로 명시적으로 지정
                .averageReviewRating(Optional.ofNullable(tuple.get(12, Double.class)).orElse(0.0)) // 기본값 0.0
                .build();
    }



}
