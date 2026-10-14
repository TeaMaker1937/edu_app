package edu.platform.enrollment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Настройки подключения к catalog-service.
 * Валидируются при старте: если URL пуст или не начинается с http(s) —
 * приложение не запустится с понятной ошибкой.
 */
@Validated
@ConfigurationProperties(prefix = "catalog")
public record CatalogProperties(
        @NotBlank(message = "catalog.base-url обязателен")
        @Pattern(regexp = "^https?://.+", message = "catalog.base-url должен начинаться с http:// или https://")
        String baseUrl,

        Duration timeout
) {
    public CatalogProperties {
        if (timeout == null) {
            timeout = Duration.ofSeconds(3);
        }
    }
}