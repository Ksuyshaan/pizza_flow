package com.pizzaflow.report.dto;

import java.time.Instant;

public class AuditLogResponse {
    private String id;
    private Instant timestamp;
    private String actor;
    private String role;
    private String action;
    private String details;

    public AuditLogResponse() {
        this.timestamp = Instant.now();
    }

    public AuditLogResponse(String id, String actor, String role, String action, String details) {
        this.id = id;
        this.timestamp = Instant.now();
        this.actor = actor;
        this.role = role;
        this.action = action;
        this.details = details;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getActor() {
        return actor;
    }

    public void setActor(String actor) {
        this.actor = actor;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }
}
