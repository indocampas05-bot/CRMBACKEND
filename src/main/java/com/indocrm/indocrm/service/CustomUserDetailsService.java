package com.indocrm.indocrm.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.indocrm.indocrm.repository.EmployeeRepository;
import com.indocrm.indocrm.repository.TelecallerRepository;
import com.indocrm.indocrm.repository.UserRepository;

/**
 * Lookup order:  users (super admin / admin)  ->  telecallers  ->  employees.
 *
 * IMPORTANT CHANGE: telecallers are now looked up BEFORE employees.
 *
 * Creating a telecaller also writes a mirror row in "employees". Before, the mirror
 * was checked first, so whenever the mirror was stale (an older employee with the same
 * username, a failed/skipped mirror write, a password changed on only one of the two
 * rows) the telecaller's NEW password was compared against the OLD employee password
 * and login failed. The telecaller document is the source of truth, so it wins.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private TelecallerRepository telecallerRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        String name = username == null ? "" : username.trim();

        return userRepository.findByUsername(name)
                .map(u -> (UserDetails) u)
                .or(() -> telecallerRepository.findByUsername(name).map(t -> (UserDetails) t))
                .or(() -> employeeRepository.findByUsername(name).map(e -> (UserDetails) e))
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + name));
    }
}
