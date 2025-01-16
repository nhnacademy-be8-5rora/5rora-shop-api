package store.aurora.book.exception.series;

import store.aurora.common.exception.DataNotFoundException;

public class SeriesNotFoundException extends DataNotFoundException {
    public SeriesNotFoundException(String message) {
        super(message);
    }
}
