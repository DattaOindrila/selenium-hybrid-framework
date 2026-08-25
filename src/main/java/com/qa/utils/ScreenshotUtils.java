package com.qa.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Captures failure screenshots.
 *
 * Two outputs, deliberately:
 *  - a Base64 string, embedded straight into the ExtentReport, so the HTML file is
 *    self-contained and still shows its images after being downloaded from a CI
 *    build artefact;
 *  - a PNG on disk, for anyone who wants the raw image.
 */
public final class ScreenshotUtils {

    private static final Logger log = LogManager.getLogger(ScreenshotUtils.class);
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");

    private ScreenshotUtils() {
    }

    public static String captureAsBase64(WebDriver driver) {
        if (driver == null) {
            return null;
        }
        try {
            return ((TakesScreenshot) driver).getScreenshotAs(OutputType.BASE64);
        } catch (Exception e) {
            // A screenshot failure must never mask the real test failure.
            log.warn("Could not capture Base64 screenshot: {}", e.toString());
            return null;
        }
    }

    public static String captureToFile(WebDriver driver, String testName) {
        if (driver == null) {
            return null;
        }
        try {
            byte[] png = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
            Path dir = Paths.get(ConfigReader.get("screenshot.dir", "reports/screenshots"));
            Files.createDirectories(dir);
            Path file = dir.resolve(sanitise(testName) + "_" + LocalDateTime.now().format(STAMP) + ".png");
            Files.write(file, png);
            log.info("Failure screenshot: {}", file.toAbsolutePath());
            return file.toAbsolutePath().toString();
        } catch (IOException | RuntimeException e) {
            log.warn("Could not write screenshot file: {}", e.toString());
            return null;
        }
    }

    private static String sanitise(String name) {
        return name == null ? "unknown" : name.replaceAll("[^a-zA-Z0-9_.-]", "_");
    }
}
