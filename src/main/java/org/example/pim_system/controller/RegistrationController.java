package org.example.pim_system.controller;

import org.example.pim_system.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class RegistrationController {

    @Autowired
    private UserService userService;

    @GetMapping("/register") //returns(register.html)
    public String showRegistrationForm() {
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam String confirmPassword,
            Model model) {

        // Validate email format
        if (!isValidEmail(email)) {
            model.addAttribute("error", "Please enter a valid email address!");
            return "register";
        }

        // Validate passwords match
        if (!password.equals(confirmPassword)) {
            model.addAttribute("error", "Passwords do not match!");
            return "register";
        }

        // Validate password length
        if (password.length() < 6) {
            model.addAttribute("error", "Password must be at least 6 characters long!");
            return "register";
        }

        // Check if email already exists
        if (userService.emailExists(email)) {
            model.addAttribute("error", "Email already exists. Please use a different email.");
            return "register";
        }

        try {
            // Register the user with default role EMPLOYEE
            userService.registerUser(email, password, "EMPLOYEE");
            model.addAttribute("success", true);
            return "register";
        } catch (Exception e) {
            model.addAttribute("error", "Registration failed: " + e.getMessage());
            return "register";
        }
    }
//Checks whether email matches the regex pattern
    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }
}

