// BACKEND: src/main/java/com/indocrm/indocrm/model/ContactNumber.java

package com.indocrm.indocrm.model;

public class ContactNumber {
    private String id;
    private String number;
    private String label;
    private String contactPerson;
    private boolean isPrimary;
    
    public ContactNumber() {}
    
    public ContactNumber(String number, String label, String contactPerson) {
        this.number = number;
        this.label = label;
        this.contactPerson = contactPerson;
    }
    
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getNumber() { return number; }
    public void setNumber(String number) { this.number = number; }
    
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    
    public String getContactPerson() { return contactPerson; }
    public void setContactPerson(String contactPerson) { this.contactPerson = contactPerson; }
    
    public boolean isPrimary() { return isPrimary; }
    public void setPrimary(boolean primary) { isPrimary = primary; }
}
