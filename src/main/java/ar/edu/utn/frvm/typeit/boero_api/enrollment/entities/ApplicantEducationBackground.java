package ar.edu.utn.frvm.typeit.boero_api.enrollment.entities;

import ar.edu.utn.frvm.typeit.boero_api.common.persistence.GeneratedUUIDv7;
import ar.edu.utn.frvm.typeit.boero_api.common.persistence.SoftDeletable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "applicant_education_backgrounds")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class ApplicantEducationBackground extends SoftDeletable {

  @Id
  @GeneratedUUIDv7
  @Column(name = "applicant_education_background_id")
  private UUID id;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "enrollment_application_id", nullable = false)
  private EnrollmentApplication enrollmentApplication;

  @Column(name = "secondary_school", length = 255)
  private String secondarySchool;
}
