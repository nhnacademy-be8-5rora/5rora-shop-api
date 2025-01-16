package store.aurora.book.exception.publisher;

import store.aurora.common.exception.DataNotFoundException;

public class PublisherNotFoundException extends DataNotFoundException {
    public PublisherNotFoundException(String message) {
        super(message);
    }
}
