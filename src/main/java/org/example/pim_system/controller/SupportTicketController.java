package org.example.pim_system.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.example.pim_system.model.SupportTicket;
import org.example.pim_system.repository.SupportTicketRepository;
import org.example.pim_system.service.AuditLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/support")
public class SupportTicketController {

    @Autowired
    private SupportTicketRepository supportTicketRepository;

    @Autowired
    private AuditLogService auditLogService;

    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null ? authentication.getName() : "SYSTEM";
    }

    @PostMapping("/tickets")
    public ResponseEntity<Map<String, Object>> createTicket(@RequestBody Map<String, String> payload,
                                                              HttpServletRequest request) {
        Map<String, Object> resp = new HashMap<>();
        try {
            String title = payload.getOrDefault("title", "").trim();
            String department = payload.getOrDefault("department", "").trim();
            String details = payload.getOrDefault("details", "").trim();

            if (title.isEmpty() || department.isEmpty() || details.isEmpty()) {
                resp.put("success", false);
                resp.put("message", "Title, department, and details are required.");
                return ResponseEntity.badRequest().body(resp);
            }

            SupportTicket ticket = new SupportTicket();
            ticket.setTitle(title);
            ticket.setSubject(title);
            ticket.setDepartment(department);
            ticket.setDetails(details);
            ticket.setDescription(details);
            ticket.setCreatedBy(getCurrentUsername());

            SupportTicket saved = supportTicketRepository.save(ticket);

            auditLogService.log(
                    "SUPPORT_TICKET_CREATED",
                    String.format("Created support ticket #%d: Title='%s', Department=%s",
                            saved.getId(), title, department),
                    request
            );

            resp.put("success", true);
            resp.put("message", "Ticket created successfully.");
            resp.put("ticket", saved);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            resp.put("success", false);
            resp.put("message", "Ticket creation failed.");
            return ResponseEntity.badRequest().body(resp);
        }
    }

    @GetMapping("/my-tickets")
    public ResponseEntity<List<SupportTicket>> getMyTickets() {
        String username = getCurrentUsername();
        return ResponseEntity.ok(supportTicketRepository.findByCreatedByOrderByCreatedAtDesc(username));
    }

    @GetMapping("/all-tickets")
    public ResponseEntity<List<SupportTicket>> getAllTickets() {
        return ResponseEntity.ok(supportTicketRepository.findAllByOrderByCreatedAtDesc());
    }

    @PostMapping("/tickets/{id}/reply")
    public ResponseEntity<Map<String, Object>> replyToTicket(@PathVariable Long id,
                                                               @RequestBody Map<String, String> payload,
                                                               HttpServletRequest request) {
        Map<String, Object> resp = new HashMap<>();
        String replyText = payload.getOrDefault("reply", "").trim();

        if (replyText.isEmpty()) {
            resp.put("success", false);
            resp.put("message", "Reply text is required.");
            return ResponseEntity.badRequest().body(resp);
        }

        return supportTicketRepository.findById(id)
                .map(ticket -> {
                    ticket.setAdminReply(replyText);
                    ticket.setRepliedBy(getCurrentUsername());
                    ticket.setRepliedAt(java.time.LocalDateTime.now());
                    ticket.setStatus("RESOLVED");
                    supportTicketRepository.save(ticket);

                    auditLogService.log(
                            "SUPPORT_TICKET_REPLIED",
                            String.format("Replied to support ticket #%d ('%s'). Status changed to RESOLVED.",
                                    id, ticket.getTitle()),
                            request
                    );

                    resp.put("success", true);
                    resp.put("message", "Reply sent successfully.");
                    resp.put("ticket", ticket);
                    return ResponseEntity.ok(resp);
                })
                .orElseGet(() -> {
                    resp.put("success", false);
                    resp.put("message", "Support ticket not found.");
                    return ResponseEntity.status(404).body(resp);
                });
    }

    @DeleteMapping("/tickets/{id}")
    public ResponseEntity<Map<String, Object>> deleteMyTicket(@PathVariable Long id,
                                                                HttpServletRequest request) {
        Map<String, Object> resp = new HashMap<>();
        String username = getCurrentUsername();

        return supportTicketRepository.findById(id)
                .map(ticket -> {
                    if (!username.equals(ticket.getCreatedBy())) {
                        resp.put("success", false);
                        resp.put("message", "You are not allowed to delete this ticket.");
                        return ResponseEntity.status(403).body(resp);
                    }
                    String ticketTitle = ticket.getTitle();
                    supportTicketRepository.delete(ticket);

                    auditLogService.log(
                            "SUPPORT_TICKET_DELETED",
                            String.format("Deleted support ticket #%d: Title='%s'", id, ticketTitle),
                            request
                    );

                    resp.put("success", true);
                    resp.put("message", "Support ticket deleted successfully.");
                    return ResponseEntity.ok(resp);
                })
                .orElseGet(() -> {
                    resp.put("success", false);
                    resp.put("message", "Support ticket not found.");
                    return ResponseEntity.status(404).body(resp);
                });
    }
}

