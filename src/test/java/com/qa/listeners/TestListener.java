package com.qa.listeners;

import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.MediaEntityBuilder;
import com.aventstack.extentreports.Status;
import com.qa.base.DriverFactory;
import com.qa.utils.ExtentManager;
import com.qa.utils.ScreenshotUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.util.Arrays;

/**
 * Bridges TestNG's lifecycle to the ExtentReport, and captures a screenshot on
 * every failure automatically.
 *
 * "Automatically" is the point. Screenshot code inside a catch block in each test
 * is forgotten exactly when it matters, and there is no catch block in a passing
 * test to begin with. A listener sees every result, so the coverage is complete
 * and no test method contains reporting code at all.
 */
public class TestListener implements ITestListener {

    private static final Logger log = LogManager.getLogger(TestListener.class);

    @Override
    public void onStart(ITestContext context) {
        ExtentManager.getInstance();
        log.info("SUITE START: {}", context.getName());
    }

    @Override
    public void onTestStart(ITestResult result) {
        ExtentTest test = ExtentManager.getInstance()
                .createTest(testName(result), description(result));

        // Groups become filterable categories in the report, so a reader can look at
        // "smoke" or "api" on its own.
        String[] groups = result.getMethod().getGroups();
        if (groups.length > 0) {
            test.assignCategory(groups);
        }
        ExtentManager.setTest(test);
        log.info("TEST START: {}", testName(result));
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        ExtentTest test = ExtentManager.getTest();
        if (test != null) {
            test.log(Status.PASS, "Passed in " + durationSeconds(result) + "s");
        }
        log.info("PASS: {} ({}s)", testName(result), durationSeconds(result));
        ExtentManager.unloadTest();
    }

    @Override
    public void onTestFailure(ITestResult result) {
        ExtentTest test = ExtentManager.getTest();
        Throwable error = result.getThrowable();

        log.error("FAIL: {} - {}", testName(result), error == null ? "no throwable" : error.toString());

        if (test != null) {
            test.log(Status.FAIL, "Failed in " + durationSeconds(result) + "s");
            if (error != null) {
                test.fail(error);
            }
            attachScreenshot(test, result);
        }
        ExtentManager.unloadTest();
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        ExtentTest test = ExtentManager.getTest();
        if (test != null) {
            String reason = result.getThrowable() == null
                    ? "Skipped - a dependency or configuration method did not pass"
                    : result.getThrowable().getMessage();
            test.log(Status.SKIP, reason);
        }
        log.warn("SKIP: {}", testName(result));
        ExtentManager.unloadTest();
    }

    @Override
    public void onTestFailedButWithinSuccessPercentage(ITestResult result) {
        onTestFailure(result);
    }

    @Override
    public void onFinish(ITestContext context) {
        log.info("SUITE END: {} | passed={} failed={} skipped={}",
                context.getName(),
                context.getPassedTests().size(),
                context.getFailedTests().size(),
                context.getSkippedTests().size());
        ExtentManager.flush();
    }

    // ------------------------------------------------------------------ helpers

    private void attachScreenshot(ExtentTest test, ITestResult result) {
        // API tests have no browser, so there is nothing to photograph.
        if (!DriverFactory.hasDriver()) {
            test.info("No browser session for this test - screenshot not applicable.");
            return;
        }
        WebDriver driver = DriverFactory.getDriver();

        // Written to disk for anyone who wants the raw file...
        ScreenshotUtils.captureToFile(driver, testName(result));

        // ...and embedded as Base64 so the HTML report stays self-contained when it
        // is downloaded from a CI build artefact.
        String base64 = ScreenshotUtils.captureAsBase64(driver);
        if (base64 != null) {
            test.fail("Screenshot at point of failure:",
                    MediaEntityBuilder.createScreenCaptureFromBase64String(base64).build());
        }
        test.info("URL at failure: " + driver.getCurrentUrl());
    }

    private String testName(ITestResult result) {
        String name = result.getMethod().getMethodName();
        Object[] params = result.getParameters();
        // Data-driven tests run the same method many times; without the parameters
        // the report would show ten identical rows.
        if (params.length > 0) {
            return name + " " + Arrays.toString(params);
        }
        return name;
    }

    private String description(ITestResult result) {
        String description = result.getMethod().getDescription();
        return description == null || description.isBlank()
                ? result.getTestClass().getRealClass().getSimpleName()
                : description;
    }

    private String durationSeconds(ITestResult result) {
        return String.format("%.1f", (result.getEndMillis() - result.getStartMillis()) / 1000.0);
    }
}
