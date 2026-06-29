package com.framework.core;

import com.framework.config.ConfigManager;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;

/**
 * DriverFactory — Thread-safe WebDriver management using ThreadLocal.
 *
 * Supports: Chrome, Firefox, Edge, Remote (Selenium Grid)
 * Config keys: browser, headless, remote, gridUrl, implicitWait, pageLoadTimeout
 *
 * Usage:
 *   DriverFactory.initDriver();
 *   WebDriver driver = DriverFactory.getDriver();
 *   DriverFactory.quitDriver();
 */
public class DriverFactory {

    private static final Logger log = LogManager.getLogger(DriverFactory.class);
    private static final ThreadLocal<WebDriver> driverThread = new ThreadLocal<>();
    private static final ThreadLocal<String> browserOverride = new ThreadLocal<>();

    private DriverFactory() {}

    /**
     * Override browser for the current thread (e.g. from TestNG suite parameter).
     * Falls back to config when not set.
     */
    public static void setBrowserOverride(String browser) {
        if (browser != null && !browser.isBlank()) {
            browserOverride.set(browser.trim().toLowerCase());
        }
    }

    /**
     * Initialize WebDriver for the current thread.
     * Browser & options read from TestNG override, then config.
     */
    public static void initDriver() {
        String browser = resolveBrowser();
        boolean headless = ConfigManager.getBoolean("headless");
        boolean remote   = ConfigManager.getBoolean("remote");

        log.info("Initializing [{}] driver | headless={} | remote={} | thread={}",
                browser, headless, remote, Thread.currentThread().getId());

        WebDriver driver;

        if (remote) {
            driver = createRemoteDriver(browser, headless);
        } else {
            driver = switch (browser) {
                case "firefox" -> createFirefoxDriver(headless);
                case "edge"    -> createEdgeDriver(headless);
                default        -> createChromeDriver(headless);
            };
        }

        driver.manage().timeouts().implicitlyWait(
                Duration.ofSeconds(ConfigManager.getInt("implicitWait")));
        driver.manage().timeouts().pageLoadTimeout(
                Duration.ofSeconds(ConfigManager.getInt("pageLoadTimeout")));
        driver.manage().window().maximize();

        driverThread.set(driver);
        log.info("WebDriver initialized successfully for thread: {}", Thread.currentThread().getId());
    }

    /**
     * Get the WebDriver for the current thread.
     */
    public static WebDriver getDriver() {
        WebDriver driver = driverThread.get();
        if (driver == null) {
            throw new IllegalStateException(
                "WebDriver not initialized for thread: " + Thread.currentThread().getId() +
                ". Call DriverFactory.initDriver() first (in @BeforeMethod)."
            );
        }
        return driver;
    }

    /**
     * Quit and remove driver for current thread.
     */
    public static void quitDriver() {
        WebDriver driver = driverThread.get();
        if (driver != null) {
            try {
                driver.quit();
                log.info("WebDriver quit successfully for thread: {}", Thread.currentThread().getId());
            } catch (Exception e) {
                log.error("Error quitting WebDriver: {}", e.getMessage());
            } finally {
                driverThread.remove();
                browserOverride.remove();
            }
        }
    }

    private static String resolveBrowser() {
        String override = browserOverride.get();
        if (override != null && !override.isBlank()) {
            return override;
        }
        return ConfigManager.get("browser", "chrome").toLowerCase();
    }

    // ── Private browser builders ──────────────────────────────────────────────

    private static WebDriver createChromeDriver(boolean headless) {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        applyCommonChromeOptions(options, headless);
        return new ChromeDriver(options);
    }

    private static WebDriver createFirefoxDriver(boolean headless) {
        WebDriverManager.firefoxdriver().setup();
        FirefoxOptions options = new FirefoxOptions();
        if (headless) options.addArguments("--headless");
        options.addArguments("--width=1920", "--height=1080");
        return new FirefoxDriver(options);
    }

    private static WebDriver createEdgeDriver(boolean headless) {
        WebDriverManager.edgedriver().setup();
        EdgeOptions options = new EdgeOptions();
        if (headless) options.addArguments("--headless");
        options.addArguments("--window-size=1920,1080");
        return new EdgeDriver(options);
    }

    private static WebDriver createRemoteDriver(String browser, boolean headless) {
        String gridUrl = ConfigManager.get("gridUrl");
        log.info("Connecting to Selenium Grid at: {}", gridUrl);

        ChromeOptions options = new ChromeOptions();
        applyCommonChromeOptions(options, headless);

        try {
            return new RemoteWebDriver(new URL(gridUrl), options);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Invalid Grid URL: " + gridUrl, e);
        }
    }

    private static void applyCommonChromeOptions(ChromeOptions options, boolean headless) {
        if (headless) {
            options.addArguments("--headless=new");
        }
        options.addArguments(
            "--window-size=1920,1080",
            "--disable-notifications",
            "--disable-popup-blocking",
            "--no-sandbox",
            "--disable-dev-shm-usage",
            "--disable-gpu",
            "--remote-allow-origins=*",
            "--disable-extensions"
        );
        options.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});
        options.setExperimentalOption("useAutomationExtension", false);
    }
}
