/*package com.indocrm.indocrm.service;
import java.io.*; import java.nio.charset.StandardCharsets; import java.time.Instant; import org.springframework.boot.CommandLineRunner; import org.springframework.core.io.ClassPathResource; import org.springframework.stereotype.Component; import com.indocrm.indocrm.model.AgreementTemplate; import com.indocrm.indocrm.repository.AgreementTemplateRepository;
@Component public class AgreementTemplateBootstrap implements CommandLineRunner {
 private final AgreementTemplateRepository repo; public AgreementTemplateBootstrap(AgreementTemplateRepository repo){this.repo=repo;}
 @Override public void run(String... args)throws Exception{seed("Digital Marketing Agreement","agreement-templates/digital-marketing-agreement.html","agreement-templates/digital-marketing-agreement.pdf");seed("School ERP Agreement","agreement-templates/school-erp-agreement.html","agreement-templates/school-erp-agreement.pdf");}
 private void seed(String name,String htmlPath,String pdfPath)throws Exception{if(repo.existsByNameIgnoreCase(name))return; AgreementTemplate t=new AgreementTemplate();t.setName(name);t.setFileName(pdfPath.substring(pdfPath.lastIndexOf('/')+1));t.setContentType("application/pdf");t.setSourceType("PDF");t.setSystemTemplate(true);t.setActive(true);t.setCreatedBy("SYSTEM");t.setCreatedAt(Instant.now());t.setUpdatedAt(Instant.now());t.setContentHtml(readText(htmlPath));t.setOriginalFile(readBytes(pdfPath));repo.save(t);}
 private String readText(String p)throws IOException{try(InputStream in=new ClassPathResource(p).getInputStream()){return new String(in.readAllBytes(),StandardCharsets.UTF_8);}}
 private byte[] readBytes(String p)throws IOException{try(InputStream in=new ClassPathResource(p).getInputStream()){return in.readAllBytes();}}
}*/
