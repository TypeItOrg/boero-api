package ar.edu.utn.frvm.typeit.boero_api.common.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;
import lombok.Getter;

@MappedSuperclass
@Getter
public abstract class SoftDeletable extends Auditable {

  @Column(name = "deleted_at")
  private Instant deletedAt;

  public boolean isDeleted() {
    return deletedAt != null;
  }

  protected boolean markDeleted(final Instant deletedAt) {
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
