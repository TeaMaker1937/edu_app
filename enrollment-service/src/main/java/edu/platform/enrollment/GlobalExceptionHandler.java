package edu.platform.enrollment;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CourseNotFoundInCatalogException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(CourseNotFoundInCatalogException ex) {
        log.warn("Курс не найден в catalog: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("COURSE_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(CatalogUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleUnavailable(CatalogUnavailableException ex) {
        log.warn("Catalog недоступен: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ErrorResponse("CATALOG_UNAVAILABLE",
                        "Сервис catalog временно недоступен"));
    }
}