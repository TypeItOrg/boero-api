package ar.edu.utn.frvm.typeit.boero_api.academic.entities;

import ar.edu.utn.frvm.typeit.boero_api.common.persistence.GeneratedUUIDv7;
import ar.edu.utn.frvm.typeit.boero_api.institutional.entities.Institution;
import jakarta.persistence.*;
import java.util.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "document_definitions")
@Getter
@NoArgsConstructor
public class DocumentDefinition {
  @Id @GeneratedUUIDv7 private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "institution_id")
  private Institution institution;

  @Column(nullable = false, length = 150)
  private String name;

  @Column(nullable = false, length = 1000)
  private String instructions;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "allowed_formats", nullable = false, columnDefinition = "jsonb")
  private List<String> allowedFormats;

  @Column(nullable = false)
  private boolean active;

  @Version private long revision;

  public static DocumentDefinition create(final Institution institution) {
    var value = new DocumentDefinition();
    value.institution = institution;
    return value;
  }

  public void update(
      final String name,
      final String instructions,
      final List<String> formats,
      final boolean active) {
    this.name = name.trim();
    this.instructions = instructions.trim();
    this.allowedFormats = formats.stream().distinct().sorted().toList();
    this.active = active;
  }
}
