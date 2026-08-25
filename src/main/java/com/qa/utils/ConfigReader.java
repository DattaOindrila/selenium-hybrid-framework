package com.qa.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Loads config.properties once and exposes typed getters.
 *
 * Precedence, highest first:
 *   1. -D system property on the Maven command line  (mvn test -Dbrowser=firefox)
 *   2. environment variable, upper-snake-cased       (BROWSER=firefox)
 *   3. value in src/test/resources/config/config.properties
 *
 * The command-line override is what lets one build run headless Chrome in CI and
 * headed Firefox on a laptop without editing a file.
 */
public final class ConfigReader {

    private static final Logger log = LogManager.getLogger(ConfigReader.class);
    private static final String CONFIG_FILE = "config/config.properties";
    private static final Properties PROPERTIES = new Properties();

    static {
        try (InputStream in = ConfigReader.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (in == null) {
                throw new IllegalStateException(
                        CONFIG_FILE + " not found on the test classpath. "
                        + "It must live in src/test/resources/config/.");
            }
            PROPERTIES.load(in);
            log.info("Loaded {} ({} keys)", CONFIG_FILE, PROPERTIES.size());
        } catch (IOException e) {
            throw new IllegalStateException("Could not read " + CONFIG_FILE, e);
        }
    }

    private ConfigReader() {
    }

    /**
     * @throws IllegalStateException if the key is absent everywhere. Failing loudly
     *         beats returning null and producing a confusing NullPointerException
     *         three layers away.
     */
    public static String get(String key) {
        String value = resolve(key);
        if (value == null) {
            throw new IllegalStateException(
                    "Property '" + key + "' is not set. Add it to " + CONFIG_FILE
                    + " or pass -D" + key + "=<value>.");
        }
        return value.trim();
    }

    public static String get(String key, String defaultValue) {
        String value = resolve(key);
        return value == null ? defaultValue : value.trim();
    }

    public static int getInt(String key) {
        String raw = get(key);
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Property '" + key + "' must be a number but was '" + raw + "'", e);
        }
    }

    public static boolean getBoolean(String key) {
        return Boolean.parseBoolean(get(key));
    }

    private static String resolve(String key) {
        // 1. -Dkey=value
        String fromSystem = System.getProperty(key);
        if (isUsable(fromSystem)) {
            return fromSystem;
        }
        // 2. KEY_WITH_UNDERSCORES=value
        String fromEnv = System.getenv(key.toUpperCase().replace('.', '_'));
        if (isUsable(fromEnv)) {
            return fromEnv;
        }
        // 3. config.properties
        return PROPERTIES.getProperty(key);
    }

    /**
     * Surefire can pass through an unresolved Maven placeholder such as "${browser}"
     * when no profile supplied a value. Treat that as "not set" rather than as a
     * literal browser name.
     */
    private static boolean isUsable(String value) {
        return value != null && !value.isBlank() && !value.startsWith("${");
    }
}
