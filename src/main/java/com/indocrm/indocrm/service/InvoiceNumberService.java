package com.indocrm.indocrm.service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bson.Document;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import com.indocrm.indocrm.repository.InvoiceRepository;


@Service
public class InvoiceNumberService {

    public static final String PREFIX = "INV-IC/";
    public static final Pattern FORMAT = Pattern.compile("^INV-IC/(\\d{5,})$");

    private static final String COLLECTION = "counters";
    private static final String KEY = "invoice-number";
    private static final long FIRST_NUMBER = 760;

    private final MongoTemplate mongo;
    private final InvoiceRepository invoiceRepo;

    public InvoiceNumberService(MongoTemplate mongo, InvoiceRepository invoiceRepo) {
        this.mongo = mongo;
        this.invoiceRepo = invoiceRepo;
    }

    public String next() {
        seedIfMissing();
        Document doc = mongo.findAndModify(
                Query.query(Criteria.where("_id").is(KEY)),
                new Update().inc("seq", 1L),
                FindAndModifyOptions.options().returnNew(true),
                Document.class, COLLECTION);
        long seq = ((Number) doc.get("seq")).longValue();
        return String.format("%s%05d", PREFIX, seq);
    }

    /** First call only: continue after the highest existing INV-IC number (or start at 760). */
    private void seedIfMissing() {
        if (mongo.exists(Query.query(Criteria.where("_id").is(KEY)), COLLECTION)) return;

        long highest = FIRST_NUMBER - 1;
        for (var inv : invoiceRepo.findAll()) {
            if (inv.getInvoiceNumber() == null) continue;
            Matcher m = FORMAT.matcher(inv.getInvoiceNumber());
            if (m.matches()) highest = Math.max(highest, Long.parseLong(m.group(1)));
        }
        // setOnInsert => safe even if two requests seed at once
        mongo.findAndModify(
                Query.query(Criteria.where("_id").is(KEY)),
                new Update().setOnInsert("seq", highest),
                FindAndModifyOptions.options().upsert(true),
                Document.class, COLLECTION);
    }
}