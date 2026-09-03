package com.vikkash.assetmanagementv1.config;

import com.vikkash.assetmanagementv1.entity.Admin;
import com.vikkash.assetmanagementv1.entity.Role;
import com.vikkash.assetmanagementv1.repository.AdminRepository;
import com.vikkash.assetmanagementv1.repository.RoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Explicit, one-shot production bootstrap for the first administrator.
 * This bean is unavailable during normal application startup.
 */
@Component
@Profile("admin-bootstrap")
@Order(Ordered.LOWEST_PRECEDENCE)
public class AdminBootstrapRunner implements org.springframework.boot.CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapRunner.class);

    private final AdminRepository adminRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${ADMIN_BOOTSTRAP_USERNAME:}")
    private String username;

    @Value("${ADMIN_BOOTSTRAP_PASSWORD:}")
    private String password;

    @Value("${ADMIN_BOOTSTRAP_EMAIL:}")
    private String email;

    @Value("${ADMIN_BOOTSTRAP_NAME:System Administrator}")
    private String name;

    public AdminBootstrapRunner(AdminRepository adminRepository,
                                RoleRepository roleRepository,
                                PasswordEncoder passwordEncoder) {
        this.adminRepository = adminRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        String normalizedUsername = requireValue("ADMIN_BOOTSTRAP_USERNAME", username).trim();
        String suppliedPassword = requireValue("ADMIN_BOOTSTRAP_PASSWORD", password);
        String normalizedEmail = requireValue("ADMIN_BOOTSTRAP_EMAIL", email).trim().toLowerCase();

        if (adminRepository.existsByUsername(normalizedUsername)) {
            log.warn("Admin bootstrap refused: username already exists username={}", normalizedUsername);
            return;
        }

        Role systemAdmin = roleRepository.findByName("SYSTEM_ADMIN")
                .orElseThrow(() -> new IllegalStateException("SYSTEM_ADMIN role is not seeded"));

        Admin admin = new Admin();
        admin.setUsername(normalizedUsername);
        admin.setPassword(passwordEncoder.encode(suppliedPassword));
        admin.setEmail(normalizedEmail);
        admin.setName(name == null || name.isBlank() ? "System Administrator" : name.trim());
        admin.setRoleRef(systemAdmin);
        adminRepository.save(admin);

        log.info("Admin bootstrap completed successfully for username={}", normalizedUsername);
    }

    private static String requireValue(String variableName, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(variableName + " must be provided for admin bootstrap");
        }
        return value;
    }
}