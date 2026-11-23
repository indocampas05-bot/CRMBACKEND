package com.indocrm.indocrm.service;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.indocrm.indocrm.model.*;
import com.indocrm.indocrm.repository.*;

@Service
public class AttendanceService {
    public static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");
    private static final LocalTime PRESENT_FROM = LocalTime.of(9, 30);
    private static final LocalTime LATE_AFTER = LocalTime.of(10, 30);
    private static final LocalTime HALF_DAY_SWITCH = LocalTime.of(13, 30);
    private static final Duration HEARTBEAT_TIMEOUT = Duration.ofSeconds(50);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    private final AttendanceRepository attendanceRepository;
    private final AttendanceSessionRepository sessionRepository;
    private final AttendanceAuditLogRepository auditRepository;
    private final LeavePermissionRequestRepository requestRepository;
    private final TelecallerRepository telecallerRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public AttendanceService(AttendanceRepository attendanceRepository,
                             AttendanceSessionRepository sessionRepository,
                             AttendanceAuditLogRepository auditRepository,
                             LeavePermissionRequestRepository requestRepository,
                             TelecallerRepository telecallerRepository,
                             SimpMessagingTemplate messagingTemplate) {
        this.attendanceRepository = attendanceRepository;
        this.sessionRepository = sessionRepository;
        this.auditRepository = auditRepository;
        this.requestRepository = requestRepository;
        this.telecallerRepository = telecallerRepository;
        this.messagingTemplate = messagingTemplate;
    }

    public String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getName() == null) {
            throw new AccessDeniedException("Authentication required");
        }
        return auth.getName();
    }

    public boolean isTelecaller(Authentication auth) {
        return auth != null && auth.getAuthorities().stream().anyMatch(a ->
            "ROLE_TELECALLER".equalsIgnoreCase(a.getAuthority()) ||
            "ROLE_TELE_CALLER".equalsIgnoreCase(a.getAuthority()));
    }

    private Telecaller currentTelecaller() {
        return telecallerRepository.findByUsername(currentUsername())
            .orElseThrow(() -> new AccessDeniedException("Telecaller account not found"));
    }

    @Transactional
    public Map<String,Object> login(String username, String deviceInfo, String ipAddress) {
        Telecaller tc = telecallerRepository.findByUsername(username)
            .orElseThrow(() -> new AccessDeniedException("Telecaller account not found"));
        Instant now = Instant.now();
        String date = LocalDate.now(ZONE).format(DATE);
        AttendanceRecord attendance = findOrCreate(tc, date, now);
        if (attendance.getFirstLoginAt() == null) attendance.setFirstLoginAt(now);
        attendance.setUpdatedAt(now);

        // Every explicit successful login creates a new session. The daily
        // attendance record is never recreated because its unique key is
        // employeeId + date. Network recovery uses /reconnect instead.
        AttendanceSession session = new AttendanceSession();
        session.setEmployeeId(tc.getEmployeeId());
        session.setUsername(username);
        session.setAttendanceId(attendance.getId());
        session.setLoginAt(now);
        session.setLastHeartbeatAt(now);
        session.setLastActiveAt(now);
        session.setConnected(true);
        session.setManuallyLoggedOut(false);
        session.setDeviceInfo(deviceInfo);
        session.setIpAddress(ipAddress);
        sessionRepository.save(session);
        attendance.setSessionCount(attendance.getSessionCount() + 1);
        attendance.setCurrentSessionId(session.getId());
        attendance.setOnline(true);
        attendance.setDisconnected(false);
        refreshAttendance(attendance, now);
        attendanceRepository.save(attendance);
        publish(attendance);
        return dashboard(attendance, now);
    }

    @Transactional
    public Map<String,Object> manualLogout(String username) {
        Instant now = Instant.now();
        String date = LocalDate.now(ZONE).format(DATE);
        AttendanceRecord attendance = findByUsernameDate(username, date).orElse(null);
        if (attendance == null) return Map.of("serverNow", now.toString(), "status", "Not Marked");
        AttendanceSession session = sessionRepository
            .findFirstByUsernameAndConnectedTrueAndManuallyLoggedOutFalseOrderByLoginAtDesc(username)
            .orElse(null);
        if (session != null) {
            accumulateSession(session, now);
            session.setLogoutAt(now);
            session.setConnected(false);
            session.setManuallyLoggedOut(true);
            sessionRepository.save(session);
            attendance.setLastLogoutAt(now);
        }
        attendance.setOnline(false);
        attendance.setDisconnected(false);
        attendance.setCurrentSessionId(null);
        refreshAttendance(attendance, now);
        attendanceRepository.save(attendance);
        publish(attendance);
        return dashboard(attendance, now);
    }

    @Transactional
    public Map<String,Object> heartbeat(String username) {
        Instant now = Instant.now();
        AttendanceSession session = sessionRepository
            .findFirstByUsernameAndConnectedTrueAndManuallyLoggedOutFalseOrderByLoginAtDesc(username)
            .orElse(null);
        if (session == null) return reconnect(username, null, null, true);
        accumulateSession(session, now);
        session.setLastHeartbeatAt(now);
        session.setLastActiveAt(now);
        session.setConnected(true);
        sessionRepository.save(session);
        AttendanceRecord attendance = attendanceRepository.findById(session.getAttendanceId()).orElse(null);
        if (attendance == null) return Map.of("serverNow", now.toString(), "status", "Not Marked");
        attendance.setOnline(true);
        attendance.setDisconnected(false);
        refreshAttendance(attendance, now);
        attendanceRepository.save(attendance);
        publish(attendance);
        return dashboard(attendance, now);
    }

    @Transactional
    public Map<String,Object> reconnect(String username, String deviceInfo, String ipAddress, boolean fromHeartbeat) {
        Instant now = Instant.now();
        String date = LocalDate.now(ZONE).format(DATE);
        AttendanceRecord attendance = findByUsernameDate(username, date).orElse(null);
        if (attendance == null) {
            Telecaller tc = telecallerRepository.findByUsername(username)
                .orElseThrow(() -> new AccessDeniedException("Telecaller account not found"));
            attendance = findOrCreate(tc, date, now);
        }
        AttendanceSession session = sessionRepository
            .findByAttendanceIdOrderByLoginAtAsc(attendance.getId()).stream()
            .filter(s -> !s.isManuallyLoggedOut() && s.getLogoutAt() == null)
            .max(Comparator.comparing(AttendanceSession::getLoginAt))
            .orElse(null);
        if (session == null) {
            return login(username, deviceInfo, ipAddress);
        }
        if (session.getDisconnectAt() != null && (session.getReconnectAt() == null || session.getDisconnectAt().isAfter(session.getReconnectAt()))) { session.setDisconnectSeconds(session.getDisconnectSeconds() + Math.max(0, Duration.between(session.getDisconnectAt(), now).getSeconds())); }
        session.setConnected(true);
        session.setReconnectAt(now);
        session.setLastHeartbeatAt(now);
        session.setLastActiveAt(now);
        if (deviceInfo != null) session.setDeviceInfo(deviceInfo);
        if (ipAddress != null) session.setIpAddress(ipAddress);
        sessionRepository.save(session);
        attendance.setCurrentSessionId(session.getId());
        attendance.setOnline(true);
        attendance.setDisconnected(false);
        addNetworkEvent(attendance, "RECONNECT @ " + now);
        refreshAttendance(attendance, now);
        attendanceRepository.save(attendance);
        publish(attendance);
        return dashboard(attendance, now);
    }

    public Map<String,Object> today(String username) {
        Instant now = Instant.now();
        String date = LocalDate.now(ZONE).format(DATE);
        AttendanceRecord attendance = findByUsernameDate(username, date).orElse(null);
        if (attendance == null) return Map.of("serverNow", now.toString(), "date", date, "status", "Not Marked", "workingSeconds", 0L, "online", false, "sessions", List.of(), "audits", List.of());
        refreshAttendance(attendance, now);
        attendanceRepository.save(attendance);
        return dashboard(attendance, now);
    }

    public List<Map<String,Object>> mine(String username) {
        return attendanceRepository.findByEmployeeIdOrderByDateDesc(currentEmployeeId(username)).stream()
            .map(a -> dashboard(a, Instant.now())).collect(Collectors.toList());
    }

    public List<Map<String,Object>> admin(String date) {
        String targetDate = (date == null || date.isBlank()) ? LocalDate.now(ZONE).format(DATE) : date;
        Instant now = Instant.now();
        List<AttendanceRecord> records = attendanceRepository.findAllByDateOrderByFirstLoginAtAsc(targetDate);
        Map<String,AttendanceRecord> byEmployee = records.stream().collect(Collectors.toMap(AttendanceRecord::getEmployeeId, a -> a, (a,b)->a, LinkedHashMap::new));
        List<Map<String,Object>> result = new ArrayList<>();
        for (Telecaller tc : telecallerRepository.findAll()) {
            AttendanceRecord a = byEmployee.get(tc.getEmployeeId());
            if (a == null) {
                Map<String,Object> empty = new LinkedHashMap<>();
                empty.put("employeeId", tc.getEmployeeId()); empty.put("username", tc.getUsername());
                empty.put("fullName", tc.getFullName()); empty.put("date", targetDate); empty.put("status", emptyStatus(tc.getUsername(), targetDate, now));
                empty.put("firstLoginAt", null); empty.put("lastLogoutAt", null); empty.put("workingSeconds", 0L);
                empty.put("online", false); empty.put("sessionCount", 0); empty.put("sessions", List.of()); empty.put("permission", null); empty.put("leave", null);
                result.add(empty);
            } else {
                refreshAttendance(a, now); attendanceRepository.save(a); result.add(dashboard(a, now));
            }
        }
        return result;
    }

    public Map<String,Object> adminDetail(String id) {
        AttendanceRecord a = attendanceRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Attendance not found"));
        Instant now = Instant.now(); refreshAttendance(a, now); attendanceRepository.save(a);
        Map<String,Object> d = dashboard(a, now);
        d.put("audits", auditRepository.findByAttendanceIdOrderByChangedAtDesc(id));
        return d;
    }

    @Transactional
    public Map<String,Object> adminEdit(String id, Map<String,Object> body) {
        AttendanceRecord a = attendanceRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Attendance not found"));
        String admin = currentUsername();
        String reason = Objects.toString(body.get("reason"), "Super Admin attendance correction");
        applyAudit(a, "status", body.get("status"), reason, admin);
        applyAudit(a, "firstLoginAt", body.get("firstLoginAt"), reason, admin);
        applyAudit(a, "lastLogoutAt", body.get("lastLogoutAt"), reason, admin);
        applyAudit(a, "remarks", body.get("remarks"), reason, admin);
        applyAudit(a, "totalWorkingSeconds", body.get("workingSeconds"), reason, admin);
        if (body.containsKey("status")) { String requested=Objects.toString(body.get("status"), null); if ("AUTO".equalsIgnoreCase(requested)) { a.setManuallyOverridden(false); a.setManualOverrideStatus(null); } else { a.setManuallyOverridden(true); a.setManualOverrideStatus(requested); a.setStatus(requested); } }
        if (body.containsKey("firstLoginAt")) a.setFirstLoginAt(parseInstant(body.get("firstLoginAt")));
        if (body.containsKey("lastLogoutAt")) a.setLastLogoutAt(parseInstant(body.get("lastLogoutAt")));
        if (body.containsKey("remarks")) a.setRemarks(Objects.toString(body.get("remarks"), ""));
        if (body.containsKey("workingSeconds")) a.setTotalWorkingSeconds(Long.parseLong(String.valueOf(body.get("workingSeconds"))));
        a.setUpdatedAt(Instant.now()); attendanceRepository.save(a); publish(a);
        return adminDetail(a.getId());
    }

    public List<AttendanceAuditLog> audits(String id) { return auditRepository.findByAttendanceIdOrderByChangedAtDesc(id); }
    public List<AttendanceSession> sessions(String id) { return sessionRepository.findByAttendanceIdOrderByLoginAtAsc(id); }

    @Scheduled(fixedDelay = 15000)
    public void reconcile() {
        Instant now = Instant.now();
        for (AttendanceSession s : sessionRepository.findAll()) {
            if (s.isConnected() && !s.isManuallyLoggedOut() && s.getLastHeartbeatAt() != null && Duration.between(s.getLastHeartbeatAt(), now).compareTo(HEARTBEAT_TIMEOUT) > 0) {
                accumulateSession(s, now);
                s.setConnected(false); s.setDisconnectAt(now); sessionRepository.save(s);
                attendanceRepository.findById(s.getAttendanceId()).ifPresent(a -> {
                    a.setOnline(false); a.setDisconnected(true); addNetworkEvent(a, "DISCONNECT @ " + now); refreshAttendance(a, now); attendanceRepository.save(a); publish(a);
                });
            }
        }
        attendanceRepository.findAllByDateOrderByFirstLoginAtAsc(LocalDate.now(ZONE).format(DATE)).forEach(a -> { refreshAttendance(a, now); attendanceRepository.save(a); publish(a); });
    }

    private AttendanceRecord findOrCreate(Telecaller tc, String date, Instant now) {
        try {
            return attendanceRepository.findByEmployeeIdAndDate(tc.getEmployeeId(), date).orElseGet(() -> {
                AttendanceRecord a = new AttendanceRecord(); a.setEmployeeId(tc.getEmployeeId()); a.setUsername(tc.getUsername());
                a.setFullName(tc.getFullName()); a.setDate(date); a.setStatus("Present"); a.setCreatedAt(now); a.setUpdatedAt(now); return attendanceRepository.save(a);
            });
        } catch (DuplicateKeyException ex) {
            return attendanceRepository.findByEmployeeIdAndDate(tc.getEmployeeId(), date).orElseThrow();
        }
    }

    private Optional<AttendanceRecord> findByUsernameDate(String username, String date) {
        return telecallerRepository.findByUsername(username).flatMap(tc -> attendanceRepository.findByEmployeeIdAndDate(tc.getEmployeeId(), date));
    }

    private String currentEmployeeId(String username) {
        return telecallerRepository.findByUsername(username).map(Telecaller::getEmployeeId).orElseThrow(() -> new AccessDeniedException("Telecaller account not found"));
    }

    private void accumulateSession(AttendanceSession s, Instant now) {
        if (s.getLastActiveAt() != null && s.isConnected()) {
            long sec = Math.max(0, Duration.between(s.getLastActiveAt(), now).getSeconds());
            s.setActiveSeconds(s.getActiveSeconds() + sec);
        }
        s.setLastActiveAt(now);
    }

    private void refreshAttendance(AttendanceRecord a, Instant now) {
        if (a.isManuallyOverridden() && a.getManualOverrideStatus() != null) { a.setStatus(a.getManualOverrideStatus()); return; }
        List<LeavePermissionRequest> approved = requestRepository.findByUsernameOrderBySubmittedAtDesc(a.getUsername()).stream()
            .filter(r -> "Approved".equalsIgnoreCase(r.getStatus()) && appliesToDate(r, a.getDate())).toList();
        LeavePermissionRequest leave = approved.stream().filter(r -> "Leave".equalsIgnoreCase(r.getType()) && leaveActive(r, now)).findFirst().orElse(null);
        LeavePermissionRequest permission = approved.stream().filter(r -> "Permission".equalsIgnoreCase(r.getType()) && permissionActive(r, now)).findFirst().orElse(null);
        if (leave != null) a.setStatus("Leave");
        else if (permission != null) a.setStatus("Permission");
        else if (a.getFirstLoginAt() == null) a.setStatus("Absent");
        else {
            LocalTime loginTime = a.getFirstLoginAt().atZone(ZONE).toLocalTime();
            boolean coveredAtLogin = approved.stream().filter(r -> "Permission".equalsIgnoreCase(r.getType())).anyMatch(r -> permissionCovers(r, a.getFirstLoginAt()));
            boolean leaveCoveredAtLogin = approved.stream().filter(r -> "Leave".equalsIgnoreCase(r.getType())).anyMatch(r -> leaveCovers(r, a.getFirstLoginAt()));
            a.setStatus((coveredAtLogin || leaveCoveredAtLogin) ? "Present" : (loginTime.isAfter(LATE_AFTER) ? "Late" : "Present"));
        }
        long work = calculateWorkingSeconds(a, now, approved);
        a.setTotalWorkingSeconds(work);
        a.setUpdatedAt(now);
    }

    private long calculateWorkingSeconds(AttendanceRecord a, Instant now, List<LeavePermissionRequest> requests) {
        long total = 0;
        for (AttendanceSession s : sessionRepository.findByAttendanceIdOrderByLoginAtAsc(a.getId())) {
            if (s.getLoginAt() == null) continue;
            Instant end = s.getLogoutAt() != null ? s.getLogoutAt() : (s.isConnected() ? now : s.getDisconnectAt());
            if (end == null || end.isBefore(s.getLoginAt())) continue;
            // Use elapsed connected time, then remove approved permission/leave overlap.
            total += netWorkingSeconds(s.getLoginAt(), end, requests);
            total = Math.max(0, total - s.getDisconnectSeconds());
        }
        return Math.max(0, total);
    }

    private long netWorkingSeconds(Instant start, Instant end, List<LeavePermissionRequest> requests) {
        List<Interval> exclusions = new ArrayList<>();
        for (LeavePermissionRequest r : requests) {
            if ("Permission".equalsIgnoreCase(r.getType())) { Interval i = permissionInterval(r); if (i != null) exclusions.add(i); }
            if ("Leave".equalsIgnoreCase(r.getType())) { Interval i = leaveInterval(r); if (i != null) exclusions.add(i); }
        }
        List<Interval> merged = merge(exclusions);
        long excluded = 0;
        for (Interval i : merged) {
            Instant s = start.isAfter(i.start) ? start : i.start;
            Instant e = end.isBefore(i.end) ? end : i.end;
            if (e.isAfter(s)) excluded += Duration.between(s,e).getSeconds();
        }
        return Math.max(0, Duration.between(start,end).getSeconds() - excluded);
    }

    private List<Interval> merge(List<Interval> items) {
        items.sort(Comparator.comparing(x -> x.start)); List<Interval> out = new ArrayList<>();
        for (Interval i : items) { if (out.isEmpty() || i.start.isAfter(out.get(out.size()-1).end)) out.add(i); else if (i.end.isAfter(out.get(out.size()-1).end)) out.get(out.size()-1).end=i.end; }
        return out;
    }
    private static class Interval { Instant start,end; Interval(Instant s, Instant e){start=s;end=e;} }

    private String emptyStatus(String username, String date, Instant now) {
        List<LeavePermissionRequest> approved=requestRepository.findByUsernameOrderBySubmittedAtDesc(username).stream().filter(r->"Approved".equalsIgnoreCase(r.getStatus())&&appliesToDate(r,date)).toList();
        if (approved.stream().anyMatch(r->"Leave".equalsIgnoreCase(r.getType())&&leaveActive(r,now))) return "Leave";
        if (approved.stream().anyMatch(r->"Permission".equalsIgnoreCase(r.getType())&&permissionActive(r,now))) return "Permission";
        return "Absent";
    }

    private boolean appliesToDate(LeavePermissionRequest r, String date) {
        if ("Permission".equalsIgnoreCase(r.getType())) return date.equals(r.getDate());
        if (r.getFromDate()==null || r.getToDate()==null) return false;
        try {
            LocalDate attendanceDate = LocalDate.parse(date);
            LocalDate fromDate = LocalDate.parse(r.getFromDate());
            LocalDate toDate = LocalDate.parse(r.getToDate());
            return !attendanceDate.isBefore(fromDate) && !attendanceDate.isAfter(toDate);
        } catch (Exception e) {
            return false;
        }
    }
    private boolean permissionActive(LeavePermissionRequest r, Instant now) { Interval i=permissionInterval(r); return i!=null && !now.isBefore(i.start) && now.isBefore(i.end); }
    private boolean permissionCovers(LeavePermissionRequest r, Instant at) { Interval i=permissionInterval(r); return i!=null && !at.isBefore(i.start) && at.isBefore(i.end); }
    private Interval permissionInterval(LeavePermissionRequest r) { try { LocalDate d=LocalDate.parse(r.getDate()); return new Interval(d.atTime(LocalTime.parse(r.getFromTime())).atZone(ZONE).toInstant(), d.atTime(LocalTime.parse(r.getToTime())).atZone(ZONE).toInstant()); } catch(Exception e){return null;} }
    private boolean leaveActive(LeavePermissionRequest r, Instant now) { Interval i=leaveInterval(r); return i!=null && !now.isBefore(i.start) && now.isBefore(i.end); }
    private boolean leaveCovers(LeavePermissionRequest r, Instant at) { Interval i=leaveInterval(r); return i!=null && !at.isBefore(i.start) && at.isBefore(i.end); }
    private Interval leaveInterval(LeavePermissionRequest r) {
        try {
            LocalDate from=LocalDate.parse(r.getFromDate()), to=LocalDate.parse(r.getToDate());
            LocalTime st=LocalTime.MIDNIGHT, en=LocalTime.MAX;
            String type=(r.getLeaveType()==null?"":r.getLeaveType()).toLowerCase();
            if (type.contains("morning")) { st=LocalTime.MIDNIGHT; en=HALF_DAY_SWITCH; }
            else if (type.contains("afternoon")) { st=HALF_DAY_SWITCH; en=LocalTime.MAX; }
            else if (type.contains("half")) { st=HALF_DAY_SWITCH; en=LocalTime.MAX; }
            return new Interval(from.atTime(st).atZone(ZONE).toInstant(), to.atTime(en).atZone(ZONE).toInstant());
        } catch(Exception e){return null;}
    }

    private void applyAudit(AttendanceRecord a, String field, Object value, String reason, String admin) {
        if (value == null) return;
        String newValue = String.valueOf(value), oldValue = switch(field) {
            case "status" -> a.getStatus(); case "firstLoginAt" -> String.valueOf(a.getFirstLoginAt()); case "lastLogoutAt" -> String.valueOf(a.getLastLogoutAt());
            case "remarks" -> a.getRemarks(); case "totalWorkingSeconds" -> String.valueOf(a.getTotalWorkingSeconds()); default -> "";
        };
        if (Objects.equals(oldValue,newValue)) return;
        AttendanceAuditLog log=new AttendanceAuditLog(); log.setAttendanceId(a.getId()); log.setEmployeeId(a.getEmployeeId()); log.setUsername(a.getUsername()); log.setField(field); log.setPreviousValue(oldValue); log.setNewValue(newValue); log.setChangedBy(admin); log.setChangedAt(Instant.now()); log.setReason(reason); auditRepository.save(log);
    }

    private Instant parseInstant(Object value) { if(value==null) return null; return Instant.parse(String.valueOf(value)); }
    private void addNetworkEvent(AttendanceRecord a,String event){ if(a.getNetworkEvents()==null)a.setNetworkEvents(new ArrayList<>()); a.getNetworkEvents().add(event); if(a.getNetworkEvents().size()>100)a.setNetworkEvents(a.getNetworkEvents().subList(a.getNetworkEvents().size()-100,a.getNetworkEvents().size())); }

    private Map<String,Object> dashboard(AttendanceRecord a, Instant now) {
        Map<String,Object> m=new LinkedHashMap<>();
        m.put("id",a.getId());m.put("employeeId",a.getEmployeeId());m.put("username",a.getUsername());m.put("fullName",a.getFullName());m.put("date",a.getDate());m.put("status",a.getStatus());
        m.put("firstLoginAt",a.getFirstLoginAt());m.put("lastLogoutAt",a.getLastLogoutAt());m.put("workingSeconds",a.getTotalWorkingSeconds());m.put("online",a.isOnline());m.put("disconnected",a.isDisconnected());m.put("sessionCount",a.getSessionCount());m.put("remarks",a.getRemarks());m.put("serverNow",now);
        List<AttendanceSession> sessions=sessionRepository.findByAttendanceIdOrderByLoginAtAsc(a.getId()); m.put("sessions",sessions);m.put("networkEvents",a.getNetworkEvents());m.put("audits",auditRepository.findByAttendanceIdOrderByChangedAtDesc(a.getId()));
        List<LeavePermissionRequest> approved=requestRepository.findByUsernameOrderBySubmittedAtDesc(a.getUsername()).stream().filter(r->"Approved".equalsIgnoreCase(r.getStatus())&&appliesToDate(r,a.getDate())).toList();
        LeavePermissionRequest p=approved.stream().filter(r->"Permission".equalsIgnoreCase(r.getType())).findFirst().orElse(null); LeavePermissionRequest l=approved.stream().filter(r->"Leave".equalsIgnoreCase(r.getType())&&leaveActive(r,now)).findFirst().orElse(null);
        if(p!=null){m.put("permission", Map.of("id",p.getId(),"start",p.getDate()+"T"+p.getFromTime()+":00+05:30","end",p.getDate()+"T"+p.getToTime()+":00+05:30","status",permissionActive(p,now)?"Active":"Completed","duration",p.getDuration(),"reason",p.getReason()));}
        else m.put("permission",null);
        if(l!=null)m.put("leave",Map.of("id",l.getId(),"leaveType",l.getLeaveType()==null?"Full-day":l.getLeaveType(),"status","Active")); else m.put("leave",null);
        return m;
    }
    private void publish(AttendanceRecord a){ try { messagingTemplate.convertAndSend("/topic/attendance/"+a.getUsername(), dashboard(a,Instant.now())); messagingTemplate.convertAndSend("/topic/attendance/admin", dashboard(a,Instant.now())); } catch(Exception ignored){} }
}
