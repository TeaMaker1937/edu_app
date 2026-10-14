package edu.platform.education.media;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

/**
 * Определяет MIME по магическим байтам.
 * Поддерживает PNG, JPEG, WEBP — этого достаточно для задания.
 */
final class ContentTypeDetector {

    private ContentTypeDetector() {}

    static Optional<String> detect(InputStream in) throws IOException {
        byte[] head = in.readNBytes(12);
        if (head.length >= 8
                && (head[0] & 0xFF) == 0x89
                && head[1] == 'P' && head[2] == 'N' && head[3] == 'G'
                && (head[4] & 0xFF) == 0x0D && (head[5] & 0xFF) == 0x0A
                && (head[6] & 0xFF) == 0x1A && (head[7] & 0xFF) == 0x0A) {
            return Optional.of("image/png");
        }
        if (head.length >= 3
                && (head[0] & 0xFF) == 0xFF
                && (head[1] & 0xFF) == 0xD8
                && (head[2] & 0xFF) == 0xFF) {
            return Optional.of("image/jpeg");
        }
        if (head.length >= 12
                && head[0] == 'R' && head[1] == 'I' && head[2] == 'F' && head[3] == 'F'
                && head[8] == 'W' && head[9] == 'E' && head[10] == 'B' && head[11] == 'P') {
            return Optional.of("image/webp");
        }
        return Optional.empty();
    }
}