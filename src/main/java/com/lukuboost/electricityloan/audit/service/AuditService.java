package com.lukuboost.electricityloan.audit.service;

import com.lukuboost.electricityloan.audit.entity.AuditLog;
import com.lukuboost.electricityloan.audit.repository.AuditLogRepository;
import com.lukuboost.electricityloan.common.security.AdminPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAction(String action, String entityType, String entityId,
                          String oldValue, String newValue, String description) {
        AuditLog.AuditLogBuilder builder = AuditLog.builder()
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .oldValue(oldValue)
                .newValue(newValue)
                .description(description)
                .correlationId(MDC.get("correlationId"))
                .createdAt(Instant.now());

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AdminPrincipal principal) {
            builder.userId(principal.userId())
                    .username(principal.username())
                    .userType("ADMIN");
        } else {
            builder.userType("SYSTEM");
        }

        auditLogRepository.save(builder.build());
        log.debug("Audit logged: action={}, entity={}/{}", action, entityType, entityId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAdminAction(String action, String entityType, String entityId,
                                String description, String ipAddress, String userAgent) {
        AuditLog.AuditLogBuilder builder = AuditLog.builder()
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .description(description)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .correlationId(MDC.get("correlationId"))
                .createdAt(Instant.now());

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AdminPrincipal principal) {
            builder.userId(principal.userId())
                    .username(principal.username())
                    .userType("ADMIN");
        }

        auditLogRepository.save(builder.build());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logSystemEvent(String action, String entityType, String entityId, String description) {
        auditLogRepository.save(AuditLog.builder()
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .description(description)
                .userType("SYSTEM")
                .correlationId(MDC.get("correlationId"))
                .createdAt(Instant.now())
                .build());
    }

    public Page<AuditLog> searchAuditLogs(Long userId, String action, String entityType,
                                           String entityId, Instant from, Instant to, Pageable pageable) {
        return auditLogRepository.searchAuditLogs(userId, action, entityType, entityId, from, to, pageable);
    }
}
