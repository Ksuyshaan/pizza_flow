package com.pizzaflow.report.service;

import com.pizzaflow.report.dto.AuditLogResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class AuditService {

    private static final Logger auditLogger = LoggerFactory.getLogger("com.pizzaflow.audit");
    private final List<AuditLogResponse> inMemoryLogs = new CopyOnWriteArrayList<>();

    public void log(String actor, String role, String action, String details) {
        String logMessage = String.format("[%s | %s] Action: %s | Details: %s", actor, role, action, details);
        auditLogger.info(logMessage);

        AuditLogResponse entry = new AuditLogResponse(
                UUID.randomUUID().toString(),
                actor,
                role,
                action,
                details
        );
        inMemoryLogs.add(0, entry); // prepend newest

        // Keep buffer size reasonable
        if (inMemoryLogs.size() > 200) {
            inMemoryLogs.remove(inMemoryLogs.size() - 1);
        }
    }

    public List<AuditLogResponse> getRecentLogs() {
        return Collections.unmodifiableList(new ArrayList<>(inMemoryLogs));
    }
}
