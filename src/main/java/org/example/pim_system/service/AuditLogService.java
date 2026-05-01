package org.example.pim_system.service;

import jakarta.servlet.http.HttpServletRequest;
import org.example.pim_system.model.AuditLog;
import org.example.pim_system.repository.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class AuditLogService {

    private static final Logger logger = LoggerFactory.getLogger(AuditLogService.class);

    @Autowired
    private AuditLogRepository auditLogRepository;

    public void log(String action, String details, HttpServletRequest request) {
        try {
            AuditLog log = new AuditLog();
            String safeAction = (action != null && !action.trim().isEmpty()) ? action.trim() : "UNKNOWN_ACTION";
            String safeDetails = details != null ? details.trim() : "";
            if (safeDetails.length() > 2000) {
                safeDetails = safeDetails.substring(0, 1997) + "...";
            }
            log.setAction(safeAction);
            log.setDetails(safeDetails);
            log.setUsername(resolveCurrentUsername());
            log.setIpAddress(resolveIpAddress(request));
            auditLogRepository.save(log);
        } catch (Exception ex) {
            // Audit logging should never block the main business flow.
            logger.warn("Failed to persist audit log action={}: {}", action, ex.getMessage());
        }
    }

    /**
     * Convenience overload when no HttpServletRequest is available.
     */
    public void log(String action, String details) {
        log(action, details, (HttpServletRequest) null);
    }

    private String resolveCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return "system";
        }
        return auth.getName() != null ? auth.getName() : "system";
    }

    private String resolveIpAddress(HttpServletRequest request) {
        if (request == null) return "unknown";

        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.trim().isEmpty()) {
            // Use the first IP if proxy chain is present.
            String first = forwardedFor.split(",")[0].trim();
            if (!first.isEmpty()) return first;
        }

        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.trim().isEmpty()) {
            return realIp.trim();
        }

        String remoteAddr = request.getRemoteAddr();
        return remoteAddr != null ? remoteAddr : "unknown";
    }
}
