package ar.edu.utn.frvm.typeit.boero_api.institutional.services;

import ar.edu.utn.frvm.typeit.boero_api.institutional.exceptions.InvalidInstitutionLogoException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageInputStream;
import org.springframework.web.multipart.MultipartFile;

public final class InstitutionLogoPolicy {
  public static final long MAX_BYTES = 2 * 1024 * 1024L;
  private static final long MAX_PIXELS = 4_000_000L;

  public record Image(byte[] bytes, String contentType) {}

  private InstitutionLogoPolicy() {}

  public static Image prepare(final MultipartFile file) {
    if (file.isEmpty() || file.getSize() > MAX_BYTES) {
      throw new InvalidInstitutionLogoException();
    }
    try (final var stream = file.getInputStream()) {
      final byte[] bytes = stream.readNBytes((int) MAX_BYTES + 1);
      if (bytes.length == 0 || bytes.length > MAX_BYTES || bytes.length != file.getSize()) {
        throw new InvalidInstitutionLogoException();
      }
      final String detected = decode(bytes);
      if (!detected.equals(file.getContentType())) {
        throw new InvalidInstitutionLogoException();
      }
      return new Image(bytes, detected);
    } catch (IOException | IllegalArgumentException exception) {
      throw new InvalidInstitutionLogoException();
    }
  }

  private static String decode(final byte[] bytes) throws IOException {
    try (final var input = new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))) {
      final var readers = ImageIO.getImageReaders(input);
      if (!readers.hasNext()) {
        throw new InvalidInstitutionLogoException();
      }
      final var reader = readers.next();
      try {
        final String format = reader.getFormatName().toLowerCase(Locale.ROOT);
        if (!format.equals("png") && !format.equals("jpeg")) {
          throw new InvalidInstitutionLogoException();
        }
        final boolean complete =
            format.equals("jpeg")
                ? bytes.length >= 2
                    && bytes[bytes.length - 2] == (byte) 0xff
                    && bytes[bytes.length - 1] == (byte) 0xd9
                : bytes.length >= 12
                    && "IEND"
                        .equals(new String(bytes, bytes.length - 8, 4, StandardCharsets.US_ASCII));
        if (!complete) {
          throw new InvalidInstitutionLogoException();
        }
        reader.setInput(input, true, true);
        final int width = reader.getWidth(0);
        final int height = reader.getHeight(0);
        if (width <= 0 || height <= 0 || (long) width * height > MAX_PIXELS) {
          throw new InvalidInstitutionLogoException();
        }
        final var warning = new AtomicBoolean();
        reader.addIIOReadWarningListener((source, message) -> warning.set(true));
        final var image = reader.read(0);
        if (image == null || warning.get()) {
          throw new InvalidInstitutionLogoException();
        }
        image.flush();
        return format.equals("png") ? "image/png" : "image/jpeg";
      } finally {
        reader.dispose();
      }
    }
  }
}
