package ar.edu.utn.frvm.typeit.boero_api.enrollment.payloads;

import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttachmentDto {
  private UUID id;
  private String attachmentType;
  private String originalFileName;
  private String storagePath;
  private String contentType;
  private Long fileSize;
  private Instant createdAt;
}
