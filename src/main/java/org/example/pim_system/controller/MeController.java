package org.example.pim_system.controller;

import org.example.pim_system.model.Role;
import org.example.pim_system.model.User;
import org.example.pim_system.repository.RoleRepository;
import org.example.pim_system.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class MeController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @GetMapping("/api/me")
    public ResponseEntity<Map<String, Object>> getCurrentUserInfo() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        String email = authentication.getName();
        User user = userRepository.findByEmail(email).orElse(null);

        Map<String, Object> body = new HashMap<>();
        body.put("email", email);

        if (user != null) {
            body.put("role", user.getRole());

            Role role = roleRepository.findByName(user.getRole()).orElse(null);
            if (role != null) {
                String permissions = role.getPermissions();
                if (permissions == null || permissions.isBlank()) {
                    body.put("modules", List.of());
                } else {
                    List<String> modules = List.of(permissions.split("\\s*,\\s*"));
                    body.put("modules", modules);
                }
            } else {
                body.put("modules", List.of());
            }
        } else {
            body.put("role", null);
            body.put("modules", List.of());
        }

        return ResponseEntity.ok(body);
    }
}

