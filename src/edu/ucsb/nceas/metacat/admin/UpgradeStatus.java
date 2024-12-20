package edu.ucsb.nceas.metacat.admin;

/**
 * @author Tao
 * enum represents the update status
 */
public enum UpgradeStatus {

    PENDING(MetacatAdmin.PENDING), // Required, but hasn't started yet
    NOT_REQUIRED(MetacatAdmin.NOT_REQUIRED),
    IN_PROGRESS(MetacatAdmin.IN_PROGRESS),
    FAILED("failed"),
    UNKNOWN(MetacatAdmin.UNKNOWN),
    COMPLETE(MetacatAdmin.COMPLETE);

    private String value;
    private static final String FAILED_STR = "failed";
    private UpgradeStatus(String value) {
        this.value = value;
    }

    /**
     * Get the value of the enum
     * @return the string value of the enum
     */
    public String getValue() {
        return this.value;
    }

    /**
     * Get the UpgradeStatus object from the given string
     * @param status  the string value of the status
     * @return the UpgradeStatus object
     * @throws IllegalArgumentException
     */
    public static UpgradeStatus getStatus(String status) throws IllegalArgumentException {
        switch (status) {
            case MetacatAdmin.COMPLETE -> {
                return COMPLETE;
            }
            case FAILED_STR -> {
                return FAILED;
            }
            case MetacatAdmin.PENDING -> {
                return PENDING;
            }
            case MetacatAdmin.NOT_REQUIRED -> {
                return NOT_REQUIRED;
            }
            case MetacatAdmin.IN_PROGRESS -> {
                return IN_PROGRESS;
            }
            case MetacatAdmin.UNKNOWN -> {
                return UNKNOWN;
            }
            default -> throw new IllegalArgumentException(
                "UpdateStatus cannot recognize this status: " + status);
        }
    }
}
