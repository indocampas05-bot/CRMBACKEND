// FILE: src/main/java/com/indocrm/indocrm/service/AgreementCategoryBootstrap.java
// NEW FILE — seeds the 5 category templates on startup (idempotent; never overwrites hand-edited ones).
package com.indocrm.indocrm.service;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(20) // after AgreementTemplateBootstrapService (10)
public class AgreementCategoryBootstrap implements CommandLineRunner {

    private final AgreementCategoryService service;

    public AgreementCategoryBootstrap(AgreementCategoryService service) {
        this.service = service;
    }

    @Override
    public void run(String... args) {
        service.seedAll();
    }
}
