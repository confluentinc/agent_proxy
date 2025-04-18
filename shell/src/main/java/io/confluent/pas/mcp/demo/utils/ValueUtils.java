package io.confluent.pas.mcp.demo.utils;

import org.apache.commons.lang3.StringUtils;

public class ValueUtils {
    /**
     * Resolve the value of a parameter or environment variable
     *
     * @param value      The value of the parameter
     * @param envKeyName The name of the environment variable
     * @return The resolved value of the parameter or environment variable
     */
    public static String resolveValueOrEnv(String value, String envKeyName) {
        return StringUtils.isEmpty(value) ? System.getenv(envKeyName) : value;
    }
}
