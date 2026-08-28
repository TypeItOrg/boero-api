package ar.edu.utn.frvm.typeit.boero_api.academic.payloads;

import ar.edu.utn.frvm.typeit.boero_api.academic.entities.AcademicSpace;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(requiredProperties = {"id", "name", "type", "format"})
public record CourseSpaceOptionResponse(UUID id, String name, String type, String format) {

  public static CourseSpaceOptionResponse from(final AcademicSpace space) {
    return new CourseSpaceOptionResponse(
        space.getId(), space.getName(), space.getType().name(), space.getFormat().name());
  }
}
