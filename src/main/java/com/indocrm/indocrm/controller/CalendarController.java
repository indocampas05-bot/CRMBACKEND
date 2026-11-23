package com.indocrm.indocrm.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.indocrm.indocrm.model.CalendarEvent;
import com.indocrm.indocrm.service.CalendarService;

@RestController
@RequestMapping("/api/calendar")
public class CalendarController {

    private final CalendarService calendarService;

    public CalendarController(
            CalendarService calendarService
    ) {
        this.calendarService =
                calendarService;
    }

    @GetMapping
    public List<Map<String, Object>> getCalendarEvents(

            @RequestParam(required = false)
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE
            )
            LocalDate from,

            @RequestParam(required = false)
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE
            )
            LocalDate to,

            Authentication authentication
    ) {

        LocalDate today =
                LocalDate.now();

        if (from == null) {
            from =
                    today.withDayOfMonth(1);
        }

        if (to == null) {
            to =
                    from.withDayOfMonth(
                            from.lengthOfMonth()
                    );
        }

        if (from.isAfter(to)) {
            throw new IllegalArgumentException(
                    "From date cannot be after to date"
            );
        }

        return calendarService.getEvents(
                from,
                to,
                authentication
        );
    }

    @GetMapping("/events")
    public List<Map<String, Object>> getEvents(

            @RequestParam(required = false)
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE
            )
            LocalDate from,

            @RequestParam(required = false)
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE
            )
            LocalDate to,

            Authentication authentication
    ) {

        LocalDate today =
                LocalDate.now();

        if (from == null) {
            from =
                    today.withDayOfMonth(1);
        }

        if (to == null) {
            to =
                    from.withDayOfMonth(
                            from.lengthOfMonth()
                    );
        }

        return calendarService.getEvents(
                from,
                to,
                authentication
        );
    }

    @PostMapping
    public CalendarEvent createEvent(

            @RequestBody CalendarEvent event,

            Authentication authentication
    ) {

        return calendarService.create(
                event,
                authentication
        );
    }

    @PutMapping("/{id}")
    public CalendarEvent updateEvent(

            @PathVariable String id,

            @RequestBody CalendarEvent event,

            Authentication authentication
    ) {

        return calendarService.update(
                id,
                event,
                authentication
        );
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> deleteEvent(

            @PathVariable String id,

            Authentication authentication
    ) {

        calendarService.delete(
                id,
                authentication
        );

        return Map.of(
                "success",
                true,

                "message",
                "Calendar event deleted successfully"
        );
    }

    @GetMapping("/telecallers")
    public List<Map<String, String>>
    getTelecallers(
            Authentication authentication
    ) {

        return calendarService.telecallerOptions(
                authentication
        );
    }
}