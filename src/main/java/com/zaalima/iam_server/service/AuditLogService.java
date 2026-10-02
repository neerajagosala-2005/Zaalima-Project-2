package com.zaalima.iam_server.service;

import com.zaalima.iam_server.entity.AuditLog;
import com.zaalima.iam_server.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void log(
            String username,
            String action,
            String status,
            HttpServletRequest request) {

        String ipAddress = request.getRemoteAddr();

        AuditLog auditLog = new AuditLog(
                username,
                action,
                status,
                ipAddress
        );

        auditLogRepository.save(auditLog);
    }
}