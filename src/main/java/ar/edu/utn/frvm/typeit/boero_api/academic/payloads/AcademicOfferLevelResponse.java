package ar.edu.utn.frvm.typeit.boero_api.academic.payloads;

import ar.edu.utn.frvm.typeit.boero_api.academic.entities.AcademicLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;

@Schema(requiredProperties = {"id", "name", "displayOrder", "description", "spaces"})
public record AcademicOfferLevelResponse(
    UUID id,
    String name,
    int displayOrder,
    @Schema(nullable = true) String description,
    List<AcademicOfferSpaceResponse> spaces) {

  public static AcademicOfferLevelResponse from(
      final AcademicLevel level, final List<AcademicOfferSpaceResponse> spaces) {
    return new AcademicOfferLevelResponse(
        level.getId(), level.getName(), level.getDisplayOrder(), level.getDescription(), spaces);
  }
}
