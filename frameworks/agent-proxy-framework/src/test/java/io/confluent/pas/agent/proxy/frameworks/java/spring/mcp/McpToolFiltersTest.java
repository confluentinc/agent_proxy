package io.confluent.pas.agent.proxy.frameworks.java.spring.mcp;

import io.confluent.pas.agent.common.services.Schemas;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class McpToolFiltersTest {

    private McpToolFilters<?> mcpToolFilters;

    @BeforeEach
    public void setUp() {
        Schemas.Registration registration = new Schemas.Registration("testTool", "description", "requestTopic", "responseTopic");
        mcpToolFilters = new McpToolFilters<>(registration);
    }

    @Test
    public void testIsToolAllowed() {
        McpSchema.Tool allowedTool = new McpSchema.Tool("allowedTool", "description", "{}");
        McpSchema.Tool deniedTool = new McpSchema.Tool("testTool", "description", "{}");

        assertTrue(mcpToolFilters.isToolAllowed(allowedTool));
        assertFalse(mcpToolFilters.isToolAllowed(deniedTool));
    }

    @Test
    public void testDeny() {
        mcpToolFilters.deny("newDeniedTool");
        assertTrue(mcpToolFilters.getDeniedTools().contains("newDeniedTool"));
    }

    @Test
    public void testDenies() {
        mcpToolFilters.denies(List.of("deniedTool1", "deniedTool2"));
        assertTrue(mcpToolFilters.getDeniedTools().containsAll(List.of("deniedTool1", "deniedTool2")));
    }

    @Test
    public void testAllow() {
        mcpToolFilters.allow("newAllowedTool");
        assertTrue(mcpToolFilters.getAllowedTools().contains("newAllowedTool"));
    }

    @Test
    public void testAllows() {
        mcpToolFilters.allows(List.of("allowedTool1", "allowedTool2"));
        assertTrue(mcpToolFilters.getAllowedTools().containsAll(List.of("allowedTool1", "allowedTool2")));
    }
}