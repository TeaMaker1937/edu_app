package edu.platform.enrollment.outbox;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table(schema = "enrollment", name = "outbox_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboxEvent {

    @Id
    private Long id;

    @Column("event_id")
    private String eventId;

    @Column("event_type")
    private String eventType;

    @Column("aggregate_id")
    private Long aggregateId;

    private String payload;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("published_at")
    private LocalDateTime publishedAt;

    @Column("locked_until")
    private LocalDateTime lockedUntil;

    private Integer attempts;

    @Column("last_error")
    private String lastError;
}