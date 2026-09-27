package ar.edu.utn.frvm.typeit.boero_api.common.storage;

import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ApplicationException;
import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ErrorCategory;

public class InvalidStorageKeyException extends ApplicationException {
  public InvalidStorageKeyException() {
    super(ErrorCategory.INVALID_INPUT, StorageMessages.KEY_INVALID);
  }
}
