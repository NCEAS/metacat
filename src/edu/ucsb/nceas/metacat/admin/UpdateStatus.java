package edu.ucsb.nceas.metacat.admin;

/**
 * @author Tao
 * enum of the update status
 */
public enum UpdateStatus {
    PENDING("pending"),
    NOT_REQUIRED("not required"),
    IN_PROGRESS("in progress"),
    FAILED("failed"),
    COMPLETE("complete");

    private String value;
    private UpdateStatus(String value) {
        this.value = value;
    }

    /**
     * Get the value of the enum
     * @return the string value of the enum
     */
    public String getValue() {
        return this.value;
    }
}
