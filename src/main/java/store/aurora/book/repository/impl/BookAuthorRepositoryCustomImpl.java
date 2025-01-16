package store.aurora.book.repository.impl;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;
import store.aurora.book.dto.AuthorDTO;
import store.aurora.book.repository.BookAuthorRepositoryCustom;
import store.aurora.document.AuthorDocument;

import java.util.List;
import static store.aurora.book.entity.QBookAuthor.bookAuthor;
import static store.aurora.book.entity.QAuthor.author;
import static store.aurora.book.entity.QAuthorRole.authorRole;

public class BookAuthorRepositoryCustomImpl extends QuerydslRepositorySupport implements BookAuthorRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Autowired
    public BookAuthorRepositoryCustomImpl(EntityManager entityManager) {
        super(AuthorDTO.class); // Entity 타입을 인자로 전달
        this.queryFactory = new JPAQueryFactory(entityManager); // JPAQueryFactory 초기화
    }

    //결과없으면 빈 리스트 보냄
    @Override
    public List<AuthorDocument> findAuthorsByBookId(Long bookId) {
        // QueryDSL 쿼리 작성
        return queryFactory
                .select(Projections.constructor(AuthorDocument.class, author.id,author.name, authorRole.role))
                .from(bookAuthor)
                .leftJoin(bookAuthor.author, author) // bookAuthor와 author 조인
                .leftJoin(bookAuthor.authorRole, authorRole) // bookAuthor와 authorRole 조인
                .where(bookAuthor.book.id.eq(bookId)) // bookId와 일치하는 데이터 필터링
                .fetch(); // 결과 리스트 반환
    }



}
