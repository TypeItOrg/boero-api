package ar.edu.utn.frvm.typeit.boero_api.common.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.time.LocalDateTime;
import lombok.Getter;

@MappedSuperclass
@Getter
public abstract class SoftDeletable extends Auditable {

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  public boolean isDeleted() {
    return deletedAt != null;
  }

  protected boolean markDeleted(final LocalDateTime deletedAt) {
    if (isDeleted()) {
      return false;
    }
    this.deletedAt = deletedAt;
    return true;
  }

  public boolean restore() {
    if (!isDeleted()) {
      return false;
    }
    deletedAt = null;
    return true;
  }
}
