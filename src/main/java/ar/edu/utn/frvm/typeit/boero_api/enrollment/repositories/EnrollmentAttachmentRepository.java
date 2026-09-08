package ar.edu.utn.frvm.typeit.boero_api.enrollment.repositories;

import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.EnrollmentAttachment;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.EnrollmentAttachmentType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnrollmentAttachmentRepository extends JpaRepository<EnrollmentAttachment, UUID> {

  Optional<EnrollmentAttachment> findByIdAndEnrollmentApplicationIdAndDeletedAtIsNull(
      UUID id, UUID applicationId);

  List<EnrollmentAttachment> findByEnrollmentApplicationIdAndDeletedAtIsNull(UUID applicationId);

  Optional<EnrollmentAttachment> findByEnrollmentApplicationIdAndAttachmentTypeAndDeletedAtIsNull(
      UUID applicationId, EnrollmentAttachmentType attachmentType);
}
