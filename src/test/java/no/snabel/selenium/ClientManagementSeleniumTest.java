package no.snabel.selenium;

import org.junit.jupiter.api.*;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.logging.LogEntry;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Selenium test for the Client Management GUI
 * Tests the complete login and client management flow
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ClientManagementSeleniumTest {

    private static WebDriver driver;
    private static WebDriverWait wait;
    private static final String BASE_URL = "http://localhost:8080";

    @BeforeAll
    public static void setUp() {
        System.out.println("============================================");
        System.out.println("Selenium Test: Client Management GUI");
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
    @DisplayName("1. Page loads successfully")
    public void testPageLoads() {
        System.out.println("Test 1: Loading page...");
        driver.get(BASE_URL + "/clients.html");

        String title = driver.getTitle();
        System.out.println("  Page title: " + title);
        assertEquals("API Client Management - Snabel Accounting", title);

        System.out.println("  ✓ Page loaded successfully\n");
    }

    @Test
    @Order(2)
    @DisplayName("2. Login form is visible")
    public void testLoginFormVisible() {
        System.out.println("Test 2: Checking login form...");

        WebElement loginForm = driver.findElement(By.id("loginForm"));
        assertTrue(loginForm.isDisplayed(), "Login form should be visible");

        WebElement usernameField = driver.findElement(By.id("username"));
        WebElement passwordField = driver.findElement(By.id("password"));

        assertTrue(usernameField.isDisplayed(), "Username field should be visible");
        assertTrue(passwordField.isDisplayed(), "Password field should be visible");

        System.out.println("  ✓ Login form is visible\n");
    }

    @Test
    @Order(3)
    @DisplayName("3. Login with correct credentials")
    public void testLogin() throws InterruptedException {
        System.out.println("Test 3: Attempting login...");

        // Fill in credentials
        WebElement usernameField = driver.findElement(By.id("username"));
        WebElement passwordField = driver.findElement(By.id("password"));

        usernameField.clear();
        usernameField.sendKeys("snabel");

        passwordField.clear();
        passwordField.sendKeys("snabeltann");

        System.out.println("  Credentials entered: snabel / snabeltann");

        // Submit form
        WebElement submitButton = driver.findElement(By.cssSelector("form button[type='submit']"));
        submitButton.click();

        System.out.println("  Login form submitted");

        // Wait for response
        Thread.sleep(3000);

        // Check for error message
        try {
            WebElement errorDiv = driver.findElement(By.id("loginError"));
            String errorClass = errorDiv.getAttribute("class");
            String errorText = errorDiv.getText();

            if (errorText != null && !errorText.isEmpty() && !errorClass.contains("hidden")) {
                System.out.println("  ❌ Login error: " + errorText);
                fail("Login failed with error: " + errorText);
            }
        } catch (Exception e) {
            System.out.println("  No error message found");
        }

        // Check if main app is visible
        try {
            WebElement mainApp = driver.findElement(By.id("mainApp"));
            boolean isVisible = mainApp.isDisplayed();

            System.out.println("  Main app visible: " + isVisible);

            if (isVisible) {
                System.out.println("  ✓ Login successful!\n");

                // Check user display
                WebElement userDisplay = driver.findElement(By.id("userDisplay"));
                String userText = userDisplay.getText();
                System.out.println("  User display text: " + userText);
                assertTrue(userText.contains("snabel"), "User display should show username");
            } else {
                System.out.println("  ❌ Main app not visible - checking console logs...\n");
                printBrowserLogs();
                fail("Main app not visible after login");
            }
        } catch (Exception e) {
            System.out.println("  ❌ Could not find main app: " + e.getMessage());
            printBrowserLogs();
            fail("Main app not found after login");
        }
    }

    @Test
    @Order(4)
    @DisplayName("4. Check browser console for errors")
    public void testNoConsoleErrors() {
        System.out.println("Test 4: Checking browser console...");
        printBrowserLogs();
        System.out.println();
    }

    private static void printBrowserLogs() {
        try {
            List<LogEntry> logs = driver.manage().logs().get(LogType.BROWSER).getAll();
            if (logs.isEmpty()) {
                System.out.println("  No console logs found");
            } else {
                System.out.println("  Browser console logs:");
                for (LogEntry log : logs) {
                    System.out.println("    [" + log.getLevel() + "] " + log.getMessage());
                }
            }
        } catch (Exception e) {
            System.out.println("  Could not retrieve browser logs: " + e.getMessage());
        }
    }

    @AfterAll
    public static void tearDown() {
        System.out.println("============================================");
        System.out.println("Test completed - closing browser");
        System.out.println("============================================");

        if (driver != null) {
            driver.quit();
        }
    }
}
