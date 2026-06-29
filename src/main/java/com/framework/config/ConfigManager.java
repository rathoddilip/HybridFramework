package com.framework.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.Map;
import java.util.Objects;

/**
 * ConfigManager — reads environment-specific YAML config files.
 * Usage: ConfigManager.get("browser") or ConfigManager.get("api.baseUrl")
 *
 * Env is resolved from: -Denv=staging (Maven) or ENV system env variable.
 * Falls back to "dev" if not set.
 */
public class ConfigManager {

    private static final Logger log = LogManager.getLogger(ConfigManager.class);
    private static final Map<String, Object> config;

    static {
        String env = resolveEnv();
        log.info("Loading config for environment: [{}]", env);
        config = loadYaml("config/" + env + ".yaml");
        mergeLocalOverrides(config, "config/" + env + ".local.yaml");
        log.info("Config loaded successfully for env: [{}]", env);
    }

    private ConfigManager() {}

    /**
     * Resolve environment from system property → env variable → default "dev"
     */
    public static String resolveEnv() {
        String env = System.getProperty("env");
        if (env == null || env.isBlank()) {
            env = System.getenv("ENV");
        }
        return (env != null && !env.isBlank()) ? env.trim().toLowerCase() : "dev";
    }

    /**
     * Load a YAML file from classpath resources.
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> loadYaml(String filePath) {
        Yaml yaml = new Yaml();
        try (InputStream is = ConfigManager.class.getClassLoader().getResourceAsStream(filePath)) {
            Objects.requireNonNull(is, "Config file not found on classpath: " + filePath);
            return yaml.load(is);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load config file: " + filePath, e);
        }
    }

    @SuppressWarnings("unchecked")
    private static void mergeLocalOverrides(Map<String, Object> base, String localFilePath) {
        Yaml yaml = new Yaml();
        try (InputStream is = ConfigManager.class.getClassLoader().getResourceAsStream(localFilePath)) {
            if (is == null) {
                log.info("No local config override found: [{}]", localFilePath);
                return;
            }

            Map<String, Object> local = yaml.load(is);
            if (local != null) {
                deepMerge(base, local);
                log.info("Local config override loaded: [{}]", localFilePath);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load local config file: " + localFilePath, e);
        }
    }

    @SuppressWarnings("unchecked")
    private static void deepMerge(Map<String, Object> base, Map<String, Object> override) {
        for (Map.Entry<String, Object> entry : override.entrySet()) {
            Object baseValue = base.get(entry.getKey());
            Object overrideValue = entry.getValue();

            if (baseValue instanceof Map && overrideValue instanceof Map) {
                deepMerge((Map<String, Object>) baseValue, (Map<String, Object>) overrideValue);
            } else {
                base.put(entry.getKey(), overrideValue);
            }
        }
    }

    /**
     * Get a config value by dot-notation key.
     * Example: ConfigManager.get("api.baseUrl")
     */
    @SuppressWarnings("unchecked")
    public static String get(String key) {
        String systemOverride = System.getProperty(key);
        if (systemOverride != null && !systemOverride.isBlank()) {
            return systemOverride;
        }

        String envOverride = System.getenv(key.toUpperCase().replace(".", "_"));
        if (envOverride == null || envOverride.isBlank()) {
            envOverride = System.getenv(toEnvKey(key));
        }
        if (envOverride != null && !envOverride.isBlank()) {
            return envOverride;
        }

        String[] parts = key.split("\\.");
        Object current = config;

        for (String part : parts) {
            if (current instanceof Map) {
                current = ((Map<String, Object>) current).get(part);
            } else {
                throw new RuntimeException("Config key not found: " + key);
            }
        }

        if (current == null) {
            throw new RuntimeException("Config value is null for key: " + key);
        }
        return current.toString();
    }

    /**
     * Get a config value with a fallback default.
     */
    public static String get(String key, String defaultValue) {
        try {
            return get(key);
        } catch (Exception e) {
            log.warn("Config key [{}] not found, using default: [{}]", key, defaultValue);
            return defaultValue;
        }
    }

    /**
     * Get a boolean config value.
     */
    public static boolean getBoolean(String key) {
        return Boolean.parseBoolean(get(key));
    }

    /**
     * Get an integer config value.
     */
    public static int getInt(String key) {
        return Integer.parseInt(get(key));
    }

    private static String toEnvKey(String key) {
        return key.replaceAll("([a-z])([A-Z])", "$1_$2")
                .replace(".", "_")
                .toUpperCase();
    }
}
