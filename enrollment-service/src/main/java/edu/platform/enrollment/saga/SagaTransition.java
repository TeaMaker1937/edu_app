package edu.platform.enrollment.saga;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table(schema = "enrollment", name = "saga_transitions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SagaTransition {

    @Id private Long id;

    @Column("saga_id")    private Long sagaId;
    @Column("from_state") private String fromState;
    @Column("to_state")   private String toState;
    private String reason;
    @Column("command_id") private String commandId;
    @Column("created_at") private LocalDateTime createdAt;
}