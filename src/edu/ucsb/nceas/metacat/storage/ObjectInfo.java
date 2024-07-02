package edu.ucsb.nceas.metacat.storage;

import java.util.Map;

/**
 * This is the Metacat version of the ObjectMetadata of Hashstore
 * ObjectMetadata is a class that models a unique identifier for an object in the HashStore. It
 * encapsulates information about a file's authority-based/persistent identifier (pid), content
 * identifier (cid), size, and associated hash digest values. By using ObjectMetadata objects,
 * client code can easily obtain metadata of a store object in HashStore without needing to know the
 * underlying file system details.
 */
public class ObjectInfo {
    private String pid = null;
    private String cid;
    private long size;
    private Map<String, String> hexDigests;

    /**
     * Creates a new instance of ObjectMetadata with the given properties.
     *
     * @param pid        Authority based or persistent identifer, null by default
     * @param cid        Unique identifier for the file
     * @param size       Size of stored file
     * @param hexDigests A map of hash algorithm names to their hex-encoded digest values for the
     *                   file
     */
    public ObjectInfo(String pid, String cid, long size, Map<String, String> hexDigests) {
        this.pid = pid;
        this.cid = cid;
        this.size = size;
        this.hexDigests = hexDigests;
    }

    /**
     * Get the persistent identifier
     * @return pid
     */
    public String getPid() {
        return pid;
    }

    /**
     * Set the persistent identifier
     */
    public void setPid(String pid) {
        this.pid = pid;
    }

    /**
     * Return the cid (content identifier)
     * @return cid
     */
    public String getCid() {
        return cid;
    }

    /**
     * Set the cid of the object
     * @param cid
     */
    public void setCid(String cid) {
        this.cid = cid;
    }

    /**
     * Return the size
     * @return size
     */
    public long getSize() {
        return size;
    }

    /**
     * Set the size of the object
     * @param size
     */
    public void setSize(long size) {
        this.size = size;
    }

    /**
     * Return a map of hex digests (checksums)
     * @return hexDigests
     */
    public Map<String, String> getHexDigests() {
        return hexDigests;
    }

    /**
     * Set the map of checksums for this object
     * @param hexDigests
     */
    public void setHexDigests(Map<String, String> hexDigests) {
        this.hexDigests = hexDigests;
    }

}
