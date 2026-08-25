package com.qa.listeners;

import com.qa.utils.ConfigReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

/**
 * Re-runs a failed test at most once.
 *
 * WHY THIS EXISTS, AND WHY THE LIMIT IS ONE
 * -----------------------------------------
 * The application under test is a shared public practice site reached over the
 * open internet. A small number of failures are genuinely environmental: a request
 * times out, an injected advert iframe covers a button for a moment. Re-running
 * those once removes noise that has nothing to do with the application.
 *
 * The limit is one, and it is configurable but not raised, because retries hide
 * real bugs. A test that only passes on the second attempt is telling you
 * something. Every retry is logged, and the README documents that this mechanism
 * exists so nobody reads a pass rate without knowing about it.
 *
 * A retry count of 0 in config.properties disables the mechanism entirely.
 */
public class RetryAnalyzer implements IRetryAnalyzer {

    private static final Logger log = LogManager.getLogger(RetryAnalyzer.class);
    private static final int MAX_RETRIES = ConfigReader.getInt("retry.count");

    private int attempt = 0;

    @Override
    public boolean retry(ITestResult result) {
        if (attempt < MAX_RETRIES) {
            attempt++;
            log.warn("RETRY {}/{}: {}.{} - first attempt failed with: {}",
                    attempt, MAX_RETRIES,
                    result.getTestClass().getRealClass().getSimpleName(),
                    result.getMethod().getMethodName(),
                    result.getThrowable() == null ? "unknown" : result.getThrowable().toString());
            return true;
        }
        return false;
    }
}
