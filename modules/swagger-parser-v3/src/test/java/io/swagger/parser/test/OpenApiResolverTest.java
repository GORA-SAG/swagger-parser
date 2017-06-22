package io.swagger.parser.test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.swagger.oas.models.OpenAPI;
import io.swagger.oas.models.headers.Header;
import io.swagger.oas.models.media.AllOfSchema;
import io.swagger.oas.models.media.ArraySchema;
import io.swagger.oas.models.media.MediaType;
import io.swagger.oas.models.media.Schema;
import io.swagger.oas.models.parameters.RequestBody;
import io.swagger.oas.models.responses.ApiResponse;
import io.swagger.parser.models.AuthorizationValue;
import io.swagger.parser.models.SwaggerParseResult;
import io.swagger.parser.v3.OpenAPIResolver;
import io.swagger.parser.v3.util.OpenAPIDeserializer;
import io.swagger.oas.models.parameters.Parameter;
import mockit.Injectable;
import org.testng.Assert;

import org.testng.annotations.Test;

import java.nio.file.Files;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.testng.Assert.assertEquals;


public class OpenAPIResolverTest {

    @Test
    public void componentsResolver(@Injectable final List<AuthorizationValue> auths) throws Exception {
        final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        final JsonNode rootNode = mapper.readTree(Files.readAllBytes(java.nio.file.Paths.get(getClass().getResource("/oas3.yaml").toURI())));
        final OpenAPIDeserializer deserializer = new OpenAPIDeserializer();
        final SwaggerParseResult result = deserializer.deserialize(rootNode);

        Assert.assertNotNull(result);
        final OpenAPI openAPI = result.getOpenAPI();
        Assert.assertNotNull(openAPI);
        assertEquals(new OpenAPIResolver(openAPI, auths, null).resolve(), openAPI);

        Map<String, Schema> schemas = openAPI.getComponents().getSchemas();

        //internal url schema
        Schema pet = schemas.get("Pet");
        Schema category = (Schema) pet.getProperties().get("category");
        assertEquals(category,schemas.get("Category"));

        //remote url schema
        Schema user = (Schema) pet.getProperties().get("user");
        assertEquals(user.getType(),"object");
        Schema id = (Schema) user.getProperties().get("id");
        assertEquals(id.getType(),"integer");
        assertEquals(id.getFormat(),"int64");

        //ArraySchema items
        ArraySchema tagsProperty = (ArraySchema) pet.getProperties().get("tags");
        assertEquals(tagsProperty.getItems(), schemas.get("Tag"));
        assertEquals(tagsProperty.getType(),"array");

        //Schema not
        assertEquals(schemas.get("OrderRef").getNot(), schemas.get("Category"));

        //Schema additionalProperties
        assertEquals(schemas.get("OrderRef").getAdditionalProperties(), schemas.get("User"));

        //AllOfSchema
        AllOfSchema extended = (AllOfSchema) schemas.get("ExtendedErrorModel");
        Schema root = (Schema) extended.getAllOf().get(0).getProperties().get("rootCause");
        assertEquals(root, schemas.get("Category"));

        Map<String, ApiResponse> responses = openAPI.getComponents().getResponses();

        //remote url response
        ApiResponse notFound = responses.get("Found");
        assertEquals(notFound.getDescription(),"Remote Description");

        //internal url response schema
        MediaType generalError = responses.get("GeneralError").getContent().get("application/json");
        assertEquals(generalError.getSchema(),schemas.get("ExtendedErrorModel"));

        Map<String, RequestBody> requestBodies = openAPI.getComponents().getRequestBodies();

        //internal url requestBody schema
        RequestBody requestBody1 = requestBodies.get("requestBody1");
        MediaType xmlMedia = requestBody1.getContent().get("application/json");
        assertEquals(xmlMedia.getSchema(),schemas.get("Pet"));

        //internal url requestBody ArraySchema
        RequestBody requestBody2 = requestBodies.get("requestBody2");
        MediaType jsonMedia = requestBody2.getContent().get("application/json");
        ArraySchema items = (ArraySchema) jsonMedia.getSchema();
        assertEquals(items.getItems(),schemas.get("User"));

        assertEquals(requestBody2,requestBodies.get("requestBody3"));

        //internal Schema Parameter
        Map<String, Parameter> parameters = openAPI.getComponents().getParameters();
        assertEquals(parameters.get("newParam").getSchema(),schemas.get("Tag"));

        //internal Schema header
        Map<String, Header> headers = openAPI.getComponents().getHeaders();
        assertEquals(headers.get("X-Rate-Limit-Remaining").getSchema(),schemas.get("User"));

    }

    @Test
    public void pathsResolver(@Injectable final List<AuthorizationValue> auths) throws Exception {
        final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        final JsonNode rootNode = mapper.readTree(Files.readAllBytes(java.nio.file.Paths.get(getClass().getResource("/oas3.yaml").toURI())));
        final OpenAPIDeserializer deserializer = new OpenAPIDeserializer();
        final SwaggerParseResult result = deserializer.deserialize(rootNode);

        Assert.assertNotNull(result);
        final OpenAPI openAPI = result.getOpenAPI();
        Assert.assertNotNull(openAPI);
        assertEquals(new OpenAPIResolver(openAPI, auths, null).resolve(), openAPI);

        assertEquals(openAPI.getPaths().get("/pathItemRef2"),openAPI.getPaths().get("/pet"));

    }

}