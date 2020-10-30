package edu.ucsb.nceas.metacat.common.index;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.dataone.service.types.v2.SystemMetadata;

public class IndexTask implements Serializable {
	
	private SystemMetadata systemMetadata;
	
	private Map<String, List<Object>> fields;
	
	private boolean isDeleteing = false; // default is not deleting task
	
	private long timeAddToQueque = 0; // the time when the task is added to the index queue
	
	/**
	 * Get the time when the task was added to the index queue
	 * @return  the time (long)
	 */
	public long getTimeAddToQueque() {
        return timeAddToQueque;
    }

	/**
	 * Set the time when the task was added to the queue
	 * @param timeAddToQueque  the time when the task is added to the index queue
	 */
    public void setTimeAddToQueque(long timeAddToQueque) {
        this.timeAddToQueque = timeAddToQueque;
    }

    public SystemMetadata getSystemMetadata() {
		return systemMetadata;
	}

	public void setSystemMetadata(SystemMetadata systemMetadata) {
		this.systemMetadata = systemMetadata;
	}

	public Map<String, List<Object>> getFields() {
		return fields;
	}

	public void setFields(Map<String, List<Object>> fields) {
		this.fields = fields;
	}
	
	/**
	 * Determine if this is a deleting index task.
	 * @return true if it is; otherwise false.
	 */
	public boolean isDeleting() {
	    return this.isDeleteing;
	}
	
	/**
	 * Set the index task to be a deleting task or not
	 * @param isDeleteing true if it is a deleting task; otherwise false.
	 */
	public void setIsDeleteing(boolean isDeleteing) {
	    this.isDeleteing = isDeleteing;
	}

}
