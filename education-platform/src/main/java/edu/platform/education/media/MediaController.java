package edu.platform.education.media;

import edu.platform.education.users.User;
import edu.platform.education.users.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
@Tag(name = "Файлы", description = "Загрузка PNG/JPEG/WEBP до 5 МиБ")
public class MediaController {

    private final MediaService mediaService;
    private final UserService userService;

    @Operation(summary = "Загрузить файл")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MediaResponse> upload(@RequestParam("file") MultipartFile file,
                                                @AuthenticationPrincipal UserDetails userDetails) throws IOException {
        User user = userService.findByEmail(userDetails.getUsername());
        return ResponseEntity.ok(mediaService.upload(file, user.getId()));
    }

    @Operation(summary = "Метаданные файла")
    @GetMapping("/{id}")
    public ResponseEntity<MediaResponse> metadata(@PathVariable Long id) {
        return ResponseEntity.ok(mediaService.findMetadata(id));
    }

    @Operation(summary = "Мои загрузки")
    @GetMapping
    public ResponseEntity<List<MediaResponse>> list(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByEmail(userDetails.getUsername());
        return ResponseEntity.ok(mediaService.listByUser(user.getId()));
    }

    @Operation(summary = "Скачать файл")
    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        MediaFile mf = mediaService.getOrThrow(id);
        Resource resource = mediaService.loadResource(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(mf.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + mf.getOriginalName() + "\"")
                .body(resource);
    }
}