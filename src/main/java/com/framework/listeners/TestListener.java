package com.framework.listeners;

import com.framework.config.ConfigManager;
import io.qameta.allure.Allure;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.ISuite;
import org.testng.ISuiteListener;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * Handles TestNG lifecycle events and builds the Allure report after suite execution.
 */
public class TestListener implements ITestListener, ISuiteListener {

    private static final Logger log = LogManager.getLogger(TestListener.class);

    @Override
    public void onStart(ISuite suite) {
        cleanAllureOutput();
        log.info("Suite started: [{}]", suite.getName());
    }

    @Override
    public void onFinish(ISuite suite) {
        log.info("Suite finished: [{}]", suite.getName());
        generateAndOpenAllureReport();
    }

    @Override
    public void onTestStart(ITestResult result) {
        log.info("Starting: [{}]", getTestName(result));
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        long duration = result.getEndMillis() - result.getStartMillis();
        log.info("PASSED: [{}] ({} ms)", getTestName(result), duration);
    }

    @Override
    public void onTestFailure(ITestResult result) {
        long duration = result.getEndMillis() - result.getStartMillis();
        log.error("FAILED: [{}] ({} ms)", getTestName(result), duration);
        log.error("Cause: {}", result.getThrowable() != null
                ? result.getThrowable().getMessage()
                : "Unknown");

        Throwable throwable = result.getThrowable();
        if (throwable != null) {
            Allure.addAttachment("Failure Reason", throwable.getMessage());
        }
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        log.warn("SKIPPED: [{}]", getTestName(result));
        if (result.getThrowable() != null) {
            log.warn("Reason: {}", result.getThrowable().getMessage());
        }
    }

    @Override
    public void onTestFailedButWithinSuccessPercentage(ITestResult result) {
        log.warn("FAILED WITHIN SUCCESS PERCENTAGE: [{}]", getTestName(result));
    }

    @Override
    public void onStart(ITestContext context) {
        log.info("Test context started: [{}]", context.getName());
    }

    @Override
    public void onFinish(ITestContext context) {
        int passed = context.getPassedTests().size();
        int failed = context.getFailedTests().size();
        int skipped = context.getSkippedTests().size();
        log.info("Test context finished: [{}] | Pass={} Fail={} Skip={}",
                context.getName(), passed, failed, skipped);
    }

    private String getTestName(ITestResult result) {
        return result.getTestClass().getName() + "." + result.getMethod().getMethodName();
    }

    private void cleanAllureOutput() {
        Path projectDir = Path.of(System.getProperty("user.dir"));
        deleteDirectory(projectDir.resolve(ConfigManager.get("allure.resultsDir", "target/allure-results")));
        deleteDirectory(projectDir.resolve("target/site/allure-maven-plugin"));
        deleteDirectory(projectDir.resolve("target/allure-report"));
    }

    private void deleteDirectory(Path directory) {
        if (!Files.exists(directory)) {
            return;
        }

        try (Stream<Path> paths = Files.walk(directory)) {
            paths.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            log.warn("Unable to delete Allure file: {}", path);
                        }
                    });
            log.info("Cleaned Allure output: {}", directory);
        } catch (IOException e) {
            log.warn("Unable to clean Allure output directory [{}]: {}", directory, e.getMessage());
        }
    }

    private void generateAndOpenAllureReport() {
        if (isCiBuild()) {
            log.info("CI build detected. Skipping local Allure report server; publish Allure from Jenkins post actions.");
            return;
        }

        if (!ConfigManager.getBoolean("allure.autoGenerate")) {
            return;
        }

        try {
            Path projectDir = Path.of(System.getProperty("user.dir"));
            Path resultsDir = projectDir.resolve(ConfigManager.get("allure.resultsDir", "target/allure-results"));

            if (!Files.isDirectory(resultsDir)) {
                log.warn("Allure results directory not found, skipping report generation: {}", resultsDir);
                return;
            }

            Process process = new ProcessBuilder(mavenCommand(), "-q", "allure:report", "-DskipTests")
                    .directory(projectDir.toFile())
                    .redirectErrorStream(true)
                    .redirectOutput(ProcessBuilder.Redirect.appendTo(
                            projectDir.resolve("target/allure-report-generation.log").toFile()))
                    .start();

            boolean finished = process.waitFor(Duration.ofMinutes(2).toMillis(), TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                log.warn("Allure report generation timed out. Check target/allure-report-generation.log");
                return;
            }

            if (process.exitValue() != 0) {
                log.warn("Allure report generation failed with exit code {}. Check target/allure-report-generation.log",
                        process.exitValue());
                return;
            }

            Path reportIndex = projectDir.resolve("target/site/allure-maven-plugin/index.html");
            log.info("Allure report generated: {}", reportIndex);

            if (ConfigManager.getBoolean("allure.autoOpen")) {
                startAllureServer(projectDir);
            }
        } catch (Exception e) {
            log.warn("Unable to generate/open Allure report: {}", e.getMessage());
        }
    }

    private String mavenCommand() {
        return System.getProperty("os.name").toLowerCase().contains("win") ? "mvn.cmd" : "mvn";
    }

    private boolean isCiBuild() {
        return System.getenv("JENKINS_HOME") != null
                || System.getenv("BUILD_NUMBER") != null
                || System.getenv("CI") != null;
    }

    private void startAllureServer(Path projectDir) throws IOException {
        new ProcessBuilder(mavenCommand(), "-q", "allure:serve", "-DskipTests")
                .directory(projectDir.toFile())
                .redirectErrorStream(true)
                .redirectOutput(ProcessBuilder.Redirect.appendTo(
                        projectDir.resolve("target/allure-serve.log").toFile()))
                .start();

        log.info("Allure server starting. If the browser does not open, check target/allure-serve.log for the URL.");
    }
}
