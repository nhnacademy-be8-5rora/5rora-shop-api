package store.aurora.book.exception.series;

import store.aurora.common.exception.DataLinkedException;

public class SeriesLinkedToBooksException extends DataLinkedException {
    public SeriesLinkedToBooksException(String message) {
        super(message);
    }
}
