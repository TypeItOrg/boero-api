package ar.edu.utn.frvm.typeit.boero_api.common.storage;

import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ApplicationException;
import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.ErrorCategory;

public class StorageFileNotFoundException extends ApplicationException {
  public StorageFileNotFoundException() {
    super(ErrorCategory.NOT_FOUND, StorageMessages.FILE_NOT_FOUND);
  }
}
