package store.aurora.book.exception.publisher;

import store.aurora.common.exception.DataLinkedException;

public class PublisherLinkedToBooksException extends DataLinkedException {
    public PublisherLinkedToBooksException(String message) {
        super(message);
    }
}
