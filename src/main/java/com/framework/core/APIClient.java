package com.framework.core;

import com.framework.config.ConfigManager;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.filter.log.LogDetail;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;

/**
 * APIClient — Base REST client wrapping RestAssured.
 *
 * Provides:
 *  - Environment-aware base URL
 *  - Authorization header management (Bearer / Basic / API Key)
 *  - Allure + Log4j request/response logging
 *  - JSON Schema validation
 *  - Fluent GET / POST / PUT / PATCH / DELETE
 *
 * Usage: extend this class for each service
 *   public class UserAPI extends APIClient { ... }
 */
public abstract class APIClient {

    private static final Logger log = LogManager.getLogger(APIClient.class);
    protected final String baseUrl;
    protected RequestSpecification requestSpec;
    protected ResponseSpecification responseSpec;

    protected APIClient() {
        this.baseUrl = ConfigManager.get("api.baseUrl");
        initSpecs();
        log.info("APIClient initialized with baseUrl: {}", baseUrl);
    }

    /**
     * Build base request/response specifications.
     * Override in subclass to add service-specific headers.
     */
    protected void initSpecs() {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        requestSpec = new RequestSpecBuilder()
                .setBaseUri(baseUrl)
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .addFilter(new AllureRestAssured()
                        .setRequestTemplate("http-request.ftl")
                        .setResponseTemplate("http-response.ftl"))
                .log(LogDetail.ALL)
                .build();

        responseSpec = new ResponseSpecBuilder()
                .log(LogDetail.ALL)
                .build();
    }

    // ── Auth Builders ─────────────────────────────────────────────────────────

    /** Set Bearer token on the current request spec */
    protected RequestSpecification withBearerToken(String token) {
        return given().spec(requestSpec).header("Authorization", "Bearer " + token);
    }

    /** Set Basic Auth */
    protected RequestSpecification withBasicAuth(String username, String password) {
        return given().spec(requestSpec).auth().basic(username, password);
    }

    /** Set API Key header */
    protected RequestSpecification withApiKey(String headerName, String apiKey) {
        return given().spec(requestSpec).header(headerName, apiKey);
    }

    /** No auth (public endpoints) */
    protected RequestSpecification noAuth() {
        return given().spec(requestSpec);
    }

    // ── HTTP Methods ──────────────────────────────────────────────────────────

    public Response get(String endpoint) {
        log.info("GET {}{}", baseUrl, endpoint);
        return noAuth()
                .when().get(endpoint)
                .then().spec(responseSpec)
                .extract().response();
    }

    public Response get(String endpoint, Map<String, ?> queryParams) {
        log.info("GET {}{} params={}", baseUrl, endpoint, queryParams);
        return noAuth()
                .queryParams(queryParams)
                .when().get(endpoint)
                .then().spec(responseSpec)
                .extract().response();
    }

    public Response get(String endpoint, String token) {
        log.info("GET (auth) {}{}", baseUrl, endpoint);
        return withBearerToken(token)
                .when().get(endpoint)
                .then().spec(responseSpec)
                .extract().response();
    }

    public Response post(String endpoint, Object body) {
        log.info("POST {}{}", baseUrl, endpoint);
        return noAuth()
                .body(body)
                .when().post(endpoint)
                .then().spec(responseSpec)
                .extract().response();
    }

    public Response post(String endpoint, Object body, String token) {
        log.info("POST (auth) {}{}", baseUrl, endpoint);
        return withBearerToken(token)
                .body(body)
                .when().post(endpoint)
                .then().spec(responseSpec)
                .extract().response();
    }

    public Response put(String endpoint, Object body, String token) {
        log.info("PUT (auth) {}{}", baseUrl, endpoint);
        return withBearerToken(token)
                .body(body)
                .when().put(endpoint)
                .then().spec(responseSpec)
                .extract().response();
    }

    public Response patch(String endpoint, Object body, String token) {
        log.info("PATCH (auth) {}{}", baseUrl, endpoint);
        return withBearerToken(token)
                .body(body)
                .when().patch(endpoint)
                .then().spec(responseSpec)
                .extract().response();
    }

    public Response delete(String endpoint, String token) {
        log.info("DELETE (auth) {}{}", baseUrl, endpoint);
        return withBearerToken(token)
                .when().delete(endpoint)
                .then().spec(responseSpec)
                .extract().response();
    }

    // ── Path Param Helpers ────────────────────────────────────────────────────

    public Response getById(String endpoint, Object id, String token) {
        log.info("GET by ID {}{}/{}", baseUrl, endpoint, id);
        return withBearerToken(token)
                .pathParam("id", id)
                .when().get(endpoint + "/{id}")
                .then().spec(responseSpec)
                .extract().response();
    }

    public Response deleteById(String endpoint, Object id, String token) {
        log.info("DELETE by ID {}{}/{}", baseUrl, endpoint, id);
        return withBearerToken(token)
                .pathParam("id", id)
                .when().delete(endpoint + "/{id}")
                .then().spec(responseSpec)
                .extract().response();
    }

    // ── Response Utilities ────────────────────────────────────────────────────

    /** Assert response matches a JSON Schema file in resources/schemas/ */
    public void validateSchema(Response response, String schemaFileName) {
        log.info("Validating JSON schema: {}", schemaFileName);
        response.then()
                .assertThat()
                .body(matchesJsonSchemaInClasspath("schemas/" + schemaFileName));
    }

    /** Assert status code */
    public void assertStatusCode(Response response, int expectedCode) {
        int actual = response.getStatusCode();
        if (actual != expectedCode) {
            throw new AssertionError(
                String.format("Expected status [%d] but got [%d]. Body: %s",
                        expectedCode, actual, response.getBody().asPrettyString())
            );
        }
        log.info("Status code assertion passed: {}", actual);
    }

    /** Extract a field value from JSON response */
    public <T> T extractField(Response response, String jsonPath) {
        return response.jsonPath().get(jsonPath);
    }

    /** Pretty-print response for debugging */
    public void logResponse(Response response) {
        log.debug("Response [{}]:\n{}", response.getStatusCode(), response.asPrettyString());
    }
}
