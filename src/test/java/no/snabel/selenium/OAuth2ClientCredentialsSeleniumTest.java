package no.snabel.selenium;

import org.junit.jupiter.api.*;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Selenium test for OAuth2 Client Credentials Flow through the GUI
 * Tests the complete flow:
 * 1. Login as admin
 * 2. Create an API client through the GUI
 * 3. Copy the client credentials
 * 4. Use credentials to get OAuth2 token (via API)
 * 5. Use token to call backend API (via API)
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class OAuth2ClientCredentialsSeleniumTest {

    private static WebDriver driver;
    private static WebDriverWait wait;
    private static final String BASE_URL = "http://localhost:8080";

    private static String clientId;
    private static String clientSecret;
    private static String accessToken;

    @BeforeAll
    public static void setUp() {
        System.out.println("============================================");
        System.out.println("Selenium Test: OAuth2 Client Credentials Flow");
        System.out.println("============================================\n");

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");

        driver = new ChromeDriver(options);
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    @Test
    @Order(1)
    @DisplayName("1. Load clients page and verify login form")
    public void testLoadPage() {
        System.out.println("Test 1: Loading clients page...");
        driver.get(BASE_URL + "/clients.html");

        String title = driver.getTitle();
        System.out.println("  Page title: " + title);
        assertEquals("API Client Management - Snabel Accounting", title);

        WebElement loginForm = driver.findElement(By.id("loginForm"));
        assertTrue(loginForm.isDisplayed(), "Login form should be visible");

        System.out.println("  ✓ Page loaded and login form is visible\n");
    }

    @Test
    @Order(2)
    @DisplayName("2. Login as admin")
    public void testAdminLogin() throws InterruptedException {
        System.out.println("Test 2: Logging in as admin...");

        WebElement usernameField = driver.findElement(By.id("username"));
        WebElement passwordField = driver.findElement(By.id("password"));

        usernameField.clear();
        usernameField.sendKeys("snabel");

        passwordField.clear();
        passwordField.sendKeys("snabeltann");

        System.out.println("  Credentials entered: snabel / snabeltann");

        WebElement submitButton = driver.findElement(By.cssSelector("form button[type='submit']"));
        submitButton.click();

        System.out.println("  Login form submitted");

        // Wait for main app to appear
        Thread.sleep(2000);

        WebElement mainApp = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("mainApp")));
        assertTrue(mainApp.isDisplayed(), "Main app should be visible after login");

        WebElement userDisplay = driver.findElement(By.id("userDisplay"));
        String userText = userDisplay.getText();
        System.out.println("  User display: " + userText);
        assertTrue(userText.contains("snabel"), "User display should show username");

        System.out.println("  ✓ Login successful\n");
    }

    @Test
    @Order(3)
    @DisplayName("3. Create new API client through GUI")
    public void testCreateApiClient() throws InterruptedException {
        System.out.println("Test 3: Creating API client...");

        // Fill in client creation form
        WebElement nameInput = driver.findElement(By.id("clientName"));
        WebElement descInput = driver.findElement(By.id("clientDescription"));
        WebElement scopesInput = driver.findElement(By.id("clientScopes"));

        nameInput.clear();
        nameInput.sendKeys("Selenium Test Client");

        descInput.clear();
        descInput.sendKeys("Client created by Selenium test");

        scopesInput.clear();
        scopesInput.sendKeys("read:invoices,write:invoices");

        System.out.println("  Client details entered");

        // Submit form (button doesn't have an ID, so use CSS selector for the submit button in the form)
        WebElement createButton = driver.findElement(By.cssSelector("form[onsubmit='createClient(event)'] button[type='submit']"));
        createButton.click();

        System.out.println("  Create button clicked");

        // Wait for secret display to appear
        Thread.sleep(2000);

        // Wait for the secret display div to become visible
        WebElement secretDisplay = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("secretDisplay")));

        // Extract client ID and secret from the specific span elements
        WebElement clientIdElement = driver.findElement(By.id("displayClientId"));
        WebElement clientSecretElement = driver.findElement(By.id("displayClientSecret"));

        clientId = clientIdElement.getText();
        clientSecret = clientSecretElement.getText();

        assertNotNull(clientId, "Client ID should be extracted");
        assertNotNull(clientSecret, "Client Secret should be extracted");
        assertTrue(clientId.startsWith("client_"), "Client ID should start with client_");
        assertTrue(clientSecret.startsWith("secret_"), "Client Secret should start with secret_");

        System.out.println("  ✓ API client created successfully");
        System.out.println("  Client ID: " + clientId);
        System.out.println("  Client Secret: " + clientSecret.substring(0, 20) + "...\n");
    }

    @Test
    @Order(4)
    @DisplayName("4. Get OAuth2 token using client credentials (API)")
    public void testGetOAuth2Token() throws Exception {
        System.out.println("Test 4: Getting OAuth2 token via API...");

        assertNotNull(clientId, "Client ID must be available from previous test");
        assertNotNull(clientSecret, "Client Secret must be available from previous test");

        // Use Java HTTP client to get token
        String tokenEndpoint = BASE_URL + "/api/auth/token";
        String formData = "grant_type=client_credentials&client_id=" + clientId + "&client_secret=" + clientSecret;

        java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
        java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create(tokenEndpoint))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString(formData))
                .build();

        java.net.http.HttpResponse<String> response = client.send(request,
                java.net.http.HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode(), "Token endpoint should return 200");

        String responseBody = response.body();
        System.out.println("  Token response: " + responseBody.substring(0, Math.min(100, responseBody.length())) + "...");

        // Parse JSON response to get token
        assertTrue(responseBody.contains("\"token\""), "Response should contain token");

        // Simple JSON parsing (extract token value)
        int tokenStart = responseBody.indexOf("\"token\":\"") + 9;
        int tokenEnd = responseBody.indexOf("\"", tokenStart);
        accessToken = responseBody.substring(tokenStart, tokenEnd);

        assertNotNull(accessToken, "Access token should be extracted");
        assertTrue(accessToken.length() > 50, "Access token should be a valid JWT");

        System.out.println("  ✓ OAuth2 token obtained successfully");
        System.out.println("  Token: " + accessToken.substring(0, 50) + "...\n");
    }

    @Test
    @Order(5)
    @DisplayName("5. Call protected API with OAuth2 token")
    public void testCallApiWithToken() throws Exception {
        System.out.println("Test 5: Calling protected API with OAuth2 token...");

        assertNotNull(accessToken, "Access token must be available from previous test");

        // Call the invoices API
        String apiEndpoint = BASE_URL + "/api/invoices";

        java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
        java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create(apiEndpoint))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();

        java.net.http.HttpResponse<String> response = client.send(request,
                java.net.http.HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode(), "API should return 200 with valid token");

        String responseBody = response.body();
        System.out.println("  API response: " + responseBody);

        // Response should be a JSON array (even if empty)
        assertTrue(responseBody.trim().startsWith("["), "Response should be a JSON array");

        System.out.println("  ✓ Successfully accessed protected API with OAuth2 token\n");
    }

    @Test
    @Order(6)
    @DisplayName("6. Verify client appears in client list")
    public void testClientListContainsNewClient() throws InterruptedException {
        System.out.println("Test 6: Checking client list...");

        // Refresh or scroll to client list
        Thread.sleep(1000);

        // Look for the client in the list (use clientsTableBody ID from the HTML)
        WebElement clientList = driver.findElement(By.id("clientsTableBody"));
        String listText = clientList.getText();

        System.out.println("  Client list text: " + listText.substring(0, Math.min(200, listText.length())));

        assertTrue(listText.contains("Selenium Test Client"),
                "Client list should contain the newly created client");

        System.out.println("  ✓ New client appears in the client list\n");
    }

    @Test
    @Order(7)
    @DisplayName("7. Test API call without token (should fail)")
    public void testApiCallWithoutToken() throws Exception {
        System.out.println("Test 7: Testing API call without token...");

        String apiEndpoint = BASE_URL + "/api/invoices";

        java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
        java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create(apiEndpoint))
                .GET()
                .build();

        java.net.http.HttpResponse<String> response = client.send(request,
                java.net.http.HttpResponse.BodyHandlers.ofString());

        assertEquals(401, response.statusCode(), "API should return 401 without token");

        System.out.println("  ✓ API correctly rejects requests without token\n");
    }

    @AfterAll
    public static void tearDown() {
        System.out.println("============================================");
        System.out.println("All Selenium OAuth2 tests completed!");
        System.out.println("============================================");

        if (driver != null) {
            driver.quit();
        }
    }
}
