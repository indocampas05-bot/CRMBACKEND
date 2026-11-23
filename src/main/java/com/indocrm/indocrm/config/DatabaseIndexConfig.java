package com.indocrm.indocrm.config;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;

import com.indocrm.indocrm.model.*;

@Configuration
public class DatabaseIndexConfig {

    private final MongoTemplate mongo;

    public DatabaseIndexConfig(MongoTemplate mongo) { this.mongo = mongo; }

    @PostConstruct
    public void ensureIndexes() {
        // Leads — most queried
        mongo.indexOps(Lead.class).ensureIndex(new Index().on("district", org.springframework.data.domain.Sort.Direction.ASC));
        mongo.indexOps(Lead.class).ensureIndex(new Index().on("assignedTo", org.springframework.data.domain.Sort.Direction.ASC));
        mongo.indexOps(Lead.class).ensureIndex(new Index().on("status", org.springframework.data.domain.Sort.Direction.ASC));
        mongo.indexOps(Lead.class).ensureIndex(new Index().on("mobile", org.springframework.data.domain.Sort.Direction.ASC));
        mongo.indexOps(Lead.class).ensureIndex(new Index().on("createdAt", org.springframework.data.domain.Sort.Direction.DESC));
        mongo.indexOps(Lead.class).ensureIndex(new Index().on("district", org.springframework.data.domain.Sort.Direction.ASC).on("assignedTo", org.springframework.data.domain.Sort.Direction.ASC));

        // Calls
        mongo.indexOps(Call.class).ensureIndex(new Index().on("agentUsername", org.springframework.data.domain.Sort.Direction.ASC));
        mongo.indexOps(Call.class).ensureIndex(new Index().on("leadId", org.springframework.data.domain.Sort.Direction.ASC));
        mongo.indexOps(Call.class).ensureIndex(new Index().on("startedAt", org.springframework.data.domain.Sort.Direction.DESC));
        mongo.indexOps(Call.class).ensureIndex(new Index().on("callStatus", org.springframework.data.domain.Sort.Direction.ASC));
        mongo.indexOps(Call.class).ensureIndex(new Index().on("agentUsername", org.springframework.data.domain.Sort.Direction.ASC).on("startedAt", org.springframework.data.domain.Sort.Direction.DESC));

        // Payments
        mongo.indexOps(Payment.class).ensureIndex(new Index().on("createdBy", org.springframework.data.domain.Sort.Direction.ASC));
        mongo.indexOps(Payment.class).ensureIndex(new Index().on("leadId", org.springframework.data.domain.Sort.Direction.ASC));
        mongo.indexOps(Payment.class).ensureIndex(new Index().on("status", org.springframework.data.domain.Sort.Direction.ASC));
        mongo.indexOps(Payment.class).ensureIndex(new Index().on("createdAt", org.springframework.data.domain.Sort.Direction.DESC));

        // Quotations
        mongo.indexOps(Quotation.class).ensureIndex(new Index().on("createdBy", org.springframework.data.domain.Sort.Direction.ASC));
        mongo.indexOps(Quotation.class).ensureIndex(new Index().on("status", org.springframework.data.domain.Sort.Direction.ASC));
        mongo.indexOps(Quotation.class).ensureIndex(new Index().on("createdAt", org.springframework.data.domain.Sort.Direction.DESC));

        // Agreements
        mongo.indexOps(Agreement.class).ensureIndex(new Index().on("createdBy", org.springframework.data.domain.Sort.Direction.ASC));
        mongo.indexOps(Agreement.class).ensureIndex(new Index().on("status", org.springframework.data.domain.Sort.Direction.ASC));
        mongo.indexOps(Agreement.class).ensureIndex(new Index().on("createdAt", org.springframework.data.domain.Sort.Direction.DESC));

        // Communications — VERY important, polled every 2s
        mongo.indexOps(CommunicationMessage.class).ensureIndex(new Index().on("recipientType", org.springframework.data.domain.Sort.Direction.ASC).on("createdAt", org.springframework.data.domain.Sort.Direction.ASC));
        mongo.indexOps(CommunicationMessage.class).ensureIndex(new Index().on("senderUsername", org.springframework.data.domain.Sort.Direction.ASC).on("recipientUsername", org.springframework.data.domain.Sort.Direction.ASC));
        mongo.indexOps(CommunicationMessage.class).ensureIndex(new Index().on("attachmentId", org.springframework.data.domain.Sort.Direction.ASC));

        // Attendance
        mongo.indexOps(AttendanceRecord.class).ensureIndex(new Index().on("date", org.springframework.data.domain.Sort.Direction.DESC));
        mongo.indexOps(AttendanceSession.class).ensureIndex(new Index().on("username", org.springframework.data.domain.Sort.Direction.ASC).on("connected", org.springframework.data.domain.Sort.Direction.DESC));

        // Telecallers
        mongo.indexOps(Telecaller.class).ensureIndex(new Index().on("districts", org.springframework.data.domain.Sort.Direction.ASC));
        mongo.indexOps(Telecaller.class).ensureIndex(new Index().on("assignedPhoneNumber", org.springframework.data.domain.Sort.Direction.ASC));

        // Leave/Permission
        mongo.indexOps(LeavePermissionRequest.class).ensureIndex(new Index().on("username", org.springframework.data.domain.Sort.Direction.ASC).on("submittedAt", org.springframework.data.domain.Sort.Direction.DESC));
        mongo.indexOps(LeavePermissionRequest.class).ensureIndex(new Index().on("status", org.springframework.data.domain.Sort.Direction.ASC));

        // Demo
        mongo.indexOps(Demo.class).ensureIndex(new Index().on("createdBy", org.springframework.data.domain.Sort.Direction.ASC).on("createdAt", org.springframework.data.domain.Sort.Direction.DESC));

        // Calendar
        mongo.indexOps(CalendarEvent.class).ensureIndex(new Index().on("date", org.springframework.data.domain.Sort.Direction.ASC).on("active", org.springframework.data.domain.Sort.Direction.ASC));
    }
}