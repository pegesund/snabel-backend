package no.snabel.integration;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Integration test for OAuth2 Client Credentials Flow
 * Tests the complete flow against a RUNNING dev server:
 * 1. Admin login
 * 2. Create API client
 * 3. Get OAuth2 token using client credentials
 * 4. Use token to call protected API endpoints
 *
 * NOTE: This test requires the dev server to be running on port 8080
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class OAuth2ClientCredentialsFlowTest {

    private static final String BASE_URL = "http://localhost:8080";
    private static String adminToken;
    private static String clientId;
    private static String clientSecret;
    private static String apiToken;

    @BeforeAll
    public static void setup() {
        RestAssured.baseURI = BASE_URL;
        System.out.println("============================================");
        System.out.println("OAuth2 Client Credentials Flow Test");
        System.out.println("Testing against: " + BASE_URL);
        System.out.println("============================================\n");
    }

    @Test
    @Order(1)
    @DisplayName("1. Admin login")
    public void testAdminLogin() {
        System.out.println("Test 1: Admin login...");

        Response response = given()
                .contentType(ContentType.JSON)
                .body("{ \"username\": \"snabel\", \"password\": \"snabeltann\" }")
                .when()
                .post("/api/auth/login")
                .then()
                .statusCode(200)
                .body("token", notNullValue())
                .body("username", equalTo("snabel"))
                .body("role", equalTo("ADMIN"))
                .extract()
                .response();

        adminToken = response.path("token");
        System.out.println("  ✓ Admin logged in successfully");
        System.out.println("  Username: " + response.path("username"));
        System.out.println("  Role: " + response.path("role"));
        System.out.println("  Token: " + adminToken.substring(0, 50) + "...\n");
    }

    @Test
    @Order(2)
    @DisplayName("2. Create API client")
    public void testCreateApiClient() {
        System.out.println("Test 2: Creating API client...");

        Response response = given()
                .header("Authorization", "Bearer " + adminToken)
                .contentType(ContentType.JSON)
                .body("{ \"name\": \"Test Integration Client\", " +
                      "\"description\": \"Client for integration testing\", " +
                      "\"scopes\": \"read:invoices,write:invoices\" }")
                .when()
                .post("/api/clients")
                .then()
                .statusCode(201)
                .body("clientId", notNullValue())
                .body("clientSecret", notNullValue())
                .body("name", equalTo("Test Integration Client"))
                .extract()
                .response();

        clientId = response.path("clientId");
        clientSecret = response.path("clientSecret");

        System.out.println("  ✓ API client created successfully");
        System.out.println("  Client ID: " + clientId);
        System.out.println("  Client Secret: " + clientSecret.substring(0, 20) + "...\n");
    }

    @Test
    @Order(3)
    @DisplayName("3. Get OAuth2 token using client credentials")
    public void testGetOAuth2Token() {
        System.out.println("Test 3: Getting OAuth2 token...");

        Response response = given()
                .contentType("application/x-www-form-urlencoded")
                .formParam("grant_type", "client_credentials")
                .formParam("client_id", clientId)
                .formParam("client_secret", clientSecret)
                .when()
                .post("/api/auth/token")
                .then()
                .statusCode(200)
                .body("token", notNullValue())
                .body("username", equalTo(clientId))  // Client ID is returned as username
                .body("role", equalTo("CLIENT"))
                .extract()
                .response();

        apiToken = response.path("token");

        System.out.println("  ✓ OAuth2 token obtained successfully");
        System.out.println("  Client ID: " + response.path("username"));
        System.out.println("  Role: " + response.path("role"));
        System.out.println("  Token: " + apiToken.substring(0, 50) + "...\n");
    }

    @Test
    @Order(4)
    @DisplayName("4. Call API with OAuth2 token - List invoices")
    public void testCallApiWithToken() {
        System.out.println("Test 4: Calling API with OAuth2 token...");

        given()
                .header("Authorization", "Bearer " + apiToken)
                .when()
                .get("/api/invoices")
                .then()
                .statusCode(200)
                .body("$", instanceOf(java.util.List.class));

        System.out.println("  ✓ Successfully called /api/invoices with OAuth2 token\n");
    }

    @Test
    @Order(5)
    @DisplayName("5. Call API with OAuth2 token - List clients (should fail - wrong role)")
    public void testCallClientsApiShouldFail() {
        System.out.println("Test 5: Attempting to call /api/clients (should fail)...");

        // API clients don't have ADMIN role, so this should fail
        given()
                .header("Authorization", "Bearer " + apiToken)
                .when()
                .get("/api/clients")
                .then()
                .statusCode(403);  // Forbidden - API client doesn't have ADMIN role

        System.out.println("  ✓ Correctly rejected - API client lacks ADMIN role\n");
    }

    @Test
    @Order(6)
    @DisplayName("6. Test invalid client credentials")
    public void testInvalidClientCredentials() {
        System.out.println("Test 6: Testing invalid client credentials...");

        given()
                .contentType("application/x-www-form-urlencoded")
                .formParam("grant_type", "client_credentials")
                .formParam("client_id", clientId)
                .formParam("client_secret", "wrong_secret")
                .when()
                .post("/api/auth/token")
                .then()
                .statusCode(401)
                .body("error", equalTo("invalid_client"));

        System.out.println("  ✓ Invalid credentials correctly rejected\n");
    }

    @Test
    @Order(7)
    @DisplayName("7. Test token without authorization header")
    public void testNoAuthorizationHeader() {
        System.out.println("Test 7: Testing API call without authorization header...");

        given()
                .when()
                .get("/api/invoices")
                .then()
                .statusCode(401);  // Unauthorized

        System.out.println("  ✓ Request without token correctly rejected\n");
    }

    @AfterAll
    public static void tearDown() {
        System.out.println("============================================");
        System.out.println("All OAuth2 tests completed successfully!");
        System.out.println("============================================");
    }
}
