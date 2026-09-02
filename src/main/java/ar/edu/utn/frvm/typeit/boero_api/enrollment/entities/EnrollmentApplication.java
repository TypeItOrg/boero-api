package ar.edu.utn.frvm.typeit.boero_api.enrollment.entities;

import ar.edu.utn.frvm.typeit.boero_api.academic.entities.AcademicYear;
import ar.edu.utn.frvm.typeit.boero_api.academic.entities.StudyPlan;
import ar.edu.utn.frvm.typeit.boero_api.common.persistence.GeneratedUUIDv7;
import ar.edu.utn.frvm.typeit.boero_api.common.persistence.SoftDeletable;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.EnrollmentApplicationStatus;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.exceptions.EnrollmentApplicationNotEditableException;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Institution;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Person;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
    name = "enrollment_applications",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "enrollment_applications_institution_id_id_unique",
          columnNames = {"institution_id", "enrollment_application_id"}),
      @UniqueConstraint(
          name = "enrollment_applications_person_id_id_unique",
          columnNames = {"person_id", "enrollment_application_id"})
    })
@Getter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PACKAGE)
public class EnrollmentApplication extends SoftDeletable {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @Id
  @GeneratedUUIDv7
  @Column(name = "enrollment_application_id")
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "person_id", nullable = false)
  private Person person;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "institution_id", nullable = false)
  private Institution institution;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "study_plan_id", nullable = false)
  private StudyPlan studyPlan;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "academic_year_id", nullable = false)
  private AcademicYear academicYear;

  @Column(name = "enrollment_period_id")
  private UUID enrollmentPeriodId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private EnrollmentApplicationStatus status;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(nullable = false)
  private JsonNode data;

  public static EnrollmentApplication create(
      final Person person,
      final Institution institution,
      final StudyPlan studyPlan,
      final AcademicYear academicYear,
      final UUID enrollmentPeriodId) {
    return EnrollmentApplication.builder()
        .person(person)
        .institution(institution)
        .studyPlan(studyPlan)
        .academicYear(academicYear)
        .enrollmentPeriodId(enrollmentPeriodId)
        .status(EnrollmentApplicationStatus.DRAFT)
        .data(OBJECT_MAPPER.createObjectNode())
        .build();
  }

  public boolean isEditable() {
    return status == EnrollmentApplicationStatus.DRAFT && getDeletedAt() == null;
  }

  public void replaceDraftData(final JsonNode data) {
    if (!isEditable()) {
      throw new EnrollmentApplicationNotEditableException();
    }
    this.data = data == null ? OBJECT_MAPPER.createObjectNode() : data.deepCopy();
  }

  public void cancel() {
    if (!isEditable()) {
      throw new EnrollmentApplicationNotEditableException();
    }
    status = EnrollmentApplicationStatus.CANCELLED;
  }

  public ObjectNode editableData() {
    if (data instanceof ObjectNode objectNode) {
      return objectNode;
    }
    return OBJECT_MAPPER.createObjectNode();
  }
}
