package edu.platform.education.media;

import edu.platform.education.shared.exception.BadRequestException;
import edu.platform.education.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MediaService {

    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/png", ".png",
            "image/jpeg", ".jpg",
            "image/webp", ".webp"
    );

    private final MediaRepository mediaRepository;
    private final MediaProperties properties;

    // ---------- Загрузка ----------

    @Transactional
    public MediaResponse upload(MultipartFile file, Long userId) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Файл пуст");
        }
        if (file.getSize() > properties.maxSizeBytes()) {
            throw new BadRequestException("Файл больше " + properties.maxSizeBytes() + " байт");
        }

        // 1) Определяем тип по содержимому
        String contentType;
        try (InputStream in = file.getInputStream()) {
            contentType = ContentTypeDetector.detect(in)
                    .orElseThrow(() -> new BadRequestException(
                            "Разрешены только PNG, JPEG, WEBP"));
        }

        // 2) Имя на сервере — UUID, не зависит от клиента
        String storedName = UUID.randomUUID() + EXTENSIONS.get(contentType);

        Path storageDir = Path.of(properties.storageDir()).toAbsolutePath().normalize();
        Path target = storageDir.resolve(storedName);

        // 3) Пишем файл
        Files.createDirectories(storageDir);
        try {
            file.transferTo(target);
        } catch (IOException e) {
            safeDelete(target);
            throw e;
        }

        // 4) Пишем метаданные. Если БД падает — чистим файл.
        try {
            MediaFile saved = mediaRepository.save(MediaFile.builder()
                    .originalName(safeOriginalName(file.getOriginalFilename()))
                    .storedName(storedName)
                    .contentType(contentType)
                    .sizeBytes(file.getSize())
                    .uploadedBy(userId)
                    .createdAt(LocalDateTime.now())
                    .build());
            log.info("Загружен файл id={}, stored={}, type={}, size={}",
                    saved.getId(), storedName, contentType, file.getSize());
            return toResponse(saved);
        } catch (RuntimeException dbError) {
            safeDelete(target);
            throw dbError;
        }
    }

    // ---------- Чтение ----------

    @Transactional(readOnly = true)
    public MediaResponse findMetadata(Long id) {
        return toResponse(getOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<MediaResponse> listByUser(Long userId) {
        return mediaRepository.findAllByUploadedByOrderByIdDesc(userId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public Resource loadResource(Long id) {
        MediaFile mf = getOrThrow(id);
        Path path = Path.of(properties.storageDir()).toAbsolutePath().normalize()
                .resolve(mf.getStoredName());
        if (!Files.exists(path)) {
            throw new ResourceNotFoundException("Файл физически отсутствует: " + mf.getStoredName());
        }
        return new FileSystemResource(path);
    }

    @Transactional(readOnly = true)
    public MediaFile getOrThrow(Long id) {
        return mediaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Файл с id " + id + " не найден"));
    }

    // ---------- Вспомогательное ----------

    private MediaResponse toResponse(MediaFile mf) {
        return new MediaResponse(
                mf.getId(), mf.getOriginalName(), mf.getContentType(),
                mf.getSizeBytes(), mf.getUploadedBy(), mf.getCreatedAt()
        );
    }

    private String safeOriginalName(String name) {
        if (name == null || name.isBlank()) return "unknown";
        // Убираем разделители пути, перевод строки, обрезаем длину
        String cleaned = name.replaceAll("[\\r\\n\\\\/]", "_");
        return cleaned.length() > 255 ? cleaned.substring(0, 255) : cleaned;
    }

    private void safeDelete(Path p) {
        try {
            Files.deleteIfExists(p);
        } catch (IOException e) {
            log.warn("Не удалось удалить файл {}: {}", p, e.getMessage());
        }
    }
}