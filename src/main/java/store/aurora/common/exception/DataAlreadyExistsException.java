package store.aurora.common.exception;

public abstract class DataAlreadyExistsException extends DataConflictException {
    protected DataAlreadyExistsException(String message) {
        super(message);
    }
}
