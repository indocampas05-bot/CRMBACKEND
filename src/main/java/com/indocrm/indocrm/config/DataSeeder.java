// src/main/java/com/indocrm/indocrm/config/DataSeeder.java

package com.indocrm.indocrm.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.indocrm.indocrm.model.User;
import com.indocrm.indocrm.repository.UserRepository;

@Configuration
public class DataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private static final String SUPERADMIN_USERNAME = "superadmin";
    private static final String SUPERADMIN_PASSWORD = "super123"; // change after first login
    private static final String SUPERADMIN_ROLE = "SUPER_ADMIN";

    @Bean
    public CommandLineRunner seedSuperAdmin(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            // Set  app.superadmin.reset-password=true  in application.properties for ONE
            // start-up to force the password back to super123, then remove the line.
            @Value("${app.superadmin.reset-password:false}") boolean resetPassword) {

        return args -> {
            User admin = userRepository.findByUsername(SUPERADMIN_USERNAME).orElse(null);

            if (admin == null) {
                admin = new User();
                admin.setUsername(SUPERADMIN_USERNAME);
                admin.setPassword(passwordEncoder.encode(SUPERADMIN_PASSWORD));
                admin.setFullName("Super Admin");
                admin.setEmail("superadmin@indocrm.com");
                admin.setRole(SUPERADMIN_ROLE);
                admin.setEnabled(true);
                userRepository.save(admin);
                log.info("Seeded Super Admin (username: {} / password: {}). Change it after first login.",
                        SUPERADMIN_USERNAME, SUPERADMIN_PASSWORD);
                return;
            }

            // ---- The account already exists: REPAIR it instead of silently skipping. ----
            // The old seeder did nothing here, so a "superadmin" row created earlier by the
            // public /register endpoint (role USER, or a plain-text / wrong password) stayed
            // broken forever and the real Super Admin could never log in.
            boolean changed = false;

            boolean isSuperAdmin = false;
            for (GrantedAuthority a : admin.getAuthorities()) {
                if ("ROLE_SUPER_ADMIN".equals(a.getAuthority())) {
                    isSuperAdmin = true;
                    break;
                }
            }
            if (!isSuperAdmin) {
                log.warn("'{}' had role '{}' - restoring {}", SUPERADMIN_USERNAME, admin.getRole(), SUPERADMIN_ROLE);
                admin.setRole(SUPERADMIN_ROLE);
                changed = true;
            }

            if (!admin.isEnabled()) {
                log.warn("'{}' was disabled - enabling", SUPERADMIN_USERNAME);
                admin.setEnabled(true);
                changed = true;
            }

            String pw = admin.getPassword();
            boolean looksHashed = pw != null && pw.matches("^\\$2[aby]?\\$\\d{2}\\$.{53}$");
            if (resetPassword || !looksHashed) {
                log.warn("'{}' password {} - resetting to default", SUPERADMIN_USERNAME,
                        resetPassword ? "reset requested" : "was not a valid BCrypt hash");
                admin.setPassword(passwordEncoder.encode(SUPERADMIN_PASSWORD));
                changed = true;
            }

            if (changed) {
                userRepository.save(admin);
                log.info("Super Admin account repaired.");
            }
        };
    }
}
