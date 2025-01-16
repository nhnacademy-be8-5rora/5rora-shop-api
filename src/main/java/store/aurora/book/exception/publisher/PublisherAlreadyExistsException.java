package store.aurora.book.exception.publisher;

import store.aurora.common.exception.DataAlreadyExistsException;

public class PublisherAlreadyExistsException extends DataAlreadyExistsException {
    public PublisherAlreadyExistsException(String message) {
        super(message);
    }
}
