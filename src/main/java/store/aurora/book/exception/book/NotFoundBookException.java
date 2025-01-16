package store.aurora.book.exception.book;

import store.aurora.common.exception.DataNotFoundException;

public class NotFoundBookException extends DataNotFoundException {
    public NotFoundBookException(Long bookId) {
        super(String.format("%d is not found book", bookId));
    }
}
