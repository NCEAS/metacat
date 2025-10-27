package edu.ucsb.nceas.metacat.systemmetadata.log;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.dataone.service.types.v1.AccessPolicy;
import org.dataone.service.types.v1.AccessRule;
import org.dataone.service.types.v1.Checksum;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v2.SystemMetadata;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * A class can log the delta change of the system metadata when the updateSystemMetadata method
 * is called.
 * @author Tao
 */
public class SystemMetadataDeltaLogger {
    private static final String ID = "identifier";
    private static final ObjectMapper mapper = new ObjectMapper()
        .enable(SerializationFeature.INDENT_OUTPUT);
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
     * Compare the oldSys and newSys and return the delta in the Json format.
     * The order changes don't count.
     * @param oldSys  the previous version of system metadata
     * @param newSys  the new version of system metadata
     * @return the delta change in the Json format
     */
    public static String compare(SystemMetadata oldSys, SystemMetadata newSys)
        throws JsonProcessingException, InvocationTargetException, IllegalAccessException {
        ObjectNode result = mapper.createObjectNode();
        result.put("timestamp", Instant.now().toString());
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

        // --- Special case: Checksum ---
        if (oldValue instanceof Checksum && newValue instanceof Checksum) {
            Checksum oldC = (Checksum) oldValue;
            Checksum newC = (Checksum) newValue;
            return Objects.equals(oldC.getAlgorithm(), newC.getAlgorithm()) &&
                Objects.equals(oldC.getValue(), newC.getValue());
        }

        // --- Special case: AccessPolicy ---
        if (oldValue instanceof AccessPolicy && newValue instanceof AccessPolicy) {
            return equalAccessPolicies((AccessPolicy) oldValue, (AccessPolicy) newValue);
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

    /**
     * Canonical equality for AccessPolicy: ignore order, compare subject+permission pairs
     */
    private static boolean equalAccessPolicies(AccessPolicy oldP, AccessPolicy newP) {
        List<AccessRule> oldRules = oldP.getAllowList();
        List<AccessRule> newRules = newP.getAllowList();

        if (oldRules == null && newRules == null) return true;
        if (oldRules == null || newRules == null) return false;

        Set<String> oldSet = new HashSet<>();
        Set<String> newSet = new HashSet<>();

        for (AccessRule r : oldRules) {
            for (Subject subject : r.getSubjectList()) {
                oldSet.add(subject + ":" + r.getPermissionList());
            }
        }

        for (AccessRule r : newRules) {
            for (Subject subject : r.getSubjectList()) {
                newSet.add(subject + ":" + r.getPermissionList());
            }
        }

        return oldSet.equals(newSet);
    }
}
