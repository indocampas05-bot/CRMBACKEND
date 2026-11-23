package com.indocrm.indocrm.controller;

import java.util.List;
import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.indocrm.indocrm.service.ReportService;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('TELECALLER')")
    public Map<String, Object> mine(
            @RequestParam(defaultValue = "MONTH") String period) {
        return reportService.myReport(period);
    }

    @GetMapping("/overall")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public Map<String, Object> overall(
            @RequestParam(defaultValue = "MONTH") String period) {
        return reportService.overallReport(period);
    }

    @GetMapping("/telecallers")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public List<Map<String, Object>> telecallers(
            @RequestParam(defaultValue = "MONTH") String period) {
        return reportService.telecallerReports(period);
    }

    @GetMapping("/telecallers/{username}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public Map<String, Object> telecaller(
            @PathVariable String username,
            @RequestParam(defaultValue = "MONTH") String period) {
        return reportService.telecallerReport(username, period);
    }
}
