package com.framework.core;

import com.framework.config.ConfigManager;
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

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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
        ChromeOptions options = new ChromeOptions();
        applyCommonChromeOptions(options, headless);
        return new ChromeDriver(options);
    }

    private static WebDriver createFirefoxDriver(boolean headless) {
        FirefoxOptions options = new FirefoxOptions();
        if (headless) options.addArguments("-headless");
        options.addArguments("--width=1920", "--height=1080");
        return new FirefoxDriver(options);
    }

    private static WebDriver createEdgeDriver(boolean headless) {
        setupEdgeDriver();
        EdgeOptions options = new EdgeOptions();
        if (headless) options.addArguments("--headless=new");
        options.addArguments("--window-size=1920,1080");
        return new EdgeDriver(options);
    }

    /**
     * Selenium Manager and WebDriverManager still hit deprecated msedgedriver.azureedge.net.
     * Download the matching driver from msedgedriver.microsoft.com instead.
     */
    private static void setupEdgeDriver() {
        if (System.getProperty("webdriver.edge.driver") != null) {
            return;
        }

        try {
            Path driverPath = downloadEdgeDriverFromMicrosoft();
            System.setProperty("webdriver.edge.driver", driverPath.toString());
            log.info("Edge driver ready at: {}", driverPath);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to resolve Edge WebDriver. Install Edge or check network access to "
                            + "https://msedgedriver.microsoft.com", e);
        }
    }

    private static Path downloadEdgeDriverFromMicrosoft() throws Exception {
        String edgeVersion = detectInstalledEdgeVersion();
        String driverVersion = fetchEdgeDriverVersion(edgeVersion);
        String safeVersion = driverVersion.replaceAll("[^0-9.]", "");
        String downloadUrl = "https://msedgedriver.microsoft.com/"
                + safeVersion + "/edgedriver_win64.zip";

        log.info("Downloading Edge driver {} from {}", safeVersion, downloadUrl);

        Path cacheDir = Path.of(System.getProperty("java.io.tmpdir"), "edge-driver-cache", safeVersion);
        Path driverExe = cacheDir.resolve("msedgedriver.exe");
        if (Files.exists(driverExe)) {
            return driverExe;
        }

        Files.createDirectories(cacheDir);
        Path zipPath = cacheDir.resolve("edgedriver.zip");

        try (InputStream in = new BufferedInputStream(URI.create(downloadUrl).toURL().openStream())) {
            Files.copy(in, zipPath, StandardCopyOption.REPLACE_EXISTING);
        }

        unzip(zipPath, cacheDir);
        Files.deleteIfExists(zipPath);

        if (!Files.exists(driverExe)) {
            throw new IllegalStateException("msedgedriver.exe not found after extracting " + downloadUrl);
        }

        return driverExe;
    }

    private static String detectInstalledEdgeVersion() throws Exception {
        String[] regCommands = {
                "reg query HKCU\\Software\\Microsoft\\Edge\\BLBeacon /v version",
                "reg query HKLM\\SOFTWARE\\Microsoft\\Edge\\BLBeacon /v version",
                "reg query HKLM\\SOFTWARE\\WOW6432Node\\Microsoft\\Edge\\BLBeacon /v version"
        };

        for (String command : regCommands) {
            Process process = new ProcessBuilder("cmd", "/c", command)
                    .redirectErrorStream(true)
                    .start();

            String output;
            try (InputStream in = process.getInputStream()) {
                output = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
            process.waitFor();

            Matcher matcher = Pattern.compile("(\\d+\\.\\d+\\.\\d+\\.\\d+)").matcher(output);
            if (matcher.find()) {
                String version = matcher.group(1);
                log.info("Detected installed Edge version: {}", version);
                return version;
            }
        }

        throw new IllegalStateException("Could not detect installed Edge version from registry");
    }

    private static String fetchEdgeDriverVersion(String edgeVersion) throws Exception {
        String major = edgeVersion.split("\\.")[0];
        String url = "https://msedgedriver.microsoft.com/LATEST_RELEASE_" + major + "_WINDOWS";

        try (InputStream in = URI.create(url).toURL().openStream()) {
            String driverVersion = new String(in.readAllBytes(), StandardCharsets.UTF_8).trim();
            driverVersion = driverVersion.replaceAll("[^0-9.]", "");
            if (!driverVersion.isBlank()) {
                return driverVersion;
            }
        } catch (Exception ignored) {
            log.warn("Could not resolve LATEST_RELEASE_{}_WINDOWS, using installed Edge version", major);
        }

        return edgeVersion;
    }

    private static void unzip(Path zipFile, Path targetDir) throws Exception {
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path extracted = targetDir.resolve(entry.getName()).normalize();
                if (!extracted.startsWith(targetDir)) {
                    throw new SecurityException("Invalid zip entry: " + entry.getName());
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(extracted);
                } else {
                    Files.createDirectories(extracted.getParent());
                    Files.copy(zis, extracted, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }

        try (Stream<Path> walk = Files.walk(targetDir)) {
            walk.filter(path -> path.getFileName().toString().equalsIgnoreCase("msedgedriver.exe"))
                    .findFirst()
                    .ifPresent(found -> {
                        try {
                            if (!found.equals(targetDir.resolve("msedgedriver.exe"))) {
                                Files.copy(found, targetDir.resolve("msedgedriver.exe"),
                                        StandardCopyOption.REPLACE_EXISTING);
                            }
                        } catch (Exception e) {
                            throw new RuntimeException("Failed to normalize msedgedriver.exe location", e);
                        }
                    });
        }
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
