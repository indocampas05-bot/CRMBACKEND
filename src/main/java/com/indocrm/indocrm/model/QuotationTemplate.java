package com.indocrm.indocrm.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "quotation_templates")
public class QuotationTemplate {
    private String id;
    private String name;
    private String originalFileName;
    private String originalContentType;
    private String storedOriginalPath;
    private String storedPreviewPath;
    // For DOC/DOCX sources we convert the uploaded template to PDF and use this path for final quotation rendering.
    private String storedRenderPdfPath;
    private int previewWidthPx;
    private int previewHeightPx;
    private double pageWidthPt;
    private double pageHeightPt;
    private boolean isPdfSource;
    private List<TemplateField> fields = new ArrayList<>();
    private TemplateDesign design = new TemplateDesign();
    private String createdBy;
    // "ERP" | "Digital Marketing" ... used by the New Quotation form to filter templates.
    private String serviceType;

    @CreatedDate @Field("createdAt") private Instant createdAt;
    @LastModifiedDate @Field("updatedAt") private Instant updatedAt;

    public String getStoredRenderPdfPath(){return storedRenderPdfPath;} public void setStoredRenderPdfPath(String v){storedRenderPdfPath=v;}

    public static class TemplateField {
        private String id;
        private String label;
        private String mapsTo;
        private double x, y, width, height;
        private double fontSize = 11;
        private String align = "left";
        private String fontFamily = "Helvetica";
        private String fontDataUrl = "";
        private String fontWeight = "normal";
        private String color = "#111827";
        private String backgroundColor = "transparent";
        private double padding = 0;
        private String verticalAlign = "top";
        private boolean bold;
        private boolean italic;
      private String defaultValue = "";

        public String getId(){return id;} public void setId(String v){id=v;}
        public String getLabel(){return label;} public void setLabel(String v){label=v;}
        public String getMapsTo(){return mapsTo;} public void setMapsTo(String v){mapsTo=v;}
        public double getX(){return x;} public void setX(double v){x=v;}
        public double getY(){return y;} public void setY(double v){y=v;}
        public double getWidth(){return width;} public void setWidth(double v){width=v;}
        public double getHeight(){return height;} public void setHeight(double v){height=v;}
        public double getFontSize(){return fontSize;} public void setFontSize(double v){fontSize=v;}
        public String getAlign(){return align;} public void setAlign(String v){align=v;}
        public String getFontFamily(){return fontFamily;} public void setFontFamily(String v){fontFamily=v;}
        public String getFontDataUrl(){return fontDataUrl;} public void setFontDataUrl(String v){fontDataUrl=v;}
        public String getFontWeight(){return fontWeight;} public void setFontWeight(String v){fontWeight=v;}
        public String getColor(){return color;} public void setColor(String v){color=v;}
        public String getBackgroundColor(){return backgroundColor;} public void setBackgroundColor(String v){backgroundColor=v;}
        public double getPadding(){return padding;} public void setPadding(double v){padding=v;}
        public String getVerticalAlign(){return verticalAlign;} public void setVerticalAlign(String v){verticalAlign=v;}
        public boolean isBold(){return bold;} public void setBold(boolean v){bold=v;}
        public boolean isItalic(){return italic;} public void setItalic(boolean v){italic=v;}
      public String getDefaultValue(){return defaultValue;} public void setDefaultValue(String v){defaultValue=v;}
    }

    public static class TemplateDesign {
        private String mode = "BUILDER"; // BUILDER | BACKGROUND
        private String pageSize = "A4";
        private String orientation = "PORTRAIT";
        private String backgroundColor = "#FFFFFF";
        private String primaryColor = "#7B1113";
        private String secondaryColor = "#172554";
        private String accentColor = "#DC2626";
        private String textColor = "#111827";
        private String headingColor = "#111827";
        private String fontFamily = "Helvetica";
        private String fontDataUrl = "";
        private String logoDataUrl = "";
        private String companyName = "Your Company Name";
        private String companySubtitle = "";
        private String companyAddress = "";
        private String companyPhone = "";
        private String companyEmail = "";
        private String companyWebsite = "";
        private String companyGstNumber = "";
        private boolean headerEnabled = true;
        private double headerHeight = 0.18;
        private String headerLayout = "logo-left";
        private boolean footerEnabled = true;
        private double footerHeight = 0.13;
        private String footerLayout = "contact-left";
        private double marginLeft = 0.08;
        private double marginRight = 0.08;
        private double marginTop = 0.05;
        private double marginBottom = 0.05;
        private List<DesignBlock> blocks = new ArrayList<>();
        private List<DesignPage> pages = new ArrayList<>();

        public String getMode(){return mode;} public void setMode(String v){mode=v;}
        public String getPageSize(){return pageSize;} public void setPageSize(String v){pageSize=v;}
        public String getOrientation(){return orientation;} public void setOrientation(String v){orientation=v;}
        public String getBackgroundColor(){return backgroundColor;} public void setBackgroundColor(String v){backgroundColor=v;}
        public String getPrimaryColor(){return primaryColor;} public void setPrimaryColor(String v){primaryColor=v;}
        public String getSecondaryColor(){return secondaryColor;} public void setSecondaryColor(String v){secondaryColor=v;}
        public String getAccentColor(){return accentColor;} public void setAccentColor(String v){accentColor=v;}
        public String getTextColor(){return textColor;} public void setTextColor(String v){textColor=v;}
        public String getHeadingColor(){return headingColor;} public void setHeadingColor(String v){headingColor=v;}
        public String getFontFamily(){return fontFamily;} public void setFontFamily(String v){fontFamily=v;}
        public String getFontDataUrl(){return fontDataUrl;} public void setFontDataUrl(String v){fontDataUrl=v;}
        public String getLogoDataUrl(){return logoDataUrl;} public void setLogoDataUrl(String v){logoDataUrl=v;}
        public String getCompanyName(){return companyName;} public void setCompanyName(String v){companyName=v;}
        public String getCompanySubtitle(){return companySubtitle;} public void setCompanySubtitle(String v){companySubtitle=v;}
        public String getCompanyAddress(){return companyAddress;} public void setCompanyAddress(String v){companyAddress=v;}
        public String getCompanyPhone(){return companyPhone;} public void setCompanyPhone(String v){companyPhone=v;}
        public String getCompanyEmail(){return companyEmail;} public void setCompanyEmail(String v){companyEmail=v;}
        public String getCompanyWebsite(){return companyWebsite;} public void setCompanyWebsite(String v){companyWebsite=v;}
        public String getCompanyGstNumber(){return companyGstNumber;} public void setCompanyGstNumber(String v){companyGstNumber=v;}
        public boolean isHeaderEnabled(){return headerEnabled;} public void setHeaderEnabled(boolean v){headerEnabled=v;}
        public double getHeaderHeight(){return headerHeight;} public void setHeaderHeight(double v){headerHeight=v;}
        public String getHeaderLayout(){return headerLayout;} public void setHeaderLayout(String v){headerLayout=v;}
        public boolean isFooterEnabled(){return footerEnabled;} public void setFooterEnabled(boolean v){footerEnabled=v;}
        public double getFooterHeight(){return footerHeight;} public void setFooterHeight(double v){footerHeight=v;}
        public String getFooterLayout(){return footerLayout;} public void setFooterLayout(String v){footerLayout=v;}
        public double getMarginLeft(){return marginLeft;} public void setMarginLeft(double v){marginLeft=v;}
        public double getMarginRight(){return marginRight;} public void setMarginRight(double v){marginRight=v;}
        public double getMarginTop(){return marginTop;} public void setMarginTop(double v){marginTop=v;}
        public double getMarginBottom(){return marginBottom;} public void setMarginBottom(double v){marginBottom=v;}
        public List<DesignBlock> getBlocks(){return blocks;} public void setBlocks(List<DesignBlock> v){blocks=v == null ? new ArrayList<>() : v;}
        public List<DesignPage> getPages(){return pages;} public void setPages(List<DesignPage> v){pages=v == null ? new ArrayList<>() : v;}
        
    }

    public static class DesignPage {
        private String id;
        private String name = "Page 1";
        private String backgroundColor = "#FFFFFF";
        private boolean headerEnabled = true;
        private double headerHeight = 0.16;
        private String headerLayout = "logo-left";
        private boolean footerEnabled = true;
        private double footerHeight = 0.12;
        private String footerLayout = "contact-left";
        private List<DesignBlock> blocks = new ArrayList<>();

        public String getId(){return id;} public void setId(String v){id=v;}
        public String getName(){return name;} public void setName(String v){name=v;}
        public String getBackgroundColor(){return backgroundColor;} public void setBackgroundColor(String v){backgroundColor=v;}
        public boolean isHeaderEnabled(){return headerEnabled;} public void setHeaderEnabled(boolean v){headerEnabled=v;}
        public double getHeaderHeight(){return headerHeight;} public void setHeaderHeight(double v){headerHeight=v;}
        public String getHeaderLayout(){return headerLayout;} public void setHeaderLayout(String v){headerLayout=v;}
        public boolean isFooterEnabled(){return footerEnabled;} public void setFooterEnabled(boolean v){footerEnabled=v;}
        public double getFooterHeight(){return footerHeight;} public void setFooterHeight(double v){footerHeight=v;}
        public String getFooterLayout(){return footerLayout;} public void setFooterLayout(String v){footerLayout=v;}
        public List<DesignBlock> getBlocks(){return blocks;} public void setBlocks(List<DesignBlock> v){blocks=v == null ? new ArrayList<>() : v;}
    }

    public static class DesignBlock {
        private String id;
        private String type = "text"; // text | heading | image | divider | table | signature | spacer
        private double x, y, width, height;
        private String text = "";
        private String mapsTo = "";
        private double fontSize = 11;
        private String fontFamily = "Helvetica";
        private String fontDataUrl = "";
        private String fontWeight = "normal";
        private String color = "#111827";
        private String backgroundColor = "transparent";
        private String align = "left";
        private String verticalAlign = "top";
        private double padding = 4;
        private double borderWidth = 0;
        private String borderColor = "#D1D5DB";
        private double borderRadius = 0;
        private boolean italic;
        private boolean underline;
        private boolean bold;
        private String imageDataUrl = "";
        private List<List<String>> tableRows = new ArrayList<>();

        public String getId(){return id;} public void setId(String v){id=v;}
        public String getType(){return type;} public void setType(String v){type=v;}
        public double getX(){return x;} public void setX(double v){x=v;}
        public double getY(){return y;} public void setY(double v){y=v;}
        public double getWidth(){return width;} public void setWidth(double v){width=v;}
        public double getHeight(){return height;} public void setHeight(double v){height=v;}
        public String getText(){return text;} public void setText(String v){text=v;}
        public String getMapsTo(){return mapsTo;} public void setMapsTo(String v){mapsTo=v;}
        public double getFontSize(){return fontSize;} public void setFontSize(double v){fontSize=v;}
        public String getFontFamily(){return fontFamily;} public void setFontFamily(String v){fontFamily=v;}
        public String getFontDataUrl(){return fontDataUrl;} public void setFontDataUrl(String v){fontDataUrl=v;}
        public String getFontWeight(){return fontWeight;} public void setFontWeight(String v){fontWeight=v;}
        public String getColor(){return color;} public void setColor(String v){color=v;}
        public String getBackgroundColor(){return backgroundColor;} public void setBackgroundColor(String v){backgroundColor=v;}
        public String getAlign(){return align;} public void setAlign(String v){align=v;}
        public String getVerticalAlign(){return verticalAlign;} public void setVerticalAlign(String v){verticalAlign=v;}
        public double getPadding(){return padding;} public void setPadding(double v){padding=v;}
        public double getBorderWidth(){return borderWidth;} public void setBorderWidth(double v){borderWidth=v;}
        public String getBorderColor(){return borderColor;} public void setBorderColor(String v){borderColor=v;}
        public double getBorderRadius(){return borderRadius;} public void setBorderRadius(double v){borderRadius=v;}
        public boolean isItalic(){return italic;} public void setItalic(boolean v){italic=v;}
        public boolean isUnderline(){return underline;} public void setUnderline(boolean v){underline=v;}
        public boolean isBold(){return bold;} public void setBold(boolean v){bold=v;}
        public String getImageDataUrl(){return imageDataUrl;} public void setImageDataUrl(String v){imageDataUrl=v;}
        public List<List<String>> getTableRows(){return tableRows;} public void setTableRows(List<List<String>> v){tableRows=v;}
    }

    public String getId(){return id;} public void setId(String v){id=v;}
    public String getName(){return name;} public void setName(String v){name=v;}
    public String getOriginalFileName(){return originalFileName;} public void setOriginalFileName(String v){originalFileName=v;}
    public String getOriginalContentType(){return originalContentType;} public void setOriginalContentType(String v){originalContentType=v;}
    public String getStoredOriginalPath(){return storedOriginalPath;} public void setStoredOriginalPath(String v){storedOriginalPath=v;}
    public String getStoredPreviewPath(){return storedPreviewPath;} public void setStoredPreviewPath(String v){storedPreviewPath=v;}
    public int getPreviewWidthPx(){return previewWidthPx;} public void setPreviewWidthPx(int v){previewWidthPx=v;}
    public int getPreviewHeightPx(){return previewHeightPx;} public void setPreviewHeightPx(int v){previewHeightPx=v;}
    public double getPageWidthPt(){return pageWidthPt;} public void setPageWidthPt(double v){pageWidthPt=v;}
    public double getPageHeightPt(){return pageHeightPt;} public void setPageHeightPt(double v){pageHeightPt=v;}
    public boolean isPdfSource(){return isPdfSource;} public void setPdfSource(boolean v){isPdfSource=v;}
    public List<TemplateField> getFields(){return fields;} public void setFields(List<TemplateField> v){fields=v;}
    public TemplateDesign getDesign(){return design;} public void setDesign(TemplateDesign v){design=v == null ? new TemplateDesign() : v;}
    public String getServiceType(){return serviceType;} public void setServiceType(String v){serviceType=v;}
    public String getCreatedBy(){return createdBy;} public void setCreatedBy(String v){createdBy=v;}
    public Instant getCreatedAt(){return createdAt;} public void setCreatedAt(Instant v){createdAt=v;}
    public Instant getUpdatedAt(){return updatedAt;} public void setUpdatedAt(Instant v){updatedAt=v;}
}