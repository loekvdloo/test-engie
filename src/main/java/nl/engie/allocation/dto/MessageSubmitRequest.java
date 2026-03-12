package nl.engie.allocation.dto;

import java.util.Objects;

public class MessageSubmitRequest {
    private String xmlContent;
    private boolean manualEntry;
    private String eanCode;

    public MessageSubmitRequest() {
    }

    public MessageSubmitRequest(String xmlContent, boolean manualEntry, String eanCode) {
        this.xmlContent = xmlContent;
        this.manualEntry = manualEntry;
        this.eanCode = eanCode;
    }

    public String getXmlContent() { return xmlContent; }
    public void setXmlContent(String xmlContent) { this.xmlContent = xmlContent; }
    public boolean isManualEntry() { return manualEntry; }
    public void setManualEntry(boolean manualEntry) { this.manualEntry = manualEntry; }
    public String getEanCode() { return eanCode; }
    public void setEanCode(String eanCode) { this.eanCode = eanCode; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MessageSubmitRequest that = (MessageSubmitRequest) o;
        return manualEntry == that.manualEntry &&
                Objects.equals(xmlContent, that.xmlContent) &&
                Objects.equals(eanCode, that.eanCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(xmlContent, manualEntry, eanCode);
    }

    @Override
    public String toString() {
        return "MessageSubmitRequest(" +
                "xmlContent=" + xmlContent +
                ", manualEntry=" + manualEntry +
                ", eanCode=" + eanCode +
                ')';
    }
}
