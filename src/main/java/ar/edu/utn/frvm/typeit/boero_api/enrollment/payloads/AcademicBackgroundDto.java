package ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads;

import ar.edu.utn.frvm.typeit.boero_api.enrollment.enums.EducationLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record AcademicBackgroundDto(
    @Schema(nullable = true) @Size(max = 255) String secondarySchool,
    @Schema(nullable = true) Boolean currentlyStudying,
    @Schema(nullable = true) EducationLevel educationLevel,
    @Schema(nullable = true) @Size(max = 150) String schoolOrigin,
    @Schema(nullable = true) @Size(max = 50) String currentGradeYear,
    @Schema(nullable = true) Boolean levelCompleted,
    @Schema(nullable = true) Boolean secondaryCompleted,
    @Schema(nullable = true) @Size(max = 150) String secondaryDegreeTitle) {
  public AcademicBackgroundDto() {
    this(null, null, null, null, null, null, null, null);
  }

  public String getSecondarySchool() {
    return secondarySchool;
  }

  public String getSchoolOrigin() {
    return schoolOrigin;
  }

  public Boolean getCurrentlyStudying() {
    return currentlyStudying;
  }

  public EducationLevel getEducationLevel() {
    return educationLevel;
  }

  public String getCurrentGradeYear() {
    return currentGradeYear;
  }

  public Boolean getSecondaryCompleted() {
    return secondaryCompleted;
  }

  public Boolean getLevelCompleted() {
    return levelCompleted;
  }

  public String getSecondaryDegreeTitle() {
    return secondaryDegreeTitle;
  }
}
