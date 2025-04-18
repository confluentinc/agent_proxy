package io.confluent.pas.agent.common.utils;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implements URI Template handling according to RFC 6570.
 * <p>
 * This class allows for matching URIs against templates to extract variables.
 * URI templates are strings with embedded expressions enclosed in curly braces,
 * such as:
 * {@code http://example.com/{username}/profile{?tab,section}}
 * <p>
 * The class parses templates into parts (literal strings and template
 * expressions),
 * creates regex patterns for matching, and extracts variable values from
 * matching URIs.
 */
public class UriTemplate {

    // Constants for security and performance limits
    private static final int MAX_TEMPLATE_LENGTH = 1_000_000;
    private static final int MAX_VARIABLE_LENGTH = 1_000_000;
    private static final int MAX_TEMPLATE_EXPRESSIONS = 10_000;
    private static final int MAX_REGEX_LENGTH = 1_000_000;

    // The original template string and parsed components
    @Getter
    private final String template;

    @Getter
    private final List<Part> parts;

    private final Pattern pattern;

    /**
     * Constructor to create a new UriTemplate instance.
     *
     * @param template The URI template string
     * @throws IllegalArgumentException if the template is invalid, too long, or
     *                                  contains too many expressions
     */
    public UriTemplate(String template) {
        validateLength(template, MAX_TEMPLATE_LENGTH, "Template");
        this.template = template;
        this.parts = Collections.unmodifiableList(parseTemplate(template));
        this.pattern = Pattern.compile(createMatchingPattern());
    }

    /**
     * Matches a URI against this template and extracts variable values.
     *
     * @param uri The URI to match
     * @return Map of variable names to extracted values, or null if the URI doesn't
     *         match
     * @throws IllegalArgumentException if the URI is too long
     */
    public Map<String, Object> match(String uri) {
        validateLength(uri, MAX_TEMPLATE_LENGTH, "URI");
        Matcher matcher = pattern.matcher(uri);

        if (!matcher.matches()) {
            return null;
        }

        return extractVariablesFromMatcher(matcher);
    }

    /**
     * Extracts variable values from a successful regex match.
     *
     * @param matcher The successful regex matcher
     * @return Map of variable names to their extracted values
     */
    private Map<String, Object> extractVariablesFromMatcher(Matcher matcher) {
        List<NameInfo> nameInfos = extractNamesFromParts();
        Map<String, Object> result = new HashMap<>();

        for (int i = 0; i < nameInfos.size(); i++) {
            NameInfo nameInfo = nameInfos.get(i);
            String value = matcher.group(i + 1);
            String cleanName = nameInfo.name().replace("*", "");

            if (nameInfo.exploded() && value.contains(",")) {
                // Handle exploded values (comma-separated lists)
                result.put(cleanName, List.of(value.split(",")));
            } else {
                result.put(cleanName, value);
            }
        }

        return result;
    }

    /**
     * Validates that a string does not exceed a maximum allowed length.
     *
     * @param str     String to validate
     * @param max     Maximum allowed length
     * @param context Context description for error message
     * @throws IllegalArgumentException if the string exceeds the maximum length
     */
    private static void validateLength(String str, int max, String context) {
        if (str.length() > max) {
            throw new IllegalArgumentException(
                    context + " exceeds maximum length of " + max + " characters (got " + str.length() + ")");
        }
    }

    /**
     * Parses a URI template into parts consisting of literal strings and template
     * parts.
     *
     * @param template The URI template to parse
     * @return List of parts (LiteralPart for literals, TemplatePart for
     *         expressions)
     * @throws IllegalArgumentException if the template syntax is invalid or
     *                                  contains too many expressions
     */
    private List<Part> parseTemplate(String template) {
        List<Part> parsedParts = new ArrayList<>();
        StringBuilder literal = new StringBuilder();
        int expressionCount = 0;

        for (int i = 0; i < template.length(); i++) {
            if (template.charAt(i) == '{') {
                // Add accumulated literal part if any
                if (!literal.isEmpty()) {
                    parsedParts.add(new LiteralPart(literal.toString()));
                    literal.setLength(0);
                }

                // Find closing brace
                int end = template.indexOf("}", i);
                if (end == -1) {
                    throw new IllegalArgumentException("Unclosed template expression");
                }

                // Check expression count limits
                expressionCount++;
                if (expressionCount > MAX_TEMPLATE_EXPRESSIONS) {
                    throw new IllegalArgumentException("Too many template expressions (max: "
                            + MAX_TEMPLATE_EXPRESSIONS + ")");
                }

                // Parse and add the template expression
                String expr = template.substring(i + 1, end);
                parsedParts.add(parseTemplatePart(expr));
                i = end;
            } else {
                literal.append(template.charAt(i));
            }
        }

        // Add final literal part if any
        if (!literal.isEmpty()) {
            parsedParts.add(new LiteralPart(literal.toString()));
        }

        return parsedParts;
    }

    /**
     * Parses a single template expression into a TemplatePart object.
     *
     * @param expr The template expression string without surrounding braces
     * @return A TemplatePart object representing the expression
     * @throws IllegalArgumentException if variable names are too long
     */
    private TemplatePart parseTemplatePart(String expr) {
        String operator = extractOperator(expr);
        String trimmedExpr = operator.isEmpty() ? expr : expr.substring(1);
        boolean exploded = trimmedExpr.contains("*");
        List<String> names = extractNames(trimmedExpr);

        // Validate variable names
        for (String name : names) {
            validateLength(name, MAX_VARIABLE_LENGTH, "Variable name");
        }

        return new TemplatePart(names.getFirst(), operator, names, exploded);
    }

    /**
     * Extracts the operator from a template expression if present.
     *
     * @param expr The template expression string
     * @return The operator as a string, or an empty string if none
     */
    private String extractOperator(String expr) {
        if (expr.isEmpty()) {
            return "";
        }

        return switch (expr.charAt(0)) {
            case '+', '#', '.', '/', '?', '&' -> String.valueOf(expr.charAt(0));
            default -> "";
        };
    }

    /**
     * Extracts variable names from a template expression.
     *
     * @param expr The template expression string (with operator removed if present)
     * @return A list of variable names
     */
    private List<String> extractNames(String expr) {
        String[] nameParts = expr.split(",");
        List<String> names = new ArrayList<>();

        for (String name : nameParts) {
            String trimmed = name.replace("*", "").trim();
            if (!trimmed.isEmpty()) {
                names.add(trimmed);
            }
        }

        return names;
    }

    /**
     * Constructs a regex pattern string to match URIs based on the template parts.
     *
     * @return A regex pattern string
     * @throws IllegalArgumentException if the generated pattern is too long
     */
    private String createMatchingPattern() {
        StringBuilder patternBuilder = new StringBuilder("^");

        for (Part part : parts) {
            if (part instanceof TemplatePart templatePart) {
                patternBuilder.append(createPatternForPart(templatePart));
            } else if (part instanceof LiteralPart literalPart) {
                patternBuilder.append(Pattern.quote(literalPart.getValue()));
            }
        }

        patternBuilder.append("$");
        String patternStr = patternBuilder.toString();

        validateLength(patternStr, MAX_REGEX_LENGTH, "Generated regex pattern");
        return patternStr;
    }

    /**
     * Creates a regex pattern for a specific template part based on its operator.
     *
     * @param part The template part
     * @return A regex pattern string
     */
    private String createPatternForPart(TemplatePart part) {
        return switch (part.getOperator()) {
            case "", "+" -> part.isExploded() ? "([^/]+(?:,[^/]+)*)" : "([^/,]+)";
            case "#" -> "(.+)";
            case "." -> "\\.([^/,]+)";
            case "/" -> "/" + (part.isExploded() ? "([^/]+(?:,[^/]+)*)" : "([^/,]+)");
            case "?", "&" -> "\\?" + part.getName() + "=([^&]+)";
            default -> "([^/]+)";
        };
    }

    /**
     * Extracts variable names from template parts.
     *
     * @return A list of NameInfo objects containing variable names and their
     *         properties
     */
    private List<NameInfo> extractNamesFromParts() {
        List<NameInfo> names = new ArrayList<>();

        for (Part part : parts) {
            if (part instanceof TemplatePart templatePart) {
                for (String name : templatePart.getNames()) {
                    names.add(new NameInfo(name, templatePart.isExploded()));
                }
            }
        }

        return names;
    }

    /**
     * Base class for all parts of a URI template.
     */
    public static abstract class Part {
        /**
         * Gets the string representation of this part for debugging.
         *
         * @return String representation
         */
        public abstract String getStringRepresentation();
    }

    /**
     * Represents a literal text part in a URI template.
     */
    public static final class LiteralPart extends Part {
        private final String value;

        /**
         * Creates a new literal part with the specified text.
         *
         * @param value The literal text
         */
        public LiteralPart(String value) {
            this.value = Objects.requireNonNull(value);
        }

        /**
         * Gets the literal text value.
         *
         * @return The literal text
         */
        public String getValue() {
            return value;
        }

        @Override
        public String getStringRepresentation() {
            return value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            LiteralPart that = (LiteralPart) o;
            return value.equals(that.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }
    }

    /**
     * Represents a template expression part in a URI template.
     */
    public static final class TemplatePart extends Part {
        @Getter
        private final String name;

        @Getter
        private final String operator;

        @Getter
        private final List<String> names;

        @Getter
        private final boolean exploded;

        /**
         * Creates a new template part.
         *
         * @param name     The primary variable name
         * @param operator The operator character (if any)
         * @param names    All variable names in this part
         * @param exploded Whether the variables are exploded (with asterisk)
         */
        public TemplatePart(String name, String operator, List<String> names, boolean exploded) {
            this.name = Objects.requireNonNull(name);
            this.operator = Objects.requireNonNull(operator);
            this.names = Collections.unmodifiableList(new ArrayList<>(names));
            this.exploded = exploded;
        }

        @Override
        public String getStringRepresentation() {
            StringBuilder sb = new StringBuilder("{");
            if (!operator.isEmpty()) {
                sb.append(operator);
            }

            for (int i = 0; i < names.size(); i++) {
                if (i > 0) {
                    sb.append(',');
                }
                sb.append(names.get(i));
                if (exploded) {
                    sb.append('*');
                }
            }

            sb.append('}');
            return sb.toString();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            TemplatePart that = (TemplatePart) o;
            return exploded == that.exploded &&
                    name.equals(that.name) &&
                    operator.equals(that.operator) &&
                    names.equals(that.names);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, operator, names, exploded);
        }
    }

    /**
     * Record class for storing variable name information during matching.
     */
    private record NameInfo(String name, boolean exploded) {
    }
}