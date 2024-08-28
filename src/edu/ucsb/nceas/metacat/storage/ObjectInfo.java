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
public record ObjectInfo (String pid, String cid, long size, Map<String, String> hexDigests) {

}
