package store.aurora.book.service;


import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import store.aurora.book.config.QuerydslConfiguration;
import store.aurora.book.dto.BookDetailsDto;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@Slf4j
@Transactional
@Import(QuerydslConfiguration.class)
public class BookServiceTest {

    @Autowired
    private BookService bookService;

    @Sql("test.sql")
    @Test
    public void testGetBookDetails() {
        // Given
        Long bookId = 1L;

        // When
        BookDetailsDto bookDetails = bookService.getBookDetails(bookId);

        // Then
        assertNotNull(bookDetails);
        assertEquals(bookId, bookDetails.getBookId());
        assertEquals("Test Book", bookDetails.getTitle());
        assertEquals("123456789", bookDetails.getIsbn());
        assertEquals(10000, bookDetails.getRegularPrice());
        assertEquals(8000, bookDetails.getSalePrice());
        assertEquals("Test explanation of the book", bookDetails.getExplanation());
        assertEquals("Test contents of the book", bookDetails.getContents());
        assertEquals("Test Publisher", bookDetails.getPublisher().getName());
    }
}
