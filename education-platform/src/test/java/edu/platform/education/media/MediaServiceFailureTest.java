package edu.platform.education.media;

import edu.platform.education.users.Role;
import edu.platform.education.users.User;
import edu.platform.education.users.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@Transactional
class MediaServiceFailureTest {

    @Autowired private MediaService mediaService;
    @Autowired private MediaProperties properties;
    @Autowired private UserService userService;

    @MockitoBean
    private MediaRepository mediaRepository;   // подменяем репозиторий

    private Long userId;

    @BeforeEach
    void setUp() {
        User u = userService.register(new edu.platform.education.users.UserRegistrationRequest(
                "media-" + UUID.randomUUID() + "@example.com",
                "password123", "Media Tester", Role.TEACHER));
        userId = u.getId();
    }

    @Test
    void whenDbSaveFails_thenFileIsDeleted() throws Exception {
        // Настоящий PNG — минимальный набор байтов
        byte[] png = new byte[]{
                (byte)0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A,
                0, 0, 0, 0, 0, 0, 0, 0
        };
        MockMultipartFile file = new MockMultipartFile("file", "x.png", "image/png", png);

        // Считаем, сколько файлов было ДО теста
        Path dir = Path.of(properties.storageDir()).toAbsolutePath().normalize();
        Files.createDirectories(dir);
        long filesBefore;
        try (var files = Files.list(dir)) {
            filesBefore = files.count();
        }

        // Репозиторий при save бросает исключение
        when(mediaRepository.save(any(MediaFile.class)))
                .thenThrow(new RuntimeException("boom"));

        // Сервис пробросил ошибку
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> mediaService.upload(file, userId));
        assertEquals("boom", ex.getMessage());

        // Файлов стало столько же, сколько было ДО (то есть новый удалён)
        long filesAfter;
        try (var files = Files.list(dir)) {
            filesAfter = files.count();
        }
        assertEquals(filesBefore, filesAfter,
                "Новый файл должен быть удалён после сбоя БД");
    }
}