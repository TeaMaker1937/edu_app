package edu.platform.catalog.domain;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;

@Table(schema = "catalog", name = "seat_operations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeatOperation {

    @Id
    private Long id;

    @Column("command_id")
    private String commandId;

    private String action;   // RESERVE | RELEASE

    @Column("enrollment_id")
    private Long enrollmentId;

    @Column("course_id")
    private Long courseId;

    private String result;   // RESERVED | RELEASED | NO_SEATS | NOT_FOUND

    private String reason;

    @Column("created_at")
    private OffsetDateTime createdAt;
}