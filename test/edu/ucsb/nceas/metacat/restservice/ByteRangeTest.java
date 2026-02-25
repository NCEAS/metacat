package edu.ucsb.nceas.metacat.restservice;

import org.apache.wicket.protocol.http.mock.MockHttpServletRequest;
import org.apache.wicket.protocol.http.mock.MockHttpSession;
import org.apache.wicket.protocol.http.mock.MockServletContext;
import org.dataone.service.exceptions.InvalidRequest;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test the class of ByteRange
 */
public class ByteRangeTest {
    private static final String RANGE = "Range";
    private static final String BYTES = "bytes";

    /**
     * Test the parseRange method
     * @throws Exception
     */
    @Test
    public void testParseRange() throws Exception {
        MockServletContext context = new MockServletContext(null, "/");
        MockHttpServletRequest request1 =
            new MockHttpServletRequest(null, new MockHttpSession(context), context);
        // Test a normal byte range
        request1.setHeader(RANGE, BYTES + "=1-500");
        ByteRange range1 = ByteRange.parseRange(request1);
        assertEquals(1, range1.getStart());
        assertEquals(500, range1.getEnd().longValue());
        MockHttpServletRequest request2 =
            new MockHttpServletRequest(null, new MockHttpSession(context), context);
        // Test a wrong byte range
        request2.setHeader(RANGE, BYTES + "=505-500");
        try {
            ByteRange.parseRange(request2);
            fail("Test can't get here");
        } catch (Exception e) {
            assertTrue(e instanceof InvalidRequest);
        }
        MockHttpServletRequest request3 =
            new MockHttpServletRequest(null, new MockHttpSession(context), context);
        // Test an omitted start
        request3.setHeader(RANGE, BYTES + "=-500");
        try {
            ByteRange.parseRange(request3);
            fail("Test can't get here");
        } catch (Exception e) {
            assertTrue(e instanceof InvalidRequest);
        }
        MockHttpServletRequest request4 =
            new MockHttpServletRequest(null, new MockHttpSession(context), context);
        // Test an opened-end
        request4.setHeader(RANGE, BYTES + "=100-");
        ByteRange range4 = ByteRange.parseRange(request4);
        assertEquals(100, range4.getStart());
        assertNull(range4.getEnd());
        MockHttpServletRequest request5 =
            new MockHttpServletRequest(null, new MockHttpSession(context), context);
        // Test multiple ranges
        request5.setHeader(RANGE, BYTES + "=100-500,600-700");
        try {
            ByteRange.parseRange(request5);
            fail("Test can't get here");
        } catch (Exception e) {
            assertTrue(e instanceof InvalidRequest);
        }
        MockHttpServletRequest request6 =
            new MockHttpServletRequest(null, new MockHttpSession(context), context);
        // Test no ranges
        assertNull(ByteRange.parseRange(request6));
        MockHttpServletRequest request7 =
            new MockHttpServletRequest(null, new MockHttpSession(context), context);
        // Test the header of range doesn't start with "bytes". It starts with "byte".
        request7.setHeader(RANGE, "byte=1-500");
        assertNull(ByteRange.parseRange(request7));
        MockHttpServletRequest request8 =
            new MockHttpServletRequest(null, new MockHttpSession(context), context);
        // Test the header of range to get the first byte
        request8.setHeader(RANGE, BYTES + "=0-0");
        ByteRange range8 = ByteRange.parseRange(request8);
        assertEquals(0, range8.getStart());
        assertEquals(0, range8.getEnd().longValue());
    }
}
