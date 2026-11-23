package com.indocrm.indocrm.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.indocrm.indocrm.model.CalendarEvent;
import com.indocrm.indocrm.model.Lead;
import com.indocrm.indocrm.repository.CalendarEventRepository;
import com.indocrm.indocrm.repository.LeadRepository;
import com.indocrm.indocrm.repository.TelecallerRepository;

@Service
public class CalendarService {

    private final CalendarEventRepository events;
    private final LeadRepository leads;
    private final TelecallerRepository telecallers;

    public CalendarService(
            CalendarEventRepository events,
            LeadRepository leads,
            TelecallerRepository telecallers
    ) {
        this.events = events;
        this.leads = leads;
        this.telecallers = telecallers;
    }

    private boolean isAdmin(Authentication authentication) {
        if (authentication == null
                || authentication.getAuthorities() == null) {
            return false;
        }

        return authentication.getAuthorities()
                .stream()
                .anyMatch(authority ->
                        "ROLE_SUPER_ADMIN".equals(authority.getAuthority())
                                || "ROLE_ADMIN".equals(authority.getAuthority())
                );
    }

    private String currentUsername(Authentication authentication) {
        if (authentication == null) {
            return "";
        }

        return authentication.getName();
    }

    public List<Map<String, Object>> getEvents(
            LocalDate from,
            LocalDate to,
            Authentication authentication
    ) {

        if (from == null || to == null) {
            throw new IllegalArgumentException(
                    "From date and to date are required"
            );
        }

        if (from.isAfter(to)) {
            throw new IllegalArgumentException(
                    "From date cannot be after to date"
            );
        }

        String username = currentUsername(authentication);
        boolean admin = isAdmin(authentication);

        List<Map<String, Object>> result = new ArrayList<>();

        // ======================================================
        // NORMAL CALENDAR EVENTS
        // ======================================================

        
               List<CalendarEvent> calendarEvents =
        events.findActiveEventsBetweenDates(
                from,
                to
        );

        for (CalendarEvent event : calendarEvents) {

            // Super Admin / Admin can see all events
            if (admin) {
                result.add(toEventMap(event));
                continue;
            }

            // Telecaller can see events assigned to everyone
            if (event.getAssignedTo() == null
                    || event.getAssignedTo().isEmpty()) {

                result.add(toEventMap(event));
                continue;
            }

            // Telecaller can see events specifically assigned to them
            boolean assigned = event.getAssignedTo()
                    .stream()
                    .filter(value -> value != null)
                    .anyMatch(value ->
                            value.equalsIgnoreCase(username)
                    );

            if (assigned) {
                result.add(toEventMap(event));
            }
        }

        // ======================================================
        // FOLLOW-UP EVENTS FROM LEADS
        // ======================================================

        List<Lead> leadList;

        if (admin) {
            leadList = leads.findAll();
        } else {
            leadList = leads.findByAssignedTo(username);
        }

        for (Lead lead : leadList) {

            LocalDate followupDate =
                    parseFollowupDate(
                            lead.getTcNextRefollowupDate()
                    );

            if (followupDate == null) {
                continue;
            }

            if (followupDate.isBefore(from)
                    || followupDate.isAfter(to)) {
                continue;
            }

            Map<String, Object> followup =
                    new LinkedHashMap<>();

            followup.put(
                    "id",
                    "followup-" + lead.getId()
            );

            followup.put(
                    "date",
                    followupDate.toString()
            );

            followup.put(
                    "type",
                    "FOLLOWUP"
            );

            followup.put(
                    "title",
                    "Follow-up: " + getLeadName(lead)
            );

            followup.put(
                    "leadId",
                    lead.getId()
            );

            followup.put(
                    "link",
                    null
            );

            followup.put(
                    "assignedTo",
                    lead.getAssignedTo()
            );

            // Follow-up is automatically generated from Lead.
            followup.put(
                    "readOnly",
                    true
            );

            result.add(followup);
        }

        result.sort(
                Comparator.comparing(
                        item -> String.valueOf(
                                item.get("date")
                        )
                )
        );

        return result;
    }

    private Map<String, Object> toEventMap(
            CalendarEvent event
    ) {

        Map<String, Object> map =
                new LinkedHashMap<>();

        map.put("id", event.getId());

        map.put(
                "date",
                event.getDate() != null
                        ? event.getDate().toString()
                        : null
        );

        map.put("title", event.getTitle());
        map.put("link", event.getLink());
        map.put("type", event.getType());
        map.put("assignedTo", event.getAssignedTo());
        map.put("createdBy", event.getCreatedBy());

        map.put(
                "readOnly",
                false
        );

        return map;
    }

    private LocalDate parseFollowupDate(String value) {

        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        String dateValue = value.trim();

        List<DateTimeFormatter> formatters =
                List.of(
                        DateTimeFormatter.ISO_LOCAL_DATE,
                        DateTimeFormatter.ofPattern("dd-MM-yyyy"),
                        DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                        DateTimeFormatter.ofPattern("yyyy/MM/dd")
                );

        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDate.parse(
                        dateValue,
                        formatter
                );
            } catch (Exception ignored) {
            }
        }

        return null;
    }

    private String getLeadName(Lead lead) {

        if (lead.getName() != null
                && !lead.getName().trim().isEmpty()) {
            return lead.getName();
        }

        if (lead.getSchool() != null
                && !lead.getSchool().trim().isEmpty()) {
            return lead.getSchool();
        }

        if (lead.getLeadId() != null
                && !lead.getLeadId().trim().isEmpty()) {
            return lead.getLeadId();
        }

        return "Lead";
    }

    public CalendarEvent create(
            CalendarEvent event,
            Authentication authentication
    ) {

        if (!isAdmin(authentication)) {
            throw new AccessDeniedException(
                    "Only Admin or Super Admin can create calendar events"
            );
        }

        if (event.getDate() == null) {
            throw new IllegalArgumentException(
                    "Date is required"
            );
        }

        if (event.getTitle() == null
                || event.getTitle().trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Title is required"
            );
        }

        event.setTitle(
                event.getTitle().trim()
        );

        if (event.getType() == null
                || event.getType().trim().isEmpty()) {

            event.setType("MEETING");

        } else {

            event.setType(
                    event.getType()
                            .trim()
                            .toUpperCase()
            );
        }

        event.setCreatedBy(
                currentUsername(authentication)
        );

        event.setCreatedAt(
                new Date()
        );

        event.setUpdatedAt(
                new Date()
        );

        event.setActive(true);

        return events.save(event);
    }

    public CalendarEvent update(
            String id,
            CalendarEvent data,
            Authentication authentication
    ) {

        if (!isAdmin(authentication)) {
            throw new AccessDeniedException(
                    "Only Admin or Super Admin can update calendar events"
            );
        }

        CalendarEvent event =
                events.findById(id)
                        .orElseThrow(
                                () -> new IllegalArgumentException(
                                        "Calendar event not found"
                                )
                        );

        if (data.getDate() != null) {
            event.setDate(data.getDate());
        }

        if (data.getTitle() != null
                && !data.getTitle().trim().isEmpty()) {

            event.setTitle(
                    data.getTitle().trim()
            );
        }

        if (data.getLink() != null) {
            event.setLink(
                    data.getLink()
            );
        }

        if (data.getType() != null
                && !data.getType().trim().isEmpty()) {

            event.setType(
                    data.getType()
                            .trim()
                            .toUpperCase()
            );
        }

        if (data.getAssignedTo() != null) {
            event.setAssignedTo(
                    data.getAssignedTo()
            );
        }

        event.setUpdatedAt(
                new Date()
        );

        return events.save(event);
    }

    public void delete(
            String id,
            Authentication authentication
    ) {

        if (!isAdmin(authentication)) {
            throw new AccessDeniedException(
                    "Only Admin or Super Admin can delete calendar events"
            );
        }

        CalendarEvent event =
                events.findById(id)
                        .orElseThrow(
                                () -> new IllegalArgumentException(
                                        "Calendar event not found"
                                )
                        );

        event.setActive(false);
        event.setUpdatedAt(new Date());

        events.save(event);
    }

    public List<Map<String, String>> telecallerOptions(
            Authentication authentication
    ) {

        if (!isAdmin(authentication)) {
            throw new AccessDeniedException(
                    "Only Admin or Super Admin can view telecallers"
            );
        }

        List<Map<String, String>> result =
                new ArrayList<>();

        telecallers.findAll()
                .stream()
                .filter(telecaller -> telecaller.isEnabled())
                .forEach(telecaller -> {

                    Map<String, String> item =
                            new LinkedHashMap<>();

                    item.put(
                            "username",
                            telecaller.getUsername()
                    );

                    item.put(
                            "fullName",
                            telecaller.getFullName()
                    );

                    item.put(
                            "employeeId",
                            telecaller.getEmployeeId()
                    );

                    result.add(item);
                });

        return result;
    }
}