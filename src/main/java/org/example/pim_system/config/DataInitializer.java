package org.example.pim_system.config;

import org.example.pim_system.model.AuditLog;
import org.example.pim_system.model.User;
import org.example.pim_system.repository.AuditLogRepository;
import org.example.pim_system.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Override
    public void run(String... args) throws Exception {
        // Create default admin user if it doesn't exist
        if (!userRepository.existsByEmail("admin@example.com")) {
            User admin = new User();
            admin.setEmail("admin@example.com");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRole("ADMIN");
            admin.setEnabled(true);
            userRepository.save(admin);
            System.out.println("Default admin user created: admin@example.com/admin123");
        }

        // Create default employee user if it doesn't exist (redirects to /employee-work)
        if (!userRepository.existsByEmail("employee@example.com")) {
            User employee = new User();
            employee.setEmail("employee@example.com");
            employee.setPassword(passwordEncoder.encode("employee123"));
            employee.setRole("EMPLOYEE");
            employee.setEnabled(true);
            userRepository.save(employee);
            System.out.println("Default employee user created: employee@example.com/employee123");
        }

        // Create default production manager user if it doesn't exist
        if (!userRepository.existsByEmail("manager@example.com")) {
            User manager = new User();
            manager.setEmail("manager@example.com");
            manager.setPassword(passwordEncoder.encode("manager123"));
            manager.setRole("PRODUCTION_MANAGER");
            manager.setEnabled(true);
            userRepository.save(manager);
            System.out.println("Default manager user created: manager@example.com/manager123");
        }

        // Seed a few sample audit log entries if none exist
        if (auditLogRepository.count() == 0) {
            AuditLog log1 = new AuditLog();
            log1.setUsername("admin@example.com");
            log1.setAction("User login");
            log1.setDetails("Admin user logged in");
            log1.setIpAddress("127.0.0.1");

            AuditLog log2 = new AuditLog();
            log2.setUsername("manager@example.com");
            log2.setAction("Production plan updated");
            log2.setDetails("Updated production schedule for Feb 28");
            log2.setIpAddress("127.0.0.1");

            AuditLog log3 = new AuditLog();
            log3.setUsername("admin@example.com");
            log3.setAction("User management");
            log3.setDetails("Created new payroll officer account");
            log3.setIpAddress("127.0.0.1");

            auditLogRepository.save(log1);
            auditLogRepository.save(log2);
            auditLogRepository.save(log3);
        }

        // Note: Needle Types are managed via Needle Management.
    }
}

