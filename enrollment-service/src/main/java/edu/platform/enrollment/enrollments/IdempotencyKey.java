package edu.platform.enrollment.enrollments;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table(schema = "enrollment", name = "idempotency_keys")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IdempotencyKey implements Persistable<String> {

    @Id
    @Column("idempotency_key")
    private String idempotencyKey;

    @Column("request_hash")
    private String requestHash;

    @Column("response_status")
    private Integer responseStatus;

    @Column("response_body")
    private String responseBody;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Override
    public String getId() {
        return idempotencyKey;
    }

    @Override
    public boolean isNew() {
        return true;
    }
}