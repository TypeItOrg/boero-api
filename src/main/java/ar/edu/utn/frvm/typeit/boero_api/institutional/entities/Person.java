package ar.edu.utn.frvm.typeit.boero_api.institutional.entities;

import static ar.edu.utn.frvm.typeit.boero_api.common.validation.PersonFieldConstraints.CHECK_SQL_PEOPLE_DOCUMENT_NUMBER;
import static ar.edu.utn.frvm.typeit.boero_api.common.validation.PersonFieldConstraints.CHECK_SQL_PEOPLE_FIRST_NAME_LENGTH;
import static ar.edu.utn.frvm.typeit.boero_api.common.validation.PersonFieldConstraints.CHECK_SQL_PEOPLE_LAST_NAME_LENGTH;
import static ar.edu.utn.frvm.typeit.boero_api.common.validation.PersonFieldConstraints.DOCUMENT_LENGTH;
import static ar.edu.utn.frvm.typeit.boero_api.common.validation.PersonFieldConstraints.DOCUMENT_PATTERN;
import static ar.edu.utn.frvm.typeit.boero_api.common.validation.PersonFieldConstraints.NAME_MAX;
import static ar.edu.utn.frvm.typeit.boero_api.common.validation.PersonFieldConstraints.NAME_MIN;
import static ar.edu.utn.frvm.typeit.boero_api.common.validation.PersonFieldConstraints.NAME_PATTERN;

import ar.edu.utn.frvm.typeit.boero_api.common.persistence.Auditable;
import ar.edu.utn.frvm.typeit.boero_api.common.persistence.GeneratedUUIDv7;
import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "people",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "people_institution_id_id_unique",
          columnNames = {"institution_id", "person_id"}),
      @UniqueConstraint(
          name = "people_document_number_unique",
          columnNames = {"institution_id", "document_number"})
    },
    check = {
      @CheckConstraint(
          name = "people_document_number_format",
          constraint = CHECK_SQL_PEOPLE_DOCUMENT_NUMBER),
      @CheckConstraint(
          name = "people_first_name_length",
          constraint = CHECK_SQL_PEOPLE_FIRST_NAME_LENGTH),
      @CheckConstraint(
          name = "people_last_name_length",
          constraint = CHECK_SQL_PEOPLE_LAST_NAME_LENGTH)
    },
    indexes =
        @Index(name = "people_institution_deleted_idx", columnList = "institution_id, deleted"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Person extends Auditable {

  @Id
  @GeneratedUUIDv7
  @Column(name = "person_id")
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "institution_id", nullable = false)
  private Institution institution;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "address_id")
  private Address address;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "birth_city_id")
  private City birthCity;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "nationality_country_id")
  private Country nationalityCountry;

  @NotBlank(message = "El número de documento es requerido.")
  @Pattern(
      regexp = DOCUMENT_PATTERN,
      message = "El número de documento debe tener exactamente 8 dígitos numéricos.")
  @Column(name = "document_number", nullable = false, length = DOCUMENT_LENGTH)
  private String documentNumber;

  @NotBlank(message = "El nombre es requerido.")
  @Size.List({
    @Size(min = NAME_MIN, message = "El nombre debe tener al menos 3 caracteres."),
    @Size(max = NAME_MAX, message = "El nombre debe tener menos de 255 caracteres.")
  })
  @Pattern(regexp = NAME_PATTERN, message = "El nombre solo puede contener letras y espacios.")
  @Column(name = "first_name", nullable = false, length = NAME_MAX)
  private String firstName;

  @NotBlank(message = "El apellido es requerido.")
  @Size.List({
    @Size(min = NAME_MIN, message = "El apellido debe tener al menos 3 caracteres."),
    @Size(max = NAME_MAX, message = "El apellido debe tener menos de 255 caracteres.")
  })
  @Pattern(regexp = NAME_PATTERN, message = "El apellido solo puede contener letras y espacios.")
  @Column(name = "last_name", nullable = false, length = NAME_MAX)
  private String lastName;

  @Column(name = "birth_date")
  private LocalDate birthDate;

  @Column(name = "phone_number", length = 30)
  private String phoneNumber;

  @Column(length = 150)
  private String email;

  @Column(name = "deleted", nullable = false)
  @Builder.Default
  private boolean deleted = false;
}
