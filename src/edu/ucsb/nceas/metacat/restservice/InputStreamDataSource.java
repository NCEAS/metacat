package edu.ucsb.nceas.metacat.restservice;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.activation.DataSource;

/**
 * Encapsulate an InputStream within a DataSource interface so that it is 
 * accessible to MIME processors.
 * 
 * @author Matthew Jones
 */
public class InputStreamDataSource implements DataSource {
    private String name;
    private InputStream stream;
    private boolean readOnce;
    
    public InputStreamDataSource(String name, InputStream stream) {
        super();
        this.name = name;
        this.stream = stream;
        this.readOnce = false;
    }

    public String getContentType() {
        return "application/octet-stream";
    }

    public InputStream getInputStream() throws IOException {
        if (readOnce) {
            throw new IOException("Only call getInputStream() once.");
        }
        readOnce = true;
        
        return stream;
    }

    public String getName() {
        return this.name;
    }

    public OutputStream getOutputStream() throws IOException {
        throw new IOException("Can't get an OutputStream from an InputStreamDataSource.");
    }
}
