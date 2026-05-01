package org.example.pim_system.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.example.pim_system.model.Role;
import org.example.pim_system.repository.RoleRepository;
import org.example.pim_system.service.AuditLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/roles")
public class RoleController {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private AuditLogService auditLogService;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllRoles() {
        List<Role> roles = roleRepository.findAll();
        List<Map<String, Object>> body = roles.stream().map(this::toDto).collect(Collectors.toList());
        return ResponseEntity.ok(body);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createRole(@RequestBody Map<String, Object> payload,
                                                            HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();

        try {
            String name = asString(payload.get("name"));
            String description = asString(payload.get("description"));
            @SuppressWarnings("unchecked")
            List<String> permissionsList = (List<String>) payload.get("permissions");
//role management validation
            if (name == null || name.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Role name is required!");
                return ResponseEntity.badRequest().body(response);
            }

            if (roleRepository.existsByName(name.trim())) {
                response.put("success", false);
                response.put("message", "A role with this name already exists!");
                return ResponseEntity.badRequest().body(response);
            }

            String permissions = permissionsList != null
                    ? String.join(",", permissionsList)
                    : "";

            Role role = new Role(name.trim(), description, permissions);
            Role saved = roleRepository.save(role);

            auditLogService.log(
                    "ROLE_CREATED",
                    String.format("Created new role: Name=%s, Description=%s, Permissions=[%s]",
                            saved.getName(),
                            description != null ? description : "none",
                            permissions),
                    request
            );

            response.put("success", true);
            response.put("message", "Role created successfully!");
            response.put("role", toDto(saved));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error creating role: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
//update roles
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateRole(@PathVariable Long id,
                                                           @RequestBody Map<String, Object> payload,
                                                           HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();

        try {
            Role role = roleRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Role not found"));

            StringBuilder changes = new StringBuilder();
            String oldName = role.getName();
            String oldDescription = role.getDescription();
            String oldPermissions = role.getPermissions();

            String name = asString(payload.get("name"));
            String description = asString(payload.get("description"));
            @SuppressWarnings("unchecked")
            List<String> permissionsList = (List<String>) payload.get("permissions");

            if (name != null && !name.trim().isEmpty()) {
                String trimmed = name.trim();
                if (!trimmed.equals(role.getName()) && roleRepository.existsByName(trimmed)) {
                    response.put("success", false);
                    response.put("message", "Another role with this name already exists!");
                    return ResponseEntity.badRequest().body(response);
                }
                if (!trimmed.equals(oldName)) {
                    changes.append(String.format("Name: '%s' → '%s'. ", oldName, trimmed));
                }
                role.setName(trimmed);
            }

            if (description != null) {
                if (!description.equals(oldDescription)) {
                    changes.append("Description updated. ");
                }
                role.setDescription(description);
            }

            if (permissionsList != null) {
                String permissions = String.join(",", permissionsList);
                if (!permissions.equals(oldPermissions)) {
                    changes.append(String.format("Permissions: [%s] → [%s]. ",
                            oldPermissions != null ? oldPermissions : "", permissions));
                }
                role.setPermissions(permissions);
            }

            Role saved = roleRepository.save(role);

            String changeDetails = changes.length() > 0 ? changes.toString() : "No fields changed.";
            auditLogService.log(
                    "ROLE_UPDATED",
                    String.format("Updated role #%d (%s). Changes: %s", id, saved.getName(), changeDetails),
                    request
            );

            response.put("success", true);
            response.put("message", "Role updated successfully!");
            response.put("role", toDto(saved));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error updating role: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
//delete roles
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteRole(@PathVariable Long id,
                                                            HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();

        try {
            Role role = roleRepository.findById(id).orElse(null);
            if (role == null) {
                response.put("success", false);
                response.put("message", "Role not found");
                return ResponseEntity.badRequest().body(response);
            }

            String roleName = role.getName();
            roleRepository.deleteById(id);

            auditLogService.log(
                    "ROLE_DELETED",
                    String.format("Deleted role #%d: Name=%s", id, roleName),
                    request
            );

            response.put("success", true);
            response.put("message", "Role deleted successfully!");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error deleting role: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    private Map<String, Object> toDto(Role role) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", role.getId());
        dto.put("name", role.getName());
        dto.put("description", role.getDescription());

        String permissions = role.getPermissions();
        if (permissions == null || permissions.isBlank()) {
            dto.put("permissions", List.of());
        } else {
            List<String> list = List.of(permissions.split("\\s*,\\s*"));
            dto.put("permissions", list);
        }

        return dto;
    }

    private String asString(Object value) {
        return value != null ? String.valueOf(value) : null;
    }
}

