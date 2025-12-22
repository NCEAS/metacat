package edu.ucsb.nceas.metacat.systemmetadata.log;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.service.types.v1.AccessPolicy;
import org.dataone.service.types.v1.AccessRule;
import org.dataone.service.types.v1.Checksum;
import org.dataone.service.types.v1.NodeReference;
import org.dataone.service.types.v1.Replica;
import org.dataone.service.types.v1.ReplicationPolicy;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v2.MediaType;
import org.dataone.service.types.v2.MediaTypeProperty;
import org.dataone.service.types.v2.SystemMetadata;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * A class can log the delta change of the system metadata when the updateSystemMetadata method
 * is called.
 * @author Tao
 */
public class SystemMetadataDeltaLogger {
    private static final String UNKNOWN = "unknown";
    private static final String ID = "identifier";
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final int THREAD_POOL_SIZE = 2;
    private static Log logMetacat = LogFactory.getLog(SystemMetadataDeltaLogger.class);
    private static ExecutorService executorService =
        Executors.newFixedThreadPool(THREAD_POOL_SIZE, r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            t.setName("SystemMetadataDeltaLogger-" + t.getId());
            return t;
        });

    // Fields to ignore when comparing SystemMetadata
    private static final Set<String> EXCLUDED_FIELDS = new HashSet<>(Arrays.asList(
        "class",
        "serialVersionUID",
        "schemaLocation",
        "_fSchemaLocation",
        "scope",
        "xmlEncoding",
        "xmlVersion",
        "anyField"
    ));

    /**
     * Default constructor
     */
    public SystemMetadataDeltaLogger() {

    }

    /**
     * Log the system metadata difference in another thread only when it set the trace debug level
     * @param session  the session who requests the change
     * @param oldSys  the old version of the system metadata
     * @param newSys  the new version of the system metadata
     */
    public void log(final Session session, final SystemMetadata oldSys,
                              final SystemMetadata newSys) {
        log(
            (session != null && session.getSubject() != null) ? session.getSubject().getValue()
                                                              : UNKNOWN, oldSys, newSys);
    }

    /**
     * Log the system metadata difference in another thread only when it set the trace debug level
     * @param user  the user who requests the change
     * @param oldSys  the old version of the system metadata
     * @param newSys  the new version of the system metadata
     */
    public void log(String user, final SystemMetadata oldSys,
                    final SystemMetadata newSys) {
        if (!logMetacat.isTraceEnabled()) return;
        if (user == null || user.isBlank()) {
            user = UNKNOWN;
        }
        final String finalUser = user;
        try {
            Runnable logRunnable = new Runnable() {
                @Override
                public void run() {
                    if (logMetacat.isTraceEnabled()) {
                        try {
                            String difference = compare(finalUser, oldSys, newSys);
                            logMetacat.trace(difference);
                        } catch (Exception e) {
                            logMetacat.error("Could not log the system metadata delta since "
                                                 + e.getMessage());
                        }
                    }
                }
            };
            executorService.submit(logRunnable);
        } catch (Exception e) {
            logMetacat.error("Could not submit the system metadata delta log task into a "
                                 + "executor service since " + e.getMessage());
        }
    }

    /**
     * Compare the oldSys and newSys and return the delta in the Json format.
     * The order changes don't count.
     * @param oldSys  the previous version of system metadata
     * @param newSys  the new version of system metadata
     * @return the delta change in the Json format
     */
    protected static String compare(String user, SystemMetadata oldSys, SystemMetadata newSys)
        throws JsonProcessingException, InvocationTargetException, IllegalAccessException {
        ObjectNode result = mapper.createObjectNode();
        result.put("timestamp", Instant.now().toString());
        result.put("principal", user);
        // Case 1: creation
        if (oldSys == null && newSys != null) {
            result.put(ID, newSys.getIdentifier().getValue());
            result.put("changeType", "created");
            result.set("new", mapper.valueToTree(newSys));
            return mapper.writeValueAsString(result);
        }

        // Case 2: deletion
        if (oldSys != null && newSys == null) {
            result.put(ID, oldSys.getIdentifier().getValue());
            result.put("changeType", "deleted");
            result.set("old", mapper.valueToTree(oldSys));
            return mapper.writeValueAsString(result);
        }

        // Case 3: both null
        if (oldSys == null && newSys == null) {
            result.put("changeType", "none");
            result.put("message", "Both SystemMetadata objects are null");
            return mapper.writeValueAsString(result);
        }

        // Case 4: both non-null → compare
        if (!oldSys.getIdentifier().getValue().equals(newSys.getIdentifier().getValue())) {
            throw new IllegalArgumentException(
                "SystemMetadataDeltaLogger.compare - the old system metadata's identifier "
                    + oldSys.getIdentifier().getValue()
                    + " is different to the new system metadata's identifier "
                    + newSys.getIdentifier().getValue());
        }
        result.put(ID, oldSys.getIdentifier().getValue());
        ObjectNode deltas = mapper.createObjectNode();
        Method[] methods = SystemMetadata.class.getMethods();

        for (Method method : methods) {
            if (isGetter(method)) {
                String fieldName = getFieldName(method.getName());

                if (EXCLUDED_FIELDS.contains(fieldName)) {
                    continue; // skip unwanted fields
                }

                Object oldValue = method.invoke(oldSys);
                Object newValue = method.invoke(newSys);

                if (!areEqual(fieldName, oldValue, newValue)) {
                    ObjectNode delta = mapper.createObjectNode();
                    delta.putPOJO("old", oldValue);
                    delta.putPOJO("new", newValue);
                    deltas.set(fieldName, delta);
                }
            }
        }

        result.put("changeType", deltas.size() > 0 ? "modified" : "none");
        result.set("changes", deltas);
        return mapper.writeValueAsString(result);
    }

    /**
     * Check if a method is a standard getter
     */
    private static boolean isGetter(Method method) {
        if (!method.getName().startsWith("get")) return false;
        if (method.getParameterCount() != 0) return false;
        if (method.getReturnType().equals(void.class)) return false;
        return true;
    }

    /**
     *  Convert getter name to field name (e.g. getIdentifier -> identifier)
     */
    private static String getFieldName(String getterName) {
        String field = getterName.substring(3);
        return Character.toLowerCase(field.charAt(0)) + field.substring(1);
    }

    /**
     * Deep equality check ignoring order in collections
     */
    private static boolean areEqual(String fieldName, Object oldValue, Object newValue) {
        if (Objects.equals(oldValue, newValue)) return true;
        if (oldValue == null || newValue == null) return false;

        // Checksum
        if (oldValue instanceof Checksum oldC && newValue instanceof Checksum newC) {
            return Objects.equals(oldC.getAlgorithm(), newC.getAlgorithm()) &&
                Objects.equals(oldC.getValue(), newC.getValue());
        }

        // AccessPolicy
        if (oldValue instanceof AccessPolicy && newValue instanceof AccessPolicy) {
            return equalAccessPolicies((AccessPolicy) oldValue, (AccessPolicy) newValue);
        }

        // ReplicationPolicy
        if (oldValue instanceof ReplicationPolicy && newValue instanceof ReplicationPolicy) {
            return replicationPolicyEquals((ReplicationPolicy) oldValue, (ReplicationPolicy) newValue);
        }

        // Replica list
        if (oldValue instanceof List && newValue instanceof List && fieldName.equals("replicaList")) {
            return replicasEqual((List<Replica>) oldValue, (List<Replica>) newValue);
        }

        // MediaType
        if (oldValue instanceof MediaType && newValue instanceof MediaType) {
            return mediaTypeEquals((MediaType) oldValue, (MediaType) newValue);
        }

        // --- Lists (order-insensitive) ---
        if (oldValue instanceof List && newValue instanceof List) {
            return new HashSet<>((List<?>) oldValue).equals(new HashSet<>((List<?>) newValue));
        }

        // --- Sets (order-insensitive) ---
        if (oldValue instanceof Set && newValue instanceof Set) {
            return ((Set<?>) oldValue).equals((Set<?>) newValue);
        }

        // --- Arrays ---
        if (oldValue.getClass().isArray() && newValue.getClass().isArray()) {
            return Arrays.deepEquals((Object[]) oldValue, (Object[]) newValue);
        }

        // --- Fallback: canonical JSON comparison for complex beans ---
        try {
            String oldJson = mapper.writeValueAsString(oldValue);
            String newJson = mapper.writeValueAsString(newValue);
            return oldJson.equals(newJson);
        } catch (Exception e) {
            return Objects.equals(oldValue, newValue);
        }
    }

   // --- Custom comparators ---
    private static boolean equalAccessPolicies(AccessPolicy a, AccessPolicy b) {
        List<AccessRule> aList = a.getAllowList();
        List<AccessRule> bList = b.getAllowList();
        if (aList == null && bList == null) return true;
        if (aList == null || bList == null) return false;

        Set<String> setA = new HashSet<>();
        for (AccessRule r : aList) for (Subject subj : r.getSubjectList())
            setA.add(subj + ":" + r.getPermissionList());

        Set<String> setB = new HashSet<>();
        for (AccessRule r : bList) for (Subject subj : r.getSubjectList())
            setB.add(subj + ":" + r.getPermissionList());

        return setA.equals(setB);
    }

    private static boolean replicationPolicyEquals(ReplicationPolicy a, ReplicationPolicy b) {
        if (!Objects.equals(a.getReplicationAllowed(), b.getReplicationAllowed())) return false;
        if (!Objects.equals(a.getNumberReplicas(), b.getNumberReplicas())) return false;

        Set<String> preferredA = new HashSet<>();
        if (a.getPreferredMemberNodeList() != null)
            preferredA.addAll(a.getPreferredMemberNodeList().stream().map(NodeReference::getValue).toList());

        Set<String> preferredB = new HashSet<>();
        if (b.getPreferredMemberNodeList() != null)
            preferredB.addAll(b.getPreferredMemberNodeList().stream().map(NodeReference::getValue).toList());

        if (!preferredA.equals(preferredB)) return false;

        Set<String> blockedA = new HashSet<>();
        if (a.getBlockedMemberNodeList() != null)
            blockedA.addAll(a.getBlockedMemberNodeList().stream().map(NodeReference::getValue).toList());

        Set<String> blockedB = new HashSet<>();
        if (b.getBlockedMemberNodeList() != null)
            blockedB.addAll(b.getBlockedMemberNodeList().stream().map(NodeReference::getValue).toList());

        return blockedA.equals(blockedB);
    }

    private static boolean replicasEqual(List<Replica> aList, List<Replica> bList) {
        if (aList == bList) return true;
        if (aList == null || bList == null) return false;
        if (aList.size() != bList.size()) return false;

        Set<String> setA = aList.stream().map(
                r -> (r.getReplicaMemberNode() == null ? "null" :
                      r.getReplicaMemberNode().getValue())
                    + "|" + (r.getReplicationStatus() == null ? "null"
                                                              : r.getReplicationStatus().xmlValue())
                    + "|" + (r.getReplicaVerified() == null ? "null"
                                                            : r.getReplicaVerified().getTime()))
            .collect(Collectors.toSet());

        Set<String> setB = bList.stream().map(
                r -> (r.getReplicaMemberNode() == null ? "null" :
                      r.getReplicaMemberNode().getValue())
                    + "|" + (r.getReplicationStatus() == null ? "null"
                                                              : r.getReplicationStatus().xmlValue())
                    + "|" + (r.getReplicaVerified() == null ? "null"
                                                            : r.getReplicaVerified().getTime()))
            .collect(Collectors.toSet());

        return setA.equals(setB);
    }

    private static boolean mediaTypeEquals(MediaType a, MediaType b) {
        if (a == b) return true;
        if (a == null || b == null) return false;

        if (!Objects.equals(a.getName(), b.getName())) return false;

        List<MediaTypeProperty> listA =
            a.getPropertyList() == null ? List.of() : a.getPropertyList();
        List<MediaTypeProperty> listB =
            b.getPropertyList() == null ? List.of() : b.getPropertyList();

        Set<String> setA = listA.stream()
            .map(p -> p.getName() + ":" + p.getValue())
            .collect(Collectors.toSet());

        Set<String> setB = listB.stream()
            .map(p -> p.getName() + ":" + p.getValue())
            .collect(Collectors.toSet());

        return setA.equals(setB);
    }

    /**
     * Set a new Long instance. This is for test only.
     * @param newLogMetacat
     */
    static void setLog(Log newLogMetacat) {
        logMetacat = newLogMetacat;
    }

    /**
     * Set a new ExecutorService instance. This is for test only
     * @param newService
     */
    static void setExecutorService(ExecutorService newService) {
        executorService = newService;
    }
}
