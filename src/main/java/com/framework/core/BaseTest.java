package com.framework.core;

import com.framework.config.ConfigManager;
import io.qameta.allure.Attachment;
import io.qameta.allure.Step;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.assertj.core.api.SoftAssertions;
import org.openqa.selenium.WebDriver;
import org.testng.ITestResult;
import org.testng.annotations.*;

/**
 * BaseTest — Parent class for all TestNG test classes.
 *
 * Responsibilities:
 *  - WebDriver lifecycle (init/quit per method, thread-safe)
 *  - SoftAssertions management
 *  - Screenshot + page source on failure
 *  - Test start/end logging with timing
 *  - @BeforeMethod / @AfterMethod hooks
 *
 * Usage:
 *   public class LoginTest extends BaseTest { ... }
 */
public abstract class BaseTest {

    private static final Logger log = LogManager.getLogger(BaseTest.class);
    protected SoftAssertions softly;

    // ── Suite Level ───────────────────────────────────────────────────────────

    @BeforeSuite(alwaysRun = true)
    public void suiteSetup() {
        log.info("========================================");
        log.info("  SUITE START — Environment: [{}]", ConfigManager.resolveEnv());
        log.info("========================================");
    }

    @AfterSuite(alwaysRun = true)
    public void suiteTeardown() {
        log.info("========================================");
        log.info("  SUITE END");
        log.info("========================================");
    }

    // ── Test Level ────────────────────────────────────────────────────────────

    @BeforeMethod(alwaysRun = true)
    @Parameters({"browser"})
    public void setUp(ITestResult result, @Optional String browser) {
        log.info("--------------------------------------------------");
        log.info("  TEST START: [{}]", result.getMethod().getMethodName());
        log.info("  Thread ID : {}", Thread.currentThread().getId());
        log.info("--------------------------------------------------");

        softly = new SoftAssertions();

        if (isWebTest()) {
            DriverFactory.setBrowserOverride(browser);
            DriverFactory.initDriver();
            log.info("WebDriver initialized for: [{}] | browser: [{}]",
                    result.getMethod().getMethodName(),
                    browser != null ? browser : ConfigManager.get("browser", "chrome"));
        }
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown(ITestResult result) {
        try {
            if (result.getStatus() == ITestResult.FAILURE) {
                log.error("TEST FAILED: [{}]", result.getMethod().getMethodName());
                if (isWebTest() && isDriverActive()) {
                    try {
                        captureScreenshot();
                        capturePageSource();
                    } catch (Exception e) {
                        log.error("Error capturing failure evidence: {}", e.getMessage());
                    }
                }
            } else if (result.getStatus() == ITestResult.SUCCESS) {
                log.info("TEST PASSED: [{}]", result.getMethod().getMethodName());
            } else if (result.getStatus() == ITestResult.SKIP) {
                log.warn("TEST SKIPPED: [{}]", result.getMethod().getMethodName());
            }
        } finally {
            if (isWebTest()) {
                try {
                    DriverFactory.quitDriver();
                } catch (Exception e) {
                    log.error("Error quitting driver: {}", e.getMessage());
                }
            }
            log.info("--------------------------------------------------\n");
        }
    }

    // ── Driver Access ─────────────────────────────────────────────────────────

    /**
     * Get current thread's WebDriver.
     */
    protected WebDriver getDriver() {
        return DriverFactory.getDriver();
    }

    /**
     * Override to false in pure API tests to skip driver init/quit.
     * Default: true (assumes web or hybrid test)
     */
    protected boolean isWebTest() {
        return true;
    }

    private boolean isDriverActive() {
        try {
            DriverFactory.getDriver();
            return true;
        } catch (IllegalStateException e) {
            return false;
        }
    }

    // ── Soft Assertions ───────────────────────────────────────────────────────

    /**
     * Call at the end of your test to evaluate all soft assertions.
     */
    @Step("Evaluate all soft assertions")
    protected void assertAll() {
        softly.assertAll();
    }

    // ── Allure Attachments ────────────────────────────────────────────────────

    @Attachment(value = "Screenshot on Failure", type = "image/png")
    private byte[] captureScreenshot() {
        try {
            return new BasePage() {}.takeScreenshot();
        } catch (Exception e) {
            log.error("Could not capture screenshot: {}", e.getMessage());
            return new byte[0];
        }
    }

    @Attachment(value = "Page Source on Failure", type = "text/html")
    private String capturePageSource() {
        try {
            return getDriver().getPageSource();
        } catch (Exception e) {
            log.error("Could not capture page source: {}", e.getMessage());
            return "";
        }
    }
}
