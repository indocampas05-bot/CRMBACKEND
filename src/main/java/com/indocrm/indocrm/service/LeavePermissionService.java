package com.indocrm.indocrm.service;

import java.time.Instant;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.indocrm.indocrm.model.LeavePermissionRequest;
import com.indocrm.indocrm.model.Telecaller;
import com.indocrm.indocrm.repository.LeavePermissionRequestRepository;
import com.indocrm.indocrm.repository.TelecallerRepository;

@Service
public class LeavePermissionService {

    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ISO_LOCAL_DATE;

    private final LeavePermissionRequestRepository repository;

    private final TelecallerRepository telecallerRepository;

    public LeavePermissionService(
            LeavePermissionRequestRepository repository,
            TelecallerRepository telecallerRepository) {

        this.repository = repository;
        this.telecallerRepository = telecallerRepository;
    }

    /*
     * ============================================================
     * CURRENT USER
     * ============================================================
     */

    public String currentUsername() {

        Authentication auth =
                SecurityContextHolder
                    .getContext()
                    .getAuthentication();

        if (auth == null ||
            auth.getName() == null ||
            !auth.isAuthenticated()) {

            throw new AccessDeniedException(
                "Authentication required"
            );
        }

        return auth.getName();
    }

    /*
     * ============================================================
     * TELECALLER - MY REQUESTS
     * ============================================================
     */

    public List<LeavePermissionRequest> mine() {

        return repository
            .findByUsernameOrderBySubmittedAtDesc(
                currentUsername()
            );
    }

    /*
     * ============================================================
     * CREATE REQUEST
     * ============================================================
     */

    public LeavePermissionRequest create(
            LeavePermissionRequest request) {

        String username = currentUsername();

        Telecaller telecaller =
            telecallerRepository
                .findByUsername(username)
                .orElseThrow(() ->
                    new AccessDeniedException(
                        "Telecaller account not found"
                    )
                );

        validateRequest(request);

        request.setId(null);

        request.setUsername(username);

        request.setEmployeeId(
            telecaller.getEmployeeId()
        );

        request.setEmployeeName(
            telecaller.getFullName() != null
                ? telecaller.getFullName()
                : username
        );

        request.setStatus("Pending");

        request.setSubmittedAt(
            Instant.now()
        );

        request.setReviewedAt(null);
        request.setReviewedBy(null);
        request.setRejectionReason(null);

        preventOverlap(
            request,
            username,
            null
        );

        return repository.save(request);
    }

    /*
     * ============================================================
     * TELECALLER - UPDATE
     * ============================================================
     */

    public LeavePermissionRequest updateMine(
            String id,
            LeavePermissionRequest input) {

        String username = currentUsername();

        LeavePermissionRequest existing =
            repository
                .findByIdAndUsername(id, username)
                .orElseThrow(() ->
                    new AccessDeniedException(
                        "Request not found or access denied"
                    )
                );

        if (!"Pending".equalsIgnoreCase(
                existing.getStatus())) {

            throw new IllegalStateException(
                "Only pending requests can be edited"
            );
        }

        validateRequest(input);

        existing.setType(input.getType());
        existing.setLeaveType(input.getLeaveType());

        existing.setFromDate(input.getFromDate());
        existing.setToDate(input.getToDate());

        existing.setDate(input.getDate());

        existing.setFromTime(input.getFromTime());
        existing.setToTime(input.getToTime());

        existing.setDuration(input.getDuration());

        existing.setReason(input.getReason());

        preventOverlap(
            existing,
            username,
            id
        );

        return repository.save(existing);
    }

    /*
     * ============================================================
     * TELECALLER - DELETE
     * ============================================================
     */

    public void deleteMine(String id) {

        String username = currentUsername();

        LeavePermissionRequest existing =
            repository
                .findByIdAndUsername(id, username)
                .orElseThrow(() ->
                    new AccessDeniedException(
                        "Request not found or access denied"
                    )
                );

        if (!"Pending".equalsIgnoreCase(
                existing.getStatus())) {

            throw new IllegalStateException(
                "Only pending requests can be deleted"
            );
        }

        repository.delete(existing);
    }

    /*
     * ============================================================
     * SUPER ADMIN - LIST
     * ============================================================
     */

    public List<LeavePermissionRequest> adminList(
            String status) {

        if (status == null ||
            status.isBlank() ||
            "All".equalsIgnoreCase(status)) {

            return repository
                .findAllByOrderBySubmittedAtDesc();
        }

        return repository
            .findByStatusOrderBySubmittedAtDesc(status);
    }

    /*
     * ============================================================
     * SUPER ADMIN - APPROVE / REJECT
     * ============================================================
     */

    public LeavePermissionRequest updateStatus(
            String id,
            String status,
            String rejectionReason) {

        if (!"Approved".equalsIgnoreCase(status) &&
            !"Rejected".equalsIgnoreCase(status)) {

            throw new IllegalArgumentException(
                "Status must be Approved or Rejected"
            );
        }

        LeavePermissionRequest request =
            repository.findById(id)
                .orElseThrow(() ->
                    new IllegalArgumentException(
                        "Leave/permission request not found"
                    )
                );

        if (!"Pending".equalsIgnoreCase(
                request.getStatus())) {

            throw new IllegalStateException(
                "Only pending requests can be reviewed"
            );
        }

        String finalStatus =
            capitalize(status);

        request.setStatus(finalStatus);

        request.setReviewedAt(
            Instant.now()
        );

        request.setReviewedBy(currentUsername());
        if ("Approved".equals(finalStatus)) { request.setApprovedBy(currentUsername()); request.setApprovalDateTime(request.getReviewedAt()); }
        else { request.setApprovedBy(null); request.setApprovalDateTime(null); }

        if ("Rejected".equals(finalStatus)) {

            request.setRejectionReason(
                rejectionReason
            );

        } else {

            request.setRejectionReason(null);
        }

        return repository.save(request);
    }


    public LeavePermissionRequest adminCreate(LeavePermissionRequest request) {
        if (request == null || request.getUsername() == null || request.getUsername().isBlank()) throw new IllegalArgumentException("Employee username is required");
        Telecaller tc = telecallerRepository.findByUsername(request.getUsername()).orElseThrow(() -> new IllegalArgumentException("Telecaller not found"));
        validateRequest(request); request.setId(null); request.setEmployeeId(tc.getEmployeeId()); request.setEmployeeName(tc.getFullName()!=null?tc.getFullName():tc.getUsername());
        if (request.getStatus()==null || request.getStatus().isBlank()) request.setStatus("Pending");
        request.setSubmittedAt(Instant.now());
        request.setReviewedAt(null); request.setReviewedBy(null);
        preventOverlap(request, request.getUsername(), null);
        return repository.save(request);
    }

    public LeavePermissionRequest adminUpdate(String id, LeavePermissionRequest input) {
        LeavePermissionRequest existing=repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Request not found"));
        validateRequest(input); existing.setType(input.getType()); existing.setLeaveType(input.getLeaveType()); existing.setFromDate(input.getFromDate()); existing.setToDate(input.getToDate()); existing.setDate(input.getDate()); existing.setFromTime(input.getFromTime()); existing.setToTime(input.getToTime()); existing.setDuration(input.getDuration()); existing.setDurationMinutes(input.getDurationMinutes()); existing.setPermissionType(input.getPermissionType()); existing.setReason(input.getReason());
        preventOverlap(existing, existing.getUsername(), id); return repository.save(existing);
    }

    /*
     * ============================================================
     * TELECALLER SUMMARY
     * ============================================================
     */

    public Map<String, Object> mySummary() {

        String username = currentUsername();

        List<LeavePermissionRequest> all =
            repository
                .findByUsernameOrderBySubmittedAtDesc(
                    username
                );

        int approvedLeave = 0;

        int pending = 0;

        int approvedPermissions = 0;

        for (LeavePermissionRequest request : all) {

            if ("Pending".equalsIgnoreCase(
                    request.getStatus())) {

                pending++;
            }

            if ("Approved".equalsIgnoreCase(
                    request.getStatus())) {

                if ("Leave".equalsIgnoreCase(
                        request.getType())) {

                    approvedLeave +=
                        parseDays(request);

                } else if (
                    "Permission".equalsIgnoreCase(
                        request.getType())) {

                    approvedPermissions++;
                }
            }
        }

        Map<String, Object> result =
            new LinkedHashMap<>();

        result.put("totalLeave", 24);

        result.put(
            "usedLeave",
            approvedLeave
        );

        result.put(
            "remainingLeave",
            Math.max(
                0,
                24 - approvedLeave
            )
        );

        result.put(
            "pendingRequests",
            pending
        );

        result.put(
            "approvedPermissions",
            approvedPermissions
        );

        return result;
    }

    /*
     * ============================================================
     * SUPER ADMIN SUMMARY
     * ============================================================
     */

    public Map<String, Object> adminSummary() {

        List<LeavePermissionRequest> all =
            repository
                .findAllByOrderBySubmittedAtDesc();

        long pending =
            all.stream()
                .filter(r ->
                    "Pending".equalsIgnoreCase(
                        r.getStatus()))
                .count();

        long approvedToday =
            all.stream()
                .filter(r ->
                    "Approved".equalsIgnoreCase(
                        r.getStatus()) &&
                    sameDay(r.getReviewedAt()))
                .count();

        long rejectedToday =
            all.stream()
                .filter(r ->
                    "Rejected".equalsIgnoreCase(
                        r.getStatus()) &&
                    sameDay(r.getReviewedAt()))
                .count();

        long approvedLeave =
            all.stream()
                .filter(r ->
                    "Approved".equalsIgnoreCase(
                        r.getStatus()) &&
                    "Leave".equalsIgnoreCase(
                        r.getType()))
                .count();

        long approvedPermission =
            all.stream()
                .filter(r ->
                    "Approved".equalsIgnoreCase(
                        r.getStatus()) &&
                    "Permission".equalsIgnoreCase(
                        r.getType()))
                .count();

        Map<String, Object> result =
            new LinkedHashMap<>();

        result.put(
            "totalRequests",
            all.size()
        );

        result.put(
            "pendingRequests",
            pending
        );

        result.put(
            "approvedToday",
            approvedToday
        );

        result.put(
            "rejectedToday",
            rejectedToday
        );

        result.put(
            "approvedLeaveRequests",
            approvedLeave
        );

        result.put(
            "approvedPermissionRequests",
            approvedPermission
        );

        return result;
    }

    /*
     * ============================================================
     * VALIDATION
     * ============================================================
     */

    private void validateRequest(
            LeavePermissionRequest request) {

        if (request == null ||
            request.getType() == null) {

            throw new IllegalArgumentException(
                "Request type is required"
            );
        }

        /*
         * LEAVE
         */
        if ("Leave".equalsIgnoreCase(
                request.getType())) {

            if (blank(request.getFromDate()) ||
                blank(request.getToDate())) {

                throw new IllegalArgumentException(
                    "From and To dates are required"
                );
            }

            LocalDate from =
                LocalDate.parse(
                    request.getFromDate(),
                    DATE
                );

            LocalDate to =
                LocalDate.parse(
                    request.getToDate(),
                    DATE
                );

            if (to.isBefore(from)) {

                throw new IllegalArgumentException(
                    "To date must be on or after From date"
                );
            }

            if (from.isBefore(
                    LocalDate.now(ZoneId.of("Asia/Kolkata")))) {

                throw new IllegalArgumentException(
                    "Leave cannot start in the past"
                );
            }

            long days =
                ChronoUnit.DAYS.between(
                    from,
                    to
                ) + 1;

            request.setDuration(days + " Days");
            request.setDurationMinutes((int) (days * 8 * 60));
        }

        /*
         * PERMISSION
         */
        else if ("Permission".equalsIgnoreCase(
                    request.getType())) {

            if (blank(request.getDate())) {

                throw new IllegalArgumentException(
                    "Date, From time and To time are required"
                );
            }

            LocalDate date =
                LocalDate.parse(
                    request.getDate(),
                    DATE
                );

            if (date.isBefore(
                    LocalDate.now(ZoneId.of("Asia/Kolkata")))) {

                throw new IllegalArgumentException(
                    "Permission date cannot be in the past"
                );
            }

            if ("HALF_DAY".equalsIgnoreCase(request.getPermissionType()) && blank(request.getFromTime())) {
                request.setFromTime("09:30");
                request.setToTime("13:30");
            }
            if (blank(request.getFromTime()) || blank(request.getToTime())) {
                throw new IllegalArgumentException("From time and To time are required");
            }

            LocalTime from = LocalTime.parse(request.getFromTime());
            LocalTime to = LocalTime.parse(request.getToTime());

            if (!to.isAfter(from)) {

                throw new IllegalArgumentException(
                    "To time must be after From time"
                );
            }

            long minutes =
                ChronoUnit.MINUTES.between(
                    from,
                    to
                );

            String duration =
                (minutes / 60) +
                "h " +
                (minutes % 60 == 0
                    ? ""
                    : (minutes % 60) + "m");

            request.setDuration(duration.trim());
            request.setDurationMinutes((int) minutes);
        }

        else {

            throw new IllegalArgumentException(
                "Type must be Leave or Permission"
            );
        }

        if (blank(request.getReason())) {

            throw new IllegalArgumentException(
                "Reason is required"
            );
        }
    }

    /*
     * ============================================================
     * OVERLAP CHECK
     * ============================================================
     */

    private void preventOverlap(
            LeavePermissionRequest request,
            String username,
            String ignoreId) {

        List<LeavePermissionRequest> mine =
            repository
                .findByUsernameOrderBySubmittedAtDesc(
                    username
                );

        if ("Leave".equalsIgnoreCase(request.getType())) {
            LocalDate from = LocalDate.parse(request.getFromDate());
            LocalDate to = LocalDate.parse(request.getToDate());
            for (LeavePermissionRequest other : mine) {
                if (ignoreId != null && ignoreId.equals(other.getId())) continue;
                if (!("Pending".equalsIgnoreCase(other.getStatus()) || "Approved".equalsIgnoreCase(other.getStatus()))) continue;
                if (!"Leave".equalsIgnoreCase(other.getType()) || blank(other.getFromDate()) || blank(other.getToDate())) continue;
                LocalDate otherFrom=LocalDate.parse(other.getFromDate()), otherTo=LocalDate.parse(other.getToDate());
                if (!to.isBefore(otherFrom) && !from.isAfter(otherTo)) throw new IllegalArgumentException("This leave overlaps an existing pending/approved leave");
            }
            return;
        }

        if ("Permission".equalsIgnoreCase(request.getType())) {
            if (blank(request.getDate()) || blank(request.getFromTime()) || blank(request.getToTime())) return;
            LocalDate date=LocalDate.parse(request.getDate()); LocalTime from=LocalTime.parse(request.getFromTime()); LocalTime to=LocalTime.parse(request.getToTime());
            for (LeavePermissionRequest other : mine) {
                if (ignoreId != null && ignoreId.equals(other.getId())) continue;
                if (!("Pending".equalsIgnoreCase(other.getStatus()) || "Approved".equalsIgnoreCase(other.getStatus()))) continue;
                if (!"Permission".equalsIgnoreCase(other.getType()) || !request.getDate().equals(other.getDate())) continue;
                try { LocalTime of=LocalTime.parse(other.getFromTime()), ot=LocalTime.parse(other.getToTime()); if (from.isBefore(ot) && to.isAfter(of)) throw new IllegalArgumentException("This permission overlaps an existing pending/approved permission"); } catch (DateTimeException ignored) {}
            }
        }
    }

    /*
     * ============================================================
     * HELPERS
     * ============================================================
     */

    private int parseDays(
            LeavePermissionRequest request) {

        try {

            if (request.getDuration() == null) {
                return 0;
            }

            String value =
                request.getDuration()
                    .replaceAll(
                        "[^0-9].*",
                        ""
                    );

            return Integer.parseInt(value);

        } catch (Exception e) {

            return 0;
        }
    }

    private boolean sameDay(
            Instant instant) {

        if (instant == null) {
            return false;
        }

        return instant
            .atZone(
                ZoneId.of("Asia/Kolkata")
            )
            .toLocalDate()
            .equals(
                LocalDate.now(ZoneId.of("Asia/Kolkata"))
            );
    }

    private boolean blank(String value) {

        return value == null ||
               value.isBlank();
    }

    private String capitalize(String value) {

        return value.substring(0, 1)
            .toUpperCase() +
            value.substring(1)
                .toLowerCase();
    }
}
