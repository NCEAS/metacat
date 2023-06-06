package edu.ucsb.nceas.metacat.properties;

import edu.ucsb.nceas.LeanTestUtils;
import edu.ucsb.nceas.utilities.PropertyNotFoundException;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.*;

public class PropertiesWrapperTest {

    private final Map<String, String> testEnvSecrets = new HashMap<>();
    private final Properties expectedSecrets = new Properties();
    private PropertiesWrapper propertiesWrapper;

    @Before
    public void setUp() throws Exception {

        final String[] secretEnvVarKeysList = {
            "METACAT_DATABASE_USER",
            "METACAT_DATABASE_PASSWORD",
            "METACAT_GUID_DOI_PASSWORD",
            "METACAT_AUTH_ADMINISTRATORS"
        };
        final String[] secretPropsKeysList = {
            "database.user",
            "database.password",
            "guid.doi.password",
            "auth.administrators"
        };
        assertEquals(secretEnvVarKeysList.length, secretPropsKeysList.length);

        for (int i = 0; i < secretEnvVarKeysList.length; i++) {
            String envKey=secretEnvVarKeysList[i];
            String propKey=secretPropsKeysList[i];
            String value = "TestValueFor_" + propKey;
            testEnvSecrets.put(envKey, value);
            expectedSecrets.setProperty(propKey, value);
        }
        // expected env secrets but which haven't been set - no default values in props file
        testEnvSecrets.put("METACAT_NOT_SET", " ");

        // empty env val, with fallback to existing property
        testEnvSecrets.put("METACAT_GUID_DOI_USERNAME", "");
        expectedSecrets.setProperty(
            "guid.doi.username",
            LeanTestUtils.getExpectedProperties().getProperty("guid.doi.username"));

        setEnv(testEnvSecrets);

        assertTrue(
            LeanTestUtils.initializePropertyService(LeanTestUtils.PropertiesMode.UNIT_TEST));

        propertiesWrapper = PropertiesWrapper.getInstance();
    }

    @Test
    public void checkEnvSecretsSet() {
        for (Map.Entry<String, String> entry : testEnvSecrets.entrySet()) {
            String envKey = entry.getKey();
            assertEquals(testEnvSecrets.get(envKey), System.getenv(envKey));
        }
    }

    @Test
    public void initializeSecretsFromEnvVars() {
        // props with expectedSecrets values
        expectedSecrets.forEach((key, value) -> {
            try {
                assertEquals("key: "+key+"; expected value: " + value, value,
                             propertiesWrapper.getProperty((String)key));
            } catch (PropertyNotFoundException e) {
                fail("unexpected exception " + e.getMessage());
            }
        });
    }

    @Test
    public void initializeSecretsFromEnvVars_fallback() {
        // expected env secrets but which haven't been set - should default to values in props file
        try {
            assertEquals(LeanTestUtils.getExpectedProperties().getProperty("guid.doi.username"),
                propertiesWrapper.getProperty("guid.doi.username"));
        } catch (PropertyNotFoundException e) {
            fail("unexpected exception " + e.getMessage());
        }
    }

    @Test
    public void initializeSecretsFromEnvVars_noFallback() {
        // expected env secrets but which haven't been set - no default values in props file
        // metacat.notSet=METACAT_NOT_SET
        try {
            assertEquals("", propertiesWrapper.getProperty("metacat.notSet"));
            fail("Expected a PropertyNotFoundException to be thrown, but it wasn't");
        } catch (PropertyNotFoundException e) {
            assertNotNull(e);
        }
    }


    // very difficult to mock or manipulate env vars in Java - hacking this for testing only. See:
    //   https://stackoverflow.com/questions/318239/how-do-i-set-environment-variables-from-java
    private void setEnv(Map<String, String> newenv) throws Exception {
        try {
            Class<?> envClass = Class.forName("java.lang.ProcessEnvironment");
            Field envField = envClass.getDeclaredField("theEnvironment");
            envField.setAccessible(true);
            Map<String, String> env = (Map<String, String>) envField.get(null);
            env.putAll(newenv);
            Field caseInsEnvField = envClass.getDeclaredField("theCaseInsensitiveEnvironment");
            caseInsEnvField.setAccessible(true);
            Map<String, String> cienv = (Map<String, String>) caseInsEnvField.get(null);
            cienv.putAll(newenv);
        } catch (NoSuchFieldException e) {
            Class[] classes = Collections.class.getDeclaredClasses();
            Map<String, String> env = System.getenv();
            for(Class cl : classes) {
                if("java.util.Collections$UnmodifiableMap".equals(cl.getName())) {
                    Field field = cl.getDeclaredField("m");
                    field.setAccessible(true);
                    Object obj = field.get(env);
                    Map<String, String> map = (Map<String, String>) obj;
                    map.clear();
                    map.putAll(newenv);
                }
            }
        }
    }
}
