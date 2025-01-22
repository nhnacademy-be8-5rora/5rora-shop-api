package store.aurora.user.exception;

import store.aurora.common.exception.DataNotFoundException;
import store.aurora.user.entity.Rank;

public class RankNotFoundException extends DataNotFoundException {
  public RankNotFoundException(String message) {
    super(message);
  }
}
