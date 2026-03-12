package nl.engie.allocation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Objects;

public class MessageSubmitRequest {

    @NotBlank(message = "XML content mag niet leeg zijn")
    @Size(max = 2097152, message = "XML content mag niet groter zijn dan 2 MB")
    private String xmlContent;

    private boolean manualEntry;

    @Pattern(regexp = "^$|^\\d{13}(\\d{5})?$", message = "EAN-code moet 13 of 18 cijfers bevatten")
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
                "xmlContent=[" + (xmlContent != null ? xmlContent.length() + " chars" : "null") + "]" +
                ", manualEntry=" + manualEntry +
                ", eanCode=" + (eanCode != null ? "***" : "null") +
                ')';
    }
}
