package edu.platform.education.notifications.inbox;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class InboxRepository {

    private final JdbcTemplate jdbc;

    public int tryInsert(String eventId, String handlerId, String payload) {
        int rowcount = jdbc.update("""
                INSERT INTO inbox_events (event_id, handler_id, event_payload)
                VALUES (?, ?, ?)
                ON CONFLICT (event_id, handler_id) DO NOTHING
                """, eventId, handlerId, payload);
        System.out.println("[DEBUG tryInsert] eventId=" + eventId
                + ", handler=" + handlerId + ", rowcount=" + rowcount);
        return rowcount;
    }

    public long countByEventIdAndHandlerId(String eventId, String handlerId) {
        Long c = jdbc.queryForObject(
                "SELECT COUNT(*) FROM inbox_events WHERE event_id = ? AND handler_id = ?",
                Long.class, eventId, handlerId);
        return c != null ? c : 0L;
    }

    public long countAll() {
        Long c = jdbc.queryForObject("SELECT COUNT(*) FROM inbox_events", Long.class);
        return c != null ? c : 0L;
    }
}