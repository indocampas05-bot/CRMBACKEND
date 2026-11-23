// FILE: src/main/java/com/indocrm/indocrm/service/TelecallerService.java

package com.indocrm.indocrm.service;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.indocrm.indocrm.model.Employee;
import com.indocrm.indocrm.model.Telecaller;
import com.indocrm.indocrm.repository.EmployeeRepository;
import com.indocrm.indocrm.repository.TelecallerRepository;
import com.indocrm.indocrm.repository.UserRepository;

@Service
public class TelecallerService {

    @Autowired
    private TelecallerRepository telecallerRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static boolean isBcryptHash(String value) {
        return value != null && value.matches("^\\$2[aby]?\\$\\d{2}\\$.{53}$");
    }

    public List<Telecaller> getAllTelecallers() {
        return telecallerRepository.findAll();
    }

    public Optional<Telecaller> getTelecallerById(String id) {
        return telecallerRepository.findById(id);
    }

    public Optional<Telecaller> getTelecallerByEmployeeId(String employeeId) {
        return telecallerRepository.findByEmployeeId(employeeId);
    }

    public Optional<Telecaller> getTelecallerByUsername(String username) {
        return telecallerRepository.findByUsername(username);
    }

    // NEW: Get telecaller by assigned phone number
    public Optional<Telecaller> findByAssignedPhoneNumber(String phoneNumber) {
        return telecallerRepository.findByAssignedPhoneNumber(phoneNumber);
    }

    public Telecaller createTelecaller(Telecaller telecaller) {
        if (telecaller.getUsername() == null || telecaller.getUsername().trim().isEmpty()) {
            throw new RuntimeException("Username is required");
        }
        // Login compares the username exactly, so never store leading/trailing spaces.
        telecaller.setUsername(telecaller.getUsername().trim());

        if (telecallerRepository.existsByUsername(telecaller.getUsername())) {
            throw new RuntimeException("Username already exists: " + telecaller.getUsername());
        }
        // "users" (super admin / admin) is looked up first at login, so a telecaller with the
        // same username could never log in. Refuse it up front instead.
        if (userRepository.existsByUsername(telecaller.getUsername())) {
            throw new RuntimeException("Username already exists: " + telecaller.getUsername());
        }
        if (telecaller.getEmployeeId() == null || telecaller.getEmployeeId().isEmpty()) {
            long count = telecallerRepository.count() + 100;
            telecaller.setEmployeeId("EMP-" + count);
        } else if (telecallerRepository.existsByEmployeeId(telecaller.getEmployeeId())) {
            throw new RuntimeException("Employee ID already exists: " + telecaller.getEmployeeId());
        }

        // Encode password
        String rawPassword;
        if (telecaller.getPassword() != null && !telecaller.getPassword().isEmpty()) {
            rawPassword = telecaller.getPassword();
            telecaller.setPassword(passwordEncoder.encode(rawPassword));
        } else {
            rawPassword = "Temp@" + UUID.randomUUID().toString().substring(0, 6);
            telecaller.setPassword(passwordEncoder.encode(rawPassword));
        }

        if (telecaller.getRole() == null) telecaller.setRole("Telecaller");
        if (telecaller.getStatus() == null) telecaller.setStatus("Active");
        if (telecaller.getDistricts() == null) telecaller.setDistricts(List.of());
        if (telecaller.getCategories() == null) telecaller.setCategories(List.of());
        if (telecaller.getPermissions() == null) telecaller.setPermissions(new java.util.HashMap<>());
        if (telecaller.getPerformance() == null) telecaller.setPerformance(new java.util.HashMap<>());
        if (telecaller.getPhoneNumberStatus() == null) telecaller.setPhoneNumberStatus("Inactive");

        telecaller.setCreatedAt(new Date());
        telecaller.setUpdatedAt(new Date());
        telecaller.setEnabled(!"Inactive".equalsIgnoreCase(telecaller.getStatus()));

        Telecaller saved = telecallerRepository.save(telecaller);
        createEmployeeFromTelecaller(saved, rawPassword);
        
        return saved;
    }

    // NEW: Assign phone number to telecaller
    @Transactional
    public Telecaller assignPhoneNumber(String telecallerId, String phoneNumber) {
        Telecaller telecaller = telecallerRepository.findById(telecallerId)
                .orElseThrow(() -> new RuntimeException("Telecaller not found"));
        
        // Check if phone number is already assigned
        if (telecallerRepository.existsByAssignedPhoneNumber(phoneNumber)) {
            throw new RuntimeException("Phone number already assigned to another telecaller");
        }
        
        telecaller.setAssignedPhoneNumber(phoneNumber);
        telecaller.setPhoneNumberStatus("Active");
        telecaller.setUpdatedAt(new Date());
        
        return telecallerRepository.save(telecaller);
    }

    // NEW: Unassign phone number
    @Transactional
    public Telecaller unassignPhoneNumber(String telecallerId) {
        Telecaller telecaller = telecallerRepository.findById(telecallerId)
                .orElseThrow(() -> new RuntimeException("Telecaller not found"));
        
        telecaller.setAssignedPhoneNumber(null);
        telecaller.setPhoneNumberStatus("Inactive");
        telecaller.setUpdatedAt(new Date());
        
        return telecallerRepository.save(telecaller);
    }

    // Mirror row in "employees". UPSERT: the old code skipped the write when an employee with
    // the same username already existed, leaving that employee's OLD password in place.
    private void createEmployeeFromTelecaller(Telecaller telecaller, String password) {
        try {
            Employee employee = employeeRepository.findByUsername(telecaller.getUsername()).orElse(null);
            boolean isNew = employee == null;
            if (isNew) {
                employee = new Employee();
                employee.setUsername(telecaller.getUsername());
                employee.setCreatedAt(new Date());
            }
            employee.setPassword(passwordEncoder.encode(password));
            employee.setFullName(telecaller.getFullName());
            employee.setEmail(telecaller.getEmail());
            employee.setPhone(telecaller.getMobile());
            employee.setRole(telecaller.getRole() != null ? telecaller.getRole() : "Telecaller");
            employee.setEnabled(telecaller.isEnabled());
            employee.setUpdatedAt(new Date());
            employeeRepository.save(employee);
            System.out.println((isNew ? "Created" : "Updated") + " Employee record for telecaller: " + telecaller.getUsername());
        } catch (Exception e) {
            System.err.println("Failed to sync employee record: " + e.getMessage());
        }
    }

    public Telecaller updateTelecaller(String id, Telecaller updated) {
        Telecaller existing = telecallerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Telecaller not found"));

        existing.setFullName(updated.getFullName());
        existing.setMobile(updated.getMobile());
        existing.setEmail(updated.getEmail());
        existing.setRole(updated.getRole());
        existing.setDistricts(updated.getDistricts());
        existing.setCategories(updated.getCategories());
        existing.setJoiningDate(updated.getJoiningDate());
        existing.setStatus(updated.getStatus());
        existing.setTarget(updated.getTarget());
        existing.setPermissions(updated.getPermissions());
        existing.setPerformance(updated.getPerformance());
        existing.setWorkTiming(updated.getWorkTiming());
        existing.setAssignedPhoneNumber(updated.getAssignedPhoneNumber());
        existing.setPhoneNumberStatus(updated.getPhoneNumberStatus());
        existing.setUpdatedAt(new Date());

        existing.setEnabled(!"Inactive".equalsIgnoreCase(existing.getStatus()));

        // Only a genuinely NEW plain-text password is re-encoded. If a client sends back the
        // stored hash (older UIs did, because GET /telecallers used to include it) it is
        // ignored - encoding a hash again silently destroys the real password.
        String newPassword = updated.getPassword();
        if (newPassword != null && !newPassword.isEmpty()
                && !newPassword.equals(existing.getPassword())
                && !isBcryptHash(newPassword)) {
            String encoded = passwordEncoder.encode(newPassword);
            existing.setPassword(encoded);
            employeeRepository.findByUsername(existing.getUsername()).ifPresent(emp -> {
                emp.setPassword(encoded);
                employeeRepository.save(emp);
            });
        }

        employeeRepository.findByUsername(existing.getUsername()).ifPresent(emp -> {
            emp.setFullName(existing.getFullName());
            emp.setEmail(existing.getEmail());
            emp.setPhone(existing.getMobile());
            emp.setRole(existing.getRole());
            emp.setEnabled(existing.isEnabled());
            emp.setUpdatedAt(new Date());
            employeeRepository.save(emp);
        });

        return telecallerRepository.save(existing);
    }

    public void deleteTelecaller(String id) {
        Telecaller telecaller = telecallerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Telecaller not found"));
        
        employeeRepository.findByUsername(telecaller.getUsername()).ifPresent(emp -> {
            employeeRepository.delete(emp);
        });
        
        telecallerRepository.deleteById(id);
    }

    public void resetPassword(String id, String newPassword) {
        Telecaller telecaller = telecallerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Telecaller not found"));
        telecaller.setPassword(passwordEncoder.encode(newPassword));
        telecaller.setUpdatedAt(new Date());
        telecallerRepository.save(telecaller);
        
        createEmployeeFromTelecaller(telecaller, newPassword);
    }

    public Telecaller toggleStatus(String id) {
        Telecaller telecaller = telecallerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Telecaller not found"));
        telecaller.setStatus("Active".equals(telecaller.getStatus()) ? "Inactive" : "Active");
        telecaller.setEnabled("Active".equals(telecaller.getStatus()));
        telecaller.setUpdatedAt(new Date());
        Telecaller saved = telecallerRepository.save(telecaller);
        
        employeeRepository.findByUsername(telecaller.getUsername()).ifPresent(emp -> {
            emp.setEnabled(telecaller.isEnabled());
            employeeRepository.save(emp);
        });
        
        return saved;
    }

    public List<Telecaller> getTelecallersByDistrict(String district) {
        return telecallerRepository.findByDistrictsContaining(district);
    }
}


