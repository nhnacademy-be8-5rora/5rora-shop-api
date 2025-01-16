package store.aurora.book.exception.series;

import store.aurora.common.exception.DataAlreadyExistsException;

public class SeriesAlreadyExistsException extends DataAlreadyExistsException {
    public SeriesAlreadyExistsException(String message) {
        super(message);
    }
}
