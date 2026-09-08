package ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateEnrollmentDraftRequest {

  private EnrollmentDraftData data;

  @SuppressWarnings("unchecked")
  public UpdateEnrollmentDraftRequest(Map<String, Object> map) {
    if (map == null) {
      this.data = null;
      return;
    }
    Map<String, Object> source = map;
    if (map.containsKey("data") && map.get("data") instanceof Map) {
      source = (Map<String, Object>) map.get("data");
    }
    try {
      ObjectMapper mapper = new ObjectMapper();
      mapper.registerModule(new JavaTimeModule());
      this.data = mapper.convertValue(source, EnrollmentDraftData.class);
    } catch (Exception e) {
      this.data = null;
    }
  }
}
