// FILE: src/main/java/com/indocrm/indocrm/dto/ContactNumberDTO.java

package com.indocrm.indocrm.dto;

public class ContactNumberDTO {
    private String id;
    private String number;
    private String label;
    private String contactPerson;
    private boolean primary;
    
    public ContactNumberDTO() {}
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getNumber() { return number; }
    public void setNumber(String number) { this.number = number; }
    
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    
    public String getContactPerson() { return contactPerson; }
    public void setContactPerson(String contactPerson) { this.contactPerson = contactPerson; }
    
    public boolean isPrimary() { return primary; }
    public void setPrimary(boolean primary) { this.primary = primary; }
}
