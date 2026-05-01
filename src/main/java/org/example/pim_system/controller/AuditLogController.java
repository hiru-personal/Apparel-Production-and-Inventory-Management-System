package org.example.pim_system.controller;

import org.example.pim_system.model.AuditLog;
import org.example.pim_system.repository.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/audit-logs")
public class AuditLogController {
//read audit log data from DB
    @Autowired
    private AuditLogRepository auditLogRepository;
//fetches all logs ordered by newest first
    @GetMapping
    public ResponseEntity<List<AuditLog>> getAuditLogs() {
        List<AuditLog> logs = auditLogRepository.findAllByOrderByTimestampDesc();
        return ResponseEntity.ok(logs);
    }
//method CSV download
    @GetMapping("/export")
    //load from DB(newest first)
    public ResponseEntity<byte[]> exportAuditLogs() {
        List<AuditLog> logs = auditLogRepository.findAllByOrderByTimestampDesc();

        StringBuilder csvBuilder = new StringBuilder();
        csvBuilder.append("Timestamp,User,Action,Details,IP Address\n");
//header file and time,date
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        for (AuditLog log : logs) {
            String timestamp = log.getTimestamp() != null ? log.getTimestamp().format(formatter) : "";
            String username = log.getUsername() != null ? log.getUsername() : "";
            String action = log.getAction() != null ? log.getAction() : "";
            String details = log.getDetails() != null ? log.getDetails() : "";
            String ipAddress = log.getIpAddress() != null ? log.getIpAddress() : "";

            // Escape any quotes in details
            details = details.replace("\"", "\"\"");

            csvBuilder
                    .append('"').append(timestamp).append('"').append(',')
                    .append('"').append(username).append('"').append(',')
                    .append('"').append(action).append('"').append(',')
                    .append('"').append(details).append('"').append(',')
                    .append('"').append(ipAddress).append('"')
                    .append('\n');
        }

        byte[] fileBytes = csvBuilder.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        headers.setContentDisposition(ContentDisposition.attachment().filename("audit-logs.csv").build());
//attach filename
        return new ResponseEntity<>(fileBytes, headers, HttpStatus.OK);
    }
}



