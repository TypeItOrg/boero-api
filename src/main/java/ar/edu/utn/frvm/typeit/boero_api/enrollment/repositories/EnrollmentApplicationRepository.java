package ar.edu.utn.frvm.typeit.boero_api.enrollment.repositories;

import ar.edu.utn.frvm.typeit.boero_api.enrollment.entities.EnrollmentApplication;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.EnrollmentApplicationStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface EnrollmentApplicationRepository
    extends JpaRepository<EnrollmentApplication, UUID>,
        JpaSpecificationExecutor<EnrollmentApplication> {

  Optional<EnrollmentApplication>
      findByApplicantPersonIdAndStudyPlanIdAndAcademicYearIdAndStatusAndDeletedAtIsNull(
          UUID applicantPersonId,
          UUID studyPlanId,
          UUID academicYearId,
          EnrollmentApplicationStatus status);
}
