// FILE: src/main/java/com/indocrm/indocrm/config/MongoConfig.java (NEW)
package com.indocrm.indocrm.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

/**
 * Explicit MongoClient/MongoTemplate configuration.
 *
 * Why this exists:
 *   Spring Boot's autoconfiguration will build a MongoTemplate
 *   eagerly and (with auto-index-creation=true, the default)
 *   will immediately try to talk to the database during
 *   context refresh. If that first handshake hits a transient
 *   network blip, the whole application fails to start with
 *   a misleading "Cannot resolve reference to bean
 *   'mongoTemplate'" error.
 *
 *   By declaring the MongoClient ourselves and disabling
 *   auto-index-creation in application.properties, we defer
 *   all real database traffic until the first request.
 *
 *   If Atlas is genuinely unreachable, you'll get a clean
 *   "Timed out while waiting for a server" error at first
 *   use instead of an opaque bootstrap failure.
 */
@Configuration
public class MongoConfig {

    private static final Logger log =
            LoggerFactory.getLogger(MongoConfig.class);

    @Value("${spring.data.mongodb.uri}")
    private String mongoUri;

    @Bean
    public MongoClient mongoClient() {
        ConnectionString connectionString =
                new ConnectionString(mongoUri);

        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(connectionString)
                // Fail fast on a bad Atlas URI. If this throws,
                // the exception will name the URI problem
                // instead of surfacing later as "Socket closed".
                .applyToClusterSettings(b ->
                        b.serverSelectionTimeout(
                                30, java.util.concurrent.TimeUnit.SECONDS))
                .build();

        log.info("Creating MongoClient for hosts {}",
                connectionString.getHosts());
        return MongoClients.create(settings);
    }

    @Bean
    public MongoTemplate mongoTemplate(MongoClient mongoClient) {
        // No database name in the URI -> fall back to "indocrm".
        ConnectionString cs = new ConnectionString(mongoUri);
        String dbName = cs.getDatabase();
        if (dbName == null || dbName.isBlank()) {
            dbName = "indocrm";
        }
        return new MongoTemplate(mongoClient, dbName);
    }
}