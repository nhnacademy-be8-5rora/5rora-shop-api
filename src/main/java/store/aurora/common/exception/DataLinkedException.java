package store.aurora.common.exception;

public class DataLinkedException extends DataConflictException {
    public DataLinkedException(String message) {
        super(message);
    }
}
