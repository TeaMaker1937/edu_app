package edu.platform.education.catalog;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

final class CourseSpecifications {

    private CourseSpecifications() {}

    /**
     * Собирает спецификацию по необязательным параметрам.
     * Если параметр null — соответствующее условие не добавляется.
     */
    public static Specification<Course> byFilter(String title, BigDecimal minPrice, BigDecimal maxPrice) {
        return (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();

            if (title != null && !title.isBlank()) {
                String pattern = "%" + escapeLike(title.toLowerCase()) + "%";
                ps.add(cb.like(cb.lower(root.get("title")), pattern, '\\'));
            }
            if (minPrice != null) {
                ps.add(cb.ge(root.get("price"), minPrice));
            }
            if (maxPrice != null) {
                ps.add(cb.le(root.get("price"), maxPrice));
            }

            return cb.and(ps.toArray(new Predicate[0]));
        };
    }

    /**
     * Экранирует спецсимволы LIKE, чтобы % и _ считались обычными символами.
     * Параметр '\\' в like(...) — escape-символ.
     */
    public static String escapeLike(String s) {
        return s.replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}