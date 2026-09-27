package ar.edu.utn.frvm.typeit.boero_api.enrollment.entities;

import ar.edu.utn.frvm.typeit.boero_api.academic.entities.TrainingPathDocumentRequirement;
import ar.edu.utn.frvm.typeit.boero_api.common.persistence.GeneratedUUIDv7;
import ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.DocumentRequirementLevel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "enrollment_document_requirements")
@Getter
@NoArgsConstructor
public class EnrollmentDocumentRequirement {
  @Id @GeneratedUUIDv7 private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "application_id")
  private EnrollmentApplication application;

  @Column(name = "source_requirement_id", nullable = false)
  private UUID sourceRequirementId;

  @Column(nullable = false, length = 150)
  private String name;

  @Column(nullable = false, length = 1000)
  private String instructions;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private DocumentRequirementLevel level;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "allowed_formats", nullable = false, columnDefinition = "jsonb")
  private List<String> allowedFormats;

  @Column(name = "display_order", nullable = false)
  private int displayOrder;

  public static EnrollmentDocumentRequirement snapshot(
      EnrollmentApplication application, TrainingPathDocumentRequirement source) {
    var result = new EnrollmentDocumentRequirement();
    result.application = application;
    result.sourceRequirementId = source.getId();
    result.name = source.getName();
    result.instructions = source.getInstructions();
    result.level = source.getLevel();
    result.allowedFormats = List.copyOf(source.getAllowedFormats());
    result.displayOrder = source.getDisplayOrder();
    return result;
  }
}
