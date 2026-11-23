// FILE: src/main/java/com/indocrm/indocrm/dto/CategoryStatsResponse.java
// NEW FILE for category statistics

package com.indocrm.indocrm.dto;

public class CategoryStatsResponse {
    private String category;
    private long count;

    public CategoryStatsResponse() {}

    public CategoryStatsResponse(String category, long count) {
        this.category = category;
        this.count = count;
    }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public long getCount() { return count; }
    public void setCount(long count) { this.count = count; }
}
