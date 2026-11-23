package com.indocrm.indocrm.security;

import java.util.ArrayList;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Single place that turns a free-text role ("Telecaller", "Senior Telecaller",
 * "Team Lead", "Super Admin", "ROLE_ADMIN" ...) into Spring authorities.
 *
 * Why this exists:
 *   Telecaller.getAuthorities() used to do  "ROLE_" + role.toUpperCase()
 *   - NPE when role was null
 *   - "Senior Telecaller" became "ROLE_SENIOR TELECALLER" (with a space), so every
 *     hasRole('TELECALLER') endpoint answered 403 for Senior Telecallers / Team Leads.
 */
public final class RoleUtil {

    private RoleUtil() {}

    public static String normalize(String role, String defaultRole) {
        if (role == null || role.trim().isEmpty()) {
            return defaultRole;
        }
        String normalized = role.trim()
                .toUpperCase()
                .replace("-", "_")
                .replace(" ", "_");
        if (normalized.startsWith("ROLE_")) {
            normalized = normalized.substring(5);
        }
        return normalized.isEmpty() ? defaultRole : normalized;
    }

    /** True for every role that belongs to the telecaller portal. */
    public static boolean isTelecallerFamily(String normalizedRole) {
        return normalizedRole != null
                && (normalizedRole.contains("TELECALLER")
                    || normalizedRole.contains("TELE_CALLER")
                    || normalizedRole.equals("TEAM_LEAD"));
    }

    /**
     * ROLE_<NORMALIZED> and, for the telecaller family, also ROLE_TELECALLER so the
     * existing hasRole('TELECALLER') checks keep working for every telecaller role.
     */
    public static List<GrantedAuthority> authorities(String role, String defaultRole) {
        String normalized = normalize(role, defaultRole);
        List<GrantedAuthority> list = new ArrayList<>();
        list.add(new SimpleGrantedAuthority("ROLE_" + normalized));
        if (isTelecallerFamily(normalized) && !normalized.equals("TELECALLER")) {
            list.add(new SimpleGrantedAuthority("ROLE_TELECALLER"));
        }
        return list;
    }
}
