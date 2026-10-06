package edu.ucsb.nceas.metacat.restservice;

import org.dataone.service.exceptions.InvalidRequest;

import javax.servlet.http.HttpServletRequest;

/**
 * This class parses the byte range header in the https requests
 */
public class ByteRange {
    private static final String BYTES_INDICATOR = "bytes=";
    private static final String DELIMITER = "-";
    private final long start;
    private final Long end;

    /**
     * Constructor
     * @param start  the start of the byte range
     * @param end  the end of the byte range. Null means open-ended
     */
    public ByteRange(long start, Long end) {
        this.start = start;
        this.end = end;
    }

    /**
     * Get the start value of the range
     * @return the start value of the range
     */
    public long getStart() {
        return this.start;
    }

    /**
     * Get the end value of the range
     * @return the end value of the range. Null means open-ended.
     */
    public Long getEnd() {
        return this.end;
    }

    /**
     * Parse the http request to get the byte range
     * @param request  the request needs to be parsed
     * @return the byte range in the request. Null will be returned if there is no range specified
     * @throws InvalidRequest
     */
    public static ByteRange parseRange(HttpServletRequest request) throws InvalidRequest {
        String range = request.getHeader("Range");
        if (range == null || !range.startsWith(BYTES_INDICATOR)) {
            return null;
        }
        String spec = range.substring(BYTES_INDICATOR.length()).trim();
        if (spec.contains(",")) {
            throw new InvalidRequest("1010", "Multiple ranges not supported");
        }
        if (spec.startsWith(DELIMITER)) {
            // suffix ranges require total size → unsupported
            throw new InvalidRequest("1010", "Suffix range unsupported without size");
        }
        if (spec.endsWith(DELIMITER)) {
            long start = Long.parseLong(spec.substring(0, spec.length() - 1));
            return new ByteRange(start, null);
        }
        String[] parts = spec.split(DELIMITER, 2);
        long start = Long.parseLong(parts[0]);
        long end = Long.parseLong(parts[1]);
        if (start < 0 || end < start) {
            throw new InvalidRequest("1010", "Invalid range");
        }
        return new ByteRange(start, end);
    }
}
