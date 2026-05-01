package org.example.pim_system.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.example.pim_system.model.User;
import org.example.pim_system.repository.UserRepository;
import org.example.pim_system.service.AuditLogService;
import org.example.pim_system.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuditLogService auditLogService;

    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userRepository.findAll();
        return ResponseEntity.ok(users);
    }
    
    @PostMapping
    public ResponseEntity<Map<String, Object>> createUser(@RequestBody Map<String, String> userData,
                                                            HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
         //email required check
        try {
            String email = userData.get("email");
            String password = userData.get("password");
            String role = userData.get("role");
            String enabledStr = userData.get("enabled");
            
            // Validate required fields
            if (email == null || email.isEmpty()) {
                response.put("success", false);
                response.put("message", "Email is required!");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (password == null || password.isEmpty()) {
                response.put("success", false);
                response.put("message", "Password is required!");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (password.length() < 6) {
                response.put("success", false);
                response.put("message", "Password must be at least 6 characters long!");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Check if email already exists
            if (userRepository.existsByEmail(email)) {
                response.put("success", false);
                response.put("message", "Email already exists!");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Create new user using UserService (handles password encoding)
            String userRole = role != null && !role.isEmpty() ? role : "EMPLOYEE";
            boolean enabled = enabledStr != null ? Boolean.parseBoolean(enabledStr) : true;
            
            User user = userService.registerUser(email, password, userRole);
            user.setEnabled(enabled);
            userRepository.save(user);

            auditLogService.log(
                    "USER_CREATED",
                    String.format("Created new user: Email=%s, Role=%s, Enabled=%s",
                            email, userRole, enabled),
                    request
            );
            
            response.put("success", true);
            response.put("message", "User created successfully!");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error creating user: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateUser(@PathVariable Long id,
                                                            @RequestBody Map<String, String> userData,
                                                            HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            StringBuilder changes = new StringBuilder();
            String oldEmail = user.getEmail();
            String oldRole = user.getRole();
            boolean oldEnabled = user.isEnabled();
            
            // Update email if provided
            if (userData.containsKey("email") && userData.get("email") != null && !userData.get("email").isEmpty()) {
                String newEmail = userData.get("email");
                // Check if email is already taken by another user
                if (!user.getEmail().equals(newEmail) && userRepository.existsByEmail(newEmail)) {
                    response.put("success", false);
                    response.put("message", "Email already exists!");
                    return ResponseEntity.badRequest().body(response);
                }
                if (!oldEmail.equals(newEmail)) {
                    changes.append(String.format("Email: '%s' → '%s'. ", oldEmail, newEmail));
                }
                user.setEmail(newEmail);
            }
            
            // Update role if provided
            if (userData.containsKey("role") && userData.get("role") != null && !userData.get("role").isEmpty()) {
                String newRole = userData.get("role");
                if (!oldRole.equals(newRole)) {
                    changes.append(String.format("Role: '%s' → '%s'. ", oldRole, newRole));
                }
                user.setRole(newRole);
            }

            // Update password if provided (optional)
            if (userData.containsKey("password")) {
                String newPassword = userData.get("password");
                if (newPassword != null && !newPassword.isEmpty()) {
                    if (newPassword.length() < 6) {
                        response.put("success", false);
                        response.put("message", "Password must be at least 6 characters long!");
                        return ResponseEntity.badRequest().body(response);
                    }
                    user.setPassword(passwordEncoder.encode(newPassword));
                    changes.append("Password changed. ");
                }
            }
            
            // Update enabled status if provided
            if (userData.containsKey("enabled")) {
                boolean newEnabled = Boolean.parseBoolean(userData.get("enabled"));
                if (oldEnabled != newEnabled) {
                    changes.append(String.format("Enabled: %s → %s. ", oldEnabled, newEnabled));
                }
                user.setEnabled(newEnabled);
            }
            
            userRepository.save(user);

            String changeDetails = changes.length() > 0 ? changes.toString() : "No fields changed.";
            auditLogService.log(
                    "USER_UPDATED",
                    String.format("Updated user #%d (%s). Changes: %s", id, user.getEmail(), changeDetails),
                    request
            );
            
            response.put("success", true);
            response.put("message", "User updated successfully!");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error updating user: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteUser(@PathVariable Long id,
                                                            HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Get current logged-in user
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String currentUserEmail = authentication != null ? authentication.getName() : null;
            
            User userToDelete = userRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Prevent self-deletion
            if (currentUserEmail != null && userToDelete.getEmail().equals(currentUserEmail)) {
                response.put("success", false);
                response.put("message", "You cannot delete your own account!");
                return ResponseEntity.badRequest().body(response);
            }

            String deletedEmail = userToDelete.getEmail();
            String deletedRole = userToDelete.getRole();
            
            userRepository.deleteById(id);

            auditLogService.log(
                    "USER_DELETED",
                    String.format("Deleted user #%d: Email=%s, Role=%s", id, deletedEmail, deletedRole),
                    request
            );
            
            response.put("success", true);
            response.put("message", "User deleted successfully!");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error deleting user: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}

