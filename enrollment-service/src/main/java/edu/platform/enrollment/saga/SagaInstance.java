package edu.platform.enrollment.saga;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table(schema = "enrollment", name = "saga_instances")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SagaInstance {

    @Id private Long id;

    @Column("enrollment_id")      private Long enrollmentId;
    @Column("course_id")          private Long courseId;
    @Column("user_id")            private Long userId;
    @Column("state")              private String state;
    @Column("current_command_id") private String currentCommandId;
    @Column("deadline_at")        private LocalDateTime deadlineAt;
    @Column("retry_count")        private Integer retryCount;
    @Column("created_at")         private LocalDateTime createdAt;
    @Column("updated_at")         private LocalDateTime updatedAt;
    @Column("release_done") private Boolean releaseDone;
    @Column("refund_done")  private Boolean refundDone;
}