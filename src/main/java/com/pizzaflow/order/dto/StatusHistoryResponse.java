package com.pizzaflow.order.dto;

import com.pizzaflow.order.entity.StatusHistory;
import java.time.Instant;

public class StatusHistoryResponse {
    private Long id;
    private String fromStatus;
    private String toStatus;
    private String changedByUsername;
    private String changedByRole;
    private String comment;
    private Instant changedAt;

    public StatusHistoryResponse() {
    }

    public static StatusHistoryResponse fromEntity(StatusHistory history) {
        StatusHistoryResponse res = new StatusHistoryResponse();
        res.setId(history.getId());
        res.setFromStatus(history.getFromStatus());
        res.setToStatus(history.getToStatus());
        res.setChangedByUsername(history.getChangedByUsername());
        res.setChangedByRole(history.getChangedByRole());
        res.setComment(history.getComment());
        res.setChangedAt(history.getChangedAt());
        return res;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFromStatus() {
        return fromStatus;
    }

    public void setFromStatus(String fromStatus) {
        this.fromStatus = fromStatus;
    }

    public String getToStatus() {
        return toStatus;
    }

    public void setToStatus(String toStatus) {
        this.toStatus = toStatus;
    }

    public String getChangedByUsername() {
        return changedByUsername;
    }

    public void setChangedByUsername(String changedByUsername) {
        this.changedByUsername = changedByUsername;
    }

    public String getChangedByRole() {
        return changedByRole;
    }

    public void setChangedByRole(String changedByRole) {
        this.changedByRole = changedByRole;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Instant getChangedAt() {
        return changedAt;
    }

    public void setChangedAt(Instant changedAt) {
        this.changedAt = changedAt;
    }
}
