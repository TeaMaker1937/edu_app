package edu.platform.enrollment.enrollments;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Table(schema = "enrollment", name = "enrollments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Enrollment {

    @Id
    private Long id;

    @Column("idempotency_key")
    private String idempotencyKey;

    @Column("user_id")
    private Long userId;

    @Column("course_id")
    private Long courseId;

    @Column("course_title")
    private String courseTitle;

    private BigDecimal price;

    private String status;

    @Column("created_at")
    private LocalDateTime createdAt;
}