package ar.edu.utn.frvm.typeit.boero_api.academic.entities;

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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "training_path_document_requirements")
@Getter
@NoArgsConstructor
public class TrainingPathDocumentRequirement {
  @Id @GeneratedUUIDv7 private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "training_path_id")
  private TrainingPath trainingPath;

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

  @Column(nullable = false)
  private boolean active;

  public static TrainingPathDocumentRequirement create(TrainingPath path) {
    var requirement = new TrainingPathDocumentRequirement();
    requirement.trainingPath = path;
    return requirement;
  }

  public void update(
      String name,
      String instructions,
      DocumentRequirementLevel level,
      List<String> formats,
      int order,
      boolean active) {
    this.name = name.trim();
    this.instructions = instructions.trim();
    this.level = level;
    this.allowedFormats = List.copyOf(new LinkedHashSet<>(formats));
    this.displayOrder = order;
    this.active = active;
  }
}
