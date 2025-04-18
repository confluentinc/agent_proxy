package io.confluent.pas.agent.common.utils;

import java.util.regex.Pattern;

public class UriUtils {

    private static final String URI_TEMPLATE_REGEX = "\\{([^}]+)}";
    private static final Pattern URI_TEMPLATE_PATTERN = Pattern.compile(URI_TEMPLATE_REGEX);

    /**
     * Check if a string is a URI template.
     *
     * @param str The string to check
     * @return true if the string is a URI template, false otherwise
     */
    public static boolean isTemplate(String str) {
        return URI_TEMPLATE_PATTERN.matcher(str).find();
    }

}
