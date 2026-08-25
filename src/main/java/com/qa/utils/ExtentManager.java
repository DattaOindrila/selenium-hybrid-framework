package com.qa.utils;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Owns the single ExtentReports instance and the per-thread ExtentTest.
 *
 * ExtentReports itself is one object for the whole run - it is the report file.
 * ExtentTest is per test method, and because tests can run in parallel it is held
 * in a ThreadLocal for exactly the same reason the WebDriver is.
 *
 * Wiring Extent by hand rather than pulling in the TestNG adapter keeps the
 * relationship between listener and report visible in this repository.
 */
public final class ExtentManager {

    private static final Logger log = LogManager.getLogger(ExtentManager.class);
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final ThreadLocal<ExtentTest> CURRENT_TEST = new ThreadLocal<>();

    private static ExtentReports extent;
    private static Path reportPath;

    private ExtentManager() {
    }

    public static synchronized ExtentReports getInstance() {
        if (extent == null) {
            String dir = ConfigReader.get("report.dir", "reports");
            reportPath = Paths.get(dir, "ExtentReport_" + LocalDateTime.now().format(STAMP) + ".html");

            ExtentSparkReporter spark = new ExtentSparkReporter(reportPath.toFile());
            spark.config().setTheme(Theme.STANDARD);
            spark.config().setDocumentTitle("Hybrid UI + API Automation Report");
            spark.config().setReportName("automationexercise.com regression suite");
            spark.config().setTimeStampFormat("dd-MM-yyyy HH:mm:ss");

            extent = new ExtentReports();
            extent.attachReporter(spark);

            // System information block at the top of the report. These are read from
            // the same config the run actually used, so the report cannot claim a
            // browser or environment the run did not use.
            extent.setSystemInfo("Application", ConfigReader.get("base.url"));
            extent.setSystemInfo("Browser", ConfigReader.get("browser"));
            extent.setSystemInfo("Headless", ConfigReader.get("headless"));
            extent.setSystemInfo("OS", System.getProperty("os.name") + " " + System.getProperty("os.version"));
            extent.setSystemInfo("Java", System.getProperty("java.version"));
            extent.setSystemInfo("Executed by", System.getProperty("user.name"));

            log.info("ExtentReport will be written to {}", reportPath.toAbsolutePath());
        }
        return extent;
    }

    public static void setTest(ExtentTest test) {
        CURRENT_TEST.set(test);
    }

    public static ExtentTest getTest() {
        return CURRENT_TEST.get();
    }

    public static void unloadTest() {
        CURRENT_TEST.remove();
    }

    /** Called once at the end of the suite; without it the HTML is never written. */
    public static synchronized void flush() {
        if (extent != null) {
            extent.flush();
            log.info("ExtentReport written: {}", reportPath.toAbsolutePath());
        }
    }

    public static Path getReportPath() {
        return reportPath;
    }

    /**
     * Convenience for logging a step from inside a test. Safe to call when no
     * ExtentTest is bound (for example when a class is run straight from an IDE
     * without the listener), so tests never crash because of reporting.
     */
    public static void logStep(String message) {
        log.info(message);
        ExtentTest test = CURRENT_TEST.get();
        if (test != null) {
            test.info(message);
        }
    }
}
