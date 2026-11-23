package com.indocrm.indocrm.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.indocrm.indocrm.dto.DemoRequest;
import com.indocrm.indocrm.model.Demo;
import com.indocrm.indocrm.model.Lead;
import com.indocrm.indocrm.repository.DemoRepository;

@Service
public class DemoService {
    private final DemoRepository demoRepo;
    private final TelecallerAccessService access;
    private final GoogleMeetService googleMeet;

    public DemoService(DemoRepository demoRepo, TelecallerAccessService access, GoogleMeetService googleMeet){this.demoRepo=demoRepo;this.access=access;this.googleMeet=googleMeet;}

    public List<Demo> list(){ return demoRepo.findByCreatedByOrderByCreatedAtDesc(access.currentUsername()); }
    public Demo get(String id){ Demo d=demoRepo.findById(id).orElseThrow(()->new IllegalArgumentException("Demo not found")); checkOwner(d); return d; }

    public Demo create(DemoRequest r) throws Exception {
        String username=access.currentUsername(); Lead lead=access.requireLeadAccess(r.getLeadId());
        Demo d=new Demo(); d.setId("DM-"+UUID.randomUUID().toString().substring(0,8).toUpperCase()); d.setCreatedBy(username); apply(d,r,lead);
        d.setCreatedAt(Instant.now()); d.setUpdatedAt(Instant.now());
        List<Demo.TimelineEntry> timeline=new ArrayList<>(); timeline.add(new Demo.TimelineEntry("Demo Scheduled",Instant.now().toString(),"📅","#dbeafe","#1d4ed8"));
        if("meet".equalsIgnoreCase(d.getMeetType()) && (d.getMeetLink()==null || d.getMeetLink().isBlank())) { d.setMeetLink(googleMeet.createMeeting(username)); timeline.add(new Demo.TimelineEntry("Meet link generated",Instant.now().toString(),"🔗","#e0e7ff","#4338ca")); }
        d.setTimeline(timeline); return demoRepo.save(d);
    }

    public Demo update(String id, DemoRequest r) throws Exception { Demo d=get(id); Lead lead=access.requireLeadAccess(r.getLeadId()); apply(d,r,lead); d.setUpdatedAt(Instant.now()); if("meet".equalsIgnoreCase(d.getMeetType()) && (d.getMeetLink()==null||d.getMeetLink().isBlank())) d.setMeetLink(googleMeet.createMeeting(access.currentUsername())); return demoRepo.save(d); }
    public void delete(String id){ demoRepo.delete(get(id)); }
    public Demo cancel(String id){ Demo d=get(id); d.setStatus("Cancelled"); d.setUpdatedAt(Instant.now()); if(d.getTimeline()==null)d.setTimeline(new ArrayList<>()); d.getTimeline().add(new Demo.TimelineEntry("Demo Cancelled",Instant.now().toString(),"❌","#fee2e2","#dc2626")); return demoRepo.save(d); }
    private void checkOwner(Demo d){ if(!access.currentUsername().equals(d.getCreatedBy())) throw new org.springframework.security.access.AccessDeniedException("Demo access denied"); }
    private void apply(Demo d,DemoRequest r,Lead l){ d.setLeadId(l.getLeadId()); d.setOrganization(l.getSchool()!=null&&!l.getSchool().isBlank()?l.getSchool():l.getName()); d.setContactPerson(l.getName()); d.setEmail(l.getEmail()); d.setPhone(l.getMobile()!=null&&!l.getMobile().isBlank()?l.getMobile():l.getPhoneNumber()); d.setTopic(r.getTopic()); d.setDate(r.getDate()); d.setTime(r.getTime()); d.setDuration(r.getDuration()); d.setMeetType(r.getMeetType()==null?"meet":r.getMeetType()); d.setMeetLink(r.getMeetLink()); d.setStatus(r.getStatus()==null?"Scheduled":r.getStatus()); d.setNotes(r.getNotes()); }
}

