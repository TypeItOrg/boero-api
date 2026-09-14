package ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record AcademicBackgroundDto(
    @Schema(nullable = true) String secondarySchool,
    @Schema(nullable = true) String schoolOrigin,
    @Schema(nullable = true) String currentGradeYear,
    @Schema(nullable = true) Boolean secondaryCompleted,
    @Schema(nullable = true) String secondaryDegreeTitle) {
  public AcademicBackgroundDto() {
    this(null, null, null, null, null);
  }

  public String getSecondarySchool() {
    return secondarySchool;
  }

  public String getSchoolOrigin() {
    return schoolOrigin;
  }

  public String getCurrentGradeYear() {
    return currentGradeYear;
  }

  public Boolean getSecondaryCompleted() {
    return secondaryCompleted;
  }

  public String getSecondaryDegreeTitle() {
    return secondaryDegreeTitle;
  }
}
