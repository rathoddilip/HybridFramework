package com.framework.listeners;

import com.framework.config.ConfigManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

/**
 * RetryAnalyzer — Retries failed tests up to configured max.
 *
 * Config key: retryCount (default: 1)
 * Applied globally via RetryTransformer — no per-test annotation needed.
 */
public class RetryAnalyzer implements IRetryAnalyzer {

    private static final Logger log = LogManager.getLogger(RetryAnalyzer.class);
    private int currentRetry = 0;
    private static final int MAX_RETRY = ConfigManager.getInt("retryCount");

    @Override
    public boolean retry(ITestResult result) {
        if (currentRetry < MAX_RETRY) {
            currentRetry++;
            log.warn("Retrying failed test [{}] — attempt {}/{}",
                    result.getMethod().getMethodName(), currentRetry, MAX_RETRY);
            return true;
        }
        return false;
    }
}
