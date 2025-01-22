package store.aurora.user.exception;

import store.aurora.common.exception.DataNotFoundException;

public class RoleNotFoundException extends DataNotFoundException {
    public RoleNotFoundException(String message) {
        super(message);
    }
}