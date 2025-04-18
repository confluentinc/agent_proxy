package io.confluent.pas.agent.proxy.rest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.confluent.pas.agent.common.services.Schemas;
import io.confluent.pas.agent.common.utils.JsonUtils;
import io.confluent.pas.agent.common.utils.UriTemplate;
import io.confluent.pas.agent.proxy.registration.RegistrationCoordinator;
import io.confluent.pas.agent.proxy.registration.RegistrationHandler;
import io.confluent.pas.agent.proxy.registration.schemas.RegistrationSchemas;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.http.HttpStatus;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Configuration class for OpenAPI documentation.
 * Responsible for generating OpenAPI paths based on registered handlers.
 */
@Slf4j
@Configuration
public class OpenAPIConfiguration {

    private static final String API_PATH_PREFIX = "/api/";
    private static final String RESOURCE_PATH_PREFIX = "/rcs/";
    private static final String PATH_PARAM_TYPE = "string";
    private static final String PATH_PARAM_LOCATION = "path";
    private static final String MEDIA_TYPE_JSON = "application/json";

    /**
     * JSON schema record representing OpenAPI schema properties
     */
    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    record JsonSchema(
            @JsonProperty("type") String type,
            @JsonProperty("properties") Map<String, Object> properties,
            @JsonProperty("required") List<String> required,
            @JsonProperty("additionalProperties") Boolean additionalProperties) {
    }

    private final RegistrationCoordinator registrationCoordinator;

    /**
     * Constructor for OpenAPIConfiguration.
     *
     * @param registrationCoordinator the registration coordinator
     */
    public OpenAPIConfiguration(RegistrationCoordinator registrationCoordinator) {
        this.registrationCoordinator = registrationCoordinator;
    }

    /**
     * Bean for OpenAPI configuration.
     *
     * @return the OpenAPI instance
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Confluent Tool API")
                        .version("1.0")
                        .license(new License().name("Apache 2.0").url("https://confluent.io")));
    }

    /**
     * Bean for customizing OpenAPI paths.
     *
     * @return the OpenApiCustomizer instance
     */
    @Bean
    public OpenApiCustomizer openApiCustomizer() {
        return openAPI -> openAPI
                .getPaths()
                .putAll(buildPathsFromRegistrations());
    }

    /**
     * Build the paths for the API based on registered handlers.
     *
     * @return a map of path strings to PathItem objects
     */
    private Map<String, PathItem> buildPathsFromRegistrations() {
        final List<RegistrationHandler<?, ?>> registrationHandlers = registrationCoordinator
                .getAllRegistrationHandlers();
        final Map<String, PathItem> pathItems = new HashMap<>();

        registrationHandlers.forEach(handler -> {
            try {
                final Schemas.Registration registration = handler.getRegistration();

                if (registration instanceof Schemas.ResourceRegistration resourceRegistration) {
                    addResourcePathItem(resourceRegistration, registration, pathItems);
                } else {
                    addStandardPathItem(registration, handler.getSchemas(), pathItems);
                }
            } catch (Exception e) {
                log.error("Error building OpenAPI path for handler: {}", handler.getClass().getSimpleName(), e);
            }
        });

        return pathItems;
    }

    /**
     * Add a standard (non-resource) path item to the path items map.
     *
     * @param registration the registration
     * @param schemas      the registration schemas
     * @param pathItems    the path items map to update
     */
    private void addStandardPathItem(Schemas.Registration registration,
            RegistrationSchemas schemas,
            Map<String, PathItem> pathItems) {
        final String path = registration.getName();
        final PathItem pathItem = new PathItem();

        try {
            final Content requestBody = createRequestBody(schemas.getRequestSchema().getSchema());
            final ApiResponse response = createApiResponse(schemas.getResponseSchema().getSchema());

            Operation operation = new Operation()
                    .summary(path)
                    .operationId(path)
                    .description(registration.getDescription())
                    .requestBody(new RequestBody().content(requestBody))
                    .responses(createSuccessResponse(response));

            pathItem.operation(PathItem.HttpMethod.POST, operation);
            pathItems.put(API_PATH_PREFIX + path, pathItem);
        } catch (JsonProcessingException e) {
            log.error("Error parsing schema for path: {}", path, e);
        }
    }

    /**
     * Adds a resource path to the path items map.
     *
     * @param resourceRegistration the resource registration
     * @param registration         the general registration
     * @param pathItems            the path items map to update
     */
    private void addResourcePathItem(Schemas.ResourceRegistration resourceRegistration,
            Schemas.Registration registration,
            Map<String, PathItem> pathItems) {
        final PathItem pathItem = new PathItem();
        final String urlPath = resourceRegistration.getUrl();

        final ApiResponse response = createApiResponse(resourceRegistration);
        Operation operation = new Operation()
                .summary(urlPath)
                .operationId(urlPath)
                .description(registration.getDescription())
                .responses(createSuccessResponse(response));

        pathItem.operation(PathItem.HttpMethod.GET, operation);

        // Process URI template parameters
        addPathParameters(pathItem, urlPath);

        pathItems.put(RESOURCE_PATH_PREFIX + urlPath, pathItem);
    }

    /**
     * Add path parameters to the path item based on the URI template.
     *
     * @param pathItem    the path item to update
     * @param uriTemplate the URI template string
     */
    private void addPathParameters(PathItem pathItem, String uriTemplate) {
        UriTemplate template = new UriTemplate(uriTemplate);
        template.getParts().stream()
                .filter(part -> part instanceof UriTemplate.TemplatePart)
                .map(part -> (UriTemplate.TemplatePart) part)
                .filter(part -> part.getNames() != null)
                .flatMap(part -> part.getNames().stream())
                .forEach(name -> pathItem.addParametersItem(createPathParameter(name)));
    }

    /**
     * Creates a path parameter with the given name.
     *
     * @param name the parameter name
     * @return the created Parameter
     */
    private Parameter createPathParameter(String name) {
        return new Parameter()
                .name(name)
                .in(PATH_PARAM_LOCATION)
                .required(true)
                .schema(new Schema<>().type(PATH_PARAM_TYPE));
    }

    /**
     * Creates a success response wrapper for an API response.
     *
     * @param response the API response
     * @return the ApiResponses object
     */
    private ApiResponses createSuccessResponse(ApiResponse response) {
        return new ApiResponses().addApiResponse(
                String.valueOf(HttpStatus.OK.value()),
                response);
    }

    /**
     * Creates a request body from the request schema.
     *
     * @param requestSchema the request schema JSON string
     * @return the content for the request body
     * @throws JsonProcessingException if the request schema cannot be parsed
     */
    private Content createRequestBody(String requestSchema) throws JsonProcessingException {
        final JsonSchema requestSchemaProperties = JsonUtils.toObject(requestSchema, JsonSchema.class);
        final Content requestBody = new Content();
        requestBody.addMediaType(MEDIA_TYPE_JSON,
                new MediaType().schema(createSchemaFromProperties(requestSchemaProperties)));
        return requestBody;
    }

    /**
     * Creates an API response for a resource registration.
     *
     * @param registration the resource registration
     * @return the API response
     */
    private ApiResponse createApiResponse(Schemas.ResourceRegistration registration) {
        final Content content = new Content();
        content.addMediaType(registration.getMimeType(), new MediaType());

        final ApiResponse response = new ApiResponse();
        response.content(content);
        response.description(HttpStatus.OK.getReasonPhrase());
        return response;
    }

    /**
     * Create an API response from the response schema.
     *
     * @param responseSchema the response schema JSON string
     * @return the API response
     * @throws JsonProcessingException if the response schema cannot be parsed
     */
    private ApiResponse createApiResponse(String responseSchema) throws JsonProcessingException {
        final JsonSchema responseSchemaProperties = JsonUtils.toObject(responseSchema, JsonSchema.class);

        final Content content = new Content()
                .addMediaType(MEDIA_TYPE_JSON, new MediaType()
                        .schema(createSchemaFromProperties(responseSchemaProperties)));

        return new ApiResponse()
                .content(content)
                .description(HttpStatus.OK.getReasonPhrase());
    }

    /**
     * Creates a schema from the schema properties.
     *
     * @param schemaProperties the schema properties
     * @return the schema
     */
    @SuppressWarnings("unchecked")
    @NotNull
    private static Schema<?> createSchemaFromProperties(JsonSchema schemaProperties) {
        Schema<?> schema = new Schema<>();
        schema.setType(schemaProperties.type);

        if (schemaProperties.required != null) {
            schema.setRequired(schemaProperties.required);
        }

        // Handle primitive types
        switch (schemaProperties.type) {
            case "string" -> schema.setType("string");
            case "integer" -> schema.setType("integer");
            case "boolean" -> schema.setType("boolean");
            case "array" -> schema.setType("array");
            default -> {
                if (schemaProperties.properties != null) {
                    schema.setProperties(schemaProperties.properties.entrySet().stream()
                            .collect(Collectors.toMap(
                                    Map.Entry::getKey,
                                    entry -> {
                                        final Map<String, Object> property = (Map<String, Object>) entry.getValue();
                                        final Schema<?> propertySchema = new Schema<>();
                                        final Set<String> types = getPropertyTypes(property);

                                        if (types.size() == 1) {
                                            propertySchema.setType(types.iterator().next());
                                        }

                                        processPropertyAttributes(property, propertySchema);
                                        return propertySchema;
                                    })));
                }

                if (schemaProperties.additionalProperties != null) {
                    schema.setAdditionalProperties(schemaProperties.additionalProperties);
                }
            }
        }

        return schema;
    }

    /**
     * Process additional attributes of a property schema.
     *
     * @param property the property map
     * @param schema   the schema to update
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static void processPropertyAttributes(Map<String, Object> property, Schema<?> schema) {
        // Process format if available
        if (property.containsKey("format")) {
            schema.setFormat((String) property.get("format"));
        }

        // Process enum if available
        if (property.containsKey("enum")) {
            // Use raw type to handle the generic type mismatch
            ((Schema) schema).setEnum((List) property.get("enum"));
        }

        // Process items for array type
        if (property.containsKey("items") && "array".equals(schema.getType())) {
            final Map<String, Object> items = (Map<String, Object>) property.get("items");
            final Schema<?> itemSchema = new Schema<>();

            if (items.containsKey("type")) {
                itemSchema.setType((String) items.get("type"));
            }

            schema.setItems(itemSchema);
        }

        // Process description if available
        if (property.containsKey("description")) {
            schema.setDescription((String) property.get("description"));
        }
    }

    /**
     * Extract property types from a property definition.
     *
     * @param property the property map
     * @return a set of property types
     */
    @SuppressWarnings("unchecked")
    private static Set<String> getPropertyTypes(Map<String, Object> property) {
        if (property.containsKey("type")) {
            final Object type = property.get("type");
            if (type instanceof String) {
                return Set.of((String) type);
            } else if (type instanceof List) {
                return new HashSet<>((List<String>) type);
            }
        }
        return Set.of("object");
    }
}